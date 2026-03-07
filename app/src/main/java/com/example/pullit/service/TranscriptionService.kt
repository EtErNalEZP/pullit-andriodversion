package com.example.pullit.service

import com.example.pullit.data.model.TaskCreationResponse
import com.example.pullit.data.model.TaskStatusResponse
import com.example.pullit.data.model.TranscriptionProgress
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class TranscribeRequest(@SerialName("video_url") val videoUrl: String)

object TranscriptionService {
    suspend fun transcribe(
        videoUrl: String,
        onProgress: (TranscriptionProgress) -> Unit
    ): String {
        onProgress(TranscriptionProgress(TranscriptionProgress.Status.CREATING, "Creating transcription task..."))
        val creation: TaskCreationResponse = ApiClient.postTyped("/api/transcribe", TranscribeRequest(videoUrl))
        val taskId = creation.taskId

        val startTime = System.currentTimeMillis()
        var attempts = 0

        while (attempts < 200) {
            if (System.currentTimeMillis() - startTime > 600_000) throw ApiError.Timeout
            if (attempts > 0) delay(3000)
            attempts++

            val status: TaskStatusResponse = ApiClient.get("/api/transcribe/$taskId")
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()

            onProgress(TranscriptionProgress(TranscriptionProgress.Status.POLLING, "Processing (${elapsed}s)...", elapsed, taskId))

            if (status.state == "completed" && status.text != null) {
                onProgress(TranscriptionProgress(TranscriptionProgress.Status.SUCCESS, "Done", elapsed, taskId))
                return status.text
            } else if (status.state == "error") {
                throw ApiError.Unknown(status.error ?: "Transcription failed")
            }
        }
        throw ApiError.Timeout
    }
}
