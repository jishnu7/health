package xyz.jishnu.health.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import xyz.jishnu.health.data.local.Settings
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.domain.WaterReminders
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor() {

    fun syncFromSettings(context: Context, settings: Settings) {
        if (settings.fastingReminderOn) {
            scheduleNext(
                context,
                kind = Kind.FastingStart,
                hhmm = settings.fastStartTime,
            )
        } else {
            cancel(context, Kind.FastingStart)
        }
        if (settings.weightReminderOn) {
            scheduleNext(
                context,
                kind = Kind.WeighIn,
                hhmm = settings.reminderTime,
            )
        } else {
            cancel(context, Kind.WeighIn)
        }
        if (settings.waterReminderOn) {
            scheduleNextWater(context)
        } else {
            cancelAllWater(context)
        }
    }

    fun scheduleNext(context: Context, kind: Kind, hhmm: String) {
        val alarm = context.getSystemService<AlarmManager>() ?: return
        val triggerAt = nextTriggerMs(hhmm)
        val pi = pendingIntent(context, kind)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun scheduleNextWater(context: Context) {
        val alarm = context.getSystemService<AlarmManager>() ?: return
        // Always cancel every water alarm before scheduling the next one so we
        // never have stale entries from a previous index lingering on the
        // AlarmManager.
        cancelAllWater(context)
        val next = WaterReminders.nextTrigger(System.currentTimeMillis())
        val pi = waterPendingIntent(context, next.windowIndex)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.triggerAtMs, pi)
    }

    fun cancel(context: Context, kind: Kind) {
        val alarm = context.getSystemService<AlarmManager>() ?: return
        val pi = pendingIntent(context, kind, mutable = false)
        alarm.cancel(pi)
    }

    fun cancelAllWater(context: Context) {
        val alarm = context.getSystemService<AlarmManager>() ?: return
        for (w in WaterReminders.windows) {
            alarm.cancel(waterPendingIntent(context, w.index))
        }
    }

    private fun pendingIntent(context: Context, kind: Kind, mutable: Boolean = false): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = kind.action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, kind.requestCode, intent, flags)
    }

    private fun waterPendingIntent(context: Context, windowIndex: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_WATER
            putExtra(EXTRA_WATER_WINDOW, windowIndex)
        }
        val requestCode = WATER_REQUEST_CODE_BASE + windowIndex
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTriggerMs(hhmm: String, zone: ZoneId = ZoneId.systemDefault()): Long {
        val lt = TimeMath.parseTime(hhmm)
        val today = LocalDate.now(zone)
        val candidate = today.atTime(lt).atZone(zone)
        val now = System.currentTimeMillis()
        val candidateMs = candidate.toInstant().toEpochMilli()
        return if (candidateMs > now) candidateMs else candidate.plusDays(1).toInstant().toEpochMilli()
    }

    enum class Kind(val action: String, val requestCode: Int) {
        FastingStart("xyz.jishnu.health.action.REMINDER_FASTING", 101),
        WeighIn("xyz.jishnu.health.action.REMINDER_WEIGH_IN", 102),
    }

    companion object {
        const val ACTION_WATER = "xyz.jishnu.health.action.REMINDER_WATER"
        const val EXTRA_WATER_WINDOW = "window_index"
        private const val WATER_REQUEST_CODE_BASE = 200
    }
}
