package za.ac.iie.TallyUp.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategorySelected: (Category) -> Unit,
    private val onAddNewClicked: () -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val isAddNew = category.name == "Add New"
        val isSelected = position == selectedPosition

        holder.binding.categoryName.text = category.name

        // Set icon background color
        try {
            val color = Color.parseColor(category.color)
            holder.binding.categoryIcon.setColorFilter(color)
        } catch (e: Exception) {
            holder.binding.categoryIcon.setColorFilter(Color.parseColor("#E0E0E0"))
        }

        // Visual feedback for selection - you can add a stroke or change opacity
        holder.binding.root.alpha = if (isSelected) 1.0f else 0.7f

        holder.binding.root.setOnClickListener {
            if (isAddNew) {
                onAddNewClicked()
            } else {
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition

                // Update both items
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onCategorySelected(category)
            }
        }
    }

    override fun getItemCount() = categories.size
}