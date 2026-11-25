package dev.suks.pupdr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.Messages
import dev.suks.pupdr.ui.ReportsViewModel
import dev.suks.pupdr.ui.RoutineDetailViewModel
import dev.suks.pupdr.ui.RoutinesViewModel
import dev.suks.pupdr.ui.WorkoutViewModel
import dev.suks.pupdr.ui.components.PupLogo
import dev.suks.pupdr.ui.screens.ExercisesScreen
import dev.suks.pupdr.ui.screens.ReportsScreen
import dev.suks.pupdr.ui.screens.RoutineDetailScreen
import dev.suks.pupdr.ui.screens.RoutinesScreen
import dev.suks.pupdr.ui.screens.WorkoutScreen
import dev.suks.pupdr.ui.theme.Motion
import dev.suks.pupdr.ui.theme.PupdrTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as PupdrApp
        setContent {
            val dynamic by app.prefs.dynamicColor.collectAsState()
            PupdrTheme(dynamic = dynamic) {
                val ready by app.ready.collectAsState()
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Crossfade(ready, animationSpec = Motion.standard(Motion.LONG), label = "boot") { isReady ->
                        if (isReady) PupdrRoot(app)
                        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PupLogo(96.dp) }
                    }
                }
            }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Workout("workout", "Workout", Icons.Outlined.Today, Icons.Default.Today),
    Exercises("exercises", "Exercises", Icons.Outlined.FitnessCenter, Icons.Default.FitnessCenter),
    Routines("routines", "Routines", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
    Reports("reports", "Reports", Icons.Outlined.Insights, Icons.Default.Insights),
}

@Composable
private fun PupdrRoot(app: PupdrApp) {
    val messages = remember { Messages() }
    val workoutVm: WorkoutViewModel = viewModel { WorkoutViewModel(app, messages) }
    val exercisesVm: ExercisesViewModel = viewModel { ExercisesViewModel(app, messages) }
    val routinesVm: RoutinesViewModel = viewModel { RoutinesViewModel(app, messages) }
    val reportsVm: ReportsViewModel = viewModel { ReportsViewModel(app) }
    val routines by routinesVm.routines.collectAsState()

    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { messages.flow.collect { snackbar.showSnackbar(it) } }
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val onTab = Tab.entries.any { t -> destination?.hierarchy?.any { it.route == t.route } == true }

    fun goTab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    fun startedToday() {
        workoutVm.setDay(LocalDate.now())
        goTab(Tab.Workout.route)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(
                visible = onTab,
                enter = slideInVertically(Motion.enter()) { it } + fadeIn(Motion.enter()),
                exit = slideOutVertically(Motion.exit()) { it } + fadeOut(Motion.exit()),
            ) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        val selected = destination?.hierarchy?.any { it.route == t.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { goTab(t.route) },
                            icon = { Icon(if (selected) t.selectedIcon else t.icon, null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Tab.Workout.route,
            modifier = Modifier.padding(pad).consumeWindowInsets(pad).imePadding(),
            // tabs: Material "fade through"
            enterTransition = { fadeIn(Motion.enter(Motion.MEDIUM, delay = 60)) + scaleIn(Motion.enter(Motion.MEDIUM, delay = 60), initialScale = 0.94f) },
            exitTransition = { fadeOut(Motion.exit(90)) },
            popEnterTransition = { fadeIn(Motion.enter(Motion.MEDIUM, delay = 60)) + scaleIn(Motion.enter(Motion.MEDIUM, delay = 60), initialScale = 0.94f) },
            popExitTransition = { fadeOut(Motion.exit(90)) },
        ) {
            composable(Tab.Workout.route) { WorkoutScreen(workoutVm, exercisesVm) }
            composable(Tab.Exercises.route) {
                ExercisesScreen(exercisesVm, routines) { id ->
                    workoutVm.addExercises(listOf(id))
                    messages.send("Added to ${if (workoutVm.day.value == LocalDate.now()) "today" else workoutVm.day.value.toString()}")
                }
            }
            composable(Tab.Routines.route) {
                RoutinesScreen(
                    routinesVm,
                    onOpen = { nav.navigate("routine/$it") },
                    onCreated = { nav.navigate("routine/$it?new=true") },
                    onStarted = ::startedToday,
                )
            }
            composable(Tab.Reports.route) { ReportsScreen(reportsVm) }
            composable(
                "routine/{id}?new={new}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("new") { type = NavType.BoolType; defaultValue = false },
                ),
                // detail: Material "shared axis X"
                enterTransition = { slideInHorizontally(Motion.enter(Motion.LONG)) { it / 5 } + fadeIn(Motion.enter(Motion.MEDIUM)) },
                exitTransition = { slideOutHorizontally(Motion.exit(Motion.MEDIUM)) { it / 5 } + fadeOut(Motion.exit()) },
                popExitTransition = { slideOutHorizontally(Motion.exit(Motion.MEDIUM)) { it / 5 } + fadeOut(Motion.exit()) },
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: RoutineDetailViewModel = viewModel(key = "routine-$id") { RoutineDetailViewModel(app, id) }
                val isNew = entry.arguments?.getBoolean("new") ?: false
                RoutineDetailScreen(vm, exercisesVm, isNew, onBack = { nav.popBackStack() }) { rid, name ->
                    routinesVm.startToday(rid, name) { startedToday() }
                }
            }
        }
    }
}
