package com.example.pullit.data.sync

import com.example.pullit.auth.SupabaseManager
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.Recipe
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SyncEngine(private val db: AppDatabase) {
    private val supabase = SupabaseManager.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun pullAll(userId: String) = withContext(Dispatchers.IO) {
        try {
            val remoteRecipes = supabase.postgrest.from("recipes")
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseRecipe>()

            for (remote in remoteRecipes) {
                db.recipeDao().upsert(remote.toLocal())
            }

            val remoteCookbooks = supabase.postgrest.from("cookbooks")
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseCookbook>()

            for (remote in remoteCookbooks) {
                db.cookbookDao().upsert(remote.toLocal())
            }
        } catch (_: Exception) { }
    }

    suspend fun pushRecipe(recipe: Recipe, userId: String) = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest.from("recipes")
                .upsert(SupabaseRecipe.fromLocal(recipe, userId))
        } catch (_: Exception) { }
    }

    suspend fun pushAll(userId: String) = withContext(Dispatchers.IO) {
        try {
            val recipes = db.recipeDao().getAll().first()
            for (recipe in recipes) {
                pushRecipe(recipe, userId)
            }
        } catch (_: Exception) { }
    }

    suspend fun deleteRemoteRecipe(recipeId: String, userId: String) = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest.from("recipes")
                .delete { filter { eq("id", recipeId); eq("user_id", userId) } }
        } catch (_: Exception) { }
    }
}
