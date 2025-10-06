@file:Suppress("PackageName")

package za.ac.iie.TallyUp.adapters


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("DefaultLocale")
        fun bind(transaction: Transaction) {
            // Description and category
            val descriptionText = if (!transaction.description.isNullOrEmpty()) {
                "${transaction.description} - ${transaction.category}"
            } else {
                transaction.category
            }
            binding.transactionDescription.text = descriptionText

            // Amount with minus sign for expenses
            val formattedAmount = String.format("%.2f", transaction.amount)
            val amountText = if (transaction.type == "Expense") {
                "-R$formattedAmount"
            } else {
                "+R$formattedAmount"
            }
            binding.transactionAmount.text = amountText

            // Color coding for income (green) vs expense (red)
            val color = if (transaction.type == "Expense") {
                ContextCompat.getColor(binding.root.context, R.color.destructive)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.success)
            }
            binding.transactionAmount.setTextColor(color)

            // Format date from timestamp
            val date = Date(transaction.date)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.transactionDate.text = dateFormat.format(date)

            // Photo status based on photoUris
            val hasPhotos = transaction.photoUris.isNotEmpty()
            binding.photoStatus.text = if (hasPhotos) "📷 Photos attached" else "No photos"

            // Optional: Show/hide photo indicator
            binding.photoStatus.visibility = if (hasPhotos) View.VISIBLE else View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}