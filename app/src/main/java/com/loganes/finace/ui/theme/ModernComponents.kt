package com.loganes.finace.ui.theme

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.loganes.finace.model.Transaction
import com.loganes.finace.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

// 1. KARTU SALDO GRADASI (LENGKAP DENGAN KAS UTAMA)
@Composable
fun GradientBalanceCard(balance: Double, kasUtama: Double, kasKecilTotal: Double) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(BlueStart, BlueEnd)))
                .padding(20.dp)
        ) {
            Column {
                Text("Total Aset (Semua Cabang)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(formatRp.format(balance), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kas Utama (Pusat)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(formatRp.format(kasUtama), color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Kas Kecil", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(formatRp.format(kasKecilTotal), color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 2. BAR CHART (GRAFIK BATANG PER TANGGAL) - FIX DATA TYPE
@Composable
fun DailyBarChart(transactions: List<Transaction>) {
    // Group data by Date (YYYY-MM-DD)
    val groupedData = transactions
        .groupBy { it.date }
        .toSortedMap()
        .entries
        .toList() // PENTING: Ubah ke List agar bisa pakai takeLast dengan aman
        .takeLast(7) // Ambil 7 hari terakhir aktif

    val incomeEntries = ArrayList<BarEntry>()
    val expenseEntries = ArrayList<BarEntry>()
    val labels = ArrayList<String>()

    groupedData.forEachIndexed { index, entry ->
        val income = entry.value.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }.toFloat()
        val expense = entry.value.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }.toFloat()

        incomeEntries.add(BarEntry(index.toFloat(), income))
        expenseEntries.add(BarEntry(index.toFloat(), expense))

        // Format label tanggal (ambil hari/tgl saja, misal: "05-12")
        val dateLabel = try { entry.key.substring(5) } catch(e:Exception) { entry.key }
        labels.add(dateLabel)
    }

    if (labels.isEmpty()) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Belum ada data grafik", color = Color.Gray)
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setDrawGridBackground(false)

                // Axis X (Tanggal)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f

                axisRight.isEnabled = false
                animateY(1000)
            }
        },
        update = { chart ->
            // Update Labels
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            // Dataset
            val set1 = BarDataSet(incomeEntries, "Masuk").apply {
                color = android.graphics.Color.parseColor("#00C853")
                valueTextSize = 10f
            }
            val set2 = BarDataSet(expenseEntries, "Keluar").apply {
                color = android.graphics.Color.parseColor("#FF3D00")
                valueTextSize = 10f
            }

            val barData = BarData(set1, set2)
            barData.barWidth = 0.3f // Ukuran batang

            chart.data = barData

            // Agar batang bersebelahan (Grouping logic)
            // Rumus: (barWidth + barSpace) * 2 + groupSpace = 1.00
            // (0.3 + 0.0) * 2 + 0.4 = 1.0
            chart.groupBars(-0.5f, 0.4f, 0.0f)

            // Atur batas X agar pas
            chart.xAxis.axisMinimum = -0.5f
            chart.xAxis.axisMaximum = labels.size.toFloat() - 0.5f

            chart.invalidate()
        }
    )
}

// 3. DONUT CHART (PERSENTASE PENGELUARAN)
// Wajib ada karena dipanggil di DashboardScreen
@Composable
fun ExpenseDonutChart(transactions: List<Transaction>) {
    // Kelompokkan pengeluaran berdasarkan Kategori
    val expenseMap = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

    val entries = expenseMap.map { PieEntry(it.value, it.key) }

    if (entries.isEmpty()) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Belum ada data pengeluaran", color = Color.Gray)
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                legend.textSize = 10f
                legend.isWordWrapEnabled = true

                isDrawHoleEnabled = true
                holeRadius = 50f
                setHoleColor(android.graphics.Color.WHITE)
                setTransparentCircleAlpha(0)

                setCenterText("Pengeluaran")
                setCenterTextSize(12f)

                animateY(1400)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "").apply {
                colors = ChartColors.map { it.hashCode() }
                sliceSpace = 2f
                valueTextColor = android.graphics.Color.WHITE
                valueTextSize = 11f
            }

            val data = PieData(dataSet)
            chart.data = data
            chart.invalidate()
        }
    )
}