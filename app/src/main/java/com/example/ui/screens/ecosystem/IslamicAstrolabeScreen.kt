package com.example.ui.screens.ecosystem

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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.astronomy.IslamicAstrolabeEngine
import com.example.domain.astronomy.StarLivePosition
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Golden-Age Islamic Astrolabe & Live Fixed Stars Celestial Observatory.
 * Renders a draggable brass-and-emerald Rete (العنكبوت), Tympan almucantar altitude arcs,
 * live Solar position, and 15 authentic Arabic-named stars computed in real time from GPS + LST.
 */
@Composable
fun IslamicAstrolabeScreen(
    userLatitude: Double,
    userLongitude: Double,
    userCityLabel: String,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var manualOffsetHours by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val effectiveMillis = nowMillis + (manualOffsetHours * 3600_000L).toLong()
    val snapshot = remember(effectiveMillis, userLatitude, userLongitude) {
        IslamicAstrolabeEngine.computeSkySnapshot(effectiveMillis, userLatitude, userLongitude)
    }
    var selectedStarId by remember { mutableStateOf(snapshot.stars.firstOrNull()?.star?.id ?: "sirius") }
    val selectedPos: StarLivePosition = snapshot.stars.firstOrNull { it.star.id == selectedStarId }
        ?: snapshot.stars.first()

    val infinite = rememberInfiniteTransition(label = "astrolabe_twinkle")
    val twinklePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "star_twinkle"
    )

    val visibleCount = snapshot.stars.count { it.isVisibleNow }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("islamic_astrolabe_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الأسطرلاب الإسلامي الذهبي الحيّ", "Golden Islamic Astrolabe", "Oltin Islomiy Asturlob"),
            subtitle = L10n.pick(
                lang,
                "سماء $userCityLabel الحقيقية · ١٥ نجماً عربياً أصيلاً بحساب الزمن النجمي",
                "Live sky over $userCityLabel · 15 historic Arabic stars via Local Sidereal Time",
                "$userCityLabel osmoni · 15 ta tarixiy arab yulduzi jonli xaritasi"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Interactive Draggable Golden Brass Astrolabe & Horizon Star Dome
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(335.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF0B2535), Color(0xFF04141D), Color(0xFF02090D))
                            )
                        )
                        .border(2.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(28.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                manualOffsetHours = (manualOffsetHours + dragAmount.x * 0.03f) % 24f
                            }
                        }
                        .testTag("astrolabe_interactive_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerR = size.minDimension * 0.44f

                        // Outer Brass Mater (الأمّ) graduated 24-hour degree rim
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF061E2B), Color(0xFF030E16)),
                                center = Offset(cx, cy),
                                radius = outerR
                            ),
                            radius = outerR,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = GoldBright.copy(alpha = 0.85f),
                            radius = outerR,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3.2.dp.toPx())
                        )
                        drawCircle(
                            color = GoldPrimary.copy(alpha = 0.42f),
                            radius = outerR * 0.90f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Degree tick marks around Mater rim (every 15 degrees = 1 Sidereal Hour)
                        for (deg in 0 until 360 step 15) {
                            val rad = Math.toRadians(deg.toDouble())
                            val innerTick = if (deg % 90 == 0) outerR * 0.84f else outerR * 0.90f
                            drawLine(
                                color = GoldBright.copy(alpha = if (deg % 90 == 0) 0.95f else 0.5f),
                                start = Offset(cx + (innerTick * cos(rad)).toFloat(), cy + (innerTick * sin(rad)).toFloat()),
                                end = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                                strokeWidth = if (deg % 90 == 0) 2.2.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        // Tympan Almucantar altitude circles (30°, 60°, Zenith)
                        val skyR = outerR * 0.84f
                        listOf(1.0f, 0.66f, 0.33f).forEach { frac ->
                            drawCircle(
                                color = EmeraldBright.copy(alpha = 0.24f),
                                radius = skyR * frac,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        // Meridian & Prime Vertical crosshairs
                        drawLine(
                            color = GoldPrimary.copy(alpha = 0.24f),
                            start = Offset(cx, cy - skyR),
                            end = Offset(cx, cy + skyR),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = GoldPrimary.copy(alpha = 0.24f),
                            start = Offset(cx - skyR, cy),
                            end = Offset(cx + skyR, cy),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Rotating Brass Ecliptic Ring (دائرة البروج) linked to LST
                        val eclipticAngle = ((snapshot.localSiderealTimeHours / 24.0) * 360.0).toFloat()
                        rotate(degrees = eclipticAngle, pivot = Offset(cx, cy)) {
                            drawCircle(
                                color = GoldPrimary.copy(alpha = 0.48f),
                                radius = skyR * 0.58f,
                                center = Offset(cx, cy - skyR * 0.16f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Plot Sun on sky dome if above horizon (or dim horizon marker below)
                        val sunFrac = ((90.0 - snapshot.sunAltitudeDeg.coerceIn(-18.0, 90.0)) / 90.0).toFloat().coerceIn(0f, 1.05f)
                        val sunAzRad = Math.toRadians(snapshot.sunAzimuthDeg - 90.0)
                        val sunX = cx + (skyR * sunFrac * cos(sunAzRad)).toFloat()
                        val sunY = cy + (skyR * sunFrac * sin(sunAzRad)).toFloat()
                        val sunAlpha = if (snapshot.sunAltitudeDeg >= 0.0) 0.95f else 0.30f
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFFDE047).copy(alpha = sunAlpha), Color.Transparent),
                                center = Offset(sunX, sunY),
                                radius = 16.dp.toPx()
                            ),
                            radius = 16.dp.toPx(),
                            center = Offset(sunX, sunY)
                        )
                        drawCircle(
                            color = Color(0xFFFEF08A).copy(alpha = sunAlpha),
                            radius = 5.5.dp.toPx(),
                            center = Offset(sunX, sunY)
                        )

                        // Plot all 15 Historic Arabic Fixed Stars (above horizon = bright twinkling gold; below = faint teal)
                        snapshot.stars.forEachIndexed { idx, pos ->
                            val isSelected = pos.star.id == selectedPos.star.id
                            val rFrac = ((90.0 - pos.altitudeDeg.coerceIn(-45.0, 90.0)) / 90.0).toFloat().coerceIn(0.03f, 0.98f)
                            val azRad = Math.toRadians(pos.azimuthDeg - 90.0)
                            val sx = cx + (skyR * rFrac * cos(azRad)).toFloat()
                            val sy = cy + (skyR * rFrac * sin(azRad)).toFloat()

                            val twinkle = (0.65f + 0.35f * sin((twinklePhase + idx * 0.85f).toDouble())).toFloat()
                            val starColor = when {
                                isSelected -> GoldBright
                                pos.isVisibleNow -> Color(0xFFFFF3C4)
                                else -> Color(0xFF38BDF8).copy(alpha = 0.28f)
                            }
                            val baseRadius = ((2.2 - pos.star.magnitude.coerceIn(-1.5, 2.0)) * 1.4f + 2.2f).toFloat().dp.toPx()

                            if (pos.isVisibleNow || isSelected) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(starColor.copy(alpha = 0.72f * twinkle), Color.Transparent),
                                        center = Offset(sx, sy),
                                        radius = baseRadius * (if (isSelected) 3.8f else 2.4f)
                                    ),
                                    radius = baseRadius * (if (isSelected) 3.8f else 2.4f),
                                    center = Offset(sx, sy)
                                )
                            }
                            drawCircle(
                                color = starColor,
                                radius = if (isSelected) baseRadius * 1.3f else baseRadius * 0.78f,
                                center = Offset(sx, sy)
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = GoldBright,
                                    radius = baseRadius * 2.4f,
                                    center = Offset(sx, sy),
                                    style = Stroke(width = 1.6.dp.toPx())
                                )
                                // Brass Alidade pointer line from center to selected star
                                drawLine(
                                    color = GoldBright.copy(alpha = 0.65f),
                                    start = Offset(cx, cy),
                                    end = Offset(sx, sy),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }

                        // Central Astrolabe Axis Pin (القطب)
                        drawCircle(color = GoldBright, radius = 4.5.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(color = EmeraldDeep, radius = 2.dp.toPx(), center = Offset(cx, cy))
                    }

                    // Cardinal labels (N / S / E / W)
                    Text("N · ش", color = GoldBright, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
                    Text("S · ج", color = GoldPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
                    Text("W · غ", color = GoldPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                    Text("E · ق", color = GoldPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
                }
            }

            // 2. Live Sidereal Telemetry & Time-Travel Reset Controls
            item {
                LiquidGlassCard(strong = true, goldBorder = true, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, snapshot.skyPhaseAr, snapshot.skyPhaseEn, snapshot.skyPhaseUz),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldBright
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "الزمن النجمي: ${IslamicAstrolabeEngine.formatSiderealTime(snapshot.localSiderealTimeHours)} · فوق الأفق: $visibleCount / ١٥ نجماً",
                                    "LST: ${IslamicAstrolabeEngine.formatSiderealTime(snapshot.localSiderealTimeHours)} · Visible: $visibleCount / 15 stars",
                                    "Yulduz vaqti: ${IslamicAstrolabeEngine.formatSiderealTime(snapshot.localSiderealTimeHours)} · Ko'rinmoqda: $visibleCount / 15"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "ارتفاع الشمس: ${snapshot.sunAltitudeDeg.roundToInt()}° · اسحب الأسطرلاب لتدوير السماء عبر الساعات",
                                    "Sun Alt: ${snapshot.sunAltitudeDeg.roundToInt()}° · Drag astrolabe to spin through hours",
                                    "Quyosh balandligi: ${snapshot.sunAltitudeDeg.roundToInt()}° · Vaqtni burish uchun aylantiring"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldBright
                            )
                        }
                        if (manualOffsetHours != 0f) {
                            OutlinedButton(onClick = { manualOffsetHours = 0f }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(L10n.pick(lang, "الآن", "Live"))
                            }
                        }
                    }
                }
            }

            // 3. Selected Star Deep Heritage & Live Horizon Telemetry Card
            item {
                LiquidGlassCard(goldBorder = true, contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = L10n.pick(lang, selectedPos.star.nameAr, selectedPos.star.nameEn, selectedPos.star.nameUz),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldBright
                                )
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "كوكبة ${selectedPos.star.constellationAr} · القدر الظاهري ${selectedPos.star.magnitude}",
                                        "Constellation: ${selectedPos.star.constellationEn} · Mag ${selectedPos.star.magnitude}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selectedPos.isVisibleNow) EmeraldBright.copy(alpha = 0.22f)
                                    else Color(0xFF64748B).copy(alpha = 0.25f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (selectedPos.isVisibleNow) L10n.pick(lang, "فوق الأفق", "Above Horizon")
                                else L10n.pick(lang, "تحت الأفق", "Below Horizon"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPos.isVisibleNow) EmeraldBright else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryChip(
                            icon = Icons.Rounded.Visibility,
                            label = L10n.pick(lang, "الارتفاع: ${selectedPos.altitudeDeg.roundToInt()}°", "Alt: ${selectedPos.altitudeDeg.roundToInt()}°"),
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryChip(
                            icon = Icons.Rounded.Explore,
                            label = L10n.pick(lang, "السمت: ${selectedPos.azimuthDeg.roundToInt()}°", "Az: ${selectedPos.azimuthDeg.roundToInt()}°"),
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryChip(
                            icon = Icons.Rounded.WbSunny,
                            label = "RA ${selectedPos.star.raHours.roundToInt()}h",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = L10n.pick(lang, selectedPos.star.heritageNoteAr, selectedPos.star.heritageNoteEn, selectedPos.star.heritageNoteUz),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 4. All 15 Historic Arabic Fixed Stars Catalog (Sorted Live by Altitude)
            items(snapshot.stars, key = { it.star.id }) { pos ->
                val isSelected = pos.star.id == selectedPos.star.id
                LiquidGlassCard(
                    goldBorder = isSelected,
                    onClick = { selectedStarId = pos.star.id },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, pos.star.nameAr, pos.star.nameEn, pos.star.nameUz),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) GoldBright else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = L10n.pick(lang, pos.star.constellationAr, pos.star.constellationEn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${pos.altitudeDeg.roundToInt()}° / ${pos.azimuthDeg.roundToInt()}°",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pos.isVisibleNow) EmeraldBright else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (pos.isVisibleNow) L10n.pick(lang, "مرئي الآن", "Visible") else L10n.pick(lang, "غارب", "Set"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pos.isVisibleNow) GoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GoldPrimary.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = GoldBright, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldBright)
    }
}
