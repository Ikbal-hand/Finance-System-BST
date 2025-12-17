package com.loganes.finace.ui.screen.settings

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.loganes.finace.utils.SessionManager
import com.loganes.finace.viewmodel.TransactionViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToReport: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userEmail = auth.currentUser?.email ?: "User"
    val userBranch by viewModel.userBranch.collectAsState()
    val isPusat = userBranch == "PUSAT"

    // Dialog States
    var showCapitalDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPassDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // PROFIL CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(userEmail.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(userEmail, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Akses: $userBranch", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            // KEAMANAN AKUN
            Spacer(modifier = Modifier.height(24.dp))
            Text("Keamanan Akun", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(icon = Icons.Outlined.Mail, title = "Ganti Email Login", onClick = { showEmailDialog = true })
            SettingsItem(icon = Icons.Outlined.LockReset, title = "Ganti Password", onClick = { showPassDialog = true })

            // MENU UMUM
            Spacer(modifier = Modifier.height(24.dp))
            Text("Menu Umum", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            // -- HANYA PUSAT YG BISA AKSES LAPORAN & SAMPAH --
            // Sesuai request: Staff cabang tidak bisa akses laporan
            if (isPusat) {
                SettingsItem(icon = Icons.Default.Description, title = "Laporan & Export", onClick = onNavigateToReport)
                SettingsItem(icon = Icons.Default.DeleteOutline, title = "Sampah (Trashbin)", onClick = onNavigateToTrash)

                Spacer(modifier = Modifier.height(24.dp))
                Text("Admin Area", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                SettingsItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Isi Modal Awal",
                    subtitle = "Suntikan dana ke Kas Pusat",
                    onClick = { showCapitalDialog = true }
                )
            } else {
                Text("Menu Laporan & Sampah dibatasi untuk Cabang.", fontSize = 12.sp, color = Color.LightGray, modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // LOGOUT BUTTON
            Button(
                onClick = {
                    auth.signOut()
                    SessionManager.clearSession()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar Aplikasi", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DIALOGS ---

    // 1. GANTI EMAIL
    if (showEmailDialog) {
        var newEmail by remember { mutableStateOf("") }
        SimpleInputDialog(
            title = "Ganti Email",
            label = "Email Baru",
            value = newEmail,
            onValueChange = { newEmail = it },
            onDismiss = { showEmailDialog = false },
            onConfirm = {
                if (newEmail.isNotEmpty()) {
                    viewModel.updateEmail(newEmail,
                        onSuccess = { Toast.makeText(context, "Email Berhasil Diubah!", Toast.LENGTH_SHORT).show(); showEmailDialog = false },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                }
            }
        )
    }

    // 2. GANTI PASSWORD
    if (showPassDialog) {
        var newPass by remember { mutableStateOf("") }
        SimpleInputDialog(
            title = "Ganti Password",
            label = "Password Baru (Min 6 Karakter)",
            value = newPass,
            onValueChange = { newPass = it },
            onDismiss = { showPassDialog = false },
            onConfirm = {
                if (newPass.length >= 6) {
                    viewModel.updatePassword(newPass,
                        onSuccess = { Toast.makeText(context, "Password Berhasil Diubah!", Toast.LENGTH_SHORT).show(); showPassDialog = false },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                } else {
                    Toast.makeText(context, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 3. MODAL AWAL (Existing)
    if (showCapitalDialog) {
        var amountStr by remember { mutableStateOf("") }
        SimpleInputDialog(
            title = "Tambah Modal Pusat",
            label = "Nominal (Rp)",
            value = amountStr,
            onValueChange = { if (it.all { c -> c.isDigit() }) amountStr = it },
            isNumber = true,
            onDismiss = { showCapitalDialog = false },
            onConfirm = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    viewModel.addCapital(amt)
                    Toast.makeText(context, "Modal Berhasil Ditambahkan", Toast.LENGTH_SHORT).show()
                    showCapitalDialog = false
                }
            }
        )
    }
}

// Helper Composable untuk Dialog Input Sederhana
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleInputDialog(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isNumber: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value, onValueChange = onValueChange,
                    label = { Text(label) },
                    keyboardOptions = if(isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) }
                    Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Simpan") }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}