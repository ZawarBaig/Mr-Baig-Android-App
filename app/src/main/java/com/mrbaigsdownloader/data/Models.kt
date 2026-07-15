package com.mrbaigsdownloader.data

import androidx.room.*

// ─── Download Task ─────────────────────────────────────────────────────────────

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }
enum class DownloadFormat { MP4_1080P, MP4_720P, MP4_480P, MP4_360P, MP3_320, MP3_192, MP3_128 }
enum class DownloadType { VIDEO, AUDIO, PLAYLIST }

@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val format: String,
    val type: String,          // VIDEO / AUDIO / PLAYLIST
    val status: String = DownloadStatus.QUEUED.name,
    val progress: Int = 0,
    val speed: String = "─",
    val eta: String = "─",
    val size: String = "─",
    val filePath: String = "",
    val thumbnail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L,
    val errorMessage: String = "",
    // Clip range in seconds, -1 = not set
    val clipStart: Int = -1,
    val clipEnd: Int = -1
)

// ─── History Entry ─────────────────────────────────────────────────────────────

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val filePath: String,
    val format: String,
    val size: String,
    val completedAt: Long = System.currentTimeMillis()
)

// ─── App Settings (single-row table) ──────────────────────────────────────────

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val downloadPath: String = "Downloads/MrBaigDownloader",
    val defaultFormat: String = DownloadFormat.MP4_720P.name,
    val maxConcurrentDownloads: Int = 2,
    val embedMetadata: Boolean = true,
    val darkMode: Boolean = true,
    val wifiOnly: Boolean = false,
    val autoRetry: Boolean = true,
    val maxRetries: Int = 3
)
