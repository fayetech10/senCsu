package com.example.sencsu.navigation.tab

import com.example.sencsu.screen.ListCardDisponible
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.screen.DashboardScreen
import com.example.sencsu.screen.ListeAdherentScreen
import com.example.sencsu.screen.ProfileScreen
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(rootNavController: NavController, sessionManager: SessionManager) {
    val nestedNavController = rememberNavController()
    val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            ModernBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    nestedNavController.navigate(route) {
                        popUpTo(nestedNavController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = AppColors.AppBackground
    ) { padding ->
        NavHost(
            navController = nestedNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Home.route) {
                DashboardScreen(rootNavController = rootNavController)
            }
            composable(BottomNavItem.Listes.route) {
                ListeAdherentScreen(
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
                ProfileScreen(
                    onLogout = {
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ModernBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Listes,
        BottomNavItem.Cards,
        BottomNavItem.Profile
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(68.dp)
            .shadow(elevation = 20.dp, shape = AppShapes.LargeRadius, spotColor = AppColors.BrandBlue.copy(alpha = 0.15f)),
        shape = AppShapes.LargeRadius,
        color = AppColors.SurfaceBackground
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) AppColors.BrandBlue else AppColors.TextSub,
                    animationSpec = tween(250),
                    label = "color"
                )
                val iconSize by animateFloatAsState(
                    targetValue = if (isSelected) 22f else 24f,
                    animationSpec = spring(dampingRatio = 0.6f),
                    label = "iconSize"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 42.dp else 28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AppColors.BrandBlue.copy(alpha = 0.12f)
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = animatedColor,
                                modifier = Modifier.size(iconSize.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.BrandBlue,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}