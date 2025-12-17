package com.loganes.finace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loganes.finace.data.model.Employee
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.BranchType
import com.loganes.finace.data.model.TransactionType
import com.loganes.finace.data.repository.FirestoreRepository
import com.loganes.finace.utils.FinanceCalculator // Pastikan import ini ada
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TransactionViewModel(private val repository: FirestoreRepository) : ViewModel() {

    // --- STATE FLOW ---
    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions.asStateFlow()

    // State untuk Sampah (Trashbin)
    private val _trashTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val trashTransactions: StateFlow<List<Transaction>> = _trashTransactions.asStateFlow()

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _userBranch = MutableStateFlow("PUSAT")
    val userBranch: StateFlow<String> = _userBranch.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Variabel pembantu navigasi
    var transactionToEdit: Transaction? = null
    var selectedDashboardBranch: BranchType? = null

    init {
        fetchUserBranch()
        fetchTransactions()
        fetchTrashbin() // Fetch sampah saat inisialisasi
        fetchEmployees()
    }

    fun fetchUserBranch() {
        viewModelScope.launch { _userBranch.value = repository.getUserBranch() }
    }

    // --- FETCH DATA ---
    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _allTransactions.value = repository.getTransactions()
            _isLoading.value = false
        }
    }

    fun fetchTrashbin() {
        viewModelScope.launch {
            _trashTransactions.value = repository.getTrashbinItems()
        }
    }
    fun updateEmail(newEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        user?.updateEmail(newEmail)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Jangan lupa update di Firestore 'users' juga agar sinkron
                viewModelScope.launch {
                    try {
                        // Cari doc user berdasarkan UID dan update field email
                        val uid = user.uid
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .update("email", newEmail)
                        onSuccess()
                    } catch (e: Exception) {
                        onError("Email login berubah, tapi gagal update database: ${e.message}")
                    }
                }
            } else {
                onError(task.exception?.message ?: "Gagal ganti email")
            }
        }
    }

    fun updatePassword(newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        user?.updatePassword(newPass)?.addOnCompleteListener { task ->
            if (task.isSuccessful) onSuccess() else onError(task.exception?.message ?: "Gagal ganti password")
        }
    }

    fun fetchEmployees() {
        viewModelScope.launch { _employees.value = repository.getEmployees() }
    }

    // --- INSERT (OPTIMISTIC) ---
    private fun insertTransactionOptimistic(transaction: Transaction) {
        val currentList = _allTransactions.value.toMutableList()
        currentList.add(0, transaction)
        _allTransactions.value = currentList

        viewModelScope.launch {
            try {
                val docRef = repository.addTransactionWithReturn(transaction)
                val index = currentList.indexOf(transaction)
                if (index != -1) {
                    currentList[index] = transaction.copy(id = docRef.id)
                    _allTransactions.value = currentList
                }
            } catch (e: Exception) {
                val revertedList = _allTransactions.value.toMutableList()
                revertedList.remove(transaction)
                _allTransactions.value = revertedList
            }
        }
    }

    // --- SAVE TRANSACTION (VALIDASI KAS KECIL) ---
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveTransaction(
        branch: BranchType,
        type: TransactionType,
        category: String,
        amount: Double,
        description: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val isExpensePettyCash = (type == TransactionType.EXPENSE) &&
                (category.equals("Harian", ignoreCase = true))

        // Validasi Saldo Kas Kecil
        if (isExpensePettyCash) {
            val currentBalance = FinanceCalculator.calculateBranchPettyCash(_allTransactions.value, branch)
            if (currentBalance < amount) {
                val formatRp = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(currentBalance)
                onError("Saldo Kas Kecil Tidak Cukup! Sisa: Rp $formatRp")
                return
            }
        }

        val newTransaction = Transaction(
            branch = branch.name,
            type = type.name,
            category = category,
            amount = amount,
            description = description,
            date = LocalDate.now().toString(),
            isPettyCash = isExpensePettyCash,
            isDeleted = false
        )

        insertTransactionOptimistic(newTransaction)
        onSuccess()
    }

    // --- TOP UP (VALIDASI KAS PUSAT) ---
    @RequiresApi(Build.VERSION_CODES.O)
    fun performTopUp(
        targetBranch: BranchType,
        amount: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 1. Validasi: Cek Saldo Pusat Cukup Gak?
        val currentMainCash = FinanceCalculator.calculateMainCash(_allTransactions.value)
        if (currentMainCash < amount) {
            val formatRp = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(currentMainCash)
            onError("Gagal! Saldo Kas Pusat tidak cukup. Sisa: Rp $formatRp")
            return
        }

        // 2. Eksekusi Top Up Double Entry
        val today = LocalDate.now().toString()
        val trfOut = Transaction(
            branch = "PUSAT",
            type = "EXPENSE",
            category = "Top Up Cabang",
            amount = amount,
            description = "Top Up ke ${targetBranch.name}",
            date = today,
            isPettyCash = false // Mengurangi Rekening Pusat
        )
        val trfIn = Transaction(
            branch = targetBranch.name,
            type = "INCOME",
            category = "Top Up Masuk",
            amount = amount,
            description = "Terima dari Pusat",
            date = today,
            isPettyCash = true // Menambah Kas Kecil Cabang
        )

        // Update Lokal
        val currentList = _allTransactions.value.toMutableList()
        currentList.add(0, trfOut)
        currentList.add(0, trfIn)
        _allTransactions.value = currentList

        // Simpan Server
        viewModelScope.launch {
            repository.addTransaction(trfOut)
            repository.addTransaction(trfIn)
        }
        onSuccess()
    }

    // --- ADD CAPITAL (MODAL AWAL) ---
    @RequiresApi(Build.VERSION_CODES.O)
    fun addCapital(amount: Double) {
        val today = LocalDate.now().toString()
        val capitalTransaction = Transaction(
            branch = "PUSAT",
            type = "INCOME",
            category = "Modal Awal",
            amount = amount,
            description = "Saldo Awal / Suntikan Modal",
            date = today,
            isPettyCash = false, // Masuk ke Kas Pusat
            isDeleted = false
        )
        insertTransactionOptimistic(capitalTransaction)
    }

    // --- UPDATE (EDIT) ---
    fun updateTransaction(transaction: Transaction) {
        val currentList = _allTransactions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            currentList[index] = transaction
            _allTransactions.value = currentList
        }
        viewModelScope.launch { repository.updateTransaction(transaction) }
    }

    // --- DELETE / RESTORE (TRASHBIN) ---
    fun softDeleteTransaction(transaction: Transaction) {
        // Hapus dari Dashboard
        val dashboardList = _allTransactions.value.toMutableList()
        dashboardList.remove(transaction)
        _allTransactions.value = dashboardList

        // Tambah ke Sampah (Lokal)
        val trashList = _trashTransactions.value.toMutableList()
        trashList.add(0, transaction.copy(isDeleted = true))
        _trashTransactions.value = trashList

        viewModelScope.launch {
            repository.softDeleteTransaction(transaction.id)
            fetchTrashbin()
        }
    }

    fun restoreTransaction(transaction: Transaction) {
        // Hapus dari Sampah
        val trashList = _trashTransactions.value.toMutableList()
        trashList.remove(transaction)
        _trashTransactions.value = trashList

        // Kembalikan ke Dashboard (Lokal)
        val dashboardList = _allTransactions.value.toMutableList()
        dashboardList.add(0, transaction.copy(isDeleted = false))
        dashboardList.sortByDescending { it.date }
        _allTransactions.value = dashboardList

        viewModelScope.launch { repository.restoreTransaction(transaction.id) }
    }

    fun permanentDelete(transaction: Transaction) {
        val trashList = _trashTransactions.value.toMutableList()
        trashList.remove(transaction)
        _trashTransactions.value = trashList
        viewModelScope.launch { repository.permanentDeleteTransaction(transaction.id) }
    }

    // --- EMPLOYEE LOGIC ---
    fun addEmployee(name: String, branch: BranchType, salary: Double, payDate: Int, phone: String) {
        val newEmployee = Employee(name = name, branch = branch.name, salaryAmount = salary, payDate = payDate, phoneNumber = phone)
        viewModelScope.launch {
            repository.addEmployee(newEmployee)
            fetchEmployees()
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.updateEmployee(employee)
            fetchEmployees()
        }
    }

    fun deleteEmployee(employeeId: String) {
        viewModelScope.launch {
            repository.deleteEmployee(employeeId)
            fetchEmployees()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun payEmployee(employee: Employee) {
        val today = LocalDate.now().toString()
        val salaryTransaction = Transaction(
            branch = "PUSAT",
            type = "EXPENSE",
            category = "Gaji Pegawai",
            amount = employee.salaryAmount,
            description = "Gaji ${employee.name} (${employee.branch})",
            date = today,
            isPettyCash = false,
            isDeleted = false
        )
        insertTransactionOptimistic(salaryTransaction)

        val updatedEmployee = employee.copy(lastPaidDate = today)
        updateEmployee(updatedEmployee)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEmployeeStatusColor(employee: Employee): Int {
        val today = LocalDate.now()
        val safePayDate = employee.payDate.coerceIn(1, 31)
        val payDay = today.withDayOfMonth(safePayDate.coerceAtMost(today.lengthOfMonth()))

        if (employee.lastPaidDate != null) {
            try {
                val lastPaid = LocalDate.parse(employee.lastPaidDate)
                if (lastPaid.month == today.month && lastPaid.year == today.year) return 1
            } catch (e: Exception) { }
        }

        val daysUntilPayday = java.time.temporal.ChronoUnit.DAYS.between(today, payDay)
        return when {
            daysUntilPayday in 0..3 -> 2
            daysUntilPayday < 0 -> 3
            else -> 0
        }
    }
}