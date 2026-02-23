package com.example.sencsu.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.local.dao.PaiementDao
import com.example.sencsu.data.local.entity.AdherentEntity
import com.example.sencsu.data.local.entity.PaiementEntity
import com.example.sencsu.domain.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val unsyncedAdherents: List<AdherentEntity> = emptyList(),
    val unsyncedPaiements: List<PaiementEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val syncSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val adherentDao: AdherentDao,
    private val paiementDao: PaiementDao,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    /** Total number of records pending sync (used in Dashboard badge/modal). */
    val pendingCount: StateFlow<Int> = combine(
        adherentDao.observeUnsyncedCount(),
        paiementDao.observeUnsyncedCount()
    ) { adherentCount, paiementCount ->
        adherentCount + paiementCount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        observePendingData()
    }

    private fun observePendingData() {
        viewModelScope.launch {
            combine(
                adherentDao.observeUnsyncedAdherents(),
                paiementDao.observeUnsyncedPaiements()
            ) { adherents, paiements ->
                _uiState.update {
                    it.copy(
                        unsyncedAdherents = adherents,
                        unsyncedPaiements = paiements
                    )
                }
            }.collect()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null, syncSuccess = false) }
            try {
                syncManager.syncAllData()
                _uiState.update { it.copy(isSyncing = false, syncSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = e.message ?: "Erreur lors de la synchronisation"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
