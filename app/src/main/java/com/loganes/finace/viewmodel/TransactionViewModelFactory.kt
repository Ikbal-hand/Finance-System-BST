package com.loganes.finace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.loganes.finace.data.repository.FirestoreRepository

class TransactionViewModelFactory(repository1: FirestoreRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Cek apakah ViewModel yang diminta adalah TransactionViewModel
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {

            // 1. Buat instance Repository (Penghubung Database)
            val repository = FirestoreRepository()

            // 2. Masukkan Repository ke dalam ViewModel dan kembalikan
            return TransactionViewModel(repository) as T
        }

        // Jika bukan TransactionViewModel, lempar error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}