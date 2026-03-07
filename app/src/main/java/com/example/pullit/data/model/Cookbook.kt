package com.example.pullit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cookbooks")
data class Cookbook(
    @PrimaryKey val id: String,
    val title: String,
    val recipeIdsJson: String? = null,
    val tagsJson: String? = null
)
