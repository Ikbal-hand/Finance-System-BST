package com.loganes.finace.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

enum class BranchType { BOX_FACTORY, MAINTENANCE_ALFA, SAUFA_OLSHOP, PUSAT }
enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    @DocumentId
    var id: String = "",

    val branch: String = "",
    val type: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = "",

    // --- PERBAIKAN FATAL DI SINI (MAPPING NAMA FIELD) ---
    // Di Firestore namanya "deleted", di Kotlin namanya "isDeleted"
    @get:PropertyName("deleted")
    @set:PropertyName("deleted")
    var isDeleted: Boolean = false,

    // Di Firestore namanya "pettyCash", di Kotlin namanya "isPettyCash"
    @get:PropertyName("pettyCash")
    @set:PropertyName("pettyCash")
    var isPettyCash: Boolean = false
) {
    // Constructor kosong wajib untuk deserialisasi Firestore
    constructor() : this("", "", "", "", 0.0, "", "", false, false)

    @get:Exclude
    val branchEnum: BranchType
        get() = try { BranchType.valueOf(branch) } catch (e: Exception) { BranchType.PUSAT }

    @get:Exclude
    val typeEnum: TransactionType
        get() = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE }
}