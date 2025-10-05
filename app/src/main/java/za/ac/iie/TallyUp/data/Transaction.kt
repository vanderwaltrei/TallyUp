package za.ac.iie.TallyUp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val amount: Double,
    val category: String,
    val type: String, // "Income" or "Expense"
    val date: Long,   // Store as timestamp
    val notes: String?
)