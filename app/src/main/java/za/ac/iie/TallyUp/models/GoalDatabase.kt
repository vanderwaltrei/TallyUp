package za.ac.iie.TallyUp.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import za.ac.iie.TallyUp.data.GoalDao

@Database(entities = [Goal::class], version = 3, exportSchema = false)
abstract class GoalDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: GoalDatabase? = null

        // Migration from version 2 to 3 to add minimum field
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE goals ADD COLUMN minimum REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): GoalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoalDatabase::class.java,
                    "goal_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}