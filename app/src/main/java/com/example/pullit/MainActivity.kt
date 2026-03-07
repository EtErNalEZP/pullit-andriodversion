package com.example.pullit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.example.pullit.auth.AuthManager
import com.example.pullit.ui.navigation.AppNavigation
import com.example.pullit.ui.theme.PullitTheme

class MainActivity : ComponentActivity() {
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PullitTheme {
                LaunchedEffect(Unit) {
                    authManager.initialize()
                }
                AppNavigation(authManager = authManager)
            }
        }
    }
}
