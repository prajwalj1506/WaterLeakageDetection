package com.waterleakage.app.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waterleakage.app.ui.screens.add.AddReadingScreen
import com.waterleakage.app.ui.screens.add.AddReadingViewModel
import com.waterleakage.app.ui.screens.dashboard.DashboardScreen
import com.waterleakage.app.ui.screens.dashboard.DashboardViewModel
import com.waterleakage.app.ui.screens.details.ReadingDetailScreen
import com.waterleakage.app.ui.screens.details.ReadingDetailViewModel
import com.waterleakage.app.ui.screens.history.ReadingHistoryScreen
import com.waterleakage.app.ui.screens.history.ReadingHistoryViewModel
import com.waterleakage.app.ui.screens.result.PredictionResultScreen
import com.waterleakage.app.ui.screens.result.PredictionResultViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Dashboard.route
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 1. Dashboard Screen
        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel()
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToAdd = { navController.navigate(Screen.AddReading.route) },
                onNavigateToHistory = { navController.navigate(Screen.ReadingHistory.route) },
                onNavigateToDetails = { id -> navController.navigate(Screen.ReadingDetails.createRoute(id)) }
            )
        }

        // 2. Add Reading Screen
        composable(Screen.AddReading.route) {
            val addViewModel: AddReadingViewModel = viewModel()
            AddReadingScreen(
                viewModel = addViewModel,
                onNavigateBack = { navController.popBackStack() },
                onReadingSubmitted = { readingId ->
                    navController.navigate(Screen.PredictionResult.createRoute(readingId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        // 3. Prediction Result Screen
        composable(
            route = Screen.PredictionResult.route,
            arguments = listOf(navArgument("readingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val readingId = backStackEntry.arguments?.getInt("readingId") ?: 0
            val resultViewModel = PredictionResultViewModel(app, readingId)
            PredictionResultScreen(
                viewModel = resultViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = {
                    navController.navigate(Screen.ReadingHistory.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onAddAnother = {
                    navController.navigate(Screen.AddReading.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        // 4. Reading History Screen
        composable(Screen.ReadingHistory.route) {
            val historyViewModel: ReadingHistoryViewModel = viewModel()
            ReadingHistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { id -> navController.navigate(Screen.ReadingDetails.createRoute(id)) }
            )
        }

        // 5. Reading Details Screen
        composable(
            route = Screen.ReadingDetails.route,
            arguments = listOf(navArgument("readingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val readingId = backStackEntry.arguments?.getInt("readingId") ?: 0
            val detailViewModel = ReadingDetailViewModel(app, readingId)
            ReadingDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

