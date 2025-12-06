package com.loganes.finace.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.util.Date

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("AutoBackup", "--- Memulai Proses Backup Otomatis Pukul ${Date()} ---")

        return try {
            val context = applicationContext
            val backupManager = BackupManager(context)
            val securityManager = SecurityManager(context)
            // Pastikan Anda sudah membuat class GoogleDriveHelper sebelumnya
            val driveHelper = GoogleDriveHelper(context)

            // 1. LAKUKAN BACKUP LOKAL (HP)
            // Kita panggil fungsi backup. Nanti di langkah selanjutnya,
            // kita akan ubah BackupManager agar mengembalikan File hasil backupnya.
            val backupFile = backupManager.backupDatabaseToDownloads()

            if (backupFile != null && backupFile.exists()) {
                Log.d("AutoBackup", "✅ Backup Lokal Sukses: ${backupFile.name}")

                // 2. LAKUKAN BACKUP CLOUD (DRIVE) - Jika User Login
                val signedInEmail = securityManager.getGoogleEmail()

                if (signedInEmail != null) {
                    Log.d("AutoBackup", "Akun Google ditemukan ($signedInEmail). Mengupload ke Drive...")

                    // Upload file hasil backup lokal tadi
                    val fileId = driveHelper.uploadFile(backupFile, signedInEmail)

                    if (fileId != null) {
                        Log.d("AutoBackup", "✅ Upload Drive Sukses! File ID: $fileId")
                        // Simpan status sukses untuk notifikasi (nanti kita update BackupManager utk ini)
                        backupManager.saveCloudStatus(true)
                    } else {
                        Log.e("AutoBackup", "❌ Upload Drive Gagal (Cek Koneksi Internet)")
                        backupManager.saveCloudStatus(false)
                    }
                } else {
                    Log.d("AutoBackup", "⚠️ Upload Drive Skip (User belum login di Settings)")
                }

                Result.success()
            } else {
                Log.e("AutoBackup", "❌ Gagal membuat file backup lokal.")
                Result.retry() // Coba lagi nanti
            }

        } catch (e: Exception) {
            Log.e("AutoBackup", "Critical Error: ${e.message}")
            e.printStackTrace()
            Result.failure()
        }
    }
}