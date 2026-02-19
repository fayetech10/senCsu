import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sencsu.components.forms.*
import com.example.sencsu.components.modals.AddDependantModalContent
import com.example.sencsu.data.remote.dto.AdherentDto
import com.example.sencsu.data.remote.dto.FormConstants
import com.example.sencsu.domain.viewmodel.AddAdherentUiEvent
import com.example.sencsu.domain.viewmodel.AddAdherentUiState
import com.example.sencsu.domain.viewmodel.AddAdherentViewModel
import com.example.sencsu.utils.toLocaleString
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.example.sencsu.data.remote.dto.PersonneChargeDto
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NeutralDark = Color(0xFF1E293B) // Texte principal et boutons
private val NeutralMedium = Color(0xFF64748B) // Texte secondaire
private val NeutralLight = Color(0xFFF1F5F9) // Fonds légers
private val BorderColor = Color(0xFFE2E8F0)
private val SuccessGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AddAdherentScreen(
    onBack: () -> Unit,
    onNavigateToPayment: (adherentId: Long, montantTotal: Int) -> Unit,
    agentId: Long?,
    viewModel: AddAdherentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("Identité", "Contact", "Zone", "Photos", "Bénéficiaires")

    var showAlertDialog by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }
    var alertAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AddAdherentUiEvent.NavigateToPayment -> {
                    event.adherentId?.let {
                        onNavigateToPayment(it, event.montantTotal)
                    }
                }
                is AddAdherentUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ModernHeader(
                title = "Nouvel Adhérent",
                currentStep = currentStep,
                totalSteps = steps.size,
                totalCost = state.totalCost,
                totalBeneficiaries = state.dependants.size + 1,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Barre de progression discrète
            StepProgressBar(
                totalSteps = steps.size,
                currentStep = currentStep,
                modifier = Modifier.padding(20.dp)
            )

            // Contenu du formulaire
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() with fadeOut() },
                label = "form_step"
            ) { step ->
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    when (step) {
                        0 -> IdentitySection(state, viewModel)
                        1 -> ContactSection(state, viewModel)
                        2 -> LocationSection(state, viewModel)
                        3 -> PhotosSection(state, viewModel)
                        4 -> BeneficiariesSection(
                            state = state,
                            viewModel = viewModel,
                            onShowAlert = { title, msg, action ->
                                alertTitle = title
                                alertMessage = msg
                                alertAction = action
                                showAlertDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation
            // Navigation
            SimpleNavigation(
                currentStep = currentStep,
                totalSteps = steps.size,
                isLoading = state.isLoading,
                onPrevious = { if (currentStep > 0) currentStep-- },
                onNext = {
                    if (viewModel.validateStep(currentStep)) {
                        if (currentStep < steps.size - 1) currentStep++
                    }
                },
                onSubmit = { viewModel.submitWithUpload(context, agentId) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Modals & Dialogs
        if (state.isModalVisible) {

            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )

            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { viewModel.hideModal() },
                containerColor = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    AddDependantModalContent(
                        viewModel = viewModel,
                        onSave = { viewModel.saveDependant() },
                        onCancel = { viewModel.hideModal() }
                    )
                }
            }
        }


        if (showAlertDialog) {
            SimpleAlertDialog(
                title = alertTitle,
                message = alertMessage,
                onConfirm = {
                    alertAction?.invoke()
                    showAlertDialog = false
                },
                onDismiss = { showAlertDialog = false }
            )
        }
    }
}

@Composable
fun ModernHeader(
    title: String,
    currentStep: Int,
    totalSteps: Int,
    totalCost: Int,
    totalBeneficiaries: Int,
    onBack: () -> Unit
) {
    Surface(
        color = AppColors.SurfaceBackground,
        shadowElevation = 1.dp,
        modifier = Modifier
            .padding(vertical = 30.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = AppColors.TextMain)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AppColors.TextMain)
                    Text("Étape ${currentStep + 1} sur $totalSteps", fontSize = 12.sp, color = AppColors.TextSub)
                }
            }

            // Infos Montant et Bénéficiaires à droite
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${totalCost.toLocaleString()} F",
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    color = AppColors.BrandBlue
                )
                Surface(
                    color = AppColors.SurfaceAlt,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$totalBeneficiaries bénéficiaire(s)",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextSub
                    )
                }
            }
        }
    }
}

@Composable
fun StepProgressBar(totalSteps: Int, currentStep: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (index <= currentStep) AppColors.BrandBlue else AppColors.BorderColor)
            )
        }
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = AppShapes.MediumRadius,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
        border = BorderStroke(1.dp, AppColors.BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AppColors.BrandBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = AppColors.TextMain, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

// --- Sections du formulaire ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerField(
    label: String,
    value: String?,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(modifier = modifier) {
        Box {
            OutlinedTextField(
                value = value ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(label, color = if (isError) AppColors.StatusRed else AppColors.TextSub) },
                placeholder = { Text("JJ/MM/AAAA", color = AppColors.TextSub.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Sélectionner une date",
                        tint = if (isError) AppColors.StatusRed else AppColors.BrandBlue
                    )
                },
                isError = isError,
                shape = AppShapes.MediumRadius,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppColors.SurfaceBackground,
                    unfocusedContainerColor = AppColors.SurfaceBackground,
                    focusedIndicatorColor = if (isError) AppColors.StatusRed else AppColors.BrandBlue,
                    unfocusedIndicatorColor = if (isError) AppColors.StatusRed else AppColors.BorderColor,
                    errorIndicatorColor = AppColors.StatusRed
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDialog = true }
            )
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppColors.StatusRed,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                                .format(Date(millis))
                            onDateSelected(formattedDate)
                        }
                        showDialog = false
                    }
                ) { Text("OK", color = AppColors.BrandBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler", color = AppColors.TextSub) }
            },
            colors = DatePickerDefaults.colors(containerColor = AppColors.SurfaceBackground)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// --- Sections du formulaire ---

@Composable
fun IdentitySection(state: AddAdherentUiState, viewModel: AddAdherentViewModel) {
    SectionCard("Informations personnelles", Icons.Outlined.Person) {
        AppTextField(
            value = state.prenoms,
            onValueChange = viewModel::updatePrenoms,
            label = "Prénoms*",
            placeholder = "Ex: Mamadou",
            isError = state.validationErrors.containsKey("prenoms"),
            errorMessage = state.validationErrors["prenoms"]
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppTextField(
            value = state.nom,
            onValueChange = viewModel::updateNom,
            label = "Nom*",
            placeholder = "Ex: Diop",
            isError = state.validationErrors.containsKey("nom"),
            errorMessage = state.validationErrors["nom"]
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppDatePickerField(
            label = "Date de naissance*",
            value = state.dateNaissance,
            onDateSelected = viewModel::updateDateNaissance,
            isError = state.validationErrors.containsKey("dateNaissance"),
            errorMessage = state.validationErrors["dateNaissance"]
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppDropdown(
            label = "Sexe*",
            options = FormConstants.SEXES,
            selected = state.sexe,
            onSelect = viewModel::updateSexe
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppDropdown(
            label = "Situation Matrimoniale*",
            options = FormConstants.SITUATIONS,
            selected = state.situationMatrimoniale,
            onSelect = viewModel::updateSituationMatrimoniale
        )
    }
}

@Composable
fun ContactSection(state: AddAdherentUiState, viewModel: AddAdherentViewModel) {
    SectionCard("Contact & ID", Icons.Outlined.Phone) {
        AppTextField(
            value = state.whatsapp,
            onValueChange = viewModel::updateWhatsapp,
            label = "WhatsApp*",
            placeholder = "Ex: 77 123 45 67",
            keyboardType = KeyboardType.Phone,
            isError = state.validationErrors.containsKey("whatsapp"),
            errorMessage = state.validationErrors["whatsapp"]
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppDropdown(
            label = "Type de pièce*",
            options = FormConstants.TYPES_PIECE,
            selected = state.typePiece,
            onSelect = viewModel::updateTypePiece
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.typePiece == "CNI") {
            AppTextField(
                value = state.numeroCNI,
                onValueChange = viewModel::updateNumeroCNI,
                label = "Numéro CNI*",
                placeholder = "Entrez le numéro",
                isError = state.validationErrors.containsKey("numeroCNI"),
                errorMessage = state.validationErrors["numeroCNI"]
            )
        } else {
            AppTextField(
                value = state.numeroExtrait,
                onValueChange = viewModel::updateNumeroExtrait,
                label = "Numéro Extrait*",
                placeholder = "Entrez le numéro",
                isError = state.validationErrors.containsKey("numeroExtrait"),
                errorMessage = state.validationErrors["numeroExtrait"]
            )
        }
    }
}

@Composable
fun LocationSection(state: AddAdherentUiState, viewModel: AddAdherentViewModel) {
    SectionCard("Localisation", Icons.Outlined.LocationOn) {
        AppDropdown(
            label = "Département*",
            options = FormConstants.DEPARTEMENTS,
            selected = state.departement,
            onSelect = viewModel::updateDepartement
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppTextField(
            value = state.commune,
            onValueChange = viewModel::updateCommune,
            label = "Commune*",
            placeholder = "Ville/Commune",
            isError = state.validationErrors.containsKey("commune"),
            errorMessage = state.validationErrors["commune"]
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppTextField(
            value = state.adresse,
            onValueChange = viewModel::updateAdresse,
            label = "Adresse*",
            placeholder = "Quartier, rue...",
            isError = state.validationErrors.containsKey("adresse"),
            errorMessage = state.validationErrors["adresse"]
        )
    }
}

@Composable
fun PhotosSection(state: AddAdherentUiState, viewModel: AddAdherentViewModel) {
    SectionCard("Documents", Icons.Outlined.CameraAlt) {
        ImagePickerComponent(
            label = "Photo d'identité*",
            imageUri = state.photoUri.toString(),
            onImageSelected = viewModel::updatePhotoUri,
            required = true,
            isError = state.validationErrors.containsKey("photoUri")
        )
        Spacer(modifier = Modifier.height(12.dp))
        ImagePickerComponent(label = "CNI Recto", imageUri = state.rectoUri.toString(), onImageSelected = viewModel::updateRectoUri)
        Spacer(modifier = Modifier.height(12.dp))
        ImagePickerComponent(label = "CNI Verso", imageUri = state.versoUri.toString(), onImageSelected = viewModel::updateVersoUri)
    }
}

@Composable
fun BeneficiariesSection(state: AddAdherentUiState, viewModel: AddAdherentViewModel, onShowAlert: (String, String, () -> Unit) -> Unit) {
    SectionCard("Bénéficiaires", Icons.Outlined.Group) {
        state.dependants.forEachIndexed { index, dep ->
            AppDependantCard(
                dependant = dep,
                onEdit = { viewModel.showEditDependantModal(index, dep) },
                onDelete = {
                    onShowAlert("Supprimer", "Retirer ce bénéficiaire ?") { viewModel.removeDependant(index) }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = { viewModel.showAddDependantModal() },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.MediumRadius,
            border = BorderStroke(1.dp, AppColors.BrandBlue.copy(0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.BrandBlue)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Ajouter une personne")
        }
    }
}

@Composable
fun AppDependantCard(
    dependant: PersonneChargeDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.BrandBlue.copy(alpha = 0.05f)
        ),
        shape = AppShapes.SmallRadius
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${dependant.prenoms} ${dependant.nom}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextMain,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "${dependant.sexe}",
                        fontSize = 12.sp,
                        color = AppColors.TextSub,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "Né(e) le: ${dependant.dateNaissance}",
                        fontSize = 12.sp,
                        color = AppColors.TextSub
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Modifier",
                        tint = AppColors.TextSub,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onEdit() }
                    )

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = AppColors.StatusRed,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleNavigation(
    currentStep: Int,
    totalSteps: Int,
    isLoading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = AppShapes.MediumRadius,
                border = BorderStroke(1.dp, AppColors.BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSub)
            ) {
                Text("Précédent")
            }
        }

        Button(
            onClick = if (currentStep == totalSteps - 1) onSubmit else onNext,
            modifier = Modifier.weight(1f).height(50.dp),
            enabled = !isLoading,
            shape = AppShapes.MediumRadius,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandBlue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (currentStep == totalSteps - 1) "Enregistrer" else "Suivant")
            }
        }
    }
}

@Composable
fun SimpleAlertDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = AppColors.TextMain) },
        text = { Text(message, color = AppColors.TextMain) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirmer", color = AppColors.StatusRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = AppColors.TextSub) }
        },
        containerColor = AppColors.SurfaceBackground,
        shape = AppShapes.MediumRadius
    )
}


@Composable
fun AppProgressIndicator(
    progress: Float? = null, // Si null, il tourne indéfiniment
    modifier: Modifier = Modifier.size(24.dp),
    color: Color = AppColors.BrandBlue,
    trackColor: Color = AppColors.BrandBlue.copy(alpha = 0.1f),
    strokeWidth: Dp = 3.dp
) {
    if (progress != null) {
        // --- Version déterminée (pour l'upload) ---
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "progress"
        )

        Canvas(modifier = modifier) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            // Rail (fond)
            drawCircle(color = trackColor, style = stroke)
            // Progression
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = stroke
            )
        }
    } else {
        // --- Version indéterminée (le spinner qui tourne) ---
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = keyframes { durationMillis = 1000 },
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )

        Canvas(modifier = modifier) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawCircle(color = trackColor, style = stroke)
            drawArc(
                color = color,
                startAngle = angle,
                sweepAngle = 90f, // Longueur du trait qui tourne
                useCenter = false,
                style = stroke
            )
        }
    }
}
// --- Composants AppTheme ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        if (value != null) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, color = if (isError) AppColors.StatusRed else AppColors.TextSub) },
                placeholder = { Text(placeholder, color = AppColors.TextSub.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
                enabled = onClick == null,
                readOnly = readOnly,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                shape = AppShapes.MediumRadius,
                trailingIcon = trailingIcon,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppColors.SurfaceBackground,
                    unfocusedContainerColor = AppColors.SurfaceBackground,
                    disabledContainerColor = AppColors.SurfaceBackground,
                    focusedIndicatorColor = if (isError) AppColors.StatusRed else AppColors.BrandBlue,
                    unfocusedIndicatorColor = if (isError) AppColors.StatusRed else AppColors.BorderColor,
                    errorIndicatorColor = AppColors.StatusRed,
                    cursorColor = AppColors.BrandBlue,
                    focusedLabelColor = AppColors.BrandBlue,
                    unfocusedLabelColor = AppColors.TextSub,
                )
            )
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppColors.StatusRed,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(
                        label,
                        color = if (isError) AppColors.StatusRed else AppColors.TextSub
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                isError = isError,
                shape = AppShapes.MediumRadius,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppColors.SurfaceBackground,
                    unfocusedContainerColor = AppColors.SurfaceBackground,
                    focusedIndicatorColor = if (isError) AppColors.StatusRed else AppColors.BrandBlue,
                    unfocusedIndicatorColor = if (isError) AppColors.StatusRed else AppColors.BorderColor,
                    errorIndicatorColor = AppColors.StatusRed
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(AppColors.SurfaceBackground)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = AppColors.TextMain) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppColors.StatusRed,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}