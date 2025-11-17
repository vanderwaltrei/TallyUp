@file:Suppress("PackageName")

package za.ac.iie.TallyUp.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import za.ac.iie.TallyUp.R
import java.util.*

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Reminder"
        val recurrence = intent.getStringExtra("recurrence") ?: "Never"
        val originalTime = intent.getLongExtra("time", 0L)

        // Check POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return // Permission not granted — skip notification
        }

        // Show the notification safely
        try {
            val builder = NotificationCompat.Builder(context, "tallyup_channel")
                .setSmallIcon(R.drawable.ic_alert_triangle)
                .setContentTitle("TallyUp Reminder")
                .setContentText(name)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            NotificationManagerCompat.from(context).notify(
                (name + originalTime).hashCode(), // Stable request code
                builder.build()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            return
        }

        // Reschedule if monthly
        if (recurrence == "Monthly") {
            val nextMonth = Calendar.getInstance().apply {
                timeInMillis = originalTime
                add(Calendar.MONTH, 1)
            }

            val newTime = nextMonth.timeInMillis

            val newIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("name", name)
                putExtra("recurrence", "Monthly")
                putExtra("time", newTime)
            }

            val requestCode = (name + newTime).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Check exact alarm permission (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                return // Can't schedule exact alarms — skip rescheduling
            }

            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, newTime, pendingIntent)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}