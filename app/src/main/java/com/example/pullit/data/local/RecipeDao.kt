package com.example.pullit.data.local

import androidx.room.*
import com.example.pullit.data.model.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY addedAt DESC")
    fun getAll(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: String): Recipe?

    @Query("SELECT * FROM recipes WHERE favorited = 1 ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<Recipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(recipes: List<Recipe>)

    @Delete
    suspend fun delete(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int
}
