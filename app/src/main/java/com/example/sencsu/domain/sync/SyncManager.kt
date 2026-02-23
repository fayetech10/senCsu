package com.example.sencsu.domain.sync

import android.util.Log
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.local.dao.PaiementDao
import com.example.sencsu.data.remote.ApiService
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.remote.dto.PaiementDto
import com.example.sencsu.data.repository.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val adherentDao: AdherentDao,
    private val paiementDao: PaiementDao,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    suspend fun syncAllData() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("SyncManager", "Starting sync process...")
                // 1. Get current agent/user info
                val agentId = sessionManager.userFlow.firstOrNull()?.id ?: return@withContext
                
                // 2. Sync Adherents
                val unsyncedAdherents = adherentDao.getUnsyncedAdherents()
                Log.d("SyncManager", "Found ${unsyncedAdherents.size} unsynced adherents")
                for (localAdherent in unsyncedAdherents) {
                    try {
                        // Map entity to Dto
                        val dto = AdherentDto(
                            prenoms = localAdherent.prenoms,
                            nom = localAdherent.nom,
                            adresse = localAdherent.adresse,
                            lieuNaissance = localAdherent.lieuNaissance,
                            sexe = localAdherent.sexe,
                            numeroCarte = localAdherent.numeroCarte,
                            dateNaissance = localAdherent.dateNaissance,
                            situationM = localAdherent.situationM,
                            whatsapp = localAdherent.whatsapp,
                            secteurActivite = localAdherent.secteurActivite,
                            typePiece = localAdherent.typePiece,
                            numeroPiece = localAdherent.numeroPiece,
                            numeroCNi = localAdherent.numeroCNi,
                            departement = localAdherent.departement,
                            commune = localAdherent.commune,
                            region = localAdherent.region,
                            typeAdhesion = localAdherent.typeAdhesion,
                            montantTotal = localAdherent.montantTotal,
                            regime = localAdherent.regime,
                            matricule = localAdherent.matricule,
                            photo = localAdherent.photo,
                            photoRecto = localAdherent.photoRecto,
                            photoVerso = localAdherent.photoVerso,
                            actif = localAdherent.actif,
                            clientUUID = localAdherent.localUuid
                        )
                        
                        val response = apiService.createAdherent(agentId, dto)
                        if (response.success) {
                            val remoteId = response.data.adherentId
                            adherentDao.markAsSynced(localAdherent.localId, remoteId)
                            Log.d("SyncManager", "Synced adherent ${localAdherent.localId} with remoteId \$remoteId")
                        }
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Failed to sync adherent ${localAdherent.localId}", e)
                    }
                }
                
                // 3. Sync Paiements
                val unsyncedPaiements = paiementDao.getUnsyncedPaiements()
                Log.d("SyncManager", "Found ${unsyncedPaiements.size} unsynced payments")
                for (localPaiement in unsyncedPaiements) {
                    try {
                        var remoteAdherentId = localPaiement.adherentId
                        
                        // If we didn't have the remote adherentId yet, try to find it now 
                        // from the localAdherentId we synced in step 2
                        if (remoteAdherentId == null && localPaiement.localAdherentId != null) {
                            val updatedAdherent = adherentDao.getAdherentById(localPaiement.localAdherentId)
                            if (updatedAdherent?.isSynced == true && updatedAdherent.id != null) {
                                remoteAdherentId = updatedAdherent.id
                            }
                        }
                        
                        if (remoteAdherentId != null) {
                            val dto = PaiementDto(
                                reference = localPaiement.reference,
                                montant = localPaiement.montant,
                                modePaiement = localPaiement.modePaiement,
                                photoPaiement = localPaiement.photoPaiement,
                                adherentId = remoteAdherentId,
                                photos = listOfNotNull(localPaiement.photoPaiement),
                                datePaiement = localPaiement.datePaiement
                            )
                            
                            val response = apiService.addPaiement(dto)
                            if (response.isSuccessful) {
                                // Assume remote ID isn't returned for payments based on ApiService
                                // We just mark it as synced. Use localId as remoteId dummy since addPaiement returns Unit
                                paiementDao.markAsSynced(localPaiement.localId, localPaiement.localId)
                                Log.d("SyncManager", "Synced payment ${localPaiement.localId}")
                            } else {
                                Log.e("SyncManager", "Failed API response for payment ${localPaiement.localId}: ${response.code()}")
                            }
                        } else {
                            Log.w("SyncManager", "Skipping payment ${localPaiement.localId} - missing adherentId")
                        }
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Failed to sync payment ${localPaiement.localId}", e)
                    }
                }
                Log.d("SyncManager", "Sync process finished.")
            } catch (e: Exception) {
                Log.e("SyncManager", "Sync process encountered error", e)
            }
        }
    }
}
