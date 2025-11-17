@file:Suppress("unused", "PackageName")

package za.ac.iie.TallyUp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val items: MutableList<NotificationItem>,
    private val onDelete: (NotificationItem) -> Unit,
    private val onEdit: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val date = formatter.format(Date(item.time))

        holder.binding.tvName.text = item.name
        holder.binding.tvDate.text = date
        holder.binding.tvRecurrence.text = item.recurrence

        holder.binding.btnEdit.setOnClickListener {
            onEdit(item)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDelete(item)
        }
    }

    fun removeItem(item: NotificationItem) {
        val index = items.indexOf(item)
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}