package net.vaultcity.splitp2p

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupJoined: (GroupJoinPayload) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Join Group", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF4CAF50)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Füge den Einladungscode (JSON) ein, um einer Gruppe beizutreten.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = jsonInput,
                onValueChange = {
                    jsonInput = it
                    errorMessage = null
                },
                label = { Text("Gruppen-JSON") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    try {
                        // TODO remove test string
                        jsonInput = "{\"id\":\"test-uuid-12345\",\"name\":\"TestWG\",\"cur\":\"USD\",\"key\":\"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\",\"v\":1}"
                        // 1. JSON in das bestehende Objekt parsen
                        val payload = Json.decodeFromString<GroupJoinPayload>(jsonInput)

                        onGroupJoined(payload)
                    } catch (e: Exception) {
                        errorMessage = "Ungültiges Format. Bitte prüfe den JSON-String."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = jsonInput.isNotBlank()
            ) {
                Text("Gruppe beitreten")
            }
        }
    }
}