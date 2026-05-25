package xyz.jishnu.health.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.screens.daydetail.DayDetailScreen
import xyz.jishnu.health.ui.screens.home.HomeScreen
import xyz.jishnu.health.ui.screens.progress.ProgressScreen
import xyz.jishnu.health.ui.screens.settings.PlanPickerScreen
import xyz.jishnu.health.ui.screens.settings.SettingsScreen
import xyz.jishnu.health.ui.screens.stages.StagesScreen
import xyz.jishnu.health.ui.screens.weight.WeightScreen
import xyz.jishnu.health.vm.FastingViewModel

object Routes {
    const val Home = "home"
    const val Weight = "weight"
    const val Progress = "progress"
    const val Settings = "settings"
    const val Stages = "stages"
    const val PlanPicker = "plan-picker"
    const val DayDetail = "day-detail"
}

@Composable
fun IntermNavHost(
    vm: FastingViewModel,
    navController: NavHostController = rememberNavController(),
) {
    fun navigateTab(tab: NavTab) {
        val route = when (tab) {
            NavTab.Today -> Routes.Home
            NavTab.Weight -> Routes.Weight
            NavTab.Progress -> Routes.Progress
        }
        navController.navigate(route) {
            popUpTo(Routes.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                vm = vm,
                onNavigateTab = ::navigateTab,
                onOpenStages = { navController.navigate(Routes.Stages) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Stages) {
            StagesScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.Weight) {
            WeightScreen(
                onNavigateTab = ::navigateTab,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.Progress) {
            ProgressScreen(
                onNavigateTab = ::navigateTab,
                onOpenDay = { dayKey -> navController.navigate("${Routes.DayDetail}/$dayKey") },
            )
        }
        composable(
            route = "${Routes.DayDetail}/{dayKey}",
            arguments = listOf(navArgument("dayKey") { type = NavType.LongType }),
        ) {
            DayDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPlanPicker = { navController.navigate(Routes.PlanPicker) },
            )
        }
        composable(Routes.PlanPicker) {
            PlanPickerScreen(onBack = { navController.popBackStack() })
        }
    }
}
