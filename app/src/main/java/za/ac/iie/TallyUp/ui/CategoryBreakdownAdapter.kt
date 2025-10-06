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


 // Adapter is for displaying budget category breakdown in BudgetDashboardFragment. This is different from CategoryAdapter which is used for category selection

@Suppress("unused")
class CategoryBreakdownAdapter(
    private val categories: List<BudgetCategory>,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    @SuppressLint("UseKtx", "SetTextI18n")
    inner class ViewHolder(val binding: ItemCategoryBreakdownBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            val expandContainer = binding.btnExpandContainer
            val expandText = binding.btnExpandText
            val expandArrow = binding.btnExpandArrow

            // Expand/Collapse functionality
            expandContainer.setOnClickListener {
                val isExpanded = binding.editSection.visibility == View.VISIBLE
                binding.editSection.visibility = if (isExpanded) View.GONE else View.VISIBLE

                // Update button text and arrow rotation
                expandText.text = if (isExpanded) "Edit Budget" else "Close"
                expandArrow.animate()
                    .rotation(if (isExpanded) 0f else 90f)
                    .setDuration(200)
                    .start()
            }

            // Cancel button
            binding.btnCancel.setOnClickListener {
                binding.editSection.visibility = View.GONE
                expandText.text = "Edit Budget"
                expandArrow.animate().rotation(0f).setDuration(200).start()
                binding.editAmount.text.clear()
            }

            // Save button
            binding.btnSave.setOnClickListener {
                val newAmount = binding.editAmount.text.toString().toDoubleOrNull()
                if (newAmount != null) {
                    binding.editSection.visibility = View.GONE
                    expandText.text = "Edit Budget"
                    expandArrow.animate().rotation(0f).setDuration(200).start()
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
        holder.binding.categorySpent.text = "R${"%.2f".format(spent)} / R${"%.2f".format(category.budgeted)}"
        holder.binding.categoryRemaining.text = "R${"%.2f".format(remaining)} left"
        holder.binding.progressBar.progress = percent

        // Reset UI each time (to prevent recycled state issues)
        holder.binding.editSection.visibility = View.GONE
        holder.binding.btnExpandText.text = "Edit Budget"
        holder.binding.btnExpandArrow.rotation = 0f
    }

    override fun getItemCount() = categories.size
}
