package com.loganes.finace.ui.theme

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Send // Ganti icon Download jadi Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loganes.finace.UI.PdfHelper
import com.loganes.finace.viewmodel.TransactionViewModel
import java.util.Calendar

// Warna Background Modern
private val ScreenBackground = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    // Setup Calendar
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    var startDate by remember { mutableStateOf("$year-${month + 1}-01") }
    var endDate by remember { mutableStateOf("$year-${month + 1}-$day") }

    // Style Input Modern
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        cursorColor = BlueStart,
        focusedBorderColor = BlueStart,
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedLabelColor = BlueStart,
        unfocusedLabelColor = Color.Gray,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedLeadingIconColor = BlueStart,
        unfocusedLeadingIconColor = Color.Gray,
        disabledContainerColor = Color.White,
        disabledTextColor = TextDark,
        disabledBorderColor = Color(0xFFE0E0E0),
        disabledLabelColor = Color.Gray,
        disabledLeadingIconColor = BlueStart
    )

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Laporan Keuangan", fontWeight = FontWeight.Bold, color = Color.White) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- HEADER ---
            Column {
                Text(
                    text = "Filter Periode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Pilih rentang tanggal untuk generate laporan PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // --- KARTU INPUT TANGGAL ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Input Tanggal Mulai
                    Box(modifier = Modifier.clickable {
                        DatePickerDialog(context, { _, y, m, d ->
                            startDate = "$y-${m + 1}-$d"
                        }, year, month, 1).show()
                    }) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = {},
                            label = { Text("Dari Tanggal") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = inputColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Input Tanggal Sampai
                    Box(modifier = Modifier.clickable {
                        DatePickerDialog(context, { _, y, m, d ->
                            endDate = "$y-${m + 1}-$d"
                        }, year, month, day).show()
                    }) {
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = {},
                            label = { Text("Sampai Tanggal") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = inputColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- TOMBOL KIRIM LAPORAN ---
            Button(
                onClick = {
                    val start = convertDate(startDate)
                    val end = convertDate(endDate)

                    val filteredData = allTransactions.filter {
                        it.date >= start && it.date <= end
                    }

                    if (filteredData.isNotEmpty()) {
                        try {
                            // Fungsi ini akan generate PDF lalu otomatis memunculkan Intent Share (WA/Email)
                            PdfHelper.generateAndSendPdf(context, filteredData, startDate, endDate)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Tidak ada transaksi di periode ini.", Toast.LENGTH_LONG).show()
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
                Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp)) // Icon Send
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "KIRIM LAPORAN (PDF)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// Helper Date Converter
fun convertDate(date: String): String {
    val parts = date.split("-")
    if (parts.size != 3) return date

    val y = parts[0]
    val m = parts[1].padStart(2, '0')
    val d = parts[2].padStart(2, '0')

    return "$y-$m-$d"
}