package za.ac.iie.TallyUp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import za.ac.iie.TallyUp.R
import java.util.*

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Reminder"
        val recurrence = intent.getStringExtra("recurrence") ?: "Never"
        val originalTime = intent.getLongExtra("time", 0L)

        // Show the notification
        val builder = NotificationCompat.Builder(context, "tallyup_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("TallyUp Reminder")
            .setContentText(name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
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

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                newTime.toInt(),
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, newTime, pendingIntent)
        }
    }
}