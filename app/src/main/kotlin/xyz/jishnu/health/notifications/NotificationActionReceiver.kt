package xyz.jishnu.health.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.repo.FastingRepository
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var fastingRepo: FastingRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_END_FAST) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val active = fastingRepo.activeSession.first()
                active?.let { fastingRepo.endFast(it.id, System.currentTimeMillis()) }
                FastingForegroundService.stop(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_END_FAST = "xyz.jishnu.health.action.END_FAST"

        fun intent(context: Context, action: String): Intent =
            Intent(context, NotificationActionReceiver::class.java).setAction(action)
    }
}
