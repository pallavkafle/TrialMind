package com.example.trailmind

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.trailmind.ui.map.MapViewModel
import com.example.trailmind.ui.map.MapScreen
import com.example.trailmind.ui.map.MapActions
import com.example.trailmind.ui.map.LocalMapActions
import androidx.compose.runtime.CompositionLocalProvider
import android.net.Uri
import android.widget.Toast
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
import com.example.trailmind.data.remote.SessionEvents
import com.example.trailmind.data.repository.AuthRepository
import com.example.trailmind.data.repository.SessionState
import com.example.trailmind.ui.auth.LoginScreen
import com.example.trailmind.ui.auth.RegisterScreen
import com.example.trailmind.ui.budget.BudgetScreen
import com.example.trailmind.ui.components.ComingSoonScreen
import com.example.trailmind.ui.currency.CurrencyScreen
import com.example.trailmind.ui.emergency.EmergencyScreen
import com.example.trailmind.ui.home.HomeScreenWithDrawer
import com.example.trailmind.ui.hotels.HotelsScreen
import com.example.trailmind.ui.permits.PermitsScreen
import com.example.trailmind.ui.places.PlaceDetailScreen
import com.example.trailmind.ui.places.PlacesScreen
import com.example.trailmind.ui.places.PlacesViewModel
import com.example.trailmind.ui.places.WriteReviewScreen
import com.example.trailmind.ui.plan.ItineraryScreen
import com.example.trailmind.ui.plan.PlanTripScreen
import com.example.trailmind.ui.plan.PlanTripViewModel
import com.example.trailmind.ui.splash.SplashScreen
import com.example.trailmind.ui.theme.StatusBarIcons
import com.example.trailmind.ui.trips.MyTripsScreen
import com.example.trailmind.ui.trips.SavedTripScreen
import com.example.trailmind.util.Resource
import com.example.trailmind.util.nationalityCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

// the splash stays at least this long, even when the login check finishes sooner
private const val MIN_SPLASH_MS = 1000L

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
    const val CURRENCY = "currency"
    const val MAP = "map"
    const val HOTELS = "hotels?destination={destination}"
    const val PLACES = "places/{category}"
    const val PLACE = "place/{name}"
    const val WRITE_REVIEW = "review/{name}"
    const val COMING_SOON = "soon/{name}"
    fun savedTrip(id: String) = "saved/$id"
    fun places(category: String) = "places/$category"
    // destination names contain spaces, slashes and brackets, so they must be encoded in the route
    fun place(name: String) = "place/${Uri.encode(name)}"
    fun writeReview(name: String) = "review/${Uri.encode(name)}"
    fun hotels(destination: String? = null) =
        if (destination.isNullOrBlank()) "hotels" else "hotels?destination=${Uri.encode(destination)}"
    fun comingSoon(name: String) = "soon/$name"
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore.get(context) }
    val authRepo = remember { AuthRepository(tokenStore) }
    val planVm: PlanTripViewModel = viewModel()
    val placesVm: PlacesViewModel = viewModel()
    val mapVm: MapViewModel = viewModel()
    val mapActions = remember(navController) {
        MapActions(
            showAll = { mapVm.showAll(); navController.navigate(Routes.MAP) },
            showRoute = { stops -> mapVm.showRoute(stops); navController.navigate(Routes.MAP) },
            focus = { name -> mapVm.focus(name); navController.navigate(Routes.MAP) }
        )
    }

    // go to a route and drop everything behind it, so Back can't return to login/splash
    fun goClearingStack(route: String) {
        navController.navigate(route) { popUpTo(0) { inclusive = true } }
    }

    // any protected request that answers 401 means the saved login expired: back to Login
    LaunchedEffect(Unit) {
        SessionEvents.unauthorized.collect {
            tokenStore.clear()
            if (navController.currentDestination?.route != Routes.LOGIN) {
                Toast.makeText(context, "Your session expired. Please log in again.", Toast.LENGTH_LONG).show()
                goClearingStack(Routes.LOGIN)
            }
        }
    }

    CompositionLocalProvider(LocalMapActions provides mapActions) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            // the login check and the minimum splash time run together, not one after the other
            LaunchedEffect(Unit) {
                val started = System.currentTimeMillis()
                val hadToken = withContext(Dispatchers.IO) { tokenStore.getToken() != null }
                val state = if (hadToken) {
                    // wait at most 3 seconds; if the server cannot be reached the user still gets in
                    withTimeoutOrNull(3000) { authRepo.validateSession() } ?: SessionState.UNKNOWN
                } else {
                    SessionState.LOGGED_OUT
                }
                val remaining = MIN_SPLASH_MS - (System.currentTimeMillis() - started)
                if (remaining > 0) delay(remaining)
                if (hadToken && state == SessionState.LOGGED_OUT) {
                    Toast.makeText(context, "Your session expired. Please log in again.", Toast.LENGTH_LONG).show()
                }
                goClearingStack(if (state == SessionState.LOGGED_OUT) Routes.LOGIN else Routes.HOME)
            }
            SplashScreen()
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
            // the Home header is white, so it needs dark status bar icons
            StatusBarIcons(darkIcons = !com.example.trailmind.ui.theme.isAppDark())
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
                        "currency" -> navController.navigate(Routes.CURRENCY)
                        "hotels" -> navController.navigate(Routes.hotels())
                        "places" -> navController.navigate(Routes.places("All"))
                        "trekking" -> navController.navigate(Routes.places("Trekking"))
                        else -> navController.navigate(Routes.comingSoon(route))
                    }
                },
                onPlaceClick = { _ -> }
            )
        }

        composable(Routes.BUDGET) {
            val nationality = remember { nationalityCategory(tokenStore.getUser()?.nationality) }
            BudgetScreen(
                onBack = { navController.popBackStack() },
                defaultNationality = nationality
            )
        }

        composable(Routes.TRIP_CHAT) {
            val nationality = remember { nationalityCategory(tokenStore.getUser()?.nationality) }
            PlanTripScreen(
                isChat = true,
                defaultNationality = nationality,
                onBack = { navController.popBackStack() },
                onResult = { navController.navigate(Routes.ITINERARY) },
                viewModel = planVm
            )
        }

        composable(Routes.TRIP_WIZARD) {
            val nationality = remember { nationalityCategory(tokenStore.getUser()?.nationality) }
            PlanTripScreen(
                isChat = false,
                defaultNationality = nationality,
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

        composable(Routes.MAP) {
            MapScreen(onBack = { navController.popBackStack() }, mapVm = mapVm, placesVm = placesVm)
        }

        composable(Routes.PERMITS) {
            val nationality = remember { nationalityCategory(tokenStore.getUser()?.nationality) }
            PermitsScreen(
                onBack = { navController.popBackStack() },
                defaultNationality = nationality
            )
        }

        composable(Routes.EMERGENCY) {
            EmergencyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CURRENCY) {
            CurrencyScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.HOTELS,
            arguments = listOf(
                navArgument("destination") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val destination = entry.arguments?.getString("destination")?.let { Uri.decode(it) }
            HotelsScreen(
                initialDestination = destination,
                onBack = { navController.popBackStack() },
                placesVm = placesVm
            )
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
                onFindHotels = { navController.navigate(Routes.hotels(name)) },
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
}
