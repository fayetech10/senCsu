package com.example.sencsu.components

import android.annotation.TargetApi
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.roundToInt

// --- Couleurs et Thème ---
// Mapped to AppColors where possible
private val CardWhite = AppColors.SurfaceBackground
private val BrandBlue = AppColors.BrandBlue
private val BrandGreen = AppColors.StatusGreen
private val BrandOrange = AppColors.StatusOrange
private val BrandRed = AppColors.StatusRed
private val DarkText = AppColors.TextMain
private val SubtleText = AppColors.TextSub
private val SurfaceColor = AppColors.SurfaceAlt
private val LightGray = AppColors.BorderColor

enum class ChartType {
    BAR, LINE
}

// --- Composant Principal ---
@Composable
fun AdherentChartCard(
    adherents: List<AdherentDto>,
    modifier: Modifier = Modifier
) {
    var selectedChartType by remember { mutableStateOf(ChartType.BAR) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }

    // Feedback tactile pour une meilleure UX
    val haptic = LocalHapticFeedback.current

    // Optimisation : Calcul des données uniquement si la liste change
    val monthlyData = remember(adherents) { processMonthlyData(adherents) }

    // Optimisation : Recalcul des stats uniquement si monthlyData change réellement
    val stats by remember(monthlyData) {
        derivedStateOf { calculateStats(monthlyData) }
    }

    val isEmpty = remember(monthlyData) { monthlyData.values.all { it == 0 } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp), // Removed horizontal padding as it's handled by parent
        shape = AppShapes.LargeRadius,
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // En-tête avec sélecteur
            ChartHeader(selectedChartType, stats) { selectedChartType = it }

            Spacer(Modifier.height(24.dp))

            // Zone du Graphique avec animation de taille
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .animateContentSize()
            ) {
                if (isEmpty) {
                    EmptyStatePlaceholder()
                } else {
                    val onMonthClick: (String) -> Unit = { month ->
                        if (selectedMonth != month) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                        selectedMonth = if (selectedMonth == month) null else month
                    }

                    when (selectedChartType) {
                        ChartType.BAR -> AnimatedBarChart(
                            data = monthlyData,
                            selectedMonth = selectedMonth,
                            onMonthSelected = onMonthClick,
                            modifier = Modifier.fillMaxSize()
                        )
                        ChartType.LINE -> AnimatedLineChart(
                            data = monthlyData,
                            selectedMonth = selectedMonth,
                            onMonthSelected = onMonthClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Détails du mois (Animation d'apparition)
            AnimatedVisibility(
                visible = selectedMonth != null,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                selectedMonth?.let { month ->
                    Column {
                        Spacer(Modifier.height(16.dp))
                        MonthDetailCard(
                            month = month,
                            count = monthlyData[month] ?: 0,
                            onDismiss = { selectedMonth = null }
                        )
                    }
                }
            }
        }
    }
}

// --- Sous-Composants Graphiques ---

@Composable
private fun EmptyStatePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.BarChart,
            contentDescription = null,
            tint = LightGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Aucune donnée récente",
            style = MaterialTheme.typography.bodyMedium,
            color = SubtleText
        )
    }
}

@Composable
fun AnimatedBarChart(
    data: Map<String, Int>,
    selectedMonth: String?,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(data) { (data.values.maxOrNull() ?: 1).coerceAtLeast(1) }

    val animationPlayed = remember { mutableStateOf(false) }
    val animatedProgress = animateFloatAsState(
        targetValue = if (animationPlayed.value) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "barAnim"
    )

    LaunchedEffect(Unit) { animationPlayed.value = true }

    Column(modifier = modifier) {
        // Zone de dessin
        Box(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height
                val segmentWidth = size.width / data.size

                // Lignes de grille
                val gridLines = 4
                repeat(gridLines) { i ->
                    val y = size.height - (chartHeight * i / gridLines)
                    drawLine(
                        color = LightGray.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                data.entries.forEachIndexed { index, (month, value) ->
                    val isSelected = month == selectedMonth
                    val barTargetHeight = (value.toFloat() / maxValue) * chartHeight
                    val animatedHeight = barTargetHeight * animatedProgress.value

                    val barWidth = segmentWidth * 0.5f // La barre fait 50% de l'espace alloué
                    val x = (index * segmentWidth) + (segmentWidth - barWidth) / 2

                    // Barre
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isSelected) {
                                listOf(BrandOrange, BrandOrange.copy(alpha = 0.8f))
                            } else {
                                listOf(BrandBlue, BrandBlue.copy(alpha = 0.8f))
                            }
                        ),
                        topLeft = Offset(x, size.height - animatedHeight),
                        size = Size(barWidth, animatedHeight),
                        cornerRadius = CornerRadius(barWidth / 3)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Labels alignés
        Row(modifier = Modifier.fillMaxWidth()) {
            data.entries.forEach { (month, value) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null // Pas de ripple par défaut pour garder propre
                        ) { onMonthSelected(month) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isSelected = month == selectedMonth
                    if (isSelected || value > 0) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) BrandOrange else BrandBlue,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    } else {
                        Spacer(Modifier.height(16.dp)) // Espace réservé
                    }

                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) BrandOrange else SubtleText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedLineChart(
    data: Map<String, Int>,
    selectedMonth: String?,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(data) { (data.values.maxOrNull() ?: 1).coerceAtLeast(1) }

    val animationPlayed = remember { mutableStateOf(false) }
    val animatedProgress = animateFloatAsState(
        targetValue = if (animationPlayed.value) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = EaseInOutCubic),
        label = "lineAnim"
    )

    LaunchedEffect(Unit) { animationPlayed.value = true }

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height
                val segmentWidth = size.width / data.size

                // Calcul des coordonnées des points (centrés dans leur segment)
                val points = data.values.mapIndexed { index, value ->
                    val x = (index * segmentWidth) + (segmentWidth / 2)
                    val y = size.height - (value.toFloat() / maxValue) * chartHeight
                    Offset(x, y)
                }

                // Grille
                repeat(4) { i ->
                    val y = size.height - (chartHeight * i / 4)
                    drawLine(
                        color = LightGray.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Dessin de la courbe uniquement si animation > 0
                val animLimit = (points.size * animatedProgress.value).toInt().coerceAtLeast(1)
                val currentPoints = points.take(animLimit + 1).take(points.size)

                if (currentPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        // Lissage de courbe (Bézier simple)
                        for (i in 0 until currentPoints.size - 1) {
                            val p1 = currentPoints[i]
                            val p2 = currentPoints[i + 1]
                            // Contrôle points pour courbe fluide
                            quadraticBezierTo(p1.x, p1.y, (p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
                        }
                        lineTo(currentPoints.last().x, currentPoints.last().y)
                    }

                    // Zone remplie (Gradient)
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(currentPoints.last().x, size.height)
                        lineTo(currentPoints.first().x, size.height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(BrandBlue.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )

                    // Ligne principale
                    drawPath(
                        path = path,
                        color = BrandBlue,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Points (Dots)
                points.forEachIndexed { index, point ->
                    if (index < currentPoints.size) {
                        val month = data.keys.elementAt(index)
                        val isSelected = month == selectedMonth

                        drawCircle(
                            color = CardWhite,
                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = if (isSelected) BrandOrange else BrandBlue,
                            radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                            center = point,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Labels (Même logique que BarChart pour alignement parfait)
        Row(modifier = Modifier.fillMaxWidth()) {
            data.entries.forEach { (month, value) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onMonthSelected(month) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isSelected = month == selectedMonth
                    if (isSelected || value > 0) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) BrandOrange else BrandBlue,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) BrandOrange else SubtleText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// --- Composants UI Génériques ---

@Composable
private fun ChartHeader(
    selectedChartType: ChartType,
    stats: ChartStats,
    onChartTypeChanged: (ChartType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Évolution",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DarkText
                )
                Text(
                    "6 derniers mois",
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtleText
                )
            }
            ChartTypeSelector(selectedChartType, onChartTypeChanged)
        }
        Spacer(Modifier.height(20.dp))
        StatsRow(stats)
    }
}

@Composable
private fun ChartTypeSelector(
    selectedChartType: ChartType,
    onChartTypeChanged: (ChartType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceColor,
        modifier = Modifier.height(36.dp)
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            ChartType.values().forEach { type ->
                val isSelected = selectedChartType == type
                val bgColor = if (isSelected) CardWhite else Color.Transparent
                val iconColor = if (isSelected) BrandBlue else SubtleText

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onChartTypeChanged(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (type == ChartType.BAR) Icons.Rounded.BarChart else Icons.Rounded.Timeline,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: ChartStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            icon = Icons.Rounded.People,
            label = "Total",
            value = stats.total.toString(),
            color = BrandBlue,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.TrendingUp,
            label = "Moy.",
            value = stats.average.toString(),
            color = BrandGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.ShowChart,
            label = "Max",
            value = stats.max.toString(),
            color = BrandOrange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = null // Retiré pour un look plus "flat" et moderne
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = DarkText
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SubtleText
            )
        }
    }
}

@Composable
private fun MonthDetailCard(
    month: String,
    count: Int,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandBlue.copy(alpha = 0.9f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Détails : $month",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$count adhérent${if (count > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Fermer",
                    tint = Color.White
                )
            }
        }
    }
}

// --- Logique Métier & Utilitaires ---

data class ChartStats(
    val total: Int,
    val average: Int,
    val max: Int,
    val min: Int
)

fun calculateStats(data: Map<String, Int>): ChartStats {
    val values = data.values.filter { it >= 0 }
    return if (values.isNotEmpty()) {
        ChartStats(
            total = values.sum(),
            average = (values.average()).roundToInt(),
            max = values.max(),
            min = values.min()
        )
    } else {
        ChartStats(0, 0, 0, 0)
    }
}

@TargetApi(Build.VERSION_CODES.O)
fun processMonthlyData(adherents: List<AdherentDto>): Map<String, Int> {
    val now = LocalDate.now()
    // Utilisation d'un LinkedHashMap pour garantir l'ordre des mois
    val result = LinkedHashMap<String, Int>()

    // Initialisation des 6 derniers mois à 0
    (0..5).map { now.minusMonths(it.toLong()) }
        .reversed()
        .forEach { date ->
            val monthName = getFrenchMonthName(date)
            result[monthName] = 0
        }

    // Remplissage avec les données
    adherents.forEach { adherent ->
        adherent.createdAt?.takeIf { it.isNotBlank() }?.let { dateString ->
            val date = parseDate(dateString)
            // Ne prendre que les dates qui rentrent dans notre fenêtre de 6 mois
            if (!date.isAfter(now) && !date.isBefore(now.minusMonths(5).withDayOfMonth(1))) {
                val monthName = getFrenchMonthName(date)
                if (result.containsKey(monthName)) {
                    result[monthName] = result[monthName]!! + 1
                }
            }
        }
    }

    return result
}

@TargetApi(Build.VERSION_CODES.O)
private fun getFrenchMonthName(date: LocalDate): String {
    return date.month
        .getDisplayName(TextStyle.SHORT, Locale.FRENCH)
        .replaceFirstChar { it.uppercaseChar() }
        .take(3)
        .replace(".", "") // Nettoyage éventuel
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseDate(dateString: String): LocalDate {
    // Liste optimisée des formats les plus probables en premier
    val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME, // 2023-10-05T14:30:00
        DateTimeFormatter.ISO_LOCAL_DATE,      // 2023-10-05
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    )

    for (formatter in formatters) {
        try {
            return LocalDate.parse(dateString.take(19), formatter) // .take(19) pour couper les millisecondes si nécessaire
        } catch (e: Exception) {
            continue
        }
    }

    // Fallback silencieux (ou log d'erreur) : on retourne une date très ancienne pour qu'elle soit filtrée
    return LocalDate.MIN
}