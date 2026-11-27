package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppLanguage
import com.example.domain.model.NearbyMosque
import com.example.location.LocationHelper
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NearbyMosquesUiState
import com.example.viewmodel.NoorViewModel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MosqueFinderScreen(
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val isAr = lang == AppLanguage.ARABIC
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsStateWithLifecycle()
    val mosquesState by viewModel.nearbyMosquesState.collectAsStateWithLifecycle()
    var radiusMeters by remember { mutableIntStateOf(5000) }
    var selectedMosque by remember { mutableStateOf<NearbyMosque?>(null) }
    var hasLocationPermission by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
    var didRequestPermissionOnce by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            viewModel.detectLocationAutomatically()
        }
    }

    // Automatically detect exact live GPS coordinates when opening Nearby Mosques!
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            hasLocationPermission = true
            viewModel.detectLocationAutomatically()
        } else if (!didRequestPermissionOnce) {
            didRequestPermissionOnce = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(prefs.latitude, prefs.longitude, radiusMeters) {
        viewModel.searchNearbyMosques(radiusMeters)
    }

    fun requestLiveGpsAndRefresh() {
        if (LocationHelper.hasLocationPermission(context)) {
            hasLocationPermission = true
            viewModel.detectLocationAutomatically()
            viewModel.searchNearbyMosques(radiusMeters)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun openExternalMapsQuery() {
        val uri = Uri.parse(
            String.format(Locale.US, "geo:%.6f,%.6f?q=mosque", prefs.latitude, prefs.longitude)
        )
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    fun openDirectionsTo(mosque: NearbyMosque) {
        val navUri = Uri.parse(
            String.format(Locale.US, "google.navigation:q=%.6f,%.6f", mosque.latitude, mosque.longitude)
        )
        val geoUri = Uri.parse(
            String.format(
                Locale.US,
                "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
                mosque.latitude, mosque.longitude,
                mosque.latitude, mosque.longitude,
                Uri.encode(mosque.name)
            )
        )
        val launched = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, navUri))
            true
        }.getOrDefault(false)
        if (!launched) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mosque_finder_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "المساجد القريبة منك", "Nearby Mosques"),
            subtitle = L10n.pick(
                lang,
                "${prefs.cityNameAr} · ${if (prefs.isAutoLocation) "موقع GPS دقيق" else "موقع المدينة"}",
                "${prefs.cityNameEn} · ${if (prefs.isAutoLocation) "Live GPS" else "Selected City"}"
            ),
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = { requestLiveGpsAndRefresh() },
                    modifier = Modifier.testTag("mosque_refresh_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = noorColors.goldAccent)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live High-Accuracy GPS Location Bar
            item {
                LiquidGlassCard(
                    goldBorder = !prefs.isAutoLocation,
                    onClick = { requestLiveGpsAndRefresh() },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("mosque_gps_detect_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (prefs.isAutoLocation) EmeraldPrimary.copy(alpha = 0.22f)
                                    else GoldPrimary.copy(alpha = 0.22f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = GoldBright
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    tint = if (prefs.isAutoLocation) EmeraldPrimary else GoldBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isDetectingLocation -> L10n.pick(lang, "جارٍ تحديد موقعك الدقيق عبر GPS...", "Detecting your exact GPS location...")
                                    prefs.isAutoLocation -> L10n.pick(lang, "موقعك الحالي الدقيق: ${prefs.cityNameAr}", "Your Live GPS: ${prefs.cityNameEn}")
                                    !hasLocationPermission -> L10n.pick(lang, "اضغط لتفعيل GPS وعرض المساجد حولك بدقة", "Tap to enable GPS for exact nearby mosques")
                                    else -> L10n.pick(lang, "اضغط لتحديث موقعك الحالي (GPS)", "Tap to refresh live GPS location")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%.4f, %.4f", prefs.latitude, prefs.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = L10n.pick(lang, "تحديث GPS", "Live GPS"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            // Radius selector & Open Maps action
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(listOf(2000 to "2 km", 5000 to "5 km", 10000 to "10 km", 15000 to "15 km")) { (r, label) ->
                        LiquidGlassChip(
                            label = label,
                            selected = radiusMeters == r,
                            onClick = { radiusMeters = r }
                        )
                    }
                    item {
                        LiquidGlassChip(
                            label = L10n.pick(lang, "خرائط جوجل", "Google Maps"),
                            selected = false,
                            icon = Icons.Rounded.Map,
                            onClick = { openExternalMapsQuery() }
                        )
                    }
                }
            }

            when (val state = mosquesState) {
                is NearbyMosquesUiState.Idle,
                is NearbyMosquesUiState.Loading -> {
                    item {
                        LiquidGlassCard(contentPadding = PaddingValues(28.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = noorColors.goldAccent)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = L10n.pick(lang, "جارٍ مسح خرائط المساجد القريبة من موقعك بدقة...", "Scanning accurate nearby mosques around you..."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                is NearbyMosquesUiState.Error -> {
                    item {
                        LiquidGlassCard(goldBorder = true, contentPadding = PaddingValues(22.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Rounded.Mosque, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = L10n.pick(lang, state.messageAr, state.messageEn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { viewModel.searchNearbyMosques(radiusMeters) }) {
                                        Text(L10n.pick(lang, "إعادة المحاولة", "Retry"))
                                    }
                                    Button(
                                        onClick = { openExternalMapsQuery() },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(L10n.pick(lang, "فتح خرائط جوجل", "Open Google Maps"))
                                    }
                                }
                            }
                        }
                    }
                }

                is NearbyMosquesUiState.Ready -> {
                    item {
                        val effectiveRadius = max(
                            state.radiusMeters,
                            (state.mosques.maxOfOrNull { it.distanceMeters }?.roundToInt() ?: state.radiusMeters)
                        )
                        MosqueRadarCard(
                            mosques = state.mosques,
                            radiusMeters = effectiveRadius,
                            selectedMosque = selectedMosque ?: state.mosques.firstOrNull(),
                            onOpenDirections = { mosque -> openDirectionsTo(mosque) }
                        )
                    }

                    if (state.mosques.isEmpty()) {
                        item {
                            LiquidGlassCard(contentPadding = PaddingValues(20.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = L10n.pick(lang, "لم يتم العثور على مساجد مسجلة ضمن هذا النطاق. جرّب توسيع النطاق أو فتح الخريطة المباشرة.", "No mapped mosques found within this radius. Try expanding the radius or opening Maps."),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (radiusMeters < 15000) {
                                            OutlinedButton(onClick = { radiusMeters = 15000 }) {
                                                Text(L10n.pick(lang, "توسيع إلى 15 كم", "Expand to 15 km"))
                                            }
                                        }
                                        Button(
                                            onClick = { openExternalMapsQuery() },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                        ) {
                                            Text(L10n.pick(lang, "البحث عبر الخرائط", "Open Maps"))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "تم العثور على ${state.mosques.size} مسجد قريب مرتّبة من الأقرب إليك",
                                    "Found ${state.mosques.size} nearby mosques sorted closest-first"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }

                        items(state.mosques, key = { it.id }) { mosque ->
                            val isSelected = selectedMosque?.id == mosque.id
                            val walkMinutes = max(1, (mosque.distanceMeters / 75.0).roundToInt())
                            LiquidGlassCard(
                                goldBorder = isSelected,
                                onClick = { selectedMosque = mosque },
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Mosque, contentDescription = null, tint = noorColors.goldAccent)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mosque.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = formatDistance(mosque.distanceMeters, isAr),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                            Text(
                                                text = " · ${cardinalDirection(mosque.bearingDegrees, isAr)} · ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Icon(
                                                Icons.Rounded.DirectionsWalk,
                                                contentDescription = null,
                                                tint = noorColors.goldAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = if (isAr) "$walkMinutes د" else "${walkMinutes}m",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (mosque.address.isNotBlank()) {
                                            Text(
                                                text = mosque.address,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { openDirectionsTo(mosque) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Directions,
                                            contentDescription = "Directions",
                                            tint = noorColors.goldAccent
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
}

@Composable
private fun MosqueRadarCard(
    mosques: List<NearbyMosque>,
    radiusMeters: Int,
    selectedMosque: NearbyMosque?,
    onOpenDirections: (NearbyMosque) -> Unit
) {
    val lang = LocalAppLanguage.current
    val isAr = lang == AppLanguage.ARABIC
    val noorColors = LocalNoorColors.current

    LiquidGlassCard(goldBorder = true, contentPadding = PaddingValues(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF10382A), Color(0xFF061611)))),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension * 0.43f

                listOf(0.35f, 0.68f, 1.0f).forEach { frac ->
                    drawCircle(
                        color = GoldPrimary.copy(alpha = 0.25f),
                        radius = maxR * frac,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                drawLine(GoldPrimary.copy(alpha = 0.18f), Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 1.dp.toPx())
                drawLine(GoldPrimary.copy(alpha = 0.18f), Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 1.dp.toPx())

                val safeRadius = radiusMeters.coerceAtLeast(500).toDouble()
                mosques.take(30).forEach { m ->
                    val distFrac = (m.distanceMeters / safeRadius).toFloat().coerceIn(0.10f, 0.96f)
                    val angleRad = (m.bearingDegrees - 90.0) * PI / 180.0
                    val px = cx + (maxR * distFrac * cos(angleRad)).toFloat()
                    val py = cy + (maxR * distFrac * sin(angleRad)).toFloat()
                    val isSel = selectedMosque?.id == m.id
                    drawCircle(
                        color = if (isSel) GoldBright else Color(0xFF39D98A),
                        radius = if (isSel) 7.dp.toPx() else 4.5.dp.toPx(),
                        center = Offset(px, py)
                    )
                    if (isSel) {
                        drawCircle(
                            color = GoldBright.copy(alpha = 0.35f),
                            radius = 13.dp.toPx(),
                            center = Offset(px, py),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                drawCircle(color = Color(0xFF60A5FA).copy(alpha = 0.28f), radius = 12.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = Color(0xFF60A5FA), radius = 6.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
            }

            Text(
                text = if (isAr) "شمال" else "N",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldBright,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            )
        }

        if (selectedMosque != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = noorColors.goldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedMosque.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${formatDistance(selectedMosque.distanceMeters, isAr)} · ${cardinalDirection(selectedMosque.bearingDegrees, isAr)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onOpenDirections(selectedMosque) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.pick(lang, "الاتجاهات", "Directions"))
                }
            }
        }
    }
}

private fun formatDistance(meters: Double, isArabic: Boolean): String {
    return if (meters < 1000.0) {
        if (isArabic) "${meters.toInt()} متر" else "${meters.toInt()} m"
    } else {
        val km = String.format(Locale.US, "%.1f", meters / 1000.0)
        if (isArabic) "$km كم" else "$km km"
    }
}

private fun cardinalDirection(deg: Double, isArabic: Boolean): String {
    val norm = ((deg % 360.0) + 360.0) % 360.0
    return when {
        norm < 22.5 || norm >= 337.5 -> if (isArabic) "شمال" else "N"
        norm < 67.5 -> if (isArabic) "شمال شرق" else "NE"
        norm < 112.5 -> if (isArabic) "شرق" else "E"
        norm < 157.5 -> if (isArabic) "جنوب شرق" else "SE"
        norm < 202.5 -> if (isArabic) "جنوب" else "S"
        norm < 247.5 -> if (isArabic) "جنوب غرب" else "SW"
        norm < 292.5 -> if (isArabic) "غرب" else "W"
        else -> if (isArabic) "شمال غرب" else "NW"
    }
}
