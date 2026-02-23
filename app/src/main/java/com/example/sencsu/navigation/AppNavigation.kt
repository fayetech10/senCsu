package com.example.sencsu.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sencsu.domain.viewmodel.AppNavigationViewModel
import com.example.sencsu.components.AddAdherentScreen
import com.example.sencsu.navigation.tab.MainScreen
import com.example.sencsu.screen.AdherentDetailsScreen
import com.example.sencsu.screen.LoginScreen
import com.example.sencsu.screen.Paiement
import com.example.sencsu.screen.SearchScreen
import com.example.sencsu.screen.SplashScreen
import com.example.sencsu.screen.SyncScreen
import com.example.sencsu.screen.forms.ClassiqueForm
import com.example.sencsu.screen.forms.DaraForm
import com.example.sencsu.domain.viewmodel.AddAdherentViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(viewModel: AppNavigationViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            navController.navigate("login") {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onNavigateToLogin = { navController.navigate("login") { popUpTo("splash") { inclusive = true } } },
                onNavigateToDashboard = { navController.navigate("main_tabs") { popUpTo("splash") { inclusive = true } } }
            )
        }

        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("main_tabs") { popUpTo("login") { inclusive = true } }
            })
        }
        composable("form_dara"){
            DaraForm(
                rootNavController = navController
            )
        }
        composable("classique"){
            ClassiqueForm(
                rootNavController = navController
            )
        }

        // LE CONTENEUR DES ONGLETS
        composable("main_tabs") {
            MainScreen(rootNavController = navController, sessionManager = viewModel.sessionManager)
        }

        // Écrans "Full Screen" (qui cachent la barre de navigation, comme Facebook)
        composable("add_adherent") {
            AddAdherentScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPayment = { id, localId, total -> 
                    val remoteIdArg = id ?: -1L
                    val localIdArg = localId ?: -1L
                    navController.navigate("payments/$remoteIdArg/$localIdArg/$total") 
                },
                agentId = viewModel.agentId.collectAsState().value
            )
        }

        composable(
            route = "adherent_details/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) {
            AdherentDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { adherentId ->
                    navController.navigate("edit_adherent/$adherentId")
                },
            )
        }

        composable(
            route = "edit_adherent/{adherentId}",
            arguments = listOf(
                navArgument("adherentId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val adherentId = backStackEntry.arguments?.getLong("adherentId")
            val editViewModel: AddAdherentViewModel = hiltViewModel()

            // Fetch the adherent and pre-fill the form once
            LaunchedEffect(adherentId) {
                if (adherentId != null) {
                    editViewModel.fetchAndLoadForEdit(adherentId)
                }
            }

            AddAdherentScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPayment = { _, _, _ ->
                    // Not used in edit mode
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
                agentId = viewModel.agentId.collectAsState().value,
                viewModel = editViewModel
            )
        }

        composable("search") {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onAdherentClick = { adherentId ->
                    navController.navigate("adherent_details/$adherentId")
                }
            )
        }

        composable("sync_screen") {
            SyncScreen(navController = navController)
        }

        composable(
            route = "payments/{adherentId}/{localAdherentId}/{montantTotal}",
            arguments = listOf(
                navArgument("adherentId") { type = NavType.LongType },
                navArgument("localAdherentId") { type = NavType.LongType },
                navArgument("montantTotal") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val adherentId = backStackEntry.arguments?.getLong("adherentId")?.takeIf { it != -1L }
            val localAdherentId = backStackEntry.arguments?.getLong("localAdherentId")?.takeIf { it != -1L }
            val montantTotal = backStackEntry.arguments?.getString("montantTotal")?.toDoubleOrNull()
            Paiement(adherentId = adherentId, localAdherentId = localAdherentId, montantTotal = montantTotal, navController = navController)
        }

    }
}
