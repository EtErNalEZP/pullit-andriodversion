package com.example.pullit.util

import java.util.Locale

object AppLanguage {
    var storedLanguage: String = "system"

    val resolved: String
        get() {
            return when (storedLanguage) {
                "zh" -> "zh"
                "en" -> "en"
                else -> {
                    val lang = Locale.getDefault().language
                    if (lang.startsWith("zh")) "zh" else "en"
                }
            }
        }
}
