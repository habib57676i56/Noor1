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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.seerah.HijrahWaypoint
import com.example.domain.seerah.SeerahAtlasEngine
import com.example.domain.seerah.SeerahEra
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import java.util.Locale

/**
 * Rare Flagship Feature: Interactive Prophetic Seerah Chronicle, GPS Hijrah Route Atlas & Shama'il Companion.
 * 1. Interactive Canvas Map of the 7-Stop Blessed Hijrah Route (Makkah -> Cave Thawr -> Quba -> Madinah) with real km distance from user.
 * 2. Filterable Prophetic Seerah Chronological Timeline with lessons & coordinates.
 * 3. Authentic Shama'il Muhammadiyyah (Imam al-Tirmidhi) + Persistent Daily Sunnah Revival Checklist.
 */
@Composable
fun SeerahChronicleAtlasScreen(
    userLatitude: Double,
    userLongitude: Double,
    userCityLabel: String,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Hijrah Atlas, 1 = Timeline, 2 = Shama'il & Sunnah
    var selectedEra by remember { mutableStateOf(SeerahEra.ALL) }
    var selectedStopIndex by remember { mutableIntStateOf(0) }
    var completedSunnahs by remember { mutableStateOf(SeerahAtlasEngine.loadCompletedSunnahs(context)) }

    val waypoints = SeerahAtlasEngine.hijrahWaypoints
    val activeStop = waypoints[selectedStopIndex.coerceIn(0, waypoints.lastIndex)]
    val totalHijrahKm = remember { SeerahAtlasEngine.totalHijrahRouteKm() }
    val distanceToStopKm = remember(userLatitude, userLongitude, activeStop) {
        SeerahAtlasEngine.distanceKm(userLatitude, userLongitude, activeStop.latitude, activeStop.longitude)
    }

    val filteredEvents = remember(selectedEra) {
        if (selectedEra == SeerahEra.ALL) SeerahAtlasEngine.chronicleEvents
        else SeerahAtlasEngine.chronicleEvents.filter { it.era == selectedEra }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("seerah_atlas_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "أطلس السيرة النبوية والشمائل", "Seerah Atlas & Shama'il", "Siyrat atlasi va Shamoil"),
            subtitle = L10n.pick(
                lang,
                "خريطة طريق الهجرة التفاعلية · الخط الزمني · الشمائل وإحياء السنن",
                "Interactive Hijrah GPS Map · Chronological Seerah · Shama'il & Sunnah Revival",
                "Hijrat xaritasi · Siyrat xronologiyasi · Shamoili Muhammadiya va sunnatlar"
            ),
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassChip(
                label = L10n.pick(lang, "خريطة الهجرة", "Hijrah Map", "Hijrat xaritasi"),
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f)
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "السيرة الزمنية", "Timeline", "Siyrat"),
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f)
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "الشمائل والسنن", "Shama'il", "Shamoil"),
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                0 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Explore, contentDescription = null, tint = GoldBright)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = L10n.pick(lang, "مسار الهجرة النبوية المباركة", "Blessed Hijrah Route Map", "Muborak Hijrat yo'li"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = noorColors.goldAccent
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "~%.0f km", totalHijrahKm),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldBright
                                    )
                                }

                                // Custom Animated Canvas Hijrah Route Map
                                HijrahRouteCanvasMap(
                                    waypoints = waypoints,
                                    selectedIndex = selectedStopIndex
                                )

                                // Waypoint selector pills
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(waypoints, key = { it.stepNumber }) { wp ->
                                        val idx = wp.stepNumber - 1
                                        LiquidGlassChip(
                                            label = "${wp.stepNumber}. ${L10n.pick(lang, wp.nameAr.substringBefore(" ("), wp.nameEn.substringBefore(" ("), wp.nameUz.substringBefore(" ("))}",
                                            selected = selectedStopIndex == idx,
                                            onClick = { selectedStopIndex = idx }
                                        )
                                    }
                                }

                                // Active Waypoint Detail Card
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(EmeraldDeep.copy(alpha = 0.50f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${activeStop.stepNumber}. ${L10n.pick(lang, activeStop.nameAr, activeStop.nameEn, activeStop.nameUz)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldBright
                                    )
                                    Text(
                                        text = L10n.pick(lang, activeStop.noteAr, activeStop.noteEn),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = L10n.pick(
                                                lang,
                                                "يبعد عن موقعك ($userCityLabel): ${String.format(Locale.US, "%,.0f", distanceToStopKm)} كم",
                                                "Distance from $userCityLabel: ${String.format(Locale.US, "%,.0f", distanceToStopKm)} km",
                                                "$userCityLabel dan masofa: ${String.format(Locale.US, "%,.0f", distanceToStopKm)} km"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldBright.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SeerahEra.entries) { era ->
                                LiquidGlassChip(
                                    label = L10n.pick(lang, era.labelAr, era.labelEn, era.labelUz),
                                    selected = selectedEra == era,
                                    onClick = { selectedEra = era }
                                )
                            }
                        }
                    }

                    items(filteredEvents, key = { it.id }) { event ->
                        val distKm = remember(userLatitude, userLongitude, event.id) {
                            SeerahAtlasEngine.distanceKm(userLatitude, userLongitude, event.latitude, event.longitude)
                        }
                        LiquidGlassCard(goldBorder = event.era == SeerahEra.HIJRAH) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(GoldPrimary.copy(alpha = 0.22f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = L10n.pick(lang, event.yearLabelAr, event.yearLabelEn),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldBright
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${L10n.pick(lang, event.locationAr, event.locationEn)} (${String.format(Locale.US, "%,.0f", distKm)} km)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = L10n.pick(lang, event.titleAr, event.titleEn, event.titleUz),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = L10n.pick(lang, event.summaryAr, event.summaryEn, event.summaryUz),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.14f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Timeline, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = L10n.pick(lang, "العِبرة: ${event.lessonAr}", "Lesson: ${event.lessonEn}"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val allSunnahs = SeerahAtlasEngine.shamailSunnahs
                    val doneCount = allSunnahs.count { it.id in completedSunnahs }
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = L10n.pick(lang, "إحياء سنن الشمائل اليومية", "Daily Prophetic Sunnah Revival", "Kundalik sunnatlarni tiriltirish"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = noorColors.goldAccent
                                        )
                                    }
                                    Text(
                                        text = "$doneCount / ${allSunnahs.size}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { if (allSunnahs.isEmpty()) 0f else doneCount.toFloat() / allSunnahs.size },
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                                    color = GoldBright,
                                    trackColor = EmeraldPrimary.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    items(allSunnahs, key = { it.id }) { sunnah ->
                        val isDone = sunnah.id in completedSunnahs
                        LiquidGlassCard(
                            goldBorder = isDone,
                            onClick = { completedSunnahs = SeerahAtlasEngine.toggleSunnahCompleted(context, sunnah.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isDone) Color(0xFF4ADE80) else noorColors.goldAccent,
                                    modifier = Modifier.size(26.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = L10n.pick(lang, sunnah.titleAr, sunnah.titleEn, sunnah.titleUz),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = L10n.pick(lang, sunnah.narrationAr, sunnah.narrationEn),
                                        fontFamily = if (lang == com.example.domain.model.AppLanguage.ARABIC) AmiriFontFamily else MaterialTheme.typography.bodyMedium.fontFamily,
                                        fontSize = if (lang == com.example.domain.model.AppLanguage.ARABIC) 17.sp else 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${sunnah.sourceAr} · ${L10n.pick(lang, sunnah.dailyPracticeAr, sunnah.dailyPracticeEn)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = noorColors.goldAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HijrahRouteCanvasMap(
    waypoints: List<HijrahWaypoint>,
    selectedIndex: Int
) {
    val infinite = rememberInfiniteTransition(label = "hijrah_pulse")
    val beaconPulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A2A22), Color(0xFF143C2E), Color(0xFF1B3A2A))
                )
            )
            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            val w = size.width
            val h = size.height

            // Subtle desert topographic latitude/longitude grid lines
            for (i in 1..4) {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, h * i / 5f),
                    end = Offset(w, h * i / 5f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(w * i / 5f, 0f),
                    end = Offset(w * i / 5f, h),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Red Sea coastline subtle blue glow on west edge
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1C5F82).copy(alpha = 0.36f), Color.Transparent),
                    startX = 0f,
                    endX = w * 0.24f
                ),
                topLeft = Offset.Zero,
                size = size
            )

            // Route Path connecting all 7 Hijrah stops
            val points = waypoints.map { Offset(it.normX * w, it.normY * h) }
            val routePath = Path().apply {
                points.forEachIndexed { idx, pt ->
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = routePath,
                color = GoldPrimary.copy(alpha = 0.45f),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )
            )

            // Lit progress trail up to selectedIndex
            if (selectedIndex > 0) {
                val activePath = Path().apply {
                    for (i in 0..selectedIndex.coerceAtMost(points.lastIndex)) {
                        val pt = points[i]
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = activePath,
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFF4B8), GoldBright, EmeraldPrimary)),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Waypoint Nodes
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawCircle(
                        color = GoldBright.copy(alpha = 0.28f),
                        radius = 15.dp.toPx() * beaconPulse,
                        center = pt
                    )
                }
                drawCircle(
                    color = if (isSelected) GoldBright else if (idx < selectedIndex) EmeraldPrimary else Color(0xFF5D7A6E),
                    radius = if (isSelected) 7.5.dp.toPx() else 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                    center = pt
                )
            }
        }

        // Compass N indicator badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x88041C14))
                .border(1.dp, GoldPrimary.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("N", color = GoldBright, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}
