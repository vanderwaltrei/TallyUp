package za.ac.iie.TallyUp.data
import za.ac.iie.TallyUp.data.User
import za.ac.iie.TallyUp.data.UserDao

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}