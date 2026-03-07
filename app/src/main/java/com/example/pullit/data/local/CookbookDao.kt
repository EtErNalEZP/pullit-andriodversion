package com.example.pullit.data.local

import androidx.room.*
import com.example.pullit.data.model.Cookbook
import kotlinx.coroutines.flow.Flow

@Dao
interface CookbookDao {
    @Query("SELECT * FROM cookbooks")
    fun getAll(): Flow<List<Cookbook>>

    @Query("SELECT * FROM cookbooks WHERE id = :id")
    suspend fun getById(id: String): Cookbook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cookbook: Cookbook)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cookbooks: List<Cookbook>)

    @Delete
    suspend fun delete(cookbook: Cookbook)

    @Query("DELETE FROM cookbooks WHERE id = :id")
    suspend fun deleteById(id: String)
}
