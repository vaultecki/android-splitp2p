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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.flow.map


sealed class Transaction {
    abstract val id: String
    abstract val timestamp: Long
    abstract val amount: Long

    data class ExpenseItem(
        val expense: Expense,
        val authorName: String // Der aufgelöste Name
    ) : Transaction() {
        override val id: String = expense.id
        override val timestamp: Long = expense.timestamp
        override val amount: Long = expense.amount
    }

    data class SettlementItem(
        val settlement: Settlement,
        val fromName: String, // Name statt Key
        val toName: String    // Name statt Key
    ) : Transaction() {
        override val id: String = settlement.id
        override val timestamp: Long = settlement.timestamp
        override val amount: Long = settlement.amount
    }
}

class ExpenseOverviewModel(private val groupDao: GroupDao, private val groupId: String) : ViewModel() {

    val groupInfo = groupDao.getGroupByIdFlow(groupId)

    // Flow der User-Map: PublicKey -> Name
    private val userMapFlow = groupDao.getUsersForGroupFlow(groupId).map { users ->
        users.associate { it.public_key to it.name }
    }

    val allTransactions: Flow<List<Transaction>> = combine(
        groupDao.getExpensesForGroup(groupId),
        groupDao.getSettlementsForGroup(groupId),
        userMapFlow
    ) { expenses, settlements, userMap ->
        val items = mutableListOf<Transaction>()

        items.addAll(expenses.map { e ->
            Transaction.ExpenseItem(
                expense = e,
                authorName = userMap[e.author_pubkey] ?: e.author_pubkey.take(6)
            )
        })

        items.addAll(settlements.map { s ->
            Transaction.SettlementItem(
                settlement = s,
                fromName = userMap[s.from_key] ?: s.from_key.take(6),
                toName = userMap[s.to_key] ?: s.to_key.take(6)
            )
        })

        items.sortByDescending { it.timestamp }
        items
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseOverviewScreen(
    viewModel: ExpenseOverviewModel,
    onBack: () -> Unit,
    onAddExpense: () -> Unit
) {
    val transactions: List<Transaction> by viewModel.allTransactions.collectAsState(initial = emptyList<Transaction>())
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
                    is Transaction.ExpenseItem -> ExpenseRow(transaction)
                    is Transaction.SettlementItem -> SettlementRow(transaction)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ExpenseRow(item: Transaction.ExpenseItem) {
    ListItem(
        headlineContent = { Text(item.expense.description ?: "Unbekannte Ausgabe") },
        supportingContent = { Text("Bezahlt von ${item.authorName}") },
        trailingContent = {
            Text(
                text = "${item.amount / 100.0} €",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@Composable
fun SettlementRow(item: Transaction.SettlementItem) {
    ListItem(
        modifier = Modifier.background(Color(0xFFF1F8E9)),
        headlineContent = { Text("Abrechnung", fontStyle = FontStyle.Italic) },
        supportingContent = {
            Text("${item.fromName} → ${item.toName}")
        },
        trailingContent = {
            Text(
                text = "${item.amount / 100.0} €",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
        }
    )
}
