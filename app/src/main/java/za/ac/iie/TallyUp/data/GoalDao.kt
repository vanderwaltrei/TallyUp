package za.ac.iie.TallyUp.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GoalDao {

    @Insert
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    // Get goals for specific user
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getGoalsByUser(userId: String): List<Goal>

    // Keep this for migration or remove if not needed
    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<Goal>
}