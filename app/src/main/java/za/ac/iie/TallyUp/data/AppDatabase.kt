package za.ac.iie.TallyUp.data
import za.ac.iie.TallyUp.data.User
import za.ac.iie.TallyUp.data.UserDao
import za.ac.iie.TallyUp.data.CategoryDao
import androidx.room.TypeConverters
import za.ac.iie.TallyUp.data.Converters
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.data.*


import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [User::class, Category::class, Transaction::class], version = 3)
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
                ).build()

                INSTANCE = instance
                prepopulateCategories(instance)
                instance
            }
        }

        private fun prepopulateCategories(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
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
                }
            }
        }
    }
}




