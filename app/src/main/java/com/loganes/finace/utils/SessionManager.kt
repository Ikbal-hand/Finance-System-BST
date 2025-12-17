package com.loganes.finace.utils

object SessionManager {
    // Variabel untuk menyimpan cabang user yang sedang login
    // Defaultnya "PUSAT" agar aman
    var currentBranch: String = "PUSAT"

    // Fungsi untuk menghapus sesi (Reset data saat Logout)
    fun clearSession() {
        currentBranch = "PUSAT"
    }
}