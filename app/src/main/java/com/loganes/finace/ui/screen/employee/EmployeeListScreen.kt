package com.loganes.finace.ui.screen.employee

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.window.Dialog
import com.loganes.finace.data.model.BranchType
import com.loganes.finace.data.model.Employee
import com.loganes.finace.ui.theme.BlueStart
import com.loganes.finace.ui.theme.GreenIncome
import com.loganes.finace.ui.theme.RedExpense
import com.loganes.finace.viewmodel.TransactionViewModel
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    targetEmployeeId: String? = null // ID Pegawai yang dicari (dari Notifikasi)
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val userBranch by viewModel.userBranch.collectAsState()
    val isPusat = userBranch == "PUSAT"

    // --- LOGIC TAB CABANG ---
    val branchTabs = listOf(BranchType.BOX_FACTORY, BranchType.MAINTENANCE_ALFA, BranchType.SAUFA_OLSHOP)
    val branchTitles = listOf("Box Factory", "Maint. Alfa", "Saufa Olshop")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Tentukan Cabang Aktif untuk Filter
    val activeBranch = if (isPusat) branchTabs[selectedTabIndex] else try { BranchType.valueOf(userBranch) } catch (e:Exception) { BranchType.BOX_FACTORY }

    // Filter Pegawai Sesuai Tab/User
    val filteredEmployees = employees.filter { it.branch == activeBranch.name }

    // State Dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var employeeToPay by remember { mutableStateOf<Employee?>(null) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    // --- AUTO SCROLL & HIGHLIGHT LOCATOR ---
    val listState = rememberLazyListState()

    // Jika ada targetID (dari notif), cari tab-nya dan scroll
    LaunchedEffect(targetEmployeeId, employees) {
        if (targetEmployeeId != null && employees.isNotEmpty()) {
            val targetEmp = employees.find { it.id == targetEmployeeId }
            if (targetEmp != null) {
                // 1. Pindah Tab jika perlu (Hanya Pusat)
                if (isPusat) {
                    val branchIndex = branchTabs.indexOfFirst { it.name == targetEmp.branch }
                    if (branchIndex >= 0) selectedTabIndex = branchIndex
                }

                // 2. Scroll ke item (Delay dikit biar render list selesai)
                delay(300)
                val index = filteredEmployees.indexOfFirst { it.id == targetEmployeeId }
                if (index >= 0) {
                    listState.animateScrollToItem(index)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchEmployees()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Pegawai", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
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
            // --- TAB BAR (HANYA PUSAT) ---
            if (isPusat) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .height(3.dp)
                                .padding(horizontal = 20.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                ) {
                    branchTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        )
                    }
                }
            } else {
                // Header Sederhana untuk Cabang
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Daftar Pegawai: ${activeBranch.name.replace("_", " ")}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // --- LIST PEGAWAI ---
            if (filteredEmployees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data pegawai di cabang ini.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(filteredEmployees) { _, employee ->
                        EmployeeCardModern(
                            employee = employee,
                            statusColor = viewModel.getEmployeeStatusColor(employee),
                            isHighlighted = employee.id == targetEmployeeId, // Efek Kedip
                            onPayClick = { employeeToPay = employee },
                            onEditClick = { employeeToEdit = employee },
                            onDeleteClick = { employeeToDelete = employee },
                            onWhatsAppClick = {
                                sendWhatsAppPaySlip(context, employee)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS (Tambah, Edit, Hapus, Bayar) ---
    // (Kode Dialog sama seperti sebelumnya, hanya disesuaikan sedikit)

    // 1. ADD EMPLOYEE
    if (showAddDialog) {
        AddEmployeeDialog(
            userBranch = userBranch,
            activeTabBranch = activeBranch, // Pre-fill branch sesuai tab aktif
            onDismiss = { showAddDialog = false },
            onConfirm = { name, branch, salary, date, phone ->
                viewModel.addEmployee(name, branch, salary, date, phone)
                showAddDialog = false
                Toast.makeText(context, "Pegawai Ditambahkan", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. PAY CONFIRMATION
    if (employeeToPay != null) {
        val emp = employeeToPay!!
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(emp.salaryAmount)
        AlertDialog(
            onDismissRequest = { employeeToPay = null },
            icon = { Icon(Icons.Default.Payments, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Bayar Gaji?") },
            text = { Text("Konfirmasi pembayaran gaji untuk ${emp.name} sebesar $formatRp?\n\nSaldo akan dipotong dari Rekening Pusat.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.payEmployee(emp)
                    employeeToPay = null
                    Toast.makeText(context, "Gaji Berhasil Dibayar", Toast.LENGTH_SHORT).show()
                }) { Text("Bayar Sekarang") }
            },
            dismissButton = { TextButton(onClick = { employeeToPay = null }) { Text("Batal") } },
            containerColor = Color.White
        )
    }

    // 3. DELETE CONFIRMATION
    if (employeeToDelete != null) {
        val emp = employeeToDelete!!
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Hapus Pegawai?") },
            text = { Text("Data ${emp.name} akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteEmployee(emp.id)
                    employeeToDelete = null
                    Toast.makeText(context, "Pegawai Dihapus", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { employeeToDelete = null }) { Text("Batal") } },
            containerColor = Color.White
        )
    }

    // 4. EDIT DIALOG (Sederhana, bisa dikembangkan)
    // Untuk mempersingkat, kita reuse dialog tambah tapi diisi data lama
    if (employeeToEdit != null) {
        // Implementasi Edit Dialog bisa mirip AddEmployeeDialog tapi panggil updateEmployee
        // Placeholder toast
        Toast.makeText(context, "Fitur Edit akan segera hadir (Gunakan Hapus & Buat Baru sementara)", Toast.LENGTH_LONG).show()
        employeeToEdit = null
    }
}

// --- HELPER WHATSAPP ---
fun sendWhatsAppPaySlip(context: Context, employee: Employee) {
    val phone = employee.phoneNumber
    if (phone.isEmpty()) {
        Toast.makeText(context, "Nomor HP Pegawai belum diisi!", Toast.LENGTH_SHORT).show()
        return
    }

    // Format nomor (ganti 08 jadi 628)
    val cleanPhone = if (phone.startsWith("0")) "62" + phone.substring(1) else phone
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(employee.salaryAmount)
    val month = LocalDate.now().month.name.lowercase().replaceFirstChar { it.uppercase() }

    val message = """
        Halo ${employee.name},
        
        Gaji bulan $month sebesar $formatRp telah ditransfer.
        Terima kasih atas kerja keras Anda di BST Finance!
        
        Salam,
        Admin Keuangan
    """.trimIndent()

    try {
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(message, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
    }
}

// --- KOMPONEN UI MODERN ---

@Composable
fun EmployeeCardModern(
    employee: Employee,
    statusColor: Int,
    isHighlighted: Boolean,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }.format(employee.salaryAmount)

    // Animasi Highlight (Kedip)
    val bgColor = remember { Animatable(Color.White) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            // Kedip 3 kali kuning -> putih
            repeat(3) {
                bgColor.animateTo(Color(0xFFFFF9C4), animationSpec = tween(500))
                bgColor.animateTo(Color.White, animationSpec = tween(500))
            }
        }
    }

    val (indicatorColor, statusText) = when(statusColor) {
        1 -> Pair(GreenIncome, "Lunas")
        2 -> Pair(Color(0xFFFF9800), "H-3 Gajian")
        3 -> Pair(RedExpense, "Telat Bayar")
        else -> Pair(Color.LightGray, "Belum Waktunya")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.value),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar & Nama & Edit/Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(employee.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(employee.branch.replace("_", " "), fontSize = 12.sp, color = Color.Gray)
                }

                // Tombol Edit & Hapus Kecil
                IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Edit, null, tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Delete, null, tint = RedExpense)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(12.dp))

            // Info Gaji & Status
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Gaji Pokok", fontSize = 11.sp, color = Color.Gray)
                    Text(formatRp, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(indicatorColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(statusText, fontSize = 12.sp, color = if(statusColor==0) Color.Gray else indicatorColor, fontWeight = FontWeight.Medium)
                    }
                }

                // Tombol Aksi (WA & Bayar)
                Row {
                    // Tombol WA
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier.background(Color(0xFF25D366).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        // Icon WA (Pakai Icon Email/Chat sebagai ganti jika tidak ada icon WA bawaan)
                        Icon(Icons.Default.Chat, null, tint = Color(0xFF25D366))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Bayar (Jika belum lunas)
                    if (statusColor != 1) {
                        Button(
                            onClick = onPayClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Bayar", fontSize = 12.sp)
                        }
                    } else {
                        // Icon Lunas
                        Icon(Icons.Default.CheckCircle, null, tint = GreenIncome, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeDialog(
    userBranch: String,
    activeTabBranch: BranchType,
    onDismiss: () -> Unit,
    onConfirm: (String, BranchType, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var payDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val isPusat = userBranch == "PUSAT"
    // Default cabang sesuai Tab yang sedang dibuka Admin Pusat
    var selectedBranch by remember { mutableStateOf(if(isPusat) activeTabBranch else try { BranchType.valueOf(userBranch) } catch(e:Exception){ BranchType.BOX_FACTORY }) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tambah Pegawai", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = salary, onValueChange = { if (it.all { c -> c.isDigit() }) salary = it }, label = { Text("Gaji (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = payDate, onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) payDate = it }, label = { Text("Tgl Gajian (1-31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                // Input No HP untuk WA
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.all { c -> c.isDigit() }) phone = it },
                    label = { Text("No. HP (Contoh: 08123...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isPusat) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(value = selectedBranch.name, onValueChange = {}, readOnly = true, label = { Text("Cabang") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            BranchType.values().filter { it != BranchType.PUSAT }.forEach { branch ->
                                DropdownMenuItem(text = { Text(branch.name) }, onClick = { selectedBranch = branch; expanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(onClick = {
                        val sal = salary.toDoubleOrNull() ?: 0.0
                        val date = payDate.toIntOrNull() ?: 25
                        if (name.isNotEmpty() && sal > 0) onConfirm(name, selectedBranch, sal, date, phone)
                    }) { Text("Simpan") }
                }
            }
        }
    }
}