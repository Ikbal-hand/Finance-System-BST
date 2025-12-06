package com.loganes.finace.ui.theme

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.loganes.finace.data.SecurityManager

// Warna Background Khusus Login (Clean Look)
private val LoginBackground = Color(0xFFF8F9FA)

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    var inputPin by remember { mutableStateOf("") }
    val correctPin = securityManager.getPin()

    // Logic Login Otomatis saat 6 digit
    LaunchedEffect(inputPin) {
        if (inputPin.length == 6) {
            if (inputPin == correctPin) {
                onLoginSuccess()
            } else {
                Toast.makeText(context, "PIN Salah!", Toast.LENGTH_SHORT).show()
                inputPin = "" // Reset jika salah
            }
        }
    }

    // Logic Biometrik
    val activity = context as? FragmentActivity
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val biometricPrompt = remember {
        activity?.let {
            BiometricPrompt(it, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onLoginSuccess()
                }
            })
        }
    }

    Scaffold(
        containerColor = LoginBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- HEADER BRANDING ---
            // Lingkaran Logo
            Surface(
                shape = CircleShape,
                color = BlueStart.copy(alpha = 0.1f), // Warna lembut
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = BlueStart,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Teks Judul
            Text(
                text = "CV BST FINANCE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Masukkan PIN Keamanan",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- INDIKATOR PIN (DOTS) ---
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(6) { index ->
                    val isFilled = index < inputPin.length
                    // Dot Biru jika terisi, Abu jika kosong
                    val dotColor = if (isFilled) BlueStart else Color.LightGray.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // --- NUMPAD MODERN ---
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { label ->
                        NumpadButtonMinimalist(
                            text = label,
                            onClick = {
                                when (label) {
                                    "DEL" -> if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                                    "BIO" -> {
                                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                            .setTitle("Verifikasi Biometrik")
                                            .setSubtitle("Sentuh sensor sidik jari")
                                            .setNegativeButtonText("Gunakan PIN")
                                            .build()
                                        biometricPrompt?.authenticate(promptInfo)
                                    }
                                    else -> if (inputPin.length < 6) inputPin += label
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun NumpadButtonMinimalist(text: String, onClick: () -> Unit) {
    // Tombol Bulat Putih dengan Shadow Halus (Neumorphic style)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 2.dp, // Shadow tipis agar timbul
        modifier = Modifier.size(75.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (text) {
                "DEL" -> Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Hapus",
                    tint = TextDark
                )
                "BIO" -> Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometrik",
                    tint = BlueStart // Aksen warna pada tombol spesial
                )
                else -> Text(
                    text = text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
            }
        }
    }
}