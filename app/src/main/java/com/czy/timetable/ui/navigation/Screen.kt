package com.czy.timetable.ui.navigation

sealed class Screen(val route: String) {
    object Schedule : Screen("schedule")
    object Calendar : Screen("calendar")
}
