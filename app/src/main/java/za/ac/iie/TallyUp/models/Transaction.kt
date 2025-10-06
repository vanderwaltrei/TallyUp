package za.ac.iie.TallyUp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val type: String, // "income" or "expense"
    val amount: Double,
    val category: String,
    val description: String?,
    val date: Date,
    val photoUris: List<String> = emptyList()
    val userId: String
)