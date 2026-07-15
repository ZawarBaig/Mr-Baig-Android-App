package com.mrbaigsdownloader.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): LiveData<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE status IN ('QUEUED','DOWNLOADING','PAUSED') ORDER BY createdAt ASC")
    fun getActiveTasks(): LiveData<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedTasks(): LiveData<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): DownloadTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadTask): Long

    @Update
    suspend fun update(task: DownloadTask)

    @Query("UPDATE download_tasks SET status=:status, progress=:progress, speed=:speed, eta=:eta WHERE id=:id")
    suspend fun updateProgress(id: Int, status: String, progress: Int, speed: String, eta: String)

    @Query("UPDATE download_tasks SET status=:status, filePath=:filePath, size=:size, completedAt=:completedAt WHERE id=:id")
    suspend fun markCompleted(id: Int, status: String, filePath: String, size: String, completedAt: Long)

    @Query("UPDATE download_tasks SET status=:status, errorMessage=:error WHERE id=:id")
    suspend fun markFailed(id: Int, status: String, error: String)

    @Delete
    suspend fun delete(task: DownloadTask)

    @Query("DELETE FROM download_tasks WHERE status='COMPLETED'")
    suspend fun clearCompleted()
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY completedAt DESC")
    fun getAll(): LiveData<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    @Delete
    suspend fun delete(entry: HistoryEntry)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id=1")
    fun getSettings(): LiveData<AppSettings>

    @Query("SELECT * FROM settings WHERE id=1")
    suspend fun getSettingsOnce(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: AppSettings)
}
