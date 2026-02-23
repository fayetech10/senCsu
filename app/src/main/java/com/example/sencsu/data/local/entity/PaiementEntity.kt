package com.example.sencsu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paiements")
data class PaiementEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    
    val id: Long? = null, // Remote ID
    val reference: String,
    val montant: Double,
    val modePaiement: String,
    val photoPaiement: String? = null,
    val adherentId: Long? = null, // Remote Adherent ID if known
    val localAdherentId: Long? = null, // Reference to local AdherentEntity if not yet synced
    val datePaiement: String? = null,
    
    // Sync metadata
    val isSynced: Boolean = false
)
