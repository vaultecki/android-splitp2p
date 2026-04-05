package net.vaultcity.splitp2p

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import net.vaultcity.splitp2p.ui.theme.Splitp2pTheme
//
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider // Für: object : ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    // Datenbank als Member-Variable (Lazy sorgt für Initialisierung bei erstem Zugriff)
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "splitp2p-db"
        )
            .fallbackToDestructiveMigration() // Löscht DB bei Version-Upgrade (gut für Dev)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Splitp2pTheme {
                val navController = rememberNavController()
                val keyAlias = "SplitP2PUser"

                // Navigation Logik
                val startDestination = if (hasIdentityKey(keyAlias)) "group_selection" else "onboarding"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("onboarding") {
                        OnboardingScreen(onFinished = { name ->
                            val pubKey = getPublicKeyAsHex(keyAlias)

                            // Speichern in Room
                            lifecycleScope.launch {
                                db.userProfileDao().insertProfile(
                                    UserProfile(name = name, publicKeyHex = pubKey)
                                )
                                navController.navigate("group_selection") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        })
                    }
                    // Das neue Gruppenauswahlfenster
                    composable("group_selection") {
                        val groupDao = db.groupDao()

                        // Erstelle die Factory als explizite Variable oder caste sie
                        val factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return GroupViewModel(groupDao) as T
                            }
                        }

                        val groupViewModel = viewModel<GroupViewModel>(factory = factory)

                        GroupSelectionScreen(
                            viewModel = groupViewModel,
                            onGroupSelected = { groupId ->
                                navController.navigate("group_detail/$groupId")
                            },
                            onAddGroup = { /* Navigation zu AddGroup */ },
                            onJoinGroup = { /* Navigation zu JoinGroup */ }
                        )
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

