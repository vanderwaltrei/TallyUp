package za.ac.iie.TallyUp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getTransactionsForUser(userId: String): List<Transaction>

    @Query("""
    SELECT * FROM transactions
    WHERE userId = :userId AND date BETWEEN :startDate AND :endDate
    ORDER BY date DESC
""")
    suspend fun getTransactionsByDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<Transaction>

}