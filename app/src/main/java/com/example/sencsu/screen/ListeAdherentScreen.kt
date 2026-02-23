package com.example.sencsu.screen

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sencsu.components.ServerImage
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.domain.viewmodel.ListeAdherentViewModel
import com.example.sencsu.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────
// Date grouping helper
// ─────────────────────────────────────────────────────────────────────

@SuppressLint("SimpleDateFormat")
private fun formatDateHeader(raw: String?): String {
    if (raw.isNullOrBlank()) return "DATE INCONNUE"
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRANCE)
            .also { it.isLenient = true }
        val output = SimpleDateFormat("d MMMM yyyy", Locale.FRANCE)
        val date = input.parse(raw)
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).parse(raw)
            ?: return raw.uppercase()
        output.format(date).uppercase(Locale.FRANCE)
    } catch (e: Exception) {
        raw.take(10).uppercase()
    }
}

private data class DateGroup(val label: String, val adherents: List<AdherentDto>)

private fun groupByDate(adherents: List<AdherentDto>): List<DateGroup> {
    return adherents
        .groupBy { formatDateHeader(it.createdAt) }
        .map { (label, list) -> DateGroup(label, list) }
}

// ─────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ListeAdherentScreen(
    viewModel: ListeAdherentViewModel = hiltViewModel(),
    sessionManager: SessionManager,
    onAdherentClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onAddClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(state.adherents, searchQuery) {
        if (searchQuery.isBlank()) state.adherents
        else state.adherents.filter {
            val full = "${it.prenoms ?: ""} ${it.nom ?: ""}".lowercase()
            searchQuery.lowercase().split(" ").all { token -> full.contains(token) }
        }
    }

    val groups = remember(filtered) { groupByDate(filtered) }

    // Track scroll to apply shadow on search bar
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

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
        containerColor = AppColors.SurfaceBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Title ───────────────────────────────────────────────
            Text(
                "Adhérents",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = AppColors.TextMain,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 4.dp)
            )

            // ── Sticky Search bar with scroll shadow ────────────────
            val shadowElevation by animateColorAsState(
                targetValue = if (isScrolled) Color.Black.copy(alpha = 0.08f) else Color.Transparent,
                animationSpec = tween(250),
                label = "searchShadow"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .shadow(
                        elevation = if (isScrolled) 6.dp else 0.dp,
                        shape = RoundedCornerShape(0.dp),
                        ambientColor = shadowElevation,
                        spotColor = shadowElevation
                    )
                    .background(AppColors.SurfaceBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            // ── List ────────────────────────────────────────────────
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(state.error!!) { viewModel.refresh() }
                groups.isEmpty() -> EmptyState(searchQuery)
                else -> AdherentList(
                    groups = groups,
                    sessionManager = sessionManager,
                    onAdherentClick = onAdherentClick,
                    listState = listState
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Search bar
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(AppColors.SurfaceAlt),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = AppColors.TextDisabled,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            // Editable text
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = AppColors.TextMain
                ),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Rechercher un adhérent...",
                            fontSize = 15.sp,
                            color = AppColors.TextDisabled
                        )
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = AppColors.TextDisabled, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Grouped list
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun AdherentList(
    groups: List<DateGroup>,
    sessionManager: SessionManager,
    onAdherentClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        groups.forEach { group ->
            // Date header
            item(key = "header_${group.label}") {
                DateHeader(group.label)
            }
            // Adherent rows
            items(group.adherents, key = { it.id?.toString() ?: it.hashCode().toString() }) { adherent ->
                AdherentRow(
                    adherent = adherent,
                    sessionManager = sessionManager,
                    onClick = {
                        val id = adherent.id?.toString() ?: return@AdherentRow
                        onAdherentClick(id)
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp),
                    thickness = 0.5.dp,
                    color = AppColors.BorderColorLight
                )
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Date section header
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun DateHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceAlt)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSub,
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Single adherent row
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun AdherentRow(
    adherent: AdherentDto,
    sessionManager: SessionManager,
    onClick: () -> Unit
) {
    val dependantCount = adherent.personnesCharge.size
    val dependantLabel = when (dependantCount) {
        0 -> "0 personne à charge"
        1 -> "1 personne à charge"
        else -> "$dependantCount personnes à charge"
    }
    val initials = buildString {
        adherent.prenoms?.firstOrNull()?.uppercaseChar()?.let { append(it) }
        adherent.nom?.firstOrNull()?.uppercaseChar()?.let { append(it) }
    }.ifEmpty { "?" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(AppColors.SurfaceBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(2.dp, AppColors.BrandBlueLite, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!adherent.photo.isNullOrBlank()) {
                ServerImage(
                    filename = adherent.photo,
                    sessionManager = sessionManager,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AppColors.BrandBlueLite),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.BrandBlue
                    )
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // Name + dependants
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${adherent.prenoms} ${adherent.nom}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMain,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Group,
                    contentDescription = null,
                    tint = AppColors.TextSub,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    dependantLabel,
                    fontSize = 13.sp,
                    color = AppColors.TextSub
                )
            }
        }

        // Chevron
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = "Voir",
            tint = AppColors.TextDisabled,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// States
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.BrandBlue, strokeWidth = 2.5.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = AppColors.StatusRed, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, color = AppColors.TextSub)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Réessayer")
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.PersonSearch, null, tint = AppColors.TextMuted, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            if (query.isBlank()) "Aucun adhérent" else "Aucun résultat pour \"$query\"",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextSub,
            textAlign = TextAlign.Center
        )
    }
}
