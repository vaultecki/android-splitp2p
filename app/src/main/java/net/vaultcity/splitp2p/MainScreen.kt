package net.vaultcity.splitp2p

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(database: AppDatabase, onLogout: () -> Unit) {
    // Beobachtet die Datenbank. Wenn sich das Profil ändert, wird die UI neu gezeichnet.
    val userProfile by database.userProfileDao().getUserProfile().collectAsState(initial = null)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userProfile != null) {
            Text(
                text = "Hallo, ${userProfile?.name}!",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Deine ID (Key): ${userProfile?.publicKeyHex?.take(8)}...",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text("Lade Profil...")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            // Beim Logout auch die DB löschen
            // Hier wäre eine Coroutine nötig, oder du löschst nur den Key im Keystore
            deleteKeyFromKeystore("SplitP2PUser")
            onLogout()
        }) {
            Text("Identität löschen & Logout")
        }
    }
}