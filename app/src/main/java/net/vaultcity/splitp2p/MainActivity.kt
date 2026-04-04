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
//
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Datenbank als Member-Variable (Lazy sorgt für Initialisierung bei erstem Zugriff)
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "splitp2p-db"
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Splitp2pTheme {
                val navController = rememberNavController()
                val keyAlias = "SplitP2PUser"

                // Navigation Logik
                val startDestination = if (hasIdentityKey(keyAlias)) "main" else "onboarding"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("onboarding") {
                        OnboardingScreen(onFinished = { name ->
                            val pubKey = getPublicKeyAsHex(keyAlias)

                            // Speichern in Room
                            lifecycleScope.launch {
                                db.userProfileDao().insertProfile(
                                    UserProfile(name = name, publicKeyHex = pubKey)
                                )
                                navController.navigate("main") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        })
                    }

                    composable("main") {
                        // Wir übergeben die DB an den MainScreen
                        MainScreen(database = db, onLogout = {
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

