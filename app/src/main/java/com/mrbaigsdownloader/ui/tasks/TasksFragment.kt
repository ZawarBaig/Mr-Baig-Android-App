package com.mrbaigsdownloader.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mrbaigsdownloader.data.DownloadStatus
import com.mrbaigsdownloader.data.DownloadTask
import com.mrbaigsdownloader.databinding.FragmentTasksBinding
import com.mrbaigsdownloader.service.DownloadService
import com.mrbaigsdownloader.ui.MainViewModel

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val vm: MainViewModel by activityViewModels()
    private lateinit var adapter: TasksAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TasksAdapter(
            onPause   = { task -> sendServiceAction(DownloadService.ACTION_PAUSE, task.id) },
            onResume  = { task -> sendServiceAction(DownloadService.ACTION_RESUME, task.id) },
            onCancel  = { task -> confirmCancel(task) },
            onDelete  = { task -> vm.deleteTask(task) }
        )

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter

        vm.allTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
            binding.emptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnClearCompleted.setOnClickListener {
            vm.clearCompleted()
            Snackbar.make(binding.root, "Completed tasks cleared", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnCancelAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel All Downloads?")
                .setMessage("This will cancel all active downloads.")
                .setPositiveButton("Cancel All") { _, _ ->
                    val intent = Intent(requireContext(), DownloadService::class.java).apply {
                        action = DownloadService.ACTION_CANCEL_ALL
                    }
                    requireContext().startService(intent)
                }
                .setNegativeButton("Keep", null)
                .show()
        }
    }

    private fun sendServiceAction(action: String, taskId: Int) {
        val intent = Intent(requireContext(), DownloadService::class.java).apply {
            this.action = action
            putExtra(DownloadService.EXTRA_TASK_ID, taskId)
        }
        requireContext().startService(intent)
    }

    private fun confirmCancel(task: DownloadTask) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Download?")
            .setMessage("Cancel \"${task.title}\"?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                sendServiceAction(DownloadService.ACTION_CANCEL, task.id)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
