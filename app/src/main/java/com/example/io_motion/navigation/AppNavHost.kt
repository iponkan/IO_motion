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
import com.example.io_motion.core.common.util.parseEnumOrDefault
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.feature.diet.DietPlanScreen
import com.example.io_motion.feature.diet.DietScreen
import com.example.io_motion.feature.history.HistoryScreen
import com.example.io_motion.feature.history.SessionReportScreen
import com.example.io_motion.feature.home.HomeHubScreen
import com.example.io_motion.feature.workout.builder.WorkoutBuilderScreen
import com.example.io_motion.feature.workout.builder.WorkoutBuilderViewModel
import com.example.io_motion.feature.workout.list.WorkoutListScreen
import com.example.io_motion.feature.workout.run.WorkoutRunScreen
import com.example.io_motion.feature.live.HomeScreen
import com.example.io_motion.feature.live.LiveScreen
import com.example.io_motion.feature.live.settings.SettingsScreen
import com.example.io_motion.feature.video.VideoScreen

private object Routes {
    const val HOME       = "home"        // new hub (start destination)
    const val ASSESSMENT = "assessment"  // the former "home" exercise-picker (Motion Assessment setup)
    const val LIVE       = "live/{exerciseType}/{modelVariant}?target={target}&workoutRun={workoutRun}"
    const val VIDEO      = "video/{exerciseType}/{modelVariant}"
    const val HISTORY    = "history"
    const val REPORT     = "report/{sessionId}"
    const val SETTINGS   = "settings"
    const val WORKOUTS        = "workouts"
    const val WORKOUT_BUILDER = "workout-builder?workoutId={workoutId}"
    const val WORKOUT_RUN     = "workout-run/{workoutId}"   // guided runner (Phase 5)
    const val DIET            = "diet"                       // diet home (Phase 6)
    const val DIET_PLAN       = "diet-plan"                  // suggested meal plan (Phase 6)

    // Optional guided-run args default to "no target"/false so the assessment flow keeps building
    // the plain route; the runner passes a target and workoutRun=true.
    const val NO_TARGET = -1

    fun live(
        exerciseType: ExerciseType,
        modelVariant: PoseModelVariant,
        target: Int = NO_TARGET,
        workoutRun: Boolean = false,
    ) = "live/${exerciseType.name}/${modelVariant.name}?target=$target&workoutRun=$workoutRun"

    fun video(exerciseType: ExerciseType, modelVariant: PoseModelVariant) =
        "video/${exerciseType.name}/${modelVariant.name}"

    fun report(sessionId: Long) = "report/$sessionId"

    fun workoutBuilder(workoutId: Long? = null) =
        if (workoutId == null) "workout-builder" else "workout-builder?workoutId=$workoutId"

    fun workoutRun(workoutId: Long) = "workout-run/$workoutId"
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
            HomeHubScreen(
                onOpenAssessment = { navController.navigate(Routes.ASSESSMENT) },
                onOpenWorkouts = { navController.navigate(Routes.WORKOUTS) },
                onOpenDiet = { navController.navigate(Routes.DIET) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.ASSESSMENT) {
            HomeScreen(
                onStart = { exercise, model, mode ->
                    when (mode) {
                        AnalysisMode.LIVE    -> navController.navigate(Routes.live(exercise, model))
                        AnalysisMode.OFFLINE -> navController.navigate(Routes.video(exercise, model))
                    }
                },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.LIVE,
            arguments = listOf(
                // Not read here — LiveViewModel reads these directly from the nav backstack's
                // SavedStateHandle (see LiveViewModel), which avoids the initialize()-after-
                // construction race the old approach had.
                navArgument("exerciseType") { type = NavType.StringType },
                navArgument("modelVariant") { type = NavType.StringType },
                navArgument("target") { type = NavType.IntType; defaultValue = Routes.NO_TARGET },
                navArgument("workoutRun") { type = NavType.BoolType; defaultValue = false },
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

        composable(Routes.WORKOUTS) {
            WorkoutListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNewWorkout = { navController.navigate(Routes.workoutBuilder()) },
                onEditWorkout = { id -> navController.navigate(Routes.workoutBuilder(id)) },
                onStartRun = { id -> navController.navigate(Routes.workoutRun(id)) },
            )
        }

        composable(
            route = Routes.WORKOUT_BUILDER,
            arguments = listOf(
                navArgument(WorkoutBuilderViewModel.ARG_WORKOUT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            WorkoutBuilderScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.WORKOUT_RUN,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
        ) {
            WorkoutRunScreen(
                onNavigateBack = { navController.popBackStack() },
                onLaunchLive = { exerciseType, modelVariant, target ->
                    val variant = parseEnumOrDefault(modelVariant, PoseModelVariant.FULL)
                    navController.navigate(Routes.live(exerciseType, variant, target = target, workoutRun = true))
                },
                // DONE returns to the workout list (the runner's parent), skipping the run screen.
                onFinish = { navController.popBackStack(Routes.WORKOUTS, inclusive = false) },
            )
        }

        composable(Routes.DIET) {
            DietScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenMealPlan = { navController.navigate(Routes.DIET_PLAN) },
            )
        }

        composable(Routes.DIET_PLAN) {
            DietPlanScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
