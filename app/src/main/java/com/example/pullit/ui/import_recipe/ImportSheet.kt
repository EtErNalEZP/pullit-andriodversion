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
import com.example.pullit.R
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.ImportMethod
import com.example.pullit.viewmodel.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(
    importViewModel: ImportViewModel,
    onDismiss: () -> Unit,
    onStartGenerating: () -> Unit
) {
    val inputText by importViewModel.inputText.collectAsState()
    val importMethod by importViewModel.importMethod.collectAsState()
    val isGenerating by importViewModel.isGenerating.collectAsState()
    val error by importViewModel.error.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            selectedImageBytes = bytes
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
        // Drag handle
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }

        // Title
        Text(
            "Import Recipe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Supported Platforms caption
        Text(
            "Supported Platforms",
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
                PlatformIcon(R.drawable.ic_platform_xiaohongshu, "\u5C0F\u7EA2\u4E66"),
                PlatformIcon(R.drawable.ic_platform_douyin, "TikTok"),
                PlatformIcon(R.drawable.ic_platform_bilibili, "Bilibili"),
                PlatformIcon(R.drawable.ic_platform_xiachufang, "\u4E0B\u53A8\u623F"),
                PlatformIcon(R.drawable.ic_platform_instagram, "Instagram"),
                PlatformIcon(R.drawable.ic_platform_youtube, "YouTube")
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
            tabs = listOf("Link", "Text", "Image"),
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
                onInputChange = { importViewModel.setInputText(it) },
                onImport = {
                    onStartGenerating()
                    importViewModel.importFromText(inputText)
                }
            )

            ImportMethod.IMAGE -> ImageTab(
                selectedImageUri = selectedImageUri,
                isGenerating = isGenerating,
                onPickImage = { imagePicker.launch("image/*") },
                onImport = {
                    selectedImageBytes?.let {
                        onStartGenerating()
                        importViewModel.importFromImage(it)
                    }
                }
            )
        }

        // Error display
        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Error.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        it,
                        color = Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = Error,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { importViewModel.clearError() }
                    )
                }
            }
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
    onInputChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onImport: () -> Unit,
    onSwitchToImage: () -> Unit,
    onSwitchToText: () -> Unit
) {
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
                        "Paste link or share text here...",
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
                    Text("Paste", style = MaterialTheme.typography.labelMedium)
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
                platformDisplayName(detectedPlatform),
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
        enabled = detectedPlatform != "unknown" && inputText.isNotBlank() && !isGenerating,
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
            Text("Processing...", color = Color.White)
        } else {
            Text(
                "Import Recipe",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // Labeled divider "Or Import From"
    Spacer(modifier = Modifier.height(24.dp))
    LabeledDivider(label = "Or Import From")
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
                "Photo",
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
                "Text",
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
    onInputChange: (String) -> Unit,
    onImport: () -> Unit
) {
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
                    "Paste recipe text here...",
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
        enabled = inputText.isNotBlank() && !isGenerating,
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
            Text("Processing...", color = Color.White)
        } else {
            Text(
                "Import Recipe",
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
    onPickImage: () -> Unit,
    onImport: () -> Unit
) {
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
            if (selectedImageUri != null) "Change Image" else "Choose Image",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Selected image preview
    if (selectedImageUri != null) {
        Spacer(modifier = Modifier.height(12.dp))
        AsyncImage(
            model = selectedImageUri,
            contentDescription = "Selected image",
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
        enabled = selectedImageUri != null && !isGenerating,
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
            Text("Processing...", color = Color.White)
        } else {
            Text(
                "Import Recipe",
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

private fun platformDisplayName(platform: String): String = when (platform) {
    "xiaohongshu" -> "\u5C0F\u7EA2\u4E66 (Xiaohongshu)"
    "douyin" -> "\u6296\u97F3 (Douyin)"
    "bilibili" -> "Bilibili"
    "xiachufang" -> "\u4E0B\u53A8\u623F (Xiachufang)"
    "tiktok" -> "TikTok"
    "instagram" -> "Instagram"
    "youtube" -> "YouTube"
    else -> platform
}
