package com.example.pullit.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pullit.auth.AuthManager
import com.example.pullit.ui.theme.Primary
import kotlinx.coroutines.launch

@Composable
fun DisplayNameSetupScreen(authManager: AuthManager, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Set your name", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("How should we call you?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 20) name = it },
            label = { Text("Display name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("${name.length}/20") }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true; errorMessage = null
                    try {
                        authManager.updateDisplayName(name.trim())
                        onComplete()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = name.trim().length >= 2 && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Continue", fontWeight = FontWeight.Bold)
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
