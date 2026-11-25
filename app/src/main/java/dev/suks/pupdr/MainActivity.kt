package dev.suks.pupdr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
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
import dev.suks.pupdr.ui.screens.ExercisesScreen
import dev.suks.pupdr.ui.screens.ReportsScreen
import dev.suks.pupdr.ui.screens.RoutineDetailScreen
import dev.suks.pupdr.ui.screens.RoutinesScreen
import dev.suks.pupdr.ui.screens.WorkoutScreen
import dev.suks.pupdr.ui.theme.PupdrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as PupdrApp
        setContent {
            PupdrTheme {
                val ready by app.ready.collectAsState()
                if (ready) PupdrRoot(app)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Workout("workout", "Workout", Icons.Default.Today),
    Exercises("exercises", "Exercises", Icons.Default.FitnessCenter),
    Routines("routines", "Routines", Icons.AutoMirrored.Filled.List),
    Reports("reports", "Reports", Icons.Default.Insights),
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
    val current = backStack?.destination?.route

    fun goTab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (Tab.entries.any { it.route == current }) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = current == t.route,
                            onClick = { goTab(t.route) },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = Tab.Workout.route, modifier = Modifier.padding(bottom = pad.calculateBottomPadding())) {
            composable(Tab.Workout.route) { WorkoutScreen(workoutVm, exercisesVm) }
            composable(Tab.Exercises.route) {
                ExercisesScreen(exercisesVm, routines) { id -> workoutVm.addExercises(listOf(id)); messages.send("Added to ${workoutVm.day.value}") }
            }
            composable(Tab.Routines.route) {
                RoutinesScreen(routinesVm, onOpen = { nav.navigate("routine/$it") }, onStarted = {
                    workoutVm.setDay(java.time.LocalDate.now())
                    goTab(Tab.Workout.route)
                })
            }
            composable(Tab.Reports.route) { ReportsScreen(reportsVm) }
            composable("routine/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: RoutineDetailViewModel = viewModel(key = "routine-$id") { RoutineDetailViewModel(app, id) }
                RoutineDetailScreen(vm, exercisesVm, onBack = { nav.popBackStack() })
            }
        }
    }
}
