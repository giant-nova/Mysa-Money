package com.giantnovadevs.mysamoney.ui.navigation


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.giantnovadevs.mysamoney.ui.screens.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.giantnovadevs.mysamoney.viewmodel.AuthViewModel
import com.giantnovadevs.mysamoney.viewmodel.BackupViewModel
import com.giantnovadevs.mysamoney.viewmodel.FinancialCoachViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import com.giantnovadevs.mysamoney.viewmodel.SettingsViewModel


@Composable
fun AppNavGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    proViewModel: ProViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backupViewModel: BackupViewModel = viewModel()
    val financialCoachViewModel: FinancialCoachViewModel = viewModel()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, onMenuClick = onMenuClick) }
        composable(
            route = "expense_entry?categoryId={categoryId}&expenseId={expenseId}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("expenseId") { // The ID of the expense to edit
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            AddExpenseScreen(
                navController = navController,
                categoryId = categoryId,
                expenseId = expenseId, // Pass the new ID
                proViewModel = proViewModel
            )
        }
        composable("list") { ExpenseListScreen(navController, onMenuClick = onMenuClick, proViewModel) }
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
            FinancialCoachScreen(
                navController = navController,
                onMenuClick = onMenuClick,
                proViewModel = proViewModel,
                viewModel = financialCoachViewModel
            )
        }

        composable("settings") {
            SettingsScreen(
                navController,
                onMenuClick = onMenuClick,
                viewModel = settingsViewModel,
                authViewModel = authViewModel,
                backupViewModel = backupViewModel,
                proViewModel = proViewModel
            )
        }

        composable("upgrade") {
            UpgradeScreen(navController, proViewModel)
        }

    }
}