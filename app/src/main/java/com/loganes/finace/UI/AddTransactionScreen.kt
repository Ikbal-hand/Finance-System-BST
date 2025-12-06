package com.loganes.finace.UI

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.TransactionType
import com.loganes.finace.viewmodel.TransactionViewModel
import com.loganes.finace.ui.theme.*

// Warna Background Modern (Fallback jika belum ada di Theme)
private val ScreenBackground = Color(0xFFF8F9FA)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // State Form
    var selectedBranch by remember { mutableStateOf(BranchType.BOX_FACTORY) }
    var selectedType by remember { mutableStateOf(TransactionType.INCOME) }
    var category by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // State Dropdown
    var branchExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Data Kategori
    val incomeCategories = listOf("Penjualan", "Top Up Kas Kecil", "Lain-lain")
    val expenseCategories = listOf("Kas Kecil (Harian)", "Operasional", "Belanja Perusahaan", "Maintenance", "Lain-lain")

    // Reset kategori saat tipe transaksi berubah
    LaunchedEffect(selectedType) { category = "" }

    // --- STYLE INPUT MODERN (CLEAN - TANPA ICON BERLEBIH) ---
    val cleanTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        cursorColor = BlueStart,
        focusedBorderColor = BlueStart,
        unfocusedBorderColor = Color(0xFFE0E0E0), // Abu muda halus
        focusedLabelColor = BlueStart,
        unfocusedLabelColor = Color.Gray,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedPrefixColor = TextDark,
        unfocusedPrefixColor = TextDark
    )

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Tambah Transaksi", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- KARTU FORM ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp), // Flat look
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // 1. SWITCH JENIS TRANSAKSI (Besar & Jelas)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Pemasukan
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedType == TransactionType.INCOME) GreenIncome else Color.Transparent)
                                .clickable { selectedType = TransactionType.INCOME },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Pemasukan",
                                color = if (selectedType == TransactionType.INCOME) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Tombol Pengeluaran
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedType == TransactionType.EXPENSE) RedExpense else Color.Transparent)
                                .clickable { selectedType = TransactionType.EXPENSE },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Pengeluaran",
                                color = if (selectedType == TransactionType.EXPENSE) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    // 2. PILIH CABANG (Dropdown Modern)
                    ExposedDropdownMenuBox(
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = !branchExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBranch.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cabang") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = cleanTextFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            BranchType.values().forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text(branch.name, color = TextDark) },
                                    onClick = {
                                        selectedBranch = branch
                                        branchExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. NOMINAL UANG
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                        label = { Text("Nominal") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = TextDark) }, // Prefix Rp yang rapi
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = cleanTextFieldColors,
                        singleLine = true
                    )

                    // 4. KATEGORI (Dropdown Modern)
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            placeholder = { Text("Pilih kategori...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = cleanTextFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            val listToShow = if (selectedType == TransactionType.INCOME) incomeCategories else expenseCategories
                            listToShow.forEach { item ->
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

                    // 5. DESKRIPSI
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Catatan / Keterangan") },
                        placeholder = { Text("Contoh: Pembelian ATK, Jual Box...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5,
                        colors = cleanTextFieldColors
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // TOMBOL SIMPAN
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val isLainLain = category.equals("Lain-lain", ignoreCase = true)

                    if (amount <= 0 || category.isEmpty()) {
                        Toast.makeText(context, "Mohon lengkapi nominal dan kategori", Toast.LENGTH_SHORT).show()
                    } else if (isLainLain && description.isBlank()) {
                        Toast.makeText(context, "Wajib isi keterangan untuk kategori Lain-lain", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveTransaction(selectedBranch, selectedType, category, amount, description)
                        Toast.makeText(context, "Transaksi Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
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
                Text(
                    text = "SIMPAN DATA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}