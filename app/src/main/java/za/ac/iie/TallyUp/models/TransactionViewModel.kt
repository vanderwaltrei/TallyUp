package za.ac.iie.TallyUp.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.data.TransactionDao

class TransactionViewModel(private val transactionDao: TransactionDao) : ViewModel() {

    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        description: String?,
        photoUris: List<String>,
        date: Long,
        userId: String
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                category = category,
                description = description,
                photoUris = photoUris,
                date = date,
                userId = userId
            )
            transactionDao.insertTransaction(transaction)
        }
    }

    fun getAllTransactions(): LiveData<List<Transaction>> = liveData {
        emit(transactionDao.getAllTransactions())
    }

    fun getTransactionsForUser(userId: String): LiveData<List<Transaction>> = liveData {
        emit(transactionDao.getTransactionsForUser(userId))
    }
}