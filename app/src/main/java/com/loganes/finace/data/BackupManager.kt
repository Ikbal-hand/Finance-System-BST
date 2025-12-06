package com.loganes.finace.data

import android.content.Context
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

    fun backupDatabaseToDownloads() {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val exportDir = File(context.getExternalFilesDir(null), "Backup_CV_BST")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val backupFile = File(exportDir, "Backup_$timestamp.sqlite")

            if (dbFile.exists()) {
                copyFile(dbFile, backupFile)
                Log.d("Backup", "File saved at: ${backupFile.absolutePath}")

                // --- PERBAIKAN: Gunakan Handler untuk Toast ---
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Backup Lokal Sukses!", Toast.LENGTH_SHORT).show()
                }
                // ---------------------------------------------
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Database belum ada!", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Gagal Backup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

    // Fungsi Restore
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