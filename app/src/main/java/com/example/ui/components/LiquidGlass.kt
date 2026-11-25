package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brightness5
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.PrayerName
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LiquidAuroraJade
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.LocalNoorVisualSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen Liquid Glass luminous atmospheric canvas.
 * Renders multi-layered iridescent emerald, cyan-prism, and warm champagne-gold
 * liquid aurora orbs beneath a delicate etched 8-point Rub el Hizb Islamic filigree.
 */
@Composable
fun IslamicGeometricBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val noorColors = LocalNoorColors.current
    val visualSettings = LocalNoorVisualSettings.current
    val intensity = visualSettings.effectsIntensity.coerceIn(0.2f, 1.5f)
    val baseGradient = Brush.verticalGradient(
        colors = listOf(
            noorColors.bgTop,
            noorColors.bgMid,
            noorColors.bgBottom,
            noorColors.bgMid
        )
    )
    val patternColor = noorColors.patternTint
    val orbPrimary = noorColors.glassOrbPrimary.copy(alpha = (noorColors.glassOrbPrimary.alpha * intensity).coerceAtMost(0.85f))
    val orbSecondary = noorColors.glassOrbSecondary.copy(alpha = (noorColors.glassOrbSecondary.alpha * intensity).coerceAtMost(0.85f))
    val orbAccent = noorColors.glassOrbAccent.copy(alpha = (noorColors.glassOrbAccent.alpha * intensity).coerceAtMost(0.85f))
    val lowerJade = (if (noorColors.isDark) noorColors.emeraldGlow.copy(alpha = 0.24f) else noorColors.emeraldGlow.copy(alpha = 0.14f))
        .let { it.copy(alpha = (it.alpha * intensity).coerceAtMost(0.65f)) }

    val ambientTransition = rememberInfiniteTransition(label = "noor_living_atmosphere")
    val auroraPhase by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora_drift_phase"
    )
    val moteProgress by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "starlight_motes"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseGradient)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val driftX = if (visualSettings.effectsAuroraEnabled) sin(auroraPhase.toDouble()).toFloat() * size.width * 0.06f else 0f
            val driftY = if (visualSettings.effectsAuroraEnabled) cos(auroraPhase.toDouble()).toFloat() * size.height * 0.04f else 0f

            if (visualSettings.effectsAuroraEnabled) {
                // Top-left radiant liquid orb (living drift)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(orbPrimary, Color.Transparent),
                        center = Offset(size.width * 0.18f + driftX, size.height * 0.08f + driftY),
                        radius = size.width * 0.94f
                    )
                )
                // Top-right warm champagne-gold refraction orb
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(orbSecondary, Color.Transparent),
                        center = Offset(size.width * 0.88f - driftX, size.height * 0.16f - driftY * 0.8f),
                        radius = size.width * 0.80f
                    )
                )
                // Mid-left liquid prism glow for frosted glass depth
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(orbAccent, Color.Transparent),
                        center = Offset(size.width * 0.10f - driftX * 0.7f, size.height * 0.56f + driftY),
                        radius = size.width * 0.78f
                    )
                )
                // Bottom-right luminous pool under the floating dock
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(lowerJade, Color.Transparent),
                        center = Offset(size.width * 0.82f + driftX * 0.8f, size.height * 0.88f - driftY * 0.6f),
                        radius = size.width * 0.88f
                    )
                )
            }

            // Fine crystal-etched 8-point Rub el Hizb geometric lattice
            val cellSize = 114.dp.toPx()
            val radius = cellSize * 0.26f
            val cols = (size.width / cellSize).toInt() + 2
            val rows = (size.height / cellSize).toInt() + 2

            for (r in 0..rows) {
                for (c in 0..cols) {
                    val cx = c * cellSize + if (r % 2 == 1) cellSize * 0.5f else 0f
                    val cy = r * cellSize * 0.88f
                    drawRubElHizb(cx, cy, radius, patternColor, 0.85.dp.toPx())
                }
            }

            // Floating Golden Noor Starlight Motes (controlled by Super Admin live effects)
            if (visualSettings.effectsStarlightEnabled) {
                val moteCount = (20 * intensity).toInt().coerceIn(8, 30)
                val moteGold = noorColors.goldSoft
                val moteJade = noorColors.emeraldGlow
                for (i in 0 until moteCount) {
                    val seedX = ((i * 73 + 19) % 97) / 97f
                    val seedY = ((i * 41 + 11) % 89) / 89f
                    val speed = 0.65f + (i % 4) * 0.22f
                    val yNorm = ((seedY - moteProgress * speed) % 1f + 1f) % 1f
                    val sway = sin((auroraPhase * 1.4f + i * 0.9f).toDouble()).toFloat() * 14.dp.toPx()
                    val mx = seedX * size.width + sway
                    val my = yNorm * size.height
                    val twinkle = (0.35f + 0.65f * ((sin((auroraPhase * 2.5f + i * 1.7f).toDouble()) + 1.0) / 2.0)).toFloat()
                    val baseAlpha = ((if (noorColors.isDark) 0.42f else 0.24f) * twinkle * intensity).coerceAtMost(0.85f)
                    val moteColor = if (i % 3 == 0) moteJade else moteGold
                    val r = (1.5f + (i % 3) * 1.1f).dp.toPx()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(moteColor.copy(alpha = baseAlpha), Color.Transparent),
                            center = Offset(mx, my),
                            radius = r * 3.4f
                        ),
                        radius = r * 3.4f,
                        center = Offset(mx, my)
                    )
                    drawCircle(
                        color = moteColor.copy(alpha = (baseAlpha * 1.45f).coerceAtMost(0.90f)),
                        radius = r * 0.72f,
                        center = Offset(mx, my)
                    )
                    if (i % 4 == 0 && twinkle > 0.72f) {
                        val rayLen = r * 3.0f * twinkle
                        drawLine(
                            color = moteColor.copy(alpha = baseAlpha * 0.85f),
                            start = Offset(mx - rayLen, my),
                            end = Offset(mx + rayLen, my),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        drawLine(
                            color = moteColor.copy(alpha = baseAlpha * 0.85f),
                            start = Offset(mx, my - rayLen),
                            end = Offset(mx, my + rayLen),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}

fun DrawScope.drawRubElHizb(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    strokeWidthPx: Float = 1.dp.toPx()
) {
    val stroke = Stroke(width = strokeWidthPx)
    val path1 = Path()
    val path2 = Path()
    for (i in 0 until 4) {
        val a1 = (i * 90.0) * PI / 180.0
        val x1 = cx + (radius * cos(a1)).toFloat()
        val y1 = cy + (radius * sin(a1)).toFloat()
        if (i == 0) path1.moveTo(x1, y1) else path1.lineTo(x1, y1)

        val a2 = (i * 90.0 + 45.0) * PI / 180.0
        val x2 = cx + (radius * cos(a2)).toFloat()
        val y2 = cy + (radius * sin(a2)).toFloat()
        if (i == 0) path2.moveTo(x2, y2) else path2.lineTo(x2, y2)
    }
    path1.close()
    path2.close()
    drawPath(path1, color = color, style = stroke)
    drawPath(path2, color = color, style = stroke)
}

/**
 * Golden 8-point Islamic star rosette badge for Surah numbers & Verse markers.
 */
@Composable
fun OrnamentalSurahBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val gold = LocalNoorColors.current.goldAccent
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.minDimension * 0.45f
            drawCircle(
                color = gold.copy(alpha = 0.14f),
                radius = r * 0.82f,
                center = Offset(cx, cy)
            )
            drawRubElHizb(cx, cy, r, gold, 1.4.dp.toPx())
            drawCircle(
                color = gold.copy(alpha = 0.50f),
                radius = r * 0.64f,
                center = Offset(cx, cy),
                style = Stroke(width = 0.85.dp.toPx())
            )
        }
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = gold
        )
    }
}

/**
 * Delicate gold Islamic medallion ornament flanking Ayah of the Day or banners.
 */
@Composable
fun IslamicRosetteOrnament(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = LocalNoorColors.current.goldAccent
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.44f
        drawRubElHizb(cx, cy, r, tint.copy(alpha = 0.88f), 1.2.dp.toPx())
        drawRubElHizb(cx, cy, r * 0.58f, tint.copy(alpha = 0.52f), 0.9.dp.toPx())
        drawCircle(color = tint, radius = r * 0.16f, center = Offset(cx, cy))
    }
}

/**
 * Elegant golden mosque skyline silhouette with domes & minarets for Hero / Tasbeeh / Audio banners.
 */
@Composable
fun MosqueHorizonSilhouette(
    modifier: Modifier = Modifier,
    tint: Color = GoldPrimary.copy(alpha = 0.28f)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val base = h

        // Center grand dome
        val domeR = w * 0.13f
        val cx = w * 0.5f
        val domePath = Path().apply {
            moveTo(cx - domeR, base)
            lineTo(cx - domeR, base - h * 0.36f)
            cubicTo(
                cx - domeR, base - h * 0.72f,
                cx - domeR * 0.25f, base - h * 0.85f,
                cx, base - h * 0.94f
            )
            cubicTo(
                cx + domeR * 0.25f, base - h * 0.85f,
                cx + domeR, base - h * 0.72f,
                cx + domeR, base - h * 0.36f
            )
            lineTo(cx + domeR, base)
            close()
        }
        drawPath(domePath, color = tint)

        // Side domes
        listOf(0.28f, 0.72f).forEach { frac ->
            val sx = w * frac
            val sr = w * 0.08f
            val sidePath = Path().apply {
                moveTo(sx - sr, base)
                lineTo(sx - sr, base - h * 0.28f)
                cubicTo(sx - sr, base - h * 0.55f, sx, base - h * 0.66f, sx, base - h * 0.68f)
                cubicTo(sx, base - h * 0.66f, sx + sr, base - h * 0.55f, sx + sr, base - h * 0.28f)
                lineTo(sx + sr, base)
                close()
            }
            drawPath(sidePath, color = tint)
        }

        // Flanking slender minarets
        listOf(0.14f, 0.86f).forEach { frac ->
            val mx = w * frac
            val mw = w * 0.022f
            drawRect(
                color = tint,
                topLeft = Offset(mx - mw / 2f, base - h * 0.78f),
                size = Size(mw, h * 0.78f)
            )
            val spire = Path().apply {
                moveTo(mx - mw * 0.8f, base - h * 0.76f)
                lineTo(mx, base - h * 0.96f)
                lineTo(mx + mw * 0.8f, base - h * 0.76f)
                close()
            }
            drawPath(spire, color = tint)
        }

        // Connecting arcade base
        drawRect(
            color = tint,
            topLeft = Offset(0f, base - h * 0.22f),
            size = Size(w, h * 0.22f)
        )
    }
}

/**
 * Next-generation Liquid Glass translucent container with:
 * - Diagonal frosted glass depth gradient
 * - Curved top-edge specular refraction sheen (liquid optical glare)
 * - Iridescent luminous multi-stop border (specular white/gold -> prism teal -> soft rim)
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    strong: Boolean = false,
    goldBorder: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val noorColors = LocalNoorColors.current
    val visualSettings = LocalNoorVisualSettings.current
    val opacityScale = visualSettings.glassOpacity.coerceIn(0.45f, 1.0f)
    val fillBrush = if (noorColors.isDark) {
        val topA = ((if (strong) 0.87f else 0.74f) * opacityScale).coerceIn(0.28f, 0.96f)
        val midA = ((if (strong) 0.83f else 0.68f) * opacityScale).coerceIn(0.24f, 0.94f)
        val botA = ((if (strong) 0.86f else 0.72f) * opacityScale).coerceIn(0.26f, 0.96f)
        Brush.linearGradient(
            colors = listOf(
                noorColors.cardTopBase.copy(alpha = topA),
                noorColors.cardMidBase.copy(alpha = midA),
                noorColors.cardBottomBase.copy(alpha = botA)
            )
        )
    } else {
        val topA = ((if (strong) 0.96f else 0.88f) * opacityScale).coerceIn(0.40f, 0.98f)
        val midA = ((if (strong) 0.92f else 0.80f) * opacityScale).coerceIn(0.36f, 0.96f)
        val botA = ((if (strong) 0.90f else 0.76f) * opacityScale).coerceIn(0.34f, 0.95f)
        Brush.linearGradient(
            colors = listOf(
                noorColors.cardTopBase.copy(alpha = topA),
                noorColors.cardMidBase.copy(alpha = midA),
                noorColors.cardBottomBase.copy(alpha = botA)
            )
        )
    }

    val specularTop = noorColors.glassSpecularTop
    val innerGlowColor = if (goldBorder) {
        GoldBright.copy(alpha = if (noorColors.isDark) 0.16f else 0.24f)
    } else if (noorColors.isDark) {
        EmeraldSoft.copy(alpha = 0.09f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }

    val borderBrush = if (goldBorder) {
        Brush.linearGradient(
            colors = listOf(
                GoldBright.copy(alpha = 0.88f),
                Color.White.copy(alpha = if (noorColors.isDark) 0.42f else 0.90f),
                GoldPrimary.copy(alpha = 0.52f),
                EmeraldSoft.copy(alpha = if (noorColors.isDark) 0.28f else 0.22f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                noorColors.glassBorderTop,
                if (noorColors.isDark) EmeraldSoft.copy(alpha = 0.22f) else Color(0x99FFFFFF),
                noorColors.glassBorderBottom
            )
        )
    }

    val elevationMod = if (noorColors.isDark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = if (strong) 10.dp else 6.dp,
            shape = shape,
            ambientColor = Color(0x1F0E7C5D),
            spotColor = Color(0x26083024),
            clip = false
        )
    }

    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .then(elevationMod)
            .clip(shape)
            .background(fillBrush)
            .drawWithContent {
                // Top curved liquid-glass specular refraction sheen
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(specularTop, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.34f
                    )
                )
                // Top-left luminous prism droplet highlight
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(innerGlowColor, Color.Transparent),
                        center = Offset(size.width * 0.14f, 0f),
                        radius = size.width * 0.68f
                    )
                )
                drawContent()
            }
            .border(width = if (goldBorder) 1.25.dp else 1.1.dp, brush = borderBrush, shape = shape)
            .then(clickMod)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Warm ivory parchment card for luxury reading experiences (Adhkar focus mode & Mushaf preview).
 */
@Composable
fun WarmIvoryParchmentCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val noorColors = LocalNoorColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(noorColors.ivoryCardBg, Color(0xFFF0E4CC))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(listOf(GoldPrimary.copy(alpha = 0.78f), GoldPrimary.copy(alpha = 0.28f))),
                shape = shape
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides IvoryInk) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun LiquidGlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val noorColors = LocalNoorColors.current
    val chipShape = RoundedCornerShape(50)
    val bgBrush = when {
        selected -> Brush.horizontalGradient(
            colors = listOf(
                if (noorColors.isDark) EmeraldBright else EmeraldPrimary,
                if (noorColors.isDark) EmeraldSoft else EmeraldBright
            )
        )
        noorColors.isDark -> Brush.verticalGradient(
            colors = listOf(Color(0xAD14362D), Color(0x940A201B))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xECFFFFFF), Color(0xD4E8F5F0))
        )
    }
    val contentColor = when {
        selected -> if (noorColors.isDark) Color(0xFF031914) else Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderBrush = if (selected) {
        Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.9f), GoldPrimary.copy(alpha = 0.55f)))
    } else {
        Brush.linearGradient(listOf(noorColors.glassBorderTop, noorColors.glassBorderBottom))
    }

    Box(
        modifier = modifier
            .clip(chipShape)
            .background(bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = chipShape)
            .clickable(onClick = onClick)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NoorSectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val noorColors = LocalNoorColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(GoldBright, noorColors.goldAccent, EmeraldSoft)
                        )
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = noorColors.goldAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(noorColors.glassSurface.copy(alpha = 0.72f))
                    .border(0.9.dp, noorColors.glassBorderBottom, RoundedCornerShape(50))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun NoorScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val noorColors = LocalNoorColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(noorColors.glassSurfaceStrong, noorColors.glassSurface)
                        )
                    )
                    .border(
                        1.1.dp,
                        Brush.linearGradient(listOf(noorColors.glassBorderTop, noorColors.glassBorderBottom)),
                        CircleShape
                    )
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

fun prayerIcon(prayer: PrayerName): ImageVector = when (prayer) {
    PrayerName.FAJR -> Icons.Rounded.WbTwilight
    PrayerName.SUNRISE -> Icons.Rounded.Brightness5
    PrayerName.DHUHR -> Icons.Rounded.WbSunny
    PrayerName.ASR -> Icons.Rounded.Brightness6
    PrayerName.MAGHRIB -> Icons.Rounded.NightsStay
    PrayerName.ISHA -> Icons.Rounded.Bedtime
}
