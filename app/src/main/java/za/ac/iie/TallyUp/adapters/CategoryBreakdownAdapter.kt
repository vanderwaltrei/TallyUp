package za.ac.iie.TallyUp.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.databinding.ItemCategoryBreakdownBinding
import za.ac.iie.TallyUp.models.BudgetCategory
import za.ac.iie.TallyUp.data.Transaction

class CategoryBreakdownAdapter(
    private val categories: List<BudgetCategory>,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    // Callback interface for saving budget changes
    var onBudgetUpdated: ((categoryName: String, newAmount: Double) -> Unit)? = null

    inner class ViewHolder(val binding: ItemCategoryBreakdownBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                updateBudgetText(s.toString())
            }
        }

        init {
            // Setup expand/collapse functionality
            binding.btnExpand.setOnClickListener {
                val isExpanded = binding.editSection.visibility == View.VISIBLE
                binding.editSection.visibility = if (isExpanded) View.GONE else View.VISIBLE

                // Update button text based on state
                binding.btnExpand.text = if (isExpanded) "Edit Budget" else "Close"

                // Rotate arrow icon
                binding.btnExpand.rotation = if (isExpanded) 0f else 180f

                // Add/remove TextWatcher when section expands/collapses
                if (!isExpanded) {
                    binding.editAmount.addTextChangedListener(textWatcher)
                    binding.editAmount.requestFocus()
                } else {
                    binding.editAmount.removeTextChangedListener(textWatcher)
                    binding.editAmount.text?.clear()
                }
            }

            binding.btnCancel.setOnClickListener {
                binding.editSection.visibility = View.GONE
                binding.btnExpand.text = "Edit Budget"
                binding.btnExpand.rotation = 0f
                binding.editAmount.removeTextChangedListener(textWatcher)
                binding.editAmount.text?.clear()
                // Reset to original values
                updateCategoryTexts()
            }

            binding.btnSave.setOnClickListener {
                val newAmount = binding.editAmount.text.toString().toDoubleOrNull()
                if (newAmount != null && newAmount > 0) {
                    // Use the callback to save the budget
                    val category = categories[adapterPosition]
                    onBudgetUpdated?.invoke(category.name, newAmount)

                    binding.editSection.visibility = View.GONE
                    binding.btnExpand.text = "Edit Budget"
                    binding.btnExpand.rotation = 0f
                    binding.editAmount.removeTextChangedListener(textWatcher)

                    // Show success message or update UI accordingly
                }
            }
        }

        private fun updateBudgetText(enteredAmount: String) {
            val amount = enteredAmount.toDoubleOrNull() ?: 0.0
            val category = categories[adapterPosition]
            val spent = getSpentAmount(category.name)

            // Update the preview text in real-time
            binding.categorySpent.text = "R${"%.2f".format(spent)} / R${"%.2f".format(amount)}"
            binding.categoryRemaining.text = "R${"%.2f".format(amount - spent)} left"

            // Update progress bar in real-time
            val percent = if (amount > 0.0) ((spent / amount) * 100.0).toInt() else 0
            binding.progressBar.progress = percent
        }

        private fun updateCategoryTexts() {
            val category = categories[adapterPosition]
            val spent = getSpentAmount(category.name)
            val remaining = category.amount - spent
            val percent = if (category.amount > 0.0) ((spent / category.amount) * 100).toInt() else 0

            binding.categorySpent.text = "R${"%.2f".format(spent)} / R${"%.2f".format(category.amount)}"
            binding.categoryRemaining.text = "R${"%.2f".format(remaining)} left"
            binding.progressBar.progress = percent
        }

        private fun getSpentAmount(categoryName: String): Double {
            return transactions
                .filter { it.category == categoryName }
                .sumOf { it.amount }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBreakdownBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val spent = transactions.filter { it.category == category.name }.sumOf { it.amount }
        val remaining = category.amount - spent
        val percent = if (category.amount > 0.0) ((spent / category.amount) * 100).toInt() else 0

        holder.binding.categoryName.text = category.name
        holder.binding.categorySpent.text = "R${"%.2f".format(spent)} / R${"%.2f".format(category.amount)}"
        holder.binding.categoryRemaining.text = "R${"%.2f".format(remaining)} left"
        holder.binding.progressBar.progress = percent

        // Category icon based on category name
        holder.binding.categoryIcon.setImageResource(getCategoryIcon(category.name))

        // Set current budget amount in edit text as hint
        holder.binding.editAmount.hint = "%.2f".format(category.amount)

        // Reset edit section visibility
        holder.binding.editSection.visibility = View.GONE
        holder.binding.btnExpand.text = "Edit Budget"
        holder.binding.btnExpand.rotation = 0f

        // Clear any existing text
        holder.binding.editAmount.text?.clear()
    }

    override fun getItemCount() = categories.size

    private fun getSpentAmount(categoryName: String): Double {
        return transactions
            .filter { it.category == categoryName }
            .sumOf { it.amount }
    }

    // Category names = appropriate icons
    private fun getCategoryIcon(categoryName: String): Int {
        return when (categoryName.lowercase()) {
            "food" -> za.ac.iie.TallyUp.R.drawable.ic_coffee
            "transport" -> za.ac.iie.TallyUp.R.drawable.ic_car
            "books" -> za.ac.iie.TallyUp.R.drawable.ic_book_open
            "fun" -> za.ac.iie.TallyUp.R.drawable.ic_heart
            "shopping" -> za.ac.iie.TallyUp.R.drawable.ic_shopping_bag
            "salary" -> za.ac.iie.TallyUp.R.drawable.ic_coin
            "gift" -> za.ac.iie.TallyUp.R.drawable.ic_star
            "freelance" -> za.ac.iie.TallyUp.R.drawable.ic_trending_up
            "allowance" -> za.ac.iie.TallyUp.R.drawable.ic_piggy_bank
            else -> za.ac.iie.TallyUp.R.drawable.ic_circle // Default icon
        }
    }
}