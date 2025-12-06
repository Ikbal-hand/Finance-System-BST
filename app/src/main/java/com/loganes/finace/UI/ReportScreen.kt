package com.loganes.finace.ui.theme

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loganes.finace.UI.PdfHelper

import com.loganes.finace.viewmodel.TransactionViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    var startDate by remember { mutableStateOf("$year-${month + 1}-01") }
    var endDate by remember { mutableStateOf("$year-${month + 1}-$day") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Keuangan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueStart, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(GrayBackground)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pilih Periode", fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Tanggal 1
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d -> startDate = "$y-${m + 1}-$d" }, year, month, 1).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = BlueStart)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dari: $startDate", color = TextDark)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Tanggal 2
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d -> endDate = "$y-${m + 1}-$d" }, year, month, day).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = BlueStart)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sampai: $endDate", color = TextDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val filteredData = allTransactions.filter { it.date >= convertDate(startDate) && it.date <= convertDate(endDate) }
                    if (filteredData.isNotEmpty()) {
                        PdfHelper.generateAndSendPdf(context, filteredData, startDate, endDate)
                    } else {
                        Toast.makeText(context, "Data kosong pada tanggal ini.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueStart)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UNDUH LAPORAN PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}
fun convertDate(date: String): String {
    val parts = date.split("-")
    if (parts.size != 3) return date // Jaga-jaga jika format salah

    val y = parts[0]
    val m = parts[1].padStart(2, '0') // Tambah 0 di depan jika 1 digit (misal: 5 -> 05)
    val d = parts[2].padStart(2, '0') // Tambah 0 di depan jika 1 digit

    return "$y-$m-$d"
}