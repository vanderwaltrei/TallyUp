@file:Suppress("PackageName")

package za.ac.iie.TallyUp.adapters  // ✅ correct package

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
    private var transactions: List<Transaction>  // ✅ var so updateTransactions can replace it
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    // Callback invoked when the user saves a new budget amount for a category
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

            // Cancel collapses without saving
            binding.btnCancel.setOnClickListener {
                toggleEditSection(false)
                binding.editAmount.text.clear()
            }

            // Save fires the callback and collapses
            binding.btnSave.setOnClickListener {
                val newAmount = binding.editAmount.text.toString().toDoubleOrNull()
                if (newAmount != null && newAmount >= 0) {
                    val category = categories[bindingAdapterPosition]
                    onBudgetUpdated?.invoke(category.name, newAmount)
                    toggleEditSection(false)
                    binding.editAmount.text.clear()
                } else {
                    binding.editAmount.error = "Enter a valid amount"
                }
            }
        }

        fun toggleEditSection(expand: Boolean) {
            if (expand) {
                binding.editSection.visibility = View.VISIBLE
                binding.btnExpandContainer.text = "Close"
                binding.btnExpandContainer.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                binding.editAmount.addTextChangedListener(textWatcher)
            } else {
                binding.editSection.visibility = View.GONE
                binding.btnExpandContainer.text = "Edit Budget"
                binding.btnExpandContainer.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_edit, 0, 0, 0
                )
                binding.editAmount.removeTextChangedListener(textWatcher)
            }
        }

        private fun updateBudgetPreview(input: String) {
            // Optional: live preview logic can go here
        }
    }

    // ─────────────────────────────────────────────
    // RecyclerView overrides
    // ─────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBreakdownBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]

        val spent = transactions
            .filter { it.type == "Expense" && it.category == category.name }
            .sumOf { it.amount }

        val remaining = category.budgeted - spent
        val percent = if (category.budgeted > 0.0) {
            ((spent / category.budgeted) * 100.0).toInt().coerceIn(0, 100)
        } else 0

        holder.binding.categoryName.text        = category.name
        holder.binding.categorySpentAmount.text = "R ${"%.2f".format(spent)}"
        holder.binding.categorySubtitle.text    = "Budget: R ${"%.2f".format(category.budgeted)}"
        holder.binding.categoryRemaining.text   = "R ${"%.2f".format(remaining)} left"
        holder.binding.progressBar.progress     = percent
        holder.binding.categoryPercentage.text  = "$percent% used"

        // Color remaining text based on status
        val context = holder.binding.root.context
        val remainingColor = when {
            remaining < 0  -> context.getColor(R.color.destructive)
            percent >= 85  -> context.getColor(R.color.warning)
            else           -> context.getColor(R.color.success)
        }
        holder.binding.categoryRemaining.setTextColor(remainingColor)

        // Always reset expand state to avoid recycled-view glitches
        holder.toggleEditSection(false)
        holder.binding.editAmount.text.clear()
    }

    override fun getItemCount() = categories.size

    // ─────────────────────────────────────────────
    // Public update methods (used by BudgetDashboardFragment filter chips)
    // ─────────────────────────────────────────────

    /**
     * Replace the displayed category list and refresh the RecyclerView.
     * Called whenever a filter chip is selected in BudgetDashboardFragment.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateCategories(newCategories: List<BudgetCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    /**
     * Update the transaction list used to calculate per-category spending,
     * then refresh the RecyclerView.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}