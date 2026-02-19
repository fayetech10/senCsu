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
import com.example.sencsu.navigation.tab.MainScreen
import com.example.sencsu.screen.AdherentDetailsScreen
import com.example.sencsu.screen.LoginScreen
import com.example.sencsu.screen.Paiement
import com.example.sencsu.screen.SearchScreen
import com.example.sencsu.screen.SplashScreen
import com.example.sencsu.screen.forms.ClassiqueForm
import com.example.sencsu.screen.forms.DaraForm

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
                onNavigateToPayment = { id, total -> navController.navigate("payments/$id/$total") },
                agentId = viewModel.agentId.collectAsState().value
            )
        }

        composable("adherent_details/{id}") {
            AdherentDetailsScreen(onNavigateBack = { navController.popBackStack() }, sessionManager = viewModel.sessionManager)
        }

        composable("search") {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onAdherentClick = { adherentId ->
                    navController.navigate("adherent_details/$adherentId")
                }
            )
        }

        composable(
            route = "payments/{adherentId}/{montantTotal}",
            arguments = listOf(
                navArgument("adherentId") { type = NavType.StringType },
                navArgument("montantTotal") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val adherentId = backStackEntry.arguments?.getString("adherentId")?.toLongOrNull()
            val montantTotal = backStackEntry.arguments?.getString("montantTotal")?.toDoubleOrNull()
            Paiement(adherentId = adherentId, montantTotal = montantTotal,navController = navController)
        }

        composable(
            route = "adherent_details/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) {
            AdherentDetailsScreen(onNavigateBack = { navController.popBackStack() }, sessionManager = viewModel.sessionManager)
        }
    }
    }
