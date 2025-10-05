package za.ac.iie.TallyUp.data

import androidx.room.migration.Migration
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

@Database(entities = [User::class, Category::class, Transaction::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tallyup_database"
                )
                    .fallbackToDestructiveMigration(true) // true = drop all tables if needed
                    .build()

                INSTANCE = instance
                prepopulateCategories(instance)
                instance
            }
        }

        private fun prepopulateCategories(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val existing = db.categoryDao().getAllCategories()
                    if (existing.isEmpty()) {
                        val defaultCategories = listOf(
                            Category(name = "Food", type = "Expense", color = "#FFB085"),       // orange
                            Category(name = "Transport", type = "Expense", color = "#A3D5FF"),  // blue
                            Category(name = "Books", type = "Expense", color = "#B2E2B2"),      // green
                            Category(name = "Fun", type = "Expense", color = "#FFF4A3"),        // yellow
                            Category(name = "Shopping", type = "Expense", color = "#FFB6C1"),   // pink
                            Category(name = "Other", type = "Expense", color = "#E0E0E0"),      // gray
                            Category(name = "Salary", type = "Income", color = "#D1B3FF"),      // purple
                            Category(name = "Gift", type = "Income", color = "#D1B3FF"),        // purple
                            Category(name = "Freelance", type = "Income", color = "#D1B3FF"),   // purple
                            Category(name = "Allowance", type = "Income", color = "#D1B3FF")    // purple
                        )
                        db.categoryDao().insertCategories(defaultCategories)
                        Log.d("AppDatabase", "Default categories populated")
                    }
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Prepopulate categories error: ${e.message}")
                }
            }
        }
    }
}