package com.example.sencsu.screen

import HealthInsuranceCard
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sencsu.components.ServerImage
import com.example.sencsu.components.modals.AddPersonneChargeModal
import com.example.sencsu.configs.ApiConfig
import com.example.sencsu.data.remote.dto.*
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.domain.viewmodel.AddAdherentViewModel
import com.example.sencsu.domain.viewmodel.AdherentDetailsViewModel
import com.example.sencsu.domain.viewmodel.DetailsUiEvent
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherentDetailsScreen(
    viewModel: AdherentDetailsViewModel = hiltViewModel(),
    viewModelP: AddAdherentViewModel = hiltViewModel(),
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // États pour les modales
    var showPersonneDetailsModal by remember { mutableStateOf<PersonneChargeDto?>(null) }
    var showAddPersonneModal by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DetailsUiEvent.AdherentDeleted -> onNavigateBack()
                is DetailsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        containerColor = AppColors.SurfaceAlt, // Couleur de fond plus douce
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // TopBar transparente car on a un header personnalisé
            // On garde juste les boutons pour la navigation
        },
        floatingActionButton = {
            if (state.adherent != null) {
                FloatingActionButton(
                    onClick = { showAddPersonneModal = true },
                    containerColor = AppColors.BrandBlue,
                    contentColor = Color.White,
                    shape = AppShapes.MediumRadius
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Ajouter bénéficiaire")
                }
            }
        }
    ) { padding ->

        // Dialogues de confirmation
        if (state.showDeleteAdherentDialog) {
            ConfirmationDialog(
                title = "Supprimer l'adhérent",
                message = "Cette action est irréversible. Voulez-vous continuer ?",
                confirmText = "Supprimer",
                onConfirm = { viewModel.confirmDeleteAdherent() },
                onDismiss = { viewModel.cancelDeleteAdherent() }
            )
        }

        state.personToDelete?.let { personne ->
            ConfirmationDialog(
                title = "Supprimer le bénéficiaire",
                message = "Supprimer ${personne.prenoms} ${personne.nom} ?",
                onConfirm = { viewModel.confirmDeletePersonne() },
                onDismiss = { viewModel.cancelDeletePersonne() }
            )
        }

        // Modal de détails personne
        showPersonneDetailsModal?.let { personne ->
            PersonneDetailsModal(
                personne = personne,
                sessionManager = viewModel.sessionManager,
                onEdit = { /* TODO: Implémenter l'édition */ },
                onDelete = {
                    showPersonneDetailsModal = null
                    viewModel.showDeletePersonneConfirmation(personne)
                },
                onDismiss = { showPersonneDetailsModal = null }
            )
        }

        // Modal d'ajout personne
        if (showAddPersonneModal) {
            Dialog(
                onDismissRequest = { showAddPersonneModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AddPersonneChargeModal(
                    viewModel = viewModelP,
                    onSave = { viewModelP.saveDependant() },
                    onCancel = { showAddPersonneModal = false }
                )
            }
        }

        // Preview image
        state.selectedImageUrl?.let { url ->
            ImagePreviewDialog(
                imageUrl = url,
                onDismiss = { viewModel.closeImagePreview() },
                sessionManager = viewModel.sessionManager
            )
        }

        // Contenu principal
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Header Background (Fixed at top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppColors.BrandBlue,
                                AppColors.BrandBlueDark
                            )
                        )
                    )
            )

            // Boutons de navigation (Overlay sur le header)
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour")
                }

                Text(
                    "Détails Adhérent",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = AppColors.SurfaceBackground
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier l'adhérent", color = AppColors.TextMain) },
                            onClick = {
                                showMenu = false
                                // TODO: Naviguer vers l'écran de modification
                            },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = AppColors.TextMain) }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer l'adhérent", color = AppColors.StatusRed) },
                            onClick = {
                                showMenu = false
                                viewModel.showDeleteAdherentConfirmation()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = AppColors.StatusRed) }
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.padding(top = 80.dp) // Pour laisser la place à la top bar custom
            ) {
                when {
                    state.error != null -> ErrorStates(message = state.error ?: "Erreur", onRetry = { viewModel.refresh() })
                    state.adherent == null -> LoadingState()
                    else -> AdherentContent(
                        adherent = state.adherent!!,
                        paiements = state.paiements,
                        cotisations = state.cotisations,
                        sessionManager = viewModel.sessionManager,
                        viewModel = viewModel,
                        onPersonneClick = { showPersonneDetailsModal = it },
                        padding = padding // Padding du scaffold pour le bas (FAB)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AdherentContent(
    adherent: AdherentDto,
    paiements: List<PaiementDto>,
    cotisations: List<CotisationDto>,
    sessionManager: SessionManager,
    viewModel: AdherentDetailsViewModel,
    onPersonneClick: (PersonneChargeDto) -> Unit,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 16.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 80.dp // Espace pour le FAB
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ProfileSection(adherent, sessionManager, viewModel) }
        
        item { 
             HealthInsuranceCard(
                data = adherent,
                sessionManager = sessionManager)
        }

        item { StatsOverview(adherent, paiements) }

        item { 
            ModernSection(title = "Informations", icon = Icons.Outlined.Person) {
                PersonalInfo(adherent)
            }
        }

        item {
            ModernSection(title = "Documents", icon = Icons.Outlined.Description) {
                 DocumentsGrid(adherent, sessionManager, viewModel)
            }
        }

        item {
            ModernSection(title = "Finances", icon = Icons.Outlined.Payments) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                     CotisationTimelineCard(cotisations)
                     PaymentStatusCard(adherent, paiements, sessionManager)
                }
            }
        }

        item {
             BeneficiariesSection(adherent, sessionManager, onPersonneClick)
        }
    }
}

@Composable
private fun ProfileSection(
    adherent: AdherentDto,
    sessionManager: SessionManager,
    viewModel: AdherentDetailsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = AppShapes.LargeRadius
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo & Status
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                ServerImage(
                    filename = adherent.photo,
                    sessionManager = sessionManager,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceAlt)
                        .clickable { viewModel.openImagePreview(adherent.photo) },
                    contentScale = ContentScale.Crop
                )

                Surface(
                    color = AppColors.StatusGreen,
                    shape = CircleShape,
                    border = BorderStroke(3.dp, AppColors.SurfaceBackground),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Actif",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name & Activity
            Text(
                "${adherent.prenoms} ${adherent.nom}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AppColors.TextMain
            )
            
            Text(
                adherent.secteurActivite?.uppercase() ?: "PARTICULIER",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.BrandBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(AppColors.BrandBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionItem(
                    icon = Icons.Rounded.Call,
                    label = "Appeler",
                    color = AppColors.StatusGreen,
                    onClick = { /* TODO */ }
                )
                QuickActionItem(
                    icon = Icons.Rounded.Message,
                    label = "SMS",
                    color = AppColors.BrandBlue,
                    onClick = { /* TODO */ }
                )
                QuickActionItem(
                    icon = Icons.Rounded.Whatsapp, // Assure-toi d'avoir une icône Whatsapp ou similaire
                    label = "WhatsApp",
                    color = Color(0xFF25D366),
                    onClick = { 
                         // Helper function for Whatsapp intent could be used here
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSub,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatsOverview(
    adherent: AdherentDto,
    paiements: List<PaiementDto>
) {
    val totalPaid = paiements.sumOf { it.montant }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        shape = AppShapes.MediumRadius,
        border = BorderStroke(1.dp, AppColors.BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItemView(
                label = "Cotisation",
                value = "${adherent.montantTotal?.toInt() ?: 0} F",
                color = AppColors.BrandBlue
            )
            
            // Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(AppColors.BorderColor)
            )

            StatItemView(
                label = "Payé",
                value = "${totalPaid.toInt()} F",
                color = AppColors.StatusGreen
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(AppColors.BorderColor)
            )

            StatItemView(
                label = "Restant",
                value = "${((adherent.montantTotal ?: 0.0) - totalPaid).toInt()} F",
                color = AppColors.StatusOrange
            )
        }
    }
}

@Composable
private fun StatItemView(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSub
        )
    }
}

@Composable
private fun ModernSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.BrandBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMain
            )
        }
        
        // Content container
        content()
    }
}

@Composable
private fun PersonalInfo(adherent: AdherentDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        shape = AppShapes.MediumRadius,
        border = BorderStroke(1.dp, AppColors.BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoRowCompact(Icons.Rounded.Fingerprint, "CNI", adherent.numeroCNi ?: "Non renseigné")
            InfoRowCompact(Icons.Rounded.Cake, "Né(e) le", adherent.dateNaissance ?: "Non renseigné")
            InfoRowCompact(Icons.Rounded.Place, "Adresse", adherent.adresse ?: "Non renseigné")
            InfoRowCompact(Icons.Rounded.Phone, "Téléphone", adherent.whatsapp ?: "Non renseigné")
        }
    }
}

@Composable
private fun InfoRowCompact(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.TextSub,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSub,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextMain,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DocumentsGrid(
    adherent: AdherentDto,
    sessionManager: SessionManager,
    viewModel: AdherentDetailsViewModel
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DocumentCard(
            title = "CNI Recto",
            imageUrl = adherent.photoRecto,
            sessionManager = sessionManager,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.openImagePreview(adherent.photoRecto) }
        )
        DocumentCard(
            title = "CNI Verso",
            imageUrl = adherent.photoVerso,
            sessionManager = sessionManager,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.openImagePreview(adherent.photoVerso) }
        )
    }
}

@Composable
private fun DocumentCard(
    title: String,
    imageUrl: String?,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = AppShapes.MediumRadius,
        border = BorderStroke(1.dp, AppColors.BorderColor),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground)
    ) {
        Box {
            if (!imageUrl.isNullOrBlank()) {
                 ServerImage(
                    filename = imageUrl,
                    sessionManager = sessionManager,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(AppColors.SurfaceAlt),
                    contentAlignment = Alignment.Center
                ){
                    Icon(Icons.Rounded.ImageNotSupported, contentDescription = null, tint = AppColors.TextSub)
                }
            }

            // Overlay with title
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 100f
                        )
                    )
            )

            Text(
                title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}


@Composable
@RequiresApi(Build.VERSION_CODES.O)
private fun CotisationTimelineCard(cotisations: List<CotisationDto>) {
    val activeCotisation = cotisations.firstOrNull() ?: return
    
    // (Conserver la logique de calcul de progression existante)
    val (progress, daysRemaining) = remember(activeCotisation) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val start = LocalDate.parse(activeCotisation.dateDebut, formatter)
            val end = LocalDate.parse(activeCotisation.dateFin, formatter)
            val now = LocalDate.now()

            val totalDays = ChronoUnit.DAYS.between(start, end).toFloat()
            val elapsedDays = ChronoUnit.DAYS.between(start, now).toFloat()
            val remaining = ChronoUnit.DAYS.between(now, end)

            (elapsedDays / totalDays).coerceIn(0f, 1f) to remaining
        } catch (e: Exception) {
            0f to 0L
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        shape = AppShapes.MediumRadius,
        border = BorderStroke(1.dp, AppColors.BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Text(
                    "Validité Adhésion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextMain
                )
                
                Badge(
                    containerColor = if (daysRemaining < 30) AppColors.StatusOrange.copy(alpha = 0.1f) else AppColors.StatusGreen.copy(alpha = 0.1f),
                    contentColor = if (daysRemaining < 30) AppColors.StatusOrange else AppColors.StatusGreen
                ) {
                    Text("$daysRemaining jours")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (daysRemaining < 30) AppColors.StatusOrange else AppColors.StatusGreen,
                trackColor = AppColors.SurfaceAlt
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Du : ${activeCotisation.dateDebut}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSub
                )
                Text(
                    "Au : ${activeCotisation.dateFin}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSub
                )
            }
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
private fun PaymentStatusCard(adherent: AdherentDto, paiements: List<PaiementDto>, sessionManager: SessionManager) {
    // Logique existante simplifiée visuellement
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        shape = AppShapes.MediumRadius,
        border = BorderStroke(1.dp, AppColors.BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
             Text(
                "Historique Paiements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMain
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            if (paiements.isEmpty()) {
                Text("Aucun paiement enregistré", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSub)
            } else {
                paiements.take(3).forEach {
                     PaymentItemRow(it)
                     if (it != paiements.take(3).last()) {
                         HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = AppColors.BorderColor)
                     }
                }
            }
        }
    }
}

@Composable
private fun PaymentItemRow(paiement: PaiementDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                paiement.reference,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextMain
            )
            Text(
                paiement.datePaiement ?: "Date Inconnue",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSub
            )
        }
        
        Text(
             "+${String.format("%,.0f", paiement.montant)} F",
             style = MaterialTheme.typography.bodyMedium,
             fontWeight = FontWeight.Bold,
             color = AppColors.StatusGreen
        )
    }
}


@Composable
private fun BeneficiariesSection(
    adherent: AdherentDto,
    sessionManager: SessionManager,
    onPersonneClick: (PersonneChargeDto) -> Unit
) {
    ModernSection(title = "Bénéficiaires", icon = Icons.Outlined.People) {
         if (adherent.personnesCharge.isEmpty()) {
            EmptyStatee()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                adherent.personnesCharge.forEach { personne ->
                    BeneficiaryCard(
                        personne = personne,
                        sessionManager = sessionManager,
                        onClick = { onPersonneClick(personne) }
                    )
                }
            }
        }
    }
}

// Reuse existing components like BeneficiaryCard, EmptyStatee, ErrorStates, LoadingState, etc.
// Need to make sure they are defined or update them to new style if included above.
// For brevity, I will re-include the necessary ones with updated styles.

@Composable
private fun BeneficiaryCard(
    personne: PersonneChargeDto,
    sessionManager: SessionManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = ApiConfig.getImageUrl(personne.photo)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        shape = AppShapes.MediumRadius,
        border = BorderStroke(1.dp, AppColors.BorderColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = personne.nom,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceAlt),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${personne.prenoms} ${personne.nom}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextMain
                )
                Text(
                    personne.lienParent?.uppercase() ?: "AUTRE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSub
                )
            }
            
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.TextSub)
        }
    }
}

@Composable
private fun EmptyStatee() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Aucun bénéficiaire ajouté", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSub)
    }
}

@Composable
private fun ErrorStates(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = AppColors.StatusRed)
        Button(onClick = onRetry) { Text("Réessayer") }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.BrandBlue)
    }
}

// Copied dialogs (ConfirmationDialog, PersonneDetailsModal, ImagePreviewDialog, AddPersonneChargeModal wrapper logic)
// Updating them to match imports and style
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirmer",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
     AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.StatusRed)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        containerColor = AppColors.SurfaceBackground
    )
}

@Composable
private fun PersonneDetailsModal(
    personne: PersonneChargeDto,
    sessionManager: SessionManager,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = ApiConfig.getImageUrl(personne.photo)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = AppShapes.LargeRadius,
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header avec bouton fermer
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fermer", tint = AppColors.TextSub)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceAlt),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "${personne.prenoms} ${personne.nom}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = AppColors.TextMain
                )

                personne.lienParent?.let { lien ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(
                        containerColor = AppColors.BrandBlue.copy(alpha = 0.1f),
                        contentColor = AppColors.BrandBlue
                    ) {
                        Text(lien.uppercase())
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Informations détaillées
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoItem(Icons.Rounded.Wc, "Sexe", personne.sexe ?: "Non spécifié")
                    InfoItem(Icons.Rounded.Cake, "Date de naissance", personne.dateNaissance ?: "Non spécifié")
                    InfoItem(Icons.Rounded.Place, "Lieu de naissance", personne.lieuNaissance ?: "Non spécifié")
                    InfoItem(Icons.Rounded.Fingerprint, "Numéro CNI", personne.numeroCNi ?: "Non spécifié")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.StatusRed),
                        border = BorderStroke(1.dp, AppColors.StatusRed.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supprimer")
                    }
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandBlue)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifier")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = AppColors.TextSub,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
private fun ImagePreviewDialog(imageUrl: String?, onDismiss: () -> Unit, sessionManager: SessionManager) {
    imageUrl?.let { url ->
        Dialog(onDismissRequest = onDismiss) {
             Box {
                ServerImage(
                    filename = url,
                    sessionManager = sessionManager,
                    modifier = Modifier.fillMaxWidth().clip(AppShapes.MediumRadius),
                    contentScale = ContentScale.FillWidth
                )
                 IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                     Icon(Icons.Rounded.Close, null, tint = Color.Black)
                 }
            }
        }
    }
}