package com.example.pullit.ui.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.auth.AuthManager
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.RecipeListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    authManager: AuthManager,
    navController: NavController,
    viewModel: RecipeListViewModel = viewModel()
) {
    val displayName by authManager.displayName.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val scope = rememberCoroutineScope()
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("You can sign back in anytime.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { authManager.signOut() }
                    showSignOutDialog = false
                }) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("More", fontWeight = FontWeight.ExtraBold) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = Primary.copy(alpha = 0.15f), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(displayName?.firstOrNull()?.uppercase() ?: "?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(displayName ?: "User", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${recipes.size} recipes", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Stats
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Recipes", recipes.size.toString())
                        StatItem("Favorites", recipes.count { it.favorited }.toString())
                    }
                }
            }

            // Settings
            item { Spacer(Modifier.height(8.dp)); Text("Settings", fontWeight = FontWeight.Bold) }
            item { SettingsItem(Icons.Outlined.Language, "Language", "System") {} }
            item { SettingsItem(Icons.Outlined.DarkMode, "Appearance", "System") {} }

            // Sign out
            item { Spacer(Modifier.height(8.dp)) }
            item {
                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Sign Out") }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("Pullit Recipes v1.0", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
