package com.loganes.finace.UI

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete // Tambahkan ikon Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Employee
import com.loganes.finace.viewmodel.TransactionViewModel
import com.loganes.finace.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// Warna Background Modern
private val ScreenBackground = Color(0xFFF8F9FA)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    val branches = listOf("Box Factory", "Maint. Alfa", "Saufa Olshop")

    // State Dialog
    var showDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }

    // State Dialog Hapus
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    // Cabang Aktif
    val currentBranch = when(selectedTab) {
        0 -> BranchType.BOX_FACTORY
        1 -> BranchType.MAINTENANCE_ALFA
        else -> BranchType.SAUFA_OLSHOP
    }

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Pegawai", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueStart)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    employeeToEdit = null
                    showDialog = true
                },
                containerColor = BlueStart,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, "Tambah")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- TAB NAVIGASI ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BlueStart,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .padding(horizontal = 12.dp)
                            .background(BlueStart, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                },
                divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
            ) {
                branches.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        selectedContentColor = BlueStart,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            // Filter List Pegawai
            val filteredEmployees = employees.filter { it.branch == currentBranch }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredEmployees.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Belum ada pegawai di cabang ini", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(filteredEmployees) { employee ->
                        EmployeeItemModern(
                            employee = employee,
                            statusColorCode = viewModel.getEmployeeStatusColor(employee),
                            onPayClick = { viewModel.payEmployee(employee) },
                            onEditClick = {
                                employeeToEdit = employee
                                showDialog = true
                            },
                            onDeleteClick = {
                                employeeToDelete = employee
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // --- DIALOG FORM ---
        if (showDialog) {
            AddEmployeeDialog(
                employeeToEdit = employeeToEdit,
                onDismiss = { showDialog = false },
                onSave = { name, salary, payDate, phone ->
                    if (employeeToEdit == null) {
                        viewModel.addEmployee(name, currentBranch, salary, payDate, phone)
                    } else {
                        val updatedEmployee = employeeToEdit!!.copy(
                            name = name,
                            salaryAmount = salary,
                            payDate = payDate,
                            phoneNumber = phone
                        )
                        viewModel.updateEmployee(updatedEmployee)
                    }
                    showDialog = false
                }
            )
        }

        // --- DIALOG KONFIRMASI HAPUS ---
        if (showDeleteConfirm && employeeToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Hapus Pegawai?") },
                text = { Text("Yakin ingin menghapus ${employeeToDelete?.name}? Data ini tidak bisa dikembalikan.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Panggil fungsi hapus di ViewModel
                            // Pastikan Anda sudah membuat fungsi deleteEmployee di TransactionViewModel
                            // viewModel.deleteEmployee(employeeToDelete!!)

                            // Contoh pemanggilan fungsi hapus (Anda perlu menambahkannya di ViewModel jika belum ada)
                            viewModel.deleteEmployee(employeeToDelete!!) // Uncomment jika sudah ada

                            showDeleteConfirm = false
                        }
                    ) {
                        Text("HAPUS", color = RedExpense, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("BATAL", color = Color.Gray)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun EmployeeItemModern(
    employee: Employee,
    statusColorCode: Int,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit // Callback baru untuk hapus
) {
    val context = LocalContext.current
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(employee.salaryAmount)

    // Warna Status
    val statusColor = when (statusColorCode) {
        1 -> GreenIncome
        2 -> Color(0xFFFF9800)
        3 -> RedExpense
        else -> Color.Gray
    }

    val statusText = when (statusColorCode) {
        1 -> "LUNAS"
        2 -> "SEGERA GAJI"
        3 -> "TELAT BAYAR"
        else -> "BELUM"
    }

    var showConfirmPay by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp), // Flat look
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: Avatar, Nama, Status & Actions
            Row(verticalAlignment = Alignment.Top) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(BlueStart.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = BlueStart,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Detail
                Column(modifier = Modifier.weight(1f)) {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text("Gajian Tanggal ${employee.payDate}", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chip Status
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (statusColorCode != 0) {
                                Icon(
                                    imageVector = if(statusColorCode == 1) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Action Icons (Edit & Delete)
                Column {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "Hapus", tint = RedExpense.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Nominal & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Gaji Pokok", fontSize = 11.sp, color = Color.Gray)
                    Text(formatRp, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                    // --- TOMBOL KIRIM WA (TEXT ONLY - DIRECT) ---
                    if (employee.phoneNumber.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val message = "Halo ${employee.name},\n\nBerikut adalah rincian gaji Anda bulan ini:\nNominal: $formatRp\nStatus: ${if(statusColorCode==1) "SUDAH DIBAYAR" else "DIPROSES"}\n\nTerima kasih atas kerja keras Anda di CV BST."
                                try {
                                    // Format nomor HP (08 -> 62)
                                    var phone = employee.phoneNumber.replace(Regex("[^0-9]"), "")
                                    if (phone.startsWith("0")) phone = "62" + phone.substring(1)
                                    if (phone.startsWith("8")) phone = "62$phone"

                                    val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
                                    val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            // Ikon Share Hijau
                            Icon(Icons.Default.Share, contentDescription = "Kirim WA", tint = Color(0xFF25D366))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Bayar
                    Button(
                        onClick = { showConfirmPay = true },
                        enabled = statusColorCode != 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (statusColorCode == 3) RedExpense else BlueStart,
                            disabledContainerColor = Color(0xFFEEEEEE)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = if (statusColorCode == 1) "TERBAYAR" else "BAYAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if(statusColorCode == 1) Color.Gray else Color.White
                        )
                    }
                }
            }
        }
    }

    if (showConfirmPay) {
        AlertDialog(
            onDismissRequest = { showConfirmPay = false },
            title = { Text("Konfirmasi Bayar Gaji", fontWeight = FontWeight.Bold) },
            text = { Text("Bayar gaji untuk ${employee.name} sebesar $formatRp?\n\nSaldo Pusat akan berkurang otomatis.") },
            confirmButton = {
                Button(
                    onClick = {
                        onPayClick()
                        showConfirmPay = false
                        Toast.makeText(context, "Gaji Berhasil Dibayar", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueStart)
                ) { Text("YA, BAYAR") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPay = false }) { Text("BATAL", color = TextDark) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Dialog Form
@Composable
fun AddEmployeeDialog(
    employeeToEdit: Employee? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(employeeToEdit?.name ?: "") }
    var salary by remember { mutableStateOf(employeeToEdit?.salaryAmount?.toInt()?.toString() ?: "") }
    var payDate by remember { mutableStateOf(employeeToEdit?.payDate?.toString() ?: "") }
    var phone by remember { mutableStateOf(employeeToEdit?.phoneNumber ?: "") }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        cursorColor = BlueStart,
        focusedBorderColor = BlueStart,
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedLabelColor = BlueStart,
        unfocusedLabelColor = Color.Gray,
        focusedContainerColor = Color(0xFFFAFAFA),
        unfocusedContainerColor = Color(0xFFFAFAFA)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employeeToEdit == null) "Tambah Pegawai" else "Edit Data Pegawai", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = inputColors,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = salary,
                    onValueChange = { if(it.all{c->c.isDigit()}) salary = it },
                    label = { Text("Gaji Pokok (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = inputColors,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = payDate,
                        onValueChange = { if(it.all{c->c.isDigit()}) payDate = it },
                        label = { Text("Tgl Gajian") },
                        placeholder = { Text("1-31") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = inputColors,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if(it.all{c->c.isDigit()}) phone = it },
                        label = { Text("No. WhatsApp") },
                        placeholder = { Text("628...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1.5f),
                        colors = inputColors,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if(name.isNotEmpty() && salary.isNotEmpty() && payDate.isNotEmpty()) {
                        onSave(name, salary.toDouble(), payDate.toIntOrNull()?.coerceIn(1,31) ?: 1, phone)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueStart),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("SIMPAN DATA", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("BATAL", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}