package com.example.pullit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pullit.data.model.Cookbook
import com.example.pullit.data.model.GroceryItem
import com.example.pullit.data.model.MealPlanItem
import com.example.pullit.data.model.Recipe

@Database(
    entities = [Recipe::class, Cookbook::class, MealPlanItem::class, GroceryItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun cookbookDao(): CookbookDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun groceryDao(): GroceryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pullit_recipes.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
