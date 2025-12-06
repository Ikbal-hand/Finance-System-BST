package com.loganes.finace.ui.theme

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loganes.finace.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val oldTransaction = viewModel.transactionToEdit

    if (oldTransaction == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    var category by remember { mutableStateOf(oldTransaction.category) }
    var amountText by remember { mutableStateOf(oldTransaction.amount.toInt().toString()) }
    var description by remember { mutableStateOf(oldTransaction.description) }

    // --- DEFINISI WARNA INPUT (SAMA SEPERTI DI ATAS) ---
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        cursorColor = BlueStart,
        focusedBorderColor = BlueStart,
        unfocusedBorderColor = Color.LightGray,
        focusedLabelColor = BlueStart,
        unfocusedLabelColor = TextLight
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaksi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueStart, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(GrayBackground)
                .padding(16.dp)
        ) {
            // Info Header
            Text(
                "Mengedit Transaksi: ${oldTransaction.branch.name}",
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Input Jumlah
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = customTextFieldColors // <--- TERAPKAN WARNA
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Kategori
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = customTextFieldColors // <--- TERAPKAN WARNA
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Deskripsi
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        colors = customTextFieldColors // <--- TERAPKAN WARNA
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val newAmount = amountText.toDoubleOrNull() ?: 0.0
                    if (newAmount > 0) {
                        val updatedTransaction = oldTransaction.copy(
                            category = category,
                            amount = newAmount,
                            description = description
                        )
                        viewModel.updateTransaction(updatedTransaction)
                        Toast.makeText(context, "Data Diperbarui!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueStart)
            ) {
                Text("SIMPAN PERUBAHAN", fontWeight = FontWeight.Bold)
            }
        }
    }
}