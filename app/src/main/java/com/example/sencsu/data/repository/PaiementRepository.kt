package com.example.sencsu.data.repository

import android.util.Log
import com.example.sencsu.data.remote.ApiService
import com.example.sencsu.data.remote.dto.ApiResponse
import com.example.sencsu.data.remote.dto.ErrorResponse
import com.example.sencsu.data.remote.dto.PaiementDto
import com.example.sencsu.data.remote.dto.ValidationException
import retrofit2.HttpException
import javax.inject.Inject
import kotlinx.serialization.json.Json

class PaiementRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getPaiementsByAdherentId(adherentId: Long): List<PaiementDto> {
        return apiService.getPaiementsByAdherentId(adherentId)
    }

    suspend fun addPaiement(paiement: PaiementDto): Result<Unit> {
        return try {
            val response = apiService.addPaiement(paiement)

            // Vérifier si la réponse est succès (2xx)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                // Gérer les erreurs (4xx, 5xx)
                val errorMessage = parseErrorResponse(response.code(), response.errorBody()?.string())

                if (response.code() == 400) {
                    Result.failure(ValidationException(errorMessage))
                } else {
                    Result.failure(Exception("Erreur ${response.code()}: $errorMessage"))
                }
            }
        } catch (e: HttpException) {
            // Gérer les exceptions HTTP (connexion, timeout, etc.)
            val errorMessage = parseHttpException(e)
            Log.e("PaiementRepository", "HttpException: $errorMessage", e)

            if (e.code() == 400) {
                Result.failure(ValidationException(errorMessage))
            } else {
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            // Autres exceptions (réseau, sérialisation, etc.)
            Log.e("PaiementRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse la réponse d'erreur du serveur
     * Supporte les formats JSON avec "message" et "error"
     */
    private fun parseErrorResponse(statusCode: Int, responseBody: String?): String {
        return try {
            if (responseBody.isNullOrEmpty()) {
                return "Erreur serveur (code $statusCode)"
            }

            // Essayer de parser le JSON
            val errorResponse = Json.decodeFromString<ErrorResponse>(responseBody)
            errorResponse.message ?: "Erreur serveur (code $statusCode)"
        } catch (e: Exception) {
            Log.e("PaiementRepository", "Erreur parsing JSON: ${e.message}")
            // Si parsing échoue, retourner le body brut ou un message générique
            responseBody?.takeIf { it.isNotEmpty() }
                ?: "Erreur serveur (code $statusCode)"
        }
    }

    /**
     * Parse les exceptions HTTP Retrofit
     */
    private fun parseHttpException(exception: HttpException): String {
        return try {
            val errorBody = exception.response()?.errorBody()?.string()

            if (!errorBody.isNullOrEmpty()) {
                val errorResponse = Json.decodeFromString<ErrorResponse>(errorBody)
                errorResponse.message ?: "Erreur ${exception.code()}"
            } else {
                "Erreur ${exception.code()}: ${exception.message()}"
            }
        } catch (e: Exception) {
            Log.e("PaiementRepository", "Erreur parsing HttpException: ${e.message}")
            "Erreur ${exception.code()}: ${exception.message()}"
        }
    }
}