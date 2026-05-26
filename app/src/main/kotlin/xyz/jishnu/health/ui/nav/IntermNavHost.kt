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
import xyz.jishnu.health.ui.screens.onboarding.OnboardNotificationsScreen
import xyz.jishnu.health.ui.screens.onboarding.OnboardPlanScreen
import xyz.jishnu.health.ui.screens.onboarding.OnboardWaterScreen
import xyz.jishnu.health.ui.screens.onboarding.OnboardWeightScreen
import xyz.jishnu.health.ui.screens.onboarding.OnboardWelcomeScreen
import xyz.jishnu.health.ui.screens.progress.ProgressScreen
import xyz.jishnu.health.ui.screens.settings.PlanPickerScreen
import xyz.jishnu.health.ui.screens.settings.SettingsScreen
import xyz.jishnu.health.ui.screens.stages.StagesScreen
import xyz.jishnu.health.ui.screens.water.WaterScreen
import xyz.jishnu.health.ui.screens.weight.WeightScreen
import xyz.jishnu.health.vm.FastingViewModel

object Routes {
    const val Home = "home"
    const val Weight = "weight"
    const val Water = "water"
    const val Progress = "progress"
    const val Settings = "settings"
    const val Stages = "stages"
    const val PlanPicker = "plan-picker"
    const val DayDetail = "day-detail"
    const val OnboardWelcome = "onboard-welcome"
    const val OnboardPlan = "onboard-plan"
    const val OnboardWeight = "onboard-weight"
    const val OnboardWater = "onboard-water"
    const val OnboardNotifications = "onboard-notifications"
}

@Composable
fun IntermNavHost(
    vm: FastingViewModel,
    startDestination: String = Routes.Home,
    navController: NavHostController = rememberNavController(),
) {
    fun navigateTab(tab: NavTab) {
        val route = when (tab) {
            NavTab.Today -> Routes.Home
            NavTab.Weight -> Routes.Weight
            NavTab.Water -> Routes.Water
            NavTab.Progress -> Routes.Progress
        }
        navController.navigate(route) {
            popUpTo(Routes.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.OnboardWelcome) {
            OnboardWelcomeScreen(
                onGetStarted = { navController.navigate(Routes.OnboardPlan) },
            )
        }
        composable(Routes.OnboardPlan) {
            OnboardPlanScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.OnboardWeight) },
            )
        }
        composable(Routes.OnboardWeight) {
            OnboardWeightScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.OnboardWater) },
            )
        }
        composable(Routes.OnboardWater) {
            OnboardWaterScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.OnboardNotifications) },
            )
        }
        composable(Routes.OnboardNotifications) {
            OnboardNotificationsScreen(
                onBack = { navController.popBackStack() },
                onFinish = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.OnboardWelcome) { inclusive = true }
                    }
                },
            )
        }
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
        composable(Routes.Water) {
            WaterScreen(onNavigateTab = ::navigateTab)
        }
        composable(Routes.Progress) {
            ProgressScreen(
                onNavigateTab = ::navigateTab,
                onOpenDay = { dayKey, sessionId ->
                    navController.navigate("${Routes.DayDetail}/$dayKey?sessionId=${sessionId ?: -1L}")
                },
            )
        }
        composable(
            route = "${Routes.DayDetail}/{dayKey}?sessionId={sessionId}",
            arguments = listOf(
                navArgument("dayKey") { type = NavType.LongType },
                navArgument("sessionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val dayKey = entry.arguments?.getLong("dayKey") ?: 0L
            DayDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenSession = { sessionId ->
                    navController.navigate("${Routes.DayDetail}/$dayKey?sessionId=$sessionId") {
                        popUpTo("${Routes.DayDetail}/{dayKey}?sessionId={sessionId}") { inclusive = true }
                    }
                },
            )
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
