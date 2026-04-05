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
    // Hier laden wir die Daten direkt via Flow oder State
    val detailData by produceState<ExpenseDetailData?>(initialValue = null) {
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
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                // 1. Header: Gesamtbetrag
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gesamtbetrag", style = MaterialTheme.typography.labelMedium)
                        Text("${data.expense.amount / 100.0} €", style = MaterialTheme.typography.headlineMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Dein Anteil (Nur für dich relevant)
                val mySplit = data.splits.find { it.debtor_key == myPublicKey }
                if (mySplit != null) {
                    Text("Dein Anteil", fontWeight = FontWeight.Bold)
                    Text("${mySplit.amount / 100.0} €", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4CAF50))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Sektionen für später (Platzhalter)
                Text("Kommentare (${data.comments.size})", fontWeight = FontWeight.Bold)

                // Hier kommt später die LazyColumn für Kommentare hin
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text("Noch keine Kommentare...", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                }

                // 4. Quick-Action Bar für Anhänge/Kommentare
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Kommentar...") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { /* Foto machen */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Anhang")
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}