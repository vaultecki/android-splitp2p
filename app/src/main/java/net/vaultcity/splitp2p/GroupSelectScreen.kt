package net.vaultcity.splitp2p

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
    fun addGroup(name: String, currency: String) {
        viewModelScope.launch {
            val newGroup = GroupInfo(
                group_id = java.util.UUID.randomUUID().toString(),
                name = name,
                currency = currency,
                group_key = java.util.UUID.randomUUID().toString().replace("-", "")
            )
            groupDao.insertGroup(newGroup)
        }
    }
}
