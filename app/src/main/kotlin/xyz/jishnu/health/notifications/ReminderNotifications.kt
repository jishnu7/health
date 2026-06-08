package xyz.jishnu.health.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import xyz.jishnu.health.MainActivity
import xyz.jishnu.health.R
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.WaterMath
import xyz.jishnu.health.domain.WaterReminders

/**
 * Centralised builders for the reminder notifications. Kept separate from the
 * receiver so that other entry points — the dev "Test notifications" buttons in
 * Settings, for instance — can post the same notifications without duplicating
 * their styling.
 */
object ReminderNotifications {

    fun buildFastingReminder(context: Context): Notification =
        NotificationCompat.Builder(context, NotifChannels.FASTING_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Time to start your fast")
            .setContentText("Begin your fasting window now to stay on track.")
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context, requestCode = 1001, route = null))
            .build()

    fun buildWeighInReminder(context: Context): Notification =
        NotificationCompat.Builder(context, NotifChannels.WEIGHT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Daily weigh-in")
            .setContentText("Take a moment to log today's weight.")
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context, requestCode = 1002, route = "weight"))
            .build()

    fun buildWaterReminder(
        context: Context,
        totalMl: Int,
        targetMl: Int,
        goalMl: Int,
        units: Units,
        windowIndex: Int,
    ): Notification {
        val remaining = (targetMl - totalMl).coerceAtLeast(0)
        val remainingFmt = WaterMath.fmtVolume(remaining, units)
        val totalFmt = WaterMath.fmtVolume(totalMl, units)
        val goalFmt = WaterMath.fmtVolume(goalMl, units)
        return NotificationCompat.Builder(context, NotifChannels.WATER_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Time to hydrate")
            .setContentText("${remainingFmt.value} ${remainingFmt.unit} short of your checkpoint.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You've had ${totalFmt.value} ${totalFmt.unit} of ${goalFmt.value} ${goalFmt.unit} today — grab a glass to catch up.",
                ),
            )
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context, requestCode = 1003, route = "water"))
            .build()
    }

    /**
     * Post-fast summary — fires after the user taps "End fast" from the
     * sticky notification, so they get an immediate confirmation + can jump
     * back to Home to see the result instead of being left with a quietly-
     * dismissed sticky.
     */
    fun buildFastEnded(context: Context, durationMs: Long, goalHours: Int): Notification {
        val totalMinutes = (durationMs.coerceAtLeast(0L) / 60_000L).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        val goalMet = h * 60 + m >= goalHours * 60
        val durationLabel = "${h}h ${m.toString().padStart(2, '0')}m"
        val title = if (goalMet) "Goal reached" else "Fast ended"
        val body = if (goalMet) {
            "$durationLabel fasted — past your ${goalHours}h goal."
        } else {
            "$durationLabel fasted — short of your ${goalHours}h goal."
        }
        return NotificationCompat.Builder(context, NotifChannels.FASTING_RESULTS)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body Tap to see the summary."))
            .setAutoCancel(true)
            .setContentIntent(openAppPending(context, requestCode = 1004, route = null))
            .build()
    }

    fun postFastEnded(context: Context, durationMs: Long, goalHours: Int) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.notify(
            NotifChannels.FAST_ENDED_NOTIFICATION_ID,
            buildFastEnded(context, durationMs, goalHours),
        )
    }

    fun fireFastingTest(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.notify(NotifChannels.FASTING_REMINDER_NOTIFICATION_ID, buildFastingReminder(context))
    }

    fun fireWeighInTest(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.notify(NotifChannels.WEIGHT_REMINDER_NOTIFICATION_ID, buildWeighInReminder(context))
    }

    fun fireWaterTest(context: Context, goalMl: Int, units: Units) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val windowIndex = 0
        val targetMl = WaterReminders.cumulativeTargetMl(windowIndex, goalMl)
        nm.notify(
            NotifChannels.WATER_REMINDER_NOTIFICATION_ID,
            buildWaterReminder(
                context = context,
                totalMl = 0,
                targetMl = targetMl,
                goalMl = goalMl,
                units = units,
                windowIndex = windowIndex,
            ),
        )
    }

    private fun openAppPending(context: Context, requestCode: Int, route: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(MainActivity.EXTRA_OPEN_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
