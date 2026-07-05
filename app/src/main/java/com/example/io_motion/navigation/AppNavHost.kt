package com.example.io_motion.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.core.common.util.parseEnumOrDefault
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.feature.history.HistoryScreen
import com.example.io_motion.feature.history.SessionReportScreen
import com.example.io_motion.feature.live.HomeScreen
import com.example.io_motion.feature.live.LiveScreen
import com.example.io_motion.feature.video.VideoScreen

private object Routes {
    const val HOME    = "home"
    const val LIVE    = "live/{exerciseType}/{modelVariant}"
    const val VIDEO   = "video/{exerciseType}/{modelVariant}"
    const val HISTORY = "history"
    const val REPORT  = "report/{sessionId}"

    fun live(exerciseType: ExerciseType, modelVariant: PoseModelVariant) =
        "live/${exerciseType.name}/${modelVariant.name}"

    fun video(exerciseType: ExerciseType, modelVariant: PoseModelVariant) =
        "video/${exerciseType.name}/${modelVariant.name}"

    fun report(sessionId: Long) = "report/$sessionId"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    themeMode: ThemeMode = ThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onStart = { exercise, model, mode ->
                    when (mode) {
                        AnalysisMode.LIVE    -> navController.navigate(Routes.live(exercise, model))
                        AnalysisMode.OFFLINE -> navController.navigate(Routes.video(exercise, model))
                    }
                },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                themeMode = themeMode,
                onToggleTheme = onToggleTheme,
            )
        }

        composable(
            route = Routes.LIVE,
            arguments = listOf(
                // Not read here — LiveViewModel reads "exerciseType"/"modelVariant" directly from
                // the nav backstack's SavedStateHandle (see LiveViewModel), which avoids the
                // initialize()-after-construction race the old approach had.
                navArgument("exerciseType") { type = NavType.StringType },
                navArgument("modelVariant") { type = NavType.StringType },
            ),
        ) {
            LiveScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.VIDEO,
            arguments = listOf(
                navArgument("exerciseType") { type = NavType.StringType },
                navArgument("modelVariant") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val exerciseType = parseEnumOrDefault(
                backStackEntry.arguments?.getString("exerciseType"), ExerciseType.SQUAT
            )
            val modelVariant = parseEnumOrDefault(
                backStackEntry.arguments?.getString("modelVariant"), PoseModelVariant.FULL
            )
            VideoScreen(
                initialExerciseType = exerciseType,
                initialModelVariant = modelVariant,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenReport = { sessionId -> navController.navigate(Routes.report(sessionId)) },
            )
        }

        composable(
            route = Routes.REPORT,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            SessionReportScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
