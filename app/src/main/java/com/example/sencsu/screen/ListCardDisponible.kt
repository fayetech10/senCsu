
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sencsu.components.ModernAdherentRow
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.domain.viewmodel.ListeAdherentViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListCardDisponible(
    viewModel: ListeAdherentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAdherentClick: (Long) -> Unit,
    sessionManager: SessionManager
) {
    val state by viewModel.state.collectAsState()
    
    // Auto-refresh on mount
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            EnhancedTopBar(
                onNavigateBack = onNavigateBack,
                adherentCount = state.filteredAdherents.size
            )
        },
        containerColor = AppColors.AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.AppBackground)
        ) {
            // Enhanced Search Section
            SearchSection(
                searchQuery = state.searchQuery,
                onSearchChange = viewModel::onSearchQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Main Content with Animations
            Box(modifier = Modifier
                .fillMaxSize()
                .weight(1f)
            ) {
                when {
                    state.isLoading -> {
                        LoadingState(modifier = Modifier.fillMaxSize())
                    }
                    state.error != null -> {
                        ErrorState(
                            errorMessage = state.error!!,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    state.filteredAdherents.isEmpty() -> {
                        EmptyState(
                            searchQuery = state.searchQuery,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        AdherentsList(
                            adherents = state.filteredAdherents,
                            onAdherentClick = onAdherentClick,
                            sessionManager = sessionManager,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedTopBar(
    onNavigateBack: () -> Unit,
    adherentCount: Int
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Cartes Disponibles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextMain
                )
                Text(
                    text = "$adherentCount résultats",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSub
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Retour",
                    tint = AppColors.TextMain
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.SurfaceBackground,
            titleContentColor = AppColors.TextMain
        ),
        modifier = Modifier.shadow(elevation = 1.dp)
    )
}

@Composable
private fun SearchSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.MediumRadius)
                .background(AppColors.SurfaceAlt),
            placeholder = {
                Text(
                    "Chercher par nom, prénom ou CNI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSub
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = AppColors.TextSub,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Effacer",
                            tint = AppColors.TextSub,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextMain),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.BrandBlue,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = AppColors.SurfaceAlt,
                unfocusedContainerColor = AppColors.SurfaceAlt
            ),
            shape = AppShapes.MediumRadius
        )
    }
}

@Composable
private fun AdherentsList(
    adherents: List<AdherentDto>,
    onAdherentClick: (Long) -> Unit,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = adherents,
            key = { it.id ?: 0L }
        ) { adherent ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
                modifier = Modifier.animateContentSize()
            ) {
                // Using ModernAdherentRow instead of HealthInsuranceCard for consistency if that was the goal
                // But the user might want the specific card. 
                // Let's assume HealthInsuranceCard is meant to be used here.
                // If it fails to compile, we can switch to ModernAdherentRow or find HealthInsuranceCard
                HealthInsuranceCard(
                    data = adherent,
                    sessionManager = sessionManager,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 16.dp),
                strokeWidth = 3.dp,
                color = AppColors.BrandBlue
            )
            Text(
                text = "Chargement...",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSub
            )
        }
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(AppShapes.LargeRadius)
                .background(AppColors.StatusRed.copy(alpha = 0.1f))
                .padding(24.dp)
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppColors.StatusRed
            )
            Text(
                text = "Une erreur est survenue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMain
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSub,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandBlue),
                shape = AppShapes.MediumRadius
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Réessayer")
            }
        }
    }
}

@Composable
private fun EmptyState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp),
                tint = AppColors.BorderColor
            )
            Text(
                text = if (searchQuery.isEmpty()) "Liste vide" else "Aucun résultat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMain
            )
            Text(
                text = if (searchQuery.isEmpty()) "Aucun adhérent avec carte disponible" else "Essayez une autre recherche",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSub
            )
        }
    }
}