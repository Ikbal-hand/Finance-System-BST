package com.loganes.finace.data

import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Transaction
import com.loganes.finace.model.TransactionType

object FinanceCalculator {

    // String kunci untuk mendeteksi jenis transaksi
    private const val KEYWORD_TOPUP = "Top Up"
    private const val KEYWORD_KAS_KECIL = "Kas Kecil"

    /**
     * Menghitung TOTAL ASET PERUSAHAAN (Kekayaan Riil).
     * Rumus: Semua Pemasukan (Kecuali TopUp) - Semua Pengeluaran (Apapun jenisnya)
     * * Top Up diabaikan di sini karena secara global itu hanya pindah buku,
     * uangnya tidak bertambah/berkurang dari total perusahaan.
     */
    fun calculateTotalRealAssets(transactions: List<Transaction>): Double {
        val realIncome = transactions
            .filter {
                it.type == TransactionType.INCOME &&
                        !it.category.contains(KEYWORD_TOPUP, ignoreCase = true)
            }
            .sumOf { it.amount }

        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return realIncome - totalExpense
    }

    /**
     * Menghitung SALDO KAS KECIL per Cabang.
     * * Aturan:
     * 1. Bertambah saat menerima Top Up.
     * 2. Berkurang HANYA jika kategori pengeluaran mengandung kata "Kas Kecil".
     * (Contoh: "Uang Makan Kas Kecil", "Bensin Kas Kecil").
     * 3. Pengeluaran lain (Gaji, Sewa, dll) TIDAK mengurangi saldo ini.
     */
    fun calculatePettyCash(transactions: List<Transaction>, branch: BranchType): Double {
        // 1. Uang Masuk (Top Up)
        val topUpReceived = transactions.filter {
            it.type == TransactionType.INCOME &&
                    it.category.contains(KEYWORD_TOPUP, ignoreCase = true) &&
                    it.branch == branch
        }.sumOf { it.amount }

        // 2. Uang Keluar (Hanya yang kategori mengandung "Kas Kecil")
        val pettyExpense = transactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.branch == branch &&
                    it.category.contains(KEYWORD_KAS_KECIL, ignoreCase = true)
        }.sumOf { it.amount }

        return topUpReceived - pettyExpense
    }

    /**
     * Menghitung SALDO KAS UTAMA (Pusat).
     * * Aturan:
     * 1. Sumber Dana: Semua Penjualan (Revenue).
     * 2. Pengeluaran:
     * - Semua Pengeluaran Umum (Gaji, Listrik, Sewa) -> Kategori yg TIDAK ada kata "Kas Kecil".
     * - Uang yang ditransfer ke Cabang (Top Up).
     */
    fun calculateMainCash(transactions: List<Transaction>): Double {
        // 1. Pemasukan Murni (Omzet)
        val revenue = transactions
            .filter {
                it.type == TransactionType.INCOME &&
                        !it.category.contains(KEYWORD_TOPUP, ignoreCase = true)
            }
            .sumOf { it.amount }

        // 2. Pengeluaran Umum (Yang ditanggung Pusat)
        //    Yaitu semua expense yang BUKAN pemakaian kas kecil.
        val generalExpenses = transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                        !it.category.contains(KEYWORD_KAS_KECIL, ignoreCase = true)
            }
            .sumOf { it.amount }

        // 3. Uang Keluar untuk Top Up Cabang
        val transfersToBranches = transactions
            .filter {
                it.type == TransactionType.INCOME &&
                        it.category.contains(KEYWORD_TOPUP, ignoreCase = true)
            }
            .sumOf { it.amount }

        return revenue - generalExpenses - transfersToBranches
    }
}