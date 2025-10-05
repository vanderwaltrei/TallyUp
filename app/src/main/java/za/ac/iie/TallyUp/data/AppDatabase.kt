package za.ac.iie.TallyUp.data

import androidx.room.migration.Migration
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

@Database(entities = [User::class, Category::class, Transaction::class], version = 4) // CHANGED TO 4
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
                    .fallbackToDestructiveMigration() // This will clear old database and create new one
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
                            Category(name = "Food", type = "Expense"),
                            Category(name = "Transport", type = "Expense"),
                            Category(name = "Books", type = "Expense"),
                            Category(name = "Fun", type = "Expense"),
                            Category(name = "Shopping", type = "Expense"),
                            Category(name = "Other", type = "Expense"),
                            Category(name = "Salary", type = "Income"),
                            Category(name = "Gift", type = "Income"),
                            Category(name = "Freelance", type = "Income"),
                            Category(name = "Allowance", type = "Income")
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