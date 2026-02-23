package com.example.sencsu.data.repository

import com.example.sencsu.data.remote.ApiService
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.remote.dto.PersonneChargeDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdherentRepository @Inject constructor(
    private val apiService: ApiService
) {




    suspend fun getAdherentById(id: Long): Result<AdherentDto> {
        return try {
            val response = apiService.getAdherentById(id)
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun deleteAdherent(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteAdherent(id)
                Result.success(Unit)
            } catch (e: IOException) {
                // Erreur réseau (pas d'internet)
                Result.failure(Exception("Problème de connexion internet"))
            } catch (e: HttpException) {
                // Erreur API (404, 500, etc.)
                Result.failure(Exception("Erreur serveur : ${e.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    suspend fun addPersonneCharge(adherentId: Long, personne: PersonneChargeDto): Result<PersonneChargeDto> {
        return withContext(Dispatchers.IO) {
            try {
                val result = apiService.addPersonneCharge(adherentId, personne)
                Result.success(result)
            } catch (e: HttpException) {
                Result.failure(Exception("Erreur HTTP: ${e.code()} - ${e.message}"))
            } catch (e: IOException) {
                Result.failure(Exception("Erreur de connexion: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("Erreur inattendue: ${e.message}"))
            }
        }
    }

    suspend fun deletePersonneCharge(adherentId: Long, pcId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deletePersonneCharge(adherentId, pcId)
                Result.success(Unit)
            } catch (e: HttpException) {
                Result.failure(Exception("Erreur HTTP: ${e.code()} - ${e.message}"))
            } catch (e: IOException) {
                Result.failure(Exception("Erreur de connexion: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("Erreur inattendue: ${e.message}"))
            }
        }
    }
}
