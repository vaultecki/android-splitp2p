package net.vaultcity.splitp2p

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
    // Daten laden
    val detailData by produceState<ExpenseDetailData?>(initialValue = null, expenseId) {
        value = groupDao.getExpenseWithSplitsAndComments(expenseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detailData?.expense?.description ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        detailData?.let { data ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // 1. Karte mit dem Gesamtbetrag
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gesamtbetrag der Ausgabe", style = MaterialTheme.typography.labelMedium)
                        Text("${data.expense.amount / 100.0} €", style = MaterialTheme.typography.headlineMedium)
                        Text("Erstellt am: ${formatDate(data.expense.timestamp)}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Dein persönlicher Anteil
                val mySplit = data.splits.find { it.debtor_key == myPublicKey }

                Text("Dein Anteil", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mySplit != null) "${mySplit.amount / 100.0} €" else "0.00 €",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        if (data.expense.author_pubkey == myPublicKey) {
                            SuggestionChip(onClick = {}, label = { Text("Du bist Ersteller") })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Platzhalter für Kommentare
                Text("Kommentare", fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        "Kommentare und Anhänge folgen bald...",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 4. Eingabezeile (Dummy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Kommentar schreiben...") },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    FloatingActionButton(
                        onClick = { /* Später: Comment speichern */ },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Senden")
                    }
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
