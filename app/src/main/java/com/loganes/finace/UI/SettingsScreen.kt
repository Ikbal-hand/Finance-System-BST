package com.loganes.finace.ui.theme

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.loganes.finace.data.BackupManager
import com.loganes.finace.data.DataSeeder
import com.loganes.finace.data.GoogleDriveHelper
import com.loganes.finace.data.SecurityManager
import com.loganes.finace.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Warna Background Modern
private val ScreenBackground = Color(0xFFF8F9FA)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Helpers
    val backupManager = remember { BackupManager(context) }
    val securityManager = remember { SecurityManager(context) }
    val driveHelper = remember { GoogleDriveHelper(context) }

    // State
    var signedInEmail by remember { mutableStateOf(securityManager.getGoogleEmail()) }
    var isUploading by remember { mutableStateOf(false) }

    // Dialog State
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) } // State Dialog Ganti PIN
    var selectedBackupFile by remember { mutableStateOf<File?>(null) }

    // Dialog Reset Data (Danger Zone)
    var showResetDialog by remember { mutableStateOf(false) }
    var showFinalResetConfirm by remember { mutableStateOf(false) }

    // --- SETUP GOOGLE SIGN IN ---
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .build()

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account.email
                if (email != null) {
                    signedInEmail = email
                    securityManager.saveGoogleEmail(email)
                    Toast.makeText(context, "Terhubung sebagai $email", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Gagal Login: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueStart)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- SECTION 1: KEAMANAN (BARU) ---
            SettingsSection(title = "Keamanan & Akun", color = Color(0xFF673AB7)) { // Ungu Keamanan
                SettingsItemModern(
                    title = "Ganti PIN Login",
                    subtitle = "Ubah kode keamanan aplikasi (6 Digit)",
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFF673AB7),
                    onClick = { showChangePinDialog = true }
                )
            }

            // --- SECTION 2: PENYIMPANAN LOKAL ---
            SettingsSection(title = "Penyimpanan Lokal", color = BlueStart) {
                SettingsItemModern(
                    title = "Backup ke HP",
                    subtitle = "Simpan database ke folder Download",
                    icon = Icons.Default.SaveAlt,
                    iconColor = BlueStart,
                    onClick = {
                        backupManager.backupDatabaseToDownloads()
                        Toast.makeText(context, "Backup disimpan di Download", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = Color(0xFFF5F5F5))
                SettingsItemModern(
                    title = "Restore Database",
                    subtitle = "Kembalikan data dari backup terakhir",
                    icon = Icons.Default.Restore,
                    iconColor = BlueStart,
                    onClick = {
                        val backupDir = File(context.getExternalFilesDir(null), "Backup_CV_BST")
                        if (backupDir.exists() && backupDir.listFiles()?.isNotEmpty() == true) {
                            val latestFile = backupDir.listFiles()?.maxByOrNull { it.lastModified() }
                            selectedBackupFile = latestFile
                            showRestoreDialog = true
                        } else {
                            Toast.makeText(context, "Belum ada file backup lokal!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // --- SECTION 3: GOOGLE DRIVE ---
            SettingsSection(title = "Cloud Backup", color = Color(0xFFEA4335)) { // Google Red
                if (signedInEmail == null) {
                    SettingsItemModern(
                        title = "Hubungkan Google Drive",
                        subtitle = "Login untuk simpan data ke awan",
                        icon = Icons.Default.Login,
                        iconColor = Color(0xFFEA4335),
                        onClick = { launcher.launch(googleSignInClient.signInIntent) }
                    )
                } else {
                    SettingsItemModern(
                        title = "Akun: $signedInEmail",
                        subtitle = "Ketuk untuk Logout / Ganti Akun",
                        icon = Icons.Default.AccountCircle,
                        iconColor = Color(0xFFEA4335),
                        onClick = {
                            googleSignInClient.signOut()
                            securityManager.logoutGoogle()
                            signedInEmail = null
                            Toast.makeText(context, "Akun Terputus", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF5F5F5))

                    if (isUploading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BlueStart, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Mengupload ke Drive...", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        SettingsItemModern(
                            title = "Upload Backup Sekarang",
                            subtitle = "Kirim data terbaru ke Google Drive",
                            icon = Icons.Default.CloudUpload,
                            iconColor = BlueStart,
                            onClick = {
                                if (signedInEmail != null) {
                                    isUploading = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            // 1. Backup Lokal Dulu
                                            backupManager.backupDatabaseToDownloads()

                                            val backupDir = File(context.getExternalFilesDir(null), "Backup_CV_BST")
                                            val latestFile = backupDir.listFiles()?.maxByOrNull { it.lastModified() }

                                            if (latestFile != null) {
                                                // 2. Upload ke Drive
                                                val fileId = driveHelper.uploadFile(latestFile, signedInEmail!!)

                                                withContext(Dispatchers.Main) {
                                                    isUploading = false
                                                    if (fileId != null) {
                                                        // --- PERBAIKAN: SIMPAN STATUS CLOUD ---
                                                        backupManager.saveCloudStatus(true)
                                                        // --------------------------------------

                                                        Toast.makeText(context, "Berhasil Upload ke Drive!", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        backupManager.saveCloudStatus(false) // Simpan status gagal
                                                        Toast.makeText(context, "Gagal Upload. Cek Koneksi.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isUploading = false
                                                backupManager.saveCloudStatus(false) // Simpan status error
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // --- SECTION 4: PENGEMBANG (DEV TOOLS) ---
            SettingsSection(title = "Alat Pengembang", color = Color(0xFFFB8C00)) {
                SettingsItemModern(
                    title = "Generate Data Dummy",
                    subtitle = "Isi database dengan data palsu (Testing)",
                    icon = Icons.Default.Science,
                    iconColor = Color(0xFFFB8C00),
                    onClick = {
                        DataSeeder.seedAllData(viewModel)
                        Toast.makeText(context, "Data Dummy Ditambahkan!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // --- SECTION 5: ZONA BAHAYA ---
            SettingsSection(title = "Zona Bahaya", color = RedExpense) {
                SettingsItemModern(
                    title = "Kosongkan Semua Data",
                    subtitle = "Hapus seluruh transaksi & pegawai (Reset Pabrik)",
                    icon = Icons.Default.DeleteForever,
                    iconColor = RedExpense,
                    textColor = RedExpense,
                    bgColor = RedExpense.copy(alpha = 0.05f),
                    onClick = { showResetDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- DIALOGS ---

    // 1. Ganti PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            securityManager = securityManager,
            onDismiss = { showChangePinDialog = false }
        )
    }

    // 2. Restore Dialog
    if (showRestoreDialog && selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Pulihkan Data?", fontWeight = FontWeight.Bold) },
            text = { Text("Aplikasi akan dimuat ulang. Data saat ini akan ditimpa oleh backup tanggal:\n\n${selectedBackupFile?.name}") },
            confirmButton = {
                Button(
                    onClick = {
                        val success = backupManager.restoreDatabase(selectedBackupFile!!)
                        if (success) {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                            Runtime.getRuntime().exit(0)
                        } else {
                            Toast.makeText(context, "Gagal memulihkan data", Toast.LENGTH_SHORT).show()
                        }
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueStart)
                ) { Text("RESTORE & RESTART") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("BATAL") } },
            containerColor = Color.White
        )
    }

    // 3. Reset Alert Tahap 1
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = RedExpense) },
            title = { Text("Peringatan Keras") },
            text = { Text("Apakah Anda sudah melakukan backup data? Data yang dihapus tidak dapat dikembalikan lagi.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    showFinalResetConfirm = true
                }) { Text("SUDAH, LANJUT", fontWeight = FontWeight.Bold, color = RedExpense) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("BATAL") }
            },
            containerColor = Color.White
        )
    }

    // 4. Reset Alert Tahap 2
    if (showFinalResetConfirm) {
        AlertDialog(
            onDismissRequest = { showFinalResetConfirm = false },
            title = { Text("Konfirmasi Akhir", color = RedExpense, fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus SELURUH database? Aplikasi akan kembali kosong seperti baru diinstal.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showFinalResetConfirm = false
                        Toast.makeText(context, "Sistem Direset. Data Kosong.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense)
                ) { Text("YA, HAPUS SEMUA") }
            },
            dismissButton = { TextButton(onClick = { showFinalResetConfirm = false }) { Text("JANGAN") } },
            containerColor = Color.White
        )
    }
}

// --- KOMPONEN DIALOG GANTI PIN (FIXED COLOR) ---
@Composable
fun ChangePinDialog(
    securityManager: SecurityManager,
    onDismiss: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    // PERBAIKAN WARNA TEKS INPUT (Agar terlihat Hitam di bg Putih)
    val pinInputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = BlueStart,
        focusedBorderColor = BlueStart,
        unfocusedBorderColor = Color.LightGray,
        focusedLabelColor = BlueStart,
        unfocusedLabelColor = Color.Gray,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti PIN Keamanan", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // PIN Lama
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) oldPin = it },
                    label = { Text("PIN Lama") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = pinInputColors // <--- Warna Hitam
                )

                // PIN Baru
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("PIN Baru (6 Digit)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = pinInputColors // <--- Warna Hitam
                )

                // Konfirmasi PIN
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Konfirmasi PIN Baru") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = pinInputColors // <--- Warna Hitam
                )

                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = RedExpense, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val currentStoredPin = securityManager.getPin()
                    if (oldPin != currentStoredPin) {
                        errorMsg = "PIN Lama salah!"
                    } else if (newPin.length != 6) {
                        errorMsg = "PIN Baru harus 6 digit!"
                    } else if (newPin != confirmPin) {
                        errorMsg = "Konfirmasi PIN tidak cocok!"
                    } else {
                        // Simpan PIN Baru
                        securityManager.savePin(newPin)
                        Toast.makeText(context, "PIN Berhasil Diganti!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueStart),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SIMPAN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

// --- KOMPONEN UI HELPER ---

@Composable
fun SettingsSection(
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItemModern(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color = TextDark,
    bgColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = iconColor)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
        }

        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}