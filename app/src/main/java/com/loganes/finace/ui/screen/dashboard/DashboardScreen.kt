package com.loganes.finace.ui.screen.dashboard

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.loganes.finace.data.model.BranchType
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.TransactionType
import com.loganes.finace.utils.FinanceCalculator
import com.loganes.finace.viewmodel.TransactionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// Data Class Notifikasi dengan Target ID (untuk Navigasi)
data class NotifItem(
    val message: String,
    val color: Color,
    val icon: ImageVector,
    val targetId: String? = null // ID Pegawai (Opsional)
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    onAddClick: () -> Unit,
    onPayrollClick: (String?) -> Unit, // Parameter diubah: Bisa terima ID
    onReportClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditTransaction: () -> Unit
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val userBranch by viewModel.userBranch.collectAsState()

    // Hemat Kuota: Fetch data hanya jika kosong
    LaunchedEffect(Unit) {
        if (allTransactions.isEmpty()) viewModel.fetchTransactions()
        viewModel.fetchUserBranch()
        viewModel.fetchEmployees()
    }

    val isPusat = userBranch == "PUSAT"

    // Hitung Uang via Calculator
    val kasUtama = remember(allTransactions) { FinanceCalculator.calculateMainCash(allTransactions) }
    val totalAset = remember(allTransactions) { FinanceCalculator.calculateTotalAssets(allTransactions) }
    val totalKasKecil = remember(allTransactions) {
        FinanceCalculator.calculateBranchPettyCash(allTransactions, BranchType.BOX_FACTORY) +
                FinanceCalculator.calculateBranchPettyCash(allTransactions, BranchType.MAINTENANCE_ALFA) +
                FinanceCalculator.calculateBranchPettyCash(allTransactions, BranchType.SAUFA_OLSHOP)
    }

    // UI State
    val branchTabs = listOf(BranchType.BOX_FACTORY, BranchType.MAINTENANCE_ALFA, BranchType.SAUFA_OLSHOP)
    val branchTitles = listOf("Box Factory", "Maint. Alfa", "Saufa Olshop")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showTopUpDialog by remember { mutableStateOf(false) }

    // State Dialog Hapus Transaksi
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val activeBranch = if (isPusat) {
        branchTabs[selectedTabIndex]
    } else {
        try { BranchType.valueOf(userBranch) } catch (e: Exception) { BranchType.BOX_FACTORY }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNotifDropdown by remember { mutableStateOf(false) }

    // --- LOGIKA NOTIFIKASI CERDAS ---
    val notifications = remember(allTransactions, employees, userBranch, isPusat) {
        val list = mutableListOf<NotifItem>()
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

        // 1. Cek Kas Kecil Per Cabang (Spesifik)
        val branches = listOf(BranchType.BOX_FACTORY, BranchType.MAINTENANCE_ALFA, BranchType.SAUFA_OLSHOP)
        branches.forEach { branch ->
            if (isPusat || userBranch == branch.name) {
                val balance = FinanceCalculator.calculateBranchPettyCash(allTransactions, branch)
                if (balance < 500000) {
                    val branchName = branch.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    list.add(NotifItem(
                        message = "Kas $branchName Menipis! (Sisa: ${formatRp.format(balance)})",
                        color = Color.Red,
                        icon = Icons.Default.Warning
                    ))
                }
            }
        }

        // 2. Notif Gaji Pegawai (Dengan ID Target)
        val relevantEmployees = if (isPusat) employees else employees.filter { it.branch == userBranch }
        relevantEmployees.forEach { emp ->
            val payDay = LocalDate.now().withDayOfMonth(emp.payDate.coerceIn(1, LocalDate.now().lengthOfMonth()))
            val daysToPay = ChronoUnit.DAYS.between(LocalDate.now(), payDay)

            // Cek apakah sudah dibayar bulan ini?
            val isPaid = try {
                val lastPaid = LocalDate.parse(emp.lastPaidDate ?: "2000-01-01")
                lastPaid.month == LocalDate.now().month && lastPaid.year == LocalDate.now().year
            } catch (e: Exception) { false }

            if (!isPaid && daysToPay in 0..3) {
                list.add(NotifItem(
                    message = "Gaji ${emp.name} H-$daysToPay",
                    color = Color(0xFFFF9800),
                    icon = Icons.Default.Payments,
                    targetId = emp.id // Simpan ID Pegawai untuk navigasi
                ))
            }
        }
        list
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // --- SIDEBAR MODERN ---
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                // Header Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("BST Finance", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(if (isPusat) "Administrator Pusat" else "Staff Cabang", color = MaterialTheme.colorScheme.onPrimary.copy(0.8f), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- MENU FILTER (Hak Akses) ---
                val menuItems = mutableListOf<Triple<String, ImageVector, () -> Unit>>()

                if (isPusat) {
                    menuItems.add(Triple("Laporan Keuangan", Icons.Outlined.Description, onReportClick))
                    // Menu Pegawai Biasa (Tanpa target ID)
                    menuItems.add(Triple("Manajemen Pegawai", Icons.Outlined.People, { onPayrollClick(null) }))
                    menuItems.add(Triple("Sampah / Restore", Icons.Outlined.Delete, onTrashClick))
                }
                menuItems.add(Triple("Pengaturan & Akun", Icons.Outlined.Settings, onSettingsClick))

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.first, fontWeight = FontWeight.Medium) },
                        icon = { Icon(item.second, null) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() }; item.third() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.onPrimary) } },
                    actions = {
                        IconButton(onClick = { viewModel.fetchTransactions() }) { Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onPrimary) }

                        // Icon Notifikasi dengan Dropdown
                        Box {
                            IconButton(onClick = { showNotifDropdown = true }) {
                                BadgedBox(badge = { if (notifications.isNotEmpty()) Badge { Text("${notifications.size}") } }) {
                                    Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            DropdownMenu(expanded = showNotifDropdown, onDismissRequest = { showNotifDropdown = false }, modifier = Modifier.background(Color.White).width(300.dp)) {
                                if (notifications.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Aman! Tidak ada notifikasi.", fontSize = 12.sp) }, onClick = { showNotifDropdown = false })
                                } else {
                                    notifications.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item.message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = { Icon(item.icon, null, tint = item.color) },
                                            onClick = {
                                                showNotifDropdown = false
                                                // NAVIGASI PINTAR: Jika ada ID, buka pegawai tersebut
                                                if (item.targetId != null) {
                                                    onPayrollClick(item.targetId)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.selectedDashboardBranch = activeBranch; onAddClick() }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Add, "Tambah")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(16.dp))

                if (isPusat) {
                    TotalAssetCardTheme(totalAset, kasUtama, totalKasKecil)
                } else {
                    val myPettyCash = FinanceCalculator.calculateBranchPettyCash(allTransactions, activeBranch)
                    BranchGreenCardTheme(myPettyCash, activeBranch.name.replace("_", " "), {}, false)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isPusat) {
                    ScrollableTabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent, edgePadding = 0.dp, indicator = { tabPositions -> Box(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]).height(3.dp).padding(horizontal = 12.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))) }, divider = {}) {
                        branchTitles.forEachIndexed { index, title ->
                            Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title, fontWeight = if(selectedTabIndex==index) FontWeight.Bold else FontWeight.Normal, color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray) })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    val branchPettyCash = FinanceCalculator.calculateBranchPettyCash(allTransactions, activeBranch)
                    BranchGreenCardTheme(amount = branchPettyCash, branchName = activeBranch.name.replace("_", " "), onTopUpClick = { showTopUpDialog = true }, isPusatUser = true)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                val chartData = FinanceCalculator.getChartData(allTransactions, activeBranch, false)
                Text("Ringkasan Harian", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))

                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    if(chartData.isNotEmpty()) {
                        DailyBarChartSimple(chartData)
                    } else {
                        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Belum ada data grafik", color = Color.Gray, fontSize = 12.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Transaksi Terkini", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))

                val listData = allTransactions.filter { it.branchEnum == activeBranch }
                if (listData.isEmpty()) {
                    Text("Belum ada transaksi.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    listData.sortedByDescending { it.date }.take(20).forEach { trans ->
                        TransactionItemWithAction(
                            transaction = trans,
                            onEdit = {
                                viewModel.transactionToEdit = trans
                                onEditTransaction()
                            },
                            onDelete = {
                                transactionToDelete = trans
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialog Top Up
    if (showTopUpDialog) {
        TopUpDialogTheme(
            branchName = activeBranch.name.replace("_", " "),
            onDismiss = { showTopUpDialog = false },
            onConfirm = { amount ->
                viewModel.performTopUp(
                    targetBranch = activeBranch,
                    amount = amount,
                    onSuccess = { Toast.makeText(context, "Top Up Berhasil!", Toast.LENGTH_SHORT).show(); showTopUpDialog = false },
                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                )
            }
        )
    }

    // Dialog Konfirmasi Hapus
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Hapus Transaksi?") },
            text = { Text("Transaksi '${transactionToDelete?.category}' akan dipindahkan ke Sampah.") },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.softDeleteTransaction(it) }
                        transactionToDelete = null
                        Toast.makeText(context, "Data dihapus sementara", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Batal") }
            },
            containerColor = Color.White
        )
    }
}

// --- KOMPONEN UI ---

@Composable
fun TransactionItemWithAction(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.typeEnum == TransactionType.INCOME
    val amountColor = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(transaction.amount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(amountColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = amountColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            val info = if(transaction.category.contains("Top Up")) "Transfer" else if (transaction.isPettyCash) "Kas Kecil" else "Pusat"
            Text("${transaction.date} • $info", fontSize = 11.sp, color = Color.Gray)
            Text(text = (if(isIncome) "+ " else "- ") + formatRp, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = amountColor)
        }

        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Edit, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error.copy(0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TotalAssetCardTheme(totalAset: Double, kasPusat: Double, totalKasKecil: Double) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))).padding(24.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.onPrimary.copy(0.8f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Total Aset Bersih", color = MaterialTheme.colorScheme.onPrimary.copy(0.8f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = formatRp.format(totalAset), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onPrimary.copy(0.15f), RoundedCornerShape(12.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Kas Pusat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(0.7f))
                        Text(formatRp.format(kasPusat), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.onPrimary.copy(0.3f)))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Kas Kecil", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(0.7f))
                        Text(formatRp.format(totalKasKecil), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun BranchGreenCardTheme(amount: Double, branchName: String, onTopUpClick: () -> Unit, isPusatUser: Boolean) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    val textGreen = MaterialTheme.colorScheme.tertiary
    val bgGreen = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)

    Card(colors = CardDefaults.cardColors(containerColor = bgGreen), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = textGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sisa Kas Kecil ($branchName)", color = textGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = formatRp.format(amount), color = textGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (isPusatUser) {
                Button(onClick = onTopUpClick, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), elevation = ButtonDefaults.buttonElevation(2.dp)) {
                    Text("Top Up", color = textGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.Add, null, tint = textGreen, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DailyBarChartSimple(transactions: List<Transaction>) {
    val groupedData = remember(transactions) {
        transactions.groupBy { it.date }.mapValues { entry ->
            val income = entry.value.filter { it.typeEnum == TransactionType.INCOME }.sumOf { it.amount }
            val expense = entry.value.filter { it.typeEnum == TransactionType.EXPENSE }.sumOf { it.amount }
            Pair(income.toFloat(), expense.toFloat())
        }.toList().sortedBy { it.first }.takeLast(5)
    }

    AndroidView(modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp), factory = { context ->
        BarChart(context).apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 10f
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
            setFitBars(true)
            animateY(800)
        }
    }, update = { chart ->
        if (groupedData.isNotEmpty()) {
            val incomeEntries = ArrayList<BarEntry>()
            val expenseEntries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()
            val formatter = DateTimeFormatter.ofPattern("dd/MM")

            groupedData.forEachIndexed { idx, (dateStr, vals) ->
                incomeEntries.add(BarEntry(idx.toFloat(), vals.first))
                expenseEntries.add(BarEntry(idx.toFloat(), vals.second))
                val lbl = try { LocalDate.parse(dateStr).format(formatter) } catch (e: Exception) { dateStr }
                labels.add(lbl)
            }

            val ds1 = BarDataSet(incomeEntries, "Pemasukan").apply { color = android.graphics.Color.parseColor("#4CAF50"); valueTextSize = 8f; setDrawValues(false) }
            val ds2 = BarDataSet(expenseEntries, "Pengeluaran").apply { color = android.graphics.Color.parseColor("#EF5350"); valueTextSize = 8f; setDrawValues(false) }

            val data = BarData(ds1, ds2)
            data.barWidth = 0.3f
            chart.data = data
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = 0f + chart.barData.getGroupWidth(0.2f, 0.1f) * labels.size
            chart.groupBars(0f, 0.2f, 0.1f)
            chart.invalidate()
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpDialogTheme(branchName: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amountStr by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Top Up Kas Kecil", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Ke: $branchName", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountStr, onValueChange = { if (it.all { c -> c.isDigit() }) amountStr = it },
                    label = { Text("Nominal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(onClick = { val amt = amountStr.toDoubleOrNull() ?: 0.0; if (amt > 0) onConfirm(amt) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Kirim") }
                }
            }
        }
    }
}