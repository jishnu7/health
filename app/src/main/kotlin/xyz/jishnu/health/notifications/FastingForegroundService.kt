package xyz.jishnu.health.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.jishnu.health.R
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class FastingForegroundService : Service() {

    @Inject lateinit var fastingRepo: FastingRepository
    @Inject lateinit var settingsRepo: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchJob: Job? = null
    @Volatile private var activeSession: FastingSessionEntity? = null
    @Volatile private var useLiveUpdate: Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Satisfy the startForeground deadline synchronously with a placeholder
        // until the repository emits the active session.
        startForegroundCompat(buildPlaceholder())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every startForegroundService call resets the "did not call startForeground in time"
        // deadline, so re-promote synchronously here on top of onCreate's placeholder.
        val snapshot = activeSession
        val notif = snapshot
            ?.let { FastingNotificationBuilder.build(applicationContext, it, useLiveUpdate) }
            ?: buildPlaceholder()
        startForegroundCompat(notif)
        observeAndPost()
        return START_STICKY
    }

    private fun observeAndPost() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            launch {
                fastingRepo.activeSession.distinctUntilChanged().collectLatest { session ->
                    activeSession = session
                    if (session == null) stopForegroundAndSelf() else post(session)
                }
            }
            launch {
                settingsRepo.settings.map { it.liveUpdateOn }.distinctUntilChanged().collect { on ->
                    useLiveUpdate = on
                    activeSession?.let { post(it) }
                }
            }
            launch {
                while (true) {
                    delay(60_000)
                    activeSession?.let { post(it) } ?: break
                }
            }
        }
    }

    private fun buildPlaceholder(): Notification =
        NotificationCompat.Builder(this, NotifChannels.FASTING_STICKY)
            .setSmallIcon(R.drawable.ic_notif_fast)
            .setContentTitle("Active fast")
            .setContentText("Starting…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun post(session: FastingSessionEntity) {
        startForegroundCompat(FastingNotificationBuilder.build(applicationContext, session, useLiveUpdate))
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotifChannels.STICKY_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NotifChannels.STICKY_NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        applicationContext.getSystemService<NotificationManager>()
            ?.cancel(NotifChannels.STICKY_NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun startIntent(context: Context): Intent =
            Intent(context, FastingForegroundService::class.java)

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, startIntent(context))
        }

        fun stop(context: Context) {
            context.stopService(startIntent(context))
        }
    }
}
