package com.example.sencsu.di

import android.app.Application
import androidx.room.Room
import com.example.sencsu.data.local.AppDatabase
import com.example.sencsu.data.local.dao.AdherentDao
import com.example.sencsu.data.local.dao.PaiementDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "sencsu_db"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    @Singleton
    fun provideAdherentDao(db: AppDatabase): AdherentDao {
        return db.adherentDao
    }

    @Provides
    @Singleton
    fun providePaiementDao(db: AppDatabase): PaiementDao {
        return db.paiementDao
    }
}
