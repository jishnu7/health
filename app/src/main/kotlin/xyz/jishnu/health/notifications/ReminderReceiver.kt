package xyz.jishnu.health.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.R
import xyz.jishnu.health.data.repo.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settings = settingsRepo.settings.first()
                val nm = context.getSystemService<NotificationManager>()
                when (action) {
                    ReminderScheduler.Kind.FastingStart.action -> {
                        if (settings.fastingReminderOn) {
                            nm?.notify(
                                NotifChannels.FASTING_REMINDER_NOTIFICATION_ID,
                                buildFastingReminder(context),
                            )
                            scheduler.scheduleNext(context, ReminderScheduler.Kind.FastingStart, settings.fastStartTime)
                        }
                    }
                    ReminderScheduler.Kind.WeighIn.action -> {
                        if (settings.weightReminderOn) {
                            nm?.notify(
                                NotifChannels.WEIGHT_REMINDER_NOTIFICATION_ID,
                                buildWeighInReminder(context),
                            )
                            scheduler.scheduleNext(context, ReminderScheduler.Kind.WeighIn, settings.reminderTime)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildFastingReminder(context: Context) =
        NotificationCompat.Builder(context, NotifChannels.FASTING_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Time to start your fast")
            .setContentText("Begin your fasting window now to stay on track.")
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context))
            .build()

    private fun buildWeighInReminder(context: Context) =
        NotificationCompat.Builder(context, NotifChannels.WEIGHT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Daily weigh-in")
            .setContentText("Take a moment to log today's weight.")
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context))
            .build()

    private fun openAppPending(context: Context): android.app.PendingIntent {
        val intent = Intent(context, xyz.jishnu.health.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
