package xyz.jishnu.health.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object NotifChannels {
    const val FASTING_STICKY = "fasting_sticky"
    const val FASTING_REMINDERS = "fasting_reminders"
    const val WEIGHT_REMINDERS = "weight_reminders"

    const val STICKY_NOTIFICATION_ID = 1001
    const val FASTING_REMINDER_NOTIFICATION_ID = 1002
    const val WEIGHT_REMINDER_NOTIFICATION_ID = 1003

    fun ensure(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                FASTING_STICKY,
                "Active fast",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Ongoing notification while you're fasting."
                setShowBadge(false)
                setSound(null, null)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FASTING_REMINDERS,
                "Fasting reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily reminder to start your fast."
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                WEIGHT_REMINDERS,
                "Weigh-in reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily reminder to log your weight."
            },
        )
    }
}
