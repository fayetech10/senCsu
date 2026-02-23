package com.example.sencsu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "adherents")
data class AdherentEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    
    val id: Long? = null, // Remote ID
    val prenoms: String? = null,
    val nom: String? = null,
    val adresse: String? = null,
    val lieuNaissance: String? = null,
    val sexe: String? = null,
    val numeroCarte: String? = null,
    val dateNaissance: String? = null,
    val situationM: String? = null,
    val whatsapp: String? = null,
    val secteurActivite: String? = null,
    val typePiece: String? = null,
    val numeroPiece: String? = null,
    val numeroCNi: String? = null,
    val departement: String? = null,
    val commune: String? = null,
    val region: String? = null,
    val typeAdhesion: String? = null,
    val montantTotal: Double? = null,
    val regime: String? = null,
    val matricule: String? = null,
    val photo: String? = null,
    val photoRecto: String? = null,
    val photoVerso: String? = null,
    val actif: Boolean = true,
    
    // Sync metadata
    val isSynced: Boolean = false,
    val localUuid: String = UUID.randomUUID().toString()
)
