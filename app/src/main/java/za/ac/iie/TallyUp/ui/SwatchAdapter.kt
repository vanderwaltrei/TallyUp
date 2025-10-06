package za.ac.iie.TallyUp.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R

class SwatchAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<SwatchAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val swatchColor: View = view.findViewById(R.id.swatchColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_swatch, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        val isSelected = position == selectedPosition

        try {
            val parsedColor = Color.parseColor(color)
            holder.swatchColor.setBackgroundColor(parsedColor)

            // Add border for selected item
            if (isSelected) {
                val drawable = GradientDrawable()
                drawable.setColor(parsedColor)
                drawable.setStroke(8, Color.BLACK)
                drawable.cornerRadius = 24f
                holder.swatchColor.background = drawable
            } else {
                val drawable = GradientDrawable()
                drawable.setColor(parsedColor)
                drawable.cornerRadius = 24f
                holder.swatchColor.background = drawable
            }
        } catch (e: Exception) {
            holder.swatchColor.setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        holder.view.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onColorSelected(color)
        }
    }

    override fun getItemCount() = colors.size
}