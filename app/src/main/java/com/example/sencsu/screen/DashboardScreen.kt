package com.example.sencsu.screen

import com.example.sencsu.components.AdherentChartCard
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.sencsu.theme.AppShapes
import java.util.Calendar

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
    val showSyncPrompt by viewModel.showSyncPrompt.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    var showNewSheet by remember { mutableStateOf(false) }

    // Refresh data when returning to this screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = AppColors.AppBackground
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
                    state.data != null -> DashboardContent(
                        data = state.data,
                        agent = authState.user,
                        navController = rootNavController,
                        sessionManager = viewModel.sessionManager,
                        pendingCount = pendingCount,
                        onSyncClick = { rootNavController.navigate("sync_screen") },
                        onNewClick = { showNewSheet = true }
                    )
                    else -> EmptyState()
                }
            }
        }

        // Sync prompt modal
        if (showSyncPrompt) {
            SyncPromptModal(
                pendingCount = pendingCount,
                onSyncNow = {
                    viewModel.syncData()
                    rootNavController.navigate("sync_screen")
                },
                onLater = { viewModel.dismissSyncPrompt() }
            )
        }

        // Bottom sheet for "Nouveau" action
        if (showNewSheet) {
            NewFormBottomSheet(
                onDismiss = { showNewSheet = false },
                onActionClick = { route ->
                    showNewSheet = false
                    rootNavController.navigate(route)
                }
            )
        }
    }
}

// ==================== NEW FORM BOTTOM SHEET ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFormBottomSheet(
    onDismiss: () -> Unit,
    onActionClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SurfaceBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Nouveau formulaire",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMain,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FormOption(
                letter = "A",
                title = "Adhérent",
                subtitle = "Ajouter un nouvel adhérent",
                color = AppColors.BrandBlue,
                onClick = { onActionClick("add_adherent") }
            )
            FormOption(
                letter = "C",
                title = "Classique",
                subtitle = "Formulaire d'adhésion classique",
                color = Color(0xFF6366F1),
                onClick = { onActionClick("classique") }
            )
            FormOption(
                letter = "D",
                title = "Dara",
                subtitle = "Formulaire d'adhésion Dara",
                color = Color(0xFFF59E0B),
                onClick = { onActionClick("form_dara") }
            )
        }
    }
}

@Composable
private fun FormOption(
    letter: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.MediumRadius,
        color = color.copy(alpha = 0.08f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(letter, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = AppColors.TextMain, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = AppColors.TextSub, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = AppColors.TextSub)
        }
    }
}

// ==================== DASHBOARD CONTENT ====================
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DashboardContent(
    data: DashboardResponseDto,
    agent: AgentDto?,
    navController: NavController,
    sessionManager: SessionManager,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    onNewClick: () -> Unit
) {
    val adherents = remember { data.data }
    val totalAmount = remember { adherents.sumOf { it.montantTotal ?: 0.0 } }
    val activeMembers = remember { adherents.count { it.actif == true } }
    val pendingMembers = remember { adherents.size - activeMembers }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { HeaderSection(agent) }
        item { HeroBalanceCard(totalAmount) }
        item { QuickStatsRow(total = adherents.size, active = activeMembers, pending = pendingMembers) }
        item {
            ActionButtonsGrid(
                onSearchClick = { navController.navigate("search") },
                onSyncClick = onSyncClick,
                onNewClick = onNewClick,
                onReportClick = { /* TBA */ },
                pendingCount = pendingCount
            )
        }
        item { AdherentChartCard(adherents) }
        item {
            RecentActivitiesSection(
                adherents = adherents.sortedByDescending { it.createdAt }.take(5),
                onAdherentClick = { id -> navController.navigate("adherent_details/$id") },
                onSeeAllClick = { },
                sessionManager = sessionManager
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ==================== HEADER ====================
@Composable
private fun HeaderSection(agent: AgentDto?) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when (hour) {
            in 5..11 -> "☀️ Bonjour"
            in 12..17 -> "🌤️ Bon après-midi"
            else -> "🌙 Bonsoir"
        }
    }
    val initials = remember(agent) {
        val p = agent?.prenom?.firstOrNull()?.uppercase() ?: ""
        val n = agent?.name?.firstOrNull()?.uppercase() ?: ""
        "$p$n".ifEmpty { "?" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSub
            )
            Text(
                agent?.prenom ?: "Agent",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = AppColors.TextMain
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification bell (placeholder)
            Surface(
                shape = CircleShape,
                color = AppColors.SurfaceAlt,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = AppColors.TextSub,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Avatar
            Surface(
                shape = CircleShape,
                color = AppColors.BrandBlue,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ==================== HERO BALANCE CARD ====================
@SuppressLint("DefaultLocale")
@Composable
private fun HeroBalanceCard(amount: Double) {
    val formatted = remember(amount) {
        try { String.format("%,.0f", amount) } catch (_: Exception) { amount.toString() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                12.dp,
                AppShapes.LargeRadius,
                spotColor = AppColors.BrandBlue.copy(0.25f)
            ),
        shape = AppShapes.LargeRadius,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            AppColors.BrandBlue,
                            Color(0xFF219150),
                            AppColors.BrandBlueDark
                        )
                    )
                )
                .padding(24.dp)
        ) {
            // Decorative circles
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-25).dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-20).dp, y = 30.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.AccountBalanceWallet,
                        null,
                        tint = Color.White.copy(0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Total Collecté",
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "$formatted FCFA",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Growth indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.TrendingUp,
                            null,
                            tint = Color(0xFF86EFAC),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "En progression",
                            color = Color(0xFF86EFAC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ==================== QUICK STATS ROW ====================
@Composable
private fun QuickStatsRow(total: Int, active: Int, pending: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.People,
            value = "$total",
            label = "Membres",
            color = AppColors.BrandBlue
        )
        QuickStatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Verified,
            value = "$active",
            label = "Actifs",
            color = AppColors.StatusGreenDark
        )
        QuickStatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Schedule,
            value = "$pending",
            label = "En attente",
            color = AppColors.StatusOrange
        )
    }
}

@Composable
private fun QuickStatPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.MediumRadius,
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon, null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppColors.TextMain,
                    lineHeight = 18.sp
                )
                Text(
                    label,
                    fontSize = 10.sp,
                    color = AppColors.TextSub,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

// ==================== ACTION BUTTONS GRID ====================
@Composable
private fun ActionButtonsGrid(
    onSearchClick: () -> Unit,
    onSyncClick: () -> Unit,
    onNewClick: () -> Unit,
    onReportClick: () -> Unit,
    pendingCount: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButtonCard(
            text = "Rechercher",
            icon = Icons.Rounded.Search,
            bgColor = AppColors.BrandBlueLite,
            tintColor = AppColors.BrandBlue,
            onClick = onSearchClick
        )
        ActionButtonCard(
            text = "Synchroniser",
            icon = Icons.Rounded.Sync,
            bgColor = AppColors.SuccessLight,
            tintColor = AppColors.StatusGreenDark,
            onClick = onSyncClick,
            badge = if (pendingCount > 0) pendingCount else null
        )
        ActionButtonCard(
            text = "Nouveau",
            icon = Icons.Rounded.Add,
            bgColor = Color(0xFFF3E8FF),
            tintColor = Color(0xFF9333EA),
            onClick = onNewClick
        )
        ActionButtonCard(
            text = "Rapports",
            icon = Icons.Rounded.Assessment,
            bgColor = Color(0xFFFEF3C7),
            tintColor = Color(0xFFD97706),
            onClick = onReportClick
        )
    }
}

@Composable
private fun ActionButtonCard(
    text: String,
    icon: ImageVector,
    bgColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
    badge: Int? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Box {
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tintColor, modifier = Modifier.size(26.dp))
                }
            }
            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (badge > 9) "9+" else "$badge",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.TextMain
        )
    }
}

// ==================== STATES ====================
@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = AppColors.BrandBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            Text(
                "Chargement...",
                color = AppColors.TextSub,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.Inbox,
                null,
                tint = AppColors.BorderColor,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "Aucune donnée",
                color = AppColors.TextSub,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorState(e: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = AppColors.DangerLight,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    null,
                    tint = AppColors.StatusRed,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Oops, une erreur !",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextMain
        )
        Spacer(Modifier.height(8.dp))
        Text(
            e,
            textAlign = TextAlign.Center,
            color = AppColors.TextSub,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = AppShapes.MediumRadius,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandBlue),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Réessayer", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==================== SYNC PROMPT MODAL ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPromptModal(
    pendingCount: Int,
    onSyncNow: () -> Unit,
    onLater: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = sheetState,
        containerColor = AppColors.SurfaceBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = AppColors.BrandBlueLite,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = AppColors.BrandBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Données non synchronisées",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.TextMain
                )
                Text(
                    "Vous avez $pendingCount enregistrement(s) en attente de synchronisation.",
                    color = AppColors.TextSub,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Badge
            Surface(
                shape = AppShapes.MediumRadius,
                color = Color(0xFFFEF3C7)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Text(
                        "$pendingCount élément(s) en attente",
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSyncNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = AppShapes.MediumRadius,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.BrandBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Synchroniser maintenant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = onLater,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = AppShapes.MediumRadius,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSub)
                ) {
                    Text("Plus tard", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}