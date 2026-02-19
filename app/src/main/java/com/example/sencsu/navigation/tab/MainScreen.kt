package com.example.sencsu.navigation.tab

import ListCardDisponible
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.screen.DashboardScreen
import com.example.sencsu.screen.ListeAdherentScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(rootNavController: NavController, sessionManager: SessionManager) {
    val nestedNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Listes,
                    BottomNavItem.Cards,
                    BottomNavItem.Profile
                )

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            nestedNavController.navigate(item.route) {
                                // Évite de recréer la pile d'onglets (comportement standard Android)
                                popUpTo(nestedNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nestedNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Home.route) {
                // On passe le rootNavController pour pouvoir naviguer vers "add_adherent" (hors tabs)
                DashboardScreen(rootNavController = rootNavController)
            }
            composable(BottomNavItem.Listes.route) {
                ListeAdherentScreen(
                    onNavigateBack = { nestedNavController.popBackStack() },
                    onAdherentClick = { id -> rootNavController.navigate("adherent_details/$id") },
                    sessionManager = sessionManager
                )
            }
            composable(BottomNavItem.Cards.route) {
                ListCardDisponible(
                    onNavigateBack = { nestedNavController.popBackStack() },
                    onAdherentClick = { id -> rootNavController.navigate("adherent_details/$id") },
                    sessionManager = sessionManager
                )
            }
            composable(BottomNavItem.Profile.route) {
                // Ton écran de profil ici
            }
        }
    }
}