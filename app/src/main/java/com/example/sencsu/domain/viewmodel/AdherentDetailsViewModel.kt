package com.example.sencsu.domain.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.sencsu.data.local.dao.AdherentDao
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val newPersonne: PersonneChargeDto = PersonneChargeDto()
)

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
    private val dashdoardRepository: DashboardRepository,
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

    fun refresh() {
        val id = adherentIdStr?.toLongOrNull()
        if (id != null) {
            fetchAdherentDetails(id)
        } else {
            _state.update { it.copy(error = "Identifiant invalide.") }
        }
    }

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
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Erreur réseau") }
                }
            )
        }
    }

    fun loadCotisationsByIdadherent() {
        val id = adherentIdStr?.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                val result = cotisationRepository.getCotisationByIdahderent(id)
                _state.update { it.copy(cotisations = result) }
            } catch (e: Exception) {
                Log.e("AdherentVM", "Erreur cotisations", e)
            }
        }
    }

    fun loadPaiementByIdadherent() {
        val id = adherentIdStr?.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                val result = paiementRepository.getPaiementsByAdherentId(id)
                _state.update { it.copy(paiements = result) }
            } catch (e: Exception) {
                Log.e("AdherentVM", "Erreur paiements", e)
            }
        }
    }

    // --- Actions ---
    fun showDeleteAdherentConfirmation() = _state.update { it.copy(showDeleteAdherentDialog = true) }
    fun cancelDeleteAdherent() = _state.update { it.copy(showDeleteAdherentDialog = false) }

    fun confirmDeleteAdherent() {
        val id = adherentIdStr?.toLongOrNull() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showDeleteAdherentDialog = false) }
            adherentRepository.deleteAdherent(id).fold(
                onSuccess = {
                    // Supprimer du cache local
                    launch { adherentDao.deleteByRemoteId(id) }

                    // Mise à jour silencieuse du dashboard en arrière-plan
                    launch {
                        try {
                            val result = dashdoardRepository.getDashboardData()
                            Log.d("Adherent", "Dashboard list updated, size : ${result.data.size}")
                        } catch (e: Exception) {
                            Log.e("Adherent", "Failed to update dashboard list in background", e)
                        }
                    }

                    _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Adhérent supprimé"))
                    _uiEvent.emit(DetailsUiEvent.AdherentDeleted)
                },
                onFailure = { e ->
                    Log.e("Adherent", e.message.toString())
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur lors de la suppression: ${e.message}"))
                }
            )
        }
    }

    fun showDeletePersonneConfirmation(personne: PersonneChargeDto) = _state.update { it.copy(personToDelete = personne) }
    fun cancelDeletePersonne() = _state.update { it.copy(personToDelete = null) }

    fun confirmDeletePersonne() {
        val adherentId = adherentIdStr?.toLongOrNull() ?: return
        val personneId = _state.value.personToDelete?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adherentRepository.deletePersonneCharge(adherentId, personneId).fold(
                onSuccess = {
                    _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Bénéficiaire supprimé"))
                    refresh()
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur: ${e.message}"))
                }
            )
            cancelDeletePersonne()
        }
    }

    fun openImagePreview(url: String?) {
        if (!url.isNullOrBlank()) _state.update { it.copy(selectedImageUrl = url) }
    }
    fun closeImagePreview() = _state.update { it.copy(selectedImageUrl = null) }

    fun onAddPersonneClicked() = _state.update { it.copy(showAddPersonneModal = true, newPersonne = PersonneChargeDto()) }
    fun onDismissAddPersonneModal() = _state.update { it.copy(showAddPersonneModal = false) }
    fun onNewPersonneChange(p: PersonneChargeDto) = _state.update { it.copy(newPersonne = p) }

    fun onSaveNewPersonne(context: Context) {
        val id = adherentIdStr?.toLongOrNull() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // 1. Uploader les images si nécessaires
                val updatedPersonne = uploadPersonneImages(context, _state.value.newPersonne)
                
                // 2. Appel API
                adherentRepository.addPersonneCharge(id, updatedPersonne).fold(
                    onSuccess = {
                        _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Bénéficiaire ajouté avec succès"))
                        onDismissAddPersonneModal()
                        refresh()
                    },
                    onFailure = { e ->
                        _state.update { it.copy(isLoading = false) }
                        _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur: ${e.message}"))
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _uiEvent.emit(DetailsUiEvent.ShowSnackbar("Erreur lors de l'upload: ${e.message}"))
            }
        }
    }

    private suspend fun uploadPersonneImages(context: Context, personne: PersonneChargeDto): PersonneChargeDto {
        suspend fun uploadIfLocal(uriString: String?): String? {
            return if (uriString?.startsWith("content://") == true) {
                fileRepository.uploadImage(context, Uri.parse(uriString)).getOrNull()
            } else uriString
        }

        return personne.copy(
            photo = uploadIfLocal(personne.photo),
            photoRecto = uploadIfLocal(personne.photoRecto),
            photoVerso = uploadIfLocal(personne.photoVerso)
        )
    }
}