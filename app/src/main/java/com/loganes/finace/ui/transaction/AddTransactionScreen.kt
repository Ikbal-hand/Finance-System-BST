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
import androidx.compose.material.icons.filled.Save
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
import com.loganes.finace.data.model.BranchType
import com.loganes.finace.data.model.TransactionType
import com.loganes.finace.utils.SessionManager
import com.loganes.finace.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // State Input
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) } // Default Pengeluaran

    // State Dropdown
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }

    // Kategori Hardcode (Wajib Konsisten)
    val expenseCategories = listOf("Harian", "Belanja Stok", "Maintenance", "Operasional", "Gaji Pegawai", "Lain-lain")
    val incomeCategories = listOf("Penjualan", "Layanan Jasa", "Lain-lain")

    // Reset kategori saat tipe berubah
    LaunchedEffect(selectedType) { selectedCategory = "" }

    // Logic Date Picker
    val calendar = Calendar.getInstance()
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) } // Format YYYY-MM-DD

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val formattedDate = LocalDate.of(year, month + 1, dayOfMonth).toString()
            dateText = formattedDate
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Abu-abu muda
        topBar = {
            TopAppBar(
                title = { Text("Tambah Transaksi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
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

            // 1. PILIH TIPE (Chip Modern)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Chip Pemasukan
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    label = { Text("Pemasukan", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { if (selectedType == TransactionType.INCOME) Icon(Icons.Default.Save, null) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary, // Hijau
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
                // Chip Pengeluaran
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    label = { Text("Pengeluaran", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { if (selectedType == TransactionType.EXPENSE) Icon(Icons.Default.Save, null) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error, // Merah
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. FORM CONTAINER (Card Putih)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Input Nominal
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        label = { Text("Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Kategori (Dropdown)
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

                    // Input Deskripsi
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Tanggal
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        label = { Text("Tanggal") },
                        enabled = false,
                        trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.CalendarToday, null) } },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. TOMBOL SIMPAN (Biru)
            Button(
                onClick = {
                    val nominal = amount.toDoubleOrNull() ?: 0.0

                    if (nominal > 0 && selectedCategory.isNotEmpty()) {
                        // Tentukan Cabang (Prioritas: Dashboard Selection -> Session -> Pusat)
                        val targetBranch = viewModel.selectedDashboardBranch ?: try {
                            BranchType.valueOf(SessionManager.currentBranch)
                        } catch (e: Exception) {
                            BranchType.PUSAT
                        }

                        // Panggil ViewModel
                        viewModel.saveTransaction(
                            branch = targetBranch,
                            type = selectedType,
                            category = selectedCategory,
                            amount = nominal,
                            description = description.ifEmpty { selectedCategory },
                            onSuccess = {
                                Toast.makeText(context, "Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            onError = { errorMsg ->
                                // Munculkan Pesan Jika Saldo Kurang
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Nominal dan Kategori wajib diisi", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SIMPAN TRANSAKSI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}