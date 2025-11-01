package com.example.spendwise.ui.navigation


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.spendwise.ui.screens.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.spendwise.viewmodel.AuthViewModel
import com.example.spendwise.viewmodel.BackupViewModel
import com.example.spendwise.viewmodel.SettingsViewModel


@Composable
fun AppNavGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backupViewModel: BackupViewModel = viewModel()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, onMenuClick = onMenuClick) }
        composable(
            route = "add?categoryId={categoryId}", // Define an optional parameter
            arguments = listOf(navArgument("categoryId") {
                type = NavType.StringType // Pass as string, can be null
                nullable = true
            })
        ) { backStackEntry ->
            // Get the categoryId from the navigation arguments
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            AddExpenseScreen(navController, categoryId)
        }
        composable("list") { ExpenseListScreen(onMenuClick = onMenuClick) }
        composable("summary") { SummaryScreen(navController, onMenuClick = onMenuClick) }
        composable("categories") { CategoryScreen(navController, onMenuClick = onMenuClick) }
        composable("about") {
            AboutScreen(navController, onMenuClick = onMenuClick)
        }
        composable("budgets") { BudgetScreen(navController, onMenuClick = onMenuClick) }
        composable("recurring_expenses") {
            RecurringExpenseScreen(
                navController,
                onMenuClick = onMenuClick
            )
        }

        composable("add_recurring_expense") {
            AddRecurringExpenseScreen(navController)
        }
        composable("incomes") {
            IncomeListScreen(navController, onMenuClick = onMenuClick)
        }

        composable("add_income") {
            AddIncomeScreen(navController)
        }

        composable("camera_screen") {
            CameraScreen(
                onImageCaptured = { uri ->
                    // Pass the URI back to the AddExpenseScreen
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("image_uri", uri)
                    navController.popBackStack()
                },
                onError = {
                    // Handle error, e.g., show a toast
                    navController.popBackStack()
                }
            )
        }

        composable("coach") {
            FinancialCoachScreen(navController, onMenuClick = onMenuClick)
        }

        composable("settings") {
            SettingsScreen(
                navController,
                onMenuClick = onMenuClick,
                viewModel = settingsViewModel,
                authViewModel = authViewModel,
                backupViewModel = backupViewModel
            )
        }

    }
}