package com.example.trailmind

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.trailmind.data.local.TokenStore
import com.example.trailmind.ui.auth.LoginScreen
import com.example.trailmind.ui.auth.RegisterScreen
import com.example.trailmind.ui.budget.BudgetScreen
import com.example.trailmind.ui.home.HomeScreenWithDrawer
import com.example.trailmind.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val BUDGET = "budget"
    const val TRIP_CHAT = "trip_chat"
    const val TRIP_WIZARD = "trip_wizard"
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }

    // go to a route and drop everything behind it, so Back can't return to login/splash
    fun goClearingStack(route: String) {
        navController.navigate(route) { popUpTo(0) { inclusive = true } }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    goClearingStack(if (tokenStore.getToken() != null) Routes.HOME else Routes.LOGIN)
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { goClearingStack(Routes.HOME) },
                onSignUpClick = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { goClearingStack(Routes.HOME) },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            val user = remember { tokenStore.getUser() }
            HomeScreenWithDrawer(
                userName = user?.name ?: "Traveller",
                userEmail = user?.email ?: "",
                onLogout = {
                    tokenStore.clear()
                    goClearingStack(Routes.LOGIN)
                },
                onBudgetClick = { navController.navigate(Routes.BUDGET) },
                onStartAiChat = { navController.navigate(Routes.TRIP_CHAT) },
                onStartWizard = { navController.navigate(Routes.TRIP_WIZARD) },
                onCategoryClick = { _ -> },
                onPlaceClick = { _ -> }
            )
        }

        composable(Routes.BUDGET) {
            BudgetScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TRIP_CHAT) {
            PlaceholderScreen("AI Chat Screen (Coming Soon)") { navController.popBackStack() }
        }

        composable(Routes.TRIP_WIZARD) {
            PlaceholderScreen("Guided Questions (Coming Soon)") { navController.popBackStack() }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}