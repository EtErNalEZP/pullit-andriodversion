package com.example.pullit.auth

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    val display_name: String? = null
)

class AuthManager {
    private val supabase = SupabaseManager.client

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _needsDisplayName = MutableStateFlow(false)
    val needsDisplayName: StateFlow<Boolean> = _needsDisplayName.asStateFlow()

    suspend fun initialize() {
        try {
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                _userId.value = session.user?.id
                _userEmail.value = session.user?.email
                _isAuthenticated.value = true
                fetchProfile()
            }
        } catch (_: Exception) {
            _isAuthenticated.value = false
        }
        _isLoading.value = false
    }

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // Auto sign-in after sign-up (if email confirmation is disabled)
        val session = supabase.auth.currentSessionOrNull()
        if (session != null) {
            _userId.value = session.user?.id
            _userEmail.value = session.user?.email
            _isAuthenticated.value = true
            fetchProfile()
        }
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val session = supabase.auth.currentSessionOrNull()
        _userId.value = session?.user?.id
        _userEmail.value = session?.user?.email
        _isAuthenticated.value = true
        fetchProfile()
    }

    suspend fun updateDisplayName(name: String) {
        val uid = _userId.value ?: return
        val trimmed = name.trim()
        if (trimmed.length < 2 || trimmed.length > 20) return

        supabase.postgrest.from("profiles")
            .update(mapOf("display_name" to trimmed)) {
                filter { eq("id", uid) }
            }

        _displayName.value = trimmed
        _needsDisplayName.value = false
    }

    suspend fun signOut() {
        supabase.auth.signOut()
        _userId.value = null
        _userEmail.value = null
        _displayName.value = null
        _isAuthenticated.value = false
        _needsDisplayName.value = false
    }

    private suspend fun fetchProfile() {
        val uid = _userId.value ?: return
        try {
            val result = supabase.postgrest.from("profiles")
                .select { filter { eq("id", uid) } }
                .decodeList<ProfileRow>()
            val name = result.firstOrNull()?.display_name
            if (!name.isNullOrEmpty() && name != "User") {
                _displayName.value = name
                _needsDisplayName.value = false
            } else {
                _needsDisplayName.value = true
            }
        } catch (_: Exception) {
            _needsDisplayName.value = _displayName.value.isNullOrEmpty()
        }
    }
}
