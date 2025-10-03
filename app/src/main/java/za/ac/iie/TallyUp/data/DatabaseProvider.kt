package za.ac.iie.TallyUp.data
import android.content.Context
import androidx.room.Room
import za.ac.iie.TallyUp.data.AppDatabase // adjust if needed

object DatabaseProvider {
    private var dbInstance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (dbInstance == null) {
            dbInstance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tallyup_database"
            ).build()
        }
        return dbInstance!!
    }
}