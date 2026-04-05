package net.vaultcity.splitp2p

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import androidx.compose.foundation.lazy.items


sealed class Transaction {
    abstract val id: String
    abstract val timestamp: Long
    abstract val amount: Long

    data class ExpenseItem(val expense: Expense) : Transaction() {
        override val id = expense.id
        override val timestamp = expense.timestamp
        override val amount = expense.amount
    }

    data class SettlementItem(val settlement: Settlement) : Transaction() {
        override val id = settlement.id
        override val timestamp = settlement.timestamp
        override val amount = settlement.amount
    }
}

class ExpenseOverviewModel(
    private val groupDao: GroupDao,
    private val groupId: String
) : ViewModel() {

    val transactions: Flow<List<Transaction>> = combine(
        groupDao.getExpensesForGroup(groupId),
        groupDao.getSettlementsForGroup(groupId)
    ) { expenses, settlements ->
        val items = expenses.map { Transaction.ExpenseItem(it) } +
                settlements.map { Transaction.SettlementItem(it) }
        items.sortedByDescending { it.timestamp }
    }

    val groupInfo = groupDao.getGroupByIdFlow(groupId) // Brauchst du für den Header
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseOverviewScreen(
    viewModel: ExpenseOverviewModel,
    onBack: () -> Unit,
    onAddExpense: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val group by viewModel.groupInfo.collectAsState(initial = null)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(group?.name ?: "Gruppe", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense, containerColor = Color(0xFF4CAF50)) {
                Icon(Icons.Default.Add, contentDescription = "Ausgabe hinzufügen", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(transactions) { transaction ->
                when (transaction) {
                    is Transaction.ExpenseItem -> ExpenseRow(transaction.expense)
                    is Transaction.SettlementItem -> SettlementRow(transaction.settlement)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense) {
    ListItem(
        headlineContent = { Text(expense.description ?: "Unbekannte Ausgabe") },
        supportingContent = { Text("${expense.author_pubkey.take(6)}...") }, // Später Name des Users laden
        trailingContent = {
            Text(
                text = "${expense.amount / 100.0} €", // Annahme: Cents in Long
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@Composable
fun SettlementRow(settlement: Settlement) {
    ListItem(
        modifier = Modifier.background(Color(0xFFF1F8E9)),
        headlineContent = { Text("Abrechnung", fontStyle = FontStyle.Italic) },
        supportingContent = { Text("Von ... an ...") },
        trailingContent = {
            Text(
                text = "${settlement.amount / 100.0} €",
                color = Color(0xFF2E7D32)
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50) // <--- Hier 'tint' statt 'contentColor'
            )
        }
    )
}