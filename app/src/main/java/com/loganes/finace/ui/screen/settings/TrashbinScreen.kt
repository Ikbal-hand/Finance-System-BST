package com.loganes.finace.ui.screen.settings

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.TransactionType
import com.loganes.finace.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashbinScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val trashItems by viewModel.trashTransactions.collectAsState()
    val context = LocalContext.current

    // State Dialog
    var itemToRestore by remember { mutableStateOf<Transaction?>(null) }
    var itemToDelete by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchTrashbin()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Sampah (Trash)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        if (trashItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sampah Kosong", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(trashItems) { item ->
                    TrashItemCard(
                        transaction = item,
                        onRestore = { itemToRestore = item },
                        onPermanentDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // DIALOG RESTORE
    if (itemToRestore != null) {
        AlertDialog(
            onDismissRequest = { itemToRestore = null },
            title = { Text("Kembalikan Transaksi?") },
            text = { Text("Data akan dikembalikan ke dashboard aktif.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.restoreTransaction(itemToRestore!!)
                    itemToRestore = null
                    Toast.makeText(context, "Data Dikembalikan", Toast.LENGTH_SHORT).show()
                }) { Text("Ya, Kembalikan") }
            },
            dismissButton = { TextButton(onClick = { itemToRestore = null }) { Text("Batal") } },
            containerColor = Color.White
        )
    }

    // DIALOG HAPUS PERMANEN
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Permanen?") },
            text = { Text("Data akan hilang selamanya dan tidak bisa dikembalikan. Yakin?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentDelete(itemToDelete!!)
                        itemToDelete = null
                        Toast.makeText(context, "Dihapus Selamanya", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus Permanen") }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Batal") } },
            containerColor = Color.White
        )
    }
}

@Composable
fun TrashItemCard(
    transaction: Transaction,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val isIncome = transaction.typeEnum == TransactionType.INCOME
    val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFEF5350) // Hardcode Hijau/Merah
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(transaction.amount)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.LightGray.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${transaction.date} • ${transaction.branch}", fontSize = 11.sp, color = Color.Gray)
                Text(formatRp, fontSize = 13.sp, color = amountColor, fontWeight = FontWeight.SemiBold)
            }

            // Tombol Aksi
            Row {
                // Restore Button (Biru/Hijau)
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary)
                }
                // Delete Button (Merah)
                IconButton(onClick = onPermanentDelete) {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}