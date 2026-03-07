package com.example.pullit.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.AiRecommendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionDetailScreen(
    index: Int,
    navController: NavController,
    viewModel: AiRecommendViewModel = viewModel()
) {
    val suggestions by viewModel.suggestions.collectAsState()
    val suggestion = suggestions.getOrNull(index)

    if (suggestion == null) {
        navController.popBackStack()
        return
    }

    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and description
            item {
                Column {
                    Text(
                        suggestion.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        suggestion.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        lineHeight = 24.sp
                    )

                    // Calories
                    suggestion.calories?.let { cal ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontSize = 14.sp)) { append("\uD83D\uDD25 ") }
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Primary)) {
                                        append(cal)
                                    }
                                    withStyle(SpanStyle(color = TextSecondary)) {
                                        append(" kcal")
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Save to Recipes button
            item {
                Button(
                    onClick = {
                        viewModel.saveRecipeFromSuggestion(suggestion)
                        saved = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !saved,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (saved) Success else Primary,
                        disabledContainerColor = Success.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (saved) "Saved to Recipes" else "Save to Recipes",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Ingredients section
            item {
                Text(
                    "Ingredients",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(suggestion.ingredients) { _, ing ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Primary colored bullet
                    Surface(
                        shape = CircleShape,
                        color = Primary,
                        modifier = Modifier
                            .size(8.dp)
                            .offset(y = 7.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        ing,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Steps section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(suggestion.steps) { i, step ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Numbered circle
                    Surface(
                        shape = CircleShape,
                        color = Primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${i + 1}",
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 5.dp),
                        lineHeight = 22.sp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
