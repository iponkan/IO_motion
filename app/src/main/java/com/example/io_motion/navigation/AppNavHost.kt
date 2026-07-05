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

/**
 * Parses [value] as a constant of enum [T], falling back to [default] for null, blank, or
 * unrecognized values (e.g. a stale deep link or restored backstack referencing a renamed
 * constant) instead of throwing and crashing navigation.
 */
private inline fun <reified T : Enum<T>> parseEnumArg(value: String?, default: T): T =
    value?.let { raw -> enumValues<T>().firstOrNull { it.name == raw } } ?: default

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
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
            )
        }

        composable(
            route = Routes.LIVE,
            arguments = listOf(
                navArgument("exerciseType") { type = NavType.StringType },
                navArgument("modelVariant") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val exerciseType = parseEnumArg(
                backStackEntry.arguments?.getString("exerciseType"), ExerciseType.SQUAT
            )
            val modelVariant = parseEnumArg(
                backStackEntry.arguments?.getString("modelVariant"), PoseModelVariant.FULL
            )
            LiveScreen(
                initialExerciseType = exerciseType,
                initialModelVariant = modelVariant,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.VIDEO,
            arguments = listOf(
                navArgument("exerciseType") { type = NavType.StringType },
                navArgument("modelVariant") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val exerciseType = parseEnumArg(
                backStackEntry.arguments?.getString("exerciseType"), ExerciseType.SQUAT
            )
            val modelVariant = parseEnumArg(
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
