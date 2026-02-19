package com.example.sencsu.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sencsu.components.ModernAdherentRow
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.domain.viewmodel.ListeAdherentViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeAdherentScreen(
    viewModel: ListeAdherentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAdherentClick: (Long) -> Unit,
    sessionManager: SessionManager
) {
    val state by viewModel.state.collectAsState()
    
    // Déclenche le rafraîchissement à chaque fois que l'écran est composé/revient au premier plan
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = AppColors.AppBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Liste des Adhérents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextMain
                    ) 
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
                    scrolledContainerColor = AppColors.SurfaceBackground
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            
            // Styled Search Field
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shadowElevation = 2.dp,
                shape = AppShapes.MediumRadius,
                color = AppColors.SurfaceBackground
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Rechercher par nom, prénom ou CNI", color = AppColors.TextSub) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.TextSub) },
                    shape = AppShapes.MediumRadius,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.BrandBlue,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = AppColors.SurfaceBackground,
                        unfocusedContainerColor = AppColors.SurfaceBackground
                    ),
                    singleLine = true
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.BrandBlue)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error!!, 
                            color = AppColors.StatusRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandBlue),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredAdherents) { adherent ->
                        ModernAdherentRow(
                            adherent = adherent,
                            onClick = { onAdherentClick(adherent.id?.toLong() ?: 0L) },
                            sessionManager = sessionManager
                        )
                        HorizontalDivider(
                             modifier = Modifier.padding(horizontal = 16.dp),
                             color = AppColors.BorderColor.copy(alpha = 0.5f)
                        )
                    }
                }
                
                if (state.filteredAdherents.isEmpty()) {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun adhérent trouvé", color = AppColors.TextSub)
                    }
                }
            }
        }
    }
}
