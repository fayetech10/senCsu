package com.example.sencsu.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sencsu.domain.viewmodel.PaiementViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

data class PaiementFormState(
    val reference: String = "",
    val photoPaiement: Uri? = null,
    val modePaiement: String = "Virement",
    val montantTotal: Double? = null,
    val adherentId: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val isSuccess: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paiement(
    adherentId: Long?,
    montantTotal: Double?,
    navController: NavController,
    viewModel: PaiementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState
    val recognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }

    // Initialize form data
    LaunchedEffect(adherentId, montantTotal) {
        viewModel.initializeFormData(adherentId, montantTotal)
    }
    LaunchedEffect(adherentId) {
        adherentId?.let {
            viewModel.loadAdherent(it)
        }
    }

    // Redirect after success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate("main_tabs") {
                popUpTo("paiement") { inclusive = true }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            state.photoPaiement?.let { uri ->
                viewModel.processImage(uri, context, recognizer)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.setPhoto(it)
            viewModel.processImage(it, context, recognizer)
        }
    }

    Scaffold(
        containerColor = AppColors.AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Paiement Cotisation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextMain
                    ) 
                },
                navigationIcon = {
                     IconButton(onClick = { navController.popBackStack() }) {
                         Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour", tint = AppColors.TextMain)
                     }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.SurfaceBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Photo Section
            PaymentPhotoSection(
                photoUri = state.photoPaiement,
                isLoading = state.isLoading,
                onRemove = { viewModel.setPhoto(null) }
            )

            // Action Buttons
            PhotoActionsRow(
                onCameraClick = {
                    val uri = createTempPictureUri(context)
                    viewModel.setPhoto(uri)
                    cameraLauncher.launch(uri)
                },
                onGalleryClick = { galleryLauncher.launch("image/*") }
            )

            // Error Display
            if (state.errorMessage.isNotEmpty()) {
                ErrorMessageCard(message = state.errorMessage)
            }

            // Reference Field
            OutlinedTextField(
                value = state.reference,
                onValueChange = viewModel::updateReference,
                label = {
                    Text(
                        "Référence ${if (state.reference.isBlank()) "(détectée ou manuelle)" else "(validée)"}",
                        color = AppColors.TextSub
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = AppColors.TextSub) },
                isError = state.reference.isBlank(),
                singleLine = true,
                shape = AppShapes.MediumRadius,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.BrandBlue,
                    unfocusedBorderColor = AppColors.BorderColor,
                    errorBorderColor = AppColors.StatusRed,
                    focusedContainerColor = AppColors.SurfaceBackground,
                    unfocusedContainerColor = AppColors.SurfaceBackground
                )
            )

            // Payment Mode Dropdown
            ModePaiementDropdown(
                selectedMode = state.modePaiement,
                onModeSelected = viewModel::updateMode
            )

            // Amount Field (Read-only)
            OutlinedTextField(
                value = montantTotal.toString(),
                onValueChange = {},
                label = { Text("Montant (FCFA)", color = AppColors.TextSub) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                shape = AppShapes.MediumRadius,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.BorderColor,
                    unfocusedBorderColor = AppColors.BorderColor,
                    disabledTextColor = AppColors.TextMain,
                    disabledBorderColor = AppColors.BorderColor,
                    disabledLabelColor = AppColors.TextSub,
                    disabledContainerColor = AppColors.SurfaceAlt
                )
            )

            // Confirm Button
            Button(
                onClick = { viewModel.addPaiement() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = AppShapes.MediumRadius,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.BrandBlue,
                    contentColor = Color.White,
                    disabledContainerColor = AppColors.BrandBlue.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                enabled = state.reference.isNotBlank() &&
                        state.photoPaiement != null &&
                        !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = if (state.isLoading) "Confirmation en cours..." else "Confirmer le paiement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.MediumRadius,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.StatusRed.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.StatusRed.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = "Erreur",
                tint = AppColors.StatusRed
            )
            Text(
                text = message,
                color = AppColors.StatusRed,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PaymentPhotoSection(
    photoUri: Uri?,
    isLoading: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = AppShapes.LargeRadius,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.SurfaceAlt
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderColor.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Photo de justificatif",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            AppColors.StatusRed.copy(alpha = 0.8f),
                            AppShapes.CircleRadius
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Supprimer la photo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.CameraAlt,
                        contentDescription = "Caméra",
                        modifier = Modifier.size(56.dp),
                        tint = AppColors.TextSub.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aucun justificatif",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextSub
                    )
                    Text(
                        "Prenez une photo du reçu",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextDisabled
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PhotoActionsRow(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = AppShapes.MediumRadius,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.SurfaceBackground,
                contentColor = AppColors.BrandBlue
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BrandBlue.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Caméra")
        }
        
        Button(
            onClick = onGalleryClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = AppShapes.MediumRadius,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.SurfaceBackground,
                contentColor = AppColors.TextMain
            ),
             border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderColor)
        ) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
             Spacer(Modifier.width(8.dp))
            Text("Galerie")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModePaiementDropdown(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("Virement", "Wave", "Orange Money", "Espèces", "Chèque")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedMode,
            onValueChange = {},
            readOnly = true,
            label = { Text("Moyen de paiement", color = AppColors.TextSub) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.BrandBlue,
                unfocusedBorderColor = AppColors.BorderColor,
                focusedContainerColor = AppColors.SurfaceBackground,
                unfocusedContainerColor = AppColors.SurfaceBackground
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            shape = AppShapes.MediumRadius
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.SurfaceBackground)
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(text = mode, color = AppColors.TextMain) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

fun createTempPictureUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "payment_proof_",
        ".jpg",
        context.cacheDir
    ).apply {
        deleteOnExit()
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}