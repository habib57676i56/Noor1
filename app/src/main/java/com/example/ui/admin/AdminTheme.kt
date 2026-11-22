package com.example.ui.admin

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Completely separate Enterprise Administration Design System for the NOOR 3.0
 * Super Admin Control Center. Deliberately distinct from the user-facing Emerald/Gold
 * Liquid Glass aesthetic — built with crisp obsidian slate, architectural borders,
 * high-contrast cyan/teal telemetry accents, and clear status semantics.
 */
object AdminPalette {
    val BgCanvas = Color(0xFF0A0F1A)
    val SurfaceCard = Color(0xFF111827)
    val SurfaceElevated = Color(0xFF1A2438)
    val BorderSubtle = Color(0xFF24324B)
    val BorderAccent = Color(0xFF38BDF8)

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    val AccentCyan = Color(0xFF38BDF8)
    val AccentIndigo = Color(0xFF818CF8)
    val AccentEmerald = Color(0xFF10B981)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentRose = Color(0xFFF43F5E)
}

private val AdminDarkColorScheme = darkColorScheme(
    primary = AdminPalette.AccentCyan,
    onPrimary = Color(0xFF031522),
    secondary = AdminPalette.AccentIndigo,
    onSecondary = Color(0xFF0B1024),
    tertiary = AdminPalette.AccentEmerald,
    background = AdminPalette.BgCanvas,
    onBackground = AdminPalette.TextPrimary,
    surface = AdminPalette.SurfaceCard,
    onSurface = AdminPalette.TextPrimary,
    surfaceVariant = AdminPalette.SurfaceElevated,
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

@Composable
fun AdminPanelCard(
    modifier: Modifier = Modifier,
    accentBorder: Color = AdminPalette.BorderSubtle,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(AdminPalette.SurfaceCard)
        .border(1.dp, accentBorder, shape)
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
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
                fontWeight = FontWeight.Bold,
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
        accentBorder = accent.copy(alpha = 0.35f),
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
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
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
    val bg = if (selected) AdminPalette.AccentCyan.copy(alpha = 0.18f) else AdminPalette.SurfaceElevated
    val border = if (selected) AdminPalette.AccentCyan else AdminPalette.BorderSubtle
    val fg = if (selected) AdminPalette.AccentCyan else AdminPalette.TextSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
