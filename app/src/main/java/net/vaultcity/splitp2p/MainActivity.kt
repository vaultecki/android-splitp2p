package net.vaultcity.splitp2p

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.vaultcity.splitp2p.ui.theme.Splitp2pTheme
//
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
//
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import android.util.Log
//
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.security.KeyPairGenerator
import java.security.KeyStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Splitp2pTheme {
                val navController = rememberNavController()
                val keyAlias = "SplitP2PUser"

                // Start-Ziel bestimmen: Wenn Key da -> Main, sonst Onboarding
                val startDestination = if (hasIdentityKey(keyAlias)) "main" else "onboarding"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("onboarding") {
                        OnboardingScreen(onKeyGenerated = {
                            // Navigiere zur Main und lösche das Onboarding aus dem Backstack
                            navController.navigate("main") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        })
                    }
                    composable("main") {
                        MainScreen(onLogout = {
                            navController.navigate("onboarding") {
                                popUpTo("main") { inclusive = true }
                            }
                        })
                    }
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Splitp2pTheme {
        Greeting("Android")
    }
}

fun hasIdentityKey(alias: String): Boolean {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    return keyStore.containsAlias(alias)
}

@Composable
fun WelcomeScreen(onGenerateKey: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Willkommen bei SplitP2P", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Erstelle deine sichere Hardware-Identität, um zu starten.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onGenerateKey) {
            Text("Identität generieren")
        }
    }
}

fun deleteKeyFromKeystore(alias: String): Boolean {
    return try {
        // 1. Zugriff auf den Android KeyStore
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        // 2. Prüfen, ob der Alias existiert
        if (keyStore.containsAlias(alias)) {
            // 3. Den Schlüssel (und das zugehörige Zertifikat) löschen
            keyStore.deleteEntry(alias)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun generateKeystoreEd25519(alias: String) {
    val kpg = KeyPairGenerator.getInstance(
        "Ed25519",
        "AndroidKeyStore"
    )

    val parameterSpec = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    ).apply {
        setDigests(KeyProperties.DIGEST_NONE) // Ed25519 benötigt kein separates Hashing
    }.build()

    kpg.initialize(parameterSpec)
    kpg.generateKeyPair()
}