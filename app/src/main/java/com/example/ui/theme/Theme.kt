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

/**
 * Live Super Admin theme palette presets applied app-wide across Material 3,
 * Liquid Glass cards, background auroras, and accent highlights.
 */
enum class NoorThemePreset(
    val key: String,
    val labelAr: String,
    val labelEn: String,
    val swatchPrimary: Color,
    val swatchAccent: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val darkGold: Color,
    val darkGoldSoft: Color,
    val darkBgTop: Color,
    val darkBgMid: Color,
    val darkBgBottom: Color,
    val darkCardTop: Color,
    val darkCardMid: Color,
    val darkCardBottom: Color,
    val darkOrbPrimary: Color,
    val darkOrbSecondary: Color,
    val darkOrbAccent: Color,
    val lightPrimary: Color,
    val lightBgTop: Color,
    val lightBgBottom: Color
) {
    EMERALD_GOLD(
        key = "emerald_gold",
        labelAr = "الزمرد والذهب",
        labelEn = "Emerald & Gold",
        swatchPrimary = Color(0xFF0E7C5D),
        swatchAccent = Color(0xFFE2BE6E),
        darkPrimary = EmeraldSoft,
        darkContainer = EmeraldForest,
        darkGold = GoldPrimary,
        darkGoldSoft = GoldBright,
        darkBgTop = Color(0xFF031411),
        darkBgMid = Color(0xFF020C0A),
        darkBgBottom = Color(0xFF041311),
        darkCardTop = Color(0xFF13352D),
        darkCardMid = Color(0xFF0A211C),
        darkCardBottom = Color(0xFF061511),
        darkOrbPrimary = LiquidAuroraEmerald,
        darkOrbSecondary = LiquidAuroraGold,
        darkOrbAccent = LiquidAuroraCyan,
        lightPrimary = EmeraldPrimary,
        lightBgTop = Color(0xFFE7F4F0),
        lightBgBottom = Color(0xFFF3F8F6)
    ),
    SAPPHIRE_GOLD(
        key = "sapphire_gold",
        labelAr = "الياقوت السماوي",
        labelEn = "Celestial Sapphire",
        swatchPrimary = Color(0xFF1D6FE2),
        swatchAccent = Color(0xFFF2CC78),
        darkPrimary = Color(0xFF60A5FA),
        darkContainer = Color(0xFF0E2952),
        darkGold = Color(0xFFF2CC78),
        darkGoldSoft = Color(0xFFFFE4A3),
        darkBgTop = Color(0xFF040E21),
        darkBgMid = Color(0xFF030916),
        darkBgBottom = Color(0xFF06142A),
        darkCardTop = Color(0xFF112A52),
        darkCardMid = Color(0xFF0B1C38),
        darkCardBottom = Color(0xFF061124),
        darkOrbPrimary = Color(0x522563EB),
        darkOrbSecondary = Color(0x3DF2CC78),
        darkOrbAccent = Color(0x4538BDF8),
        lightPrimary = Color(0xFF1B57B8),
        lightBgTop = Color(0xFFE6F0FA),
        lightBgBottom = Color(0xFFF2F6FC)
    ),
    ROYAL_AMETHYST(
        key = "royal_amethyst",
        labelAr = "الجمشت الأندلسي",
        labelEn = "Royal Amethyst",
        swatchPrimary = Color(0xFF8B5CF6),
        swatchAccent = Color(0xFFF0C97A),
        darkPrimary = Color(0xFFC4B5FD),
        darkContainer = Color(0xFF2D1654),
        darkGold = Color(0xFFF0C97A),
        darkGoldSoft = Color(0xFFFFE3A3),
        darkBgTop = Color(0xFF110722),
        darkBgMid = Color(0xFF0A0416),
        darkBgBottom = Color(0xFF140929),
        darkCardTop = Color(0xFF271449),
        darkCardMid = Color(0xFF190C31),
        darkCardBottom = Color(0xFF0E071D),
        darkOrbPrimary = Color(0x548B5CF6),
        darkOrbSecondary = Color(0x3AF0C97A),
        darkOrbAccent = Color(0x42E879F9),
        lightPrimary = Color(0xFF6D28D9),
        lightBgTop = Color(0xFFF1EBFA),
        lightBgBottom = Color(0xFFF8F4FC)
    ),
    MAKKAH_OBSIDIAN(
        key = "makkah_obsidian",
        labelAr = "أسود الكعبة والذهب",
        labelEn = "Kaaba Obsidian",
        swatchPrimary = Color(0xFF1C1C22),
        swatchAccent = Color(0xFFFFD166),
        darkPrimary = Color(0xFFFFD166),
        darkContainer = Color(0xFF2B2415),
        darkGold = Color(0xFFFFD166),
        darkGoldSoft = Color(0xFFFFE79E),
        darkBgTop = Color(0xFF0A0A0D),
        darkBgMid = Color(0xFF050507),
        darkBgBottom = Color(0xFF100E0A),
        darkCardTop = Color(0xFF201D17),
        darkCardMid = Color(0xFF151310),
        darkCardBottom = Color(0xFF0C0B09),
        darkOrbPrimary = Color(0x42FFD166),
        darkOrbSecondary = Color(0x36D4AF37),
        darkOrbAccent = Color(0x28F59E0B),
        lightPrimary = Color(0xFF8A6518),
        lightBgTop = Color(0xFFF6F1E6),
        lightBgBottom = Color(0xFFFAF7F0)
    ),
    DAMASCUS_RUBY(
        key = "damascus_ruby",
        labelAr = "الياقوت الدمشقي",
        labelEn = "Damascus Ruby",
        swatchPrimary = Color(0xFFBE185D),
        swatchAccent = Color(0xFFF5D08A),
        darkPrimary = Color(0xFFF472B6),
        darkContainer = Color(0xFF47112A),
        darkGold = Color(0xFFF5D08A),
        darkGoldSoft = Color(0xFFFFE5B4),
        darkBgTop = Color(0xFF1A0611),
        darkBgMid = Color(0xFF10030A),
        darkBgBottom = Color(0xFF1E0814),
        darkCardTop = Color(0xFF391225),
        darkCardMid = Color(0xFF260B19),
        darkCardBottom = Color(0xFF16060F),
        darkOrbPrimary = Color(0x52DB2777),
        darkOrbSecondary = Color(0x3DF5D08A),
        darkOrbAccent = Color(0x40FB7185),
        lightPrimary = Color(0xFFA61B55),
        lightBgTop = Color(0xFFFAECF2),
        lightBgBottom = Color(0xFFFDF4F8)
    ),
    ANDALUS_OLIVE(
        key = "andalus_olive",
        labelAr = "زيتون الأندلس",
        labelEn = "Andalusian Olive",
        swatchPrimary = Color(0xFF4D7C0F),
        swatchAccent = Color(0xFFEAC56D),
        darkPrimary = Color(0xFFA3E635),
        darkContainer = Color(0xFF24380C),
        darkGold = Color(0xFFEAC56D),
        darkGoldSoft = Color(0xFFFFE099),
        darkBgTop = Color(0xFF0A1406),
        darkBgMid = Color(0xFF060D04),
        darkBgBottom = Color(0xFF0D1908),
        darkCardTop = Color(0xFF1E3113),
        darkCardMid = Color(0xFF13200B),
        darkCardBottom = Color(0xFF0A1306),
        darkOrbPrimary = Color(0x4D65A30D),
        darkOrbSecondary = Color(0x3CEAC56D),
        darkOrbAccent = Color(0x3D34D399),
        lightPrimary = Color(0xFF3F6212),
        lightBgTop = Color(0xFFEEF5E6),
        lightBgBottom = Color(0xFFF6FAF0)
    );

    companion object {
        fun fromKey(key: String?): NoorThemePreset =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: EMERALD_GOLD
    }
}

/**
 * Real-time visual tuning controlled by Super Admin.
 */
data class NoorVisualSettings(
    val themePreset: String = NoorThemePreset.EMERALD_GOLD.key,
    val glassOpacity: Float = 0.85f,
    val effectsStarlightEnabled: Boolean = true,
    val effectsAuroraEnabled: Boolean = true,
    val effects3DSplashEnabled: Boolean = true,
    val effectsIntensity: Float = 1.0f
)

val LocalNoorVisualSettings = staticCompositionLocalOf { NoorVisualSettings() }

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
    val glassDockBottom: Color = Color(0xEB051512),
    val bgTop: Color = Color(0xFF031411),
    val bgMid: Color = Color(0xFF020C0A),
    val bgBottom: Color = Color(0xFF041311),
    val cardTopBase: Color = Color(0xFF13352D),
    val cardMidBase: Color = Color(0xFF0A211C),
    val cardBottomBase: Color = Color(0xFF061511)
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
    visualSettings: NoorVisualSettings = NoorVisualSettings(),
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val preset = NoorThemePreset.fromKey(visualSettings.themePreset)
    val opacityScale = visualSettings.glassOpacity.coerceIn(0.45f, 1.0f)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = preset.darkPrimary,
            onPrimary = preset.darkBgMid,
            primaryContainer = preset.darkContainer,
            onPrimaryContainer = EmeraldMist,
            secondary = preset.darkGold,
            onSecondary = NightBlack,
            secondaryContainer = GoldContainerDark,
            onSecondaryContainer = preset.darkGoldSoft,
            tertiary = preset.swatchPrimary,
            onTertiary = Color.White,
            background = preset.darkBgMid,
            onBackground = DarkInk,
            surface = preset.darkCardMid,
            onSurface = DarkInk,
            surfaceVariant = preset.darkCardTop,
            onSurfaceVariant = DarkInkMuted,
            outline = preset.darkPrimary.copy(alpha = 0.36f),
            outlineVariant = preset.darkContainer,
            error = SemanticError,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = preset.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = preset.lightBgTop,
            onPrimaryContainer = preset.darkBgMid,
            secondary = GoldMuted,
            onSecondary = Color.White,
            secondaryContainer = GoldContainerLight,
            onSecondaryContainer = Color(0xFF4A3611),
            tertiary = preset.swatchPrimary,
            onTertiary = Color.White,
            background = preset.lightBgBottom,
            onBackground = LightInk,
            surface = LightSurface,
            onSurface = LightInk,
            surfaceVariant = preset.lightBgTop,
            onSurfaceVariant = LightInkMuted,
            outline = preset.lightPrimary.copy(alpha = 0.32f),
            outlineVariant = preset.lightBgTop,
            error = SemanticError,
            onError = Color.White
        )
    }

    val extendedColors = if (isDark) {
        NoorExtendedColors(
            isDark = true,
            glassSurface = preset.darkCardMid.copy(alpha = (0.74f * opacityScale).coerceIn(0.32f, 0.96f)),
            glassSurfaceStrong = preset.darkCardTop.copy(alpha = (0.88f * opacityScale).coerceIn(0.42f, 0.98f)),
            glassBorderTop = Color(0x57FFFFFF),
            glassBorderBottom = preset.darkGold.copy(alpha = 0.32f),
            goldAccent = preset.darkGold,
            goldSoft = preset.darkGoldSoft,
            emeraldGlow = preset.swatchPrimary,
            patternTint = preset.darkGold.copy(alpha = 0.11f),
            ivoryCardBg = IvorySurface,
            ivoryCardInk = IvoryInk,
            glassSpecularTop = LiquidSpecularDark,
            glassOrbPrimary = preset.darkOrbPrimary,
            glassOrbSecondary = preset.darkOrbSecondary,
            glassOrbAccent = preset.darkOrbAccent,
            glassDockTop = preset.darkCardTop.copy(alpha = (0.88f * opacityScale).coerceIn(0.45f, 0.98f)),
            glassDockBottom = preset.darkCardBottom.copy(alpha = (0.94f * opacityScale).coerceIn(0.50f, 0.99f)),
            bgTop = preset.darkBgTop,
            bgMid = preset.darkBgMid,
            bgBottom = preset.darkBgBottom,
            cardTopBase = preset.darkCardTop,
            cardMidBase = preset.darkCardMid,
            cardBottomBase = preset.darkCardBottom
        )
    } else {
        NoorExtendedColors(
            isDark = false,
            glassSurface = Color.White.copy(alpha = (0.82f * opacityScale).coerceIn(0.42f, 0.96f)),
            glassSurfaceStrong = Color.White.copy(alpha = (0.94f * opacityScale).coerceIn(0.52f, 0.99f)),
            glassBorderTop = Color(0xF5FFFFFF),
            glassBorderBottom = preset.lightPrimary.copy(alpha = 0.28f),
            goldAccent = GoldMuted,
            goldSoft = preset.darkGold,
            emeraldGlow = preset.lightBgTop,
            patternTint = preset.lightPrimary.copy(alpha = 0.09f),
            ivoryCardBg = Color(0xFFFDF8EE),
            ivoryCardInk = IvoryInk,
            glassSpecularTop = LiquidSpecularLight,
            glassOrbPrimary = preset.swatchPrimary.copy(alpha = 0.16f),
            glassOrbSecondary = preset.swatchAccent.copy(alpha = 0.18f),
            glassOrbAccent = preset.darkPrimary.copy(alpha = 0.14f),
            glassDockTop = Color.White.copy(alpha = (0.92f * opacityScale).coerceIn(0.50f, 0.98f)),
            glassDockBottom = preset.lightBgTop.copy(alpha = (0.88f * opacityScale).coerceIn(0.48f, 0.96f)),
            bgTop = preset.lightBgTop,
            bgMid = preset.lightBgBottom,
            bgBottom = preset.lightBgTop,
            cardTopBase = Color.White,
            cardMidBase = preset.lightBgBottom,
            cardBottomBase = preset.lightBgTop
        )
    }

    CompositionLocalProvider(
        LocalNoorColors provides extendedColors,
        LocalNoorVisualSettings provides visualSettings
    ) {
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
