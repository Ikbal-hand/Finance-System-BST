package com.loganes.finace.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.Employee
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Cache sederhana untuk cabang user agar tidak fetch berulang kali (Hemat Read)
    private var cachedUserBranch: String? = null

    // --- USER & CABANG ---
    suspend fun getUserBranch(): String {
        // Cek cache dulu
        if (cachedUserBranch != null) return cachedUserBranch!!

        val userId = auth.currentUser?.uid ?: return "PUSAT"
        val snapshot = db.collection("users").document(userId).get().await()

        cachedUserBranch = snapshot.getString("branch") ?: "PUSAT"
        return cachedUserBranch!!
    }

    // --- TRANSAKSI ---
    suspend fun getTransactions(): List<Transaction> {
        val myBranch = getUserBranch()

        // PERBAIKAN UTAMA:
        // 1. Gunakan "deleted" (bukan isDeleted)
        // 2. Limit 100 data saja (Hemat Kuota)
        var query: Query = db.collection("transactions")
            .whereEqualTo("deleted", false)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(100)

        if (myBranch != "PUSAT") {
            query = query.whereEqualTo("branch", myBranch)
        }

        return try {
            query.get().await().toObjects(Transaction::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Fungsi Tambah Data (Return ID Dokumen untuk Update Lokal UI)
    suspend fun addTransactionWithReturn(transaction: Transaction): DocumentReference {
        val myBranch = getUserBranch()
        // Pastikan cabang terisi
        val finalBranch = if (transaction.branch.isNotEmpty()) transaction.branch else myBranch
        val data = transaction.copy(branch = finalBranch)

        // Return DocumentReference (berisi ID baru)
        return db.collection("transactions").add(data).await()
    }

    // Fungsi add lama (untuk kompatibilitas, panggil fungsi baru)
    suspend fun addTransaction(transaction: Transaction) {
        addTransactionWithReturn(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        if (transaction.id.isNotEmpty()) {
            db.collection("transactions").document(transaction.id).set(transaction).await()
        }
    }

    // --- SAMPAH (TRASHBIN) ---
    suspend fun getTrashbinItems(): List<Transaction> {
        val myBranch = getUserBranch()
        var query: Query = db.collection("transactions")
            .whereEqualTo("deleted", true) // Gunakan "deleted"
            .limit(50) // Limit sampah juga biar hemat

        if (myBranch != "PUSAT") {
            query = query.whereEqualTo("branch", myBranch)
        }
        return query.get().await().toObjects(Transaction::class.java)
    }

    suspend fun softDeleteTransaction(id: String) {
        db.collection("transactions").document(id).update("deleted", true).await()
    }

    suspend fun restoreTransaction(id: String) {
        db.collection("transactions").document(id).update("deleted", false).await()
    }

    suspend fun permanentDeleteTransaction(id: String) {
        db.collection("transactions").document(id).delete().await()
    }

    // --- PEGAWAI (EMPLOYEE) ---
    suspend fun getEmployees(): List<Employee> {
        val myBranch = getUserBranch()
        var query: Query = db.collection("employees")

        if (myBranch != "PUSAT") {
            query = query.whereEqualTo("branch", myBranch)
        }
        return query.get().await().toObjects(Employee::class.java)
    }

    suspend fun addEmployee(employee: Employee) {
        val myBranch = getUserBranch()
        val data = employee.copy(branch = if (myBranch == "PUSAT") employee.branch else myBranch)
        db.collection("employees").add(data).await()
    }

    suspend fun updateEmployee(employee: Employee) {
        if (employee.id.isNotEmpty()) {
            db.collection("employees").document(employee.id).set(employee).await()
        }
    }

    suspend fun deleteEmployee(id: String) {
        db.collection("employees").document(id).delete().await()
    }
}