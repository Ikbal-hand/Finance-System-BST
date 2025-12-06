package com.loganes.finace.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityManager(context: Context) {

    // Kita gunakan EncryptedSharedPreferences agar PIN aman tidak bisa dibaca hacker
    // MasterKey digunakan untuk mengenkripsi kunci enkripsi preferensi
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_app_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PIN = "USER_PIN"
        private const val KEY_BIOMETRIC = "BIOMETRIC_ENABLED"
        private const val KEY_GOOGLE_EMAIL = "GOOGLE_EMAIL"
        private const val DEFAULT_PIN = "123456"
    }

    // Simpan PIN Baru
    fun savePin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    // Ambil PIN (Default: 123456 jika belum disetting)
    fun getPin(): String {
        return prefs.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    // Cek apakah fitur Biometrik diaktifkan user?
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC, false)
    }

    // Aktifkan/Nonaktifkan Biometrik
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    // Simpan Email Google
    fun saveGoogleEmail(email: String) {
        prefs.edit().putString(KEY_GOOGLE_EMAIL, email).apply()
    }

    // Ambil Email Google
    fun getGoogleEmail(): String? {
        return prefs.getString(KEY_GOOGLE_EMAIL, null)
    }

    // Logout Google
    fun logoutGoogle() {
        prefs.edit().remove(KEY_GOOGLE_EMAIL).apply()
    }

    // Reset Data Keamanan (Opsional, untuk fitur 'Hapus Data')
    fun resetSecurity() {
        prefs.edit().clear().apply()
    }
}