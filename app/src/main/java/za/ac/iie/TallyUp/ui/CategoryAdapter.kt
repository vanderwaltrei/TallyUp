package za.ac.iie.TallyUp.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.data.Category
import za.ac.iie.TallyUp.utils.CharacterManager
import android.os.Bundle

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategorySelected: (Category) -> Unit,
    private val onAddNewClicked: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_ADD_NEW = 1
    }

    override fun getItemCount(): Int = categories.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position < categories.size) TYPE_CATEGORY else TYPE_ADD_NEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_CATEGORY) {
            val view = inflater.inflate(R.layout.item_category, parent, false)
            CategoryViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_add_category, parent, false)
            AddNewViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CategoryViewHolder) {
            val category = categories[position]
            holder.bind(category)
            holder.itemView.setOnClickListener { onCategorySelected(category) }
        } else if (holder is AddNewViewHolder) {
            holder.itemView.setOnClickListener { onAddNewClicked() }
        }
    }

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.categoryIcon)
        private val label: TextView = view.findViewById(R.id.categoryName) // Changed from categoryLabel to categoryName

        fun bind(category: Category) {
            label.text = category.name

            // Set dynamic background color instead of using the static circle_background
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(category.color))
            }
            icon.background = circle

            // Use the selected character instead of hardcoded character_happy
            val characterDrawable = CharacterManager.getCharacterDrawable(itemView.context)
            icon.setImageResource(characterDrawable)
        }
    }

    inner class AddNewViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        // You can add references to the addIcon and addLabel if needed
        // private val addIcon: ImageView = view.findViewById(R.id.addIcon)
        // private val addLabel: TextView = view.findViewById(R.id.addLabel)
    }
}