package com.loganes.finace.ui.screen.transaction

import android.app.DatePickerDialog
import android.os.Build
import android.widget.DatePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.TransactionType
import com.loganes.finace.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Ambil data yang mau diedit
    val existingTransaction = viewModel.transactionToEdit

    // Jika null (error), kembali
    if (existingTransaction == null) {
        onNavigateBack()
        return
    }

    // State UI (Diisi Data Lama)
    var amount by remember { mutableStateOf(existingTransaction.amount.toInt().toString()) }
    var description by remember { mutableStateOf(existingTransaction.description) }
    var selectedType by remember { mutableStateOf(existingTransaction.typeEnum) }
    var selectedCategory by remember { mutableStateOf(existingTransaction.category) }

    var expanded by remember { mutableStateOf(false) }

    val expenseCategories = listOf("Harian", "Belanja Stok", "Maintenance", "Operasional", "Gaji Pegawai", "Lain-lain")
    val incomeCategories = listOf("Penjualan", "Layanan Jasa", "Lain-lain")

    // Date Picker
    val calendar = Calendar.getInstance()
    var dateText by remember { mutableStateOf(existingTransaction.date) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            dateText = LocalDate.of(year, month + 1, dayOfMonth).toString()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaksi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // 1. TIPE (Disable Edit Tipe agar tidak merusak flow kas kecil)
            // User sebaiknya hapus dan buat baru jika salah tipe fatal
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { /* Disable Click */ },
                    label = { Text("Pemasukan") },
                    enabled = false, // Tidak bisa ubah tipe saat edit
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = FilterChipDefaults.filterChipColors(disabledContainerColor = if(selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.tertiary else Color.LightGray)
                )
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { /* Disable Click */ },
                    label = { Text("Pengeluaran") },
                    enabled = false,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = FilterChipDefaults.filterChipColors(disabledContainerColor = if(selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else Color.LightGray)
                )
            }
            Text("Tip: Hapus transaksi dan buat baru jika ingin mengubah Tipe.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // 2. FORM CONTAINER
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Nominal
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        label = { Text("Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Kategori
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            val listToShow = if (selectedType == TransactionType.INCOME) incomeCategories else expenseCategories
                            listToShow.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = { selectedCategory = item; expanded = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Deskripsi
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tanggal
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        label = { Text("Tanggal") },
                        enabled = false,
                        trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.CalendarToday, null) } },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. TOMBOL UPDATE
            Button(
                onClick = {
                    val nominal = amount.toDoubleOrNull() ?: 0.0
                    if (nominal > 0 && selectedCategory.isNotEmpty()) {

                        // Buat Objek Baru dengan ID Lama
                        val updatedTransaction = existingTransaction.copy(
                            amount = nominal,
                            category = selectedCategory,
                            description = description,
                            date = dateText
                        )

                        // Panggil Update di ViewModel
                        viewModel.updateTransaction(updatedTransaction)

                        Toast.makeText(context, "Data Diperbarui!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    } else {
                        Toast.makeText(context, "Lengkapi data nominal", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SIMPAN PERUBAHAN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}