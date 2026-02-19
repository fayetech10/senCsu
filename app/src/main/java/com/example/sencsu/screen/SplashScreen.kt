package com.example.sencsu.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sencsu.domain.viewmodel.SplashViewModel
import com.example.sencsu.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit, onNavigateToDashboard: () -> Unit) {
    val viewModel: SplashViewModel = hiltViewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var startAnimation by remember { mutableStateOf(false) }
    val scale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1500) // Simulate loading/intro time
        
        // Check ViewModel condition after animation
        isLoggedIn?.let {
            if (it) {
                onNavigateToDashboard()
            } else {
                onNavigateToLogin()
            }
        } ?: run {
             // Fallback if state is not ready yet
             onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BrandBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Using a simple icon as a placeholder for Logo if no resource is available
            // In a real app, use painterResource(R.drawable.logo)
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Placeholder Icon
                     Text(
                        "S",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = AppColors.BrandBlue
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "SenCSU",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
             Spacer(modifier = Modifier.height(48.dp))
             
             CircularProgressIndicator(
                 color = Color.White.copy(alpha = 0.7f),
                 strokeWidth = 2.dp,
                 modifier = Modifier.size(24.dp)
             )
        }
    }
}