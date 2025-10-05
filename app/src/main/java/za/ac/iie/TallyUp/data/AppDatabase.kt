package za.ac.iie.TallyUp.data
import za.ac.iie.TallyUp.data.User
import za.ac.iie.TallyUp.data.UserDao
import za.ac.iie.TallyUp.data.CategoryDao


import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [User::class, Category::class, Transaction::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}

