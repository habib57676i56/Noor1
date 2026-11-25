package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
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
    outline = Color(0xFF2A4D43),
    outlineVariant = Color(0xFF16322A),
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
    outline = Color(0xFFB7D1C8),
    outlineVariant = Color(0xFFD5E6E0),
    error = SemanticError,
    onError = Color.White
)

val NoorShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
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
    val ivoryCardInk: Color,
    val glassSpecularTop: Color = LiquidSpecularDark,
    val glassOrbPrimary: Color = LiquidAuroraEmerald,
    val glassOrbSecondary: Color = LiquidAuroraGold,
    val glassOrbAccent: Color = LiquidAuroraCyan,
    val glassDockTop: Color = Color(0xDE0E2D26),
    val glassDockBottom: Color = Color(0xEB051512)
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
        patternTint = Color(0x1BE2BE6E),
        ivoryCardBg = IvorySurface,
        ivoryCardInk = IvoryInk,
        glassSpecularTop = LiquidSpecularDark,
        glassOrbPrimary = LiquidAuroraEmerald,
        glassOrbSecondary = LiquidAuroraGold,
        glassOrbAccent = LiquidAuroraCyan,
        glassDockTop = Color(0xDE0E2D26),
        glassDockBottom = Color(0xEB051512)
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
            glassSurface = Color(0xB30B241E),
            glassSurfaceStrong = Color(0xD90E2D26),
            glassBorderTop = Color(0x57FFFFFF),
            glassBorderBottom = Color(0x4AE2BE6E),
            goldAccent = GoldPrimary,
            goldSoft = GoldBright,
            emeraldGlow = EmeraldPrimary,
            patternTint = Color(0x1AE2BE6E),
            ivoryCardBg = IvorySurface,
            ivoryCardInk = IvoryInk,
            glassSpecularTop = LiquidSpecularDark,
            glassOrbPrimary = LiquidAuroraEmerald,
            glassOrbSecondary = LiquidAuroraGold,
            glassOrbAccent = LiquidAuroraCyan,
            glassDockTop = Color(0xDF0F3129),
            glassDockBottom = Color(0xEE051612)
        )
    } else {
        NoorExtendedColors(
            isDark = false,
            glassSurface = Color(0xC9FFFFFF),
            glassSurfaceStrong = Color(0xEBFFFFFF),
            glassBorderTop = Color(0xF5FFFFFF),
            glassBorderBottom = Color(0x470E7C5D),
            goldAccent = GoldMuted,
            goldSoft = GoldPrimary,
            emeraldGlow = EmeraldMist,
            patternTint = Color(0x140E7C5D),
            ivoryCardBg = Color(0xFFFDF8EE),
            ivoryCardInk = IvoryInk,
            glassSpecularTop = LiquidSpecularLight,
            glassOrbPrimary = LiquidAuroraLightEmerald,
            glassOrbSecondary = LiquidAuroraLightGold,
            glassOrbAccent = LiquidAuroraLightCyan,
            glassDockTop = Color(0xF0FFFFFF),
            glassDockBottom = Color(0xE0E8F4F0)
        )
    }

    CompositionLocalProvider(LocalNoorColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoorTypography,
            shapes = NoorShapes
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onBackground,
                content = content
            )
        }
    }
}
