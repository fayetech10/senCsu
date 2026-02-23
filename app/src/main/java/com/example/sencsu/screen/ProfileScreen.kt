package com.example.sencsu.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sencsu.domain.viewmodel.DashboardViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val agent = authState.user

    var showContent by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    val initials = remember(agent) {
        val p = agent?.prenom?.firstOrNull()?.uppercase() ?: ""
        val n = agent?.name?.firstOrNull()?.uppercase() ?: ""
        "$p$n".ifEmpty { "?" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppColors.AppBackground)
    ) {
        // En-tête avec gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(AppColors.BrandBlue, Color(0xFF1A5CC8))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Cercle décoratif
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            )

            if (showContent) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        initials,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AppColors.BrandBlue
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${agent?.prenom ?: ""} ${agent?.name ?: ""}".trim().ifEmpty { "Agent" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        agent?.role?.replaceFirstChar { it.uppercase() } ?: "Agent terrain",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Card d'info rapide
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, delayMillis = 150)) + slideInVertically(
                tween(300, delayMillis = 150),
                initialOffsetY = { 40 }
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                shape = AppShapes.LargeRadius,
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileInfoRow(
                        icon = Icons.Rounded.Email,
                        label = "Email",
                        value = agent?.email ?: "N/A"
                    )
                    HorizontalDivider(color = AppColors.BorderColor.copy(alpha = 0.3f))
                    ProfileInfoRow(
                        icon = Icons.Rounded.Phone,
                        label = "Téléphone",
                        value = agent?.telephone ?: "N/A"
                    )
                    HorizontalDivider(color = AppColors.BorderColor.copy(alpha = 0.3f))
                    ProfileInfoRow(
                        icon = Icons.Rounded.Badge,
                        label = "Identifiant",
                        value = "#${agent?.id ?: "—"}"
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Section paramètres
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, delayMillis = 250)) + slideInVertically(
                tween(300, delayMillis = 250),
                initialOffsetY = { 30 }
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Paramètres",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextMain,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = "À propos",
                    subtitle = "Version 1.0.0"
                )

                SettingsItem(
                    icon = Icons.Rounded.Security,
                    title = "Confidentialité",
                    subtitle = "Gestion des données"
                )

                SettingsItem(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = "Aide & Support",
                    subtitle = "Centre d'aide"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Bouton déconnexion
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, delayMillis = 350))
        ) {
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = AppShapes.MediumRadius,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.StatusRed.copy(alpha = 0.1f),
                    contentColor = AppColors.StatusRed
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Se déconnecter", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))

        // Mention copyright
        Text(
            "SenCSU © 2025\nCouverture Santé Universelle",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextDisabled,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
    }

    // Dialog de confirmation de déconnexion
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Déconnexion", fontWeight = FontWeight.Bold) },
            text = { Text("Êtes-vous sûr de vouloir vous déconnecter ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.StatusRed)
                ) {
                    Text("Déconnecter", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler")
                }
            },
            containerColor = AppColors.SurfaceBackground
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = AppColors.BrandBlue.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = AppColors.BrandBlue, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSub
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextMain
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.MediumRadius,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AppColors.SurfaceAlt,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = AppColors.TextSub, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextMain
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSub
                )
            }
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = AppColors.TextSub.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
