package com.loganes.finace.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val branch: BranchType, // Box Factory, Alfa, atau Saufa
    val salaryAmount: Double,
    val payDate: Int, // Tanggal gajian (misal: tgl 25 setiap bulan)
    val lastPaidDate: String? = null, // Kapan terakhir dibayar (Format: YYYY-MM-DD)
    val phoneNumber: String = ""
)