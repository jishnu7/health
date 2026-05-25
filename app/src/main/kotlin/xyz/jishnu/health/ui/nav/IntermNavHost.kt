package xyz.jishnu.health.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.screens.home.HomeScreen
import xyz.jishnu.health.ui.screens.settings.PlanPickerScreen
import xyz.jishnu.health.ui.screens.settings.SettingsScreen
import xyz.jishnu.health.ui.screens.stages.StagesScreen
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingViewModel

object Routes {
    const val Home = "home"
    const val Weight = "weight"
    const val Progress = "progress"
    const val Settings = "settings"
    const val Stages = "stages"
    const val PlanPicker = "plan-picker"
}

@Composable
fun IntermNavHost(
    vm: FastingViewModel,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                vm = vm,
                onNavigateTab = { tab ->
                    when (tab) {
                        NavTab.Today -> Unit
                        NavTab.Weight -> navController.navigate(Routes.Weight) { launchSingleTop = true }
                        NavTab.Progress -> navController.navigate(Routes.Progress) { launchSingleTop = true }
                    }
                },
                onOpenStages = { navController.navigate(Routes.Stages) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Stages) {
            StagesScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.Weight) { PlaceholderScreen("Weight") }
        composable(Routes.Progress) { PlaceholderScreen("Progress") }
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

@Composable
private fun PlaceholderScreen(title: String) {
    val c = IntermTheme.colors
    Box(modifier = Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
        Text(title, style = IntermTheme.typography.hTitle, color = c.ink)
    }
}
