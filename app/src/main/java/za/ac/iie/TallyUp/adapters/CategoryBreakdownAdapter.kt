@file:Suppress("PackageName")

package za.ac.iie.TallyUp.adapters

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.ItemCategoryBreakdownBinding
import za.ac.iie.TallyUp.models.BudgetCategory
import za.ac.iie.TallyUp.data.Transaction

@Suppress("DEPRECATION")
class CategoryBreakdownAdapter(
    private var categories: List<BudgetCategory>,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    // Callback for budget update events
    var onBudgetUpdated: ((categoryName: String, newAmount: Double) -> Unit)? = null

    @SuppressLint("UseKtx")
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

        /** Opens or closes the edit section and updates the Button state */
        @SuppressLint("SetTextI18n")
        private fun toggleEditSection(expand: Boolean) {
            if (expand) {
                binding.editSection.visibility = View.VISIBLE

                // Update the MaterialButton directly
                binding.btnExpandContainer.text = "Close"
                // Optional: Change icon to a close icon if you have one, or remove it
                // binding.btnExpandContainer.setIconResource(R.drawable.ic_close)

                binding.editAmount.addTextChangedListener(textWatcher)
                binding.editAmount.requestFocus()

                // Set current budget as hint when opening
                val category = categories[adapterPosition]
                binding.editAmount.hint = "%.2f".format(category.budgeted)
            } else {
                binding.editSection.visibility = View.GONE

                // Reset the MaterialButton
                binding.btnExpandContainer.text = "Edit Budget"
                // binding.btnExpandContainer.setIconResource(R.drawable.ic_edit)

                binding.editAmount.removeTextChangedListener(textWatcher)
                binding.editAmount.text?.clear()
            }
        }

        /** Update preview text and progress as user types */
        @SuppressLint("SetTextI18n")
        private fun updateBudgetPreview(entered: String) {
            val newAmount = entered.toDoubleOrNull() ?: 0.0
            val category = categories[adapterPosition]
            val spent = getSpentAmount(category.name)

            // Update visible amounts
            binding.categorySpentAmount.text = "R ${"%.2f".format(spent)}"
            binding.categorySubtitle.text = "Budget: R ${"%.2f".format(newAmount)}"

            val remaining = newAmount - spent
            binding.categoryRemaining.text = "R ${"%.2f".format(remaining)} left"

            val percent = if (newAmount > 0) ((spent / newAmount) * 100).toInt() else 0
            binding.progressBar.progress = percent
            binding.categoryPercentage.text = "$percent% used"
        }

        /** Refresh category labels and progress after closing editor */
        @SuppressLint("SetTextI18n")
        private fun updateCategoryTexts() {
            val category = categories[adapterPosition]
            val spent = getSpentAmount(category.name)
            val remaining = category.budgeted - spent
            val percent = if (category.budgeted > 0) ((spent / category.budgeted) * 100).toInt() else 0

            binding.categorySpentAmount.text = "R ${"%.2f".format(spent)}"
            binding.categorySubtitle.text = "Budget: R ${"%.2f".format(category.budgeted)}"
            binding.categoryRemaining.text = "R ${"%.2f".format(remaining)} left"
            binding.progressBar.progress = percent
            binding.categoryPercentage.text = "$percent% used"
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val spent = transactions.filter { it.category == category.name }.sumOf { it.amount }
        val remaining = category.budgeted - spent
        val percent = if (category.budgeted > 0.0) ((spent / category.budgeted) * 100).toInt() else 0

        with(holder.binding) {
            categoryName.text = category.name

            // Fixed ID references based on XML
            categorySpentAmount.text = "R ${"%.2f".format(spent)}"
            categorySubtitle.text = "Budget: R ${"%.2f".format(category.budgeted)}"

            categoryRemaining.text = "R ${"%.2f".format(remaining)} left"
            categoryPercentage.text = "$percent% used"

            progressBar.progress = percent
            categoryIcon.setImageResource(getCategoryIcon(category.name))

            // Reset states
            editSection.visibility = View.GONE
            btnExpandContainer.text = "Edit Budget"
            // btnExpandContainer.setIconResource(R.drawable.ic_edit) // Optional ensure icon reset

            editAmount.text?.clear()
            editAmount.hint = "%.2f".format(category.budgeted)
        }
    }

    override fun getItemCount() = categories.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateCategories(newCategories: List<BudgetCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    private fun getCategoryIcon(categoryName: String): Int {
        return when (categoryName.lowercase()) {
            "food" -> R.drawable.ic_coffee
            "transport" -> R.drawable.ic_car
            "books" -> R.drawable.ic_book_open
            "fun" -> R.drawable.ic_heart
            "shopping" -> R.drawable.ic_shopping_bag
            "salary" -> R.drawable.ic_coin
            "gift" -> R.drawable.ic_star
            "freelance" -> R.drawable.ic_trending_up
            "allowance" -> R.drawable.ic_piggy_bank
            else -> R.drawable.ic_circle
        }
    }
}