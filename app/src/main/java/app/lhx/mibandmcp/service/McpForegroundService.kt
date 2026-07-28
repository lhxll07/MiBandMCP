package app.lhx.mibandmcp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.lhx.mibandmcp.MainActivity
import app.lhx.mibandmcp.R
import app.lhx.mibandmcp.appContainer
import app.lhx.mibandmcp.util.localizedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class McpForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container by lazy { appContainer }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionStop -> stopSelf()
            else -> startServiceWork()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        container.mcpServerManager.stop()
        container.snapshotRepository.setServiceRunning(isRunning = false, endpoint = null)
        scope.cancel()
        super.onDestroy()
    }

    private fun startServiceWork() {
        startForegroundCompat(buildNotification(localizedString(R.string.service_starting)))
        scope.launch {
            runCatching {
                val port = container.settingsStore.preferences.first().port
                val endpoint = container.mcpServerManager.start(port)
                container.snapshotRepository.setServiceRunning(isRunning = true, endpoint = endpoint)
                updateNotification(endpoint.url)
            }.onFailure { error ->
                container.snapshotRepository.setServiceError(
                    error.message ?: localizedString(R.string.service_failed_to_start),
                )
                stopSelf()
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, McpForegroundService::class.java).setAction(ActionStop),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(openAppIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    localizedString(R.string.stop_service),
                    stopIntent,
                ).build(),
            )
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            NotificationManagerCompat.from(this).notify(NotificationId, buildNotification(text))
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ChannelId,
            localizedString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = localizedString(R.string.notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NotificationId, notification)
        }
    }

    companion object {
        private const val ActionStart = "app.lhx.mibandmcp.action.START_MCP"
        private const val ActionStop = "app.lhx.mibandmcp.action.STOP_MCP"
        private const val ChannelId = "mcp_service"
        private const val NotificationId = 1001

        fun start(context: Context) {
            val intent = Intent(context, McpForegroundService::class.java).setAction(ActionStart)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, McpForegroundService::class.java).setAction(ActionStop)
            context.startService(intent)
        }
    }
}
