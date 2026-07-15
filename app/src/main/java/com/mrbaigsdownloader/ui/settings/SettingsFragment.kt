package com.mrbaigsdownloader.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.mrbaigsdownloader.data.AppSettings
import com.mrbaigsdownloader.data.DownloadFormat
import com.mrbaigsdownloader.databinding.FragmentSettingsBinding
import com.mrbaigsdownloader.service.DownloadService
import com.mrbaigsdownloader.ui.MainViewModel
import com.mrbaigsdownloader.utils.ErrorDialog
import com.mrbaigsdownloader.utils.PermissionHelper

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val vm: MainViewModel by activityViewModels()
    private var currentSettings = AppSettings()

    private val formatLabels = arrayOf(
        "MP4 · 720p", "MP4 · 1080p", "MP4 · 480p", "MP4 · 360p",
        "MP3 · 320kbps", "MP3 · 192kbps", "MP3 · 128kbps"
    )
    private val formatValues = arrayOf(
        DownloadFormat.MP4_720P.name, DownloadFormat.MP4_1080P.name,
        DownloadFormat.MP4_480P.name, DownloadFormat.MP4_360P.name,
        DownloadFormat.MP3_320.name, DownloadFormat.MP3_192.name, DownloadFormat.MP3_128.name
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.spinnerDefaultFormat.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, formatLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.sliderConcurrent.addOnChangeListener { _, value, _ ->
            binding.tvConcurrentVal.text = "${value.toInt()} simultaneous"
        }

        vm.settings.observe(viewLifecycleOwner) { s ->
            s ?: return@observe
            currentSettings = s
            populateUi(s)
        }

        binding.btnSave.setOnClickListener { saveSettings() }

        // Update yt-dlp button
        binding.btnUpdateYtdlp.setOnClickListener { updateYtDlp() }

        // Storage permission button
        binding.btnGrantStorage.setOnClickListener {
            if (!PermissionHelper.hasStoragePermission(requireContext())) {
                PermissionHelper.requestAll(requireActivity())
            } else {
                Toast.makeText(requireContext(), "Storage permission already granted ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateUi(s: AppSettings) {
        binding.switchDarkMode.isChecked    = s.darkMode
        binding.switchEmbedMeta.isChecked   = s.embedMetadata
        binding.switchWifiOnly.isChecked    = s.wifiOnly
        binding.switchAutoRetry.isChecked   = s.autoRetry
        binding.etDownloadPath.setText(s.downloadPath)
        binding.sliderConcurrent.value      = s.maxConcurrentDownloads.toFloat()
        binding.tvConcurrentVal.text        = "${s.maxConcurrentDownloads} simultaneous"
        val idx = formatValues.indexOf(s.defaultFormat).coerceAtLeast(0)
        binding.spinnerDefaultFormat.setSelection(idx)
    }

    private fun saveSettings() {
        val updated = currentSettings.copy(
            darkMode               = binding.switchDarkMode.isChecked,
            embedMetadata          = binding.switchEmbedMeta.isChecked,
            wifiOnly               = binding.switchWifiOnly.isChecked,
            autoRetry              = binding.switchAutoRetry.isChecked,
            downloadPath           = binding.etDownloadPath.text.toString().trim()
                .ifEmpty { "Downloads/MrBaigDownloader" },
            maxConcurrentDownloads = binding.sliderConcurrent.value.toInt(),
            defaultFormat          = formatValues.getOrElse(
                binding.spinnerDefaultFormat.selectedItemPosition) { DownloadFormat.MP4_720P.name }
        )
        vm.saveSettings(updated)
        Snackbar.make(binding.root, "Settings saved ✅", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateYtDlp() {
        if (!PermissionHelper.isNetworkAvailable(requireContext())) {
            ErrorDialog.showNoNetwork(requireContext())
            return
        }
        ErrorDialog.showYtDlpUpdate(requireContext()) {
            Toast.makeText(requireContext(), "Updating yt-dlp…", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), DownloadService::class.java).apply {
                action = DownloadService.ACTION_UPDATE_YTDLP
            }
            requireContext().startService(intent)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
