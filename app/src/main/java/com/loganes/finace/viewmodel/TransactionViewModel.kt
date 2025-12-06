package com.loganes.finace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganes.finace.data.EmployeeDao
import com.loganes.finace.data.TransactionDao
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Employee
import com.loganes.finace.model.Transaction
import com.loganes.finace.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TransactionViewModel(
    val dao: TransactionDao,
    private val employeeDao: EmployeeDao
) : ViewModel() {

    // --- DATA STREAMS (LIVE DATA) ---
    // Total Saldo (Opsional, jika masih dipakai di UI lain)
    val totalBalance: Flow<Double> = dao.getTotalBalance()

    // List Pegawai
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()

    // Semua Transaksi AKTIF (Tidak termasuk sampah) -> Dipakai Dashboard
    val allTransactions: Flow<List<Transaction>> = dao.getAllActiveTransactions()

    // Data Sampah (Trashbin)
    val trashTransactions: Flow<List<Transaction>> = dao.getTrashbinItems()

    // State untuk Edit Transaksi
    var transactionToEdit by mutableStateOf<Transaction?>(null)

    // --- FUNGSI CRUD TRANSAKSI ---

    // 1. Update (Edit)
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.update(transaction)
        }
    }

    // 2. Simpan Transaksi Baru
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveTransaction(
        branch: BranchType,
        type: TransactionType,
        category: String,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            val newTransaction = Transaction(
                branch = branch,
                type = type,
                category = category,
                amount = amount,
                description = description,
                date = LocalDate.now().toString()
            )
            dao.insert(newTransaction)
        }
    }

    // 3. Soft Delete (Pindah ke Sampah)
    fun moveToTrash(transaction: Transaction) {
        viewModelScope.launch {
            dao.moveToTrash(transaction.id)
        }
    }

    // 4. Restore (Kembalikan dari Sampah)
    fun restoreTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.restoreFromTrash(transaction.id)
        }
    }

    // 5. Hard Delete (Hapus Permanen) - Untuk aksi di dalam Trashbin
    fun deletePermanently(transaction: Transaction) {
        viewModelScope.launch {
            dao.delete(transaction)
        }
    }

    // 6. KOSONGKAN DATA (FACTORY RESET) - Untuk Menu Settings
    fun clearAllData() {
        viewModelScope.launch {
            // Hapus semua transaksi
            dao.deleteAllTransactions()
            // Opsional: Hapus pegawai juga jika diinginkan
            // employeeDao.deleteAllEmployees()
        }
    }


    // --- LOGIC PEGAWAI ---

    // Tambah Pegawai
    fun addEmployee(name: String, branch: BranchType, salary: Double, payDate: Int, phone: String) {
        viewModelScope.launch {
            employeeDao.insertEmployee(
                Employee(name = name, branch = branch, salaryAmount = salary, payDate = payDate, phoneNumber = phone) // Pastikan Model Employee punya field phoneNumber
            )
        }
    }

    // Update Pegawai
    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            employeeDao.updateEmployee(employee)
        }
    }

    // Bayar Gaji
    @RequiresApi(Build.VERSION_CODES.O)
    fun payEmployee(employee: Employee) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()

            // A. Update data pegawai (sudah digaji)
            employeeDao.updateLastPaidDate(employee.id, today)

            // B. Catat sebagai Pengeluaran otomatis
            val transaction = Transaction(
                branch = employee.branch,
                type = TransactionType.EXPENSE,
                category = "Gaji Pegawai",
                description = "Gaji bulan ini: ${employee.name}",
                amount = employee.salaryAmount, // Pastikan ini gaji total
                date = today
            )
            dao.insert(transaction)
        }
    }
    // Di TransactionViewModel.kt
    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            employeeDao.deleteEmployee(employee) // Pastikan DAO punya fungsi @Delete
        }
    }

    // Logic Warna Status Gaji (Hijau/Kuning/Merah)
    @RequiresApi(Build.VERSION_CODES.O)
    fun getEmployeeStatusColor(employee: Employee): Int {
        val today = LocalDate.now()
        val payDay = today.withDayOfMonth(employee.payDate.coerceAtMost(today.lengthOfMonth()))

        // Cek apakah sudah dibayar bulan ini?
        if (employee.lastPaidDate != null) {
            val lastPaid = LocalDate.parse(employee.lastPaidDate)
            if (lastPaid.month == today.month && lastPaid.year == today.year) {
                return 1 // Hijau (Sudah Bayar)
            }
        }

        // Cek H-3
        val daysUntilPayday = ChronoUnit.DAYS.between(today, payDay)
        return when {
            daysUntilPayday in 0..3 -> 2 // Kuning (H-3)
            daysUntilPayday < 0 -> 3     // Merah (Telat)
            else -> 0                    // Netral
        }
    }
}

// Factory: Resep untuk membuat TransactionViewModel
class TransactionViewModelFactory(
    private val dao: TransactionDao,
    private val employeeDao: EmployeeDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(dao, employeeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}