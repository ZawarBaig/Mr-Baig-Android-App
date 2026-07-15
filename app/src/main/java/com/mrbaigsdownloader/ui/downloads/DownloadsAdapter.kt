package com.mrbaigsdownloader.ui.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrbaigsdownloader.data.HistoryEntry
import com.mrbaigsdownloader.databinding.ItemDownloadBinding
import java.text.SimpleDateFormat
import java.util.*

class DownloadsAdapter(
    private val onOpen:   (HistoryEntry) -> Unit,
    private val onShare:  (HistoryEntry) -> Unit,
    private val onDelete: (HistoryEntry) -> Unit
) : ListAdapter<HistoryEntry, DownloadsAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HistoryEntry>() {
            override fun areItemsTheSame(a: HistoryEntry, b: HistoryEntry) = a.id == b.id
            override fun areContentsTheSame(a: HistoryEntry, b: HistoryEntry) = a == b
        }
        val DATE_FMT = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
    }

    inner class VH(val b: ItemDownloadBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = getItem(position)
        val b = holder.b

        b.tvTitle.text  = entry.title
        b.tvFormat.text = formatLabel(entry.format)
        b.tvSize.text   = entry.size
        b.tvDate.text   = DATE_FMT.format(Date(entry.completedAt))

        val isAudio = entry.filePath.endsWith(".mp3")
        b.ivIcon.setImageResource(
            if (isAudio) com.mrbaigsdownloader.R.drawable.ic_audio
            else         com.mrbaigsdownloader.R.drawable.ic_video
        )

        b.btnOpen.setOnClickListener   { onOpen(entry) }
        b.btnShare.setOnClickListener  { onShare(entry) }
        b.btnDelete.setOnClickListener { onDelete(entry) }
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
}
