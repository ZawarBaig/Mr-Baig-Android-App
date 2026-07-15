package com.mrbaigsdownloader.ui.downloads

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mrbaigsdownloader.databinding.FragmentDownloadsBinding
import com.mrbaigsdownloader.ui.MainViewModel
import java.io.File

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val vm: MainViewModel by activityViewModels()
    private lateinit var adapter: DownloadsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DownloadsAdapter(
            onOpen  = { entry -> openFile(entry.filePath) },
            onShare = { entry -> shareFile(entry.filePath) },
            onDelete= { entry ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete from history?")
                    .setMessage("Remove \"${entry.title}\" from history?\n(The file won't be deleted.)")
                    .setPositiveButton("Remove") { _, _ -> vm.deleteHistory(entry) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        vm.history.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.tvCount.text = "${list.size} downloads"
        }

        binding.btnClearAll.setOnClickListener {
            if (adapter.itemCount == 0) return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All History?")
                .setMessage("This will remove all history entries. Downloaded files are kept.")
                .setPositiveButton("Clear All") { _, _ ->
                    vm.clearHistory()
                    Snackbar.make(binding.root, "History cleared", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun openFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Snackbar.make(binding.root, "File not found", Snackbar.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        val mime = if (path.endsWith(".mp3")) "audio/mpeg" else "video/mp4"
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun shareFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Snackbar.make(binding.root, "File not found", Snackbar.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        val mime = if (path.endsWith(".mp3")) "audio/mpeg" else "video/mp4"
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share via"
        ))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
