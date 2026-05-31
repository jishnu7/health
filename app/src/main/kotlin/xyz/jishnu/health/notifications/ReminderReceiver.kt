package xyz.jishnu.health.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WaterRepository
import xyz.jishnu.health.domain.WaterReminders
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var fastingRepo: FastingRepository
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
                            // Skip the nudge if a fast is already in progress — no
                            // point prompting "time to start" when the user is mid-fast.
                            val alreadyFasting = fastingRepo.activeSession.first() != null
                            if (!alreadyFasting) {
                                nm?.notify(
                                    NotifChannels.FASTING_REMINDER_NOTIFICATION_ID,
                                    ReminderNotifications.buildFastingReminder(context),
                                )
                            }
                            scheduler.scheduleNext(context, ReminderScheduler.Kind.FastingStart, settings.fastStartTime)
                        }
                    }
                    ReminderScheduler.Kind.WeighIn.action -> {
                        if (settings.weightReminderOn) {
                            nm?.notify(
                                NotifChannels.WEIGHT_REMINDER_NOTIFICATION_ID,
                                ReminderNotifications.buildWeighInReminder(context),
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
                                    ReminderNotifications.buildWaterReminder(
                                        context = context,
                                        totalMl = totalMl,
                                        targetMl = targetMl,
                                        goalMl = settings.waterGoalMl,
                                        units = settings.units,
                                        windowIndex = windowIndex,
                                    ),
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
}
