package com.example.pullit.data.local

import androidx.room.*
import com.example.pullit.data.model.MealPlanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan_items ORDER BY addedAt DESC")
    fun getAll(): Flow<List<MealPlanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MealPlanItem)

    @Delete
    suspend fun delete(item: MealPlanItem)

    @Query("DELETE FROM meal_plan_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM meal_plan_items")
    suspend fun deleteAll()
}
