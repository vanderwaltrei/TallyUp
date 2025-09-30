package za.ac.iie.TallyUp.model

import java.util.Date
import java.util.UUID

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val target: Double,
    var current: Double = 0.0,
    val deadline: String,
    val createdAt: Date = Date()
)