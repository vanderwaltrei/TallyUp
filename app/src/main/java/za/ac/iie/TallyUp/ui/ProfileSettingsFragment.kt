@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.core.content.edit

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
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null)

        if (email == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val key = "notifications_$email"
        val existing = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val entry = "$name|$time|$recurrence"
        existing.add(entry)

        prefs.edit {
            putStringSet(key, existing)
        }

        Toast.makeText(requireContext(), "Notification scheduled!", Toast.LENGTH_SHORT).show()
        println("Saved notification for $email: $entry")
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
