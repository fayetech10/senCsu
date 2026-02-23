package com.example.sencsu.domain.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.local.dao.PaiementDao
import com.example.sencsu.data.remote.dto.DashboardResponseDto
import com.example.sencsu.data.repository.DashboardRepository
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.domain.model.AuthState
import com.example.sencsu.domain.model.DashboardState
import com.example.sencsu.domain.sync.SyncManager
import com.example.sencsu.utils.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val connectivityObserver: ConnectivityObserver,
    private val adherentDao: AdherentDao,
    private val paiementDao: PaiementDao
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Whether the "Sync now?" prompt has been dismissed this session. */
    private val _syncPromptDismissed = MutableStateFlow(false)

    /** Number of records waiting for sync (adherents + paiements). */
    val pendingCount: StateFlow<Int> = combine(
        adherentDao.observeUnsyncedCount(),
        paiementDao.observeUnsyncedCount()
    ) { a, p -> a + p }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** True when there is pending data and the user hasn't dismissed the prompt yet. */
    val showSyncPrompt: StateFlow<Boolean> = combine(pendingCount, _syncPromptDismissed) { count, dismissed ->
        count > 0 && !dismissed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // 1. Observer l'utilisateur
        observeUser()
        // 2. Observer l'agentId de manière réactive
        observeAgentAndFetch()
        // 3. Observer la connectivité pour la synchronisation automatique
        observeConnectivity()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { isOnline ->
                if (isOnline) {
                    Log.d("DashboardViewModel", "Network restored. Triggering auto-sync.")
                    syncData()
                }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true) }
            _syncPromptDismissed.value = true // Dismiss the prompt once sync starts
            // Trigger sync
            syncManager.syncAllData()
            // Refresh data after sync
            val currentId = sessionManager.agentIdFlow.firstOrNull()
            if (currentId != null) {
                fetchDashboardData(currentId)
            } else {
                _dashboardState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Dismiss "Sync now?" prompt without syncing (user chose "Later"). */
    fun dismissSyncPrompt() {
        _syncPromptDismissed.value = true
    }

    private fun observeUser() {
        viewModelScope.launch {
            sessionManager.userFlow.collect { user ->
                _authState.update { it.copy(user = user) }
            }
        }
    }

    private fun observeAgentAndFetch() {
        viewModelScope.launch {
            sessionManager.agentIdFlow
                .filterNotNull()
                .distinctUntilChanged()
                .collect { id ->
                    fetchDashboardData(id)
                }
        }
    }

    fun fetchDashboardData(agentId: Long) {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true, error = null) }

            repository.getAdherentsByAgentId(agentId).fold(
                onSuccess = { adherents ->
                    _dashboardState.update {
                        it.copy(
                            isLoading = false,
                            data = DashboardResponseDto(data = adherents, success = true),
                            isSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _dashboardState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message,
                            isSuccess = false
                        )
                    }
                }
            )
        }
    }

    // Version améliorée du refresh pour le "Pull to Refresh"
    fun refresh() {
        viewModelScope.launch {
            // On récupère la dernière valeur émise par le flow au lieu du .value risqué
            val currentId = sessionManager.agentIdFlow.firstOrNull()
            currentId?.let { fetchDashboardData(it) }
        }
    }


    /**
     * Déconnexion de l'utilisateur
     */
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSessionAndNotify()
            _dashboardState.value = DashboardState()
            _authState.value = AuthState()
        }
    }
}