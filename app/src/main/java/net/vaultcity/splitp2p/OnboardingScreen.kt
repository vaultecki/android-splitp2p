package net.vaultcity.splitp2p

import androidx.compose.runtime.Composable


@Composable
fun OnboardingScreen(onKeyGenerated: () -> Unit) {
    val keyAlias = "SplitP2PUser"
    WelcomeScreen(onGenerateKey = {
        generateKeystoreEd25519(keyAlias)
        onKeyGenerated()
    })
}
