package net.vaultcity.splitp2p

import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class AddExpenseViewModel(
    private val groupDao: GroupDao,
    private val groupId: String,
    private val myPublicKey: String
) : ViewModel() {

    fun saveExpense(description: String, amountInCents: Long) {
        viewModelScope.launch {
            val expenseId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            // 1. Das Objekt erstellen (ohne Signatur)
            val expense = Expense(
                id = expenseId,
                group_id = groupId,
                timestamp = timestamp,
                expense_date = timestamp,
                lamport_clock = 0, // Hier müsste dein globaler Lamport-Counter rein
                author_pubkey = myPublicKey,
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

            // 4. Splits für alle Mitglieder erstellen (einfaches Teilen)
            val members = groupDao.getUsersInGroup(groupId)
//            if (members.isNotEmpty()) {
//                val share = amountInCents / members.size
//                members.forEach { member ->
//                    val split = Split(
//                        id = java.util.UUID.randomUUID().toString(),
//                        belongs_to = expenseId,
//                        timestamp = now,
//                        lamport_clock = 0,
//                        author_pubkey = myPublicKey,
//                        payer_key = myPublicKey,
//                        debtor_key = member.public_key,
//                        amount = share,
//                        signature = "p2p-signed" // Hier müsste auch eine Signatur hin
//                    )
//                    groupDao.insertSplit(split)
//                }
//            }
        }
    }

    private val _memberSplits = mutableStateOf<List<MemberSplitState>>(emptyList())
    val memberSplits: State<List<MemberSplitState>> = _memberSplits

    fun loadMembers() {
        viewModelScope.launch {
            groupDao.getUsersForGroupFlow(groupId).collect { users ->
                _memberSplits.value = users.map { user ->
                    // Standardmäßig: Ich selbst bin ausgewählt
                    val isMe = user.public_key == myPublicKey
                    MemberSplitState(
                        publicKey = user.public_key,
                        name = user.name,
                        isSelected = isMe
                    )
                }
                //recalculateSplits(totalAmountInCents) // Initiale Berechnung
            }
        }
    }

    fun toggleMember(publicKey: String, totalAmount: Long) {
        _memberSplits.value = _memberSplits.value.map {
            if (it.publicKey == publicKey) it.copy(isSelected = !it.isSelected) else it
        }
        recalculateSplits(totalAmount)
    }

    fun recalculateSplits(totalAmount: Long) {
        val selectedMembers = _memberSplits.value.filter { it.isSelected }
        if (selectedMembers.isEmpty()) return

        val share = totalAmount / selectedMembers.size
        _memberSplits.value = _memberSplits.value.map { member ->
            if (member.isSelected) {
                member.copy(
                    amountInCents = share,
                    percentage = 1f / selectedMembers.size
                )
            } else {
                member.copy(amountInCents = 0, percentage = 0f)
            }
        }
    }
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
    onBack: () -> Unit,
    onSave: (description: String, amountInCents: Long) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

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
            // Speichern-Button nur aktiv, wenn Beschreibung und Betrag da sind
            if (description.isNotBlank() && amountText.isNotBlank()) {
                FloatingActionButton(
                    onClick = {
                        // Konvertierung von "12,50" oder "12.50" zu 1250 Cents
                        val amountInCents = parseAmountToCents(amountText)
                        onSave(description, amountInCents)
                    },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Speichern")
                }
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

            Text(
                text = "Die Ausgabe wird zu gleichen Teilen auf alle Gruppenmitglieder aufgeteilt.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
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
    onToggle: () -> Unit,
    totalAmount: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = member.isSelected, onCheckedChange = { onToggle() })

        Text(
            text = member.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Slider (deaktiviert, wenn nicht ausgewählt)
        Slider(
            value = member.percentage,
            onValueChange = { /* Später für manuellen Split */ },
            modifier = Modifier.weight(1.5f).padding(horizontal = 8.dp),
            enabled = member.isSelected
        )

        // Betrags-Box
        OutlinedTextField(
            value = (member.amountInCents / 100.0).toString(),
            onValueChange = {},
            readOnly = true, // Erstmal nur Anzeige für Auto-Split
            modifier = Modifier.width(80.dp),
            suffix = { Text("€") },
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

data class MemberSplitState(
    val publicKey: String,
    val name: String,
    val isSelected: Boolean = false,
    val amountInCents: Long = 0,
    val percentage: Float = 0f
)


