package com.loganes.finace

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.loganes.finace.data.repository.FirestoreRepository
import com.loganes.finace.ui.screen.auth.LoginScreen
import com.loganes.finace.ui.screen.dashboard.DashboardScreen
import com.loganes.finace.ui.screen.employee.EmployeeListScreen
import com.loganes.finace.ui.screen.settings.ReportScreen
import com.loganes.finace.ui.screen.settings.SettingsScreen
import com.loganes.finace.ui.screen.settings.TrashbinScreen
import com.loganes.finace.ui.screen.transaction.AddTransactionScreen
import com.loganes.finace.ui.screen.transaction.EditTransactionScreen
import com.loganes.finace.ui.theme.MyApplicationTheme
import com.loganes.finace.viewmodel.TransactionViewModel
import com.loganes.finace.viewmodel.TransactionViewModelFactory

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inisialisasi Firebase (PENTING)
        // Pastikan google-services.json sudah ada di folder app
        FirebaseApp.initializeApp(this)

        // 2. Siapkan Repository (Cloud Firestore)
        val repository = FirestoreRepository()

        // 3. Siapkan ViewModel Factory
        // Factory ini akan membuat TransactionViewModel dengan menyuntikkan repository
        val viewModelFactory = TransactionViewModelFactory(repository)

        setContent {
            // Gunakan Tema Aplikasi
            MyApplicationTheme() {
                Surface(color = MaterialTheme.colorScheme.background) {

                    val navController = rememberNavController()

                    // Inisialisasi ViewModel (Single Instance untuk seluruh aplikasi)
                    val viewModel: TransactionViewModel = viewModel(factory = viewModelFactory)

                    // State Login Sederhana
                    // (Idealnya menggunakan FirebaseAuth listener, tapi ini cukup untuk MVP)
                    var isLoggedIn by remember { mutableStateOf(false) }

                    // --- NAVIGASI UTAMA ---
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "dashboard" else "login"
                    ) {

                        // 1. HALAMAN LOGIN
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    // Login Berhasil
                                    isLoggedIn = true

                                    // PENTING: Ambil data cabang user agar Dashboard menyesuaikan tampilan
                                    viewModel.fetchUserBranch()

                                    // Pindah ke Dashboard & Hapus Login dari Backstack
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. DASHBOARD
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onAddClick = { navController.navigate("add_transaction") },
                                onPayrollClick = { navController.navigate("employee_list") },
                                onReportClick = { navController.navigate("report_screen") },
                                onTrashClick = { navController.navigate("trashbin") },
                                onSettingsClick = { navController.navigate("settings") },
                                onEditTransaction = { navController.navigate("edit_transaction") }
                            )
                        }

                        // 3. TAMBAH TRANSAKSI
                        composable("add_transaction") {
                            AddTransactionScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 4. EDIT TRANSAKSI
                        composable("edit_transaction") {
                            EditTransactionScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 5. MANAJEMEN PEGAWAI
                        composable("employee_list") {
                            EmployeeListScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 6. LAPORAN PDF
                        composable("report_screen") {
                            ReportScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 7. TEMPAT SAMPAH (TRASHBIN)
                        composable("trashbin") {
                            TrashbinScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 8. PENGATURAN
                        // 8. PENGATURAN
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onLogout = {
                                    // Logika Logout: Pindah ke Login & Hapus semua riwayat layar sebelumnya
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToTrash = { navController.navigate("trashbin") },
                                onNavigateToReport = { navController.navigate("report") }
                            )
                        }
                        composable("trashbin") {
                            TrashbinScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("report") {
                            ReportScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "employee_list?targetId={targetId}",
                            arguments = listOf(navArgument("targetId") { nullable = true; type = NavType.StringType })
                        ) { backStackEntry ->
                            val targetId = backStackEntry.arguments?.getString("targetId")
                            EmployeeListScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                targetEmployeeId = targetId // Pasang di sini
                            )
                        }
                    }
                }
            }
        }
    }
}