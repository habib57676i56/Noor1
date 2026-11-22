package com.example.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldForest
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient atmospheric background with subtle Islamic 8-point geometric Rub el Hizb lattice
 * and gentle emerald/champagne-gold luminescence.
 */
@Composable
fun IslamicGeometricBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val noorColors = LocalNoorColors.current
    val bgColor = MaterialTheme.colorScheme.background
    val patternColor = noorColors.patternTint
    val topGlow = if (noorColors.isDark) Color(0x3D0F6E4E) else Color(0x1E1A9A6F)
    val goldGlow = if (noorColors.isDark) Color(0x1FD9B464) else Color(0x18D9B464)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(topGlow, Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.06f),
                    radius = size.width * 0.95f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(goldGlow, Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.20f),
                    radius = size.width * 0.72f
                )
            )

            val cellSize = 112.dp.toPx()
            val radius = cellSize * 0.27f
            val cols = (size.width / cellSize).toInt() + 2
            val rows = (size.height / cellSize).toInt() + 2

            for (r in 0..rows) {
                for (c in 0..cols) {
                    val cx = c * cellSize + if (r % 2 == 1) cellSize * 0.5f else 0f
                    val cy = r * cellSize * 0.88f
                    drawRubElHizb(cx, cy, radius, patternColor, 0.9.dp.toPx())
                }
            }
        }
        content()
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
                color = gold.copy(alpha = 0.12f),
                radius = r * 0.82f,
                center = Offset(cx, cy)
            )
            drawRubElHizb(cx, cy, r, gold, 1.4.dp.toPx())
            drawCircle(
                color = gold.copy(alpha = 0.45f),
                radius = r * 0.64f,
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx())
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
        drawRubElHizb(cx, cy, r, tint.copy(alpha = 0.85f), 1.2.dp.toPx())
        drawRubElHizb(cx, cy, r * 0.58f, tint.copy(alpha = 0.5f), 0.9.dp.toPx())
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
 * Premium Liquid Glass translucent container with specular gradient border and layered depth.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false,
    goldBorder: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val noorColors = LocalNoorColors.current
    val fillBrush = if (noorColors.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                if (strong) Color(0xEB122C23) else Color(0xCF0F251D),
                if (strong) Color(0xE0081611) else Color(0xBA07130F)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                if (strong) Color(0xFAFFFFFF) else Color(0xEBFFFFFF),
                if (strong) Color(0xF0F3F8F5) else Color(0xDCF0F6F3)
            )
        )
    }

    val borderBrush = if (goldBorder) {
        Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.75f), GoldPrimary.copy(alpha = 0.28f)))
    } else {
        Brush.linearGradient(listOf(noorColors.glassBorderTop, noorColors.glassBorderBottom))
    }

    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickMod)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
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
                brush = Brush.linearGradient(listOf(GoldPrimary.copy(alpha = 0.7f), GoldPrimary.copy(alpha = 0.25f))),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
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
    val bgColor = when {
        selected -> MaterialTheme.colorScheme.primary
        noorColors.isDark -> Color(0x9910261E)
        else -> Color(0xD9FFFFFF)
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        noorColors.goldAccent.copy(alpha = 0.75f)
    } else {
        noorColors.glassBorderBottom
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
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
                    .size(width = 4.dp, height = 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(noorColors.goldAccent)
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
                color = noorColors.goldAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
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
                    .background(LocalNoorColors.current.glassSurface)
                    .border(1.dp, LocalNoorColors.current.glassBorderBottom, CircleShape)
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
