package za.ac.iie.TallyUp.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Goal::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}