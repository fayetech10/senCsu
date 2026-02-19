package com.example.sencsu.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sencsu.components.ModernAdherentRow
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.repository.SessionManager
import com.example.sencsu.domain.viewmodel.SearchViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes
import kotlinx.coroutines.delay

// ==================== MAIN SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAdherentClick: (String) -> Unit
) {
    val uiState by rememberSearchUiState(viewModel)
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = AppColors.AppBackground,
        topBar = {
            ElegantSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onBack = onNavigateBack,
                onFilterClick = { showFilterSheet = true },
                activeFilterCount = uiState.activeFilterCount
            )
        }
    ) { padding ->
        SearchContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            uiState = uiState,
            onAdherentClick = onAdherentClick,
            sessionManager = viewModel.sessionManager
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            viewModel = viewModel,
            sheetState = sheetState,
            onDismiss = { showFilterSheet = false }
        )
    }
}

// ==================== UI STATE ====================
@Immutable
data class SearchUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filteredAdherents: List<AdherentDto> = emptyList(),
    val activeFilterCount: Int = 0
)

@Composable
private fun rememberSearchUiState(viewModel: SearchViewModel): State<SearchUiState> {
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredAdherents by viewModel.filteredAdherents.collectAsState()
    val activeFilterCount = remember(
        viewModel.selectedRegion.collectAsState().value,
        viewModel.selectedDepartement.collectAsState().value,
        viewModel.selectedCommune.collectAsState().value
    ) {
        countActiveFilters(viewModel)
    }

    return remember(isLoading, searchQuery, filteredAdherents, activeFilterCount) {
        derivedStateOf {
            SearchUiState(
                isLoading = isLoading,
                searchQuery = searchQuery,
                filteredAdherents = filteredAdherents,
                activeFilterCount = activeFilterCount
            )
        }
    }
}

// ==================== CONTENT ====================
@Composable
private fun SearchContent(
    modifier: Modifier = Modifier,
    uiState: SearchUiState,
    onAdherentClick: (String) -> Unit,
    sessionManager: SessionManager
) {
    Box(modifier = modifier) {
        when {
            uiState.isLoading -> LoadingView()
            uiState.searchQuery.isEmpty() -> EmptyHistoryView()
            uiState.filteredAdherents.isEmpty() -> EmptyResultView("Recherche : ${uiState.searchQuery}")
            else -> ResultList(
                adherents = uiState.filteredAdherents,
                onItemClick = onAdherentClick,
                sessionManager = sessionManager
            )
        }
    }
}

// ==================== SEARCH BAR ====================
@Composable
private fun ElegantSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onFilterClick: () -> Unit,
    activeFilterCount: Int
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (e: Exception) {
            // Ignore focus errors
        }
    }

    Column(
        modifier = Modifier
            .background(AppColors.SurfaceBackground)
            .statusBarsPadding()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton(onClick = onBack)
            SearchTextField(
                query = query,
                onQueryChange = onQueryChange,
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f)
            )
            FilterButton(
                onClick = onFilterClick,
                activeCount = activeFilterCount
            )
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(AppColors.SurfaceAlt, CircleShape)
            .border(1.dp, AppColors.BorderColor.copy(alpha = 0.5f), CircleShape)
    ) {
        Icon(
            Icons.Rounded.ArrowBack,
            contentDescription = "Retour",
            tint = AppColors.TextMain,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(48.dp)
            .focusRequester(focusRequester),
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = AppColors.TextMain,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(AppColors.BrandBlue),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { keyboardController?.hide() }
        ),
        decorationBox = { innerTextField ->
            SearchTextFieldDecoration(
                query = query,
                onClear = { onQueryChange("") },
                innerTextField = innerTextField
            )
        }
    )
}

@Composable
private fun SearchTextFieldDecoration(
    query: String,
    onClear: () -> Unit,
    innerTextField: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(AppShapes.MediumRadius)
            .background(AppColors.SurfaceAlt)
            .border(1.dp, if(query.isNotEmpty()) AppColors.BrandBlue.copy(0.3f) else AppColors.BorderColor, AppShapes.MediumRadius)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Rechercher un membre...",
                    color = AppColors.TextSub,
                    fontSize = 16.sp
                )
            }
            innerTextField()
        }

        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Effacer",
                tint = AppColors.TextSub,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}

@Composable
private fun FilterButton(
    onClick: () -> Unit,
    activeCount: Int
) {
    val isActive = activeCount > 0

    Box {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = 1.dp,
                    color = if (isActive) AppColors.BrandBlue else AppColors.BorderColor,
                    shape = AppShapes.MediumRadius
                )
                .background(if (isActive) AppColors.BrandBlue.copy(alpha=0.1f) else Color.Transparent, AppShapes.MediumRadius)
        ) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = "Filtres",
                tint = if (isActive) AppColors.BrandBlue else AppColors.TextSub
            )
        }

        AnimatedVisibility(
            visible = isActive,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(AppColors.StatusRed, CircleShape)
                    .border(2.dp, AppColors.SurfaceBackground, CircleShape)
            )
        }
    }
}

// ==================== RESULT LIST ====================
@Composable
private fun ResultList(
    adherents: List<AdherentDto>,
    onItemClick: (String) -> Unit,
    sessionManager: SessionManager
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ResultHeader(count = adherents.size)
        }

        itemsIndexed(
            items = adherents,
            key = { _, adherent -> adherent.id ?: System.currentTimeMillis() }
        ) { index, adherent ->
            AdherentListItem(
                adherent = adherent,
                index = index,
                onClick = onItemClick,
                sessionManager = sessionManager
            )
        }
    }
}

@Composable
private fun ResultHeader(count: Int) {
    Text(
        text = "RÉSULTATS ($count)",
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.TextSub,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun AdherentListItem(
    adherent: AdherentDto,
    index: Int,
    onClick: (String) -> Unit,
    sessionManager: SessionManager
) {
    val animationDelay = (index * 50).coerceAtMost(500)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300, delayMillis = animationDelay)) +
                slideInVertically(
                    animationSpec = tween(300, delayMillis = animationDelay),
                    initialOffsetY = { 50 }
                )
    ) {
        ModernAdherentRow(
            adherent = adherent,
            onClick = { adherent.id?.let { onClick(it.toString()) } },
            sessionManager = sessionManager
        )
    }
}

// ==================== FILTER SHEET ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    viewModel: SearchViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SurfaceBackground,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        FilterSheetContent(
            viewModel = viewModel,
            onClose = onDismiss
        )
    }
}

@Composable
private fun FilterSheetContent(
    viewModel: SearchViewModel,
    onClose: () -> Unit
) {
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val regions by viewModel.availableRegions.collectAsState()
    val selectedDepartement by viewModel.selectedDepartement.collectAsState()
    val selectedCommune by viewModel.selectedCommune.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        FilterSheetHeader(
            onReset = {
                viewModel.onRegionChange(null)
                viewModel.onDepartementChange(null)
                viewModel.onCommuneChange(null)
            }
        )

        HorizontalDivider(color = AppColors.BorderColor)

        FilterSection(
            title = "Région",
            options = regions,
            selected = selectedRegion,
            onSelect = viewModel::onRegionChange
        )

         // Placeholder for other sections (Departement, Commune) if needed
         // FilterSection("Département", departements, selectedDepartement, viewModel::onDepartementChange)

        ApplyFiltersButton(onClick = onClose)
    }
}

@Composable
private fun FilterSheetHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Affiner la recherche",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextMain
        )
        Text(
            text = "Réinitialiser",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.BrandBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onReset)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextMain
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterOption(
                label = "Tous",
                isSelected = selected == null,
                onClick = { onSelect(null) }
            )

            options.take(6).forEach { option ->
                FilterOption(
                    label = option,
                    isSelected = selected == option,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (label == "Tous") AppColors.TextMain else AppColors.BrandBlue,
            selectedLabelColor = Color.White,
            containerColor = AppColors.SurfaceAlt,
             labelColor = AppColors.TextMain
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.Transparent,
            enabled = true,
            selected = isSelected
        )
    )
}

@Composable
private fun ApplyFiltersButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = AppShapes.MediumRadius,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.TextMain
        )
    ) {
        Text(
            text = "Appliquer les filtres",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==================== EMPTY STATES ====================
@Composable
private fun EmptyResultView(query: String) {
    EmptyStateLayout {
        EmptyStateIcon(icon = Icons.Rounded.SearchOff)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Aucun résultat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextMain
        )
        Text(
            text = "Nous n'avons trouvé aucun membre correspondant à\n\"$query\"",
            textAlign = TextAlign.Center,
            color = AppColors.TextSub,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun EmptyHistoryView() {
    EmptyStateLayout(modifier = Modifier.padding(top = 40.dp)) {
        Icon(
            Icons.Rounded.TravelExplore,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = AppColors.BorderColor
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Commencez votre recherche",
            color = AppColors.TextSub
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.BrandBlue,
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun EmptyStateLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun EmptyStateIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(AppColors.SurfaceAlt, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = AppColors.TextSub
        )
    }
}

// ==================== HELPERS ====================
private fun countActiveFilters(viewModel: SearchViewModel): Int {
    return listOfNotNull(
        viewModel.selectedRegion.value,
        viewModel.selectedDepartement.value,
        viewModel.selectedCommune.value
    ).size
}