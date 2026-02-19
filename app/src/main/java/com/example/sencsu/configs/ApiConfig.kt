package com.example.sencsu.configs

object ApiConfig {
//   const val BASE_URL = "http://192.168.1.80:8080"
    const val BASE_URL = "http://192.168.1.129:8080/"

    private const val FILES_ENDPOINT = "api/files/"
    const val IMAGE_BASE_URL = "$BASE_URL$FILES_ENDPOINT"

    fun getImageUrl(filename: String?): String? {
        if (filename.isNullOrBlank()) {
            return null
        }
        return IMAGE_BASE_URL + filename
    }
}
