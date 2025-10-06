package za.ac.iie.TallyUp.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.data.TransactionDao
import java.util.*

class TransactionViewModel(private val transactionDao: TransactionDao) : ViewModel() {

    private val typeFilter = MutableLiveData("All")
    private val timeFilter = MutableLiveData("All")
    private val categoryFilter = MutableLiveData("All")
    private val allTransactions = MutableLiveData<List<Transaction>>()

    val filteredTransactions = MediatorLiveData<List<Transaction>>()

    init {
        filteredTransactions.addSource(allTransactions) { updateFiltered() }
        filteredTransactions.addSource(typeFilter) { updateFiltered() }
        filteredTransactions.addSource(timeFilter) { updateFiltered() }
        filteredTransactions.addSource(categoryFilter) { updateFiltered() }
    }

    private fun updateFiltered() {
        val all = allTransactions.value ?: emptyList()
        val type = typeFilter.value ?: "All"
        val time = timeFilter.value ?: "All"
        val category = categoryFilter.value ?: "All"

        val filtered = all.filter { transaction ->
            val matchesType = type == "All" || transaction.type == type
            val matchesTime = when (time) {
                "Today" -> isToday(transaction.date)
                "This Week" -> isThisWeek(transaction.date)
                "This Month" -> isThisMonth(transaction.date)
                else -> true
            }
            val matchesCategory = category == "All" || transaction.category == category

            matchesType && matchesTime && matchesCategory
        }

        filteredTransactions.value = filtered
    }

    fun setTypeFilter(type: String) {
        typeFilter.value = type
    }

    fun setTimeFilter(time: String) {
        timeFilter.value = time
    }

    fun setCategoryFilter(category: String) {
        categoryFilter.value = category
    }

    fun loadTransactionsForUser(userId: String) {
        viewModelScope.launch {
            val transactions = transactionDao.getTransactionsForUser(userId)
            allTransactions.postValue(transactions)
        }
    }

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
                amount = amount,
                type = type,
                category = category,
                description = description,
                photoUris = photoUris,
                date = date,
                userId = userId
            )
            transactionDao.insertTransaction(transaction)
        }
    }

    private fun isToday(date: Long): Boolean {
        val today = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(date: Long): Boolean {
        val now = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == transactionDate.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(date: Long): Boolean {
        val now = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == transactionDate.get(Calendar.MONTH)
    }
}