package com.mrbaigsdownloader.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrbaigsdownloader.data.DownloadStatus
import com.mrbaigsdownloader.data.DownloadTask
import com.mrbaigsdownloader.databinding.ItemTaskBinding

class TasksAdapter(
    private val onPause:  (DownloadTask) -> Unit,
    private val onResume: (DownloadTask) -> Unit,
    private val onCancel: (DownloadTask) -> Unit,
    private val onDelete: (DownloadTask) -> Unit
) : ListAdapter<DownloadTask, TasksAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DownloadTask>() {
            override fun areItemsTheSame(a: DownloadTask, b: DownloadTask) = a.id == b.id
            override fun areContentsTheSame(a: DownloadTask, b: DownloadTask) = a == b
        }
    }

    inner class VH(val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = getItem(position)
        val b = holder.b
        val status = DownloadStatus.valueOf(task.status)

        b.tvTitle.text  = task.title.ifEmpty { task.url.take(50) }
        b.tvFormat.text = formatLabel(task.format)
        b.tvStatus.text = statusLabel(status)
        b.tvSpeed.text  = if (status == DownloadStatus.DOWNLOADING) "⚡ ${task.speed}" else ""
        b.tvEta.text    = if (status == DownloadStatus.DOWNLOADING) "⏱ ${task.eta}" else ""

        b.progressBar.progress = task.progress
        b.tvPercent.text = if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED)
            "${task.progress}%" else ""

        b.chipStatus.text = statusChipText(status)

        b.tvSize.text = if (task.size != "─") task.size else ""

        b.tvError.visibility = if (task.errorMessage.isNotEmpty() &&
            (status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED))
            View.VISIBLE else View.GONE
        b.tvError.text = task.errorMessage.take(100)

        b.btnPause.visibility  = if (status == DownloadStatus.DOWNLOADING) View.VISIBLE else View.GONE
        b.btnResume.visibility = if (status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED) View.VISIBLE else View.GONE
        b.btnCancel.visibility = if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED || status == DownloadStatus.QUEUED) View.VISIBLE else View.GONE
        b.btnDelete.visibility = if (status == DownloadStatus.COMPLETED || status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED) View.VISIBLE else View.GONE

        b.btnPause.setOnClickListener  { onPause(task) }
        b.btnResume.setOnClickListener { onResume(task) }
        b.btnCancel.setOnClickListener { onCancel(task) }
        b.btnDelete.setOnClickListener { onDelete(task) }
    }

    private fun statusChipText(s: DownloadStatus) = when (s) {
        DownloadStatus.DOWNLOADING -> "Downloading"
        DownloadStatus.PAUSED      -> "Paused"
        DownloadStatus.COMPLETED   -> "Done"
        DownloadStatus.FAILED      -> "Failed"
        DownloadStatus.CANCELLED   -> "Cancelled"
        DownloadStatus.QUEUED      -> "Queued"
    }

    private fun formatLabel(format: String) = when {
        format.contains("1080") -> "MP4 · 1080p"
        format.contains("720")  -> "MP4 · 720p"
        format.contains("480")  -> "MP4 · 480p"
        format.contains("360")  -> "MP4 · 360p"
        format.contains("320")  -> "MP3 · 320kbps"
        format.contains("192")  -> "MP3 · 192kbps"
        format.contains("128")  -> "MP3 · 128kbps"
        else -> format
    }

    private fun statusLabel(s: DownloadStatus) = when (s) {
        DownloadStatus.QUEUED      -> "⏳ Queued"
        DownloadStatus.DOWNLOADING -> "⬇ Downloading"
        DownloadStatus.PAUSED      -> "⏸ Paused"
        DownloadStatus.COMPLETED   -> "✅ Completed"
        DownloadStatus.FAILED      -> "❌ Failed"
        DownloadStatus.CANCELLED   -> "🚫 Cancelled"
    }
}
