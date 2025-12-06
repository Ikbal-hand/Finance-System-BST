package com.loganes.finace

import com.loganes.finace.data.FinanceCalculator
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Transaction
import com.loganes.finace.model.TransactionType
import junit.framework.TestCase.assertEquals
import org.junit.Test

class FinanceCalculatorTest {

    // Helper untuk membuat transaksi dummy biar cepat
    private fun createTrx(amount: Double, type: TransactionType, category: String, branch: BranchType): Transaction {
        return Transaction(
            id = 0, amount = amount, type = type, category = category, branch = branch,
            date = "2025-01-01", description = "Test"
        )
    }

    @Test
    fun `test 1 - Pemasukan Cabang Masuk ke Kas Utama`() {
        // Skenario: Cabang Box Factory dapat omzet 10 Juta.
        // Harapan: Kas Utama = 10jt, Kas Kecil Box Factory = 0 (Karena omzet setor ke pusat).
        val transactions = listOf(
            createTrx(10_000_000.0, TransactionType.INCOME, "Penjualan", BranchType.BOX_FACTORY)
        )

        val kasUtama = FinanceCalculator.calculateMainCash(transactions)
        val kasKecilBox = FinanceCalculator.calculatePettyCash(transactions, BranchType.BOX_FACTORY)

        assertEquals(10_000_000.0, kasUtama, 0.0)
        assertEquals(0.0, kasKecilBox, 0.0)
    }

    @Test
    fun `test 2 - Top Up Mengurangi Kas Utama Menambah Kas Kecil`() {
        // Skenario: Ada Saldo 10jt. Lalu Admin Top Up 1jt ke Box Factory.
        // Harapan: Kas Utama sisa 9jt. Kas Kecil Box Factory jadi 1jt. Total Aset tetap 10jt.
        val transactions = listOf(
            createTrx(10_000_000.0, TransactionType.INCOME, "Modal", BranchType.PUSAT), // Saldo Awal
            createTrx(1_000_000.0, TransactionType.EXPENSE, "Top Up Kas Kecil", BranchType.BOX_FACTORY)
        )

        val kasUtama = FinanceCalculator.calculateMainCash(transactions)
        val kasKecilBox = FinanceCalculator.calculatePettyCash(transactions, BranchType.BOX_FACTORY)
        val totalAset = FinanceCalculator.calculateTotalRealAssets(transactions)

        assertEquals("Kas Utama harus berkurang", 9_000_000.0, kasUtama, 0.0)
        assertEquals("Kas Kecil harus bertambah", 1_000_000.0, kasKecilBox, 0.0)
        assertEquals("Total uang fisik tidak boleh hilang", 10_000_000.0, totalAset, 0.0)
    }

    @Test
    fun `test 3 - Pengeluaran Cabang Mengurangi Kas Kecil SAJA`() {
        // Skenario:
        // 1. Pemasukan 10jt.
        // 2. Top Up 1jt ke Box Factory. (Main: 9, Box: 1)
        // 3. Box Factory beli Bensin 100rb.
        // Harapan: Kas Utama TETAP 9jt. Kas Kecil Box sisa 900rb.

        val transactions = listOf(
            createTrx(10_000_000.0, TransactionType.INCOME, "Modal", BranchType.PUSAT),
            createTrx(1_000_000.0, TransactionType.EXPENSE, "Top Up Kas Kecil", BranchType.BOX_FACTORY),
            createTrx(100_000.0, TransactionType.EXPENSE, "Bensin", BranchType.BOX_FACTORY)
        )

        val kasUtama = FinanceCalculator.calculateMainCash(transactions)
        val kasKecilBox = FinanceCalculator.calculatePettyCash(transactions, BranchType.BOX_FACTORY)
        val totalAset = FinanceCalculator.calculateTotalRealAssets(transactions)

        assertEquals("Kas Utama tidak boleh terpengaruh belanja cabang", 9_000_000.0, kasUtama, 0.0)
        assertEquals("Kas Kecil harus berkurang", 900_000.0, kasKecilBox, 0.0)
        assertEquals("Total aset real berkurang 100rb", 9_900_000.0, totalAset, 0.0)
    }

    @Test
    fun `test 4 - Pengeluaran Pusat Mengurangi Kas Utama`() {
        // Skenario: Beli server pakai uang pusat.
        val transactions = listOf(
            createTrx(10_000_000.0, TransactionType.INCOME, "Modal", BranchType.PUSAT),
            createTrx(500_000.0, TransactionType.EXPENSE, "Server", BranchType.PUSAT)
        )

        val kasUtama = FinanceCalculator.calculateMainCash(transactions)
        assertEquals(9_500_000.0, kasUtama, 0.0)
    }
}