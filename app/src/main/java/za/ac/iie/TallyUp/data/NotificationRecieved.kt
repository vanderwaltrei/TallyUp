package za.ac.iie.TallyUp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import za.ac.iie.TallyUp.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Reminder"

        val builder = NotificationCompat.Builder(context, "tallyup_channel")
            .setSmallIcon(R.drawable.ic_notification) // use your own icon
            .setContentTitle("TallyUp Reminder")
            .setContentText(name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}