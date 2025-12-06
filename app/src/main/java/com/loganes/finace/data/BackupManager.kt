package com.loganes.finace.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {

    private val DB_NAME = "cv_bst_database"
    private val PREFS_NAME = "backup_prefs"

    companion object {
        // KUNCI PENYIMPANAN TERPISAH
        const val KEY_LOCAL_TIME = "local_backup_time"
        const val KEY_LOCAL_STATUS = "local_backup_status"

        const val KEY_CLOUD_TIME = "cloud_backup_time"
        const val KEY_CLOUD_STATUS = "cloud_backup_status"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- BACKUP LOKAL ---
    fun backupDatabaseToDownloads(): File? {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val exportDir = File(context.getExternalFilesDir(null), "Backup_CV_BST")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val backupFile = File(exportDir, "Backup_$timestamp.sqlite")

            if (dbFile.exists()) {
                copyFile(dbFile, backupFile)
                Log.d("Backup", "File saved at: ${backupFile.absolutePath}")

                // Simpan Status Lokal: SUKSES
                saveLocalStatus(true)

                if (Looper.myLooper() == Looper.getMainLooper()) {
                    Toast.makeText(context, "Backup Lokal Sukses!", Toast.LENGTH_SHORT).show()
                }
                backupFile
            } else {
                saveLocalStatus(false)
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            saveLocalStatus(false)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(context, "Gagal Backup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }

    // --- HELPER STATUS ---

    private fun saveLocalStatus(isSuccess: Boolean) {
        val time = SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")).format(Date())
        prefs.edit()
            .putString(KEY_LOCAL_TIME, time)
            .putString(KEY_LOCAL_STATUS, if(isSuccess) "SUCCESS" else "FAILED")
            .apply()
    }

    // Fungsi Publik untuk dipanggil oleh Worker setelah upload Drive selesai
    fun saveCloudStatus(isSuccess: Boolean) {
        val time = SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")).format(Date())
        prefs.edit()
            .putString(KEY_CLOUD_TIME, time)
            .putString(KEY_CLOUD_STATUS, if(isSuccess) "SUCCESS" else "FAILED")
            .apply()
    }

    // Ambil Status untuk Dashboard (Return: Map data)
    fun getAllBackupStatus(): Map<String, String> {
        return mapOf(
            "local_status" to (prefs.getString(KEY_LOCAL_STATUS, "NONE") ?: "NONE"),
            "local_time" to (prefs.getString(KEY_LOCAL_TIME, "-") ?: "-"),
            "cloud_status" to (prefs.getString(KEY_CLOUD_STATUS, "NONE") ?: "NONE"),
            "cloud_time" to (prefs.getString(KEY_CLOUD_TIME, "-") ?: "-")
        )
    }

    private fun copyFile(src: File, dst: File) {
        val inChannel: FileChannel = FileInputStream(src).channel
        val outChannel: FileChannel = FileOutputStream(dst).channel
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel)
        } finally {
            if (inChannel.isOpen) inChannel.close()
            if (outChannel.isOpen) outChannel.close()
        }
    }

    fun restoreDatabase(backupFile: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (backupFile.exists()) {
                copyFile(backupFile, dbFile)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}