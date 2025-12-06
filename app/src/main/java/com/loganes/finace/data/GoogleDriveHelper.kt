package com.loganes.finace.data

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.util.Collections

class GoogleDriveHelper(private val context: Context) {

    companion object {
        // Minta akses hanya untuk mengelola file yang dibuat oleh aplikasi ini (Lebih aman/Privasi terjaga)
        // Atau gunakan DriveScopes.DRIVE_FILE untuk akses file umum
        val SCOPES = listOf(DriveScopes.DRIVE_FILE)
    }

    // Fungsi Upload File ke Drive
    fun uploadFile(localFile: File, email: String): String? {
        try {
            // 1. Setup Kredensial (Login pakai Email yang disimpan)
            val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
            credential.selectedAccountName = email

            // 2. Setup Service Drive
            val service = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("CV BST Finance")
                .build()

            // 3. Metadata File (Nama & Tipe di Drive)
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = localFile.name // Misal: Backup_2025-12-05.sqlite
            fileMetadata.parents = Collections.singletonList("root") // Simpan di folder utama (My Drive)

            // 4. Isi File
            val mediaContent = FileContent("application/x-sqlite3", localFile)

            // 5. Eksekusi Upload
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            return uploadedFile.id // Kembalikan ID file jika sukses

        } catch (e: Exception) {
            android.util.Log.e("CekDisini", "Gagal Upload: ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }
}