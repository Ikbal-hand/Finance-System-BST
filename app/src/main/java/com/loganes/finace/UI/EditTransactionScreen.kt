package com.loganes.finace.ui.theme

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loganes.finace.model.TransactionType
import com.loganes.finace.viewmodel.TransactionViewModel
// Pastikan warna ini ada di Theme.kt atau Color.kt Anda.
// Jika belum, uncomment baris di bawah ini:
// val ModernBackground = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val oldTransaction = viewModel.transactionToEdit

    // Safety check jika data null (misal setelah refresh)
    if (oldTransaction == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    // State Input
    var category by remember { mutableStateOf(oldTransaction.category) }
    var amountText by remember { mutableStateOf(oldTransaction.amount.toInt().toString()) }
    var description by remember { mutableStateOf(oldTransaction.description) }

    // State Dropdown
    var categoryExpanded by remember { mutableStateOf(false) }

    // Data Kategori
    val incomeCategories = listOf("Penjualan", "Top Up Kas Kecil", "Lain-lain")
    val expenseCategories = listOf("Kas Kecil (Harian)", "Operasional", "Belanja Perusahaan", "Maintenance", "Lain-lain")

    // Tentukan list kategori berdasarkan tipe transaksi yang sedang diedit
    val currentCategoryList = if (oldTransaction.type == TransactionType.INCOME) incomeCategories else expenseCategories

    // Style Input Modern (Konsisten dengan AddTransaction)
    val modernTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        cursorColor = BlueStart,
        focusedBorderColor = BlueStart,
        unfocusedBorderColor = Color(0xFFE0E0E0), // Abu lembut
        focusedLabelColor = BlueStart,
        unfocusedLabelColor = Color.Gray,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedSuffixColor = TextDark,
        unfocusedSuffixColor = TextDark
    )

    // Warna Background (Fallback jika ModernBackground belum diimport)
    val bgColor = Color(0xFFF8F9FA)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaksi", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueStart)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- HEADER & STATUS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Detail Transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = oldTransaction.date, // Tampilkan tanggal
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Badge Tipe (Hijau/Merah)
                val isIncome = oldTransaction.type == TransactionType.INCOME
                Surface(
                    color = if(isIncome) GreenIncome.copy(alpha=0.15f) else RedExpense.copy(alpha=0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if(isIncome) GreenIncome.copy(alpha=0.3f) else RedExpense.copy(alpha=0.3f))
                ) {
                    Text(
                        text = if(isIncome) "Pemasukan" else "Pengeluaran",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if(isIncome) GreenIncome else RedExpense,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // --- KARTU FORM EDIT ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Judul Cabang
                    Text(
                        text = oldTransaction.branch.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BlueStart
                    )

                    HorizontalDivider(color = Color(0xFFF5F5F5))

                    // 1. Input Nominal
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                        label = { Text("Jumlah Nominal") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = modernTextFieldColors,
                        singleLine = true
                    )

                    // 2. Kategori (DROPDOWN)
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = modernTextFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            currentCategoryList.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item, color = TextDark) },
                                    onClick = {
                                        category = item
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Deskripsi
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Catatan / Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5,
                        colors = modernTextFieldColors
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- TOMBOL SIMPAN ---
            Button(
                onClick = {
                    val newAmount = amountText.toDoubleOrNull() ?: 0.0
                    if (newAmount > 0 && category.isNotEmpty()) {
                        val updatedTransaction = oldTransaction.copy(
                            category = category,
                            amount = newAmount,
                            description = description
                        )
                        viewModel.updateTransaction(updatedTransaction)
                        Toast.makeText(context, "Data Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    } else {
                        Toast.makeText(context, "Mohon lengkapi nominal dan kategori", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueStart,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIMPAN PERUBAHAN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}