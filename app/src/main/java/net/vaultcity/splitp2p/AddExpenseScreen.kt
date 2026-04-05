package net.vaultcity.splitp2p

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

            // 4. TODO: Splits erzeugen (Jeder in der Gruppe schuldet seinen Teil)
        }
    }
}

