package com.loganes.finace.data // Simpan di folder data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.loganes.finace.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * From transactions ORDER BY date DESC") // Sesuaikan nama tabel
    fun getAllTransactions(): Flow<List<Transaction>>

    // 1. Simpan Transaksi Baru
    @Insert
    suspend fun insert(transaction: Transaction)

    // 2. Ambil SEMUA data aktif (yang tidak dihapus) untuk Laporan & Grafik
    // Menggunakan Flow agar data di layar update otomatis (Real-time)
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllActiveTransactions(): Flow<List<Transaction>>

    // 3. Ambil data khusus per Cabang (Untuk Tab Dashboard)
    @Query("SELECT * FROM transactions WHERE branch = :branchName AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsByBranch(branchName: String): Flow<List<Transaction>>

    // 4. Hitung Saldo Total (Income - Expense)
    // Ini logic SQL agar aplikasi tidak berat menghitung manual
    @Query("SELECT (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND isDeleted = 0) - (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND isDeleted = 0)")
    fun getTotalBalance(): Flow<Double>

    // 5. Fitur Trashbin: Soft Delete (Hanya tandai sebagai terhapus)
    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun moveToTrash(id: Int)

    // 6. Lihat isi Trashbin (Data yang terhapus)
    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY date DESC")
    fun getTrashbinItems(): Flow<List<Transaction>>

    // 7. Restore (Kembalikan data dari sampah)
    @Query("UPDATE transactions SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Int)

    @Update
    suspend fun update(transaction: Transaction)
    // Di dalam TransactionDao.kt
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    @Delete
    suspend fun delete(transaction: Transaction)
}