package com.example.sencsu.navigation.tab

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("dashboard_content", Icons.Rounded.Home, "Accueil")
    object Listes : BottomNavItem("liste_adherents", Icons.AutoMirrored.Rounded.FormatListBulleted, "Liste")
    object Cards : BottomNavItem("liste_cartes", Icons.Rounded.Badge, "Cartes")
    object Profile : BottomNavItem("profile", Icons.Rounded.Person, "Profil")
}