package com.example.pullit.data.local

import androidx.room.*
import com.example.pullit.data.model.GroceryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_items")
    fun getAll(): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroceryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<GroceryItem>)

    @Delete
    suspend fun delete(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM grocery_items")
    suspend fun deleteAll()

    @Query("DELETE FROM grocery_items WHERE checked = 1")
    suspend fun deleteChecked()
}
