package com.example.sencsu.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Listen for one-time UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is LoginUiEvent.NavigateToDashboard -> onLoginSuccess()
            }
        }
    }

    Scaffold(
        containerColor = AppColors.AppBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Background decoration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(AppColors.BrandBlue, AppColors.AppBackground)
                        )
                    )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = AppShapes.LargeRadius,
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = AppShapes.CircleRadius,
                            color = AppColors.BrandBlueLite
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = AppColors.BrandBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Bienvenue",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextMain
                        )
                        Text(
                            text = "Connectez-vous pour continuer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSub
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Inputs
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Rounded.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.MediumRadius,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppColors.SurfaceAlt,
                            unfocusedContainerColor = AppColors.SurfaceAlt,
                            focusedBorderColor = AppColors.BrandBlue,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.MediumRadius,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppColors.SurfaceAlt,
                            unfocusedContainerColor = AppColors.SurfaceAlt,
                            focusedBorderColor = AppColors.BrandBlue,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )

                    // Error Message
                    if (state.error != null) {
                        Surface(
                            color = AppColors.StatusRed.copy(alpha = 0.1f),
                            shape = AppShapes.SmallRadius,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.error ?: "",
                                color = AppColors.StatusRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Action Button
                    Button(
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = AppShapes.MediumRadius,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.BrandBlue,
                            contentColor = Color.White
                        ),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Se connecter", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Footer text
            Text(
                "Version 1.0.0",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSub
            )
        }
    }
}