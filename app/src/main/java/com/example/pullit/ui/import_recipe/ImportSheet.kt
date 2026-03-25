package com.example.pullit.ui.import_recipe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.pullit.R
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.ImportErrorType
import com.example.pullit.viewmodel.ImportMethod
import com.example.pullit.viewmodel.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(
    importViewModel: ImportViewModel,
    onDismiss: () -> Unit,
    onStartGenerating: () -> Unit
) {
    val S = LocalStrings.current
    val inputText by importViewModel.inputText.collectAsState()
    val importMethod by importViewModel.importMethod.collectAsState()
    val isGenerating by importViewModel.isGenerating.collectAsState()
    val canStartNew = !isGenerating || com.example.pullit.viewmodel.BackgroundGenerationState.canStartNew
    val error by importViewModel.error.collectAsState()
    val errorType by importViewModel.errorType.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val importScope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            importScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    selectedImageBytes = bytes
                }
            }
        }
    }

    val detectedPlatform = remember(inputText) {
        if (inputText.isNotBlank()) importViewModel.detectPlatform(inputText) else "unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title
        Text(
            S.importRecipe,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Supported Platforms caption
        Text(
            S.supportedPlatforms,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Platform icons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data class PlatformIcon(val iconRes: Int, val label: String)

            val platforms = listOf(
                PlatformIcon(R.drawable.ic_platform_xiaohongshu, S.xiaohongshu),
                PlatformIcon(R.drawable.ic_platform_douyin, S.douyin),
                PlatformIcon(R.drawable.ic_platform_bilibili, "Bilibili"),
                PlatformIcon(R.drawable.ic_platform_xiachufang, S.xiachufang),
                PlatformIcon(R.drawable.ic_platform_tiktok, "TikTok"),
                PlatformIcon(R.drawable.ic_platform_instagram, "Instagram"),
                PlatformIcon(R.drawable.ic_platform_youtube, "YouTube"),
                PlatformIcon(R.drawable.ic_platform_pinterest, S.pinterest)
            )

            platforms.forEach { platform ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = platform.iconRes),
                            contentDescription = platform.label,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        platform.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))


        // Custom segmented control (Link / Text / Image)
        SegmentedTabControl(
            selectedIndex = importMethod.ordinal,
            tabs = listOf(S.link, S.text, S.image),
            onTabSelected = { index ->
                importViewModel.setImportMethod(ImportMethod.entries[index])
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tab content
        when (importMethod) {
            ImportMethod.LINK -> LinkTab(
                inputText = inputText,
                detectedPlatform = detectedPlatform,
                isGenerating = isGenerating,
                canStartNew = canStartNew,
                onInputChange = { importViewModel.setInputText(it) },
                onPaste = {
                    clipboardManager.getText()?.text?.let { importViewModel.setInputText(it) }
                },
                onClear = { importViewModel.setInputText("") },
                onImport = {
                    onStartGenerating()
                    importViewModel.importFromLink()
                },
                onSwitchToImage = {
                    importViewModel.setImportMethod(ImportMethod.IMAGE)
                },
                onSwitchToText = {
                    importViewModel.setImportMethod(ImportMethod.TEXT)
                }
            )

            ImportMethod.TEXT -> TextTab(
                inputText = inputText,
                isGenerating = isGenerating,
                canStartNew = canStartNew,
                onInputChange = { importViewModel.setInputText(it) },
                onImport = {
                    onStartGenerating()
                    importViewModel.importFromText(inputText)
                }
            )

            ImportMethod.IMAGE -> ImageTab(
                selectedImageUri = selectedImageUri,
                isGenerating = isGenerating,
                canStartNew = canStartNew,
                onPickImage = { imagePicker.launch("image/*") },
                onImport = {
                    selectedImageBytes?.let {
                        onStartGenerating()
                        importViewModel.importFromImage(it)
                    }
                }
            )
        }

        // Error display — NOT_RECIPE stays until dismissed; others auto-dismiss after 5s
        if (error != null) {
            LaunchedEffect(error) {
                if (errorType != ImportErrorType.NOT_RECIPE) {
                    kotlinx.coroutines.delay(5000)
                    importViewModel.clearError()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ImportErrorCard(
                errorType = errorType,
                onDismiss = { importViewModel.clearError() },
                onSwitchToText = { importViewModel.setImportMethod(ImportMethod.TEXT) },
                onSwitchToImage = { importViewModel.setImportMethod(ImportMethod.IMAGE) }
            )
        }
    }
}

@Composable
private fun SegmentedTabControl(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Primary else Color.Transparent,
                    animationSpec = tween(200),
                    label = "tabBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextSecondary,
                    animationSpec = tween(200),
                    label = "tabText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkTab(
    inputText: String,
    detectedPlatform: String,
    isGenerating: Boolean,
    canStartNew: Boolean,
    onInputChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onImport: () -> Unit,
    onSwitchToImage: () -> Unit,
    onSwitchToText: () -> Unit
) {
    val S = LocalStrings.current
    // Text field with link icon and paste/clear button
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.Link,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        S.pasteLink,
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            if (inputText.isEmpty()) {
                TextButton(
                    onClick = onPaste,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Primary.copy(alpha = 0.1f),
                        contentColor = Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(S.paste, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Detected platform badge
    if (detectedPlatform != "unknown" && inputText.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                platformDisplayName(detectedPlatform, S),
                style = MaterialTheme.typography.labelMedium,
                color = Success,
                fontWeight = FontWeight.Medium
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Import Recipe button
    Button(
        onClick = onImport,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = detectedPlatform != "unknown" && inputText.isNotBlank() && canStartNew,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            disabledContainerColor = Primary.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(S.processing, color = Color.White)
        } else {
            Text(
                S.importRecipe,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // Labeled divider "Or Import From"
    Spacer(modifier = Modifier.height(24.dp))
    LabeledDivider(label = S.orImportFrom)
    Spacer(modifier = Modifier.height(16.dp))

    // Two equal buttons side by side
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Photo button
        OutlinedButton(
            onClick = onSwitchToImage,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                S.photo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Text button
        OutlinedButton(
            onClick = onSwitchToText,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(
                Icons.Outlined.TextFields,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                S.text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TextTab(
    inputText: String,
    isGenerating: Boolean,
    canStartNew: Boolean,
    onInputChange: (String) -> Unit,
    onImport: () -> Unit
) {
    val S = LocalStrings.current
    // Large text area
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    S.pasteRecipeText,
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Import Recipe button
    Button(
        onClick = onImport,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = inputText.isNotBlank() && canStartNew,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            disabledContainerColor = Primary.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(S.processing, color = Color.White)
        } else {
            Text(
                S.importRecipe,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ImageTab(
    selectedImageUri: Uri?,
    isGenerating: Boolean,
    canStartNew: Boolean,
    onPickImage: () -> Unit,
    onImport: () -> Unit
) {
    val S = LocalStrings.current
    // Photo picker button
    OutlinedButton(
        onClick = onPickImage,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (selectedImageUri != null) S.changeImage else S.chooseImage,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Selected image preview
    if (selectedImageUri != null) {
        Spacer(modifier = Modifier.height(12.dp))
        AsyncImage(
            model = selectedImageUri,
            contentDescription = S.selectedImage,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Import Recipe button
    Button(
        onClick = onImport,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = selectedImageUri != null && canStartNew,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            disabledContainerColor = Primary.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(S.processing, color = Color.White)
        } else {
            Text(
                S.importRecipe,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LabeledDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ImportErrorCard(
    errorType: ImportErrorType?,
    onDismiss: () -> Unit,
    onSwitchToText: () -> Unit,
    onSwitchToImage: () -> Unit
) {
    val S = LocalStrings.current
    val isNotRecipe = errorType == ImportErrorType.NOT_RECIPE

    val containerColor = if (isNotRecipe)
        MaterialTheme.colorScheme.secondaryContainer
    else
        Error.copy(alpha = 0.1f)
    val iconTint = if (isNotRecipe) MaterialTheme.colorScheme.secondary else Error
    val icon = if (isNotRecipe) Icons.Outlined.Info else Icons.Outlined.ErrorOutline

    val message = when (errorType) {
        ImportErrorType.NOT_RECIPE -> S.importErrors.importErrorNotRecipe
        ImportErrorType.NETWORK -> S.importErrors.importErrorNetwork
        ImportErrorType.TIMEOUT -> S.importErrors.importErrorTimeout
        else -> S.importErrors.importErrorUnknown
    }
    val hint = if (isNotRecipe) S.importErrors.importErrorNotRecipeHint else null

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    message,
                    color = if (isNotRecipe) MaterialTheme.colorScheme.onSecondaryContainer else Error,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = if (isNotRecipe) MaterialTheme.colorScheme.onSecondaryContainer else Error,
                    modifier = Modifier.size(18.dp).clickable { onDismiss() }
                )
            }
            if (hint != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    hint,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onDismiss(); onSwitchToText() },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(S.importErrors.importTryText, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { onDismiss(); onSwitchToImage() },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(S.importErrors.importTryImage, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun platformDisplayName(platform: String, S: com.example.pullit.ui.AppStrings): String = when (platform) {
    "xiaohongshu" -> S.xiaohongshu
    "douyin" -> S.douyin
    "bilibili" -> S.bilibili
    "xiachufang" -> S.xiachufang
    "tiktok" -> "TikTok"
    "instagram" -> "Instagram"
    "youtube" -> "YouTube"
    "pinterest" -> S.pinterest
    else -> platform
}
