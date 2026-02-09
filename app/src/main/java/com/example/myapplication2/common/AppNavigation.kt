package com.example.myapplication2.common

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication2.login.LoginScreen
import com.example.myapplication2.wallet.SendTransactionScreen
import com.example.myapplication2.wallet.WalletDetailsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate("wallet") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("wallet") {
            WalletDetailsScreen(
                onSendClick = { navController.navigate("send") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("send") {
            SendTransactionScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
