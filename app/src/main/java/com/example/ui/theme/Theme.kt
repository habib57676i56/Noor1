package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode

private val NoorDarkColorScheme = darkColorScheme(
    primary = EmeraldSoft,
    onPrimary = EmeraldDeep,
    primaryContainer = EmeraldForest,
    onPrimaryContainer = EmeraldMist,
    secondary = GoldPrimary,
    onSecondary = NightBlack,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldBright,
    tertiary = EmeraldBright,
    onTertiary = Color.White,
    background = NightBlack,
    onBackground = DarkInk,
    surface = NightSurface,
    onSurface = DarkInk,
    surfaceVariant = NightSurfaceElevated,
    onSurfaceVariant = DarkInkMuted,
    outline = Color(0xFF27453A),
    outlineVariant = Color(0xFF172E26),
    error = SemanticError,
    onError = Color.White
)

private val NoorLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldMist,
    onPrimaryContainer = EmeraldDeep,
    secondary = GoldMuted,
    onSecondary = Color.White,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = Color(0xFF4A3611),
    tertiary = EmeraldBright,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightInkMuted,
    outline = Color(0xFFC1D3CB),
    outlineVariant = Color(0xFFDAE6E0),
    error = SemanticError,
    onError = Color.White
)

val NoorShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

data class NoorExtendedColors(
    val isDark: Boolean,
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val goldAccent: Color,
    val goldSoft: Color,
    val emeraldGlow: Color,
    val patternTint: Color,
    val ivoryCardBg: Color,
    val ivoryCardInk: Color
)

val LocalNoorColors = staticCompositionLocalOf {
    NoorExtendedColors(
        isDark = true,
        glassSurface = NightSurfaceGlass,
        glassSurfaceStrong = NightSurfaceElevated,
        glassBorderTop = GlassHighlightDark,
        glassBorderBottom = GlassBorderDark,
        goldAccent = GoldPrimary,
        goldSoft = GoldBright,
        emeraldGlow = EmeraldPrimary,
        patternTint = Color(0x1AD9B464),
        ivoryCardBg = IvorySurface,
        ivoryCardInk = IvoryInk
    )
}

@Composable
fun NoorTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) NoorDarkColorScheme else NoorLightColorScheme
    val extendedColors = if (isDark) {
        NoorExtendedColors(
            isDark = true,
            glassSurface = Color(0xC20D2019),
            glassSurfaceStrong = Color(0xE6112B22),
            glassBorderTop = Color(0x3DFFFFFF),
            glassBorderBottom = Color(0x3DD9B464),
            goldAccent = GoldPrimary,
            goldSoft = GoldBright,
            emeraldGlow = EmeraldPrimary,
            patternTint = Color(0x1AD9B464),
            ivoryCardBg = IvorySurface,
            ivoryCardInk = IvoryInk
        )
    } else {
        NoorExtendedColors(
            isDark = false,
            glassSurface = Color(0xD8FFFFFF),
            glassSurfaceStrong = Color(0xF5FFFFFF),
            glassBorderTop = Color(0xEEFFFFFF),
            glassBorderBottom = Color(0x420F6E4E),
            goldAccent = GoldMuted,
            goldSoft = GoldPrimary,
            emeraldGlow = EmeraldMist,
            patternTint = Color(0x150F6E4E),
            ivoryCardBg = Color(0xFFFDF8EE),
            ivoryCardInk = IvoryInk
        )
    }

    CompositionLocalProvider(LocalNoorColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoorTypography,
            shapes = NoorShapes,
            content = content
        )
    }
}
