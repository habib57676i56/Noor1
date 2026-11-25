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
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.astronomy.CosmicEarthGlobeEngine
import com.example.domain.astronomy.GeodesicPoint
import com.example.domain.astronomy.GlobeCityBeacon
import com.example.domain.astronomy.SubsolarPoint
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Rare Flagship Astronomy Feature:
 * Interactive 3D Rotating Earth Globe showing:
 * - Real-time Solar Day/Night Terminator Hemisphere
 * - Glowing Golden Geodesic Qibla Arc from user's location directly to the Holy Kaaba
 * - Live pulsing global prayer-wave & Adhan beacons across Earth
 */
@Composable
fun CosmicQiblaGlobeScreen(
    userLatitude: Double,
    userLongitude: Double,
    userCityLabel: String,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var viewLat by remember { mutableDoubleStateOf(((userLatitude + CosmicEarthGlobeEngine.KAABA_LAT) / 2.0).coerceIn(-60.0, 60.0)) }
    var viewLon by remember { mutableDoubleStateOf((userLongitude + CosmicEarthGlobeEngine.KAABA_LON) / 2.0) }
    var autoSpin by remember { mutableStateOf(true) }

    // Live clock & smooth optional 3D globe rotation
    LaunchedEffect(autoSpin) {
        while (true) {
            delay(50L)
            if (autoSpin) {
                viewLon = ((viewLon + 0.35 + 180.0) % 360.0) - 180.0
            }
            if (System.currentTimeMillis() - nowMs >= 30_000L) {
                nowMs = System.currentTimeMillis()
            }
        }
    }

    val sun = remember(nowMs) { CosmicEarthGlobeEngine.computeSubsolarPoint(nowMs) }
    val geodesicArc = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.computeGeodesicToKaaba(userLatitude, userLongitude, steps = 40)
    }
    val qiblaBearing = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.qiblaBearingDegrees(userLatitude, userLongitude)
    }
    val kaabaDistKm = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.distanceToKaabaKm(userLatitude, userLongitude)
    }
    val beacons = remember(nowMs) { CosmicEarthGlobeEngine.evaluateGlobalBeacons(nowMs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cosmic_qibla_globe_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الكرة الأرضية ٣D وشعاع القبلة", "3D Earth & Qibla Geodesic Globe", "3D Yer shari va Qibla nuri"),
            subtitle = L10n.pick(
                lang,
                "اسحب بإصبعك لتدوير الكرة الأرضية · الليل والنهار الحي · قوس القبلة للكعبة",
                "Drag to spin 3D Earth · Live Day/Night Solar Terminator · Golden Kaaba Arc",
                "3D Yerni aylantiring · Jonli kun/tun chizig'i · Ka'baga oltin qibla yoyi"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Interactive 3D Earth Globe Card
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(315.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF0A2838), Color(0xFF031018), Color(0xFF01060A))
                                    )
                                )
                                .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { autoSpin = false },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            viewLon = ((viewLon - dragAmount.x * 0.35 + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
                                            viewLat = (viewLat + dragAmount.y * 0.25).coerceIn(-75.0, 75.0)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Interactive3DEarthCanvas(
                                viewLat = viewLat,
                                viewLon = viewLon,
                                sun = sun,
                                geodesicArc = geodesicArc,
                                beacons = beacons,
                                userLat = userLatitude,
                                userLon = userLongitude
                            )

                            // Live Day/Night legend badge
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x9902120E))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = GoldBright, modifier = Modifier.size(14.dp))
                                Text(
                                    text = L10n.pick(lang, "نهار / ليل حي", "Live Sun Terminator", "Jonli quyosh"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        // Camera Quick Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassChip(
                                label = if (autoSpin) L10n.pick(lang, "إيقاف الدوران", "Pause Spin", "To'xtatish")
                                else L10n.pick(lang, "دوران تلقائي", "Auto Spin", "Aylantirish"),
                                selected = autoSpin,
                                onClick = { autoSpin = !autoSpin },
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassChip(
                                label = L10n.pick(lang, "الكعبة المشرفة", "Center Kaaba", "Ka'ba"),
                                selected = false,
                                onClick = {
                                    autoSpin = false
                                    viewLat = CosmicEarthGlobeEngine.KAABA_LAT
                                    viewLon = CosmicEarthGlobeEngine.KAABA_LON
                                },
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassChip(
                                label = L10n.pick(lang, "موقعي", "My City", "Shahrim"),
                                selected = false,
                                onClick = {
                                    autoSpin = false
                                    viewLat = userLatitude
                                    viewLon = userLongitude
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Geodesic Telemetry Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlobeMetricPill(
                                label = L10n.pick(lang, "زاوية القبلة", "Qibla Bearing", "Qibla burchagi"),
                                value = String.format(Locale.US, "%.1f°", qiblaBearing),
                                modifier = Modifier.weight(1f)
                            )
                            GlobeMetricPill(
                                label = L10n.pick(lang, "المسافة للكعبة", "To Kaaba", "Ka'bagacha"),
                                value = String.format(Locale.US, "%,.0f km", kaabaDistKm),
                                modifier = Modifier.weight(1f)
                            )
                            GlobeMetricPill(
                                label = L10n.pick(lang, "تعامد الشمس", "Subsolar", "Quyosh nuqtasi"),
                                value = String.format(Locale.US, "%.0f°, %.0f°", sun.latitudeDeg, sun.longitudeDeg),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Live Global Prayer & Day/Night Beacons List
            item {
                Text(
                    text = L10n.pick(
                        lang,
                        "موجة الليل والنهار والأذان حول الكوكب الآن (${userCityLabel} ← مكة)",
                        "Live Global Daylight & Adhan Wave ($userCityLabel → Makkah)",
                        "Sayyoramiz bo'ylab jonli kun/tun va azon to'lqini"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
            }

            items(beacons, key = { it.id }) { beacon ->
                LiquidGlassCard(
                    goldBorder = beacon.isAdhanActiveNow || beacon.id == "makkah",
                    contentPadding = PaddingValues(12.dp),
                    onClick = {
                        autoSpin = false
                        viewLat = beacon.lat
                        viewLon = beacon.lon
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (beacon.isAdhanActiveNow) GoldPrimary.copy(alpha = 0.25f)
                                        else if (beacon.isDaylight) Color(0xFF38BDF8).copy(alpha = 0.2f)
                                        else EmeraldDeep.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (beacon.isAdhanActiveNow) Icons.Rounded.Public
                                    else if (beacon.isDaylight) Icons.Rounded.WbSunny
                                    else Icons.Rounded.NightsStay,
                                    contentDescription = null,
                                    tint = if (beacon.isAdhanActiveNow) GoldBright else if (beacon.isDaylight) Color(0xFFFFE082) else Color(0xFF93C5FD),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = L10n.pick(lang, beacon.nameAr, beacon.nameEn),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f°N, %.1f°E", beacon.lat, beacon.lon),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (beacon.isAdhanActiveNow) EmeraldPrimary.copy(alpha = 0.28f)
                                    else noorColors.glassSurfaceStrong
                                )
                                .border(
                                    1.dp,
                                    if (beacon.isAdhanActiveNow) GoldBright else noorColors.glassBorderBottom,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = L10n.pick(lang, beacon.currentPhaseLabelAr, beacon.currentPhaseLabelEn),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (beacon.isAdhanActiveNow) GoldBright else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Interactive3DEarthCanvas(
    viewLat: Double,
    viewLon: Double,
    sun: SubsolarPoint,
    geodesicArc: List<GeodesicPoint>,
    beacons: List<GlobeCityBeacon>,
    userLat: Double,
    userLon: Double
) {
    val infinite = rememberInfiniteTransition(label = "globe_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.43f

        // Outer atmospheric celestial halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A8F73).copy(alpha = 0.35f), Color(0xFF0E4B6E).copy(alpha = 0.14f), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius * 1.24f
            ),
            radius = radius * 1.24f,
            center = Offset(cx, cy)
        )

        // Base night ocean sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0B2E3B), Color(0xFF051822), Color(0xFF020B11)),
                center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
                radius = radius * 1.3f
            ),
            radius = radius,
            center = Offset(cx, cy)
        )

        // Real-time Solar Illumination Grid Dots (gives true 3D Day/Night Terminator across the sphere)
        for (lat in -75..75 step 12) {
            for (lon in -180..168 step 12) {
                val proj = CosmicEarthGlobeEngine.projectOrthographic(lat.toDouble(), lon.toDouble(), viewLat, viewLon)
                if (!proj.isVisible) continue
                val inDay = CosmicEarthGlobeEngine.isPointInDaylight(lat.toDouble(), lon.toDouble(), sun)
                val sx = cx + proj.x * radius
                val sy = cy + proj.y * radius
                val depthAlpha = (0.25f + 0.75f * proj.z).coerceIn(0f, 1f)
                drawCircle(
                    color = if (inDay) Color(0xFF28A988).copy(alpha = 0.45f * depthAlpha)
                    else Color(0xFF173A4A).copy(alpha = 0.22f * depthAlpha),
                    radius = (if (inDay) 3.4.dp else 2.0.dp).toPx() * (0.6f + 0.4f * proj.z),
                    center = Offset(sx, sy)
                )
            }
        }

        // Latitude & Longitude Graticule Lines
        val gridColor = Color.White.copy(alpha = 0.10f)
        for (lat in listOf(-60.0, -30.0, 0.0, 30.0, 60.0)) {
            val path = Path()
            var started = false
            for (lon in -180..180 step 6) {
                val p = CosmicEarthGlobeEngine.projectOrthographic(lat, lon.toDouble(), viewLat, viewLon)
                if (p.isVisible) {
                    val ox = cx + p.x * radius
                    val oy = cy + p.y * radius
                    if (!started) { path.moveTo(ox, oy); started = true } else path.lineTo(ox, oy)
                } else {
                    started = false
                }
            }
            drawPath(path, color = if (lat == 0.0) GoldPrimary.copy(alpha = 0.26f) else gridColor, style = Stroke(1.dp.toPx()))
        }

        for (lon in -150..180 step 30) {
            val path = Path()
            var started = false
            for (lat in -84..84 step 6) {
                val p = CosmicEarthGlobeEngine.projectOrthographic(lat.toDouble(), lon.toDouble(), viewLat, viewLon)
                if (p.isVisible) {
                    val ox = cx + p.x * radius
                    val oy = cy + p.y * radius
                    if (!started) { path.moveTo(ox, oy); started = true } else path.lineTo(ox, oy)
                } else {
                    started = false
                }
            }
            drawPath(path, color = gridColor, style = Stroke(1.dp.toPx()))
        }

        // Golden Geodesic Qibla Arc from User Location directly to the Holy Kaaba (lifted slightly in 3D space!)
        val arcPath = Path()
        var arcStarted = false
        geodesicArc.forEachIndexed { idx, pt ->
            val p = CosmicEarthGlobeEngine.projectOrthographic(pt.latDeg, pt.lonDeg, viewLat, viewLon)
            if (p.isVisible) {
                val t = idx.toFloat() / geodesicArc.lastIndex.coerceAtLeast(1)
                val lift = 1f + 0.08f * kotlin.math.sin(t * Math.PI).toFloat()
                val ox = cx + p.x * radius * lift
                val oy = cy + p.y * radius * lift
                if (!arcStarted) { arcPath.moveTo(ox, oy); arcStarted = true } else arcPath.lineTo(ox, oy)
            } else {
                arcStarted = false
            }
        }
        drawPath(
            path = arcPath,
            brush = Brush.linearGradient(listOf(Color(0xFF4ADE80), GoldBright, Color(0xFFFFF8D6))),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Global City Beacons
        beacons.forEach { b ->
            val p = CosmicEarthGlobeEngine.projectOrthographic(b.lat, b.lon, viewLat, viewLon)
            if (!p.isVisible) return@forEach
            val ox = cx + p.x * radius
            val oy = cy + p.y * radius
            if (b.id == "makkah") {
                drawCircle(GoldBright.copy(alpha = 0.30f), radius = 14.dp.toPx() * pulse, center = Offset(ox, oy))
                drawCircle(GoldBright, radius = 6.dp.toPx(), center = Offset(ox, oy))
                drawCircle(Color(0xFF1A1002), radius = 2.5.dp.toPx(), center = Offset(ox, oy))
            } else if (b.isAdhanActiveNow) {
                drawCircle(Color(0xFF4ADE80).copy(alpha = 0.32f), radius = 11.dp.toPx() * pulse, center = Offset(ox, oy))
                drawCircle(Color(0xFF4ADE80), radius = 4.5.dp.toPx(), center = Offset(ox, oy))
            } else {
                drawCircle(
                    color = if (b.isDaylight) Color(0xFFFFF1A8).copy(alpha = 0.8f) else Color(0xFF93C5FD).copy(alpha = 0.85f),
                    radius = 3.2.dp.toPx(),
                    center = Offset(ox, oy)
                )
            }
        }

        // User City Marker
        val userP = CosmicEarthGlobeEngine.projectOrthographic(userLat, userLon, viewLat, viewLon)
        if (userP.isVisible) {
            val ux = cx + userP.x * radius
            val uy = cy + userP.y * radius
            drawCircle(Color(0xFF4ADE80).copy(alpha = 0.32f), radius = 10.dp.toPx() * pulse, center = Offset(ux, uy))
            drawCircle(Color(0xFF4ADE80), radius = 5.dp.toPx(), center = Offset(ux, uy))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(ux, uy))
        }

        // Rim atmospheric glass edge
        drawCircle(
            brush = Brush.linearGradient(
                listOf(Color(0x88FFF3C4), Color(0x552FC291), Color(0x331E3A8A))
            ),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun GlobeMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    val noorColors = LocalNoorColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(noorColors.glassSurfaceStrong)
            .border(1.dp, noorColors.glassBorderBottom, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = GoldBright, maxLines = 1)
    }
}
