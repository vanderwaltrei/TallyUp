package za.ac.iie.TallyUp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "Income" or "Expense"
    val color: String = "#FFFFFF", // default white
    val userId: String
)


