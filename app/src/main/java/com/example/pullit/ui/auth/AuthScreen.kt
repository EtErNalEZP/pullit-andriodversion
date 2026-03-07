package com.example.pullit.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pullit.auth.AuthManager
import com.example.pullit.ui.theme.Primary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit,
    onNeedsDisplayName: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val needsDisplayName by authManager.needsDisplayName.collectAsState()
    val isAuthenticated by authManager.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated, needsDisplayName) {
        if (isAuthenticated) {
            if (needsDisplayName) onNeedsDisplayName() else onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pullit Recipes", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Your recipe companion", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("you@example.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true; errorMessage = null
                    try {
                        if (isSignUp) {
                            authManager.signUp(email.trim(), password)
                        } else {
                            authManager.signIn(email.trim(), password)
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Authentication failed"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (isSignUp) "Sign Up" else "Sign In", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { isSignUp = !isSignUp; errorMessage = null }) {
            Text(
                if (isSignUp) "Already have an account? Sign In"
                else "Don't have an account? Sign Up"
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
