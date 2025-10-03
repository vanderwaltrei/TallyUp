package za.ac.iie.TallyUp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)
