package com.example.pullit.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.pullit.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.auth.AuthManager
import com.example.pullit.data.AppAppearance
import com.example.pullit.data.AppLanguage
import com.example.pullit.data.AppSettings
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.RecipeListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    authManager: AuthManager,
    navController: NavController,
    viewModel: RecipeListViewModel = viewModel()
) {
    val S = LocalStrings.current
    val context = LocalContext.current
    val settings = remember { AppSettings.getInstance(context) }
    val displayName by authManager.displayName.collectAsState()
    val userEmail by authManager.userEmail.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val cookbooks by viewModel.cookbooks.collectAsState()
    val scope = rememberCoroutineScope()

    val appearance by settings.appearance.collectAsState()
    val language by settings.language.collectAsState()
    val autoDetectClipboard by settings.autoDetectClipboard.collectAsState()
    val autoCookbookRecommend by settings.autoCookbookRecommend.collectAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showDisplayNameDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    val recipeCount = recipes.size
    val cookbookCount = cookbooks.size
    val favoriteCount = recipes.count { it.favorited }

    // ── Dialogs ──

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(S.signOutConfirm, fontWeight = FontWeight.Bold) },
            text = { Text(S.signOutMessage) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { authManager.signOut() }
                    showSignOutDialog = false
                }) { Text(S.signOut, color = Error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(S.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            title = { Text(S.deleteAccountConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(S.deleteAccountConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletingAccount = true
                        scope.launch {
                            val db = AppDatabase.getInstance(context)
                            try { authManager.deleteAccount(db) } catch (_: Exception) {}
                            isDeletingAccount = false
                            showDeleteAccountDialog = false
                        }
                    },
                    enabled = !isDeletingAccount
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(S.deleteAccount, color = Error, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount
                ) { Text(S.cancel, color = TextSecondary) }
            }
        )
    }

    if (showLanguageDialog) {
        // Language names should stay in their own language for recognition
        val options = listOf(
            AppLanguage.SYSTEM to S.followSystem,
            AppLanguage.CHINESE to "中文",
            AppLanguage.ENGLISH to "English"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(S.language, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { (lang, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == lang,
                                onClick = {
                                    settings.setLanguage(lang)
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showAppearanceDialog) {
        val options = listOf(
            AppAppearance.SYSTEM to S.followSystem,
            AppAppearance.LIGHT to S.lightMode,
            AppAppearance.DARK to S.darkMode
        )
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text(S.appearance, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appearance == mode,
                                onClick = {
                                    settings.setAppearance(mode)
                                    showAppearanceDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showDisplayNameDialog) {
        var nameInput by remember { mutableStateOf(displayName ?: "") }
        var isUpdating by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showDisplayNameDialog = false },
            title = { Text(S.displayName, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text(S.displayNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.length in 2..20) {
                            isUpdating = true
                            scope.launch {
                                authManager.updateDisplayName(trimmed)
                                isUpdating = false
                                showDisplayNameDialog = false
                            }
                        }
                    },
                    enabled = !isUpdating && nameInput.trim().length in 2..20
                ) { Text(S.save, color = Primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisplayNameDialog = false },
                    enabled = !isUpdating
                ) { Text(S.cancel, color = TextSecondary) }
            }
        )
    }

    // ── Content ──

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── App Header Section ──
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    S.pullitRecipes,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    S.personalAssistant,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(value = recipeCount.toString(), label = S.recipes)
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                            StatItem(value = cookbookCount.toString(), label = S.cookbooks)
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                            StatItem(value = favoriteCount.toString(), label = S.favorites)
                        }
                    }
                }
            }

            // ── General Section ──
            item { SectionHeader(S.general) }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.Language,
                            title = S.language,
                            onClick = { showLanguageDialog = true },
                            trailing = {
                                Text(
                                    when (language) {
                                        AppLanguage.SYSTEM -> S.followSystem
                                        AppLanguage.CHINESE -> "中文"
                                        AppLanguage.ENGLISH -> "English"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.DarkMode,
                            title = S.appearance,
                            onClick = { showAppearanceDialog = true },
                            trailing = {
                                Text(
                                    when (appearance) {
                                        AppAppearance.SYSTEM -> S.followSystem
                                        AppAppearance.LIGHT -> S.lightMode
                                        AppAppearance.DARK -> S.darkMode
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.ContentPaste,
                            title = S.autoDetectClipboard,
                            onClick = { settings.setAutoDetectClipboard(!autoDetectClipboard) },
                            showChevron = false,
                            trailing = {
                                Switch(
                                    checked = autoDetectClipboard,
                                    onCheckedChange = { settings.setAutoDetectClipboard(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Primary),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.AutoAwesome,
                            title = S.autoCookbookRecommend,
                            onClick = { settings.setAutoCookbookRecommend(!autoCookbookRecommend) },
                            showChevron = false,
                            trailing = {
                                Switch(
                                    checked = autoCookbookRecommend,
                                    onCheckedChange = { settings.setAutoCookbookRecommend(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Primary),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        )
                    }
                }
            }

            // ── Content Section ──
            item { SectionHeader(S.content) }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    SettingsRow(
                        icon = Icons.Outlined.SwapVert,
                        title = S.importExport,
                        onClick = {}
                    )
                }
            }

            // ── Support Section ──
            item { SectionHeader(S.support) }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.PrivacyTip,
                            title = S.privacyInfo,
                            onClick = { navController.navigate(Screen.PrivacyInfo.route) }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Description,
                            title = S.privacyPolicy,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://pullit-landing.up.railway.app/privacy.html"))
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Outlined.Send,
                            title = S.feedbackGroup,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+ppV812MEhoJiOWU8"))
                                )
                            },
                            trailing = {
                                Text(
                                    "Telegram",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Star,
                            title = S.rateUs,
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                    )
                                }.onFailure {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                    )
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Info,
                            title = S.aboutPullit,
                            onClick = {}
                        )
                    }
                }
            }

            // ── Account Section ──
            item { SectionHeader(S.account) }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        if (userEmail != null) {
                            SettingsRow(
                                icon = Icons.Outlined.Email,
                                title = S.email,
                                onClick = {},
                                showChevron = false,
                                trailing = {
                                    Text(
                                        userEmail!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            )
                            SettingsDivider()
                        }
                        SettingsRow(
                            icon = Icons.Outlined.Person,
                            title = S.displayName,
                            onClick = { showDisplayNameDialog = true },
                            trailing = {
                                Text(
                                    displayName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            title = S.signOut,
                            onClick = { showSignOutDialog = true },
                            titleColor = Error,
                            iconTint = Error,
                            showChevron = false
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Delete,
                            title = S.deleteAccount,
                            onClick = { showDeleteAccountDialog = true },
                            titleColor = Error,
                            iconTint = Error,
                            showChevron = false
                        )
                    }
                }
            }

            // ── Version Footer ──
            item {
                Text(
                    S.version,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Helper composables ──

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = Primary,
    showChevron: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (trailing != null) {
                trailing()
                if (showChevron) Spacer(modifier = Modifier.width(8.dp))
            }
            if (showChevron) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
