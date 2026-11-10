package com.example.trailmind

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.trailmind.data.local.TokenStore
import com.example.trailmind.ui.auth.LoginScreen
import com.example.trailmind.ui.auth.RegisterScreen
import com.example.trailmind.ui.budget.BudgetScreen
import com.example.trailmind.ui.components.ComingSoonScreen
import com.example.trailmind.ui.emergency.EmergencyScreen
import com.example.trailmind.ui.home.HomeScreenWithDrawer
import com.example.trailmind.ui.permits.PermitsScreen
import com.example.trailmind.ui.places.PlaceDetailScreen
import com.example.trailmind.ui.places.PlacesScreen
import com.example.trailmind.ui.places.PlacesViewModel
import com.example.trailmind.ui.places.WriteReviewScreen
import com.example.trailmind.ui.plan.ItineraryScreen
import com.example.trailmind.ui.plan.PlanTripScreen
import com.example.trailmind.ui.plan.PlanTripViewModel
import com.example.trailmind.ui.splash.SplashScreen
import com.example.trailmind.ui.trips.MyTripsScreen
import com.example.trailmind.ui.trips.SavedTripScreen
import com.example.trailmind.util.Resource

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val BUDGET = "budget"
    const val TRIP_CHAT = "trip_chat"
    const val TRIP_WIZARD = "trip_wizard"
    const val ITINERARY = "itinerary"
    const val MY_TRIPS = "my_trips"
    const val SAVED_TRIP = "saved/{id}"
    const val PERMITS = "permits"
    const val EMERGENCY = "emergency"
    const val PLACES = "places/{category}"
    const val PLACE = "place/{name}"
    const val WRITE_REVIEW = "review/{name}"
    const val COMING_SOON = "soon/{name}"
    fun savedTrip(id: String) = "saved/$id"
    fun places(category: String) = "places/$category"
    // destination names contain spaces, slashes and brackets, so they must be encoded in the route
    fun place(name: String) = "place/${Uri.encode(name)}"
    fun writeReview(name: String) = "review/${Uri.encode(name)}"
    fun comingSoon(name: String) = "soon/$name"
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val planVm: PlanTripViewModel = viewModel()
    val placesVm: PlacesViewModel = viewModel()

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
                onMyTripsClick = { navController.navigate(Routes.MY_TRIPS) },
                onStartAiChat = { navController.navigate(Routes.TRIP_CHAT) },
                onStartWizard = { navController.navigate(Routes.TRIP_WIZARD) },
                onCategoryClick = { route ->
                    when (route) {
                        "permits" -> navController.navigate(Routes.PERMITS)
                        "emergency" -> navController.navigate(Routes.EMERGENCY)
                        "places" -> navController.navigate(Routes.places("All"))
                        "trekking" -> navController.navigate(Routes.places("Trekking"))
                        else -> navController.navigate(Routes.comingSoon(route))
                    }
                },
                onPlaceClick = { _ -> }
            )
        }

        composable(Routes.BUDGET) {
            BudgetScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TRIP_CHAT) {
            PlanTripScreen(
                isChat = true,
                onBack = { navController.popBackStack() },
                onResult = { navController.navigate(Routes.ITINERARY) },
                viewModel = planVm
            )
        }

        composable(Routes.TRIP_WIZARD) {
            PlanTripScreen(
                isChat = false,
                onBack = { navController.popBackStack() },
                onResult = { navController.navigate(Routes.ITINERARY) },
                viewModel = planVm
            )
        }

        composable(Routes.ITINERARY) {
            // keep our own copy so the screen doesn't flicker when the plan screen clears its state
            val data = remember { (planVm.itinerary.value as? Resource.Success)?.data }
            val request = remember { planVm.lastRequest }
            if (data == null || request == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ItineraryScreen(
                    data = data,
                    request = request,
                    onBack = { navController.popBackStack() },
                    viewModel = planVm
                )
            }
        }

        composable(Routes.MY_TRIPS) {
            MyTripsScreen(
                onBack = { navController.popBackStack() },
                onOpenTrip = { id -> navController.navigate(Routes.savedTrip(id)) }
            )
        }

        composable(
            Routes.SAVED_TRIP,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            SavedTripScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
                planVm = planVm
            )
        }

        composable(Routes.PERMITS) {
            PermitsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.EMERGENCY) {
            EmergencyScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.PLACES,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { entry ->
            PlacesScreen(
                initialCategory = entry.arguments?.getString("category").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpen = { name -> navController.navigate(Routes.place(name)) },
                viewModel = placesVm
            )
        }

        composable(
            Routes.PLACE,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = Uri.decode(entry.arguments?.getString("name").orEmpty())
            PlaceDetailScreen(
                name = name,
                onBack = { navController.popBackStack() },
                onWriteReview = { navController.navigate(Routes.writeReview(name)) },
                placesVm = placesVm
            )
        }

        composable(
            Routes.WRITE_REVIEW,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = Uri.decode(entry.arguments?.getString("name").orEmpty())
            WriteReviewScreen(
                name = name,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            Routes.COMING_SOON,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            ComingSoonScreen(
                name = entry.arguments?.getString("name").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}