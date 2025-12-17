package com.loganes.finace.utils

import com.loganes.finace.data.model.BranchType
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.TransactionType

object FinanceCalculator {

    /**
     * Menghitung Saldo Rekening Pusat (Kas Utama).
     * Rumus: (Semua Pemasukan Non-Fisik) - (Semua Pengeluaran Non-Fisik).
     * Catatan: Top Up ke cabang dianggap Pengeluaran Non-Fisik di sini.
     */
    fun calculateMainCash(transactions: List<Transaction>): Double {
        // Filter: Bukan Petty Cash
        val relevantTrans = transactions.filter { !it.isPettyCash }

        val income = relevantTrans
            .filter { it.typeEnum == TransactionType.INCOME }
            .sumOf { it.amount }

        val expense = relevantTrans
            .filter { it.typeEnum == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return income - expense
    }

    /**
     * Menghitung Sisa Kas Kecil untuk Cabang Tertentu.
     * Rumus: (Pemasukan Fisik + Top Up Masuk) - (Pengeluaran Fisik Harian).
     */
    fun calculateBranchPettyCash(transactions: List<Transaction>, branch: BranchType): Double {
        // Filter: Milik Cabang Ini DAN Merupakan Petty Cash
        val branchTrans = transactions.filter {
            it.branchEnum == branch && it.isPettyCash
        }

        val income = branchTrans
            .filter { it.typeEnum == TransactionType.INCOME }
            .sumOf { it.amount }

        val expense = branchTrans
            .filter { it.typeEnum == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return income - expense
    }

    /**
     * Menghitung Total Aset Perusahaan.
     * Rumus: Uang di Bank (Pusat) + Uang Fisik (Kas Kecil) di SEMUA cabang.
     */
    fun calculateTotalAssets(transactions: List<Transaction>): Double {
        val mainCash = calculateMainCash(transactions)

        val pettyBox = calculateBranchPettyCash(transactions, BranchType.BOX_FACTORY)
        val pettyAlfa = calculateBranchPettyCash(transactions, BranchType.MAINTENANCE_ALFA)
        val pettySaufa = calculateBranchPettyCash(transactions, BranchType.SAUFA_OLSHOP)

        return mainCash + pettyBox + pettyAlfa + pettySaufa
    }

    /**
     * Filter Transaksi untuk Grafik.
     * Logika: Kita sembunyikan transaksi "Top Up" agar grafik tidak melonjak aneh
     * hanya karena pemindahan uang antar kantong sendiri.
     */
    fun getChartData(transactions: List<Transaction>, branch: BranchType, isPusatUser: Boolean): List<Transaction> {
        val rawData = if (isPusatUser) {
            // Jika pusat, tampilkan data global yang BUKAN Petty Cash (Arus Rekening)
            transactions.filter { !it.isPettyCash }
        } else {
            // Jika cabang, tampilkan semua data cabang tersebut
            transactions.filter { it.branchEnum == branch }
        }

        // Hapus transaksi Top Up dari visualisasi grafik agar akurat secara Omzet vs Biaya Riil
        return rawData.filter {
            !it.category.contains("Top Up", ignoreCase = true)
        }
    }
}