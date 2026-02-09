@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentProfileSettingsBinding
import za.ac.iie.TallyUp.data.NotificationReceiver
import za.ac.iie.TallyUp.data.AppRepository
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

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnScheduleNotification.setOnClickListener {
            showNotificationDialog()
        }

        binding.btnManageNotifications.setOnClickListener {
            binding.profileFragmentContainer.visibility = View.VISIBLE
            childFragmentManager.beginTransaction()
                .replace(R.id.profile_fragment_container, ManageNotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showNotificationDialog() {
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

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? Your data will be saved locally.")
            .setPositiveButton("Yes") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * ✅ UPDATED: Secure Logout Function (Preserves Data)
     */
    @SuppressLint("UseKtx")
    private fun logoutUser() {
        try {
            // 1. Sign out from Firebase
            FirebaseAuth.getInstance().signOut()

            // 2. Clear ONLY the login session data from SharedPreferences (Who is logged in)
            // WE DO NOT CALL repository.clearUserData() HERE. This ensures the data file remains.
            val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Log.d("ProfileSettings", "✅ Logout successful - Session cleared, Data preserved.")
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // 3. Navigate back to LoginFragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()

        } catch (e: Exception) {
            Log.e("ProfileSettings", "❌ Error during logout: ${e.message}")
            Toast.makeText(requireContext(), "Logout failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleNotification(name: String, time: Long, recurrence: String) {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null) ?: return
        val key = "notifications_$email"
        val existing = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val entry = "$name|$time|$recurrence"
        existing.add(entry)
        prefs.edit { putStringSet(key, existing) }

        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra("name", name)
            putExtra("recurrence", recurrence)
            putExtra("time", time)
        }

        val requestCode = (name + time).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(requireContext(), "Exact alarms not allowed. Please enable in system settings.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            when (recurrence) {
                "Never", "Monthly" -> alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                "Weekly" -> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY * 7, pendingIntent)
            }
            Toast.makeText(requireContext(), "Notification scheduled!", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to schedule alarm due to missing permission.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.profileFragmentContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}