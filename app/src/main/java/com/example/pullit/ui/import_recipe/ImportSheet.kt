package com.example.pullit.ui.import_recipe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.ImportMethod
import com.example.pullit.viewmodel.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(
    navController: NavController,
    viewModel: ImportViewModel = viewModel()
) {
    val inputText by viewModel.inputText.collectAsState()
    val importMethod by viewModel.importMethod.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val error by viewModel.error.collectAsState()
    val generatedRecipe by viewModel.generatedRecipe.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) viewModel.importFromImage(bytes)
        }
    }

    LaunchedEffect(generatedRecipe) {
        generatedRecipe?.let {
            navController.navigate(Screen.RecipeDetail.createRoute(it.id)) {
                popUpTo(Screen.Import.route) { inclusive = true }
            }
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Recipe") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Method tabs
            TabRow(selectedTabIndex = importMethod.ordinal) {
                Tab(selected = importMethod == ImportMethod.LINK, onClick = { viewModel.setImportMethod(ImportMethod.LINK) }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Link")
                    }
                }
                Tab(selected = importMethod == ImportMethod.TEXT, onClick = { viewModel.setImportMethod(ImportMethod.TEXT) }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Text")
                    }
                }
                Tab(selected = importMethod == ImportMethod.IMAGE, onClick = { viewModel.setImportMethod(ImportMethod.IMAGE) }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Image")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (importMethod) {
                ImportMethod.LINK -> {
                    Text("Paste a recipe link", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Supports: Xiaohongshu, Douyin, Bilibili, TikTok, Instagram, YouTube, Xiachufang", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.setInputText(it) },
                        placeholder = { Text("Paste link or share text here...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.importFromLink() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = inputText.isNotBlank() && !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(progress?.message ?: "Processing...", color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Import Recipe", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ImportMethod.TEXT -> {
                    Text("Paste recipe text", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.setInputText(it) },
                        placeholder = { Text("Paste recipe text here...") },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        minLines = 8
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.importFromText(inputText) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = inputText.isNotBlank() && !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Parse Recipe", fontWeight = FontWeight.Bold)
                    }
                }
                ImportMethod.IMAGE -> {
                    Text("Import from image", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Take a photo or pick an image of a recipe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else { Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(8.dp)); Text("Choose Image", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
