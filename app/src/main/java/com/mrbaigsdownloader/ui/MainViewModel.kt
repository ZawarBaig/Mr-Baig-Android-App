package com.mrbaigsdownloader.ui

import android.app.Application
import androidx.lifecycle.*
import com.mrbaigsdownloader.data.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    // ── Exposed LiveData ──────────────────────────────────────────────────────
    val allTasks       = repo.allTasks
    val activeTasks    = repo.activeTasks
    val completedTasks = repo.completedTasks
    val history        = repo.history
    val settings       = repo.settings

    // ── Task ops ─────────────────────────────────────────────────────────────
    fun addTask(task: DownloadTask, onAdded: (Long) -> Unit) = viewModelScope.launch {
        val id = repo.addTask(task)
        onAdded(id)
    }

    fun deleteTask(task: DownloadTask) = viewModelScope.launch {
        repo.deleteTask(task)
    }

    fun clearCompleted() = viewModelScope.launch {
        repo.clearCompleted()
    }

    // ── History ops ───────────────────────────────────────────────────────────
    fun deleteHistory(entry: HistoryEntry) = viewModelScope.launch {
        repo.deleteHistory(entry)
    }

    fun clearHistory() = viewModelScope.launch {
        repo.clearHistory()
    }

    // ── Settings ops ─────────────────────────────────────────────────────────
    fun saveSettings(s: AppSettings) = viewModelScope.launch {
        repo.saveSettings(s)
    }
}
