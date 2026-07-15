package com.mrbaigsdownloader.data

import android.content.Context

class Repository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    val taskDao = db.downloadTaskDao()
    val historyDao = db.historyDao()
    val settingsDao = db.settingsDao()

    // Tasks
    val allTasks = taskDao.getAllTasks()
    val activeTasks = taskDao.getActiveTasks()
    val completedTasks = taskDao.getCompletedTasks()

    suspend fun addTask(task: DownloadTask): Long = taskDao.insert(task)
    suspend fun updateTask(task: DownloadTask) = taskDao.update(task)
    suspend fun deleteTask(task: DownloadTask) = taskDao.delete(task)
    suspend fun clearCompleted() = taskDao.clearCompleted()

    // History
    val history = historyDao.getAll()
    suspend fun addHistory(entry: HistoryEntry) = historyDao.insert(entry)
    suspend fun deleteHistory(entry: HistoryEntry) = historyDao.delete(entry)
    suspend fun clearHistory() = historyDao.clearAll()

    // Settings
    val settings = settingsDao.getSettings()
    suspend fun saveSettings(s: AppSettings) = settingsDao.save(s)
    suspend fun getSettings(): AppSettings = settingsDao.getSettingsOnce() ?: AppSettings()
}
