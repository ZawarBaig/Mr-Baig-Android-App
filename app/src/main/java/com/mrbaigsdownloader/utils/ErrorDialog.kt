package com.mrbaigsdownloader.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mrbaigsdownloader.R

object ErrorDialog {

    fun show(context: Context, title: String, message: String, onRetry: (() -> Unit)? = null) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)

        if (onRetry != null) {
            builder.setNeutralButton("Retry") { _, _ -> onRetry() }
        }
        builder.show()
    }

    fun showNoNetwork(context: Context, onRetry: (() -> Unit)? = null) {
        show(
            context,
            "No Internet Connection",
            "Please check your Wi-Fi or mobile data and try again.",
            onRetry
        )
    }

    fun showPermissionDenied(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Permission Required")
            .setMessage("Storage permission is needed to save downloads.\n\nPlease grant it in App Settings.")
            .setPositiveButton("Open Settings") { _, _ -> PermissionHelper.openAppSettings(context) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showDownloadError(context: Context, error: String, onRetry: (() -> Unit)? = null) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle("Download Failed")
            .setMessage(error)
            .setPositiveButton("OK", null)
        if (onRetry != null) {
            builder.setNeutralButton("Retry") { _, _ -> onRetry() }
        }
        builder.show()
    }

    fun showWifiOnly(context: Context, onProceed: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Wi-Fi Only Mode")
            .setMessage("You are on mobile data but Wi-Fi Only is enabled in Settings.\n\nDownload anyway?")
            .setPositiveButton("Download Anyway") { _, _ -> onProceed() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showUnsupportedUrl(context: Context) {
        show(
            context,
            "Unsupported URL",
            "This website is not supported.\n\nSupported sites include YouTube, TikTok, Instagram, Twitter/X, Facebook, Vimeo, SoundCloud and 1000+ more."
        )
    }

    fun showYtDlpUpdate(context: Context, onUpdate: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Update Available")
            .setMessage("A yt-dlp update is available. Updating improves compatibility with all sites.\n\nUpdate now? (Requires internet)")
            .setPositiveButton("Update") { _, _ -> onUpdate() }
            .setNegativeButton("Later", null)
            .show()
    }
}
