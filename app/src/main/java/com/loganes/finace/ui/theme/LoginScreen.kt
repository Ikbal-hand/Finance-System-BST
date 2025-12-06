package com.loganes.finace.ui.theme

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.loganes.finace.data.SecurityManager

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    var inputPin by remember { mutableStateOf("") }
    val correctPin = securityManager.getPin()

    // Logic Login
    LaunchedEffect(inputPin) {
        if (inputPin.length == 6) {
            if (inputPin == correctPin) {
                onLoginSuccess()
            } else {
                Toast.makeText(context, "PIN Salah!", Toast.LENGTH_SHORT).show()
                inputPin = ""
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

    // UI Login Modern dengan Gradasi Penuh
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BlueStart, BlueEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Gembok Besar
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("CV BST FINANCE", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Masukkan PIN Keamanan", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))

            Spacer(modifier = Modifier.height(48.dp))

            // Indikator PIN (Dots)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < inputPin.length) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Numpad Modern
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
                        NumpadButtonModern(text = label, onClick = {
                            when (label) {
                                "DEL" -> if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                                "BIO" -> {
                                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Login Biometrik")
                                        .setSubtitle("Sentuh sensor sidik jari")
                                        .setNegativeButtonText("Gunakan PIN")
                                        .build()
                                    biometricPrompt?.authenticate(promptInfo)
                                }
                                else -> if (inputPin.length < 6) inputPin += label
                            }
                        })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun NumpadButtonModern(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(75.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f)) // Transparan kaca
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text == "DEL") {
            Icon(Icons.Default.Backspace, contentDescription = "Hapus", tint = Color.White)
        } else if (text == "BIO") {
            Icon(Icons.Default.Fingerprint, contentDescription = "Biometrik", tint = Color.White)
        } else {
            Text(text, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}