package za.ac.iie.TallyUp.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: String)
}