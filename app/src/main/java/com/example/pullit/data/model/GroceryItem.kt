package com.example.pullit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey val id: String,
    val name: String,
    val amount: String = "",
    val checked: Boolean = false,
    val category: String? = null,
    val fromRecipesJson: String? = null
)
