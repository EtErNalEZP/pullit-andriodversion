package com.example.pullit.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloadService {
    suspend fun downloadAndCompress(urlString: String, maxBytes: Int = 1024 * 1024): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                try {
                    if (conn.responseCode !in 200..299) return@withContext null
                    val data = conn.inputStream.readBytes()
                    compress(data, maxBytes)
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun compress(data: ByteArray, maxBytes: Int): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return data
        var quality = 80
        var compressed: ByteArray
        do {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            compressed = stream.toByteArray()
            quality -= 10
        } while (compressed.size > maxBytes && quality > 10)
        return compressed
    }
}
