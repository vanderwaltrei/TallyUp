package za.ac.iie.TallyUp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String,
    val category: String,
    val description: String?,
    val photoUri: String?,
    val date: Long
) : Parcelable
