package za.ac.iie.TallyUp.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.data.TransactionDao
import za.ac.iie.TallyUp.models.Transaction
import java.util.Date
import java.util.UUID

class TransactionViewModel(private val transactionDao: TransactionDao) : ViewModel() {

    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        description: String?,
        photoUris: List<String>,
        date: Date,
        userId: String
    ) {
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            type = type,
            amount = amount,
            category = category,
            description = description,
            photoUris = photoUris,
            date = date,
            userId = userId
        )

        viewModelScope.launch {
            transactionDao.insertTransaction(transaction)
        }
    }
}

