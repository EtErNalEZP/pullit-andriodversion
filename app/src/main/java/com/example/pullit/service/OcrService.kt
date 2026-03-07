package com.example.pullit.service

import android.util.Base64
import com.example.pullit.data.model.OCRResult
import com.example.pullit.data.model.ParsedRecipeResponse
import com.example.pullit.util.AppLanguage
import kotlinx.serialization.Serializable

@Serializable
private data class OCRRequest(val image: String, val language: String)

@Serializable
private data class ImageRequest(val image: String, val language: String)

object OcrService {
    suspend fun extractText(imageData: ByteArray): String {
        val base64 = Base64.encodeToString(imageData, Base64.NO_WRAP)
        val result: OCRResult = ApiClient.postTyped("/api/ocr", OCRRequest(image = base64, language = AppLanguage.resolved))
        return result.text ?: ""
    }

    suspend fun extractRecipe(imageData: ByteArray): ParsedRecipeResponse {
        val base64 = Base64.encodeToString(imageData, Base64.NO_WRAP)
        return ApiClient.postTyped("/api/image-to-recipe", ImageRequest(image = base64, language = AppLanguage.resolved))
    }
}
