package com.loganes.finace.data

import androidx.room.TypeConverter
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.TransactionType

class Converters {
    // Mengubah BranchType ke Teks (String) agar bisa disimpan di DB
    @TypeConverter
    fun fromBranch(branch: BranchType): String {
        return branch.name
    }

    // Mengubah Teks dari DB kembali ke BranchType
    @TypeConverter
    fun toBranch(value: String): BranchType {
        return try {
            BranchType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            BranchType.BOX_FACTORY // Default jika terjadi error data
        }
    }

    // Sama juga untuk TransactionType (Pemasukan/Pengeluaran)
    @TypeConverter
    fun fromType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TransactionType.INCOME
        }
    }
}