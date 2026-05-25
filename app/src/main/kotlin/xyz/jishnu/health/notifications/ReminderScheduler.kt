package xyz.jishnu.health.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import xyz.jishnu.health.data.local.Settings
import xyz.jishnu.health.domain.TimeMath
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
    }

    fun scheduleNext(context: Context, kind: Kind, hhmm: String) {
        val alarm = context.getSystemService<AlarmManager>() ?: return
        val triggerAt = nextTriggerMs(hhmm)
        val pi = pendingIntent(context, kind)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancel(context: Context, kind: Kind) {
        val alarm = context.getSystemService<AlarmManager>() ?: return
        val pi = pendingIntent(context, kind, mutable = false)
        alarm.cancel(pi)
    }

    private fun pendingIntent(context: Context, kind: Kind, mutable: Boolean = false): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = kind.action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, kind.requestCode, intent, flags)
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
}
