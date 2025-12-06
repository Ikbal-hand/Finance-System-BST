package com.loganes.finace.ui.theme

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.TransactionType
import com.loganes.finace.viewmodel.TransactionViewModel


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedBranch by remember { mutableStateOf(BranchType.BOX_FACTORY) }
    var selectedType by remember { mutableStateOf(TransactionType.INCOME) }
    var category by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var branchExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val incomeCategories = listOf("Penjualan", "Top Up Kas Kecil", "Lain-lain")
    val expenseCategories = listOf("Kas Kecil (Harian)","Operasional (Operasional)", "Belanja Perusahaan", "Maintenance", "Lain-lain")

    LaunchedEffect(selectedType) { category = "" }

    // --- DEFINISI WARNA INPUT (PERBAIKAN DISINI) ---
    // Kita paksa teks jadi Gelap (TextDark) agar kelihatan di background putih
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
                title = { Text("Tambah Transaksi", fontWeight = FontWeight.Bold) },
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
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // 1. Pilihan Cabang
                    Text("Cabang", style = MaterialTheme.typography.labelLarge, color = TextLight)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { branchExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(selectedBranch.name, color = TextDark) // Teks Tombol jadi Gelap
                        }
                        DropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            BranchType.values().forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text(branch.name, color = TextDark) }, // Teks Menu jadi Gelap
                                    onClick = {
                                        selectedBranch = branch
                                        branchExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Jenis Transaksi
                    Text("Jenis Transaksi", style = MaterialTheme.typography.labelLarge, color = TextLight)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            label = { Text("Pemasukan / Top Up") },
                            leadingIcon = { if (selectedType == TransactionType.INCOME) Icon(Icons.Default.Check, null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenIncome.copy(alpha = 0.2f),
                                selectedLabelColor = GreenIncome,
                                labelColor = TextLight
                            )
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = { Text("Pengeluaran") },
                            leadingIcon = { if (selectedType == TransactionType.EXPENSE) Icon(Icons.Default.Check, null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedExpense.copy(alpha = 0.2f),
                                selectedLabelColor = RedExpense,
                                labelColor = TextLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Input Kategori (Dropdown)
                    Text("Kategori", style = MaterialTheme.typography.labelLarge, color = TextLight)
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Kategori") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors // <--- TERAPKAN WARNA DISINI
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            val listToShow = if (selectedType == TransactionType.INCOME) incomeCategories else expenseCategories
                            listToShow.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item, color = TextDark) }, // Teks Item jadi Gelap
                                    onClick = {
                                        category = item
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Input Jumlah
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = customTextFieldColors // <--- TERAPKAN WARNA DISINI
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. Input Deskripsi
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi / Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        colors = customTextFieldColors // <--- TERAPKAN WARNA DISINI
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val isLainLain = category.equals("Lain-lain", ignoreCase = true)

                    if (amount <= 0 || category.isEmpty()) {
                        Toast.makeText(context, "Lengkapi Jumlah dan Kategori!", Toast.LENGTH_SHORT).show()
                    } else if (isLainLain && description.isBlank()) {
                        Toast.makeText(context, "Deskripsi wajib untuk 'Lain-lain'!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveTransaction(selectedBranch, selectedType, category, amount, description)
                        Toast.makeText(context, "Transaksi Tersimpan!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueStart)
            ) {
                Text("SIMPAN TRANSAKSI", fontWeight = FontWeight.Bold)
            }
        }
    }
}