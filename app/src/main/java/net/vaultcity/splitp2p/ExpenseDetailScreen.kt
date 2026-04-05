package net.vaultcity.splitp2p

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    myPublicKey: String,
    groupDao: GroupDao,
    onBack: () -> Unit
) {
    val detailData by produceState<ExpenseDetailData?>(initialValue = null, expenseId) {
        value = groupDao.getExpenseWithSplitsAndComments(expenseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ausgabe hinzufügen", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF4CAF50)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onBack()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Ausgabe hinzufügen", tint = Color.White)
            }
        }
    ) { padding ->
        detailData?.let { data ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Info-Karte
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gesamtbetrag", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${data.expense.amount / 100.0} €",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Erstellt am: ${formatDate(data.expense.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dein Anteil
                val mySplit = data.splits.find { it.debtor_key == myPublicKey }

                Text("Dein Anteil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mySplit != null) "${mySplit.amount / 100.0} €" else "Kein Anteil",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        if (data.expense.author_pubkey == myPublicKey) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Du bist Ersteller") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollbarer Bereich für Kommentare
                Text("Kommentare", style = MaterialTheme.typography.titleSmall)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        "Noch keine Kommentare vorhanden.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

// Einfache Hilfsfunktion für das Datum
fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
