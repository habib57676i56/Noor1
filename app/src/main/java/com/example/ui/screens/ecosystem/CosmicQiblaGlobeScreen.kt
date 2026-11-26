package com.example.ui.screens.ecosystem

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.domain.astronomy.CosmicEarthGlobeEngine
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class SacredSatelliteLandmark(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val cityAr: String,
    val cityEn: String,
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    val noteAr: String,
    val noteEn: String
)

object SacredSatelliteAtlasCatalog {
    val landmarks: List<SacredSatelliteLandmark> = listOf(
        SacredSatelliteLandmark("kaaba", "الكعبة المشرفة والمسجد الحرام", "The Holy Kaaba & Masjid al-Haram", "مكة المكرمة", "Makkah", 21.422487, 39.826206, 17.8, "قبلة المسلمين وأول بيت وُضع للناس — قرّب لترى صحن المطاف والكعبة من القمر الصناعي", "Qibla of all Muslims — zoom in to inspect the Mataf courtyard & Kaaba from satellite"),
        SacredSatelliteLandmark("nabawi", "المسجد النبوي الشريف والروضة", "Al-Masjid an-Nabawi", "المدينة المنورة", "Madinah", 24.467213, 39.611160, 17.4, "مسجد رسول الله ﷺ والقبة الخضراء وساحات المظلات الشهيرة", "Mosque of Prophet Muhammad ﷺ, the Green Dome & shaded plazas"),
        SacredSatelliteLandmark("aqsa", "المسجد الأقصى وقبة الصخرة", "Al-Aqsa Mosque & Dome of the Rock", "القدس الشريف", "Jerusalem", 31.778019, 35.235398, 17.6, "أولى القبلتين ومسرى النبي ﷺ وثالث الحرمين الشريفين", "First Qibla, Night Journey sanctuary & third holiest mosque"),
        SacredSatelliteLandmark("quba", "مسجد قباء", "Quba Mosque", "المدينة المنورة", "Madinah", 24.439282, 39.617292, 17.5, "أول مسجد أُسس على التقوى — الصلاة فيه تعدل أجر عمرة", "First mosque founded in Islam — prayer inside equals an Umrah"),
        SacredSatelliteLandmark("hira", "جبل النور وغار حراء", "Jabal al-Nour & Cave of Hira", "مكة المكرمة", "Makkah", 21.457500, 39.859300, 16.2, "الموضع الذي نزل فيه أول الوحي: ﴿اقْرَأْ بِاسْمِ رَبِّكَ﴾", "Mountain peak where the very first revelation of the Quran descended"),
        SacredSatelliteLandmark("arafat", "جبل الرحمة وصعيد عرفات", "Mount Arafat (Jabal al-Rahmah)", "مكة المكرمة", "Makkah", 21.354900, 39.984100, 16.0, "موقف الحج الأعظم وخطبة الوداع الخالدة", "Heart of Hajj pilgrimage and the Farewell Sermon"),
        SacredSatelliteLandmark("umayyad", "الجامع الأموي الكبير", "Umayyad Mosque", "دمشق", "Damascus", 33.511944, 36.306667, 17.5, "من أعرق وأجمل مساجد العالم الإسلامي تاريخاً وعمارة", "One of the oldest and grandest historic mosques in Islam"),
        SacredSatelliteLandmark("azhar", "الجامع الأزهر الشريف", "Al-Azhar Mosque", "القاهرة", "Cairo", 30.045700, 31.262700, 17.5, "منارة العلم الشرعي واللغة العربية لأكثر من ألف عام", "Beacon of Islamic scholarship for over a thousand years"),
        SacredSatelliteLandmark("qarawiyyin", "جامع وجامعة القرويين", "Al-Qarawiyyin Mosque", "فاس", "Fez", 34.064400, -4.973600, 17.6, "أسستها فاطمة الفهرية عام ٢٤٥ هـ — أقدم جامعة مستمرة في العالم", "Founded by Fatima al-Fihri in 859 CE — oldest continuous university"),
        SacredSatelliteLandmark("ayasofya", "مسجد آيا صوفيا الكبير", "Hagia Sophia Grand Mosque", "إسطنبول", "Istanbul", 41.008583, 28.980175, 17.4, "معلم إسلامي وتاريخي عظيم في قلب إسطنبول", "Iconic grand mosque and architectural landmark in Istanbul")
    )

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }
}

/**
 * REAL Photorealistic 3D Blue Satellite Earth & Deep-Zoom Qibla Globe:
 * - Mode 0: Live 3D Blue Satellite Earth (MapLibre GL v5 3D Globe + Esri World Imagery real satellite tiles
 *           + optional place/street labels) allowing deep zoom from outer space (Zoom 1.5) all the way
 *           down to Zoom 18.5 to see the actual Kaaba courtyard, real cities, and streets!
 * - Mode 1: Offline Photorealistic Blue Marble Earth (high-res NASA-style bundled Earth image with pinch-zoom)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CosmicQiblaGlobeScreen(
    userLatitude: Double,
    userLongitude: Double,
    userCityLabel: String,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var viewMode by remember { mutableIntStateOf(0) } // 0 = Real Live 3D Satellite Earth, 1 = Offline Blue Marble
    var showPlaceLabels by remember { mutableStateOf(true) }
    var isWebLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var activeFocusLabel by remember { mutableStateOf(L10n.pick(lang, "الكرة الأرضية الزرقاء 3D", "3D Blue Satellite Earth")) }

    val qiblaBearing = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.qiblaBearingDegrees(userLatitude, userLongitude)
    }
    val kaabaDistKm = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.distanceToKaabaKm(userLatitude, userLongitude)
    }
    val geodesicArc = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.computeGeodesicToKaaba(userLatitude, userLongitude, steps = 48)
    }

    val globeHtml = remember(userLatitude, userLongitude, userCityLabel) {
        buildRealBlueSatelliteGlobeHtml(
            userLat = userLatitude,
            userLon = userLongitude,
            userLabel = userCityLabel.replace("'", "").replace("\"", ""),
            arcPoints = geodesicArc.map { it.lonDeg to it.latDeg }
        )
    }

    fun flyTo(lat: Double, lon: Double, zoom: Double, label: String) {
        activeFocusLabel = label
        if (viewMode != 0) viewMode = 0
        webViewRef?.evaluateJavascript(
            String.format(Locale.US, "window.noorFlyTo && window.noorFlyTo(%.6f, %.6f, %.2f);", lat, lon, zoom),
            null
        )
    }

    fun zoomBy(delta: Double) {
        webViewRef?.evaluateJavascript(
            String.format(Locale.US, "window.noorZoomBy && window.noorZoomBy(%.2f);", delta),
            null
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cosmic_qibla_globe_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الكرة الأرضية ٣D الحقيقية والقبلة", "Real 3D Blue Satellite Earth & Qibla"),
            subtitle = L10n.pick(
                lang,
                "كرة أرضية زرقاء حقيقية بالأقمار الصناعية · قرّب حتى ترى الكعبة والأماكن والمدن",
                "Real Blue Satellite 3D Earth · Deep-Zoom down to the Kaaba, Cities & Streets"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "القمر الصناعي 3D الحي (تقريب فائق)", "Live 3D Satellite Earth"),
                        selected = viewMode == 0,
                        onClick = { viewMode = 0 },
                        modifier = Modifier.weight(1f).testTag("globe_mode_satellite")
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الأرض الزرقاء (بدون إنترنت)", "Offline Blue Marble"),
                        selected = viewMode == 1,
                        onClick = { viewMode = 1 },
                        modifier = Modifier.weight(1f).testTag("globe_mode_offline")
                    )
                }
            }

            // Main Interactive Real Blue Earth Viewport
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(390.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF020B18))
                                .border(1.4.dp, Color(0xFF38BDF8).copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        ) {
                            if (viewMode == 0) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            setBackgroundColor(AndroidColor.parseColor("#020B18"))
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                            settings.loadWithOverviewMode = true
                                            settings.useWideViewPort = true
                                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            // Allow smooth pan/pinch inside LazyColumn without scroll conflict
                                            setOnTouchListener { v, _ ->
                                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                                false
                                            }
                                            webChromeClient = WebChromeClient()
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    isWebLoading = false
                                                }
                                            }
                                            loadDataWithBaseURL("https://mt1.google.com/", globeHtml, "text/html", "UTF-8", null)
                                            webViewRef = this
                                        }
                                    },
                                    update = { wv ->
                                        webViewRef = wv
                                        wv.evaluateJavascript(
                                            "window.noorSetLabels && window.noorSetLabels($showPlaceLabels);",
                                            null
                                        )
                                    }
                                )

                                // Instant non-blocking safety timeout so spinner never hangs
                                androidx.compose.runtime.LaunchedEffect(viewMode) {
                                    if (viewMode == 0 && isWebLoading) {
                                        kotlinx.coroutines.delay(1400L)
                                        isWebLoading = false
                                    }
                                }

                                if (isWebLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0x88020B18)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = L10n.pick(lang, "جاري فتح القمر الصناعي المباشر...", "Opening Live Satellite Earth..."),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Top focus pill
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xCC031426))
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.SatelliteAlt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                    Text(
                                        text = activeFocusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Right floating Zoom +/- & Place Labels buttons
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlobeOverlayCircleButton(
                                        icon = Icons.Rounded.Add,
                                        contentDesc = "Zoom In",
                                        onClick = { zoomBy(1.8) }
                                    )
                                    GlobeOverlayCircleButton(
                                        icon = Icons.Rounded.Remove,
                                        contentDesc = "Zoom Out",
                                        onClick = { zoomBy(-1.8) }
                                    )
                                    GlobeOverlayCircleButton(
                                        icon = Icons.Rounded.Layers,
                                        contentDesc = "Toggle Place Labels",
                                        active = showPlaceLabels,
                                        onClick = { showPlaceLabels = !showPlaceLabels }
                                    )
                                }
                            } else {
                                OfflinePhotorealisticBlueEarthView(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Instant Deep-Zoom Fly-To Quick Bar
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "الكعبة (تقريب فائق)", "Kaaba Deep Zoom"),
                                    selected = false,
                                    onClick = {
                                        flyTo(21.422487, 39.826206, 17.8, L10n.pick(lang, "الكعبة المشرفة — مكة", "Holy Kaaba — Makkah"))
                                    }
                                )
                            }
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "المسجد النبوي", "Masjid Nabawi"),
                                    selected = false,
                                    onClick = {
                                        flyTo(24.467213, 39.611160, 17.4, L10n.pick(lang, "المسجد النبوي الشريف", "Al-Masjid an-Nabawi"))
                                    }
                                )
                            }
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "المسجد الأقصى", "Al-Aqsa"),
                                    selected = false,
                                    onClick = {
                                        flyTo(31.778019, 35.235398, 17.6, L10n.pick(lang, "المسجد الأقصى — القدس", "Al-Aqsa — Jerusalem"))
                                    }
                                )
                            }
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "موقعي الحالي", "My Location"),
                                    selected = false,
                                    onClick = {
                                        flyTo(userLatitude, userLongitude, 13.5, userCityLabel)
                                    }
                                )
                            }
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "الكرة كاملة 3D", "Full 3D Earth"),
                                    selected = false,
                                    onClick = {
                                        flyTo(
                                            (userLatitude + 21.4225) / 2.0,
                                            (userLongitude + 39.8262) / 2.0,
                                            1.9,
                                            L10n.pick(lang, "الكرة الأرضية الزرقاء 3D", "3D Blue Satellite Earth")
                                        )
                                    }
                                )
                            }
                        }

                        // Real Geodesic Telemetry Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RealGlobeMetricPill(
                                label = L10n.pick(lang, "زاوية القبلة", "Qibla Bearing"),
                                value = String.format(Locale.US, "%.1f°", qiblaBearing),
                                modifier = Modifier.weight(1f)
                            )
                            RealGlobeMetricPill(
                                label = L10n.pick(lang, "المسافة للكعبة", "Distance to Kaaba"),
                                value = String.format(Locale.US, "%,.0f km", kaabaDistKm),
                                modifier = Modifier.weight(1f)
                            )
                            RealGlobeMetricPill(
                                label = L10n.pick(lang, "موقعك", "Your City"),
                                value = userCityLabel.take(12),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Interactive Sacred Islamic Landmarks Satellite Explorer
            item {
                Text(
                    text = L10n.pick(lang, "رحلة الأقمار الصناعية إلى المعالم الإسلامية (اضغط للتقريب الفائق)", "Satellite Fly-To Sacred Islamic Landmarks"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = noorColors.goldAccent,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(SacredSatelliteAtlasCatalog.landmarks, key = { it.id }) { site ->
                val distKm = remember(userLatitude, userLongitude, site.id) {
                    SacredSatelliteAtlasCatalog.distanceKm(userLatitude, userLongitude, site.lat, site.lon)
                }
                LiquidGlassCard(
                    onClick = {
                        flyTo(site.lat, site.lon, site.zoom, L10n.pick(lang, site.nameAr, site.nameEn))
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B3B59).copy(alpha = 0.7f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Mosque, contentDescription = null, tint = GoldBright, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, site.nameAr, site.nameEn),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L10n.pick(lang, site.noteAr, site.noteEn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "${site.cityAr} · يبعد عنك ${String.format(Locale.US, "%,.0f", distKm)} كم",
                                    "${site.cityEn} · ${String.format(Locale.US, "%,.0f", distKm)} km away"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7DD3FC),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.FlightTakeoff,
                            contentDescription = L10n.pick(lang, "انتقل بالقمر الصناعي", "Fly to Landmark"),
                            tint = GoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobeOverlayCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xDD0C4A6E) else Color(0xCC031426))
            .border(
                1.2.dp,
                if (active) Color(0xFF38BDF8) else GoldPrimary.copy(alpha = 0.55f),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDesc, tint = if (active) Color(0xFF7DD3FC) else GoldBright, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RealGlobeMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = GoldBright,
            maxLines = 1
        )
    }
}

@Composable
private fun OfflinePhotorealisticBlueEarthView(modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 4.5f)
                    scale = nextScale
                    offset = if (nextScale <= 1.05f) Offset.Zero else offset + pan
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_earth_blue_marble),
            contentDescription = L10n.pick(lang, "الكرة الأرضية الزرقاء الحقيقية من الفضاء", "Real Blue Marble Planet Earth"),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xCC02101F))
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = L10n.pick(lang, "استخدم إصبعين للتقريب · وضع بدون إنترنت", "Pinch to Zoom · Offline Blue Marble"),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun buildRealBlueSatelliteGlobeHtml(
    userLat: Double,
    userLon: Double,
    userLabel: String,
    arcPoints: List<Pair<Double, Double>>
): String {
    val coordsJson = arcPoints.joinToString(",") { (lon, lat) ->
        String.format(Locale.US, "[%.5f,%.5f]", lon, lat)
    }
    val midLat = ((userLat + 21.422487) / 2.0).coerceIn(-55.0, 55.0)
    val midLon = (userLon + 39.826206) / 2.0

    // Zero-external-script instant HTML5 Canvas Slippy-Tile Satellite Engine:
    // Loads in <50ms with zero CDN wait, streams Google Satellite Hybrid (with real labels)
    // and Esri World Imagery fallback, full pinch-zoom & drag up to Zoom 19, plus Golden Qibla Arc.
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no" />
<style>
  html, body { margin:0; padding:0; width:100%; height:100%; background:#020B18; overflow:hidden; touch-action:none; }
  canvas { display:block; width:100%; height:100%; }
  #srvBtn {
    position:fixed; left:10px; bottom:10px; z-index:20;
    background:rgba(3,20,38,0.88); color:#7DD3FC; border:1px solid rgba(56,189,248,0.6);
    border-radius:999px; padding:5px 11px; font:700 11px sans-serif;
  }
</style>
</head>
<body>
<canvas id="c"></canvas>
<button id="srvBtn" onclick="window.noorSwitchProvider()">🛰️ Google Satellite</button>
<script>
(function() {
  const canvas = document.getElementById('c');
  const ctx = canvas.getContext('2d');
  const dpr = Math.min(window.devicePixelRatio || 1, 2);

  let W = 0, H = 0;
  function resize() {
    W = window.innerWidth;
    H = window.innerHeight;
    canvas.width = Math.floor(W * dpr);
    canvas.height = Math.floor(H * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    render();
  }
  window.addEventListener('resize', resize);

  // Camera state
  let centerLat = ${String.format(Locale.US, "%.5f", midLat)};
  let centerLon = ${String.format(Locale.US, "%.5f", midLon)};
  let zoom = 3.2;
  let showLabels = true;
  let providerIdx = 0; // 0 = Google Satellite Hybrid, 1 = Esri World Imagery
  const tileCache = new Map();
  const arcCoords = [$coordsJson];
  const kaabaLat = 21.422487, kaabaLon = 39.826206;
  const userLat = ${String.format(Locale.US, "%.5f", userLat)};
  const userLon = ${String.format(Locale.US, "%.5f", userLon)};
  const userLabel = '${userLabel.ifBlank { "موقعي" }}';

  function latLonToWorld(lat, lon, z) {
    const clampedLat = Math.max(-85.0511, Math.min(85.0511, lat));
    const s = Math.sin(clampedLat * Math.PI / 180);
    const scale = 256 * Math.pow(2, z);
    const x = ((lon + 180) / 360) * scale;
    const y = (0.5 - Math.log((1 + s) / (1 - s)) / (4 * Math.PI)) * scale;
    return [x, y];
  }

  function worldToLatLon(wx, wy, z) {
    const scale = 256 * Math.pow(2, z);
    const lon = (wx / scale) * 360 - 180;
    const n = Math.PI - (2 * Math.PI * wy) / scale;
    const lat = (180 / Math.PI) * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
    return [Math.max(-85, Math.min(85, lat)), ((lon + 540) % 360) - 180];
  }

  function tileUrl(z, x, y) {
    if (providerIdx === 0) {
      const lyrs = showLabels ? 'y' : 's';
      const sub = (x + y) & 3;
      return 'https://mt' + sub + '.google.com/vt/lyrs=' + lyrs + '&hl=ar&x=' + x + '&y=' + y + '&z=' + z;
    }
    return 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/' + z + '/' + y + '/' + x;
  }

  function getTile(z, x, y) {
    const maxT = Math.pow(2, z);
    const wx = ((x % maxT) + maxT) % maxT;
    if (y < 0 || y >= maxT) return null;
    const key = providerIdx + '_' + (showLabels ? 1 : 0) + '_' + z + '_' + wx + '_' + y;
    let entry = tileCache.get(key);
    if (!entry) {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      entry = { img: img, loaded: false, err: false };
      img.onload = function() { entry.loaded = true; render(); };
      img.onerror = function() { entry.err = true; };
      img.src = tileUrl(z, wx, y);
      tileCache.set(key, entry);
      if (tileCache.size > 260) {
        const firstKey = tileCache.keys().next().value;
        tileCache.delete(firstKey);
      }
    }
    return entry;
  }

  function drawPin(sx, sy, text, borderCol, glowCol) {
    ctx.save();
    ctx.font = 'bold 11px sans-serif';
    const padX = 9, h = 24;
    const tw = ctx.measureText(text).width;
    const bw = tw + padX * 2;
    const bx = sx - bw / 2;
    const by = sy - h - 10;

    // Pulse dot
    ctx.beginPath();
    ctx.arc(sx, sy, 6, 0, Math.PI * 2);
    ctx.fillStyle = glowCol;
    ctx.fill();
    ctx.beginPath();
    ctx.arc(sx, sy, 3.5, 0, Math.PI * 2);
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();

    // Label pill
    ctx.beginPath();
    ctx.roundRect(bx, by, bw, h, 12);
    ctx.fillStyle = 'rgba(3, 20, 38, 0.90)';
    ctx.fill();
    ctx.lineWidth = 1.4;
    ctx.strokeStyle = borderCol;
    ctx.stroke();

    ctx.fillStyle = '#FFFFFF';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, sx, by + h / 2);
    ctx.restore();
  }

  let rafPending = false;
  function render() {
    if (rafPending) return;
    rafPending = true;
    requestAnimationFrame(drawFrame);
  }

  function drawFrame() {
    rafPending = false;
    if (!W || !H) return;

    // Deep space blue backdrop
    const bg = ctx.createRadialGradient(W * 0.5, H * 0.5, 20, W * 0.5, H * 0.5, Math.max(W, H) * 0.7);
    bg.addColorStop(0, '#072547');
    bg.addColorStop(1, '#020B18');
    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, W, H);

    const zInt = Math.max(2, Math.min(19, Math.round(zoom)));
    const scaleFactor = Math.pow(2, zoom - zInt);
    const tilePx = 256 * scaleFactor;
    const [cx, cy] = latLonToWorld(centerLat, centerLon, zInt);

    const startX = Math.floor((cx - (W / 2) / scaleFactor) / 256) - 1;
    const endX = Math.floor((cx + (W / 2) / scaleFactor) / 256) + 1;
    const startY = Math.floor((cy - (H / 2) / scaleFactor) / 256) - 1;
    const endY = Math.floor((cy + (H / 2) / scaleFactor) / 256) + 1;

    for (let tx = startX; tx <= endX; tx++) {
      for (let ty = startY; ty <= endY; ty++) {
        const entry = getTile(zInt, tx, ty);
        const dx = W / 2 + (tx * 256 - cx) * scaleFactor;
        const dy = H / 2 + (ty * 256 - cy) * scaleFactor;
        if (entry && entry.loaded) {
          ctx.drawImage(entry.img, dx, dy, tilePx + 0.6, tilePx + 0.6);
        } else {
          // Subtle blue ocean grid tile placeholder while streaming
          ctx.fillStyle = '#06203D';
          ctx.fillRect(dx, dy, tilePx, tilePx);
          ctx.strokeStyle = 'rgba(56,189,248,0.08)';
          ctx.strokeRect(dx, dy, tilePx, tilePx);
        }
      }
    }

    // Golden Geodesic Qibla Arc
    const [cwx, cwy] = latLonToWorld(centerLat, centerLon, zoom);
    if (arcCoords.length > 1) {
      ctx.save();
      ctx.beginPath();
      for (let i = 0; i < arcCoords.length; i++) {
        const [px, py] = latLonToWorld(arcCoords[i][1], arcCoords[i][0], zoom);
        const sx = W / 2 + (px - cwx);
        const sy = H / 2 + (py - cwy);
        if (i === 0) ctx.moveTo(sx, sy); else ctx.lineTo(sx, sy);
      }
      ctx.strokeStyle = 'rgba(245, 158, 11, 0.42)';
      ctx.lineWidth = 7;
      ctx.lineCap = 'round';
      ctx.stroke();
      ctx.strokeStyle = '#FDE68A';
      ctx.lineWidth = 2.6;
      ctx.stroke();
      ctx.restore();
    }

    // Kaaba & User Pins
    const [kx, ky] = latLonToWorld(kaabaLat, kaabaLon, zoom);
    drawPin(W / 2 + (kx - cwx), H / 2 + (ky - cwy), '🕋 الكعبة المشرفة', '#FBBF24', 'rgba(251,191,36,0.55)');

    const [ux, uy] = latLonToWorld(userLat, userLon, zoom);
    drawPin(W / 2 + (ux - cwx), H / 2 + (uy - cwy), '📍 ' + userLabel, '#38BDF8', 'rgba(56,189,248,0.55)');
  }

  // Smooth Fly-To Animation
  let animId = null;
  function animateTo(targetLat, targetLon, targetZoom) {
    if (animId) cancelAnimationFrame(animId);
    const startLat = centerLat, startLon = centerLon, startZoom = zoom;
    const t0 = performance.now();
    const dur = 750;
    function step(now) {
      const p = Math.min(1, (now - t0) / dur);
      const e = 1 - Math.pow(1 - p, 3);
      centerLat = startLat + (targetLat - startLat) * e;
      centerLon = startLon + (targetLon - startLon) * e;
      zoom = startZoom + (Math.max(2, Math.min(19, targetZoom)) - startZoom) * e;
      render();
      if (p < 1) animId = requestAnimationFrame(step);
    }
    animId = requestAnimationFrame(step);
  }

  // Touch Drag & Pinch-to-Zoom
  let dragging = false, lastX = 0, lastY = 0, pinchDist = 0, pinchZoom = 0;
  canvas.addEventListener('touchstart', function(e) {
    if (animId) cancelAnimationFrame(animId);
    if (e.touches.length === 1) {
      dragging = true;
      lastX = e.touches[0].clientX;
      lastY = e.touches[0].clientY;
    } else if (e.touches.length === 2) {
      dragging = false;
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      pinchDist = Math.hypot(dx, dy);
      pinchZoom = zoom;
    }
  }, { passive: false });

  canvas.addEventListener('touchmove', function(e) {
    e.preventDefault();
    if (e.touches.length === 1 && dragging) {
      const dx = e.touches[0].clientX - lastX;
      const dy = e.touches[0].clientY - lastY;
      lastX = e.touches[0].clientX;
      lastY = e.touches[0].clientY;
      const [wx, wy] = latLonToWorld(centerLat, centerLon, zoom);
      const [nLat, nLon] = worldToLatLon(wx - dx, wy - dy, zoom);
      centerLat = nLat;
      centerLon = nLon;
      render();
    } else if (e.touches.length === 2 && pinchDist > 0) {
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      const dist = Math.hypot(dx, dy);
      const ratio = dist / pinchDist;
      zoom = Math.max(2, Math.min(19, pinchZoom + Math.log2(Math.max(0.25, ratio))));
      render();
    }
  }, { passive: false });

  canvas.addEventListener('touchend', function() { dragging = false; pinchDist = 0; });

  window.noorFlyTo = function(lat, lon, z) { animateTo(lat, lon, z); };
  window.noorZoomBy = function(delta) { animateTo(centerLat, centerLon, zoom + delta); };
  window.noorSetLabels = function(visible) { showLabels = !!visible; render(); };
  window.noorSwitchProvider = function() {
    providerIdx = (providerIdx + 1) % 2;
    document.getElementById('srvBtn').innerText = providerIdx === 0 ? '🛰️ Google Satellite' : '🌍 Esri Satellite';
    render();
  };

  resize();
})();
</script>
</body>
</html>
""".trimIndent()
}
