package com.example.sencsu.domain.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.local.entity.AdherentEntity
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.remote.dto.AdherentUpdateDto
import com.example.sencsu.data.remote.dto.FormConstants
import com.example.sencsu.data.remote.dto.PersonneChargeDto
import com.example.sencsu.data.repository.AdherentRepository
import com.example.sencsu.data.repository.DashboardRepository
import com.example.sencsu.data.repository.FileRepository
import com.example.sencsu.utils.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val ADHERENT_PRICE = 4500
private const val DEPENDANT_PRICE = 3500

sealed class AddAdherentUiEvent {
    data class ShowSnackbar(val message: String) : AddAdherentUiEvent()
    data class NavigateToPayment(val adherentId: Long?, val localAdherentId: Long?, val montantTotal: Int) : AddAdherentUiEvent()
    object NavigateBack : AddAdherentUiEvent()
}

/**
 * État UI pour le formulaire d'ajout/modification d'adhérent
 */
data class AddAdherentUiState(
    // ── Informations Personnelles ──
    val prenoms: String = "",
    val nom: String = "",
    val dateNaissance: String = "",
    val lieuNaissance: String = "",
    val sexe: String = "M",
    val situationMatrimoniale: String = "",

    // ── Contact & ID ──
    val whatsapp: String = "",
    val typePiece: String = "CNI",
    val numeroCNI: String = "",
    val numeroExtrait: String = "",
    val secteurActivite: String = "",

    // ── Localisation ──
    val departement: String = "",
    val commune: String = "",
    val adresse: String = "",

    // ── Photos Adhérent Principal ──
    val photoUri: Uri? = null,                    // Nouvelle photo (local)
    val rectoUri: Uri? = null,                    // Nouveau recto (local)
    val versoUri: Uri? = null,                    // Nouveau verso (local)
    val existingPhotoUrl: String? = null,         // Photo serveur existante
    val existingRectoUrl: String? = null,         // Recto serveur existant
    val existingVersoUrl: String? = null,         // Verso serveur existant
    val photoUploadProgress: Float = 0f,          // Progrès upload photo

    // ── Personnes à Charge ──
    val dependants: List<PersonneChargeDto> = emptyList(),
    val currentDependant: PersonneChargeDto = PersonneChargeDto(
        prenoms = "",
        nom = "",
        dateNaissance = "",
        sexe = "M",
        situationM = FormConstants.SITUATIONS.firstOrNull() ?: "",
        typePiece = "CNI"
    ),
    val editingIndex: Int? = null,                 // Index du dépendant en édition
    val isModalVisible: Boolean = false,

    // ── État Global ──
    val isLoading: Boolean = false,
    val totalCost: Int = ADHERENT_PRICE,
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class AddAdherentViewModel @Inject constructor(
    private val adherentRepository: DashboardRepository,
    private val fileRepository: FileRepository,
    private val adherentDao: AdherentDao,
    private val adherentRepo: AdherentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAdherentUiState())
    val uiState: StateFlow<AddAdherentUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<AddAdherentUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // ── Edit mode support ──────────────────────────────────────────────
    var isEditMode: Boolean = false
        private set
    var editAdherentId: Long? = null
        private set

    /**
     * Récupère un adhérent depuis l'API et prépare le formulaire pour l'édition
     */
    fun fetchAndLoadForEdit(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                adherentRepo.getAdherentById(id)
                    .onSuccess { adherent -> loadAdherentForEdit(adherent) }
                    .onFailure { e ->
                        val message = "Impossible de charger l'adhérent: ${e.message}"
                        _uiEvent.send(AddAdherentUiEvent.ShowSnackbar(message))
                        Log.e("AddAdherentVM", message, e)
                    }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Remplit le formulaire avec les données d'un adhérent existant pour l'édition
     * ⚠️ IMPORTANT: Les URLs serveur sont conservées séparément des URIs locales
     */
    fun loadAdherentForEdit(adherent: AdherentDto) {
        isEditMode = true
        editAdherentId = adherent.id
        _uiState.update {
            it.copy(
                prenoms = adherent.prenoms ?: "",
                nom = adherent.nom ?: "",
                adresse = adherent.adresse ?: "",
                lieuNaissance = adherent.lieuNaissance ?: "",
                sexe = adherent.sexe ?: "M",
                dateNaissance = adherent.dateNaissance ?: "",
                situationMatrimoniale = adherent.situationM ?: "",
                whatsapp = adherent.whatsapp ?: "",
                secteurActivite = adherent.secteurActivite ?: "",
                typePiece = adherent.typePiece ?: "CNI",
                numeroCNI = adherent.numeroCNi ?: "",
                departement = adherent.departement ?: "",
                commune = adherent.commune ?: "",

                // ✅ Conserver les URLs serveur EXISTANTES
                existingPhotoUrl = adherent.photo,
                existingRectoUrl = adherent.photoRecto,
                existingVersoUrl = adherent.photoVerso,

                // ✅ Les URIs locales restent à null jusqu'à une nouvelle sélection
                photoUri = null,
                rectoUri = null,
                versoUri = null,

                // ✅ Charger les dépendants
                dependants = adherent.personnesCharge,
                totalCost = calculateTotalCost(adherent.personnesCharge.size)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── MISES À JOUR DES CHAMPS ADHÉRENT
    // ─────────────────────────────────────────────────────────────────

    fun updatePrenoms(value: String) = _uiState.update {
        it.copy(prenoms = value, validationErrors = it.validationErrors - "prenoms")
    }

    fun updateNom(value: String) = _uiState.update {
        it.copy(nom = value, validationErrors = it.validationErrors - "nom")
    }

    fun updateAdresse(value: String) = _uiState.update {
        it.copy(adresse = value, validationErrors = it.validationErrors - "adresse")
    }

    fun updateLieuNaissance(value: String) = _uiState.update {
        it.copy(lieuNaissance = value, validationErrors = it.validationErrors - "lieuNaissance")
    }

    fun updateSexe(value: String) = _uiState.update { it.copy(sexe = value) }

    fun updateDateNaissance(date: String) = _uiState.update {
        it.copy(dateNaissance = date, validationErrors = it.validationErrors - "dateNaissance")
    }

    fun updateSituationMatrimoniale(value: String) = _uiState.update {
        it.copy(situationMatrimoniale = value)
    }

    fun updateWhatsapp(value: String) = _uiState.update {
        it.copy(
            whatsapp = Formatters.formatPhoneNumber(value),
            validationErrors = it.validationErrors - "whatsapp"
        )
    }

    fun updateSecteurActivite(value: String) = _uiState.update {
        it.copy(secteurActivite = value)
    }

    fun updateTypePiece(value: String) = _uiState.update { it.copy(typePiece = value) }

    fun updateNumeroCNI(value: String) = _uiState.update {
        it.copy(numeroCNI = value, validationErrors = it.validationErrors - "numeroCNI")
    }

    fun updateNumeroExtrait(value: String) = _uiState.update {
        it.copy(numeroExtrait = value, validationErrors = it.validationErrors - "numeroExtrait")
    }

    fun updateDepartement(value: String) = _uiState.update { it.copy(departement = value) }

    fun updateCommune(value: String) = _uiState.update {
        it.copy(commune = value, validationErrors = it.validationErrors - "commune")
    }

    // ─────────────────────────────────────────────────────────────────
    // ── GESTION DES PHOTOS ADHÉRENT PRINCIPAL
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ Mise à jour photo principale
     * - Accepte une nouvelle URI locale
     * - Invalide l'URL serveur existante si une nouvelle image est sélectionnée
     */
    fun updatePhotoUri(uri: Uri?) = _uiState.update {
        it.copy(
            photoUri = uri,
            // ✅ Si une nouvelle photo est sélectionnée, on ignore l'ancienne URL serveur
            existingPhotoUrl = if (uri != null) null else it.existingPhotoUrl,
            validationErrors = it.validationErrors - "photoUri"
        )
    }

    /**
     * ✅ Mise à jour recto CNI
     */
    fun updateRectoUri(uri: Uri?) = _uiState.update {
        it.copy(
            rectoUri = uri,
            existingRectoUrl = if (uri != null) null else it.existingRectoUrl
        )
    }

    /**
     * ✅ Mise à jour verso CNI
     */
    fun updateVersoUri(uri: Uri?) = _uiState.update {
        it.copy(
            versoUri = uri,
            existingVersoUrl = if (uri != null) null else it.existingVersoUrl
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // ── GESTION DES PHOTOS PERSONNES À CHARGE (MODAL)
    // ─────────────────────────────────────────────────────────────────

    fun updateCurrentDependant(dependant: PersonneChargeDto) {
        _uiState.update { it.copy(currentDependant = dependant) }
    }

    fun updateDependantPhotoUri(uri: Uri?) = _uiState.update {
        it.copy(currentDependant = it.currentDependant.copy(photo = uri?.toString()))
    }

    fun updateDependantRectoUri(uri: Uri?) = _uiState.update {
        it.copy(currentDependant = it.currentDependant.copy(photoRecto = uri?.toString()))
    }

    fun updateDependantVersoUri(uri: Uri?) = _uiState.update {
        it.copy(currentDependant = it.currentDependant.copy(photoVerso = uri?.toString()))
    }

    // ─────────────────────────────────────────────────────────────────
    // ── SOUMISSION DU FORMULAIRE
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ Soumission en MODE CRÉATION (ajout d'un nouvel adhérent)
     */
    fun submitWithUpload(context: Context, agentId: Long?) {
        if (agentId == null) {
            viewModelScope.launch {
                _uiEvent.send(AddAdherentUiEvent.ShowSnackbar("Agent ID manquant"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value
                val total = calculateTotalCost(state.dependants.size)
                val numeroPieceFinale = if (state.typePiece == "CNI") state.numeroCNI else state.numeroExtrait

                // 1️⃣ Sauvegarde locale dans Room (Offline-First)
                val localUuid = UUID.randomUUID().toString()
                val entity = AdherentEntity(
                    prenoms = state.prenoms,
                    nom = state.nom,
                    adresse = state.adresse,
                    lieuNaissance = state.lieuNaissance,
                    sexe = state.sexe,
                    dateNaissance = Formatters.formatDateForApi(state.dateNaissance),
                    situationM = state.situationMatrimoniale,
                    whatsapp = state.whatsapp,
                    secteurActivite = state.secteurActivite,
                    typePiece = state.typePiece,
                    numeroPiece = numeroPieceFinale,
                    numeroCNi = numeroPieceFinale,
                    departement = state.departement,
                    commune = state.commune,
                    region = "Thiès",
                    typeAdhesion = "Familiale",
                    montantTotal = total.toDouble(),
                    regime = "Contributif",
                    photo = state.photoUri?.toString(),
                    photoRecto = state.rectoUri?.toString(),
                    photoVerso = state.versoUri?.toString(),
                    actif = true,
                    isSynced = false,
                    localUuid = localUuid
                )
                val localId = adherentDao.insertAdherent(entity)

                // 2️⃣ Tentative de synchronisation API
                try {
                    // Upload des images
                    val photoUrl = state.photoUri?.let { uploadImage(context, it) }
                    val rectoUrl = state.rectoUri?.let { uploadImage(context, it) }
                    val versoUrl = state.versoUri?.let { uploadImage(context, it) }

                    // Upload des images des dépendants
                    val updatedDeps = uploadDependantsImages(context, state.dependants)

                    // Préparation du DTO
                    val adherentDto = buildAdherentDto(
                        photoUrl = photoUrl,
                        rectoUrl = rectoUrl,
                        versoUrl = versoUrl,
                        updatedDependants = updatedDeps,
                        clientUUID = localUuid
                    )

                    // Appel API
                    adherentRepository.ajouterAdherent(agentId, adherentDto)
                        .onSuccess { idResponse ->
                            adherentDao.markAsSynced(localId, idResponse.adherentId)
                            _uiEvent.send(AddAdherentUiEvent.ShowSnackbar("Adhérent ajouté avec succès !"))
                            _uiEvent.send(
                                AddAdherentUiEvent.NavigateToPayment(idResponse.adherentId, localId, total)
                            )
                            resetForm()
                        }
                        .onFailure { throw it }

                } catch (e: HttpException) {
                    handleHttpException(e, localId)
                } catch (e: IOException) {
                    Log.e("SubmitError", "Mode hors ligne", e)
                    _uiEvent.send(AddAdherentUiEvent.ShowSnackbar("Enregistré localement (hors ligne)"))
                    _uiEvent.send(AddAdherentUiEvent.NavigateToPayment(null, localId, total))
                    resetForm()
                } catch (e: Exception) {
                    Log.e("SubmitError", "Erreur inattendue", e)
                    adherentDao.deleteByLocalId(localId)
                    _uiEvent.send(AddAdherentUiEvent.ShowSnackbar(e.message ?: "Erreur inattendue"))
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * ✅ SOUMISSION EN MODE ÉDITION (modification d'un adhérent existant)
     *
     * Points clés:
     * - Les images locales (photoUri, rectoUri, versoUri) sont uploadées
     * - Les images serveur existantes sont conservées si pas de nouvelle sélection
     * - Synchronisation complète avec le backend
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun submitEdit(context: Context) {
        val id = editAdherentId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val numeroPieceFinale = if (state.typePiece == "CNI") state.numeroCNI else state.numeroExtrait

                // ✅ Traitement intelligent des photos:
                // - Si nouvelle URI locale → upload
                // - Sinon → garder l'URL serveur existante
                val photoUrl = when {
                    state.photoUri != null -> uploadImage(context, state.photoUri)
                    else -> state.existingPhotoUrl
                }

                val rectoUrl = when {
                    state.rectoUri != null -> uploadImage(context, state.rectoUri)
                    else -> state.existingRectoUrl
                }

                val versoUrl = when {
                    state.versoUri != null -> uploadImage(context, state.versoUri)
                    else -> state.existingVersoUrl
                }

                // Upload des photos des dépendants
                val updatedDeps = uploadDependantsImages(context, state.dependants)

                // Préparation du DTO de modification
                val dto = AdherentUpdateDto(
                    nom = state.nom,
                    prenoms = state.prenoms,
                    adresse = state.adresse,
                    lieuNaissance = state.lieuNaissance,
                    sexe = state.sexe,
                    dateNaissance = Formatters.formatDateForAPI(state.dateNaissance),
                    situationMatrimoniale = state.situationMatrimoniale,
                    whatsapp = state.whatsapp,
                    secteurActivite = state.secteurActivite,
                    region = "Thiès",
                    departement = state.departement,
                    commune = state.commune,
                    photo = photoUrl,
                    photoRecto = rectoUrl,
                    photoVerso = versoUrl,
                    personnesCharge = updatedDeps,
                    typePiece = state.typePiece,
                    numeroPiece = numeroPieceFinale,
                    numeroCNi = numeroPieceFinale
                )

                // Appel API de modification
                adherentRepository.updateAdherent(id, dto)
                    .onSuccess {
                        _uiEvent.send(AddAdherentUiEvent.ShowSnackbar("Adhérent mis à jour avec succès !"))
                        _uiEvent.send(AddAdherentUiEvent.NavigateBack)
                        resetForm()
                    }
                    .onFailure { e ->
                        Log.e("SubmitEdit", "Erreur modification", e)
                        _uiEvent.send(
                            AddAdherentUiEvent.ShowSnackbar(e.message ?: "Erreur lors de la mise à jour")
                        )
                    }
            } catch (e: Exception) {
                Log.e("SubmitEdit", "Erreur inattendue", e)
                sendError(e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── FONCTIONS UTILITAIRES POUR UPLOAD
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ Upload une image et retourne l'URL serveur
     */
    private suspend fun uploadImage(context: Context, uri: Uri): String? {
        return try {
            fileRepository.uploadImage(context, uri).getOrNull()
        } catch (e: Exception) {
            Log.e("ImageUpload", "Erreur upload image", e)
            null
        }
    }

    /**
     * ✅ Upload les images des dépendants
     * Traite intelligemment:
     * - URIs locales (content://) → upload
     * - URLs serveur (http://) → conservation
     */
    private suspend fun uploadDependantsImages(
        context: Context,
        dependants: List<PersonneChargeDto>
    ): List<PersonneChargeDto> {
        return dependants.map { dep ->
            dep.copy(
                photo = processImageUri(context, dep.photo),
                photoRecto = processImageUri(context, dep.photoRecto),
                photoVerso = processImageUri(context, dep.photoVerso)
            )
        }
    }

    /**
     * ✅ Traite une URI/URL d'image:
     * - Si c'est une URI locale (content://) → upload et retourne URL serveur
     * - Si c'est déjà une URL serveur → la retourne telle quelle
     * - Sinon → retourne null
     */
    private suspend fun processImageUri(context: Context, uriString: String?): String? {
        return when {
            uriString?.startsWith("content://") == true -> {
                uploadImage(context, Uri.parse(uriString))
            }
            uriString?.startsWith("http") == true -> {
                uriString // Garder l'URL serveur
            }
            else -> null
        }
    }

    /**
     * ✅ Construit le DTO pour la création d'adhérent
     */
    private fun buildAdherentDto(
        photoUrl: String?,
        rectoUrl: String?,
        versoUrl: String?,
        updatedDependants: List<PersonneChargeDto>,
        clientUUID: String
    ): AdherentDto {
        val state = _uiState.value
        val numeroPieceFinale = if (state.typePiece == "CNI") state.numeroCNI else state.numeroExtrait

        return AdherentDto(
            prenoms = state.prenoms,
            nom = state.nom,
            adresse = state.adresse,
            lieuNaissance = state.lieuNaissance,
            sexe = state.sexe,
            dateNaissance = Formatters.formatDateForApi(state.dateNaissance),
            situationM = state.situationMatrimoniale,
            whatsapp = state.whatsapp,
            secteurActivite = state.secteurActivite,
            typePiece = state.typePiece,
            numeroPiece = numeroPieceFinale,
            numeroCNi = numeroPieceFinale,
            departement = state.departement,
            commune = state.commune,
            region = "Thiès",
            regime = "Contributif",
            typeBenef = "Classique",
            typeAdhesion = "Familiale",
            photo = photoUrl,
            photoRecto = rectoUrl,
            photoVerso = versoUrl,
            clientUUID = clientUUID,
            personnesCharge = updatedDependants.map { dep ->
                dep.copy(
                    dateNaissance = if (!dep.dateNaissance.isNullOrBlank()) {
                        Formatters.formatDateForApi(dep.dateNaissance)
                    } else dep.dateNaissance
                )
            }
        )
    }

    /**
     * ✅ Gestion des erreurs HTTP
     */
    private suspend fun handleHttpException(e: HttpException, localId: Long) {
        Log.e("SubmitError", "Erreur API: ${e.code()}", e)
        adherentDao.deleteByLocalId(localId)

        val errorBody = try {
            e.response()?.errorBody()?.string()
        } catch (ex: Exception) {
            null
        }
        val message = errorBody ?: "Erreur serveur (${e.code()})"
        _uiEvent.send(AddAdherentUiEvent.ShowSnackbar(message))
    }

    // ─────────────────────────────────────────────────────────────────
    // ── GESTION DES PERSONNES À CHARGE (MODAL)
    // ─────────────────────────────────────────────────────────────────

    fun showAddDependantModal() {
        resetDependantForm()
        _uiState.update { it.copy(isModalVisible = true, editingIndex = null) }
    }

    fun showEditDependantModal(index: Int, dependant: PersonneChargeDto) {
        _uiState.update {
            it.copy(
                isModalVisible = true,
                editingIndex = index,
                currentDependant = dependant
            )
        }
    }

    fun hideModal() = _uiState.update { it.copy(isModalVisible = false) }

    fun saveDependant() {
        _uiState.update { state ->
            val newList = state.dependants.toMutableList()
            if (state.editingIndex != null) {
                newList[state.editingIndex] = state.currentDependant
            } else {
                newList.add(state.currentDependant)
            }
            state.copy(
                dependants = newList,
                totalCost = calculateTotalCost(newList.size),
                isModalVisible = false,
                editingIndex = null
            )
        }
        resetDependantForm()
    }

    fun removeDependant(index: Int) {
        _uiState.update { state ->
            val newList = state.dependants.toMutableList()
            newList.removeAt(index)
            state.copy(
                dependants = newList,
                totalCost = calculateTotalCost(newList.size)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── RESETS & CALCULS
    // ─────────────────────────────────────────────────────────────────

    fun resetForm() {
        isEditMode = false
        editAdherentId = null
        _uiState.value = AddAdherentUiState()
    }

    private fun resetDependantForm() {
        _uiState.update {
            it.copy(
                currentDependant = PersonneChargeDto(
                    prenoms = "",
                    nom = "",
                    dateNaissance = "",
                    sexe = "M",
                    situationM = FormConstants.SITUATIONS.firstOrNull() ?: "",
                    typePiece = "CNI"
                ),
                editingIndex = null
            )
        }
    }

    private fun calculateTotalCost(count: Int): Int = ADHERENT_PRICE + (count * DEPENDANT_PRICE)

    // ─────────────────────────────────────────────────────────────────
    // ── VALIDATION DU FORMULAIRE
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ Validation par étape du formulaire
     */
    fun validateStep(step: Int): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        when (step) {
            0 -> { // Identité
                if (state.prenoms.isBlank()) errors["prenoms"] = "Le prénom est requis"
                if (state.nom.isBlank()) errors["nom"] = "Le nom est requis"
                if (state.dateNaissance.isBlank()) errors["dateNaissance"] = "La date de naissance est requise"
            }
            1 -> { // Contact & ID
                if (state.whatsapp.isBlank()) errors["whatsapp"] = "Le numéro WhatsApp est requis"

                if (state.typePiece == "CNI") {
                    if (state.numeroCNI.isBlank()) errors["numeroCNI"] = "Le numéro CNI est requis"
                } else {
                    if (state.numeroExtrait.isBlank()) errors["numeroExtrait"] = "Le numéro d'extrait est requis"
                }
            }
            2 -> { // Localisation
                if (state.commune.isBlank()) errors["commune"] = "La commune est requise"
                if (state.adresse.isBlank()) errors["adresse"] = "L'adresse est requise"
            }
            3 -> { // Photos
                // En mode édition, la photo n'est pas obligatoire si elle existe déjà
                if (!isEditMode) {
                    if (state.photoUri == null && state.existingPhotoUrl.isNullOrBlank()) {
                        errors["photoUri"] = "La photo d'identité est requise"
                    }
                } else {
                    // En édition, au moins une photo doit exister
                    if (state.photoUri == null && state.existingPhotoUrl.isNullOrBlank()) {
                        errors["photoUri"] = "La photo d'identité est requise"
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return false
        }

        _uiState.update { it.copy(validationErrors = emptyMap()) }
        return true
    }

    // ─────────────────────────────────────────────────────────────────
    // ── GESTION DES ERREURS
    // ─────────────────────────────────────────────────────────────────

    private suspend fun sendError(exception: Exception) {
        val errorMessage = when (exception) {
            is HttpException -> {
                try {
                    exception.response()?.errorBody()?.string()
                        ?: "Erreur serveur (${exception.code()})"
                } catch (e: Exception) {
                    "Erreur serveur (${exception.code()})"
                }
            }
            is IOException -> "Problème de connexion internet."
            else -> exception.localizedMessage ?: "Une erreur est survenue."
        }

        _uiEvent.send(AddAdherentUiEvent.ShowSnackbar(errorMessage))
    }
}