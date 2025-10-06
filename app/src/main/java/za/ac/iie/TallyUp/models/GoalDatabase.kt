@file:Suppress("PackageName")

package za.ac.iie.TallyUp.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import za.ac.iie.TallyUp.data.GoalDao

@Database(entities = [Goal::class], version = 2, exportSchema = false)
abstract class GoalDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: GoalDatabase? = null

        fun getDatabase(context: Context): GoalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoalDatabase::class.java,
                    "goal_database"
                )
                    .fallbackToDestructiveMigration() // This will clear old data
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}