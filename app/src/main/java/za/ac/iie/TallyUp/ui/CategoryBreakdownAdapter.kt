@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.databinding.ItemCategoryBreakdownBinding
import za.ac.iie.TallyUp.models.BudgetCategory
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.R

@Suppress("unused")
class CategoryBreakdownAdapter(
    private val categories: List<BudgetCategory>,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    @SuppressLint("UseKtx", "SetTextI18n")
    inner class ViewHolder(val binding: ItemCategoryBreakdownBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Expand/Collapse functionality
            binding.btnExpandContainer.setOnClickListener {
                val isExpanded = binding.editSection.visibility == View.VISIBLE

                if (isExpanded) {
                    // Collapse
                    binding.editSection.visibility = View.GONE
                    binding.btnExpandContainer.text = "Edit Budget"
                    // Restore edit icon (Standard Android API)
                    binding.btnExpandContainer.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0)
                } else {
                    // Expand
                    binding.editSection.visibility = View.VISIBLE
                    binding.btnExpandContainer.text = "Close"
                    // Remove icon (Standard Android API: 0 means no drawable)
                    binding.btnExpandContainer.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            }

            // Cancel button inside the edit section
            binding.btnCancel.setOnClickListener {
                binding.editSection.visibility = View.GONE
                binding.btnExpandContainer.text = "Edit Budget"
                // Restore edit icon
                binding.btnExpandContainer.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0)
                binding.editAmount.text.clear()
            }

            // Save button
            binding.btnSave.setOnClickListener {
                val newAmount = binding.editAmount.text.toString().toDoubleOrNull()
                if (newAmount != null) {
                    // Logic to save the budget would go here

                    // Reset UI
                    binding.editSection.visibility = View.GONE
                    binding.btnExpandContainer.text = "Edit Budget"
                    // Restore edit icon
                    binding.btnExpandContainer.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0)
                    binding.editAmount.text.clear()
                }
            }
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

        // Calculate spent and remaining amounts
        val spent = transactions
            .filter { it.type == "Expense" && it.category == category.name }
            .sumOf { it.amount }

        val remaining = category.budgeted - spent
        val percent = if (category.budgeted > 0.0) ((spent / category.budgeted) * 100.0).toInt() else 0

        // Bind data
        holder.binding.categoryName.text = category.name
        holder.binding.categorySpentAmount.text = "R ${"%.2f".format(spent)}"
        holder.binding.categorySubtitle.text = "Budget: R ${"%.2f".format(category.budgeted)}"
        holder.binding.categoryRemaining.text = "R ${"%.2f".format(remaining)} left"
        holder.binding.progressBar.progress = percent
        holder.binding.categoryPercentage.text = "$percent% used"

        // Reset UI each time (to prevent recycled state issues)
        holder.binding.editSection.visibility = View.GONE
        holder.binding.btnExpandContainer.text = "Edit Budget"
        // Ensure icon is visible by default
        holder.binding.btnExpandContainer.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0)
    }

    override fun getItemCount() = categories.size
}