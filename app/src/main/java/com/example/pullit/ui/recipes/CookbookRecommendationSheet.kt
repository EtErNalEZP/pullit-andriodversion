package com.example.pullit.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.pullit.data.model.Recipe
import com.example.pullit.data.model.RecipeLabels
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.RecipeListViewModel
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CookbookRecommendationSheet(
    recipe: Recipe,
    viewModel: RecipeListViewModel,
    onDismiss: () -> Unit
) {
    val S = LocalStrings.current
    val json = remember { Json { ignoreUnknownKeys = true } }

    val initialLabels = remember(recipe) {
        recipe.labelsJson?.let {
            runCatching { json.decodeFromString<RecipeLabels>(it) }.getOrNull()
        } ?: RecipeLabels()
    }

    var editableLabels by remember { mutableStateOf(initialLabels) }
    var newTagText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCookbookName by remember { mutableStateOf("") }
    var addedCookbookIds by remember { mutableStateOf(setOf<String>()) }

    val cookbooks by viewModel.cookbooks.collectAsState()

    // Matching cookbooks by tag overlap
    val matches = remember(cookbooks, editableLabels) {
        val recipeValues = editableLabels.allValues.map { it.lowercase() }.toSet()
        if (recipeValues.isEmpty()) emptyList()
        else cookbooks.mapNotNull { cookbook ->
            val cookbookTags = cookbook.tagsJson?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            val score = cookbookTags.count { it.lowercase() in recipeValues }
            if (score > 0) cookbook to score else null
        }.sortedByDescending { it.second }
    }

    // Existing cookbook membership
    val existingCookbookIds = remember(cookbooks, recipe) {
        cookbooks.filter { cookbook ->
            val ids = cookbook.recipeIdsJson?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            recipe.id in ids
        }.map { it.id }.toSet()
    }

    // Save labels when they change
    LaunchedEffect(editableLabels) {
        if (editableLabels != initialLabels) {
            viewModel.updateRecipeLabels(recipe.id, editableLabels)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Title bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    S.cookbookRecommendTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        S.done,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Recipe Header ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!recipe.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = recipe.imageUrl,
                                contentDescription = recipe.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Restaurant,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            recipe.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            S.cookbookRecommendSubtitle,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ── Labels Section ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    S.recipeLabelsTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editableLabels.cuisine?.takeIf { it.isNotEmpty() }?.let { cuisine ->
                        EditableTagChip(
                            text = cuisine,
                            icon = Icons.Outlined.Language,
                            onDelete = {
                                editableLabels = editableLabels.copy(cuisine = null)
                            }
                        )
                    }

                    editableLabels.mealType?.takeIf { it.isNotEmpty() }?.let { mealType ->
                        EditableTagChip(
                            text = mealType,
                            icon = Icons.Outlined.Schedule,
                            onDelete = {
                                editableLabels = editableLabels.copy(mealType = null)
                            }
                        )
                    }

                    editableLabels.tags.forEach { tag ->
                        EditableTagChip(
                            text = tag,
                            icon = Icons.Outlined.LocalOffer,
                            onDelete = {
                                editableLabels = editableLabels.copy(
                                    tags = editableLabels.tags.filter { it != tag }
                                )
                            }
                        )
                    }
                }

                // Add new tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(Primary),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (newTagText.isEmpty()) {
                                    Text(
                                        S.addTagPlaceholder,
                                        fontSize = 13.sp,
                                        color = TextTertiary
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            val tag = newTagText.trim()
                            if (tag.isNotEmpty()) {
                                val existing = editableLabels.allValues.map { it.lowercase() }
                                if (tag.lowercase() !in existing) {
                                    editableLabels = editableLabels.copy(
                                        tags = editableLabels.tags + tag
                                    )
                                }
                                newTagText = ""
                            }
                        },
                        enabled = newTagText.trim().isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = S.add,
                            tint = if (newTagText.trim().isNotEmpty()) Primary else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ── Cookbooks Section ──
            val matchedIds = matches.map { it.first.id }.toSet()
            val otherCookbooks = cookbooks.filter { it.id !in matchedIds }

            if (matches.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        S.matchingCookbooksTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    matches.forEach { (cookbook, _) ->
                        CookbookRow(
                            cookbook = cookbook,
                            isAdded = cookbook.id in addedCookbookIds || cookbook.id in existingCookbookIds,
                            onAdd = {
                                viewModel.addRecipeToCookbook(recipe.id, cookbook.id)
                                addedCookbookIds = addedCookbookIds + cookbook.id
                            },
                            onRemove = {
                                viewModel.removeRecipeFromCookbook(recipe.id, cookbook.id)
                                addedCookbookIds = addedCookbookIds - cookbook.id
                            }
                        )
                    }
                }
            }

            if (otherCookbooks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (matches.isNotEmpty()) S.allCookbooksTitle else S.cookbooks,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    otherCookbooks.forEach { cookbook ->
                        CookbookRow(
                            cookbook = cookbook,
                            isAdded = cookbook.id in addedCookbookIds || cookbook.id in existingCookbookIds,
                            onAdd = {
                                viewModel.addRecipeToCookbook(recipe.id, cookbook.id)
                                addedCookbookIds = addedCookbookIds + cookbook.id
                            },
                            onRemove = {
                                viewModel.removeRecipeFromCookbook(recipe.id, cookbook.id)
                                addedCookbookIds = addedCookbookIds - cookbook.id
                            }
                        )
                    }
                }
            }

            // ── Create New Section ──
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        newCookbookName = editableLabels.mealType?.takeIf { it.isNotEmpty() }
                            ?: editableLabels.cuisine?.takeIf { it.isNotEmpty() }
                            ?: editableLabels.tags.firstOrNull()
                            ?: ""
                        showCreateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        Icons.Filled.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        S.createNewCookbook,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // ── Create Cookbook Dialog ──
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newCookbookName = "" },
            title = { Text(S.createNewCookbook) },
            text = {
                OutlinedTextField(
                    value = newCookbookName,
                    onValueChange = { newCookbookName = it },
                    placeholder = { Text(S.cookbookName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newCookbookName.trim()
                        if (name.isNotEmpty()) {
                            viewModel.createCookbookWithTags(name, recipe.id, editableLabels.allValues)
                            showCreateDialog = false
                            newCookbookName = ""
                        }
                    },
                    enabled = newCookbookName.trim().isNotEmpty()
                ) {
                    Text(S.create, color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newCookbookName = "" }) {
                    Text(S.cancel)
                }
            }
        )
    }
}

@Composable
private fun CookbookRow(
    cookbook: com.example.pullit.data.model.Cookbook,
    isAdded: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val S = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )

            Text(
                cookbook.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isAdded) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(S.add, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EditableTagChip(
    text: String,
    icon: ImageVector,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = PrimaryLight.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = PrimaryDark
            )
            Text(
                text,
                fontSize = 12.sp,
                color = PrimaryDark
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Filled.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = PrimaryDark.copy(alpha = 0.5f)
                )
            }
        }
    }
}
