package com.example.pullit.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppAppearance(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark")
}

enum class AppLanguage(val key: String) {
    SYSTEM("system"), CHINESE("zh"), ENGLISH("en")
}

class AppSettings private constructor(private val prefs: SharedPreferences) {

    private val _appearance = MutableStateFlow(
        AppAppearance.entries.find { it.key == prefs.getString("appAppearance", "system") }
            ?: AppAppearance.SYSTEM
    )
    val appearance: StateFlow<AppAppearance> = _appearance.asStateFlow()

    private val _language = MutableStateFlow(
        AppLanguage.entries.find { it.key == prefs.getString("appLanguage", "system") }
            ?: AppLanguage.SYSTEM
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _autoDetectClipboard = MutableStateFlow(
        prefs.getBoolean("autoDetectClipboard", true)
    )
    val autoDetectClipboard: StateFlow<Boolean> = _autoDetectClipboard.asStateFlow()

    fun setAppearance(value: AppAppearance) {
        prefs.edit().putString("appAppearance", value.key).apply()
        _appearance.value = value
    }

    fun setLanguage(value: AppLanguage) {
        prefs.edit().putString("appLanguage", value.key).apply()
        _language.value = value
    }

    fun setAutoDetectClipboard(value: Boolean) {
        prefs.edit().putBoolean("autoDetectClipboard", value).apply()
        _autoDetectClipboard.value = value
    }

    companion object {
        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(
                    context.applicationContext.getSharedPreferences("pullit_settings", Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
