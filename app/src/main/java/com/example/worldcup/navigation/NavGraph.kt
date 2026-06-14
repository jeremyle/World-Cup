package com.example.worldcup.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.worldcup.ui.screens.home.HomeScreen

@Composable
fun NavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.HOME.route,
    ) {
        composable(Screen.HOME.route) {
            HomeScreen()
        }
    }
}