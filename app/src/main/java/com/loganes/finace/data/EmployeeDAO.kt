package com.loganes.finace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.loganes.finace.model.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Insert
    suspend fun insertEmployee(employee: Employee)

    @Update
    suspend fun updateEmployee(employee: Employee)

    // Ambil semua pegawai untuk ditampilkan di List
    @Query("SELECT * FROM employees ORDER BY branch ASC, name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    // Update tanggal bayar terakhir (Saat tombol 'Bayar' ditekan)
    @Query("UPDATE employees SET lastPaidDate = :date WHERE id = :id")
    suspend fun updateLastPaidDate(id: Int, date: String)
    // Tambahkan fungsi delete
    @Delete
    suspend fun deleteEmployee(employee: Employee)
}