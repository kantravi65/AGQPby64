package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.repository.OtsRepository
import com.example.util.WebServerManager
import com.example.util.WebServerState

class WebServerService : Service() {

    private var webServerManager: WebServerManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServerAndService()
            return START_NOT_STICKY
        }

        val mode = intent?.getStringExtra("SERVER_MODE") ?: "admin"
        val adminUser = intent?.getStringExtra("ADMIN_USER") ?: "admin"
        val adminPass = intent?.getStringExtra("ADMIN_PASS") ?: "1234"
        val notification = createNotification(mode)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (webServerManager != null) {
            webServerManager?.stopServer()
            webServerManager = null
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = OtsRepository(
            database.questionDao(),
            database.bookDao(),
            database.paperDao(),
            database.testAttemptDao(),
            database.testSubmissionDao()
        )
        val settingsManager = com.example.util.SettingsManager(applicationContext)
        val publicUrl = settingsManager.publicTunnelUrl.ifBlank { null }
        webServerManager = WebServerManager(applicationContext, repository, mode)
        WebServerState.setError(null)
        webServerManager?.startServer(adminUser, adminPass, onStarted = { httpsUrl, httpUrl ->
            WebServerState.setUrl(httpsUrl, mode, httpUrl, publicUrl)
            val updatedNotification = createNotification(mode, httpsUrl, publicUrl)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, updatedNotification)
        }, onError = { err ->
            WebServerState.setError(err)
        })
        
        return START_STICKY
    }

    private var isStopping = false

    private fun stopServerAndService() {
        if (isStopping) return
        isStopping = true

        WebServerState.setUrl(null)
        try {
            webServerManager?.stopServer()
        } catch (e: Throwable) {
            android.util.Log.e("WebServerService", "Error stopping web server", e)
        }
        webServerManager = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Throwable) {
            android.util.Log.e("WebServerService", "Error removing foreground", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isStopping) {
            isStopping = true
            WebServerState.setUrl(null)
            try {
                webServerManager?.stopServer()
            } catch (e: Throwable) {
                android.util.Log.e("WebServerService", "Error stopping web server", e)
            }
            webServerManager = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web Dashboard Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Web Dashboard server running in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(mode: String, url: String? = null, publicUrl: String? = null): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val modeLabel = when(mode) {
            "expert" -> "Expert Review"
            "livetest" -> "Live Test Portal"
            "all" -> "Unified Portal (All Services)"
            else -> "Admin Dashboard"
        }
        
        val contentText = when {
            publicUrl != null && url != null -> "Internet: $publicUrl | LAN: $url"
            url != null -> "Running at $url"
            else -> "Starting server..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Web Server: $modeLabel")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(0, "Stop Server", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "WebServerChannel"
        const val NOTIFICATION_ID = 12345
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
    }
}
