package com.moneytracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.screens.AddEditScreen
import com.moneytracker.ui.screens.DashboardScreen
import com.moneytracker.ui.screens.StatsScreen
import com.moneytracker.ui.screens.TransactionsScreen
import com.moneytracker.ui.viewmodel.AddEditViewModel
import com.moneytracker.ui.viewmodel.DashboardViewModel
import com.moneytracker.ui.viewmodel.StatsViewModel
import com.moneytracker.ui.viewmodel.TransactionsViewModel
import com.moneytracker.ui.viewmodel.ViewModelFactory

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Transactions : Screen("transactions", "Transactions", Icons.Default.List)
    data object Stats : Screen("stats", "Stats", Icons.Default.ShowChart)
    data object AddTransaction : Screen("add_transaction", "Add", Icons.Default.Add)
    data object EditTransaction : Screen("edit_transaction/{transactionId}", "Edit", Icons.Default.Add) {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
}

private val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Stats
)

@Composable
fun MoneyTrackerNavHost(repository: TransactionRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = ViewModelFactory(repository)
                )
                DashboardScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                    onViewAll = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Transactions.route) {
                val viewModel: TransactionsViewModel = viewModel(
                    factory = ViewModelFactory(repository)
                )
                TransactionsScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                    onEditTransaction = { id ->
                        navController.navigate(Screen.EditTransaction.createRoute(id))
                    }
                )
            }

            composable(Screen.Stats.route) {
                val viewModel: StatsViewModel = viewModel(
                    factory = ViewModelFactory(repository)
                )
                StatsScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }

            composable(Screen.AddTransaction.route) {
                val viewModel: AddEditViewModel = viewModel(
                    factory = ViewModelFactory(repository)
                )
                AddEditScreen(
                    viewModel = viewModel,
                    title = "Add Transaction",
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
                val viewModel: AddEditViewModel = viewModel(
                    factory = ViewModelFactory(repository, transactionId)
                )
                AddEditScreen(
                    viewModel = viewModel,
                    title = "Edit Transaction",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
