package net.vaultcity.splitp2p

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

                // userprofile wird benötigt
                val userProfile by db.userProfileDao().getUserProfile().collectAsState(initial = null)

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
                                navController.navigate("expense_overview/$groupId")
                            },
                            onAddGroup = { navController.navigate("add_group") },
                            onJoinGroup = { navController.navigate("join_group") }
                        )
                    }

                    composable("add_group") {
                        val groupDao = db.groupDao()
                        val groupViewModel = viewModel<GroupViewModel>(
                            factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return GroupViewModel(groupDao) as T
                                }
                            }
                        )

                        AddGroupScreen(
                            onGroupCreated = { payload ->
                                userProfile?.let { profile ->
                                    groupViewModel.addGroup(
                                        payload = payload,
                                        myName = profile.name,
                                        myPublicKey = profile.publicKeyHex
                                    )
                                }
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("join_group") {
                        val groupDao = db.groupDao()
                        val groupViewModel = viewModel<GroupViewModel>(
                            factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return GroupViewModel(groupDao) as T
                                }
                            }
                        )

                        JoinGroupScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onGroupJoined = { payload ->
                                userProfile?.let { profile ->
                                    groupViewModel.addGroup(
                                        payload = payload,
                                        myName = profile.name,
                                        myPublicKey = profile.publicKeyHex
                                    )

                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable("expense_overview/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                        val groupDao = db.groupDao()

                        // ViewModel mit Factory, um die groupId zu übergeben
                        val detailViewModel = viewModel<ExpenseOverviewModel>(
                            factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return ExpenseOverviewModel(groupDao, groupId) as T
                                }
                            }
                        )

                        ExpenseOverviewScreen(
                            viewModel = detailViewModel,
                            onBack = { navController.popBackStack() },
                            onAddExpense = {
                                // Hier später: navController.navigate("add_expense/$groupId")
                            }
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

