package com.loganes.finace.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Employee
import com.loganes.finace.model.Transaction
import com.loganes.finace.model.TransactionType
import com.loganes.finace.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

object DataSeeder {

    // Daftar Nama Pegawai Palsu
    private val employeeNames = listOf(
        "Ujang Sutisna", "Asep Sunandar", "Siti Aminah", "Budi Santoso",
        "Rina Kartika", "Doni Salmanan", "Eka Putri", "Fajar Sadboy",
        "Gita Gutawa", "Hendra Setiawan"
    )

    // Daftar Kategori Dummy
    private val incomeCategories = listOf("Penjualan", "Penjualan", "Penjualan", "Top Up Kas Kecil") // Diperbanyak "Penjualan" biar saldo positif
    private val expenseCategories = listOf("Kebutuhan Kas Kecil (Harian)", "Operasional (Pusat)", "Belanja Perusahaan", "Maintenance", "Lain-lain")

    // Fungsi Utama Generate Data
    @RequiresApi(Build.VERSION_CODES.O)
    fun seedAllData(viewModel: TransactionViewModel) {
        seedEmployees(viewModel)
        seedTransactions(viewModel)
    }

    private fun seedEmployees(viewModel: TransactionViewModel) {
        // Generate 10 Pegawai
        employeeNames.forEach { name ->
            val randomBranch = BranchType.values().random()
            val randomSalary = Random.nextLong(2_000_000, 5_000_000).toDouble()
            val randomDate = Random.nextInt(1, 28) // Tanggal gajian 1-28

            viewModel.addEmployee(
                name = name,
                branch = randomBranch,
                salary = randomSalary,
                payDate = randomDate,
                phone = "628${Random.nextLong(100000000, 999999999)}"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun seedTransactions(viewModel: TransactionViewModel) {
        val today = LocalDate.now()

        // Generate 50 Transaksi (Mundur ke belakang selama 30 hari)
        repeat(50) {
            val daysAgo = Random.nextLong(0, 30)
            val transactionDate = today.minusDays(daysAgo).toString()

            val randomBranch = BranchType.values().random()
            val isIncome = Random.nextBoolean() // True = Masuk, False = Keluar

            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

            // Pilih kategori
            val category = if (isIncome) incomeCategories.random() else expenseCategories.random()

            // Nominal Uang (50rb - 5jt)
            val amount = if (isIncome) {
                Random.nextLong(500_000, 5_000_000).toDouble() // Pemasukan biasanya besar
            } else {
                Random.nextLong(20_000, 1_000_000).toDouble()  // Pengeluaran bervariasi
            }

            // Simpan manual lewat DAO logic di ViewModel (Kita buat objek Transaction langsung)
            // Catatan: Kita panggil fungsi saveTransaction tapi kita hack sedikit agar tanggalnya bisa custom
            // Karena fungsi saveTransaction di ViewModel pakai LocalDate.now(), kita harus buat fungsi khusus di ViewModel atau insert manual.

            // SOLUSI: Kita pakai fungsi insertTransaction yang menerima objek Transaction utuh
            // (Pastikan di ViewModel ada akses ke insert atau kita panggil manual)

            // Agar mudah, kita asumsikan Anda menambahkan fungsi 'insertRawTransaction' di ViewModel
            // Atau kita panggil viewModel.saveTransaction tapi tanggalnya hari ini (kurang bagus buat grafik).

            // Opsi Terbaik: Kita kirim data ke ViewModel untuk diproses
            viewModel.seedRawTransaction(
                Transaction(
                    branch = randomBranch,
                    type = type,
                    category = category,
                    amount = amount,
                    description = "Data Dummy Auto-Generated",
                    date = transactionDate
                )
            )
        }
    }
}

private fun TransactionViewModel.seedRawTransaction(transaction: Transaction) {
    viewModelScope.launch {
        dao.insert(transaction)
    }
}
