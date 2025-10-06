package za.ac.iie.TallyUp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.databinding.ItemCategoryBinding

class CategoryAdapter(
    val categories: MutableList<Category>,
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

        // Set icon color safely using KTX
        holder.binding.categoryIcon.setColorFilter(safeParseColor(category.color))

        // Selection feedback
        holder.binding.root.alpha = if (isSelected) 1.0f else 0.7f

        holder.binding.root.setOnClickListener {
            if (isAddNew) {
                onAddNewClicked()
            } else {
                val previousPosition = selectedPosition
                selectedPosition = holder.bindingAdapterPosition

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onCategorySelected(category)
            }
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newList: List<Category>) {
        val oldSize = categories.size
        categories.clear()
        categories.addAll(newList)
        notifyItemRangeChanged(0, newList.size)
    }

    @ColorInt
    private fun safeParseColor(colorString: String): Int {
        return try {
            colorString.toColorInt()
        } catch (e: IllegalArgumentException) {
            "#E0E0E0".toColorInt()
        }
    }
}