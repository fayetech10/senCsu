package com.example.sencsu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sencsu.models.ActivityItem
import com.example.sencsu.models.GreenBg
import com.example.sencsu.models.GreenText
import com.example.sencsu.models.RedBg
import com.example.sencsu.models.RedText
import com.example.sencsu.models.StatItem
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes

@Composable
fun StatCard(item: StatItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat look
        shape = AppShapes.MediumRadius,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ligne Icone + Badge Trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icone carrée
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppShapes.ExtraSmallRadius)
                        .background(item.themeColor.copy(alpha = 0.1f)), // Softer background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.iconRes,
                        contentDescription = null,
                        tint = item.themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Badge (Trend)
                // Using AppColors for trend if feasible, otherwise keeping model colors but using AppShapes
                val badgeBg = if (item.isAlert) AppColors.StatusRed.copy(alpha=0.1f) else AppColors.StatusGreen.copy(alpha=0.1f)
                val badgeText = if (item.isAlert) AppColors.StatusRed else AppColors.StatusGreen

                Box(
                    modifier = Modifier
                        .clip(AppShapes.CircleRadius)
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.trend,
                        color = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Valeur et Titre
            Column {
                Text(
                    text = item.value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextMain
                )
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    color = AppColors.TextSub
                )
            }
        }
    }
}

@Composable
fun ActivityRow(activity: ActivityItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        shape = AppShapes.MediumRadius,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderColor.copy(alpha=0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (Simulé avec un cercle gris ici, utilisez AsyncImage en prod)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.CircleRadius)
                    .background(AppColors.SurfaceAlt)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Textes
            Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.name, fontWeight = FontWeight.Bold, color = AppColors.TextMain)
                Text(
                    text = "${activity.action} • ${activity.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSub
                )
            }

            // Badge Status
            Box(
                modifier = Modifier
                    .clip(AppShapes.ExtraSmallRadius)
                    .background(activity.statusBg) // Keeping model provided bg for now as logic is there
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = activity.status,
                    color = activity.statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}