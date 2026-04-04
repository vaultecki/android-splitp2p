package net.vaultcity.splitp2p

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun OnboardingScreen(onKeyGenerated: () -> Unit) {
    val keyAlias = "SplitP2PUser"
    WelcomeScreen(onGenerateKey = {
        generateKeystoreEd25519(keyAlias)
        onKeyGenerated()
    })
}

@Composable
fun OnboardingScreen(onFinished: (String) -> Unit) {
    var userName by remember { mutableStateOf("SplitP2PUser") }
    val keyAlias = "SplitP2PUser"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Willkommen bei SplitP2P", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Wähle einen Namen, unter dem dich andere Peers sehen können.")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Dein Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // 1. Hardware-Key im Keystore erzeugen
                generateKeystoreEd25519(keyAlias)
                // 2. Callback mit dem Namen (um ihn z.B. in Room zu speichern)
                onFinished(userName)
            },
            enabled = userName.isNotBlank(), // Button sperren wenn leer
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Profil erstellen")
        }
    }
}