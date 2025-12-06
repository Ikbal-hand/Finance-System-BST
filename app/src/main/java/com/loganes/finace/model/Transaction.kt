package com.loganes.finace.model // Sesuaikan package Anda

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enum tetap sama
enum class BranchType { BOX_FACTORY, MAINTENANCE_ALFA, SAUFA_OLSHOP, PUSAT }
enum class TransactionType { INCOME, EXPENSE }

@Entity(tableName = "transactions") // Nama tabel di SQLite
data class Transaction(
    @PrimaryKey(autoGenerate = true) // ID otomatis (1, 2, 3...)
    val id: Int = 0,

    val branch: BranchType,
    val type: TransactionType,
    val category: String, // "Gaji", "Operasional", dll
    val amount: Double,
    val description: String = "",
    val date: String, // Format YYYY-MM-DD

    // Fitur Trashbin: Jika dihapus, nilai ini jadi 'true', tapi data tidak hilang
    val isDeleted: Boolean = false
)