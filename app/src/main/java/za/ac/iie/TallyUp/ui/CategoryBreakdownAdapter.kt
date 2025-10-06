package za.ac.iie.TallyUp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.databinding.ItemCategoryBreakdownBinding
import za.ac.iie.TallyUp.models.BudgetCategory
import za.ac.iie.TallyUp.data.Transaction

/**
 * Adapter for displaying budget category breakdown in BudgetDashboardFragment
 * This is DIFFERENT from CategoryAdapter which is used for category selection
 */
class CategoryBreakdownAdapter(
    private val categories: List<BudgetCategory>,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCategoryBreakdownBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Setup expand/collapse functionality
            binding.btnExpand.setOnClickListener {
                val isExpanded = binding.editSection.visibility == View.VISIBLE
                binding.editSection.visibility = if (isExpanded) View.GONE else View.VISIBLE
                // Rotate arrow icon
                binding.btnExpand.rotation = if (isExpanded) 0f else 90f
            }

            binding.btnCancel.setOnClickListener {
                binding.editSection.visibility = View.GONE
                binding.btnExpand.rotation = 0f
                binding.editAmount.text.clear()
            }
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
        // Filter expense transactions for this category and sum amounts
        val spent = transactions
            .filter { it.type == "Expense" && it.category == category.name }
            .sumOf { it.amount }
        val remaining = category.budgeted - spent
        val percent = if (category.budgeted > 0.0) ((spent / category.budgeted) * 100.0).toInt() else 0

        holder.binding.categoryName.text = category.name
        holder.binding.categorySpent.text = "R${"%.2f".format(spent)} / R${"%.2f".format(category.budgeted)}"
        holder.binding.categoryRemaining.text = "R${"%.2f".format(remaining)} left"
        holder.binding.progressBar.progress = percent

        // Reset edit section visibility
        holder.binding.editSection.visibility = View.GONE
        holder.binding.btnExpand.rotation = 0f

        // Handle save button
        holder.binding.btnSave.setOnClickListener {
            val newAmount = holder.binding.editAmount.text.toString().toDoubleOrNull()
            if (newAmount != null) {
                // TODO: Implement saving the updated budget amount
                // You'll need to add a callback or use a ViewModel for this
                holder.binding.editSection.visibility = View.GONE
                holder.binding.btnExpand.rotation = 0f
            }
        }
    }

    override fun getItemCount() = categories.size
}