package com.example.pullit.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class XiaohongshuContent(
    val videoUrl: String? = null,
    val title: String,
    val description: String,
    val coverUrl: String? = null,
    val noteId: String,
    val fullUrl: String,
    val isTextNote: Boolean,
    val useBackendDirectly: Boolean
)

@Serializable
data class FetchXiaohongshuRequest(val url: String)

@Serializable
data class FetchXiaohongshuResponse(
    val html: String? = null,
    @SerialName("resolved_url") val resolvedUrl: String? = null
)

object XiaohongshuService {

    fun isXiaohongshuUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("xiaohongshu.com") || lower.contains("xhslink.com") || lower.contains("rednote")
    }

    fun extractUrlFromText(text: String): String? {
        val patterns = listOf(
            "https?://xhslink\\.com/[^\\s]+",
            "https?://www\\.xiaohongshu\\.com/[^\\s]+",
            "https?://xiaohongshu\\.com/[^\\s]+"
        )
        for (pattern in patterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            if (match != null) {
                return match.value.replace(Regex("[,，。！!？?；;）)】\\]]+$"), "")
            }
        }
        return null
    }

    suspend fun fetchContent(inputText: String): XiaohongshuContent {
        var url = inputText.trim()

        if (url.contains(" ") || Regex("[\\u4e00-\\u9fa5]").containsMatchIn(url)) {
            url = extractUrlFromText(url) ?: throw ApiError.Unknown("Cannot extract valid URL from text")
        }

        if (url.contains("xhslink.com")) {
            var fallbackTitle = ""
            val originalText = inputText.trim()
            if (originalText.contains(" ") || Regex("[\\u4e00-\\u9fa5]").containsMatchIn(originalText)) {
                val cleaned = originalText
                    .replace(Regex("https?://[^\\s]+"), "")
                    .replace(Regex("复制打开小红书.*$"), "")
                    .trim()
                val lines = cleaned.split("\n").filter { it.isNotBlank() }
                fallbackTitle = lines.firstOrNull()?.trim() ?: ""
            }
            return XiaohongshuContent(
                videoUrl = url, title = fallbackTitle, description = "",
                noteId = "", fullUrl = url, isTextNote = false, useBackendDirectly = true
            )
        }

        val noteId = extractNoteId(url)
        var fullUrl = url
        if (!fullUrl.contains("xiaohongshu.com") && noteId != null) {
            fullUrl = "https://www.xiaohongshu.com/explore/$noteId"
        }

        val response: FetchXiaohongshuResponse = ApiClient.postTyped("/api/fetch-xiaohongshu", FetchXiaohongshuRequest(fullUrl))
        val html = response.html ?: throw ApiError.Unknown("No HTML content received")
        if (response.resolvedUrl != null) fullUrl = response.resolvedUrl

        val videoUrl = extractVideoUrl(html)
        val title = extractTitle(html)
        val description = extractDescription(html)
        val coverUrl = extractCoverImage(html)

        return XiaohongshuContent(
            videoUrl = videoUrl, title = title, description = description,
            coverUrl = coverUrl, noteId = noteId ?: "", fullUrl = fullUrl,
            isTextNote = videoUrl == null, useBackendDirectly = false
        )
    }

    private fun extractNoteId(url: String): String? {
        val patterns = listOf("/explore/([a-zA-Z0-9]+)", "/discovery/item/([a-zA-Z0-9]+)", "/([a-zA-Z0-9]{24})")
        for (pattern in patterns) {
            val match = Regex(pattern).find(url)
            if (match != null && match.groupValues.size > 1) return match.groupValues[1]
        }
        return null
    }

    private fun extractVideoUrl(html: String): String? {
        val patterns = listOf(
            "(https?://sns-video-[a-z]+\\.xhscdn\\.com/[^\\s\"'<>]*\\.mp4[^\\s\"'<>]*)",
            "(https?://[^\\s\"'<>]*xhscdn\\.com[^\\s\"'<>]*\\.mp4[^\\s\"'<>]*)",
            "(https?://[^\\s\"'<>]+\\.mp4[^\\s\"'<>]*)"
        )
        for (pattern in patterns) {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).find(html)
            if (match != null) return match.groupValues[1].replace("&amp;", "&")
        }
        return null
    }

    private fun extractTitle(html: String): String {
        val m1 = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(html)
        if (m1 != null) {
            val title = m1.groupValues[1]
            if (!title.contains("小红书") && title.length > 2) return title.trim()
        }
        val m2 = Regex("<meta[^>]*property=\"og:title\"[^>]*content=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(html)
        if (m2 != null) return m2.groupValues[1].replace(Regex("\\s*-\\s*小红书.*$"), "")
        return "Untitled"
    }

    private fun extractDescription(html: String): String {
        val patterns = listOf(
            "\"desc\"\\s*:\\s*\"([^\"]+)\"",
            "\"description\"\\s*:\\s*\"([^\"]+)\"",
            "<meta[^>]*property=\"og:description\"[^>]*content=\"([^\"]+)\""
        )
        for (pattern in patterns) {
            val m = Regex(pattern, RegexOption.IGNORE_CASE).find(html)
            if (m != null) {
                val desc = m.groupValues[1]
                if (desc.length > 5) return desc.trim()
            }
        }
        return ""
    }

    private fun cleanXhsImageUrl(url: String): String {
        return url
            .replace(Regex("![a-z_]+\\d*$"), "")
            .replace(Regex("\\?imageView2.*$"), "")
            .replace(Regex("\\?x-oss-process=.*$"), "")
            .replace(Regex("^http://"), "https://")
    }

    private fun extractCoverImage(html: String): String? {
        val m1 = Regex("\"(?:imageList|images|coverImages)\"\\s*:\\s*\\[([^\\]]+)]", RegexOption.IGNORE_CASE).find(html)
        if (m1 != null) {
            val listContent = m1.groupValues[1]
            val m2 = Regex("\"(https?:[^\"]+(?:xhscdn|xiaohongshu)[^\"]+)\"", RegexOption.IGNORE_CASE).find(listContent)
            if (m2 != null) {
                return cleanXhsImageUrl(m2.groupValues[1].replace("\\/", "/").replace("\\u002F", "/"))
            }
        }

        val coverPatterns = listOf(
            "\"cover\"\\s*:\\s*\"(https?:[^\"]+)\"", "\"coverUrl\"\\s*:\\s*\"(https?:[^\"]+)\"",
            "\"coverImage\"\\s*:\\s*\"(https?:[^\"]+)\"", "\"firstNoteImage\"\\s*:\\s*\"(https?:[^\"]+)\""
        )
        for (p in coverPatterns) {
            val m = Regex(p, RegexOption.IGNORE_CASE).find(html)
            if (m != null) return cleanXhsImageUrl(m.groupValues[1].replace("\\/", "/").replace("\\u002F", "/"))
        }

        val m3 = Regex("(https?://[^\\s\"'<>]*xhscdn\\.com[^\\s\"'<>]*\\.(?:jpg|jpeg|png|webp)[^\\s\"'<>]*)", RegexOption.IGNORE_CASE).find(html)
        if (m3 != null) return cleanXhsImageUrl(m3.groupValues[1].replace("&amp;", "&"))

        val m4 = Regex("<meta[^>]*property=\"og:image\"[^>]*content=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(html)
        if (m4 != null) return cleanXhsImageUrl(m4.groupValues[1])

        return null
    }
}
