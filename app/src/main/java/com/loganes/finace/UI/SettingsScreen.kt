package com.loganes.finace.ui.theme

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel, // Menerima ViewModel untuk fitur Data Seeder
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

    // State Dialog Restore
    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedBackupFile by remember { mutableStateOf<File?>(null) }

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
                    Toast.makeText(context, "Login Berhasil: $email", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Login Gagal: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Backup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlueStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(GrayBackground) // Background Modern
                .verticalScroll(rememberScrollState()) // Agar bisa discroll di layar kecil
        ) {

            // --- BAGIAN 1: PENYIMPANAN LOKAL ---
            Text(
                "Penyimpanan Lokal",
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                color = BlueStart,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    SettingsItem(
                        title = "Backup ke HP",
                        subtitle = "Simpan database ke folder Download",
                        icon = Icons.Default.Backup,
                        onClick = { backupManager.backupDatabaseToDownloads() }
                    )

                    Divider(color = Color.LightGray.copy(alpha = 0.2f))

                    SettingsItem(
                        title = "Restore Database",
                        subtitle = "Kembalikan data dari file backup terakhir",
                        icon = Icons.Default.Restore,
                        onClick = {
                            val backupDir = File(context.getExternalFilesDir(null), "Backup_CV_BST")
                            if (backupDir.exists() && backupDir.listFiles()?.isNotEmpty() == true) {
                                val latestFile = backupDir.listFiles()?.maxByOrNull { it.lastModified() }
                                selectedBackupFile = latestFile
                                showRestoreDialog = true
                            } else {
                                Toast.makeText(context, "Tidak ada file backup ditemukan!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // --- BAGIAN 2: GOOGLE DRIVE CLOUD ---
            Text(
                "Google Cloud",
                modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp),
                color = BlueStart,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    if (signedInEmail == null) {
                        // TOMBOL LOGIN
                        SettingsItem(
                            title = "Hubungkan Akun Google",
                            subtitle = "Login agar bisa simpan ke Cloud",
                            icon = Icons.Default.Login,
                            onClick = { launcher.launch(googleSignInClient.signInIntent) }
                        )
                    } else {
                        // INFO AKUN & LOGOUT
                        SettingsItem(
                            title = "Terhubung: $signedInEmail",
                            subtitle = "Ketuk untuk Logout",
                            icon = Icons.Default.Logout,
                            onClick = {
                                googleSignInClient.signOut()
                                securityManager.logoutGoogle()
                                signedInEmail = null
                                Toast.makeText(context, "Logout Berhasil", Toast.LENGTH_SHORT).show()
                            }
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.2f))

                        // TOMBOL UPLOAD
                        if (isUploading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BlueStart)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Sedang mengupload...", color = TextLight)
                            }
                        } else {
                            SettingsItem(
                                title = "Backup ke Google Drive",
                                subtitle = "Upload database terbaru ke Cloud",
                                icon = Icons.Default.CloudUpload,
                                onClick = {
                                    if (signedInEmail != null) {
                                        isUploading = true
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                // 1. Backup Lokal Dulu
                                                backupManager.backupDatabaseToDownloads()

                                                // 2. Cari file terbaru
                                                val backupDir = File(context.getExternalFilesDir(null), "Backup_CV_BST")
                                                val latestFile = backupDir.listFiles()?.maxByOrNull { it.lastModified() }

                                                // 3. Upload
                                                if (latestFile != null) {
                                                    val fileId = driveHelper.uploadFile(latestFile, signedInEmail!!)
                                                    withContext(Dispatchers.Main) {
                                                        isUploading = false
                                                        if (fileId != null) {
                                                            Toast.makeText(context, "Upload Sukses!", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "Upload Gagal. Cek Logcat.", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        isUploading = false
                                                        Toast.makeText(context, "Gagal membuat file lokal.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                withContext(Dispatchers.Main) {
                                                    isUploading = false
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // --- BAGIAN 3: DEVELOPER TOOLS (DATA DUMMY) ---
            Text(
                "Developer Tools",
                modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp),
                color = RedExpense,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                SettingsItem(
                    title = "Generate Data Dummy",
                    subtitle = "Buat 50 transaksi & 10 pegawai palsu",
                    icon = Icons.Default.Add,
                    onClick = {
                        DataSeeder.seedAllData(viewModel)
                        Toast.makeText(context, "Data Dummy Berhasil Dibuat!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Spacer Bawah
            Spacer(modifier = Modifier.height(50.dp))
        }

        // --- DIALOG KONFIRMASI RESTORE ---
        if (showRestoreDialog && selectedBackupFile != null) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Data?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Aplikasi akan restart. Data saat ini akan digantikan oleh backup tanggal:\n\n${selectedBackupFile?.name}")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val success = backupManager.restoreDatabase(selectedBackupFile!!)
                        if (success) {
                            // Restart Aplikasi
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                            Runtime.getRuntime().exit(0)
                        } else {
                            Toast.makeText(context, "Gagal Restore", Toast.LENGTH_SHORT).show()
                        }
                        showRestoreDialog = false
                    }) {
                        Text("RESTORE & RESTART", color = RedExpense, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("BATAL", color = TextLight)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

// Komponen Item Settings Rapi
@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = BlueStart.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = BlueStart)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Text(subtitle, fontSize = 12.sp, color = TextLight)
        }
    }
}