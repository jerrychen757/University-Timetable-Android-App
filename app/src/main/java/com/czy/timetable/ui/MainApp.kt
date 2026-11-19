package com.czy.timetable.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.czy.timetable.BaseApplication
import com.czy.timetable.SettingsManager
import com.czy.timetable.calendar.CalendarScreen
import com.czy.timetable.calendar.CalendarViewModel
import com.czy.timetable.ui.navigation.Screen
import com.czy.timetable.ScheduleMainScreen
import com.czy.timetable.ScheduleViewModel

@Composable
fun MainApp(
    scheduleViewModel: ScheduleViewModel,
    settingsManager: SettingsManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "課表") },
                    label = { Text("課表") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Schedule.route } == true,
                    onClick = {
                        navController.navigate(Screen.Schedule.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Event, contentDescription = "行事曆") },
                    label = { Text("行事曆") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Calendar.route } == true,
                    onClick = {
                        navController.navigate(Screen.Calendar.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Schedule.route
            ) {
                composable(Screen.Schedule.route) {
                    ScheduleMainScreen(
                        viewModel = scheduleViewModel,
                        settingsManager = settingsManager
                    )
                }
                composable(Screen.Calendar.route) {
                    val appContainer = (LocalContext.current.applicationContext as BaseApplication).container
                    val calendarViewModel: CalendarViewModel = viewModel(
                        factory = CalendarViewModel.provideFactory(appContainer.courseDao)
                    )
                    CalendarScreen(
                        viewModel = calendarViewModel
                    )
                }
            }
        }
    }
}
