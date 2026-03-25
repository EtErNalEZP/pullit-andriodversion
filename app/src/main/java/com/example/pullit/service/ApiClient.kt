package com.example.pullit.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed class ApiError : Exception() {
    data object InvalidURL : ApiError()
    data object InvalidResponse : ApiError()
    data class HttpError(val code: Int, val msg: String?) : ApiError()
    data class DecodingError(override val cause: Throwable) : ApiError()
    data class NetworkError(override val cause: Throwable) : ApiError()
    data object Timeout : ApiError()
    data class Unknown(override val message: String) : ApiError()
}

object ApiClient {
    @PublishedApi internal val BASE_URL = com.example.pullit.BuildConfig.API_BASE_URL
    @PublishedApi internal const val API_KEY = "a3f7b2e9c4d8f1a6e5b3c7d9f2a4e8b6c1d5f9a2e7b4c8d3f6a1e9b5c2d7f4a8"
    @PublishedApi internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    suspend inline fun <reified T> get(path: String): T {
        return request("GET", path, null)
    }

    suspend inline fun <reified T> post(path: String, body: Any? = null): T {
        val bodyJson = when (body) {
            null -> null
            is String -> body
            else -> body.toString()
        }
        return request("POST", path, bodyJson)
    }

    suspend inline fun <reified B, reified T> postTyped(path: String, body: B): T {
        val bodyJson = json.encodeToString(body)
        return request("POST", path, bodyJson)
    }

    suspend inline fun <reified T> request(method: String, path: String, body: String?): T {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("$BASE_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-API-Key", API_KEY)
                conn.connectTimeout = 30000
                conn.readTimeout = 30000

                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                    throw ApiError.HttpError(code, errorBody)
                }

                val responseBody = conn.inputStream.bufferedReader().readText()
                json.decodeFromString<T>(responseBody)
            } catch (e: ApiError) {
                throw e
            } catch (e: IOException) {
                throw ApiError.NetworkError(e)
            } catch (e: Exception) {
                throw ApiError.DecodingError(e)
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun delete(path: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("$BASE_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("X-API-Key", API_KEY)
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                val code = conn.responseCode
                if (code !in 200..299) {
                    val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                    throw ApiError.HttpError(code, errorBody)
                }
            } catch (e: ApiError) { throw e }
            catch (e: IOException) { throw ApiError.NetworkError(e) }
            catch (e: Exception) { throw ApiError.Unknown(e.message ?: "Unknown error") }
            finally { conn.disconnect() }
        }
    }

    suspend fun postRaw(path: String, body: String?): ByteArray {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("$BASE_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-API-Key", API_KEY)
                conn.connectTimeout = 30000
                conn.readTimeout = 30000

                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                    throw ApiError.HttpError(code, errorBody)
                }

                conn.inputStream.readBytes()
            } finally {
                conn.disconnect()
            }
        }
    }
}
