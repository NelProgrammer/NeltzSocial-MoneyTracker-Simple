package com.moneytracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
// import removed: hierarchy extension not needed
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
import com.moneytracker.ui.viewmodel.AddEditViewModel
import com.moneytracker.ui.viewmodel.DashboardViewModel
import com.moneytracker.ui.viewmodel.StatsViewModel
import com.moneytracker.ui.viewmodel.TransactionsViewModel
import com.moneytracker.ui.viewmodel.CategoriesViewModel
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.ui.viewmodel.MonthComparisonViewModel
import com.moneytracker.ui.viewmodel.ViewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.List)
    object Stats : Screen("stats", "Stats", Icons.Default.PieChart)
    object Groceries : Screen("groceries", "Groceries", Icons.Default.ShoppingCart)
    object TaxiFare : Screen("taxi", "Taxi", Icons.Default.DirectionsCar)
    object MonthComparison : Screen("monthComparison", "Month Compare", Icons.Default.CalendarMonth)
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object AddTransaction : Screen("addTransaction", "Add", Icons.Default.Add)
    object EditTransaction : Screen("editTransaction/{transactionId}", "Edit", Icons.Default.Edit) {
        fun createRoute(transactionId: Long) = "editTransaction/$transactionId"
    }
}

@Composable
fun MoneyTrackerNavHost(repository: TransactionRepository) {
    val navController = rememberNavController()
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    val categories by categoriesViewModel.categories.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Transactions,
        Screen.Stats,
        Screen.Groceries,
        Screen.TaxiFare,
        Screen.MonthComparison
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDestination?.route == screen.route,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
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
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onEditTransaction = { id ->
                        navController.navigate(Screen.EditTransaction.createRoute(id))
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
            composable(Screen.Groceries.route) {
                GroceriesScreen(contentPadding = innerPadding)
            }
            composable(Screen.TaxiFare.route) {
                TaxiFareScreen(contentPadding = innerPadding)
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
