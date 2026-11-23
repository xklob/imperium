package io.klob.imperium.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.klob.imperium.R
import io.klob.imperium.settings.ThemeMode

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

@Immutable
data class KlobColors(
    val isDark: Boolean,
    val surface: Color,
    val surfaceElevated: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val hover: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentBorder: Color,
    val brandGreen: Color,
    val onBrandGreen: Color,
    val dragLockRed: Color,
    val warning: Color,
)

private val LightKlobColors = KlobColors(
    isDark = false,
    surface = Color(0xFFFAFAF8),
    surfaceElevated = Color(0xFFF3F3EF),
    text = Color(0xFF202020),
    muted = Color(0xFF686862),
    line = Color(0xFFDEDED7),
    hover = Color(0xFFF0F0EB),
    accent = Color(0xFF007500),
    accentSoft = Color(0xFFECF8E8),
    accentBorder = Color(0xFF8BBF82),
    brandGreen = Color(0xFF00FF00),
    onBrandGreen = Color(0xFF171717),
    dragLockRed = Color(0xFFFF1E2D),
    warning = Color(0xFFB56A00),
)

private val DarkKlobColors = KlobColors(
    isDark = true,
    surface = Color(0xFF171717),
    surfaceElevated = Color(0xFF1F1F1D),
    text = Color(0xFFEEEEEE),
    muted = Color(0xFFA5A5A0),
    line = Color(0xFF363634),
    hover = Color(0xFF222220),
    accent = Color(0xFF00FF00),
    accentSoft = Color(0xFF172B15),
    accentBorder = Color(0xFF3B6434),
    brandGreen = Color(0xFF00FF00),
    onBrandGreen = Color(0xFF171717),
    dragLockRed = Color(0xFFFF1E2D),
    warning = Color(0xFFE5A024),
)

val LocalKlobColors = staticCompositionLocalOf { DarkKlobColors }

private val ImperiumTypography = Typography(
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp),
    labelMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.5.sp),
)

@Composable
fun ImperiumTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val klob = if (useDark) DarkKlobColors else LightKlobColors
    val materialColors = if (useDark) {
        darkColorScheme(
            primary = klob.brandGreen,
            onPrimary = klob.onBrandGreen,
            background = klob.surface,
            onBackground = klob.text,
            surface = klob.surface,
            onSurface = klob.text,
            surfaceVariant = klob.surfaceElevated,
            onSurfaceVariant = klob.muted,
            outline = klob.line,
        )
    } else {
        lightColorScheme(
            primary = klob.accent,
            onPrimary = Color.White,
            background = klob.surface,
            onBackground = klob.text,
            surface = klob.surface,
            onSurface = klob.text,
            surfaceVariant = klob.surfaceElevated,
            onSurfaceVariant = klob.muted,
            outline = klob.line,
        )
    }

    CompositionLocalProvider(LocalKlobColors provides klob) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = ImperiumTypography,
            content = content,
        )
    }
}
