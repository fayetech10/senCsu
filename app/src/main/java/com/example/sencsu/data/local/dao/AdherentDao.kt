package com.example.sencsu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sencsu.data.local.entity.AdherentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdherentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdherent(adherent: AdherentEntity): Long

    @Update
    suspend fun updateAdherent(adherent: AdherentEntity)

    @Query("SELECT * FROM adherents")
    fun getAllAdherents(): Flow<List<AdherentEntity>>

    @Query("SELECT * FROM adherents WHERE isSynced = 0")
    suspend fun getUnsyncedAdherents(): List<AdherentEntity>

    @Query("UPDATE adherents SET isSynced = 1, id = :remoteId WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long, remoteId: Long)
    
    @Query("SELECT * FROM adherents WHERE localId = :localId")
    suspend fun getAdherentById(localId: Long): AdherentEntity?
    
    @Query("SELECT COUNT(*) FROM adherents WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM adherents WHERE isSynced = 0")
    fun observeUnsyncedAdherents(): Flow<List<AdherentEntity>>
    
    @Query("DELETE FROM adherents WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("DELETE FROM adherents WHERE id = :remoteId")
    suspend fun deleteByRemoteId(remoteId: Long)

    @Query("DELETE FROM adherents")
    suspend fun clearAll()
}
