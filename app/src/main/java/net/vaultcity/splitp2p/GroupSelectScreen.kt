package net.vaultcity.splitp2p

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
//
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject

@OptIn(ExperimentalMaterial3Api::class) // Notwendig für CenterAlignedTopAppBar
@Composable
fun GroupSelectionScreen(
    viewModel: GroupViewModel,
    onGroupSelected: (String) -> Unit,
    onAddGroup: () -> Unit,
    onJoinGroup: () -> Unit
) {
    // Liste der Gruppen aus dem ViewModel beobachten
    val groups by viewModel.allGroups.collectAsState(initial = emptyList())

    Scaffold(
        // --- Der neue grüne Balken oben ---
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SplitP2P",
                        fontWeight = FontWeight.Bold,
                        color = Color.White // Textfarbe Weiß
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF4CAF50) // Schönes Grün (Material Green 500)
                )
            )
        },
        // ----------------------------------
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onAddGroup) { Text("Add Group") }
                    Button(onClick = onJoinGroup) { Text("Join Group") }
                }
            }
        }
    ) { paddingValues -> // paddingValues enthält den Platz, den TopBar und BottomBar einnehmen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // WICHTIG: Damit die Liste nicht unter den Balken verschwindet
                .padding(horizontal = 8.dp)
        ) {
            items(groups) { group ->
                GroupCard(group = group, onClick = { onGroupSelected(group.group_id) })
            }
        }
    }
}

@Composable
fun GroupCard(group: GroupInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            // Optional: Währung anzeigen, falls hilfreich
            Text(
                text = "Currency: ${group.currency}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

class GroupViewModel(private val groupDao: GroupDao) : ViewModel() {
    val allGroups = groupDao.getAllGroupsFlow()

    // Hier gehört die Funktion hin!
    fun addGroup(payload: GroupJoinPayload, myName: String, myPublicKey: String) {
        viewModelScope.launch {
            val newGroup = GroupInfo(
                group_id = payload.i,
                name = payload.n,
                currency = payload.c,
                group_key = payload.k
            )
            groupDao.insertGroup(newGroup)

            // Deterministischen JSON String für die Signatur bauen
            val lamportStart = 0
            val signJson = createSignatureJson(
                publicKey = myPublicKey,
                groupId = payload.i,
                name = myName,
                lamport = lamportStart
            )

            Log.d("Crypto", "Json String to sign: $signJson")

            // 3Signieren mit dem Hardware-Key
            val mySignature = signJsonWithKeystore("SplitP2PUser", signJson)

            // In die users-Tabelle schreiben
            val selfAsUser = User(
                public_key = myPublicKey,
                name = myName,
                timestamp = System.currentTimeMillis(),
                group_id = payload.i,
                lamport_clock = lamportStart,
                signature = mySignature
            )
            groupDao.insertUser(selfAsUser)
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            // Ggf. hier noch Logik, um deinen eigenen public_key aus der users-Tabelle für diese Gruppe zu löschen
            groupDao.deleteGroupById(groupId)
            // TODO delete from user table
            // TODO delete from expense table -> delete split, comment, attachment
            // TODO delete from settlement table -> delete split, comment, attachment
        }
    }
}


// Funktion um das deterministische JSON zu erzeugen
fun createSignatureJson(publicKey: String, groupId: String, name: String, lamport: Int): String {
    val jsonObject = buildJsonObject {
        // Wir nutzen die expliziten Typ-Methoden, um die Inferenz-Fehler zu umgehen
        put("group_id", groupId as String)
        put("lamport_clock", lamport as Number)
        put("name", name as String)
        put("public_key", publicKey as String)
    }

    // Für JsonElements (wie JsonObject) ist .toString() der korrekte Weg,
    // um ein kompaktes JSON ohne Leerzeichen zu erhalten.
    return jsonObject.toString()
}

