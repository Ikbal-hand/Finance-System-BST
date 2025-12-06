package com.loganes.finace.data

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class AutoBackupWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Kode ini akan jalan otomatis di background
        return try {
            Log.d("AutoBackup", "Memulai Backup Otomatis...")

            // Panggil Backup Manager kita
            val backupManager = BackupManager(applicationContext)
            backupManager.backupDatabaseToDownloads()

            Log.d("AutoBackup", "Backup Sukses!")
            Result.success()
        } catch (e: Exception) {
            Log.e("AutoBackup", "Backup Gagal: ${e.message}")
            Result.failure()
        }
    }
}