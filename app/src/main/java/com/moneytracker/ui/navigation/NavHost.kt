package com.moneytracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
import com.moneytracker.ui.screens.GroceriesScreen
import com.moneytracker.ui.screens.TaxiFareScreen
import com.moneytracker.ui.screens.MonthComparisonScreen
import com.moneytracker.ui.screens.CategoriesScreen
import com.moneytracker.ui.screens.SettingsScreen
import com.moneytracker.ui.screens.ProfileSelectionScreen
import com.moneytracker.ui.viewmodel.AddEditViewModel
import com.moneytracker.ui.viewmodel.DashboardViewModel
import com.moneytracker.ui.viewmodel.StatsViewModel
import com.moneytracker.ui.viewmodel.TransactionsViewModel
import com.moneytracker.ui.viewmodel.CategoriesViewModel
import com.moneytracker.ui.viewmodel.SettingsViewModel
import com.moneytracker.ui.viewmodel.GroceriesViewModel
import com.moneytracker.ui.viewmodel.TaxiFareViewModel
import com.moneytracker.ui.viewmodel.ProfileViewModel
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.ui.viewmodel.MonthComparisonViewModel
import com.moneytracker.ui.viewmodel.ViewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt

import com.moneytracker.ui.screens.SummaryTableScreen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.moneytracker.ui.screens.ShoppingListScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object ProfileSelection : Screen("profileSelection", "Profile", Icons.Default.Person)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object SummaryTable : Screen("summaryTable", "Table", Icons.Default.TableChart)
    object Transactions : Screen("transactions", "Transactions", Icons.AutoMirrored.Filled.List)
    object Stats : Screen("stats", "Stats", Icons.Default.PieChart)
    object Groceries : Screen("groceries", "Monthly Groceries", Icons.Default.ShoppingCart)
    object ShoppingList : Screen("shoppingList", "Grocery Shopping List", Icons.AutoMirrored.Filled.ListAlt)
    object TaxiFare : Screen("taxi", "Taxi", Icons.Default.DirectionsCar)
    object MonthComparison : Screen("monthComparison", "Month Compare", Icons.Default.CalendarMonth)
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AddTransaction : Screen("addTransaction", "Add", Icons.Default.Add)
    object EditTransaction : Screen("editTransaction/{transactionId}", "Edit", Icons.Default.Edit) {
        fun createRoute(transactionId: Long) = "editTransaction/$transactionId"
    }
}

@Composable
fun MoneyTrackerNavHost(repository: TransactionRepository) {
    val navController = rememberNavController()
    val profileViewModel: ProfileViewModel = viewModel(factory = ViewModelFactory(repository))
    val activeProfile by profileViewModel.activeProfile.collectAsState()

    val categoriesViewModel: CategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    val categories by categoriesViewModel.categories.collectAsState(initial = emptyList())

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.SummaryTable,
        Screen.Transactions,
        Screen.Groceries,
        Screen.ShoppingList,
        Screen.TaxiFare,
        Screen.MonthComparison
    )

    if (activeProfile == null) {
        ProfileSelectionScreen(
            viewModel = profileViewModel,
            onProfileSelected = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (currentRoute != Screen.ProfileSelection.route) {
                    NavigationBar {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    label = {
                                        Text(
                                            text = screen.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    alwaysShowLabel = true
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.ProfileSelection.route) {
                    ProfileSelectionScreen(
                        viewModel = profileViewModel,
                        onProfileSelected = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Dashboard.route) {
                    val viewModel: DashboardViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    DashboardScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding,
                        onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                        onViewAll = { navController.navigate(Screen.SummaryTable.route) },
                        onEditTransaction = { id ->
                            navController.navigate(Screen.EditTransaction.createRoute(id))
                        },
                        onOpenSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }
                composable(Screen.SummaryTable.route) {
                    val viewModel: DashboardViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    SummaryTableScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding
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
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Stats.route) {
                    val viewModel: StatsViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    StatsScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Groceries.route) {
                    val viewModel: GroceriesViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    GroceriesScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding
                    )
                }
                composable(Screen.ShoppingList.route) {
                    val viewModel: GroceriesViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    ShoppingListScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding
                    )
                }
                composable(Screen.TaxiFare.route) {
                    val viewModel: TaxiFareViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    TaxiFareScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding
                    )
                }
                composable(Screen.MonthComparison.route) {
                    val viewModel: MonthComparisonViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    MonthComparisonScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding
                    )
                }
                composable(Screen.Categories.route) {
                    val viewModel: CategoriesViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    CategoriesScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding
                    )
                }
                composable(Screen.Settings.route) {
                    val viewModel: SettingsViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    SettingsScreen(
                        viewModel = viewModel,
                        profileViewModel = profileViewModel,
                        contentPadding = innerPadding,
                        onNavigateBack = { navController.popBackStack() },
                        onSwitchProfile = {
                            navController.navigate(Screen.ProfileSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.AddTransaction.route) {
                    val viewModel: AddEditViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )
                    AddEditScreen(
                        viewModel = viewModel,
                        repository = repository,
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
                        repository = repository,
                        title = "Edit Transaction",
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
