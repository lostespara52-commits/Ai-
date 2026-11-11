package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuraPersistentService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_MANUAL_POWER_OFF) {
            _isSessionActive.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val statusText = intent?.getStringExtra(EXTRA_STATUS_TEXT)
            ?: getString(R.string.persistent_service_subtitle)
        val notification = buildPersistentNotification(statusText)
        try {
            startForeground(NOTIFICATION_ID, notification)
            _isSessionActive.value = true
        } catch (e: Exception) {
            _isSessionActive.value = false
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _isSessionActive.value = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.persistent_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains AURA persistent autonomous assistant session until manually powered off."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildPersistentNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val powerOffIntent = Intent(this, AuraPersistentService::class.java).apply {
            action = ACTION_MANUAL_POWER_OFF
        }
        val powerOffPending = PendingIntent.getService(
            this,
            1,
            powerOffIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.persistent_service_title))
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(
                android.R.drawable.ic_lock_power_off,
                getString(R.string.power_off_action),
                powerOffPending
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "aura_persistent_session_channel"
        private const val NOTIFICATION_ID = 4091
        const val ACTION_MANUAL_POWER_OFF = "com.example.action.MANUAL_POWER_OFF"
        const val EXTRA_STATUS_TEXT = "extra_status_text"

        private val _isSessionActive = MutableStateFlow(false)
        val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

        fun startPersistentSession(context: Context, statusText: String = "Active • Listening & Workflow Engine Online") {
            val intent = Intent(context, AuraPersistentService::class.java).apply {
                putExtra(EXTRA_STATUS_TEXT, statusText)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stopPersistentSession(context: Context) {
            _isSessionActive.value = false
            try {
                val intent = Intent(context, AuraPersistentService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
