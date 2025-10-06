package za.ac.iie.TallyUp.adapters

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

        fun bind(transaction: Transaction) {
            // Description and type
            binding.transactionDescription.text = "${transaction.description} - ${transaction.type}"

            // Amount with minus sign for expenses
            val formattedAmount = String.format("%.2f", transaction.amount)
            val amountText = if (transaction.type == "Expense") {
                "-R $formattedAmount"
            } else {
                "R $formattedAmount"
            }
            binding.transactionAmount.text = amountText


            // Optional color tint
            val color = if (transaction.type == "Expense") {
                ContextCompat.getColor(binding.root.context, R.color.destructive)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.success)
            }
            binding.transactionAmount.setTextColor(color)

            // Date
            binding.transactionDate.text = transaction.dateFormatted

            // Photo status
            binding.photoStatus.text = if (transaction.photos?.isNotEmpty() == true) "Photos attached" else "No photos"
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