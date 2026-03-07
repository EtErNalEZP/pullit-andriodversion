package com.example.pullit.data.sync

import android.content.Context
import com.example.pullit.data.local.AppDatabase

class MigrationService(private val context: Context) {
    private val prefs = context.getSharedPreferences("pullit_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getInstance(context)
    private val syncEngine = SyncEngine(db)

    suspend fun migrateIfNeeded(userId: String) {
        if (prefs.getBoolean("migration_done_$userId", false)) return

        val localCount = db.recipeDao().count()
        if (localCount > 0) {
            syncEngine.pushAll(userId)
        } else {
            syncEngine.pullAll(userId)
        }

        prefs.edit().putBoolean("migration_done_$userId", true).apply()
    }
}
