package com.mrbaigsdownloader.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mrbaigsdownloader.R
import com.mrbaigsdownloader.data.*
import com.mrbaigsdownloader.ui.MainActivity
import com.mrbaigsdownloader.utils.DownloadEngine
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID   = "mrbaigsdownloader_channel"
        const val NOTIF_ID     = 2001
        const val ACTION_START      = "ACTION_START"
        const val ACTION_PAUSE      = "ACTION_PAUSE"
        const val ACTION_RESUME     = "ACTION_RESUME"
        const val ACTION_CANCEL     = "ACTION_CANCEL"
        const val ACTION_CANCEL_ALL = "ACTION_CANCEL_ALL"
        const val ACTION_UPDATE_YTDLP = "ACTION_UPDATE_YTDLP"
        const val EXTRA_TASK_ID     = "task_id"

        // Broadcast extras
        const val BROADCAST_PROGRESS = "com.mrbaigsdownloader.PROGRESS"
        const val BROADCAST_COMPLETE = "com.mrbaigsdownloader.COMPLETE"
        const val BROADCAST_FAILED   = "com.mrbaigsdownloader.FAILED"
        const val BROADCAST_PAUSED   = "com.mrbaigsdownloader.PAUSED"
        const val BROADCAST_YTDLP_UPDATED = "com.mrbaigsdownloader.YTDLP_UPDATED"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_SPEED    = "speed"
        const val EXTRA_ETA      = "eta"
        const val EXTRA_FILE     = "file"
        const val EXTRA_SIZE     = "size"
        const val EXTRA_ERROR    = "error"
    }

    private val scope      = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repo: Repository
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private val engines    = ConcurrentHashMap<Int, DownloadEngine>()
    private var settings   = AppSettings()

    override fun onCreate() {
        super.onCreate()
        repo = Repository(applicationContext)
        createNotificationChannel()
        scope.launch { settings = repo.getSettings() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START      -> startDownload(intent.getIntExtra(EXTRA_TASK_ID, -1))
            ACTION_PAUSE      -> pauseDownload(intent.getIntExtra(EXTRA_TASK_ID, -1))
            ACTION_RESUME     -> resumeDownload(intent.getIntExtra(EXTRA_TASK_ID, -1))
            ACTION_CANCEL     -> cancelDownload(intent.getIntExtra(EXTRA_TASK_ID, -1))
            ACTION_CANCEL_ALL -> cancelAllDownloads()
            ACTION_UPDATE_YTDLP -> updateYtDlp()
        }
        return START_STICKY
    }

    private fun startDownload(taskId: Int) {
        if (taskId == -1 || activeJobs.containsKey(taskId)) return
        startForeground(NOTIF_ID, buildNotification("Starting download…", 0, taskId))

        val job = scope.launch {
            val task = repo.taskDao.getTaskById(taskId) ?: return@launch
            val engine = DownloadEngine(applicationContext, settings)
            engines[taskId] = engine

            repo.taskDao.updateProgress(taskId, DownloadStatus.DOWNLOADING.name, 0, "─", "─")

            engine.download(
                task,
                onProgress = { pct, speed, eta ->
                    scope.launch {
                        repo.taskDao.updateProgress(
                            taskId, DownloadStatus.DOWNLOADING.name,
                            pct.toInt(), speed, eta
                        )
                    }
                    sendBroadcast(Intent(BROADCAST_PROGRESS).apply {
                        putExtra(EXTRA_TASK_ID, taskId)
                        putExtra(EXTRA_PROGRESS, pct.toInt())
                        putExtra(EXTRA_SPEED, speed)
                        putExtra(EXTRA_ETA, eta)
                    })
                    updateNotification("Downloading ${pct.toInt()}%  $speed", pct.toInt(), taskId)
                },
                onComplete = { filePath, size ->
                    scope.launch {
                        repo.taskDao.markCompleted(
                            taskId, DownloadStatus.COMPLETED.name,
                            filePath, size, System.currentTimeMillis()
                        )
                        val t = repo.taskDao.getTaskById(taskId)
                        if (t != null) {
                            repo.addHistory(HistoryEntry(
                                title    = t.title,
                                url      = t.url,
                                filePath = filePath,
                                format   = t.format,
                                size     = size
                            ))
                        }
                    }
                    sendBroadcast(Intent(BROADCAST_COMPLETE).apply {
                        putExtra(EXTRA_TASK_ID, taskId)
                        putExtra(EXTRA_FILE, filePath)
                        putExtra(EXTRA_SIZE, size)
                    })
                    showCompletedNotification(task.title, taskId)
                    cleanupJob(taskId)
                },
                onFailed = { error ->
                    scope.launch {
                        repo.taskDao.markFailed(taskId, DownloadStatus.FAILED.name, error)
                    }
                    sendBroadcast(Intent(BROADCAST_FAILED).apply {
                        putExtra(EXTRA_TASK_ID, taskId)
                        putExtra(EXTRA_ERROR, error)
                    })
                    cleanupJob(taskId)
                }
            )
        }
        activeJobs[taskId] = job
    }

    private fun pauseDownload(taskId: Int) {
        engines[taskId]?.pause()
        scope.launch {
            repo.taskDao.updateProgress(taskId, DownloadStatus.PAUSED.name, 0, "─", "─")
        }
        sendBroadcast(Intent(BROADCAST_PAUSED).putExtra(EXTRA_TASK_ID, taskId))
        updateNotification("Paused", 0, taskId)
    }

    private fun resumeDownload(taskId: Int) {
        engines[taskId]?.resume() ?: startDownload(taskId)
    }

    private fun cancelDownload(taskId: Int) {
        engines[taskId]?.cancel()
        activeJobs[taskId]?.cancel()
        scope.launch {
            repo.taskDao.markFailed(taskId, DownloadStatus.CANCELLED.name, "Cancelled by user")
        }
        cleanupJob(taskId)
    }

    private fun cancelAllDownloads() {
        engines.keys.toList().forEach { cancelDownload(it) }
    }

    private fun updateYtDlp() {
        scope.launch {
            try {
                val result = com.yausername.youtubedl_android.YoutubeDL.getInstance()
                    .updateYoutubeDL(applicationContext)
                sendBroadcast(Intent(BROADCAST_YTDLP_UPDATED).apply {
                    putExtra("success", true)
                    putExtra("result", result.toString())
                })
            } catch (e: Exception) {
                sendBroadcast(Intent(BROADCAST_YTDLP_UPDATED).apply {
                    putExtra("success", false)
                    putExtra("result", e.message ?: "Update failed")
                })
            }
        }
    }

    private fun cleanupJob(taskId: Int) {
        activeJobs.remove(taskId)
        engines.remove(taskId)
        if (activeJobs.isEmpty()) stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "MrBaig Downloader",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
                setShowBadge(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, progress: Int, taskId: Int): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelIntent = PendingIntent.getService(
            this, taskId,
            Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("MrBaig Downloader")
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_cancel, "Cancel", cancelIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int, taskId: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text, progress, taskId))
    }

    private fun showCompletedNotification(title: String, taskId: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download Complete")
            .setContentText(title.take(60))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID + taskId, notif)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
