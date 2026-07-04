package com.example.io_motion.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.feature.live.HomeScreen
import com.example.io_motion.feature.live.LiveScreen

private object Routes {
    const val HOME = "home"
    const val LIVE = "live/{exerciseType}/{modelVariant}"

    fun live(exerciseType: ExerciseType, modelVariant: PoseModelVariant) =
        "live/${exerciseType.name}/${modelVariant.name}"
}

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
                onStartLive = { exercise, model ->
                    navController.navigate(Routes.live(exercise, model))
                },
            )
        }

        composable(
            route = Routes.LIVE,
            arguments = listOf(
                navArgument("exerciseType") { type = NavType.StringType },
                navArgument("modelVariant") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val exerciseType = ExerciseType.valueOf(
                backStackEntry.arguments?.getString("exerciseType") ?: ExerciseType.SQUAT.name
            )
            val modelVariant = PoseModelVariant.valueOf(
                backStackEntry.arguments?.getString("modelVariant") ?: PoseModelVariant.FULL.name
            )
            LiveScreen(
                initialExerciseType = exerciseType,
                initialModelVariant = modelVariant,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
