package com.example.sencsu.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sencsu.domain.viewmodel.LoginUiEvent
import com.example.sencsu.domain.viewmodel.LoginViewModel
import com.example.sencsu.theme.AppColors
import com.example.sencsu.theme.AppShapes
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Animations d'entrée séquentielles
    var showContent by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        showContent = true
        delay(200)
        showForm = true
        delay(150)
        showButton = true
    }

    // Listen for one-time UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is LoginUiEvent.NavigateToDashboard -> onLoginSuccess()
            }
        }
    }

    // Cercles décoratifs animés
    val infiniteTransition = rememberInfiniteTransition(label = "deco")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Scaffold(
        containerColor = AppColors.AppBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Background gradient haut
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                AppColors.BrandBlue,
                                Color(0xFF1A5CC8),
                                AppColors.AppBackground
                            )
                        )
                    )
            )

            // Cercles décoratifs flottants
            Box(
                modifier = Modifier
                    .offset(x = (-60).dp, y = (-180 + floatAnim).dp)
                    .size(140.dp)
                    .alpha(0.08f)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .offset(x = 100.dp, y = (-120 + floatAnim * 0.7f).dp)
                    .size(80.dp)
                    .alpha(0.06f)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.TopCenter)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header animé (Logo + Textes)
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        animationSpec = tween(500),
                        initialOffsetY = { -40 }
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 12.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "S",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Black,
                                    color = AppColors.BrandBlue
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Bienvenue",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Connectez-vous pour continuer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Card formulaire
                AnimatedVisibility(
                    visible = showForm,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        animationSpec = tween(400, easing = EaseOutCubic),
                        initialOffsetY = { 60 }
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(horizontal = 8.dp),
                        shape = AppShapes.LargeRadius,
                        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Email
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Email, null,
                                        tint = AppColors.TextSub
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.MediumRadius,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.SurfaceAlt,
                                    unfocusedContainerColor = AppColors.SurfaceAlt,
                                    focusedBorderColor = AppColors.BrandBlue,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedLeadingIconColor = AppColors.BrandBlue
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )

                            // Password
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Mot de passe") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Lock, null,
                                        tint = AppColors.TextSub
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Rounded.Visibility
                                            else Icons.Rounded.VisibilityOff,
                                            null,
                                            tint = AppColors.TextSub
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None
                                else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.MediumRadius,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.SurfaceAlt,
                                    unfocusedContainerColor = AppColors.SurfaceAlt,
                                    focusedBorderColor = AppColors.BrandBlue,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedLeadingIconColor = AppColors.BrandBlue
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (email.isNotBlank() && password.isNotBlank()) {
                                            viewModel.login(email, password)
                                        }
                                    }
                                )
                            )

                            // Error Message avec animation
                            AnimatedVisibility(
                                visible = state.error != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Surface(
                                    color = AppColors.StatusRed.copy(alpha = 0.1f),
                                    shape = AppShapes.SmallRadius,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.error ?: "",
                                        color = AppColors.StatusRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // Bouton de connexion
                            Button(
                                onClick = { viewModel.login(email, password) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = AppShapes.MediumRadius,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.BrandBlue,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                ),
                                enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank()
                            ) {
                                AnimatedContent(
                                    targetState = state.isLoading,
                                    transitionSpec = {
                                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                                    },
                                    label = "loading"
                                ) { isLoading ->
                                    if (isLoading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                "Connexion...",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text(
                                            "Se connecter",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer text
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(500)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "SenCSU · Version 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSub,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}