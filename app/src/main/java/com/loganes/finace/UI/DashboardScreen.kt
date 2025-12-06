package com.loganes.finace.UI

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Transaction
import com.loganes.finace.model.TransactionType
import com.loganes.finace.data.FinanceCalculator
import com.loganes.finace.data.BackupManager // Import BackupManager
import com.loganes.finace.viewmodel.TransactionViewModel
import com.loganes.finace.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

// Warna Background Modern (Off-White)
val ModernBackground = Color(0xFFF8F9FA)
val ModernSurface = Color(0xFFFFFFFF)
val ModernSubText = Color(0xFF8898AA)

// Data Class untuk Item Notifikasi agar lebih fleksibel
data class NotifItem(
    val message: String,
    val color: Color,
    val icon: ImageVector,
    val isSeparator: Boolean = false // Penanda Garis Pemisah
)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    onAddClick: () -> Unit,
    onPayrollClick: () -> Unit,
    onReportClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditTransaction: () -> Unit
) {
    val context = LocalContext.current

    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val allEmployees by viewModel.allEmployees.collectAsState(initial = emptyList())

    // --- HITUNG KEUANGAN ---
    val companyRealAssets = FinanceCalculator.calculateTotalRealAssets(allTransactions)
    val kasKecilBox = FinanceCalculator.calculatePettyCash(allTransactions, BranchType.BOX_FACTORY)
    val kasKecilMaint = FinanceCalculator.calculatePettyCash(allTransactions, BranchType.MAINTENANCE_ALFA)
    val kasKecilSaufa = FinanceCalculator.calculatePettyCash(allTransactions, BranchType.SAUFA_OLSHOP)
    val totalKasKecilDiCabang = kasKecilBox + kasKecilMaint + kasKecilSaufa
    val kasUtama = FinanceCalculator.calculateMainCash(allTransactions)

    // --- LOGIC COUNTDOWN ---
    var timeToBackup by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while(true) {
            val now = LocalDateTime.now()
            val nextBackup = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
            val target = if (now.isAfter(nextBackup)) nextBackup.plusDays(1) else nextBackup

            val duration = Duration.between(now, target)
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60

            timeToBackup = "${hours}j ${minutes}m"
            delay(60000)
        }
    }

    // --- LOGIC NOTIFIKASI ---
    val backupStatusMap = remember { BackupManager(context).getAllBackupStatus() }

    val notifications = remember(kasKecilBox, kasKecilMaint, kasKecilSaufa, allEmployees, backupStatusMap, timeToBackup) {
        val list = mutableListOf<NotifItem>()
        val today = LocalDate.now()

        // 1. COUNTDOWN (PALING ATAS) - Ada 2 entri sesuai request
        list.add(NotifItem("Jadwal Backup Lokal: $timeToBackup lagi", BlueStart, Icons.Default.Update))
        list.add(NotifItem("Jadwal Upload Cloud: $timeToBackup lagi", Color(0xFFEA4335), Icons.Default.CloudQueue))

        // 2. ALERT OPERASIONAL (TENGAH)
        if (kasKecilBox <= 50000) list.add(NotifItem("Kas Box Factory Menipis!", RedExpense, Icons.Default.Warning))
        if (kasKecilMaint <= 50000) list.add(NotifItem("Kas Maint. Alfa Menipis!", RedExpense, Icons.Default.Warning))
        if (kasKecilSaufa <= 50000) list.add(NotifItem("Kas Saufa Olshop Menipis!", RedExpense, Icons.Default.Warning))

        val reportDeadline = today.withDayOfMonth(30.coerceAtMost(today.lengthOfMonth()))
        val daysToReport = ChronoUnit.DAYS.between(today, reportDeadline)
        if (daysToReport in 0..3) {
            val msg = if (daysToReport == 0L) "Deadline Laporan Hari Ini!" else "Laporan Bulanan H-$daysToReport"
            list.add(NotifItem(msg, Color(0xFFFF9800), Icons.Default.Assignment))
        }

        allEmployees.forEach { emp ->
            val isPaidThisMonth = emp.lastPaidDate?.let { dateString ->
                try {
                    val lastPaid = LocalDate.parse(dateString)
                    lastPaid.month == today.month && lastPaid.year == today.year
                } catch (e: Exception) { false }
            } ?: false

            if (!isPaidThisMonth) {
                val payDay = today.withDayOfMonth(emp.payDate.coerceAtMost(today.lengthOfMonth()))
                val daysToPay = ChronoUnit.DAYS.between(today, payDay)
                if (daysToPay in 0..3) {
                    val msg = if (daysToPay == 0L) "Gaji ${emp.name} Hari Ini!" else "Gaji ${emp.name} H-$daysToPay"
                    list.add(NotifItem(msg, Color(0xFFFF9800), Icons.Default.Payments))
                } else if (daysToPay < 0) {
                    list.add(NotifItem("Gaji ${emp.name} TELAT ${-daysToPay} hari!", RedExpense, Icons.Default.Error))
                }
            }
        }

        // 3. SEPARATOR (GARIS PEMISAH)
        list.add(NotifItem("SEPARATOR", Color.Gray, Icons.Default.Minimize, isSeparator = true))

        // 4. LOG STATUS TERAKHIR (PALING BAWAH)
        val locStatus = backupStatusMap["local_status"]
        val locTime = backupStatusMap["local_time"]
        val cloudStatus = backupStatusMap["cloud_status"]
        val cloudTime = backupStatusMap["cloud_time"]

        // Status Lokal
        if (locStatus == "SUCCESS") {
            list.add(NotifItem("Lokal Terakhir: Sukses ($locTime)", GreenIncome, Icons.Default.CheckCircle))
        } else if (locStatus == "FAILED") {
            list.add(NotifItem("Lokal Terakhir: GAGAL ($locTime)", RedExpense, Icons.Default.Error))
        }

        // Status Cloud
        if (cloudStatus == "SUCCESS") {
            list.add(NotifItem("Cloud Terakhir: Sukses ($cloudTime)", GreenIncome, Icons.Default.CloudDone))
        } else if (cloudStatus == "FAILED") {
            list.add(NotifItem("Cloud Terakhir: GAGAL ($cloudTime)", RedExpense, Icons.Default.CloudOff))
        } else {
            // Jika belum pernah atau belum login
            list.add(NotifItem("Cloud Terakhir: Belum diset", Color.Gray, Icons.Default.CloudQueue))
        }

        list
    }

    var showNotifDropdown by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItemIndex by remember { mutableIntStateOf(-1) }
    val branches = listOf("Box Factory", "Maint. Alfa", "Saufa Olshop")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                drawerContainerColor = ModernSurface,
                modifier = Modifier.width(300.dp)
            ) {
                // Header Sidebar dengan Gradasi Biru Ungu
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(colors = listOf(BlueStart, BlueEnd))),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(60.dp), shadowElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) { Text("BST", color = BlueStart, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Bendahara", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("CV. BERKARYA SATU TUJUAN", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Menu Items
                val menuItems = listOf(
                    Triple("Laporan", Icons.Outlined.Description, onReportClick),
                    Triple("Pegawai", Icons.Outlined.People, onPayrollClick),
                    Triple("Sampah", Icons.Outlined.Delete, onTrashClick),
                    Triple("Pengaturan", Icons.Outlined.Settings, onSettingsClick)
                )
                menuItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(item.first, fontWeight = FontWeight.Medium) },
                        icon = { Icon(item.second, null) },
                        selected = selectedItemIndex == index,
                        onClick = { selectedItemIndex = index; scope.launch { drawerState.close() }; item.third() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = BlueStart.copy(alpha = 0.1f), selectedIconColor = BlueStart, selectedTextColor = BlueStart, unselectedIconColor = ModernSubText, unselectedTextColor = TextDark)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("v1.1.1 Stable", modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp), color = ModernSubText, fontSize = 12.sp)
            }
        }
    ) {
        Scaffold(
            containerColor = ModernBackground,
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BlueStart) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ModernBackground),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = TextDark) }
                    },
                    actions = {
                        // --- NOTIFIKASI DROPDOWN ---
                        Box {
                            IconButton(onClick = { showNotifDropdown = true }) {
                                BadgedBox(
                                    badge = {
                                        // Badge hanya muncul jika ada notifikasi selain separator & countdown
                                        val realNotifCount = notifications.count { !it.isSeparator && it.icon != Icons.Default.Update && it.icon != Icons.Default.CloudQueue }
                                        if (realNotifCount > 0) {
                                            val hasUrgent = notifications.any { it.color == RedExpense }
                                            Badge(containerColor = if(hasUrgent) RedExpense else GreenIncome, contentColor = Color.White) {
                                                Text(notifications.size.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Notifications,
                                        null,
                                        tint = if (notifications.isNotEmpty()) BlueStart else ModernSubText
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showNotifDropdown,
                                onDismissRequest = { showNotifDropdown = false },
                                modifier = Modifier
                                    .width(340.dp) // Lebih lebar agar muat teks panjang
                                    .background(ModernSurface, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text("Pusat Notifikasi", style = MaterialTheme.typography.titleMedium, color = TextDark, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                                notifications.forEach { item ->
                                    if (item.isSeparator) {
                                        // RENDER GARIS PEMISAH
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = Color.Gray.copy(alpha = 0.3f),
                                            thickness = 1.dp
                                        )
                                    } else {
                                        // RENDER ITEM NOTIFIKASI
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = item.message,
                                                    color = TextDark,
                                                    fontWeight = if(item.color == RedExpense) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier.size(32.dp).background(item.color.copy(alpha = 0.1f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                showNotifDropdown = false
                                                if (item.message.contains("Gaji")) onPayrollClick()
                                                if (item.message.contains("Laporan")) onReportClick()
                                                if (item.message.contains("Backup")) onSettingsClick()
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddClick, containerColor = BlueStart, contentColor = Color.White, shape = RoundedCornerShape(16.dp), elevation = FloatingActionButtonDefaults.elevation(4.dp)) {
                    Icon(Icons.Default.Add, "Tambah Transaksi")
                }
            }
        ) { padding ->
            // --- BODY DASHBOARD ---
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                GradientBalanceCard(companyRealAssets, kasUtama, totalKasKecilDiCabang)
                Spacer(modifier = Modifier.height(24.dp))
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        Box(Modifier.tabIndicatorOffset(tabPositions[selectedTab]).height(3.dp).padding(horizontal = 10.dp).background(BlueStart, RoundedCornerShape(10.dp)))
                    },
                    divider = {}
                ) {
                    branches.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 14.sp, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal, color = if(selectedTab == index) BlueStart else ModernSubText) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                BranchContent(branchIndex = selectedTab, transactions = allTransactions, onDelete = { viewModel.moveToTrash(it) }, onEdit = { viewModel.transactionToEdit = it; onEditTransaction() })
            }
        }
    }
}

// --- KOMPONEN PENDUKUNG UI ---
// Pastikan fungsi-fungsi komponen di bawah ini TETAP ADA di file Anda (copas dari versi sebelumnya):
// 1. GradientBalanceCard
// 2. BranchContent
// 3. ModernPettyCashAlert
// 4. ModernTransactionItem
// 5. DailyBarChart
@Composable
fun GradientBalanceCard(total: Double, main: Double, petty: Double) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(BlueStart, BlueEnd)
                    )
                )
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Total Aset Bersih", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatRp.format(total),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Kas Pusat", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(formatRp.format(main), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Kas Kecil", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(formatRp.format(petty), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun BranchContent(
    branchIndex: Int,
    transactions: List<Transaction>,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
) {
    val selectedBranchType = when(branchIndex) {
        0 -> BranchType.BOX_FACTORY
        1 -> BranchType.MAINTENANCE_ALFA
        else -> BranchType.SAUFA_OLSHOP
    }

    val branchTransactions = transactions.filter { it.branch == selectedBranchType }
    val currentKasKecilBranch = FinanceCalculator.calculatePettyCash(transactions, selectedBranchType)

    Column {
        ModernPettyCashAlert(amount = currentKasKecilBranch)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Ringkasan Harian", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = ModernSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(20.dp))
        ) {
            DailyBarChart(transactions = branchTransactions)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Transaksi Terkini", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.weight(1f))
            Text("${branchTransactions.size} item", fontSize = 12.sp, color = ModernSubText)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (branchTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(ModernSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada data", color = ModernSubText)
            }
        } else {
            val sortedList = branchTransactions.sortedByDescending { it.date }
            sortedList.take(15).forEach { trans ->
                ModernTransactionItem(transaction = trans, onDeleteClick = { onDelete(trans) }, onEditClick = { onEdit(trans) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ModernPettyCashAlert(amount: Double) {
    val isWarning = amount <= 50000
    val bgColor = if (isWarning) Color(0xFFFFF0F0) else Color(0xFFF0FFF4)
    val contentColor = if (isWarning) RedExpense else GreenIncome
    val icon = if (isWarning) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = contentColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Sisa Kas Kecil", fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
            Text(formatRp, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun ModernTransactionItem(transaction: Transaction, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME

    // WARNA TEKS UANG: Hijau jika Masuk, Merah jika Keluar
    val amountColor = if (isIncome) GreenIncome else RedExpense

    val iconBg = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val iconTint = if (isIncome) GreenIncome else RedExpense
    val icon = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(transaction.amount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ModernSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextDark)
            Text(transaction.date, fontSize = 11.sp, color = ModernSubText)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text((if(isIncome) "+ " else "- ") + formatRp, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = amountColor)
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.Edit, null, tint = ModernSubText, modifier = Modifier.size(16.dp).clickable { onEditClick() })
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Delete, null, tint = ModernSubText, modifier = Modifier.size(16.dp).clickable { onDeleteClick() })
            }
        }
    }
}

@Composable
fun DailyBarChart(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Belum ada data grafik", color = ModernSubText, fontSize = 12.sp)
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(250.dp).padding(8.dp),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                xAxis.isGranularityEnabled = true
                xAxis.setCenterAxisLabels(true)
                xAxis.textColor = android.graphics.Color.GRAY
                xAxis.textSize = 10f
                xAxis.setDrawAxisLine(false)
                axisLeft.textColor = android.graphics.Color.LTGRAY
                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor = android.graphics.Color.parseColor("#F5F5F5")
                axisLeft.textSize = 10f
                axisLeft.axisMinimum = 0f
                axisLeft.setDrawAxisLine(false)
                axisRight.isEnabled = false
                setFitBars(true)
                setTouchEnabled(false)
                animateY(1000)
            }
        },
        update = { chart ->
            val groupedData = transactions.groupBy { it.date }.mapValues { entry ->
                val income = entry.value.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = entry.value.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                Pair(income.toFloat(), expense.toFloat())
            }.toList().sortedBy { it.first }.takeLast(5)

            val incomeEntries = ArrayList<BarEntry>()
            val expenseEntries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()

            groupedData.forEachIndexed { index, (date, values) ->
                incomeEntries.add(BarEntry(index.toFloat(), values.first))
                expenseEntries.add(BarEntry(index.toFloat(), values.second))
                val label = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM", Locale("id", "ID"))
                    val parsed = inputFormat.parse(date)
                    parsed?.let { outputFormat.format(it) } ?: date
                } catch (e: Exception) { date }
                labels.add(label)
            }

            if (incomeEntries.isNotEmpty()) {
                val groupSpace = 0.08f
                val barSpace = 0.03f
                val barWidth = 0.43f
                val incomeDataSet = BarDataSet(incomeEntries, "Pemasukan")
                incomeDataSet.color = android.graphics.Color.parseColor("#4CAF50")
                incomeDataSet.valueTextSize = 9f
                incomeDataSet.setDrawValues(false)
                val expenseDataSet = BarDataSet(expenseEntries, "Pengeluaran")
                expenseDataSet.color = android.graphics.Color.parseColor("#EF5350")
                expenseDataSet.valueTextSize = 9f
                expenseDataSet.setDrawValues(false)
                val data = BarData(incomeDataSet, expenseDataSet)
                data.barWidth = barWidth
                chart.data = data
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = 0f + chart.barData.getGroupWidth(groupSpace, barSpace) * labels.size
                chart.groupBars(0f, groupSpace, barSpace)
                chart.invalidate()
            }
        }
    )
}