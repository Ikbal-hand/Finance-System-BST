package com.loganes.finace.data // Sesuaikan package
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityManager(context: Context) {

    // Kita gunakan EncryptedSharedPreferences agar PIN aman tidak bisa dibaca hacker
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

    // Simpan PIN Baru
    fun savePin(pin: String) {
        prefs.edit().putString("USER_PIN", pin).apply()
    }

    // Ambil PIN (Default: 123456 jika belum disetting)
    fun getPin(): String {
        return prefs.getString("USER_PIN", "123456") ?: "123456"
    }

    // Cek apakah fitur Biometrik diaktifkan user?
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("BIOMETRIC_ENABLED", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("BIOMETRIC_ENABLED", enabled).apply()
    }
    fun saveGoogleEmail(email: String) {
        prefs.edit().putString("GOOGLE_EMAIL", email).apply()
    }

    fun getGoogleEmail(): String? {
        return prefs.getString("GOOGLE_EMAIL", null)
    }

    fun logoutGoogle() {
        prefs.edit().remove("GOOGLE_EMAIL").apply()
    }
}