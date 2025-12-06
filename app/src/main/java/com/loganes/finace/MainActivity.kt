// File: MainActivity.kt

package com.loganes.finace


import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loganes.finace.data.AppDatabase
import com.loganes.finace.data.AutoBackupWorker
import com.loganes.finace.UI.DashboardScreen

import com.loganes.finace.ui.theme.AddTransactionScreen
import com.loganes.finace.ui.theme.EditTransactionScreen
import com.loganes.finace.ui.theme.EmployeeListScreen
import com.loganes.finace.ui.theme.LoginScreen
import com.loganes.finace.ui.theme.MyApplicationTheme
import com.loganes.finace.ui.theme.ReportScreen
import com.loganes.finace.ui.theme.SettingsScreen
import com.loganes.finace.ui.theme.TrashbinScreen
import com.loganes.finace.viewmodel.TransactionViewModel
import com.loganes.finace.viewmodel.TransactionViewModelFactory
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Siapkan Database
        val database = AppDatabase.getDatabase(this)
        val dao = database.transactionDao()
        val employeeDao = database.employeeDao() // <-- Tambahkan ini
        val viewModelFactory = TransactionViewModelFactory(dao, employeeDao)

        scheduleDailyBackup()


        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    // 2. Siapkan Navigasi
                    val navController = rememberNavController()

                    // 3. Ambil ViewModel
                    val viewModel: TransactionViewModel = viewModel(factory = viewModelFactory)

                    NavHost(navController = navController, startDestination = "login") {

                        // 1. HALAMAN LOGIN
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    // Jika sukses, masuk dashboard dan hapus login dari history (agar tombol back tidak balik ke login)
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Halaman Dashboard
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel, // <--- TAMBAHKAN BARIS INI (Kirim ViewModel ke Dashboard)
                                onAddClick = { navController.navigate("add_transaction") },
                                onPayrollClick ={navController.navigate("employee_list")},
                                onReportClick={navController.navigate("report_screen")},
                                onTrashClick = {navController.navigate("trashbin")},
                                onSettingsClick = {navController.navigate("Settings")},
                                onEditTransaction = { navController.navigate("edit_transaction") }
                            )
                        }

                        // Halaman Input
                        composable("add_transaction") {
                            AddTransactionScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("employee_list") {
                            EmployeeListScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("report_screen") {
                            ReportScreen( // Pastikan nama file ini sesuai dengan yang Anda buat
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("trashbin"){
                            TrashbinScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
                        }
                        composable("edit_transaction") {
                            EditTransactionScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel, // <-- KIRIM VIEWMODEL
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scheduleDailyBackup() {

        val workManager = WorkManager.getInstance(applicationContext)

        // A. Hitung waktu menuju jam 12:00 malam (00:00)
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Set target ke jam 00:00:00
        dueDate.set(Calendar.HOUR_OF_DAY, 0)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        // Jika jam 00:00 sudah lewat hari ini, jadwalkan untuk besok
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        // B. Buat Permintaan Kerja Berkala (Setiap 24 Jam)
        val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.SECONDS) // Tunggu sampai jam 12 malam
            .addTag("daily_backup")
            .build()

        // C. Kirim ke Sistem (KEEP = Jangan diduplikasi kalau sudah ada)
        workManager.enqueueUniquePeriodicWork(
            "DailyBackupJob",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }
}