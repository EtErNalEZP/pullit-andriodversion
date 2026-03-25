package com.example.pullit.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pullit.auth.AuthManager
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.Primary
import com.example.pullit.ui.theme.PrimaryLight
import com.example.pullit.ui.theme.Success
import com.example.pullit.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit,
    onNeedsDisplayName: () -> Unit
) {
    val S = LocalStrings.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetSent by remember { mutableStateOf(false) }
    var emailSignUpSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val needsDisplayName by authManager.needsDisplayName.collectAsState()
    val isAuthenticated by authManager.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated, needsDisplayName) {
        if (isAuthenticated) {
            if (needsDisplayName) onNeedsDisplayName() else onAuthSuccess()
        }
    }

    // Password reset dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(S.resetPasswordTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (resetSent) {
                        Text(S.resetSentMessage, color = Success)
                    } else {
                        Text(S.resetPasswordMessage)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text(S.email) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }
                }
            },
            confirmButton = {
                if (!resetSent) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                authManager.resetPassword(resetEmail.trim())
                                resetSent = true
                            } catch (e: Exception) {
                                errorMessage = friendlyAuthError(e, S)
                            }
                        }
                    }) { Text(S.sendResetLink, color = Primary, fontWeight = FontWeight.SemiBold) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false; resetSent = false }) {
                    Text(S.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (emailSignUpSuccess) {
        // Email confirmation screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.MarkEmailRead,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                S.checkYourEmail,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                S.verificationSent,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = {
                emailSignUpSuccess = false
                isSignUp = false
            }) {
                Text(S.backToLogin, color = Primary, fontSize = 14.sp)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = com.example.pullit.R.drawable.logo),
                contentDescription = S.pullitRecipes,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                S.pullitRecipes,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                S.yourRecipeCompanion,
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(S.email) },
                placeholder = { Text(S.emailPlaceholder) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(S.password) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true; errorMessage = null
                        try {
                            if (isSignUp) {
                                val needsConfirmation = authManager.signUp(email.trim(), password)
                                if (needsConfirmation) emailSignUpSuccess = true
                            } else {
                                authManager.signIn(email.trim(), password)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AuthScreen", "Auth error: ${e::class.simpleName}: ${e.message}", e)
                            errorMessage = friendlyAuthError(e, S)
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = PrimaryLight
                )
            ) {
                if (isLoading) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                else Text(
                    if (isSignUp) S.signUp else S.signIn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { isSignUp = !isSignUp; errorMessage = null }) {
                Text(
                    if (isSignUp) S.alreadyHaveAccount
                    else S.dontHaveAccount,
                    color = Primary,
                    fontSize = 14.sp
                )
            }

            // Forgot password link (sign-in mode only)
            if (!isSignUp) {
                TextButton(onClick = {
                    resetEmail = email
                    resetSent = false
                    showResetDialog = true
                }) {
                    Text(S.forgotPassword, color = Primary, fontSize = 14.sp)
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}

private fun friendlyAuthError(e: Exception, S: com.example.pullit.ui.AppStrings): String {
    val msg = (e.message ?: "").lowercase()
    return when {
        "invalid login credentials" in msg || "invalid credential" in msg -> S.authErrorInvalidCredentials
        "user already registered" in msg -> S.authErrorUserAlreadyRegistered
        "email not confirmed" in msg -> S.authErrorEmailNotConfirmed
        "unable to validate email" in msg || "invalid email" in msg -> S.authErrorInvalidEmail
        "password" in msg && ("weak" in msg || "short" in msg || "at least" in msg) -> S.authErrorWeakPassword
        "rate limit" in msg || "too many requests" in msg || "request this after" in msg -> S.authErrorRateLimited
        e is java.net.UnknownHostException || e is java.net.ConnectException
            || "network" in msg || "unable to resolve host" in msg || "connection" in msg -> S.authErrorNetwork
        else -> S.authErrorUnknown
    }
}
