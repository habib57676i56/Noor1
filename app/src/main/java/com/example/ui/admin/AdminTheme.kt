package com.example.ui.admin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ultra-Luxury Liquid Glass Enterprise Design System for the NOOR Super Admin Control Center.
 * Layered translucent obsidian-emerald-cyan glass surfaces with iridescent specular sheens,
 * living drifting aurora light orbs, and luminous telemetry accents.
 */
object AdminPalette {
    val BgCanvas = Color(0xFF040914)
    val SurfaceCard = Color(0xA60E1B2E)
    val SurfaceElevated = Color(0xB8142640)
    val BorderSubtle = Color(0x4768C5F8)
    val BorderAccent = Color(0xFF38BDF8)

    val GlassTop = Color(0xC2172C47)
    val GlassMid = Color(0xAD0D1A2E)
    val GlassBottom = Color(0xBD081120)
    val GlassSpecular = Color(0x30FFFFFF)
    val GoldLuxe = Color(0xFFF3CF7A)

    val TextPrimary = Color(0xFFF4F8FF)
    val TextSecondary = Color(0xFFA7BCD6)
    val TextMuted = Color(0xFF778FA9)

    val AccentCyan = Color(0xFF38BDF8)
    val AccentIndigo = Color(0xFFA5B4FC)
    val AccentEmerald = Color(0xFF34D399)
    val AccentAmber = Color(0xFFFBBF24)
    val AccentRose = Color(0xFFFB7185)
}

private val AdminDarkColorScheme = darkColorScheme(
    primary = AdminPalette.AccentCyan,
    onPrimary = Color(0xFF031522),
    secondary = AdminPalette.GoldLuxe,
    onSecondary = Color(0xFF171204),
    tertiary = AdminPalette.AccentEmerald,
    background = AdminPalette.BgCanvas,
    onBackground = AdminPalette.TextPrimary,
    surface = Color(0xFF0D1829),
    onSurface = AdminPalette.TextPrimary,
    surfaceVariant = Color(0xFF13243B),
    onSurfaceVariant = AdminPalette.TextSecondary,
    outline = AdminPalette.BorderSubtle,
    error = AdminPalette.AccentRose
)

@Composable
fun NoorAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AdminDarkColorScheme,
        content = content
    )
}

/**
 * Living Liquid Glass Aurora Canvas for the entire Super Admin Control Center.
 */
@Composable
fun AdminLiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "admin_liquid_aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "admin_aurora_phase"
    )
    val moteProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "admin_starlight_motes"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050D1D),
                        Color(0xFF030812),
                        Color(0xFF061523),
                        Color(0xFF030710)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dx = sin(phase.toDouble()).toFloat() * size.width * 0.08f
            val dy = cos(phase.toDouble()).toFloat() * size.height * 0.05f

            // Celestial Cyan Liquid Orb (Top-Left)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4A38BDF8), Color.Transparent),
                    center = Offset(size.width * 0.16f + dx, size.height * 0.08f + dy),
                    radius = size.width * 0.92f
                )
            )
            // Imperial Gold Champagne Orb (Top-Right)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x38F3CF7A), Color.Transparent),
                    center = Offset(size.width * 0.88f - dx, size.height * 0.18f - dy),
                    radius = size.width * 0.78f
                )
            )
            // Emerald Jade Prism Glow (Mid-Left)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3B10B981), Color.Transparent),
                    center = Offset(size.width * 0.12f - dx * 0.6f, size.height * 0.58f + dy),
                    radius = size.width * 0.80f
                )
            )
            // Deep Indigo Luminescence (Bottom-Right)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3D6366F1), Color.Transparent),
                    center = Offset(size.width * 0.85f + dx * 0.7f, size.height * 0.88f - dy * 0.5f),
                    radius = size.width * 0.85f
                )
            )

            // Delicate luminous starlight motes floating behind glass cards
            for (i in 0 until 18) {
                val sx = ((i * 67 + 13) % 97) / 97f
                val sy = ((i * 43 + 7) % 89) / 89f
                val spd = 0.55f + (i % 4) * 0.2f
                val yNorm = ((sy - moteProgress * spd) % 1f + 1f) % 1f
                val mx = sx * size.width + sin((phase + i).toDouble()).toFloat() * 10.dp.toPx()
                val my = yNorm * size.height
                val twinkle = (0.35f + 0.65f * ((sin((phase * 2.2f + i * 1.5f).toDouble()) + 1.0) / 2.0)).toFloat()
                val color = when (i % 3) {
                    0 -> AdminPalette.AccentCyan
                    1 -> AdminPalette.GoldLuxe
                    else -> AdminPalette.AccentEmerald
                }
                val r = (1.3f + (i % 3) * 0.9f).dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.38f * twinkle), Color.Transparent),
                        center = Offset(mx, my),
                        radius = r * 3.6f
                    ),
                    radius = r * 3.6f,
                    center = Offset(mx, my)
                )
                drawCircle(
                    color = color.copy(alpha = 0.75f * twinkle),
                    radius = r * 0.7f,
                    center = Offset(mx, my)
                )
            }
        }
        content()
    }
}

/**
 * Translucent Liquid Glass panel card with diagonal depth tint, curved top specular
 * reflection sheen, and iridescent multi-stop border.
 */
@Composable
fun AdminPanelCard(
    modifier: Modifier = Modifier,
    accentBorder: Color = AdminPalette.BorderSubtle,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val fillBrush = Brush.linearGradient(
        colors = listOf(
            AdminPalette.GlassTop,
            AdminPalette.GlassMid,
            AdminPalette.GlassBottom
        )
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.38f),
            accentBorder.copy(alpha = 0.78f),
            AdminPalette.GoldLuxe.copy(alpha = 0.24f),
            accentBorder.copy(alpha = 0.42f)
        )
    )
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(fillBrush)
        .drawWithContent {
            // Top liquid optical specular highlight
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(AdminPalette.GlassSpecular, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.34f
                )
            )
            drawContent()
        }
        .border(1.1.dp, borderBrush, shape)
    val finalMod = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(
        modifier = finalMod.padding(contentPadding),
        content = content
    )
}

@Composable
fun AdminStatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0.11f))
                )
            )
            .border(1.dp, color.copy(alpha = 0.58f), shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AdminSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AdminPalette.TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AdminPalette.TextSecondary
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun AdminKpiTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    sublabel: String? = null
) {
    AdminPanelCard(
        modifier = modifier,
        accentBorder = accent.copy(alpha = 0.55f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AdminPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = AdminPalette.TextPrimary
        )
        if (!sublabel.isNullOrBlank()) {
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdminFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            colors = listOf(
                AdminPalette.AccentCyan.copy(alpha = 0.30f),
                AdminPalette.AccentEmerald.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0x88162945),
                Color(0x700D1A2E)
            )
        )
    }
    val borderColor = if (selected) AdminPalette.AccentCyan.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f)
    val fg = if (selected) Color.White else AdminPalette.TextSecondary
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1
        )
    }
}
