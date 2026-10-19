package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object AttendanceCapture : Screen("attendance_capture/{type}") {
        fun createRoute(type: String) = "attendance_capture/$type"
    }
    object AttendanceHistory : Screen("attendance_history")
    object AttendanceDetail : Screen("attendance_detail/{id}") {
        fun createRoute(id: String) = "attendance_detail/$id"
    }
    object Profile : Screen("profile")
    object AdminDashboard : Screen("admin_dashboard")
}
