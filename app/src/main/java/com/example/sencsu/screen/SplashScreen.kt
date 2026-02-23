package com.example.sencsu.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sencsu.domain.viewmodel.SplashViewModel
import com.example.sencsu.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit, onNavigateToDashboard: () -> Unit) {
    val viewModel: SplashViewModel = hiltViewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var startAnimation by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showLoader by remember { mutableStateOf(false) }

    // Animation du logo (rebond élégant)
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    // Fade in du logo
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logo_alpha"
    )

    // Animation de pulsation douce du cercle décoratif
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(400)
        showSubtitle = true
        delay(300)
        showLoader = true
        delay(1000)

        isLoggedIn?.let {
            if (it) onNavigateToDashboard() else onNavigateToLogin()
        } ?: onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.BrandBlue,
                        Color(0xFF1A3F87),
                        AppColors.BrandBlueDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Cercles décoratifs en arrière-plan
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale * 0.95f)
                .alpha(pulseAlpha * 0.7f)
                .clip(CircleShape)
                .background(Color.White)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo animé
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "S",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = AppColors.BrandBlue,
                        fontSize = 52.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Nom de l'app
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(tween(500)) + slideInVertically(
                    animationSpec = tween(500),
                    initialOffsetY = { 30 }
                )
            ) {
                Text(
                    text = "SenCSU",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sous-titre
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(400)) + slideInVertically(
                    animationSpec = tween(400),
                    initialOffsetY = { 20 }
                )
            ) {
                Text(
                    text = "Couverture Santé Universelle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(56.dp))

            // Indicateur de chargement animé
            AnimatedVisibility(
                visible = showLoader,
                enter = fadeIn(tween(300)) + scaleIn(tween(300))
            ) {
                CircularProgressIndicator(
                    color = Color.White.copy(alpha = 0.7f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Version en bas
        AnimatedVisibility(
            visible = showSubtitle,
            enter = fadeIn(tween(600)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}