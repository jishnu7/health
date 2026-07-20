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
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var fastingRepo: FastingRepository
    @Inject lateinit var settingsRepo: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (action) {
                    ACTION_END_FAST -> endFast(context)
                    ACTION_START_FAST -> startFast(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun endFast(context: Context) {
        val active = fastingRepo.activeSession.first()
        if (active != null) {
            val endMs = System.currentTimeMillis()
            fastingRepo.endFast(active.id, endMs)
            FastingForegroundService.stop(context)
            // Heads-up summary so the user sees the result instead
            // of just watching the sticky notification vanish.
            val durationMs = (endMs - active.startMs).coerceAtLeast(0L)
            ReminderNotifications.postFastEnded(
                context = context,
                durationMs = durationMs,
                goalHours = active.goalHours,
            )
        } else {
            FastingForegroundService.stop(context)
        }
    }

    private suspend fun startFast(context: Context) {
        // Dismiss the reminder that launched this action.
        context.getSystemService<NotificationManager>()
            ?.cancel(NotifChannels.FASTING_REMINDER_NOTIFICATION_ID)
        // Don't start a second fast when one is already running.
        if (fastingRepo.activeSession.first() != null) return
        val settings = settingsRepo.settings.first()
        val plan = Plans.byId(settings.planId)
        fastingRepo.startFast(System.currentTimeMillis(), plan.fastHours, plan.id)
        if (settings.stickyNotificationOn) FastingForegroundService.start(context)
    }

    companion object {
        const val ACTION_END_FAST = "xyz.jishnu.health.action.END_FAST"
        const val ACTION_START_FAST = "xyz.jishnu.health.action.START_FAST"

        fun intent(context: Context, action: String): Intent =
            Intent(context, NotificationActionReceiver::class.java).setAction(action)
    }
}
