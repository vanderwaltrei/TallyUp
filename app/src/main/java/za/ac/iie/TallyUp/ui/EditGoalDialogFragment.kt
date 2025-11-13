package za.ac.iie.TallyUp.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.models.Goal

class EditGoalDialogFragment(
    private val goal: Goal,
    private val onGoalUpdated: (Goal) -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_goal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameInput = view.findViewById<TextInputEditText>(R.id.editGoalNameInput)
        val minAmountInput = view.findViewById<TextInputEditText>(R.id.editGoalMinAmountInput)
        val maxAmountInput = view.findViewById<TextInputEditText>(R.id.editGoalMaxAmountInput)
        val deadlineInput = view.findViewById<TextInputEditText>(R.id.editGoalDeadlineInput)
        val saveButton = view.findViewById<View>(R.id.editGoalSaveButton)
        val cancelButton = view.findViewById<View>(R.id.editGoalCancelButton)

        // Pre-fill with existing values
        nameInput.setText(goal.name)
        minAmountInput.setText(goal.minimum.toString())
        maxAmountInput.setText(goal.target.toString())
        deadlineInput.setText(goal.deadline)

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val minAmount = minAmountInput.text.toString().toDoubleOrNull()
            val maxAmount = maxAmountInput.text.toString().toDoubleOrNull()
            val deadline = deadlineInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a goal name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minAmount == null || maxAmount == null) {
                Toast.makeText(requireContext(), "Please enter valid amounts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minAmount > maxAmount) {
                Toast.makeText(requireContext(), "Minimum amount cannot be greater than maximum", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (deadline.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a deadline", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create updated goal
            val updatedGoal = goal.copy(
                name = name,
                minimum = minAmount,
                target = maxAmount,
                deadline = deadline
            )

            onGoalUpdated(updatedGoal)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}