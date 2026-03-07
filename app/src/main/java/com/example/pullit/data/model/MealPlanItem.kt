package com.example.pullit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plan_items")
data class MealPlanItem(
    @PrimaryKey val id: String,
    val recipeId: String,
    val servings: Int = 1,
    val addedAt: Long = System.currentTimeMillis()
)
