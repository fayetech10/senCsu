package com.example.sencsu.domain.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.remote.dto.CotisationDto
import com.example.sencsu.data.remote.dto.PaiementDto
import com.example.sencsu.data.remote.dto.PersonneChargeDto
import com.example.sencsu.data.repository.AdherentRepository
import com.example.sencsu.data.repository.Cotisationepository
import com.example.sencsu.data.repository.DashboardRepository
import com.example.sencsu.data.repository.FileRepository
import com.example.sencsu.data.repository.PaiementRepository
import com.example.sencsu.data.repository.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * État UI pour l'écran de détails d'un adhérent
 */
data class AdherentDetailsState(
    val isLoading: Boolean = false,
    val adherent: AdherentDto? = null,
    val error: String? = null,
    val paiements: List<PaiementDto> = emptyList(),
    val cotisations: List<CotisationDto> = emptyList(),
    val showDeleteAdherentDialog: Boolean = false,
    val personToDelete: PersonneChargeDto? = null,
    val showAddPersonneModal: Boolean = false,
    val selectedImageUrl: String? = null,
    val newPersonne: PersonneChargeDto = PersonneChargeDto(),
    val newPersonnePhotoUri: Uri? = null,              // ✅ Photo nouvelle pour personne
    val newPersonneRectoUri: Uri? = null,              // ✅ Recto nouveau
    val newPersonneVersoUri: Uri? = null,              // ✅ Verso nouveau
)

/**
 * Événements UI pour l'écran de détails
 */
sealed class DetailsUiEvent {
    data class ShowSnackbar(val message: String) : DetailsUiEvent()
    data object AdherentDeleted : DetailsUiEvent()
}

@HiltViewModel
class AdherentDetailsViewModel @Inject constructor(
    private val adherentRepository: AdherentRepository,
    savedStateHandle: SavedStateHandle,
    val sessionManager: SessionManager,
    private val paiementRepository: PaiementRepository,
    private val cotisationRepository: Cotisationepository,
    private val dashboardRepository: DashboardRepository,
    private val fileRepository: FileRepository,
    private val adherentDao: AdherentDao
) : ViewModel() {

    private val _state = MutableStateFlow(AdherentDetailsState())
    val state: StateFlow<AdherentDetailsState> = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<DetailsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val adherentIdStr: String? = savedStateHandle["id"]

    init {
        refresh()
    }

    // ─────────────────────────────────────────────────────────────────
    // ── CHARGEMENT DES DONNÉES
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ Rafraîchit les données de l'adhérent et ses informations associées
     */
    fun refresh() {
        val id = adherentIdStr?.toLongOrNull()
        if (id != null) {
            fetchAdherentDetails(id)
        } else {
            _state.update { it.copy(error = "Identifiant invalide.") }
        }
    }

    /**
     * ✅ Récupère les détails de l'adhérent depuis l'API
     */
    private fun fetchAdherentDetails(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            adherentRepository.getAdherentById(id).fold(
                onSuccess = { adherent ->
                    _state.update { it.copy(isLoading = false, adherent = adherent) }
                    loadPaiementByIdadherent()
                    loadCotisationsByIdadherent()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Erreur réseau"
                        )
                    }
                    Log.e("AdherentDetailsVM", "Erreur chargement", error)
                }
            )
        }
    }

    /**
     * ✅ Charge les cotisations de l'adhérent
     */
    fun loadCotisationsByIdadherent() {
        val id = adherentIdStr?.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                val result = cotisationRepository.getCotisationByIdahderent(id)
                _state.update { it.copy(cotisations = result) }
            } catch (e: Exception) {
                Log.e("AdherentDetailsVM", "Erreur cotisations", e)
            }
        }
    }

    /**
     * ✅ Charge les paiements de l'adhérent
     */
    fun loadPaiementByIdadherent() {
        val id = adherentIdStr?.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                val result = paiementRepository.getPaiementsByAdherentId(id)
                _state.update { it.copy(paiements = result) }
            } catch (e: Exception) {
                Log.e("AdherentDetailsVM", "Erreur paiements", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── SUPPRESSION D'ADHÉRENT
    // ─────────────────────────────────────────────────────────────────

    fun showDeleteAdherentConfirmation() {
        _state.update { it.copy(showDeleteAdherentDialog = true) }
    }

    fun cancelDeleteAdherent() {
        _state.update { it.copy(showDeleteAdherentDialog = false) }
    }

    /**
     * ✅ Supprime l'adhérent et met à jour les données locales
     */
    fun confirmDeleteAdherent() {
        val id = adherentIdStr?.toLongOrNull() ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showDeleteAdherentDialog = false) }
            try {
                adherentRepository.deleteAdherent(id).fold(
                    onSuccess = {
                        // Supprimer du cache local
                        adherentDao.deleteByRemoteId(id)

                        // Mettre à jour le dashboard en arrière-plan
                        viewModelScope.launch {
                            try {
                                dashboardRepository.getDashboardData()
                                Log.d("AdherentDetailsVM", "Dashboard mis à jour")
                            } catch (e: Exception) {
                                Log.e("AdherentDetailsVM", "Erreur MAJ dashboard", e)
                            }
                        }

                        _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Adhérent supprimé"))
                        _uiEvent.emit(DetailsUiEvent.AdherentDeleted)
                    },
                    onFailure = { e ->
                        _state.update { it.copy(isLoading = false) }
                        Log.e("AdherentDetailsVM", "Erreur suppression adhérent", e)
                        _uiEvent.emit(
                            DetailsUiEvent.ShowSnackbar("Erreur: ${e.message}")
                        )
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                Log.e("AdherentDetailsVM", "Erreur inattendue", e)
                _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur inattendue"))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── SUPPRESSION DE PERSONNE À CHARGE
    // ─────────────────────────────────────────────────────────────────

    fun showDeletePersonneConfirmation(personne: PersonneChargeDto) {
        _state.update { it.copy(personToDelete = personne) }
    }

    fun cancelDeletePersonne() {
        _state.update { it.copy(personToDelete = null) }
    }

    /**
     * ✅ Supprime une personne à charge et rafraîchit les données
     */
    fun confirmDeletePersonne() {
        val adherentId = adherentIdStr?.toLongOrNull() ?: return
        val personneId = _state.value.personToDelete?.id ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                adherentRepository.deletePersonneCharge(adherentId, personneId).fold(
                    onSuccess = {
                        _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Bénéficiaire supprimé"))
                        refresh()
                    },
                    onFailure = { e ->
                        _state.update { it.copy(isLoading = false) }
                        Log.e("AdherentDetailsVM", "Erreur suppression personne", e)
                        _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur: ${e.message}"))
                    }
                )
            } finally {
                cancelDeletePersonne()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── APERÇU D'IMAGE
    // ─────────────────────────────────────────────────────────────────

    fun openImagePreview(url: String?) {
        if (!url.isNullOrBlank()) {
            _state.update { it.copy(selectedImageUrl = url) }
        }
    }

    fun closeImagePreview() {
        _state.update { it.copy(selectedImageUrl = null) }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── GESTION DE LA MODAL AJOUT PERSONNE À CHARGE
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ Ouvre la modal d'ajout de personne
     */
    fun onAddPersonneClicked() {
        _state.update {
            it.copy(
                showAddPersonneModal = true,
                newPersonne = PersonneChargeDto(),
                newPersonnePhotoUri = null,
                newPersonneRectoUri = null,
                newPersonneVersoUri = null
            )
        }
    }

    fun onDismissAddPersonneModal() {
        _state.update {
            it.copy(
                showAddPersonneModal = false,
                newPersonne = PersonneChargeDto(),
                newPersonnePhotoUri = null,
                newPersonneRectoUri = null,
                newPersonneVersoUri = null
            )
        }
    }

    /**
     * ✅ Met à jour les données de la nouvelle personne
     */
    fun onNewPersonneChange(p: PersonneChargeDto) {
        _state.update { it.copy(newPersonne = p) }
    }

    /**
     * ✅ Mise à jour des photos de la nouvelle personne
     */
    fun updateNewPersonnePhotoUri(uri: Uri?) {
        _state.update { it.copy(newPersonnePhotoUri = uri) }
    }

    fun updateNewPersonneRectoUri(uri: Uri?) {
        _state.update { it.copy(newPersonneRectoUri = uri) }
    }

    fun updateNewPersonneVersoUri(uri: Uri?) {
        _state.update { it.copy(newPersonneVersoUri = uri) }
    }

    /**
     * ✅ Sauvegarde la nouvelle personne à charge avec ses photos
     */
    fun onSaveNewPersonne(context: Context) {
        val adherentId = adherentIdStr?.toLongOrNull() ?: return
        val state = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // 1️⃣ Upload les photos si fournies
                val photoUrl = state.newPersonnePhotoUri?.let {
                    uploadImage(context, it)
                }
                val rectoUrl = state.newPersonneRectoUri?.let {
                    uploadImage(context, it)
                }
                val versoUrl = state.newPersonneVersoUri?.let {
                    uploadImage(context, it)
                }

                // 2️⃣ Prépare le DTO avec les URLs uploadées
                val personneToAdd = state.newPersonne.copy(
                    photo = photoUrl ?: state.newPersonne.photo,
                    photoRecto = rectoUrl ?: state.newPersonne.photoRecto,
                    photoVerso = versoUrl ?: state.newPersonne.photoVerso
                )

                // 3️⃣ Appel API
                adherentRepository.addPersonneCharge(adherentId, personneToAdd).fold(
                    onSuccess = {
                        _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Bénéficiaire ajouté avec succès"))
                        onDismissAddPersonneModal()
                        refresh()
                    },
                    onFailure = { e ->
                        _state.update { it.copy(isLoading = false) }
                        Log.e("AdherentDetailsVM", "Erreur ajout personne", e)
                        _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur: ${e.message}"))
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                Log.e("AdherentDetailsVM", "Erreur upload images", e)
                _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur lors de l'upload: ${e.message}"))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── FONCTIONS UTILITAIRES
    // ─────────────────────────────────────────────────────────────────

    /**
     * ✅ Upload une image et retourne son URL serveur
     */
    private suspend fun uploadImage(context: Context, uri: Uri): String? {
        return try {
            fileRepository.uploadImage(context, uri).getOrNull()
        } catch (e: Exception) {
            Log.e("ImageUpload", "Erreur upload", e)
            null
        }
    }
}