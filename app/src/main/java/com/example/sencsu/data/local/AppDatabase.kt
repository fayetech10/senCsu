package com.example.sencsu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.local.dao.PaiementDao
import com.example.sencsu.data.local.entity.AdherentEntity
import com.example.sencsu.data.local.entity.PaiementEntity

@Database(
    entities = [AdherentEntity::class, PaiementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val adherentDao: AdherentDao
    abstract val paiementDao: PaiementDao
}
