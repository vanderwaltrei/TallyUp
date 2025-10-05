package za.ac.iie.TallyUp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.model.Goal

class GoalAdapter(private val goals: List<Goal>) :
    RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.goalIcon)
        val name: TextView = itemView.findViewById(R.id.goalName)
        val amount: TextView = itemView.findViewById(R.id.goalAmount)
        val deadline: TextView = itemView.findViewById(R.id.goalDeadline)
        val percentage: TextView = itemView.findViewById(R.id.goalPercentage)
        val progress: ProgressBar = itemView.findViewById(R.id.goalProgress)
        val button: Button = itemView.findViewById(R.id.goalButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]

        // Bind data to the card
        holder.name.text = goal.name
        holder.amount.text = "R${goal.current.toInt()} / R${goal.target.toInt()}"
        holder.deadline.text = "Deadline: ${goal.deadline}"
        val percent = goal.progressPercent()
        holder.percentage.text = "$percent%"
        holder.progress.progress = percent

        // Set button text based on progress
        holder.button.text = if (goal.current >= goal.target) "Complete Goal" else "Add Money"
        holder.button.setOnClickListener {
            // Later: implement adding money or completing goal
        }

        // Optionally, set an icon based on goal state
        // holder.icon.setImageResource(R.drawable.character_happy)
    }

    override fun getItemCount(): Int = goals.size
}