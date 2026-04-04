package net.vaultcity.splitp2p

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Haupt-App: Deine Hardware-Identität ist aktiv!")
        Button(onClick = {
            deleteKeyFromKeystore("SplitP2PUser")
            onLogout()
        }) {
            Text("Identität löschen & Logout")
        }
    }
}