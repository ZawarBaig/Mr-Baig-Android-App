package com.mrbaigsdownloader.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.mrbaigsdownloader.data.AppSettings
import com.mrbaigsdownloader.data.DownloadTask
import com.mrbaigsdownloader.data.DownloadType
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import java.io.File

/**
 * DownloadEngine — uses yt-dlp directly on-device (no API, no limits).
 * Works exactly like the Seal app: 1000+ sites, no rate limits, no dependencies.
 */
class DownloadEngine(
    private val context: Context,
    private val settings: AppSettings
) {
    companion object {
        private const val TAG = "DownloadEngine"
    }

    @Volatile private var cancelled = false
    @Volatile private var paused    = false
    private var processId: String?  = null

    fun pause()  { paused = true }
    fun resume() { paused = false }
    fun cancel() {
        cancelled = true
        processId?.let {
            try { YoutubeDL.getInstance().destroyProcessById(it) } catch (e: Exception) { }
        }
    }

    suspend fun download(
        task: DownloadTask,
        onProgress: (Float, String, String) -> Unit,
        onComplete: (String, String) -> Unit,
        onFailed:   (String) -> Unit
    ) = withContext(Dispatchers.IO) {

        if (cancelled) return@withContext

        try {
            // ── Output directory ──────────────────────────────────────────────
            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MrBaigDownloader"
            ).also { it.mkdirs() }

            val isAudio    = task.type == DownloadType.AUDIO.name
            val isPlaylist = task.type == DownloadType.PLAYLIST.name

            // ── Build yt-dlp request ──────────────────────────────────────────
            val request = YoutubeDLRequest(task.url).apply {

                // Output template
                if (isPlaylist) {
                    addOption("-o", "${outputDir.absolutePath}/%(playlist_title)s/%(playlist_index)s - %(title)s.%(ext)s")
                } else {
                    addOption("-o", "${outputDir.absolutePath}/%(title)s.%(ext)s")
                }

                // Format selection
                if (isAudio) {
                    addOption("-x")  // extract audio
                    val audioBitrate = when {
                        task.format.contains("320") -> "320"
                        task.format.contains("192") -> "192"
                        else -> "128"
                    }
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "${audioBitrate}K")
                } else {
                    val height = when {
                        task.format.contains("1080") -> "1080"
                        task.format.contains("720")  -> "720"
                        task.format.contains("480")  -> "480"
                        task.format.contains("360")  -> "360"
                        else -> "720"
                    }
                    // Best video + audio merged into mp4
                    addOption("-f", "bestvideo[height<=${height}][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=${height}]+bestaudio/best[height<=${height}]/best")
                    addOption("--merge-output-format", "mp4")
                }

                // Quality of life options
                if (!isPlaylist) {
                    addOption("--no-playlist")              // single video unless playlist type
                }

                addOption("--embed-thumbnail")              // embed thumbnail
                addOption("--add-metadata")                 // embed metadata
                addOption("--no-warnings")                  // suppress yt-dlp warnings
                addOption("--ignore-errors")                // skip failed items in playlist
                addOption("--no-abort-on-error")
                addOption("--socket-timeout", "30")
                addOption("--retries", "3")
                addOption("--fragment-retries", "3")
                addOption("--concurrent-fragments", "4")    // faster downloads
                addOption("--no-part")                      // no .part files
                addOption("--windows-filenames")            // safe filenames

                // Cookies / age-gate bypass
                addOption("--extractor-retries", "3")
            }

            processId = task.id.toString()

            // ── Execute with progress callback ────────────────────────────────
            var lastFile = ""
            YoutubeDL.getInstance().execute(
                request,
                task.id.toString()
            ) { progress, etaInSeconds, line ->

                if (cancelled) return@execute
                while (paused && !cancelled) Thread.sleep(300)

                // Parse speed from yt-dlp output line
                val speed = parseSpeed(line)
                val eta   = if (etaInSeconds > 0) formatTime(etaInSeconds.toInt()) else "─"

                onProgress(progress, speed, eta)

                // Track output filename
                if (line.contains("[download] Destination:") || line.contains("[Merger]")) {
                    lastFile = line.substringAfter("Destination:").trim()
                        .ifEmpty { lastFile }
                }
            }

            if (cancelled) return@withContext

            // ── Find downloaded file ──────────────────────────────────────────
            val downloadedFile = findDownloadedFile(outputDir, task, lastFile)
            val size = if (downloadedFile != null) formatSize(downloadedFile.length()) else "─"
            val path = downloadedFile?.absolutePath ?: outputDir.absolutePath

            onComplete(path, size)

        } catch (e: YoutubeDLException) {
            if (!cancelled) {
                Log.e(TAG, "YoutubeDL error: ${e.message}")
                onFailed(translateError(e.message ?: "Download failed"))
            }
        } catch (e: InterruptedException) {
            if (!cancelled) onFailed("Download was interrupted")
        } catch (e: Exception) {
            if (!cancelled) {
                Log.e(TAG, "Download error: ${e.message}")
                onFailed(translateError(e.message ?: "Unknown error"))
            }
        }
    }

    // ── Translate raw yt-dlp errors into friendly messages ──────────────────
    private fun translateError(raw: String): String {
        val msg = raw.lowercase()
        return when {
            msg.contains("private video") || msg.contains("private") ->
                "This video is private and cannot be downloaded."
            msg.contains("age") && msg.contains("restrict") ->
                "This video is age-restricted. Sign in to download."
            msg.contains("unavailable") || msg.contains("not available") ->
                "This video is unavailable in your region."
            msg.contains("copyright") || msg.contains("blocked") ->
                "This video is blocked due to copyright restrictions."
            msg.contains("live") && msg.contains("stream") ->
                "Live streams cannot be downloaded while streaming."
            msg.contains("members") || msg.contains("premium") ->
                "This content requires a membership or premium subscription."
            msg.contains("deleted") || msg.contains("removed") ->
                "This video has been deleted or removed."
            msg.contains("playlist") && msg.contains("empty") ->
                "This playlist is empty or unavailable."
            msg.contains("no video formats") || msg.contains("no formats") ->
                "No downloadable format found for this URL."
            msg.contains("urlopen") || msg.contains("connection") ||
            msg.contains("network") || msg.contains("timeout") ->
                "Network error. Check your internet connection and try again."
            msg.contains("unsupported url") || msg.contains("not supported") ->
                "This website is not supported. Try a different URL."
            msg.contains("403") || msg.contains("forbidden") ->
                "Access denied by the server. The link may have expired."
            msg.contains("404") ->
                "Video not found. It may have been deleted."
            msg.contains("429") || msg.contains("too many") ->
                "Too many requests. Please wait a moment and try again."
            msg.contains("sign in") || msg.contains("login") ->
                "This content requires signing in. Cannot download."
            msg.contains("ffmpeg") ->
                "Media processing failed. Please try a different format."
            msg.contains("no space") || msg.contains("disk") ->
                "Not enough storage space. Free up some space and try again."
            else -> "Download failed. Please check the URL and try again."
        }
    }

    // ── Find the output file yt-dlp created ──────────────────────────────────
    private fun findDownloadedFile(dir: File, task: DownloadTask, hint: String): File? {
        if (hint.isNotEmpty()) {
            val f = File(hint)
            if (f.exists()) return f
        }
        // Search by most recently modified file
        val ext = if (task.type == DownloadType.AUDIO.name) "mp3" else "mp4"
        return dir.walkTopDown()
            .filter { it.isFile && it.extension.equals(ext, ignoreCase = true) }
            .maxByOrNull { it.lastModified() }
    }

    // ── Parse speed from yt-dlp output line ─────────────────────────────────
    private fun parseSpeed(line: String): String {
        // yt-dlp outputs lines like: "[download]  45.2% of 12.34MiB at 1.23MiB/s ETA 00:05"
        val regex = Regex("at\\s+([\\d.]+\\s*[KMG]iB/s)")
        return regex.find(line)?.groupValues?.get(1)?.trim() ?: "─"
    }

    // ── Formatters ────────────────────────────────────────────────────────────
    private fun formatSize(bytes: Long) = when {
        bytes >= 1_073_741_824 -> "${"%.2f".format(bytes / 1_073_741_824.0)} GB"
        bytes >= 1_048_576     -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        bytes >= 1024          -> "${"%.0f".format(bytes / 1024.0)} KB"
        else                   -> "$bytes B"
    }

    private fun formatTime(sec: Int) = when {
        sec <= 0      -> "─"
        sec >= 3600   -> "%02d:%02d:%02d".format(sec/3600, (sec%3600)/60, sec%60)
        else          -> "%02d:%02d".format(sec/60, sec%60)
    }
}
