@file:Suppress("PackageName", "UnusedImport")

package za.ac.iie.TallyUp.ui

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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentManageNotificationsBinding
import za.ac.iie.TallyUp.data.NotificationReceiver
import za.ac.iie.TallyUp.ui.NotificationAdapter
import za.ac.iie.TallyUp.ui.NotificationItem

class ManageNotificationsFragment : Fragment() {

    private var _binding: FragmentManageNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotificationAdapter
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var key: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null)
        key = "notifications_$email"

        loadNotifications()
    }

    private fun loadNotifications() {
        val saved = prefs.getStringSet(key, emptySet()) ?: emptySet()

        val notifications = saved.mapNotNull {
            val parts = it.split("|")
            if (parts.size == 3) NotificationItem(parts[0], parts[1].toLong(), parts[2]) else null
        }

        adapter = NotificationAdapter(
            notifications.toMutableList(),
            onDelete = { itemToDelete ->
                cancelNotification(itemToDelete)
                val updated = saved.toMutableSet().apply {
                    remove("${itemToDelete.name}|${itemToDelete.time}|${itemToDelete.recurrence}")
                }
                prefs.edit { putStringSet(key, updated) }
                adapter.removeItem(itemToDelete)
                Toast.makeText(requireContext(), "Notification deleted", Toast.LENGTH_SHORT).show()
            },
            onEdit = { itemToEdit ->
                showEditDialog(itemToEdit)
            }
        )

        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifications.adapter = adapter
    }

    private fun showEditDialog(item: NotificationItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_schedule_notification, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDatePicker)
            .setView(dialogView)
            .create()

        val nameField = dialogView.findViewById<EditText>(R.id.etNotificationName)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerRecurrence)

        nameField.setText(item.name)

        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = item.time }
        datePicker.updateDate(
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )

        val recurrenceOptions = listOf("Never", "Weekly", "Monthly")
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, recurrenceOptions)
        spinner.setSelection(recurrenceOptions.indexOf(item.recurrence))

        val confirmButton = dialogView.findViewById<Button>(R.id.btnConfirmSchedule)
        confirmButton.setOnClickListener {
            val newName = nameField.text.toString()
            val newRecurrence = spinner.selectedItem.toString()
            val newCalendar = java.util.Calendar.getInstance().apply {
                set(datePicker.year, datePicker.month, datePicker.dayOfMonth, 9, 0)
            }
            val newTime = newCalendar.timeInMillis

            val saved = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

            // Replace old entry
            saved.remove("${item.name}|${item.time}|${item.recurrence}")
            saved.add("$newName|$newTime|$newRecurrence")
            prefs.edit { putStringSet(key, saved) }

            cancelNotification(item)
            scheduleNotification(newName, newTime, newRecurrence)

            Toast.makeText(requireContext(), "Notification updated!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadNotifications()
        }

        dialog.show()
    }

    private fun cancelNotification(item: NotificationItem) {
        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra("name", item.name)
            putExtra("recurrence", item.recurrence)
            putExtra("time", item.time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            item.time.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleNotification(name: String, time: Long, recurrence: String) {
        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra("name", name)
            putExtra("recurrence", recurrence)
            putExtra("time", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            time.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            when (recurrence) {
                "Never", "Monthly" -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                        !alarmManager.canScheduleExactAlarms()
                    ) {
                        Toast.makeText(
                            requireContext(),
                            "Exact alarms not allowed. Please enable in system settings.",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                }

                "Weekly" -> alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    time,
                    AlarmManager.INTERVAL_DAY * 7,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Failed to schedule alarm due to missing permission.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}