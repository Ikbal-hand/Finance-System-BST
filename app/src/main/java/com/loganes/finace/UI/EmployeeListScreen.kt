package com.loganes.finace.ui.theme

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Employee
import com.loganes.finace.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val employees by viewModel.allEmployees.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(0) }
    val branches = listOf("Box Factory", "Maint. Alfa", "Saufa Olshop")

    // State untuk Dialog (Tambah/Edit)
    var showDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) } // Jika null = Tambah, Jika isi = Edit

    // Tentukan Cabang Aktif
    val currentBranch = when(selectedTab) {
        0 -> BranchType.BOX_FACTORY
        1 -> BranchType.MAINTENANCE_ALFA
        else -> BranchType.SAUFA_OLSHOP
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Pegawai", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlueStart,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    employeeToEdit = null // Reset ke mode Tambah
                    showDialog = true
                },
                containerColor = BlueEnd,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Tambah Pegawai")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(GrayBackground) // Background Modern
        ) {
            // Tab Navigasi Cabang
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BlueStart,
                divider = {}
            ) {
                branches.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                }
            }

            // Filter List Pegawai
            val filteredEmployees = employees.filter { it.branch == currentBranch }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEmployees) { employee ->
                    EmployeeItemModern(
                        employee = employee,
                        statusColorCode = viewModel.getEmployeeStatusColor(employee),
                        onPayClick = { viewModel.payEmployee(employee) },
                        onEditClick = {
                            employeeToEdit = employee // Set mode Edit
                            showDialog = true
                        }
                    )
                }

                if (filteredEmployees.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada pegawai di cabang ini.", color = Color.Gray)
                        }
                    }
                }

                // Ruang kosong bawah
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Dialog Form (Re-use untuk Tambah & Edit)
        if (showDialog) {
            AddEmployeeDialog(
                employeeToEdit = employeeToEdit,
                onDismiss = { showDialog = false },
                onSave = { name, salary, payDate, phone ->
                    if (employeeToEdit == null) {
                        // LOGIKA TAMBAH BARU
                        viewModel.addEmployee(name, currentBranch, salary, payDate, phone)
                    } else {
                        // LOGIKA UPDATE (EDIT)
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
    }
}

@Composable
fun EmployeeItemModern(
    employee: Employee,
    statusColorCode: Int,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(employee.salaryAmount)

    // Logika Warna Status
    val statusColor = when (statusColorCode) {
        1 -> GreenIncome      // Sudah Digaji
        2 -> Color(0xFFFFC107)// H-3 (Kuning)
        3 -> RedExpense       // Belum/Telat (Merah)
        else -> Color.Gray
    }

    val statusText = when (statusColorCode) {
        1 -> "Lunas"
        2 -> "Segera Gaji"
        3 -> "Belum Digaji"
        else -> "-"
    }

    var showConfirmPay by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar, Nama, Edit Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = BlueStart.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = BlueStart)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text("Gajian Tgl: ${employee.payDate}", fontSize = 12.sp, color = TextLight)
                }

                // Indikator Status
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha=0.5f))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tombol Edit (Pensil)
                IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))

            // Footer: Nominal & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatRp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BlueStart
                )

                Row {
                    // Tombol WA (Kirim Struk)
                    if (employee.phoneNumber.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val message = "Halo ${employee.name},\n\nBerikut adalah rincian gaji Anda bulan ini:\nNominal: $formatRp\nStatus: ${if(statusColorCode==1) "SUDAH DIBAYAR" else "DIPROSES"}\n\nTerima kasih atas kerja keras Anda di CV BST."
                                try {
                                    val url = "https://api.whatsapp.com/send?phone=${employee.phoneNumber}&text=${Uri.encode(message)}"
                                    val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            // Kita pakai ikon Share tapi warnanya Hijau WhatsApp
                            Icon(Icons.Default.Share, contentDescription = "Kirim WA", tint = Color(0xFF25D366))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Bayar
                    Button(
                        onClick = { showConfirmPay = true },
                        enabled = statusColorCode != 1, // Disable jika sudah lunas
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (statusColorCode == 3) RedExpense else BlueStart,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (statusColorCode == 1) "LUNAS" else "BAYAR", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Bayar
    if (showConfirmPay) {
        AlertDialog(
            onDismissRequest = { showConfirmPay = false },
            title = { Text("Konfirmasi Pembayaran") },
            text = { Text("Apakah Anda yakin ingin membayar gaji untuk ${employee.name} sebesar $formatRp?\n\nSaldo Pusat akan berkurang otomatis.") },
            confirmButton = {
                TextButton(onClick = {
                    onPayClick()
                    showConfirmPay = false
                    Toast.makeText(context, "Gaji Dibayar!", Toast.LENGTH_SHORT).show()
                }) { Text("YA, BAYAR", color = BlueStart, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPay = false }) { Text("BATAL", color = TextLight) }
            },
            containerColor = Color.White
        )
    }
}

// Dialog Tambah/Edit Pegawai
@Composable
fun AddEmployeeDialog(
    employeeToEdit: Employee? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, String) -> Unit
) {
    // State form (diisi data lama jika mode Edit)
    var name by remember { mutableStateOf(employeeToEdit?.name ?: "") }
    var salary by remember { mutableStateOf(employeeToEdit?.salaryAmount?.toInt()?.toString() ?: "") }
    var payDate by remember { mutableStateOf(employeeToEdit?.payDate?.toString() ?: "") }
    var phone by remember { mutableStateOf(employeeToEdit?.phoneNumber ?: "") }

    // Warna input text
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        cursorColor = BlueStart,
        focusedBorderColor = BlueStart,
        unfocusedBorderColor = Color.LightGray,
        focusedLabelColor = BlueStart,
        unfocusedLabelColor = TextLight
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employeeToEdit == null) "Tambah Pegawai" else "Edit Data Pegawai", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = salary,
                    onValueChange = { if(it.all{c->c.isDigit()}) salary = it },
                    label = { Text("Gaji Pokok (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = payDate,
                    onValueChange = { if(it.all{c->c.isDigit()}) payDate = it },
                    label = { Text("Tanggal Gajian (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { if(it.all{c->c.isDigit()}) phone = it },
                    label = { Text("No. WhatsApp (628...)") },
                    placeholder = { Text("Cth: 628123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
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
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SIMPAN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL", color = TextLight) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}