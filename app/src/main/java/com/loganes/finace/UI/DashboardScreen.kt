package com.loganes.finace.UI

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.loganes.finace.viewmodel.TransactionViewModel
import com.loganes.finace.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
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
    // 1. AMBIL DATA REALTIME
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    // 2. HITUNG KEUANGAN (LOGIKA BARU: Kas Kecil vs Kas Utama)

    // Total Kekayaan
    val companyRealAssets = FinanceCalculator.calculateTotalRealAssets(allTransactions)

    // Kas Kecil
    val kasKecilBox = FinanceCalculator.calculatePettyCash(allTransactions, BranchType.BOX_FACTORY)
    val kasKecilMaint = FinanceCalculator.calculatePettyCash(allTransactions, BranchType.MAINTENANCE_ALFA)
    val kasKecilSaufa = FinanceCalculator.calculatePettyCash(allTransactions, BranchType.SAUFA_OLSHOP)

    val totalKasKecilDiCabang = kasKecilBox + kasKecilMaint + kasKecilSaufa

    // Kas Utama
    val kasUtama = FinanceCalculator.calculateMainCash(allTransactions)


    // --- UI STATE ---
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItemIndex by remember { mutableIntStateOf(-1) }
    val branches = listOf("Box Factory", "Maint. Alfa", "Saufa Olshop")

    // --- NAVIGATION DRAWER ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                // HEADER SIDEBAR
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(BlueStart, BlueEnd)
                            )
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = Modifier.padding(start = 24.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            modifier = Modifier.size(54.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = BlueStart, modifier = Modifier.size(30.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Admin Keuangan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("CV BST Finance", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Menu Utama", modifier = Modifier.padding(start = 28.dp, bottom = 12.dp), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                NavigationDrawerItem(
                    label = { Text("Laporan Keuangan", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Default.Description, null) },
                    selected = selectedItemIndex == 0,
                    onClick = { selectedItemIndex = 0; scope.launch { drawerState.close() }; onReportClick() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(50),
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = BlueStart.copy(alpha = 0.1f), selectedIconColor = BlueStart, selectedTextColor = BlueStart)
                )

                NavigationDrawerItem(
                    label = { Text("Data Pegawai", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Default.Person, null) },
                    selected = selectedItemIndex == 1,
                    onClick = { selectedItemIndex = 1; scope.launch { drawerState.close() }; onPayrollClick() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(50),
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = BlueStart.copy(alpha = 0.1f), selectedIconColor = BlueStart, selectedTextColor = BlueStart)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                Text("Sistem", modifier = Modifier.padding(start = 28.dp, bottom = 12.dp), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                NavigationDrawerItem(
                    label = { Text("Trashbin", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Default.Delete, null) },
                    selected = selectedItemIndex == 2,
                    onClick = { selectedItemIndex = 2; scope.launch { drawerState.close() }; onTrashClick() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(50),
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = RedExpense.copy(alpha = 0.1f), selectedIconColor = RedExpense, selectedTextColor = RedExpense)
                )

                NavigationDrawerItem(
                    label = { Text("Pengaturan", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Default.Settings, null) },
                    selected = selectedItemIndex == 3,
                    onClick = { selectedItemIndex = 3; scope.launch { drawerState.close() }; onSettingsClick() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(50),
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color.Gray.copy(alpha = 0.1f), selectedIconColor = Color.DarkGray, selectedTextColor = Color.DarkGray)
                )
            }
        }
    ) {
        // --- SCAFFOLD UTAMA ---
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CV BST Keuangan", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BlueStart,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = BlueEnd,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(GrayBackground)
                    .padding(16.dp)
            ) {
                // KARTU SALDO
                GradientBalanceCard(
                    balance = companyRealAssets,
                    kasUtama = kasUtama,
                    kasKecilTotal = totalKasKecilDiCabang
                )

                Spacer(modifier = Modifier.height(24.dp))

                // TAB CABANG
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BlueStart,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = BlueStart)
                    },
                    divider = {}
                ) {
                    branches.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            selectedContentColor = BlueStart,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // KONTEN PER CABANG
                BranchContent(
                    branchIndex = selectedTab,
                    transactions = allTransactions,
                    onDelete = { viewModel.moveToTrash(it) },
                    onEdit = { viewModel.transactionToEdit = it; onEditTransaction() }
                )
            }
        }
    }
}

// --- BRANCH CONTENT ---
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

    // Hitung Sisa Kas Kecil (Logic Baru)
    val currentKasKecilBranch = FinanceCalculator.calculatePettyCash(transactions, selectedBranchType)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // Status Kas Kecil
        PettyCashStatus(amount = currentKasKecilBranch)

        Spacer(modifier = Modifier.height(24.dp))

        // Grafik Harian
        Text("Grafik Harian", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DailyBarChart(transactions = branchTransactions)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Riwayat Transaksi
        Text("Riwayat Transaksi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))

        if (branchTransactions.isEmpty()) {
            Text("Belum ada transaksi.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
        } else {
            val sortedList = branchTransactions.sortedByDescending { it.date }
            sortedList.take(15).forEach { trans ->
                TransactionItem(transaction = trans, onDeleteClick = { onDelete(trans) }, onEditClick = { onEdit(trans) })
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- KOMPONEN GRAFIK (GROUPED BAR CHART) ---
@Composable
fun DailyBarChart(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Belum ada data grafik", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

                // Konfigurasi X Axis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                xAxis.isGranularityEnabled = true
                xAxis.setCenterAxisLabels(true) // PENTING: Untuk Grouped Chart
                xAxis.textColor = android.graphics.Color.DKGRAY
                xAxis.textSize = 10f

                // Konfigurasi Y Axis
                axisLeft.textColor = android.graphics.Color.GRAY
                axisLeft.setDrawGridLines(true)
                axisLeft.textSize = 10f
                axisLeft.axisMinimum = 0f

                axisRight.isEnabled = false

                setFitBars(true)
                setTouchEnabled(false)
                animateY(1000)
            }
        },
        update = { chart ->
            // Group Data: Date -> Pair(Income, Expense)
            val groupedData = transactions
                .groupBy { it.date }
                .mapValues { entry ->
                    val income = entry.value.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val expense = entry.value.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    Pair(income.toFloat(), expense.toFloat())
                }
                .toList()
                .sortedBy { it.first }
                .takeLast(5) // Ambil 5 hari terakhir agar tidak sempit

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
                } catch (e: Exception) {
                    date
                }
                labels.add(label)
            }

            if (incomeEntries.isNotEmpty()) {
                // Konfigurasi Grouping
                val groupSpace = 0.08f
                val barSpace = 0.03f
                val barWidth = 0.43f
                // (0.43 + 0.03) * 2 + 0.08 = 1.00 -> Pas 1 unit X Axis

                val incomeDataSet = BarDataSet(incomeEntries, "Pemasukan")
                incomeDataSet.color = android.graphics.Color.parseColor("#43A047") // Hijau
                incomeDataSet.valueTextSize = 9f

                val expenseDataSet = BarDataSet(expenseEntries, "Pengeluaran")
                expenseDataSet.color = android.graphics.Color.parseColor("#E53935") // Merah
                expenseDataSet.valueTextSize = 9f

                // Formatter Angka
                val valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        if (value == 0f) return ""
                        return if (value >= 1000000 || value <= -1000000) {
                            String.format("%.1fM", value / 1000000)
                        } else {
                            NumberFormat.getIntegerInstance(Locale("id", "ID")).format(value.toLong())
                        }
                    }
                }
                incomeDataSet.valueFormatter = valueFormatter
                expenseDataSet.valueFormatter = valueFormatter

                val data = BarData(incomeDataSet, expenseDataSet)
                data.barWidth = barWidth

                chart.data = data

                // Set Label X Axis
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                // Atur batas axis agar group bar pas di tengah
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = 0f + chart.barData.getGroupWidth(groupSpace, barSpace) * labels.size

                chart.groupBars(0f, groupSpace, barSpace)
                chart.invalidate()
            }
        }
    )
}

// --- KOMPONEN PENDUKUNG UI ---
@Composable
fun GradientBalanceCard(balance: Double, kasUtama: Double, kasKecilTotal: Double) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(brush = Brush.horizontalGradient(colors = listOf(BlueStart, BlueEnd)))
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column {
                Text("Total Aset Perusahaan", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(formatRp.format(balance), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Kas Pusat (Ready)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(formatRp.format(kasUtama), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Kas di Cabang", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(formatRp.format(kasKecilTotal), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PettyCashStatus(amount: Double) {
    val isWarning = amount <= 50000
    val color = if (isWarning) RedExpense else GreenIncome
    val textStatus = if (isWarning) "PERINGATAN: Kas Menipis!" else "Kas Operasional Aman"
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(50)))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Sisa Kas Kecil (Cabang Ini)", fontSize = 12.sp, color = TextLight)
                Text(text = formatRp, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
                if (isWarning) { Text(textStatus, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val color = if (isIncome) GreenIncome else RedExpense
    val prefix = if (isIncome) "+ " else "- "
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(transaction.amount)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 14.sp)
                Text(transaction.date, fontSize = 11.sp, color = TextLight)
                if (transaction.description.isNotEmpty()) { Text(transaction.description, fontSize = 10.sp, color = Color.Gray, maxLines = 1) }
            }
            Text(text = "$prefix$formatRp", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Edit, "Edit", tint = BlueStart, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, "Hapus", tint = Color.LightGray, modifier = Modifier.size(18.dp)) }
        }
    }
}