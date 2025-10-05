package za.ac.iie.TallyUp.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var dbInstance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (dbInstance == null) {
            dbInstance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tallyup_database"
            )
                .fallbackToDestructiveMigration() // This fixes the migration issue
                .build()
        }
        return dbInstance!!
    }
}