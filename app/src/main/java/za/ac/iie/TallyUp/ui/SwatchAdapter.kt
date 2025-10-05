package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R

class SwatchAdapter(
    private val colors: List<String>,
    private val onSwatchSelected: (String) -> Unit
) : RecyclerView.Adapter<SwatchAdapter.SwatchViewHolder>() {

    private var selectedColor: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_swatch, parent, false)
        return SwatchViewHolder(view)
    }

    override fun getItemCount(): Int = colors.size

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: SwatchViewHolder, position: Int) {
        val colorHex = colors[position]
        holder.bind(colorHex, colorHex == selectedColor)
        holder.itemView.setOnClickListener {
            selectedColor = colorHex
            onSwatchSelected(colorHex)
            notifyDataSetChanged()
        }
    }

    inner class SwatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val swatchView: View = view.findViewById(R.id.swatchColor)

        @SuppressLint("UseKtx")
        fun bind(colorHex: String, isSelected: Boolean) {
            val drawable = swatchView.background as GradientDrawable
            drawable.setColor(Color.parseColor(colorHex))

            if (isSelected) {
                drawable.setStroke(4, Color.BLACK) // highlight selected
            } else {
                drawable.setStroke(0, Color.TRANSPARENT)
            }
        }
    }
}
