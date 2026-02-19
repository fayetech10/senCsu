package com.example.sencsu.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import com.example.sencsu.theme.AppShapes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sencsu.components.ModernAdherentRow
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.remote.dto.FormConstants
import com.example.sencsu.data.repository.SessionManager // Import de SessionManager
import com.example.sencsu.theme.AppColors
import kotlinx.coroutines.delay
@Composable
fun RecentActivitiesSection(
    adherents: List<AdherentDto>,
    onAdherentClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    // 1. On utilise 'adherents' comme clé pour que l'état d'animation se réinitialise
    val itemsToShow = remember(adherents) { adherents.take(5) }

    // 2. On lie la liste d'états à 'itemsToShow'
    val visibleStates = remember(itemsToShow) {
        mutableStateListOf<Boolean>().apply {
            repeat(itemsToShow.size) { add(false) }
        }
    }

    LaunchedEffect(itemsToShow) {
        itemsToShow.indices.forEach { index ->
            delay(index * 80L) // Un peu plus rapide pour la fluidité
            if (index < visibleStates.size) {
                visibleStates[index] = true
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ... (Header identique)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activités récentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                "Voir tout",
                modifier = Modifier.clip(CircleShape).clickable { onSeeAllClick() }.padding(8.dp),
                color = AppColors.BrandBlue, // Remplace par ta couleur
                style = MaterialTheme.typography.labelLarge
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppColors.SurfaceBackground,
            shape = AppShapes.LargeRadius,
            shadowElevation = 2.dp
        ) {
            if (itemsToShow.isEmpty()) {
                Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun adhérent récent", color = AppColors.TextSub)
                }
            } else {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    itemsToShow.forEachIndexed { index, adherent ->
                        // L'animation se déclenchera à chaque fois que la liste change
                        AnimatedVisibility(
                            visible = visibleStates.getOrElse(index) { false },
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                ModernAdherentRow(
                                    adherent = adherent,
                                    onClick = { adherent.id?.let { onAdherentClick(it) } },
                                    sessionManager = sessionManager
                                )

                                if (index < itemsToShow.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        thickness = 0.5.dp,
                                        color = AppColors.BorderColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
