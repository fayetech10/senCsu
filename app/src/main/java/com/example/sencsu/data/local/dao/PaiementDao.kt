package com.example.sencsu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sencsu.data.local.entity.PaiementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaiementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaiement(paiement: PaiementEntity): Long

    @Update
    suspend fun updatePaiement(paiement: PaiementEntity)

    @Query("SELECT * FROM paiements")
    fun getAllPaiements(): Flow<List<PaiementEntity>>

    @Query("SELECT * FROM paiements WHERE isSynced = 0")
    suspend fun getUnsyncedPaiements(): List<PaiementEntity>

    @Query("UPDATE paiements SET isSynced = 1, id = :remoteId WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long, remoteId: Long)
    
    @Query("SELECT COUNT(*) FROM paiements WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM paiements WHERE isSynced = 0")
    fun observeUnsyncedPaiements(): Flow<List<PaiementEntity>>
    
    @Query("DELETE FROM paiements")
    suspend fun clearAll()
}
