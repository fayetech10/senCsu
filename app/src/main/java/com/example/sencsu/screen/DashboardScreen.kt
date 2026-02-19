package com.example.sencsu.screen

import com.example.sencsu.components.AdherentChartCard
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sencsu.components.RecentActivitiesSection
import com.example.sencsu.data.remote.dto.AgentDto
import com.example.sencsu.data.remote.dto.DashboardResponseDto
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.domain.viewmodel.DashboardViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppElevation
import com.example.sencsu.theme.AppShapes

// ==================== MAIN SCREEN ====================
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    rootNavController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    var isMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColors.AppBackground,
        floatingActionButton = {
            ExpandableFAB(
                expanded = isMenuExpanded,
                onExpandedChange = { isMenuExpanded = it },
                onActionClick = { route ->
                    isMenuExpanded = false
                    rootNavController.navigate(route)
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = dashboardState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Crossfade(targetState = dashboardState, label = "state_transition") { state ->
                when {
                    state.isLoading && state.data == null -> LoadingState()
                    state.error != null -> ErrorState(state.error) { viewModel.refresh() }
                    state.data != null -> DashboardContent(state.data, authState.user, rootNavController, viewModel.sessionManager)
                    else -> EmptyState()
                }
            }
        }
    }
}

// ==================== EXPANDABLE FAB (WHATSAPP STYLE) ====================
@Composable
fun ExpandableFAB(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onActionClick: (String) -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically() + scaleIn(),
            exit = fadeOut() + shrinkVertically() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniFabAction(letter = "C", label = "Classique", color = Color(0xFF6366F1)) { onActionClick("classique") }
                MiniFabAction(letter = "D", label = "Dara", color = Color(0xFFF59E0B)) { onActionClick("form_dara") }
                MiniFabAction(letter = "A", label = "Adhérent", color = AppColors.BrandBlue) { onActionClick("add_adherent") }
            }
        }

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = if (expanded) AppColors.SurfaceBackground else AppColors.BrandBlue,
            contentColor = if (expanded) AppColors.BrandBlue else Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(AppElevation.fab)
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.rotate(rotation).size(28.dp)
            )
        }
    }
}

@Composable
fun MiniFabAction(
    letter: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.7f),
        ) {
            Text(
                text = label,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Text(
                text = letter,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DashboardContent(
    data: DashboardResponseDto,
    agent: AgentDto?,
    navController: NavController,
    sessionManager: SessionManager
) {
    val adherents = remember { data.data }
    val totalAmount = remember { adherents.sumOf { it.montantTotal ?: 0.0 } }
    val activeMembers = remember { adherents.count { it.actif == true } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { HeaderSection(agent) { navController.navigate("profile") } }
        item { HeroBalanceCard(totalAmount) }
        item { ModernSearchBar { navController.navigate("search") } }
        item { StatsBentoGrid(adherents.size, activeMembers) }
        item { AdherentChartCard(adherents) }
        item {
            RecentActivitiesSection(
                adherents = adherents.sortedByDescending { it.createdAt }.take(5),
                onAdherentClick = { id -> navController.navigate("adherent_details/$id") },
                onSeeAllClick = { },
                sessionManager = sessionManager
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun HeaderSection(agent: AgentDto?, onProfileClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column {
            Text("Tableau de bord", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSub)
            Text(agent?.prenom ?: "Agent", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = AppColors.TextMain))
        }
        Surface(
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onProfileClick() },
            color = AppColors.BrandBlueLite,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) { 
                Icon(Icons.Rounded.Person, contentDescription = null, tint = AppColors.BrandBlue) 
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun HeroBalanceCard(amount: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, AppShapes.LargeRadius, spotColor = AppColors.BrandBlue.copy(0.2f)), 
        shape = AppShapes.LargeRadius, 
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(AppColors.BrandBlue, AppColors.BrandBlueDark.copy(alpha = 0.8f))
                    )
                )
                .padding(24.dp)
        ) {
            // Un petit cercle décoratif en fond
             Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(18.dp))
                    Text("Total Collecté", color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                // Formattage plus robuste
                val formatted = try {
                    String.format("%,.0f", amount)
                } catch (e: Exception) {
                    amount.toString()
                }
                Text(
                    "$formatted FCFA", 
                    color = Color.White, 
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                )
            }
        }
    }
}

@Composable
private fun ModernSearchBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() },
        shape = AppShapes.MediumRadius,
        color = AppColors.SurfaceBackground,
        border = BorderStroke(1.dp, AppColors.BorderColor),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Search, null, tint = AppColors.TextSub)
            Text("Rechercher un adhérent...", color = AppColors.TextSub, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatsBentoGrid(total: Int, active: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BentoCard(Modifier.weight(1f), "Membres Total", "$total", Icons.Rounded.People, AppColors.BrandBlueLite, AppColors.BrandBlue)
        BentoCard(Modifier.weight(1f), "Membres Actifs", "$active", Icons.Rounded.Verified, AppColors.SuccessLight, AppColors.StatusGreenDark)
    }
}

@Composable
private fun BentoCard(
    modifier: Modifier, 
    label: String, 
    value: String, 
    icon: ImageVector,
    bgColor: Color,
    tintColor: Color
) {
    Surface(
        modifier = modifier, 
        shape = AppShapes.LargeRadius, 
        color = AppColors.SurfaceBackground, 
        border = BorderStroke(1.dp, AppColors.BorderColor.copy(alpha=0.5f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tintColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = AppColors.TextMain)
            Text(label, color = AppColors.TextSub, style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable private fun LoadingState() { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = AppColors.BrandBlue) } }
@Composable private fun EmptyState() { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Vide", color = AppColors.TextDisabled) } }
@Composable private fun ErrorState(e: String, onRetry: () -> Unit) { 
    Column(
        Modifier.fillMaxSize().padding(16.dp), 
        Arrangement.Center, 
        Alignment.CenterHorizontally
    ) { 
        Icon(Icons.Rounded.ErrorOutline, null, tint = AppColors.StatusRed, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(e, textAlign = TextAlign.Center, color = AppColors.TextMain)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandBlue)
        ) { 
            Text("Réessayer") 
        } 
    } 
}