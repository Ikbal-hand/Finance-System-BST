package com.loganes.finace.data.model

import com.google.firebase.firestore.DocumentId

data class Employee(
    @DocumentId
    var id: String = "",

    val name: String = "",
    val branch: String = "", // String dari Enum BranchType
    val salaryAmount: Double = 0.0,
    val payDate: Int = 1, // Tanggal gajian (1-31)
    val lastPaidDate: String? = null, // Format: YYYY-MM-DD
    val phoneNumber: String = ""
) {
    // Constructor kosong wajib untuk Firestore
    constructor() : this("", "", "", 0.0, 1, null, "")
}