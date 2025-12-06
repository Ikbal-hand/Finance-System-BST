package com.loganes.finace.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loganes.finace.model.Transaction
import com.loganes.finace.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Locale

// ... imports ...
import androidx.compose.foundation.background // Tambah ini
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashbinScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val trashItems by viewModel.trashTransactions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sampah", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueStart, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        // Background Abu
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(GrayBackground)) {
            if (trashItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sampah kosong.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Ketuk ikon pulihkan untuk mengembalikan data.",
                            fontSize = 12.sp,
                            color = TextLight,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(trashItems) { item ->
                        TrashItemModern(item = item, onRestore = { viewModel.restoreTransaction(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun TrashItemModern(item: Transaction, onRestore: () -> Unit) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.amount)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.category, fontWeight = FontWeight.SemiBold, color = TextDark)
                Text("${item.branch.name} • ${item.date}", fontSize = 12.sp, color = TextLight)
                Text("Dihapus", fontSize = 10.sp, color = RedExpense)
            }
            Text(formatRp, fontWeight = FontWeight.Bold, color = TextLight)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = BlueStart)
            }
        }
    }
}