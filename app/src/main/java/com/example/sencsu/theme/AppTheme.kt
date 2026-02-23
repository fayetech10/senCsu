package com.example.sencsu.theme


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sencsu.ui.theme.Typography

// ==============================================================================
// DÉFINITION DES COULEURS
// ==============================================================================

object AppColors {
    // ── Palette principale CMU (Couverture Maladie Universelle) ──
    // Vert CMU profond — couleur primaire
    val BrandBlue = Color(0xFF1B7C3D)          // renommé logiquement → vert CMU
    val BrandBlueDark = Color(0xFF145C2C)       // vert foncé
    val BrandBlueLite = Color(0xFFE8F5EC)       // vert très clair (fond chip/avatar)

    // Gris neutres CMU
    val AppBackground = Color(0xFFF4F6F5)       // fond général gris-vert très clair
    val SurfaceBackground = Color(0xFFFFFFFF)   // surface blanche
    val SurfaceAlt = Color(0xFFF0F4F1)          // surface alternative gris-vert

    // Couleurs de statut
    val StatusGreen = Color(0xFF2ECC71)         // vert vif statut actif
    val StatusGreenDark = Color(0xFF1A9B50)     // vert foncé texte actif
    val StatusOrange = Color(0xFFFB923C)        // orange avertissement
    val StatusOrangeDark = Color(0xFFDC2626)    // rouge foncé
    val StatusRed = Color(0xFFEF4444)           // rouge erreur

    // Couleurs de texte
    val TextMain = Color(0xFF1A2E22)            // presque noir teinté vert
    val TextSub = Color(0xFF5C7264)             // gris-vert moyen
    val TextDisabled = Color(0xFF9DB5A3)        // gris-vert clair
    val TextMuted = Color(0xFFCCDDD3)           // gris très clair

    // Couleurs de bordure
    val BorderColor = Color(0xFFD5E6D9)         // vert-gris clair
    val BorderColorLight = Color(0xFFEBF4EE)    // vert-gris très clair

    // Couleurs additionnelles
    val SuccessLight = Color(0xFFD1FAE5)        // fond succès
    val WarningLight = Color(0xFFFEF3C7)        // fond avertissement
    val DangerLight = Color(0xFFFEE2E2)        // fond danger
    val InfoLight = Color(0xFFE8F5EC)           // fond info (même ton vert)
}

// ==============================================================================
// FORMES PERSONNALISÉES
// ==============================================================================

object AppShapes {
    val LargeRadius: RoundedCornerShape = RoundedCornerShape(24.dp)
    val MediumRadius: RoundedCornerShape = RoundedCornerShape(16.dp)
    val SmallRadius: RoundedCornerShape = RoundedCornerShape(12.dp)
    val ExtraSmallRadius: RoundedCornerShape = RoundedCornerShape(8.dp)
    val CircleRadius: RoundedCornerShape = RoundedCornerShape(50)
}

// ==============================================================================
// DIMENSIONS ET ESPACEMENTS
// ==============================================================================

object AppDimensions {
    // Espacements
    val paddingXSmall: Dp = 4.dp
    val paddingSmall: Dp = 8.dp
    val paddingMedium: Dp = 16.dp
    val paddingLarge: Dp = 20.dp
    val paddingXLarge: Dp = 24.dp
    val paddingXXLarge: Dp = 32.dp

    // Hauteurs
    val buttonHeight: Dp = 56.dp
    val buttonSmallHeight: Dp = 40.dp
    val appBarHeight: Dp = 64.dp

    // Largeurs/Tailles
    val cardElevation: Dp = 2.dp
    val borderWidth: Dp = 1.dp
    val dividerThickness: Dp = 0.5.dp

    // Icônes
    val iconSmall: Dp = 16.dp
    val iconMedium: Dp = 24.dp
    val iconLarge: Dp = 32.dp
    val iconXLarge: Dp = 48.dp

    // Images
    val profileImageSize: Dp = 110.dp
    val avatarSize: Dp = 50.dp
    val thumbnailSize: Dp = 100.dp
}

// ==============================================================================
// ÉLÉVATION ET OMBRES
// ==============================================================================

object AppElevation {
    val card: Dp = 0.dp
    val button: Dp = 4.dp
    val fab: Dp = 6.dp
    val dialog: Dp = 24.dp
}

// ==============================================================================
// DURATIONS (Animations)
// ==============================================================================

object AppDurations {
    const val SHORT: Long = 150L
    const val MEDIUM: Long = 300L
    const val LONG: Long = 500L
}

// ==============================================================================
// SCHÉMA DE COULEUR MATERIAL 3
// ==============================================================================

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = AppColors.BrandBlue,
        onPrimary = Color.White,
        secondary = AppColors.StatusGreenDark,
        onSecondary = Color.White,
        tertiary = AppColors.StatusOrange,
        onTertiary = Color.White,
        error = AppColors.StatusRed,
        onError = Color.White,
        background = AppColors.AppBackground,
        onBackground = AppColors.TextMain,
        surface = AppColors.SurfaceBackground,
        onSurface = AppColors.TextMain,
        surfaceVariant = AppColors.SurfaceAlt,
        onSurfaceVariant = AppColors.TextSub,
        outline = AppColors.BorderColor,
        outlineVariant = AppColors.BorderColorLight,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ==============================================================================
// UTILITAIRES DE COULEUR
// ==============================================================================

/**
 * Extension pour faciliter l'utilisation des couleurs avec transparence
 */
fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha)

/**
 * Extension pour obtenir la couleur complémentaire
 */
fun Color.getComplementary(): Color {
    val r = 255 - (this.red * 255).toInt()
    val g = 255 - (this.green * 255).toInt()
    val b = 255 - (this.blue * 255).toInt()
    return Color(r, g, b)
}

/**
 * Extension pour assombrir une couleur
 */
fun Color.darken(factor: Float = 0.2f): Color {
    return Color(
        red = (this.red * (1 - factor)).coerceIn(0f, 1f),
        green = (this.green * (1 - factor)).coerceIn(0f, 1f),
        blue = (this.blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}

/**
 * Extension pour éclaircir une couleur
 */
fun Color.lighten(factor: Float = 0.2f): Color {
    return Color(
        red = (this.red + (1 - this.red) * factor).coerceIn(0f, 1f),
        green = (this.green + (1 - this.green) * factor).coerceIn(0f, 1f),
        blue = (this.blue + (1 - this.blue) * factor).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}