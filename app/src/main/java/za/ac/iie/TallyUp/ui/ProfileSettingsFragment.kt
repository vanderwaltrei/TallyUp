@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentProfileSettingsBinding
import java.util.*

class ProfileSettingsFragment : Fragment() {

    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Log Out Button
        binding.logoutButton.setOnClickListener {
            logoutUser()
        }

        //Schedule Notification Button
        binding.btnScheduleNotification.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_schedule_notification, null)
            val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDatePicker)
                .setView(dialogView)
                .create()

            val spinner = dialogView.findViewById<Spinner>(R.id.spinnerRecurrence)
            val recurrenceOptions = listOf("Never", "Weekly", "Monthly")
            spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, recurrenceOptions)

            val confirmButton = dialogView.findViewById<Button>(R.id.btnConfirmSchedule)
            confirmButton.setOnClickListener {
                val name = dialogView.findViewById<EditText>(R.id.etNotificationName).text.toString()
                val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
                val recurrence = spinner.selectedItem.toString()

                val calendar = Calendar.getInstance().apply {
                    set(datePicker.year, datePicker.month, datePicker.dayOfMonth, 9, 0)
                }

                scheduleNotification(name, calendar.timeInMillis, recurrence)
                dialog.dismiss()
            }

            dialog.show()
        }

        //Manage Notifications Button
        binding.btnManageNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Manage Notifications clicked", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to a fragment or show a list of saved notifications
        }
    }

    private fun scheduleNotification(name: String, time: Long, recurrence: String) {
        // Placeholder: we'll wire this up with AlarmManager next
        println("Scheduled: $name at $time recurring: $recurrence")
    }

    @SuppressLint("UseKtx")
    private fun logoutUser() {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        prefs.edit().remove("loggedInEmail").apply()

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
