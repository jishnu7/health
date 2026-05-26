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
import xyz.jishnu.health.data.repo.WaterRepository
import xyz.jishnu.health.domain.WaterMath
import xyz.jishnu.health.domain.WaterReminders
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var waterRepo: WaterRepository
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
                    ReminderScheduler.ACTION_WATER -> {
                        if (settings.waterReminderOn) {
                            val windowIndex = intent.getIntExtra(ReminderScheduler.EXTRA_WATER_WINDOW, 0)
                            val zone = ZoneId.systemDefault()
                            val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
                            val dayEnd = dayStart + 86_400_000L
                            val totalMl = waterRepo.totalInRange(dayStart, dayEnd).first()
                            val targetMl = WaterReminders.cumulativeTargetMl(windowIndex, settings.waterGoalMl)
                            if (totalMl < targetMl) {
                                nm?.notify(
                                    NotifChannels.WATER_REMINDER_NOTIFICATION_ID,
                                    buildWaterReminder(context, totalMl, targetMl, settings.waterGoalMl, settings.units, windowIndex),
                                )
                            }
                            scheduler.scheduleNextWater(context)
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
            .setContentIntent(openAppPending(context, requestCode = 1001, route = null))
            .build()

    private fun buildWeighInReminder(context: Context) =
        NotificationCompat.Builder(context, NotifChannels.WEIGHT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Daily weigh-in")
            .setContentText("Take a moment to log today's weight.")
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context, requestCode = 1002, route = "weight"))
            .build()

    private fun buildWaterReminder(
        context: Context,
        totalMl: Int,
        targetMl: Int,
        goalMl: Int,
        units: xyz.jishnu.health.data.model.Units,
        windowIndex: Int,
    ): android.app.Notification {
        val remaining = (targetMl - totalMl).coerceAtLeast(0)
        val remainingFmt = WaterMath.fmtVolume(remaining, units)
        val totalFmt = WaterMath.fmtVolume(totalMl, units)
        val goalFmt = WaterMath.fmtVolume(goalMl, units)
        val window = WaterReminders.windows.getOrNull(windowIndex)
        val cumPct = window?.cumulativePct ?: 0
        return NotificationCompat.Builder(context, NotifChannels.WATER_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Time to hydrate")
            .setContentText("${remainingFmt.value} ${remainingFmt.unit} short of your $cumPct% checkpoint.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've had ${totalFmt.value} ${totalFmt.unit} of ${goalFmt.value} ${goalFmt.unit} today. " +
                        "By now you'd typically be at $cumPct% — grab a glass to catch up.",
                ),
            )
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context, requestCode = 1003, route = "water"))
            .build()
    }

    private fun openAppPending(
        context: Context,
        requestCode: Int,
        route: String?,
    ): android.app.PendingIntent {
        val intent = Intent(context, xyz.jishnu.health.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(xyz.jishnu.health.MainActivity.EXTRA_OPEN_ROUTE, route)
        }
        return android.app.PendingIntent.getActivity(
            context, requestCode, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
