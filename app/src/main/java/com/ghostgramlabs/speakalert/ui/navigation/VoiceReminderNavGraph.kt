package com.ghostgramlabs.speakalert.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghostgramlabs.speakalert.ui.home.HomeScreen
import com.ghostgramlabs.speakalert.ui.addedit.AddEditReminderScreen
import com.ghostgramlabs.speakalert.ui.details.ReminderDetailsScreen
import com.ghostgramlabs.speakalert.ui.settings.BatteryOptimizationGuideScreen
import com.ghostgramlabs.speakalert.ui.settings.SettingsScreen

@Composable
fun VoiceReminderNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    startReminderId: Long? = null,
    autoplay: Boolean = false,
    startAddEdit: Boolean = false
) {
    // If launched from notification, navigate to details screen
    LaunchedEffect(startReminderId) {
        if (startReminderId != null) {
            navController.navigate(NavigationDestination.Details.createRoute(startReminderId, autoplay)) {
                // Clear backstack so back button goes home
                popUpTo(NavigationDestination.Home.route) { inclusive = false }
            }
        }
    }
    LaunchedEffect(startAddEdit) {
        if (startAddEdit) {
            navController.navigate(NavigationDestination.AddEdit.createRoute()) {
                popUpTo(NavigationDestination.Home.route) { inclusive = false }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Home.route,
        modifier = modifier
    ) {
        composable(route = NavigationDestination.Home.route) {
            HomeScreen(
                navigateToItemUpdate = { id ->
                    navController.navigate(NavigationDestination.Details.createRoute(id, false))
                },
                navigateToAddItem = {
                    navController.navigate(NavigationDestination.AddEdit.route)
                },
                navigateToSettings = {
                    navController.navigate(NavigationDestination.Settings.route)
                }
            )
        }
        
        composable(
            route = NavigationDestination.AddEdit.route,
            arguments = listOf(
                navArgument("reminderId") { 
                    type = NavType.LongType 
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
            AddEditReminderScreen(
                reminderId = reminderId,
                navigateBack = { navController.popBackStack() },
                onNavigateUp = { navController.navigateUp() }
            )
        }
        
        composable(
            route = NavigationDestination.Details.route,
            arguments = listOf(
                navArgument("reminderId") { type = NavType.LongType },
                navArgument("autoplay") { 
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
             val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: return@composable
             val shouldAutoplay = backStackEntry.arguments?.getBoolean("autoplay") ?: false
             ReminderDetailsScreen(
                 reminderId = reminderId,
                 autoplay = shouldAutoplay,
                 navigateBack = { navController.popBackStack() },
                 navigateToEdit = { id -> 
                     navController.navigate(NavigationDestination.AddEdit.createRoute(id))
                 }
             )
        }
        
        composable(route = NavigationDestination.Settings.route) {
            val viewModel: com.ghostgramlabs.speakalert.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.ghostgramlabs.speakalert.ui.AppViewModelProvider.Factory)
            SettingsScreen(
                viewModel = viewModel,
                onNavigateUp = { navController.popBackStack() },
                onOpenBatteryOptimizationGuide = {
                    navController.navigate(NavigationDestination.BatteryOptimizationGuide.route)
                }
            )
        }

        composable(route = NavigationDestination.BatteryOptimizationGuide.route) {
            BatteryOptimizationGuideScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
