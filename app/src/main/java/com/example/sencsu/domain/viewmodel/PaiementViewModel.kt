package com.example.sencsu.domain.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.local.dao.PaiementDao
import com.example.sencsu.data.local.entity.PaiementEntity
import com.example.sencsu.data.remote.dto.PaiementDto
import com.example.sencsu.data.remote.dto.ValidationException
import com.example.sencsu.data.repository.AdherentRepository
import com.example.sencsu.data.repository.FileRepository
import com.example.sencsu.data.repository.PaiementRepository
import com.example.sencsu.screen.PaiementFormState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PaiementViewModel"
private const val MAX_CODE_LENGTH = 35
private const val MIN_CODE_LENGTH = 15

@HiltViewModel
class PaiementViewModel @Inject constructor(
    private val paiementRepository: PaiementRepository,
    private val adherentRepository: AdherentRepository,
    private val fileRepository: FileRepository,
    private val paiementDao: PaiementDao,
    private val adherentDao: AdherentDao,
    application: Application
) : ViewModel() {
    private val context: Context = application.applicationContext


    var uiState by mutableStateOf(PaiementFormState())
        private set

    /* ---------------------- FORM UPDATES ---------------------- */

    fun initializeFormData(adherentId: Long?, localAdherentId: Long?, montantTotal: Double?) {
        uiState = uiState.copy(
            adherentId = adherentId,
            localAdherentId = localAdherentId,
            montantTotal = montantTotal
        )
    }

    fun updateReference(ref: String) {
        uiState = uiState.copy(reference = ref)
    }

    fun updateMode(mode: String) {
        uiState = uiState.copy(modePaiement = mode)
    }

    fun setPhoto(uri: Uri?) {
        uiState = uiState.copy(photoPaiement = uri)
    }

    /* ---------------------- ADD PAYMENT ---------------------- */

    fun loadAdherent(adherentId: Long?, localAdherentId: Long?) {
        viewModelScope.launch {
            if (adherentId != null) {
                adherentRepository.getAdherentById(adherentId)
                    .onSuccess { adherent ->
                        uiState = uiState.copy(
                            adherentId = adherent.id,
                            montantTotal = adherent.montantTotal
                        )
                    }
            } else if (localAdherentId != null) {
                val localAdherent = adherentDao.getAdherentById(localAdherentId)
                if (localAdherent != null) {
                    uiState = uiState.copy(
                        localAdherentId = localAdherent.localId,
                        montantTotal = localAdherent.montantTotal
                    )
                }
            }
        }
    }


    fun addPaiement() {
        viewModelScope.launch {
            try {
                // 1. Validation de l'UI
                if (uiState.photoPaiement == null) {
                    uiState = uiState.copy(errorMessage = "Veuillez ajouter la photo du reçu")
                    return@launch
                }

                uiState = uiState.copy(isLoading = true, errorMessage = "")

                // 2. Offline-First: Sauvegarde immédiate dans Room
                val entity = PaiementEntity(
                    reference = uiState.reference,
                    montant = uiState.montantTotal ?: 0.0,
                    modePaiement = uiState.modePaiement,
                    photoPaiement = uiState.photoPaiement?.toString(),
                    adherentId = uiState.adherentId,
                    localAdherentId = uiState.localAdherentId,
                    isSynced = false
                )
                val localId = paiementDao.insertPaiement(entity)

                // 3. Tentative d'envoi API
                try {
                    // Upload de l'image
                    val uploadResult = fileRepository.uploadImage(context, uiState.photoPaiement!!)
                    val photoUrl = uploadResult.getOrThrow()

                    if (uiState.adherentId != null) {
                        val paiementDto = PaiementDto(
                            adherentId = uiState.adherentId!!,
                            reference = uiState.reference,
                            montant = uiState.montantTotal!!,
                            modePaiement = uiState.modePaiement,
                            photoPaiement = photoUrl,
                            photos = listOf(photoUrl)
                        )

                        // Appel du repository
                        paiementRepository.addPaiement(paiementDto)
                            .onSuccess {
                                Log.d(TAG, "Paiement ajouté avec succès")
                                // Synchronisation réussie
                                paiementDao.markAsSynced(localId, localId)
                                uiState = uiState.copy(isLoading = false, isSuccess = true)
                            }
                            .onFailure { exception ->
                                Log.e(TAG, "Erreur lors de l'ajout: ${exception.message}", exception)

                                // SI ERREUR DE VALIDATION (ex: Référence déjà utilisée)
                                if (exception is ValidationException) {
                                    // On supprime de Room car la donnée est invalide
                                    // et ne pourra JAMAIS être synchronisée
                                    paiementDao.clearAll()

                                    uiState = uiState.copy(
                                        isLoading = false,
                                        isSuccess = false,
                                        errorMessage = exception.message
                                            ?: "Erreur de validation du serveur"
                                    )
                                } else {
                                    // Autre erreur (500, Timeout, etc)
                                    // On laisse en local pour synchro ultérieure
                                    throw exception
                                }
                            }
                    } else {
                        // Pas d'ID distant, on reste en succès local
                        uiState = uiState.copy(isLoading = false, isSuccess = true)
                    }

                } catch (e: Exception) {
                    // 4. Gestion du mode Hors-ligne
                    // (Erreur réseau, timeout, ou erreur non-validation)
                    Log.w(TAG, "Mode offline activé: ${e.message}", e)

                    uiState = uiState.copy(
                        isLoading = false,
                        isSuccess = true, // L'utilisateur voit "Succès"
                        errorMessage = "" // Synchro en tâche de fond
                    )
                }
            } catch (e: Exception) {
                // Erreurs critiques inattendues
                Log.e(TAG, "Erreur critique", e)
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erreur inattendue",
                    isSuccess = false
                )
            }
        }
    }
    /* ---------------------- OCR ---------------------- */

    fun processImage(
        uri: Uri,
        context: Context,
        recognizer: TextRecognizer
    ) {
        uiState = uiState.copy(isLoading = true, errorMessage = "")

        try {
            val image = InputImage.fromFilePath(context, uri)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedRef = extractReferencePattern(visionText.text)
                    uiState = uiState.copy(
                        reference = detectedRef,
                        isLoading = false,
                        errorMessage = if (detectedRef.isEmpty()) "Aucune référence détectée" else ""
                    )
                }
                .addOnFailureListener { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = "Erreur lors de la lecture du document OCR"
                    )
                    Log.e(TAG, "Erreur OCR", exception)
                }
        } catch (e: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = "Fichier invalide ou inaccessible"
            )
            Log.e(TAG, "Exception lors du traitement de l'image", e)
        }
    }

    /* ---------------------- OCR PARSING ---------------------- */

    private fun extractReferencePattern(text: String): String {
        val normalizedText = normalizeText(text)
        Log.d(TAG, "Texte OCR normalisé: $normalizedText")

        val strategies = listOf(
            ::findByTransactionIdPattern,
            ::findByTransactionIdWithSeparator,
            ::findByTransactionLabel,
            ::findByCodePattern
        )

        for (strategy in strategies) {
            val code = strategy(normalizedText)
            if (code.isNotEmpty()) {
                Log.d(TAG, "Code validé: $code")
                return code
            }
        }

        Log.d(TAG, "Aucun code trouvé avec aucune stratégie")
        return ""
    }

    private fun normalizeText(text: String): String {
        return text
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun findByTransactionIdPattern(text: String): String {
        val regex = Regex(
            """(?i)\bid\s+de\s+transaction\s+([A-Za-z0-9]+(?:\s+[A-Za-z0-9]+)?)""",
            RegexOption.IGNORE_CASE
        )
        return extractAndValidateCode(regex.find(text), "Stratégie 1: ID de transaction")
    }

    private fun findByTransactionIdWithSeparator(text: String): String {
        val regex = Regex(
            """(?i)\bid\s+de\s+transaction\s*[:\-]?\s*([A-Za-z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        return extractAndValidateCode(regex.find(text), "Stratégie 2: ID de transaction avec séparateur")
    }

    private fun findByTransactionLabel(text: String): String {
        val regex = Regex(
            """(?i)\btransaction\s+([A-Za-z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        return extractAndValidateCode(regex.find(text), "Stratégie 3: Étiquette transaction")
    }

    private fun findByCodePattern(text: String): String {
        val regex = Regex("""([A-Za-z0-9]{$MIN_CODE_LENGTH,$MAX_CODE_LENGTH})""")
        val allMatches = regex.findAll(text)

        for (match in allMatches) {
            val code = match.value.trim()
            if (isValidTransactionCode(code)) {
                Log.d(TAG, "Code candidat validé (Stratégie 4): $code")
                return code
            }
        }
        return ""
    }

    private fun extractAndValidateCode(match: MatchResult?, strategy: String): String {
        match?.let {
            var code = it.groupValues[1]
                .trim()
                .replace(Regex("\\s+"), "")

            if (code.length > MAX_CODE_LENGTH) {
                code = code.substring(0, MAX_CODE_LENGTH)
            }

            if (isValidTransactionCode(code)) {
                Log.d(TAG, "$strategy - Code trouvé: $code")
                return code
            }
        }
        return ""
    }

    private fun isValidTransactionCode(code: String): Boolean {
        if (code.length < MIN_CODE_LENGTH || code.length > MAX_CODE_LENGTH) {
            Log.d(TAG, "Code rejeté (longueur invalide): ${code.length}")
            return false
        }

        if (!code.any { it.isDigit() }) {
            Log.d(TAG, "Code rejeté (pas de chiffres): $code")
            return false
        }

        if (isFrenchKeyword(code)) {
            Log.d(TAG, "Code rejeté (mot-clé français): $code")
            return false
        }

        return true
    }

    private fun isFrenchKeyword(code: String): Boolean {
        val invalidPatterns = listOf(
            "PAIEMENT", "STATUT", "EFFECTUE", "FRAIS", "MONTANT",
            "REFERENCE", "DATE", "HEURE", "PARTENARIAT", "DIGITAL",
            "FINANCE", "WAVE", "ORANGE"
        )

        return invalidPatterns.any { pattern ->
            code.startsWith(pattern, ignoreCase = true)
        }
    }
}