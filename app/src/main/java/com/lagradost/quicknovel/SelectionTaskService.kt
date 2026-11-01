package com.lagradost.quicknovel

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lagradost.quicknovel.NotificationHelper.createNotificationChannel
import com.lagradost.quicknovel.mvvm.logError

/**
 * Keeps a data-sync foreground notification while chapter download/translate/export runs,
 * so the user can leave the novel screen (or the app) without Android freezing the job.
 */
class SelectionTaskService : Service() {

    companion object {
        const val NOTIFICATION_ID = 6671
        private const val ACTION_START = "com.lagradost.quicknovel.selection.START"
        private const val ACTION_UPDATE = "com.lagradost.quicknovel.selection.UPDATE"
        private const val ACTION_STOP = "com.lagradost.quicknovel.selection.STOP"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_DONE = "done"
        private const val EXTRA_TOTAL = "total"
        private const val EXTRA_INDETERMINATE = "indeterminate"

        @Volatile
        private var lastTitle: String = ""
        @Volatile
        private var lastMessage: String = ""
        @Volatile
        private var lastDone: Int = 0
        @Volatile
        private var lastTotal: Int = 1
        @Volatile
        private var lastIndeterminate: Boolean = false

        fun start(
            context: Context,
            title: String,
            message: String,
            done: Int = 0,
            total: Int = 1,
            indeterminate: Boolean = false,
        ) {
            lastTitle = title
            lastMessage = message
            lastDone = done
            lastTotal = total.coerceAtLeast(1)
            lastIndeterminate = indeterminate
            val intent = Intent(context, SelectionTaskService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_DONE, done)
                putExtra(EXTRA_TOTAL, lastTotal)
                putExtra(EXTRA_INDETERMINATE, indeterminate)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun update(
            context: Context,
            title: String,
            message: String,
            done: Int,
            total: Int,
            indeterminate: Boolean = false,
        ) {
            lastTitle = title
            lastMessage = message
            lastDone = done
            lastTotal = total.coerceAtLeast(1)
            lastIndeterminate = indeterminate
            val intent = Intent(context, SelectionTaskService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_DONE, done)
                putExtra(EXTRA_TOTAL, lastTotal)
                putExtra(EXTRA_INDETERMINATE, indeterminate)
            }
            // Service may already be running; startForegroundService is safe to refresh extras.
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure {
                    // If start is blocked, still push a normal notification update.
                    notifyProgress(context.applicationContext)
                }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SelectionTaskService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
            NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
        }

        private fun notifyProgress(context: Context) {
            context.createNotificationChannel()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, buildNotification(context))
        }

        fun buildNotification(context: Context): Notification {
            context.createNotificationChannel()
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0),
            )
            val max = lastTotal.coerceAtLeast(1)
            val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_autorenew_24)
                .setContentTitle(lastTitle.ifBlank { context.getString(R.string.app_name) })
                .setContentText(lastMessage)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(open)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            if (lastIndeterminate) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(max, lastDone.coerceIn(0, max), false)
                builder.setSubText(
                    context.getString(R.string.task_progress_format, lastDone, max)
                )
            }
            return builder.build()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_UPDATE, null -> {
                intent?.let { applyExtras(it) }
                val notification = buildNotification(this)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (t: Throwable) {
                    logError(t)
                    NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun applyExtras(intent: Intent) {
        lastTitle = intent.getStringExtra(EXTRA_TITLE) ?: lastTitle
        lastMessage = intent.getStringExtra(EXTRA_MESSAGE) ?: lastMessage
        lastDone = intent.getIntExtra(EXTRA_DONE, lastDone)
        lastTotal = intent.getIntExtra(EXTRA_TOTAL, lastTotal).coerceAtLeast(1)
        lastIndeterminate = intent.getBooleanExtra(EXTRA_INDETERMINATE, lastIndeterminate)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
