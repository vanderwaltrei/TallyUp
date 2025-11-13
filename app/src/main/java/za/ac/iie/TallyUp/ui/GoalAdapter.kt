package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.models.Goal
import za.ac.iie.TallyUp.utils.CharacterManager

class GoalAdapter(
    private val goals: List<Goal>,
    private val onAddMoneyClicked: (Goal) -> Unit,
    private val onCompleteGoalClicked: (Goal) -> Unit,
    private val onEditGoalClicked: (Goal) -> Unit,  // NEW
    private val onDeleteGoalClicked: (Goal) -> Unit  // NEW
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    private lateinit var context: Context

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.goalName)
        val amount: TextView = itemView.findViewById(R.id.goalAmount)
        val deadline: TextView = itemView.findViewById(R.id.goalDeadline)
        val percentage: TextView = itemView.findViewById(R.id.goalPercentage)
        val progress: ProgressBar = itemView.findViewById(R.id.goalProgress)
        val button: Button = itemView.findViewById(R.id.goalButton)
        val icon: ImageView = itemView.findViewById(R.id.goalIcon)
        val editButton: ImageButton = itemView.findViewById(R.id.goalEditButton)  // NEW
        val deleteButton: ImageButton = itemView.findViewById(R.id.goalDeleteButton)  // NEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]

        // Set character icon based on user's selection
        val characterDrawable = CharacterManager.getCharacterDrawable(context)
        holder.icon.setImageResource(characterDrawable)

        holder.name.text = goal.name

        // Show current / target amount (target is max)
        holder.amount.text = "R${goal.current.toInt()} / R${goal.target.toInt()}"

        // Show minimum if it's greater than 0
        if (goal.minimum > 0) {
            holder.amount.text = "R${goal.current.toInt()} / R${goal.target.toInt()} (Min: R${goal.minimum.toInt()})"
        }

        holder.deadline.text = "Deadline: ${normalizeDeadline(goal.deadline)}"
        holder.percentage.text = "${goal.progressPercent()}%"
        holder.progress.progress = goal.progressPercent()

        if (goal.current >= goal.target) {
            holder.button.text = "Complete Goal"
            holder.button.setOnClickListener {
                onCompleteGoalClicked(goal)
            }
        } else {
            holder.button.text = "Add Money"
            holder.button.setOnClickListener {
                onAddMoneyClicked(goal)
            }
        }

        // NEW: Edit button click
        holder.editButton.setOnClickListener {
            onEditGoalClicked(goal)
        }

        // NEW: Delete button click
        holder.deleteButton.setOnClickListener {
            onDeleteGoalClicked(goal)
        }
    }

    override fun getItemCount(): Int = goals.size

    private fun normalizeDeadline(input: String?): String {
        if (input.isNullOrBlank()) return ""

        val parts = input.trim().split("\\s+".toRegex())
        val number = parts.getOrNull(0)?.toIntOrNull() ?: return input

        val suffix = if (number == 1) "Month" else "Months"
        return "$number $suffix"
    }
}