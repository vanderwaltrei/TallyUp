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
    private var categories: List<BudgetCategory>,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    // Callback for budget update events
    var onBudgetUpdated: ((categoryName: String, newAmount: Double) -> Unit)? = null

    inner class ViewHolder(val binding: ItemCategoryBreakdownBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateBudgetPreview(s.toString())
            }
        }

        init {
            // Expand or collapse when container is clicked
            binding.btnExpandContainer.setOnClickListener {
                val isExpanded = binding.editSection.visibility == View.VISIBLE
                toggleEditSection(!isExpanded)
            }

            // Cancel editing
            binding.btnCancel.setOnClickListener {
                toggleEditSection(false)
                updateCategoryTexts()
            }

            // Save new budget
            binding.btnSave.setOnClickListener {
                val newAmount = binding.editAmount.text.toString().toDoubleOrNull()
                if (newAmount != null && newAmount > 0) {
                    val category = categories[adapterPosition]
                    onBudgetUpdated?.invoke(category.name, newAmount)
                    toggleEditSection(false)
                }
            }
        }

        /** Opens or closes the edit section with animation and arrow rotation */
        private fun toggleEditSection(expand: Boolean) {
            if (expand) {
                binding.editSection.visibility = View.VISIBLE
                binding.btnExpandText.text = "Close"
                binding.btnExpandArrow.animate().rotation(90f).setDuration(200).start()
                binding.editAmount.addTextChangedListener(textWatcher)
                binding.editAmount.requestFocus()
                // Set current budget as hint when opening
                val category = categories[adapterPosition]
                binding.editAmount.hint = "%.2f".format(category.budgeted)
            } else {
                binding.editSection.visibility = View.GONE
                binding.btnExpandText.text = "Edit Budget"
                binding.btnExpandArrow.animate().rotation(0f).setDuration(200).start()
                binding.editAmount.removeTextChangedListener(textWatcher)
                binding.editAmount.text?.clear()
            }
        }

        /** Update preview text and progress as user types */
        private fun updateBudgetPreview(entered: String) {
            val newAmount = entered.toDoubleOrNull() ?: 0.0
            val category = categories[adapterPosition]
            val spent = getSpentAmount(category.name)

            binding.categorySpent.text =
                "R${"%.2f".format(spent)} / R${"%.2f".format(newAmount)}"
            binding.categoryRemaining.text =
                "R${"%.2f".format(newAmount - spent)} left"

            val percent = if (newAmount > 0) ((spent / newAmount) * 100).toInt() else 0
            binding.progressBar.progress = percent
        }

        /** Refresh category labels and progress after closing editor */
        private fun updateCategoryTexts() {
            val category = categories[adapterPosition]
            val spent = getSpentAmount(category.name)
            val remaining = category.budgeted - spent
            val percent = if (category.budgeted > 0) ((spent / category.budgeted) * 100).toInt() else 0

            binding.categorySpent.text =
                "R${"%.2f".format(spent)} / R${"%.2f".format(category.budgeted)}"
            binding.categoryRemaining.text = "R${"%.2f".format(remaining)} left"
            binding.progressBar.progress = percent
        }

        private fun getSpentAmount(categoryName: String): Double {
            return transactions.filter { it.category == categoryName }.sumOf { it.amount }
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
        val remaining = category.budgeted - spent
        val percent = if (category.budgeted > 0.0) ((spent / category.budgeted) * 100).toInt() else 0

        with(holder.binding) {
            categoryName.text = category.name
            categorySpent.text = "R${"%.2f".format(spent)} / R${"%.2f".format(category.budgeted)}"
            categoryRemaining.text = "R${"%.2f".format(remaining)} left"
            progressBar.progress = percent
            categoryIcon.setImageResource(getCategoryIcon(category.name))

            // Reset states
            editSection.visibility = View.GONE
            btnExpandText.text = "Edit Budget"
            btnExpandArrow.rotation = 0f
            editAmount.text?.clear()
            editAmount.hint = "%.2f".format(category.budgeted)
        }
    }

    override fun getItemCount() = categories.size

    // Add this method to update categories and refresh the adapter
    fun updateCategories(newCategories: List<BudgetCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

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
            else -> za.ac.iie.TallyUp.R.drawable.ic_circle
        }
    }
}