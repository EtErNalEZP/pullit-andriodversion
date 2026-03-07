package com.example.pullit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: String,
    val title: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val desc: String? = null,
    val cookTime: String? = null,
    val calories: String? = null,
    val servings: Int = 2,
    val nutritionJson: String? = null,
    val ingredientsJson: String? = null,
    val stepsJson: String? = null,
    val sourceUrl: String? = null,
    val transcription: String? = null,
    val labelsJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val favorited: Boolean = false,
    val cookbookIdsJson: String? = null
)
