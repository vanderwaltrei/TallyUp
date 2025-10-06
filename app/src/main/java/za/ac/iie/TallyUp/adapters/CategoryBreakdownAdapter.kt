package za.ac.iie.TallyUp.ui.budget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.databinding.ItemCategoryBreakdownBinding
import za.ac.iie.TallyUp.models.BudgetCategory
import za.ac.iie.TallyUp.data.Transaction

class CategoryBreakdownAdapter(
    private val categories: List<BudgetCategory>,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCategoryBreakdownBinding) :
        RecyclerView.ViewHolder(binding.root)

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
        // Removed categoryProgress if it doesn't exist in your layout
        holder.binding.categorySpent.text = "R${"%.2f".format(spent)} of R${"%.2f".format(category.amount)}"
        holder.binding.categoryRemaining.text = "R${"%.2f".format(remaining)} left"
        holder.binding.progressBar.progress = percent
    }

    override fun getItemCount() = categories.size
}