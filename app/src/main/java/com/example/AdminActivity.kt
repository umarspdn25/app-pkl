package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.AttendanceDetailScreen
import com.example.ui.theme.PklTsmTheme
import com.example.ui.viewmodel.MainViewModel

class AdminActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PklTsmTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AdminAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AdminAppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isAdminLoggedIn) "admin_dashboard" else "admin_login"
    ) {
        composable("admin_login") {
            AdminLoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("admin_dashboard") {
                        popUpTo("admin_login") { inclusive = true }
                    }
                }
            )
        }

        composable("admin_dashboard") {
            AdminDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.logoutAdmin {
                        navController.navigate("admin_login") {
                            popUpTo("admin_dashboard") { inclusive = true }
                        }
                    }
                },
                onNavigateToDetail = { id ->
                    navController.navigate("admin_attendance_detail/$id")
                }
            )
        }

        composable(
            route = "admin_attendance_detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            AttendanceDetailScreen(
                attendanceId = id,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
