package xyz.jishnu.health.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import xyz.jishnu.health.MainActivity
import xyz.jishnu.health.R
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.domain.StageCalculator
import xyz.jishnu.health.domain.TimeMath

object FastingNotificationBuilder {

    fun build(
        context: Context,
        session: FastingSessionEntity,
        nowMs: Long = System.currentTimeMillis(),
    ): android.app.Notification {
        val elapsedMs = (nowMs - session.startMs).coerceAtLeast(0L)
        val elapsed = TimeMath.fmtDuration(elapsedMs)
        val stage = StageCalculator.stageFor(elapsedMs / 3_600_000.0)
        val stageIdx = Stages.all.indexOf(stage) + 1

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val endPending = PendingIntent.getBroadcast(
            context,
            1,
            NotificationActionReceiver.intent(context, NotificationActionReceiver.ACTION_END_FAST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = "Stage $stageIdx · ${stage.name}"
        val text = "${elapsed.hours}h ${elapsed.mm}m elapsed"

        return NotificationCompat.Builder(context, NotifChannels.FASTING_STICKY)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setColor(primaryColor(context))
            .setContentIntent(openAppPending)
            .addAction(0, "End Fast", endPending)
            .build()
    }
}

private fun primaryColor(context: Context): Int {
    val isDark = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    return if (isDark) Color.parseColor("#7DD3A8") else Color.parseColor("#2A4D3E")
}
