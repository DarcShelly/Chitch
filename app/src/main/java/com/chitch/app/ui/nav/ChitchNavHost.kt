package com.chitch.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chitch.app.di.AppContainer
import com.chitch.app.di.ViewModelFactory
import com.chitch.app.ui.dailyreview.DailyReviewScreen
import com.chitch.app.ui.dailyreview.DailyReviewViewModel
import com.chitch.app.ui.habit.CreateHabitScreen
import com.chitch.app.ui.habit.CreateHabitViewModel
import com.chitch.app.ui.habit.EditHabitScreen
import com.chitch.app.ui.habit.EditHabitViewModel
import com.chitch.app.ui.habit.HabitsListScreen
import com.chitch.app.ui.habit.HabitsListViewModel
import com.chitch.app.ui.home.HomeScreen
import com.chitch.app.ui.home.HomeViewModel
import com.chitch.app.ui.onboarding.OnboardingScreen
import com.chitch.app.ui.routine.CreateRoutineScreen
import com.chitch.app.ui.routine.CreateRoutineViewModel
import com.chitch.app.ui.routine.EditRoutineScreen
import com.chitch.app.ui.routine.EditRoutineViewModel
import com.chitch.app.ui.routine.RoutinesListScreen
import com.chitch.app.ui.routine.RoutinesListViewModel
import com.chitch.app.ui.settings.SettingsScreen
import com.chitch.app.ui.settings.SettingsViewModel
import com.chitch.app.ui.stats.StatsScreen
import com.chitch.app.ui.stats.StatsViewModel
import com.chitch.app.ui.summary.SummaryScreen
import com.chitch.app.ui.summary.SummaryViewModel
import com.chitch.app.ui.task.CreateTaskScreen
import com.chitch.app.ui.task.CreateTaskViewModel
import com.chitch.app.ui.task.EditTaskScreen
import com.chitch.app.ui.task.EditTaskViewModel
import com.chitch.app.ui.task.TasksListScreen
import com.chitch.app.ui.task.TasksListViewModel

@Composable
fun ChitchNavHost(
    navController: NavHostController,
    container: AppContainer,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val factory = remember { ViewModelFactory(container) }
    val startDestination = remember {
        if (container.onboardingPreferences.hasSeenOnboarding()) Destinations.HOME else Destinations.ONBOARDING
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onGetStarted = {
                    container.onboardingPreferences.setSeenOnboarding()
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = viewModel<HomeViewModel>(factory = factory),
                searchQuery = searchQuery,
                onCreateTask = { navController.navigate(Destinations.CREATE_TASK) },
                onCreateRoutine = { navController.navigate(Destinations.CREATE_ROUTINE) },
                onCreateHabit = { navController.navigate(Destinations.CREATE_HABIT) },
                onEditHabit = { habitId -> navController.navigate("${Destinations.EDIT_HABIT}/$habitId") },
                onEditRoutine = { routineId -> navController.navigate("${Destinations.EDIT_ROUTINE}/$routineId") },
                onEditTask = { taskId -> navController.navigate("${Destinations.EDIT_TASK}/$taskId") }
            )
        }
        composable(Destinations.CREATE_HABIT) {
            CreateHabitScreen(
                viewModel = viewModel<CreateHabitViewModel>(factory = factory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Destinations.CREATE_TASK) {
            CreateTaskScreen(
                viewModel = viewModel<CreateTaskViewModel>(factory = factory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Destinations.CREATE_TASK_FOR_HABIT}/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
            val createFactory = remember(habitId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        CreateTaskViewModel(container.taskRepository, habitId = habitId) as T
                }
            }
            CreateTaskScreen(
                viewModel = viewModel<CreateTaskViewModel>(factory = createFactory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Destinations.CREATE_TASK_FOR_ROUTINE}/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
            val createFactory = remember(routineId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        CreateTaskViewModel(container.taskRepository, routineId = routineId) as T
                }
            }
            CreateTaskScreen(
                viewModel = viewModel<CreateTaskViewModel>(factory = createFactory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Destinations.CREATE_ROUTINE) {
            CreateRoutineScreen(
                viewModel = viewModel<CreateRoutineViewModel>(factory = factory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(viewModel = viewModel<SettingsViewModel>(factory = factory))
        }
        composable(Destinations.STATS) {
            StatsScreen(viewModel = viewModel<StatsViewModel>(factory = factory))
        }
        composable(Destinations.DAILY_REVIEW) {
            DailyReviewScreen(viewModel = viewModel<DailyReviewViewModel>(factory = factory))
        }
        composable(Destinations.SUMMARY) {
            SummaryScreen(viewModel = viewModel<SummaryViewModel>(factory = factory))
        }
        composable(Destinations.ROUTINES_LIST) {
            RoutinesListScreen(
                viewModel = viewModel<RoutinesListViewModel>(factory = factory),
                searchQuery = searchQuery,
                onCreateRoutine = { navController.navigate(Destinations.CREATE_ROUTINE) },
                onEditRoutine = { routineId -> navController.navigate("${Destinations.EDIT_ROUTINE}/$routineId") }
            )
        }
        composable(
            route = "${Destinations.EDIT_ROUTINE}/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
            val editFactory = remember(routineId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        EditRoutineViewModel(routineId, container.routineRepository, container.taskRepository) as T
                }
            }
            EditRoutineScreen(
                viewModel = viewModel<EditRoutineViewModel>(factory = editFactory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                onEditTask = { taskId -> navController.navigate("${Destinations.EDIT_TASK}/$taskId") },
                onAddNewTask = { navController.navigate("${Destinations.CREATE_TASK_FOR_ROUTINE}/$routineId") }
            )
        }
        composable(Destinations.HABITS) {
            HabitsListScreen(
                viewModel = viewModel<HabitsListViewModel>(factory = factory),
                searchQuery = searchQuery,
                onCreateHabit = { navController.navigate(Destinations.CREATE_HABIT) },
                onEditHabit = { habitId -> navController.navigate("${Destinations.EDIT_HABIT}/$habitId") }
            )
        }
        composable(
            route = "${Destinations.EDIT_HABIT}/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
            val editFactory = remember(habitId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        EditHabitViewModel(habitId, container.habitRepository, container.taskRepository) as T
                }
            }
            EditHabitScreen(
                viewModel = viewModel<EditHabitViewModel>(factory = editFactory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                onEditTask = { taskId -> navController.navigate("${Destinations.EDIT_TASK}/$taskId") },
                onAddNewTask = { navController.navigate("${Destinations.CREATE_TASK_FOR_HABIT}/$habitId") }
            )
        }
        composable(Destinations.TASKS_LIST) {
            TasksListScreen(
                viewModel = viewModel<TasksListViewModel>(factory = factory),
                searchQuery = searchQuery,
                onCreateTask = { navController.navigate(Destinations.CREATE_TASK) },
                onEditTask = { taskId -> navController.navigate("${Destinations.EDIT_TASK}/$taskId") }
            )
        }
        composable(
            route = "${Destinations.EDIT_TASK}/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            val editFactory = remember(taskId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        EditTaskViewModel(
                            taskId,
                            container.taskRepository,
                            container.routineRepository,
                            container.habitRepository
                        ) as T
                }
            }
            EditTaskScreen(
                viewModel = viewModel<EditTaskViewModel>(factory = editFactory),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
