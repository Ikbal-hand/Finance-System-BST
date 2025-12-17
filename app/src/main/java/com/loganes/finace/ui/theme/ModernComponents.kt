package com.loganes.finace.ui.theme

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
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

// 1. KARTU SALDO GRADASI
@Composable
fun GradientCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BlueStart, BlueEnd) // Menggunakan warna dari Color.kt
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Format Rupiah
                val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
                Text(
                    text = formatRp,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 2. PIE CHART PENGELUARAN (Diperbarui untuk Firebase)
@Composable
fun ExpensePieChart(transactions: List<Transaction>) {

    // LOGIC BARU: Filter menggunakan String (.name)
    val expenseMap = transactions
        .filter { it.type == TransactionType.EXPENSE.name } // <-- Perbaikan disini
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

    val entries = expenseMap.map { PieEntry(it.value, it.key) }

    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada data pengeluaran", color = Color.Gray)
        }
        return
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
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
                // Mengambil warna dari ChartColors di Color.kt
                colors = ChartColors.map { it.hashCode() } // Convert Compose Color ke Android Color Int
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