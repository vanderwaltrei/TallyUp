package za.ac.iie.TallyUp.data

import androidx.room.migration.Migration
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

@Database(entities = [User::class, Category::class, Transaction::class], version = 5)
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
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
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
                        val defaultUserId = "default" // or use real user ID if available

                        val defaultCategories = listOf(
                            Category(name = "Food", type = "Expense", color = "#FFB085", userId = defaultUserId),
                            Category(name = "Transport", type = "Expense", color = "#A3D5FF", userId = defaultUserId),
                            Category(name = "Books", type = "Expense", color = "#B2E2B2", userId = defaultUserId),
                            Category(name = "Fun", type = "Expense", color = "#FFF4A3", userId = defaultUserId),
                            Category(name = "Shopping", type = "Expense", color = "#FFB6C1", userId = defaultUserId),
                            Category(name = "Other", type = "Expense", color = "#E0E0E0", userId = defaultUserId),
                            Category(name = "Salary", type = "Income", color = "#D1B3FF", userId = defaultUserId),
                            Category(name = "Gift", type = "Income", color = "#D1B3FF", userId = defaultUserId),
                            Category(name = "Freelance", type = "Income", color = "#D1B3FF", userId = defaultUserId),
                            Category(name = "Allowance", type = "Income", color = "#D1B3FF", userId = defaultUserId)
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
    }
}
