package xyz.jishnu.health.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
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
        useLiveUpdate: Boolean = true,
        nowMs: Long = System.currentTimeMillis(),
    ): Notification {
        val elapsedMs = (nowMs - session.startMs).coerceAtLeast(0L)
        val elapsed = TimeMath.fmtDuration(elapsedMs)
        val goalMs = session.goalHours * 3_600_000L
        val rawProgress = elapsedMs.toDouble() / goalMs
        val progress = rawProgress.coerceIn(0.0, 1.0).toFloat()
        val progressPercent = (progress * 100).toInt().coerceIn(0, 100)
        val stage = StageCalculator.stageFor(elapsedMs / 3_600_000.0)
        val stageIdx = Stages.all.indexOf(stage) + 1

        val title = "Stage $stageIdx · ${stage.name}"
        val text = "${elapsed.hours}h ${elapsed.mm}m elapsed"
        val chipText = if (elapsed.hours > 0) "${elapsed.hours}h ${elapsed.minutes}m"
            else "${elapsed.minutes}m"
        val fastEndMs = session.startMs + goalMs

        return if (useLiveUpdate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            buildLiveUpdate(context, title, text, chipText, progressPercent, session.goalHours, fastEndMs)
        } else {
            buildLegacy(context, title, text, progressPercent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildLiveUpdate(
        context: Context,
        title: String,
        text: String,
        chipText: String,
        progressPercent: Int,
        goalHours: Int,
        fastEndMs: Long,
    ): Notification {
        val primary = primaryColor(context)
        val style = Notification.ProgressStyle()
            .setProgress(progressPercent)
            .setStyledByProgress(true)
            .also { s ->
                val segments = stageSegments(goalHours, primary)
                if (segments.isNotEmpty()) s.setProgressSegments(segments)
            }

        val endAction = Notification.Action.Builder(
            null as Icon?,
            "End Fast",
            endFastPending(context),
        ).build()

        return Notification.Builder(context, NotifChannels.FASTING_STICKY)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle(title)
            .setContentText(text)
            .setShortCriticalText(chipText)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            // Body chronometer: ticks down toward the predicted end of the fast.
            .setWhen(fastEndMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setColor(primary)
            .setRequestPromotedOngoing(true)
            .setContentIntent(openAppPending(context))
            .setStyle(style)
            .addAction(endAction)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun stageSegments(
        goalHours: Int,
        @androidx.annotation.ColorInt primary: Int,
    ): List<Notification.ProgressStyle.Segment> {
        if (goalHours <= 0) return emptyList()
        val stages = Stages.all
        val durations = mutableListOf<Int>()
        for ((idx, stage) in stages.withIndex()) {
            if (stage.startHour >= goalHours) break
            val nextStart = stages.getOrNull(idx + 1)?.startHour ?: Int.MAX_VALUE
            val endHour = minOf(nextStart, goalHours)
            val durationH = (endHour - stage.startHour).coerceAtLeast(0)
            if (durationH > 0) durations += durationH
        }
        if (durations.isEmpty()) return emptyList()

        // Convert hour-durations to percentage segment lengths summing to 100,
        // absorbing rounding remainder in the last segment so the bar is exact.
        val lengths = durations.map { (it * 100f / goalHours).toInt().coerceAtLeast(1) }.toMutableList()
        val remainder = 100 - lengths.sum()
        if (lengths.isNotEmpty()) lengths[lengths.lastIndex] = (lengths.last() + remainder).coerceAtLeast(1)

        return lengths.map { len -> Notification.ProgressStyle.Segment(len).setColor(primary) }
    }

    private fun buildLegacy(
        context: Context,
        title: String,
        text: String,
        progressPercent: Int,
    ): Notification = NotificationCompat.Builder(context, NotifChannels.FASTING_STICKY)
        .setSmallIcon(R.drawable.ic_notif_fast)
        .setContentTitle(title)
        .setContentText(text)
        .setProgress(100, progressPercent, false)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .setShowWhen(false)
        .setColor(primaryColor(context))
        .setContentIntent(openAppPending(context))
        .addAction(0, "End Fast", endFastPending(context))
        .build()

    private fun openAppPending(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Always land on the timer ring (Home) — that's the screen the
            // notification is showing progress for, even if the user was on
            // another tab when they tapped it.
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, "home")
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun endFastPending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            1,
            NotificationActionReceiver.intent(context, NotificationActionReceiver.ACTION_END_FAST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

private fun primaryColor(context: Context): Int {
    val isDark = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    return if (isDark) Color.parseColor("#7DD3A8") else Color.parseColor("#2A4D3E")
}
