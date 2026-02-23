package com.example.sencsu.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.sencsu.data.local.entity.AdherentEntity
import com.example.sencsu.data.local.entity.PaiementEntity
import com.example.sencsu.domain.viewmodel.SyncViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    navController: NavController,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppColors.AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Données à synchroniser",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            tint = AppColors.TextMain
                        )
                    }
                },
                actions = {
                    // Live badge showing total pending
                    val total = uiState.unsyncedAdherents.size + uiState.unsyncedPaiements.size
                    if (total > 0) {
                        Surface(
                            shape = CircleShape,
                            color = AppColors.BrandBlue,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "$total",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.SurfaceBackground
                )
            )
        },
        bottomBar = {
            val total = uiState.unsyncedAdherents.size + uiState.unsyncedPaiements.size
            AnimatedVisibility(visible = total > 0) {
                Surface(
                    color = AppColors.SurfaceBackground,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .height(56.dp),
                        shape = AppShapes.MediumRadius,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.BrandBlue,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Synchronisation en cours...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Synchroniser maintenant ($total)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Success banner
            if (uiState.syncSuccess) {
                item {
                    SyncSuccessBanner()
                }
            }

            // Error banner
            uiState.errorMessage?.let { msg ->
                item {
                    SyncErrorBanner(message = msg, onDismiss = { viewModel.dismissError() })
                }
            }

            // Empty state
            if (uiState.unsyncedAdherents.isEmpty() && uiState.unsyncedPaiements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.1f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Text(
                                "Tout est synchronisé !",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = AppColors.TextMain
                            )
                            Text(
                                "Aucune donnée en attente de synchronisation.",
                                color = AppColors.TextSub,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            // --- Adherents Section ---
            if (uiState.unsyncedAdherents.isNotEmpty()) {
                item {
                    SyncSectionHeader(
                        icon = Icons.Rounded.Person,
                        title = "Adhérents",
                        count = uiState.unsyncedAdherents.size,
                        color = AppColors.BrandBlue
                    )
                }
                items(uiState.unsyncedAdherents, key = { "adh_${it.localId}" }) { adherent ->
                    AdherentSyncCard(adherent = adherent)
                }
            }

            // --- Paiements Section ---
            if (uiState.unsyncedPaiements.isNotEmpty()) {
                item {
                    SyncSectionHeader(
                        icon = Icons.Rounded.Payment,
                        title = "Paiements",
                        count = uiState.unsyncedPaiements.size,
                        color = Color(0xFFF59E0B)
                    )
                }
                items(uiState.unsyncedPaiements, key = { "pai_${it.localId}" }) { paiement ->
                    PaiementSyncCard(paiement = paiement)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SyncSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = AppColors.TextMain
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = "$count",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun AdherentSyncCard(adherent: AdherentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.MediumRadius,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = AppColors.BrandBlueLite,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (adherent.prenoms?.firstOrNull() ?: "?").toString().uppercase(),
                        fontWeight = FontWeight.Black,
                        color = AppColors.BrandBlue,
                        fontSize = 18.sp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${adherent.prenoms ?: ""} ${adherent.nom ?: ""}".trim().ifEmpty { "Nom inconnu" },
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = adherent.whatsapp ?: adherent.departement ?: "—",
                    color = AppColors.TextSub,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            PendingChip()
        }
    }
}

@Composable
private fun PaiementSyncCard(paiement: PaiementEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.MediumRadius,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Payment,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paiement.reference.ifBlank { "Réf. manquante" },
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format("%,.0f", paiement.montant)} FCFA · ${paiement.modePaiement}",
                    color = AppColors.TextSub,
                    fontSize = 13.sp
                )
            }
            PendingChip()
        }
    }
}

@Composable
private fun PendingChip() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFEF3C7)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B))
            )
            Text(
                "En attente",
                color = Color(0xFF92400E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SyncSuccessBanner() {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.MediumRadius,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF10B981))
                Text(
                    "Synchronisation réussie !",
                    color = Color(0xFF065F46),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SyncErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.MediumRadius,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.StatusRed.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Error, null, tint = AppColors.StatusRed, modifier = Modifier.size(20.dp))
            Text(
                message,
                color = AppColors.StatusRed,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, null, tint = AppColors.StatusRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
