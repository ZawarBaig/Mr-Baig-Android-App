package com.mrbaigsdownloader.ui.home

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mrbaigsdownloader.R
import com.mrbaigsdownloader.data.*
import com.mrbaigsdownloader.databinding.FragmentHomeBinding
import com.mrbaigsdownloader.service.DownloadService
import com.mrbaigsdownloader.ui.MainActivity
import com.mrbaigsdownloader.ui.MainViewModel
import com.mrbaigsdownloader.utils.ErrorDialog
import com.mrbaigsdownloader.utils.PermissionHelper

class HomeFragment : Fragment() {

    companion object {
        private const val ARG_URL = "shared_url"
        fun newInstance(url: String?) = HomeFragment().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    private val formatLabels = arrayOf(
        "MP4 · 1080p (Best Quality)",
        "MP4 · 720p (Recommended)",
        "MP4 · 480p (Medium)",
        "MP4 · 360p (Small Size)",
        "MP3 · 320kbps (Best Audio)",
        "MP3 · 192kbps",
        "MP3 · 128kbps (Small)"
    )
    private val formatValues = arrayOf(
        DownloadFormat.MP4_1080P.name,
        DownloadFormat.MP4_720P.name,
        DownloadFormat.MP4_480P.name,
        DownloadFormat.MP4_360P.name,
        DownloadFormat.MP3_320.name,
        DownloadFormat.MP3_192.name,
        DownloadFormat.MP3_128.name
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill from share intent
        arguments?.getString(ARG_URL)?.let { binding.etUrl.setText(it) }

        // Format spinner
        binding.spinnerFormat.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, formatLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerFormat.setSelection(1) // default 720p

        binding.btnPaste.setOnClickListener { pasteFromClipboard() }
        binding.btnClear.setOnClickListener {
            binding.etUrl.setText("")
            binding.etTitle.setText("")
        }
        binding.btnDownload.setOnClickListener { initiateDownload() }
        binding.btnSupportedSites.setOnClickListener { showSupportedSites() }
    }

    private fun pasteFromClipboard() {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        if (text.startsWith("http://") || text.startsWith("https://")) {
            binding.etUrl.setText(text)
        } else {
            Snackbar.make(binding.root, "No valid URL in clipboard", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun initiateDownload() {
        val url = binding.etUrl.text.toString().trim()

        // Validate URL
        if (url.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a URL", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            ErrorDialog.show(requireContext(), "Invalid URL",
                "Please enter a valid URL starting with http:// or https://")
            return
        }

        // Check network
        if (!PermissionHelper.isNetworkAvailable(requireContext())) {
            ErrorDialog.showNoNetwork(requireContext()) { initiateDownload() }
            return
        }

        // Check storage permission
        if (!PermissionHelper.hasStoragePermission(requireContext())) {
            ErrorDialog.showPermissionDenied(requireContext())
            return
        }

        // Check WiFi-only setting
        vm.settings.value?.let { s ->
            if (s.wifiOnly && !PermissionHelper.isWifi(requireContext())) {
                ErrorDialog.showWifiOnly(requireContext()) { startDownload(url) }
                return
            }
        }

        startDownload(url)
    }

    private fun startDownload(url: String) {
        val idx       = binding.spinnerFormat.selectedItemPosition
        val format    = formatValues[idx]
        val isAudio   = format.startsWith("MP3")
        val isPlaylist = url.contains("playlist") || url.contains("list=") || url.contains("/playlist/")
        val title     = binding.etTitle.text.toString().trim().ifEmpty {
            if (isPlaylist) "Playlist" else "Video Download"
        }
        val type = when {
            isPlaylist -> DownloadType.PLAYLIST.name
            isAudio    -> DownloadType.AUDIO.name
            else       -> DownloadType.VIDEO.name
        }

        val task = DownloadTask(url = url, title = title, format = format, type = type)

        vm.addTask(task) { taskId ->
            val intent = Intent(requireContext(), DownloadService::class.java).apply {
                action = DownloadService.ACTION_START
                putExtra(DownloadService.EXTRA_TASK_ID, taskId.toInt())
            }
            requireContext().startForegroundService(intent)

            Snackbar.make(binding.root, "Download queued!", Snackbar.LENGTH_LONG)
                .setAction("View Tasks") {
                    (activity as? MainActivity)?.let {
                        it.loadFragment(com.mrbaigsdownloader.ui.tasks.TasksFragment())
                        it.getBottomNav().selectedItemId = R.id.nav_tasks
                    }
                }.show()

            binding.etUrl.setText("")
            binding.etTitle.setText("")
        }
    }

    private fun showSupportedSites() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supported Websites (1000+)")
            .setMessage(
                "YouTube & YouTube Music\n" +
                "TikTok\n" +
                "Instagram (Reels, Posts, Stories)\n" +
                "Twitter / X\n" +
                "Facebook\n" +
                "Vimeo\n" +
                "Dailymotion\n" +
                "SoundCloud\n" +
                "Twitch (Clips & VODs)\n" +
                "Reddit\n" +
                "Pinterest\n" +
                "Bilibili\n" +
                "Tumblr\n" +
                "LinkedIn\n" +
                "Snapchat\n" +
                "...and 1000+ more sites\n\n" +
                "Powered by yt-dlp (no API, no limits)"
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
