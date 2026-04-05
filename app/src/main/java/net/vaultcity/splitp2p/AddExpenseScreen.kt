package net.vaultcity.splitp2p

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

class AddExpenseViewModel(
    private val groupDao: GroupDao,
    private val groupId: String,
    private val myPublicKey: String
) : ViewModel() {

    private var currentExpenseLamport: Long = 0
    private var splitLamport: Long = 0

    private val _memberSplits = MutableStateFlow<List<MemberSplitState>>(emptyList())
    val memberSplits: StateFlow<List<MemberSplitState>> = _memberSplits.asStateFlow()

    init {
        viewModelScope.launch {
            currentExpenseLamport = groupDao.getMaxLamportExpenses(groupId) ?: 0L
            splitLamport = groupDao.getMaxLamportSplits(groupId) ?: 0L
            loadMembers()
        }
    }

    private suspend fun loadMembers() {
        val users = groupDao.getUsersInGroup(groupId)
        _memberSplits.value = users.map {
            MemberSplitState(publicKey = it.public_key, name = it.name)
        }
    }

    fun toggleMember(index: Int) {
        val newList = _memberSplits.value.toMutableList()
        val member = newList[index]
        newList[index] = member.copy(isSelected = !member.isSelected)
        _memberSplits.value = newList
        recalculateSplits(0) // 0 übergeben, um gleichmäßige Verteilung zu triggern
    }

    fun updateManualAmount(index: Int, amount: Long) {
        val newList = _memberSplits.value.toMutableList()
        newList[index] = newList[index].copy(amountInCents = amount, amountText = (amount / 100.0).toString())
        _memberSplits.value = newList
        // Hier könnte man noch eine Logik einbauen, die prüft, ob die Summe noch passt
    }

    fun recalculateSplits(totalAmount: Long) {
        val selectedMembers = _memberSplits.value.filter { it.isSelected }
        if (selectedMembers.isEmpty() || totalAmount <= 0L) return

        val share = totalAmount / selectedMembers.size
        val newList = _memberSplits.value.map { member ->
            if (member.isSelected) {
                member.copy(amountInCents = share, amountText = (share / 100.0).toString())
            } else {
                member.copy(amountInCents = 0, amountText = "0")
            }
        }
        _memberSplits.value = newList
    }

    fun saveExpense(description: String, amountInCents: Long) {
        viewModelScope.launch {
            val keyAlias = "SplitP2PUser"
            val now = System.currentTimeMillis()

            // 1. Das Objekt erstellen (ohne Signatur)
            currentExpenseLamport++
            val expenseId = UUID.randomUUID().toString()

            val expense = Expense(
                id = expenseId,
                group_id = groupId,
                timestamp = now,
                expense_date = now,
                lamport_clock = currentExpenseLamport,
                author_pubkey = myPublicKey,
                is_deleted = 0,
                amount = amountInCents,
                description = description,
                category = "General",
                original_amount = amountInCents,
                original_currency = "EUR",
                signature = "" // Platzhalter
            )

            // 2. Signieren (Deterministisches JSON erzeugen)
            // Hier nutzt du eine Hilfsfunktion ähnlich wie createSignatureJson
            val jsonToSign = createExpenseSignatureJson(expense)
            val signature = signJsonWithKeystore("SplitP2PUser", jsonToSign)

            // 3. Mit Signatur speichern
            groupDao.insertExpense(expense.copy(signature = signature))

            // 2. Splits
            _memberSplits.value.filter { it.isSelected && it.amountInCents > 0 }.forEach { member ->
                splitLamport++
                val splitId = UUID.randomUUID().toString()
                val splitData = Split(
                    id = splitId,
                    belongs_to = expenseId,
                    timestamp = now,
                    lamport_clock = splitLamport,
                    author_pubkey = myPublicKey,
                    payer_key = myPublicKey,
                    debtor_key = member.publicKey,
                    amount = member.amountInCents,
                    signature = ""
                )

                val splitJson = createSplitSignatureJson(splitData)

                val splitSig = signJsonWithKeystore(keyAlias, splitJson)

                groupDao.insertSplit(splitData.copy(signature = splitSig))
            }
        }
    }
}

fun createSplitSignatureJson(splitData: Split): String {
    val splitJson = buildJsonObject {
        put("id", splitData.id)
        put("belongs_to", splitData.belongs_to)
        put("payer_key", splitData.payer_key)
        put("debtor_key", splitData.debtor_key)
        put("amount", splitData.amount)
        put("author_pubkey", splitData.author_pubkey)
        put("lamport_clock", splitData.lamport_clock)
        put("timestamp", splitData.timestamp)
    }
    return splitJson.toString()
}

// Funktion um das deterministische JSON zu erzeugen
fun createExpenseSignatureJson(expense: Expense): String {
    val jsonObject = buildJsonObject {
        // Wir nutzen die expliziten Typ-Methoden, um die Inferenz-Fehler zu umgehen
        put("id", expense.id as String)
        put("group_id", expense.group_id as String)
        put("timestamp", expense.timestamp as Number)
        put("expense_date", expense.expense_date as Number)
        put("lamport_clock", expense.lamport_clock as Number)
        put("author_pubkey", expense.author_pubkey as String)
        put("amount", expense.amount as Number)
        put("is_deleted", expense.is_deleted as Number)
        put("description", expense.description as String)
        put("category", expense.category as String)
        put("original_amount", expense.original_amount as Number)
        put("original_currency", expense.original_currency as String)
    }

    // Für JsonElements (wie JsonObject) ist .toString() der korrekte Weg,
    // um ein kompaktes JSON ohne Leerzeichen zu erhalten.
    return jsonObject.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: AddExpenseViewModel, // Hier das ViewModel hinzufügen!
    onBack: () -> Unit,
    onSave: (description: String, amountInCents: Long) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val memberSplits by viewModel.memberSplits.collectAsState()

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
                // Sicherstellen, dass wir Cents an das ViewModel schicken
                val total = (amountText.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100
                viewModel.saveExpense(description, total.toLong())
                onBack()
            }) {
                Icon(Icons.Default.Check, contentDescription = "Speichern")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Was hast du bezahlt?") },
                placeholder = { Text("z.B. Pizza, Miete, Einkauf") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    // Erlaube nur Zahlen und ein Trennzeichen
                    if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                        amountText = input
                    }
                },
                label = { Text("Betrag") },
                suffix = { Text("€") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Verteilung:", fontWeight = FontWeight.Bold)

            LazyColumn {
                itemsIndexed(memberSplits) { index, member ->
                    MemberSplitRow(
                        member = member,
                        totalAmount = (amountText.toDoubleOrNull() ?: 0.0).toLong() * 100,
                        onToggle = { viewModel.toggleMember(index) },
                        onAmountChange = { viewModel.updateManualAmount(index, it) }
                    )
                }
            }
        }
    }
}

/**
 * Hilfsfunktion, um Strings wie "12,50" sicher in 1250 (Long) zu wandeln.
 */
fun parseAmountToCents(input: String): Long {
    return try {
        val sanitized = input.replace(',', '.')
        val doubleValue = sanitized.toDouble()
        (doubleValue * 100).toLong()
    } catch (e: Exception) {
        0L
    }
}

@Composable
fun MemberSplitRow(
    member: MemberSplitState,
    totalAmount: Long,
    onToggle: () -> Unit,
    onAmountChange: (Long) -> Unit // Neu hinzufügen
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = member.isSelected, onCheckedChange = { onToggle() })

        Text(member.name, modifier = Modifier.weight(1f), maxLines = 1)

        Slider(
            value = member.amountInCents.toFloat(), // Nutzt jetzt den Cent-Wert für den Slider
            onValueChange = { onAmountChange(it.toLong()) },
            valueRange = 0f..(if (totalAmount > 0) totalAmount.toFloat() else 1f),
            modifier = Modifier.weight(1.5f).padding(horizontal = 8.dp),
            enabled = member.isSelected
        )

        OutlinedTextField(
            value = member.amountText,
            onValueChange = { input ->
                val cents = parseAmountToCents(input)
                onAmountChange(cents)
            },
            modifier = Modifier.width(90.dp),
            enabled = member.isSelected,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

data class MemberSplitState(
    val publicKey: String,
    val name: String,
    val isSelected: Boolean = false,
    val amountInCents: Long = 0,
    val percentage: Float = 0f,
    val amountText: String = "0.00" // Neu für flüssiges Tippen
)


