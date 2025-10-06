@file:Suppress("PackageName")

package za.ac.iie.TallyUp.models
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import za.ac.iie.TallyUp.data.TransactionDao


class TransactionViewModelFactory(private val transactionDao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
