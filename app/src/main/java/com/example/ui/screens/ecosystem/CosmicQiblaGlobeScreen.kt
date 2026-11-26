package com.example.ui.screens.ecosystem

import android.graphics.BitmapFactory
import android.util.LruCache
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.astronomy.CosmicEarthGlobeEngine
import com.example.domain.astronomy.GeodesicPoint
import com.example.domain.astronomy.GlobeCityBeacon
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

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
        SacredSatelliteLandmark("kaaba", "الكعبة المشرفة والمسجد الحرام", "The Holy Kaaba & Masjid al-Haram", "مكة المكرمة", "Makkah", 21.422487, 39.826206, 17.6, "قبلة المسلمين وأول بيت وُضع للناس — قرّب لترى صحن المطاف والكعبة من القمر الصناعي", "Qibla of all Muslims — zoom in to inspect the Mataf courtyard & Kaaba from satellite"),
        SacredSatelliteLandmark("nabawi", "المسجد النبوي الشريف والروضة", "Al-Masjid an-Nabawi", "المدينة المنورة", "Madinah", 24.467213, 39.611160, 17.2, "مسجد رسول الله ﷺ والقبة الخضراء وساحات المظلات الشهيرة", "Mosque of Prophet Muhammad ﷺ, the Green Dome & shaded plazas"),
        SacredSatelliteLandmark("aqsa", "المسجد الأقصى وقبة الصخرة", "Al-Aqsa Mosque & Dome of the Rock", "القدس الشريف", "Jerusalem", 31.778019, 35.235398, 17.4, "أولى القبلتين ومسرى النبي ﷺ وثالث الحرمين الشريفين", "First Qibla, Night Journey sanctuary & third holiest mosque"),
        SacredSatelliteLandmark("quba", "مسجد قباء", "Quba Mosque", "المدينة المنورة", "Madinah", 24.439282, 39.617292, 17.3, "أول مسجد أُسس على التقوى — الصلاة فيه تعدل أجر عمرة", "First mosque founded in Islam — prayer inside equals an Umrah"),
        SacredSatelliteLandmark("hira", "جبل النور وغار حراء", "Jabal al-Nour & Cave of Hira", "مكة المكرمة", "Makkah", 21.457500, 39.859300, 16.0, "الموضع الذي نزل فيه أول الوحي: ﴿اقْرَأْ بِاسْمِ رَبِّكَ﴾", "Mountain peak where the very first revelation of the Quran descended"),
        SacredSatelliteLandmark("arafat", "جبل الرحمة وصعيد عرفات", "Mount Arafat (Jabal al-Rahmah)", "مكة المكرمة", "Makkah", 21.354900, 39.984100, 15.8, "موقف الحج الأعظم وخطبة الوداع الخالدة", "Heart of Hajj pilgrimage and the Farewell Sermon"),
        SacredSatelliteLandmark("umayyad", "الجامع الأموي الكبير", "Umayyad Mosque", "دمشق", "Damascus", 33.511944, 36.306667, 17.3, "من أعرق وأجمل مساجد العالم الإسلامي تاريخاً وعمارة", "One of the oldest and grandest historic mosques in Islam"),
        SacredSatelliteLandmark("azhar", "الجامع الأزهر الشريف", "Al-Azhar Mosque", "القاهرة", "Cairo", 30.045700, 31.262700, 17.3, "منارة العلم الشرعي واللغة العربية لأكثر من ألف عام", "Beacon of Islamic scholarship for over a thousand years"),
        SacredSatelliteLandmark("qarawiyyin", "جامع وجامعة القرويين", "Al-Qarawiyyin Mosque", "فاس", "Fez", 34.064400, -4.973600, 17.4, "أسستها فاطمة الفهرية عام ٢٤٥ هـ — أقدم جامعة مستمرة في العالم", "Founded by Fatima al-Fihri in 859 CE — oldest continuous university"),
        SacredSatelliteLandmark("ayasofya", "مسجد آيا صوفيا الكبير", "Hagia Sophia Grand Mosque", "إسطنبول", "Istanbul", 41.008583, 28.980175, 17.2, "معلم إسلامي وتاريخي عظيم في قلب إسطنبول", "Iconic grand mosque and architectural landmark in Istanbul")
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

private enum class SatelliteTileLayer(
    val id: String,
    val labelAr: String,
    val labelEn: String
) {
    GOOGLE_HYBRID("ghybrid", "قمر صناعي + أسماء", "Satellite Hybrid"),
    ESRI_IMAGERY("esri", "قمر صناعي صافي", "Clean Satellite"),
    OSM_STREETS("osm", "خريطة شوارع", "Street Map")
}

/**
 * 100% Native Android Satellite Tile Engine (HttpURLConnection + BitmapFactory + LRU Cache).
 * Zero WebView, zero CORS rejection, zero blank blue screens, instant multi-provider fallback!
 */
private object NativeSatelliteTileFetcher {
    private val memoryCache = object : LruCache<String, ImageBitmap>(160) {}
    private val fetchSemaphore = Semaphore(8)

    fun getCached(key: String): ImageBitmap? = synchronized(memoryCache) { memoryCache.get(key) }

    fun candidateUrls(layer: SatelliteTileLayer, z: Int, x: Int, y: Int): List<String> {
        val sub = ((x + y) and 3)
        return when (layer) {
            SatelliteTileLayer.GOOGLE_HYBRID -> listOf(
                "https://mt$sub.google.com/vt/lyrs=y&hl=ar&x=$x&y=$y&z=$z",
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x",
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            )
            SatelliteTileLayer.ESRI_IMAGERY -> listOf(
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x",
                "https://mt$sub.google.com/vt/lyrs=s&x=$x&y=$y&z=$z",
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            )
            SatelliteTileLayer.OSM_STREETS -> listOf(
                "https://tile.openstreetmap.org/$z/$x/$y.png",
                "https://mt$sub.google.com/vt/lyrs=m&hl=ar&x=$x&y=$y&z=$z"
            )
        }
    }

    suspend fun fetchTile(layer: SatelliteTileLayer, z: Int, x: Int, y: Int): ImageBitmap? {
        val key = "${layer.id}/$z/$x/$y"
        getCached(key)?.let { return it }
        return fetchSemaphore.withPermit {
            getCached(key)?.let { return@withPermit it }
            withContext(Dispatchers.IO) {
                for (urlStr in candidateUrls(layer, z, x, y)) {
                    val bmp = downloadBitmap(urlStr)
                    if (bmp != null) {
                        val img = bmp.asImageBitmap()
                        synchronized(memoryCache) { memoryCache.put(key, img) }
                        return@withContext img
                    }
                }
                null
            }
        }
    }

    private fun downloadBitmap(urlStr: String): android.graphics.Bitmap? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NoorIslamicApp/3.3 (Android; Satellite Atlas)")
                setRequestProperty("Accept", "image/webp,image/png,image/jpeg,image/*;q=0.8")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { stream -> BitmapFactory.decodeStream(stream) }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

private object WebMercatorMath {
    private const val TILE_SIZE = 256.0

    fun lonToWorldX(lon: Double, zoom: Double): Double =
        ((lon + 180.0) / 360.0) * TILE_SIZE * 2.0.pow(zoom)

    fun latToWorldY(lat: Double, zoom: Double): Double {
        val clamped = lat.coerceIn(-85.05112878, 85.05112878)
        val rad = Math.toRadians(clamped)
        return ((1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0) * TILE_SIZE * 2.0.pow(zoom)
    }

    fun worldXToLon(worldX: Double, zoom: Double): Double {
        val n = TILE_SIZE * 2.0.pow(zoom)
        var lon = (worldX / n) * 360.0 - 180.0
        while (lon > 180.0) lon -= 360.0
        while (lon < -180.0) lon += 360.0
        return lon
    }

    fun worldYToLat(worldY: Double, zoom: Double): Double {
        val n = TILE_SIZE * 2.0.pow(zoom)
        val yNorm = (worldY / n).coerceIn(0.0, 1.0)
        val rad = atan(sinh(PI * (1.0 - 2.0 * yNorm)))
        return Math.toDegrees(rad).coerceIn(-85.0511, 85.0511)
    }

    fun tileXY(lat: Double, lon: Double, z: Int): Pair<Int, Int> {
        val n = 1 shl z
        val x = floor(((lon + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
        val rad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val y = floor((1.0 - asinh(tan(rad)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
        return x to y
    }
}

/**
 * Simplified real-world continent coastlines (lat, lon) for the live mathematical 3D rotating Earth sphere.
 */
private object WorldContinentsGeometry {
    val polygons: List<List<Pair<Double, Double>>> = listOf(
        // Africa
        listOf(
            35.8 to -5.5, 37.0 to 10.2, 32.8 to 13.2, 31.2 to 29.9, 30.0 to 32.5,
            22.0 to 36.8, 12.6 to 43.3, 11.8 to 51.3, 2.0 to 45.3, -10.5 to 40.5,
            -25.9 to 32.6, -34.4 to 18.5, -22.0 to 14.3, -6.0 to 12.2, 4.0 to 9.5,
            5.0 to -4.0, 14.7 to -17.4, 21.0 to -17.0, 30.4 to -9.6, 35.8 to -5.5
        ),
        // Arabian Peninsula + Middle East + Asia
        listOf(
            30.0 to 32.5, 29.5 to 35.0, 21.4 to 39.2, 12.8 to 43.5, 14.5 to 49.5,
            22.5 to 59.8, 26.2 to 56.2, 29.9 to 48.0, 27.0 to 56.5, 25.0 to 67.0,
            20.0 to 72.8, 8.1 to 77.5, 16.0 to 81.0, 22.0 to 89.0, 16.0 to 97.5,
            1.3 to 103.8, 10.5 to 107.0, 21.0 to 108.0, 31.2 to 121.5, 39.9 to 119.5,
            35.0 to 129.0, 43.0 to 135.0, 55.0 to 138.0, 66.0 to 170.0, 72.0 to 130.0,
            73.0 to 80.0, 68.0 to 60.0, 55.0 to 58.0, 42.0 to 48.0, 41.0 to 29.0,
            36.8 to 27.5, 36.0 to 36.0, 31.5 to 34.5, 30.0 to 32.5
        ),
        // Europe
        listOf(
            36.1 to -5.4, 38.7 to -9.1, 43.4 to -8.4, 43.4 to -1.8, 48.4 to -4.5,
            51.0 to 2.0, 53.5 to 8.5, 57.5 to 10.5, 63.0 to 10.0, 70.5 to 25.0,
            68.0 to 42.0, 60.0 to 55.0, 47.0 to 40.0, 41.0 to 29.0, 37.9 to 23.7,
            40.5 to 18.0, 44.5 to 12.5, 43.5 to 7.0, 41.3 to 2.2, 36.1 to -5.4
        ),
        // North America
        listOf(
            70.0 to -162.0, 60.0 to -165.0, 58.0 to -136.0, 48.5 to -124.5, 34.0 to -118.5,
            23.0 to -109.5, 16.0 to -95.0, 9.0 to -79.5, 18.5 to -88.0, 21.5 to -87.0,
            26.0 to -97.0, 29.5 to -90.0, 25.2 to -80.3, 35.0 to -75.5, 44.5 to -66.5,
            52.0 to -56.0, 62.0 to -75.0, 69.0 to -95.0, 71.0 to -130.0, 70.0 to -162.0
        ),
        // South America
        listOf(
            10.5 to -75.5, 11.0 to -63.0, 5.0 to -52.5, -1.5 to -48.5, -6.0 to -35.0,
            -23.0 to -43.0, -34.5 to -58.0, -54.5 to -68.0, -45.0 to -75.0, -18.0 to -71.0,
            -5.0 to -81.0, 5.0 to -77.5, 10.5 to -75.5
        ),
        // Australia
        listOf(
            -12.5 to 130.8, -11.5 to 142.5, -20.0 to 148.5, -33.9 to 151.2,
            -38.0 to 145.0, -35.0 to 136.0, -32.0 to 115.8, -22.0 to 114.0, -12.5 to 130.8
        ),
        // Indonesia / Maritime SE Asia
        listOf(
            5.5 to 95.3, -6.0 to 105.8, -8.5 to 116.0, -7.0 to 112.0, -3.5 to 106.0, 2.0 to 99.5, 5.5 to 95.3
        ),
        // Greenland
        listOf(
            60.0 to -44.0, 68.0 to -53.0, 78.0 to -68.0, 82.0 to -35.0, 72.0 to -22.0, 60.0 to -44.0
        )
    )
}

/**
 * REAL Native Blue Satellite Earth + Real Interactive 3D Rotating Spherical Globe.
 * - Mode 0: 100% Native Jetpack Compose Real Satellite Tile Engine (Google Satellite Hybrid / Esri World Imagery / OSM)
 *           with direct Android HttpURLConnection + BitmapFactory (ZERO WebView CORS blank screens!),
 *           Zoom 2 → 18.5, pinch/drag, golden geodesic Qibla arc, Kaaba & User markers, and 10 Sacred Landmarks!
 * - Mode 1: 100% Real Interactive 3D Spherical Globe rendered live with CosmicEarthGlobeEngine
 *           (live Day/Night terminator, real continent geometry, 360° drag rotation, golden geodesic Qibla arc — ZERO static photos!).
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

    var viewMode by remember { mutableIntStateOf(0) } // 0 = Native Real Satellite Earth, 1 = Real 3D Interactive Sphere
    var activeLayer by remember { mutableStateOf(SatelliteTileLayer.GOOGLE_HYBRID) }
    var centerLat by remember { mutableDoubleStateOf(CosmicEarthGlobeEngine.KAABA_LAT) }
    var centerLon by remember { mutableDoubleStateOf(CosmicEarthGlobeEngine.KAABA_LON) }
    var zoomLevel by remember { mutableDoubleStateOf(3.2) }
    var activeFocusLabel by remember { mutableStateOf(L10n.pick(lang, "القمر الصناعي الحقيقي المباشر", "Live Native Satellite Earth")) }
    var retryNonce by remember { mutableIntStateOf(0) }

    val qiblaBearing = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.qiblaBearingDegrees(userLatitude, userLongitude)
    }
    val kaabaDistKm = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.distanceToKaabaKm(userLatitude, userLongitude)
    }
    val geodesicArc = remember(userLatitude, userLongitude) {
        CosmicEarthGlobeEngine.computeGeodesicToKaaba(userLatitude, userLongitude, steps = 48)
    }

    fun flyTo(lat: Double, lon: Double, zoom: Double, label: String) {
        centerLat = lat.coerceIn(-82.0, 82.0)
        centerLon = lon.coerceIn(-180.0, 180.0)
        zoomLevel = zoom.coerceIn(2.0, 18.5)
        activeFocusLabel = label
        viewMode = 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cosmic_qibla_globe_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "الكرة الأرضية ٣D والقمر الصناعي الحقيقي", "Real 3D Earth & Satellite Atlas"),
            subtitle = L10n.pick(lang, "قمر صناعي مباشر حقيقي + كرة أرضية ٣D تفاعلية (بدون صور ثابتة)", "Native Real Satellite Tiles + Interactive 3D Sphere"),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode Switcher (Native Live Satellite vs Interactive Real 3D Mathematical Globe)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "🛰️ قمر صناعي حقيقي مباشر", "🛰️ Live Real Satellite"),
                        selected = viewMode == 0,
                        onClick = { viewMode = 0 },
                        modifier = Modifier.weight(1f).testTag("globe_mode_satellite")
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "🌍 كرة أرضية ٣D حقيقية", "🌍 Interactive 3D Globe"),
                        selected = viewMode == 1,
                        onClick = { viewMode = 1 },
                        modifier = Modifier.weight(1f).testTag("globe_mode_3d_sphere")
                    )
                }
            }

            // Main Interactive Viewport
            item {
                LiquidGlassCard(contentPadding = PaddingValues(10.dp)) {
                    if (viewMode == 0) {
                        NativeRealSatelliteViewport(
                            centerLat = centerLat,
                            centerLon = centerLon,
                            zoomLevel = zoomLevel,
                            layer = activeLayer,
                            userLat = userLatitude,
                            userLon = userLongitude,
                            geodesicArc = geodesicArc,
                            focusLabel = activeFocusLabel,
                            retryNonce = retryNonce,
                            onViewportChange = { newLat, newLon, newZoom ->
                                centerLat = newLat
                                centerLon = newLon
                                zoomLevel = newZoom
                            },
                            onCycleLayer = {
                                val layers = SatelliteTileLayer.entries
                                activeLayer = layers[(layers.indexOf(activeLayer) + 1) % layers.size]
                            },
                            onRetry = { retryNonce++ },
                            onZoomIn = { zoomLevel = (zoomLevel + 1.2).coerceAtMost(18.5) },
                            onZoomOut = { zoomLevel = (zoomLevel - 1.2).coerceAtLeast(2.0) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Fly-To Pills
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                QuickFlyPill(
                                    label = L10n.pick(lang, "🕋 الكعبة (تقريب فائق)", "🕋 Kaaba (Ultra Zoom)"),
                                    highlight = true,
                                    onClick = {
                                        flyTo(
                                            CosmicEarthGlobeEngine.KAABA_LAT,
                                            CosmicEarthGlobeEngine.KAABA_LON,
                                            17.6,
                                            L10n.pick(lang, "الكعبة المشرفة — قمر صناعي", "Holy Kaaba — Satellite")
                                        )
                                    }
                                )
                            }
                            item {
                                QuickFlyPill(
                                    label = L10n.pick(lang, "🕌 المسجد النبوي", "🕌 Al-Nabawi"),
                                    highlight = false,
                                    onClick = {
                                        flyTo(24.467213, 39.611160, 17.2, L10n.pick(lang, "المسجد النبوي الشريف", "Al-Masjid an-Nabawi"))
                                    }
                                )
                            }
                            item {
                                QuickFlyPill(
                                    label = L10n.pick(lang, "🕌 المسجد الأقصى", "🕌 Al-Aqsa"),
                                    highlight = false,
                                    onClick = {
                                        flyTo(31.778019, 35.235398, 17.4, L10n.pick(lang, "المسجد الأقصى المبارك", "Al-Aqsa Mosque"))
                                    }
                                )
                            }
                            item {
                                QuickFlyPill(
                                    label = L10n.pick(lang, "📍 موقعي الآن", "📍 My Location"),
                                    highlight = false,
                                    onClick = {
                                        flyTo(userLatitude, userLongitude, 14.0, userCityLabel.ifBlank { L10n.pick(lang, "موقعي الحالي", "My Location") })
                                    }
                                )
                            }
                            item {
                                QuickFlyPill(
                                    label = L10n.pick(lang, "🌍 رؤية الأرض كاملة", "🌍 Whole Earth"),
                                    highlight = false,
                                    onClick = {
                                        flyTo(23.0, 38.0, 2.6, L10n.pick(lang, "العالم الإسلامي من الفضاء", "Earth from Space"))
                                    }
                                )
                            }
                        }
                    } else {
                        InteractiveReal3DGlobeViewport(
                            userLatitude = userLatitude,
                            userLongitude = userLongitude,
                            userCityLabel = userCityLabel,
                            geodesicArc = geodesicArc
                        )
                    }
                }
            }

            // Real Qibla Geodesic Telemetry Card
            item {
                LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "الخط الذهبي الجيوديسي نحو الكعبة المشرفة", "Golden Geodesic Arc to the Kaaba"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldBright
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "من $userCityLabel إلى المسجد الحرام · زاوية القبلة ${String.format(Locale.US, "%.1f°", qiblaBearing)}",
                                    "From $userCityLabel to Masjid al-Haram · Bearing ${String.format(Locale.US, "%.1f°", qiblaBearing)}"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%,.0f", kaabaDistKm),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = L10n.pick(lang, "كيلومتر", "km"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 10 Sacred Places & Historic Mosques Satellite Fly-To Atlas
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Rounded.FlightTakeoff, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "أطلس الأماكن المقدسة بالتقريب الفضائي المباشر (١٠ معالم)", "Sacred Landmarks Live Satellite Fly-To (10 Sites)"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
            }

            items(SacredSatelliteAtlasCatalog.landmarks, key = { it.id }) { place ->
                val distFromUser = remember(userLatitude, userLongitude, place.id) {
                    SacredSatelliteAtlasCatalog.distanceKm(userLatitude, userLongitude, place.lat, place.lon)
                }
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            flyTo(
                                place.lat,
                                place.lon,
                                place.zoom,
                                L10n.pick(lang, place.nameAr, place.nameEn)
                            )
                        }
                        .testTag("satellite_landmark_${place.id}"),
                    contentPadding = PaddingValues(14.dp)
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
                                .background(
                                    Brush.radialGradient(
                                        listOf(GoldBright.copy(alpha = 0.28f), EmeraldPrimary.copy(alpha = 0.20f))
                                    )
                                )
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Mosque, contentDescription = null, tint = GoldBright, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, place.nameAr, place.nameEn),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "${place.cityAr} · يبعد ${String.format(Locale.US, "%,.0f", distFromUser)} كم",
                                    "${place.cityEn} · ${String.format(Locale.US, "%,.0f", distFromUser)} km away"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = L10n.pick(lang, place.noteAr, place.noteEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.2f))
                                .border(1.dp, GoldBright.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = L10n.pick(lang, "تقريب", "Zoom"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldBright
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeRealSatelliteViewport(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Double,
    layer: SatelliteTileLayer,
    userLat: Double,
    userLon: Double,
    geodesicArc: List<GeodesicPoint>,
    focusLabel: String,
    retryNonce: Int,
    onViewportChange: (Double, Double, Double) -> Unit,
    onCycleLayer: () -> Unit,
    onRetry: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var canvasSize by remember { mutableStateOf(IntSize(1000, 1000)) }
    val loadedTiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val inFlightKeys = remember { mutableSetOf<String>() }
    var loadedCount by remember { mutableIntStateOf(0) }
    var failedCount by remember { mutableIntStateOf(0) }

    val intZoom = floor(zoomLevel).toInt().coerceIn(2, 18)
    val scaleFactor = 2.0.pow(zoomLevel - intZoom)
    val renderedTilePx = 256.0 * scaleFactor
    val maxTiles = 1 shl intZoom

    val centerWorldX = WebMercatorMath.lonToWorldX(centerLon, zoomLevel)
    val centerWorldY = WebMercatorMath.latToWorldY(centerLat, zoomLevel)

    val halfW = (canvasSize.width / 2.0).coerceAtLeast(150.0)
    val halfH = (canvasSize.height / 2.0).coerceAtLeast(150.0)
    val minTileX = floor((centerWorldX - halfW) / renderedTilePx).toInt()
    val maxTileX = ceil((centerWorldX + halfW) / renderedTilePx).toInt()
    val minTileY = floor((centerWorldY - halfH) / renderedTilePx).toInt().coerceIn(0, maxTiles - 1)
    val maxTileY = ceil((centerWorldY + halfH) / renderedTilePx).toInt().coerceIn(0, maxTiles - 1)

    // Preload low-zoom base tiles + all currently visible tiles natively on Dispatchers.IO (ZERO CORS!)
    LaunchedEffect(layer, intZoom, minTileX, maxTileX, minTileY, maxTileY, retryNonce) {
        val toFetch = mutableListOf<Triple<Int, Int, Int>>()
        // Ensure a few parent tiles at z-2 exist so zooming never shows empty ocean
        val baseZ = (intZoom - 2).coerceAtLeast(2)
        if (baseZ < intZoom) {
            val (bx, by) = WebMercatorMath.tileXY(centerLat, centerLon, baseZ)
            val bMax = 1 shl baseZ
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val tx = ((bx + dx) % bMax + bMax) % bMax
                    val ty = (by + dy).coerceIn(0, bMax - 1)
                    toFetch += Triple(baseZ, tx, ty)
                }
            }
        }
        for (ty in minTileY..maxTileY) {
            for (txRaw in minTileX..maxTileX) {
                val tx = ((txRaw % maxTiles) + maxTiles) % maxTiles
                toFetch += Triple(intZoom, tx, ty)
            }
        }

        toFetch.distinct().forEach { (z, tx, ty) ->
            val key = "${layer.id}/$z/$tx/$ty"
            val cached = NativeSatelliteTileFetcher.getCached(key)
            if (cached != null) {
                if (loadedTiles[key] == null) {
                    loadedTiles[key] = cached
                    loadedCount++
                }
            } else if (inFlightKeys.add(key)) {
                scope.launch {
                    try {
                        val img = NativeSatelliteTileFetcher.fetchTile(layer, z, tx, ty)
                        if (img != null) {
                            loadedTiles[key] = img
                            loadedCount++
                            if (loadedTiles.size > 140) {
                                val oldest = loadedTiles.keys.firstOrNull { !it.startsWith("${layer.id}/$intZoom/") }
                                if (oldest != null) loadedTiles.remove(oldest)
                            }
                        } else {
                            failedCount++
                        }
                    } finally {
                        inFlightKeys.remove(key)
                    }
                }
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "sat_pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 8f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "beacon_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF041529))
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .onSizeChanged { canvasSize = it }
            .pointerInput(centerLat, centerLon, zoomLevel) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newZoom = (zoomLevel + ln(zoom.toDouble().coerceIn(0.5, 2.0)) / ln(2.0)).coerceIn(2.0, 18.5)
                    val wx = WebMercatorMath.lonToWorldX(centerLon, newZoom) - pan.x
                    val wy = WebMercatorMath.latToWorldY(centerLat, newZoom) - pan.y
                    val newLon = WebMercatorMath.worldXToLon(wx, newZoom)
                    val newLat = WebMercatorMath.worldYToLat(wy, newZoom)
                    onViewportChange(newLat, newLon, newZoom)
                }
            }
            .testTag("native_satellite_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2.0
            val cy = h / 2.0

            // Deep space ocean backdrop
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0A2E52), Color(0xFF041428)),
                    center = Offset(w / 2f, h / 2f),
                    radius = w.coerceAtLeast(h)
                )
            )

            // Draw real downloaded satellite tiles with smooth parent-tile fallback while zooming
            for (ty in minTileY..maxTileY) {
                for (txRaw in minTileX..maxTileX) {
                    val tx = ((txRaw % maxTiles) + maxTiles) % maxTiles
                    val dstLeft = (cx + (txRaw * renderedTilePx - centerWorldX)).roundToInt()
                    val dstTop = (cy + (ty * renderedTilePx - centerWorldY)).roundToInt()
                    val dstSize = ceil(renderedTilePx).toInt() + 1

                    val key = "${layer.id}/$intZoom/$tx/$ty"
                    val tileBmp = loadedTiles[key] ?: NativeSatelliteTileFetcher.getCached(key)
                    if (tileBmp != null) {
                        drawImage(
                            image = tileBmp,
                            dstOffset = IntOffset(dstLeft, dstTop),
                            dstSize = IntSize(dstSize, dstSize)
                        )
                    } else {
                        // Search up to 3 parent zoom levels so zooming never flashes blank!
                        var parentDrawn = false
                        for (dz in 1..3) {
                            val pz = intZoom - dz
                            if (pz < 2) break
                            val shift = 1 shl dz
                            val px = tx / shift
                            val py = ty / shift
                            val pKey = "${layer.id}/$pz/$px/$py"
                            val pBmp = loadedTiles[pKey] ?: NativeSatelliteTileFetcher.getCached(pKey)
                            if (pBmp != null) {
                                val subW = pBmp.width / shift
                                val subH = pBmp.height / shift
                                val srcX = (tx % shift) * subW
                                val srcY = (ty % shift) * subH
                                drawImage(
                                    image = pBmp,
                                    srcOffset = IntOffset(srcX, srcY),
                                    srcSize = IntSize(subW.coerceAtLeast(1), subH.coerceAtLeast(1)),
                                    dstOffset = IntOffset(dstLeft, dstTop),
                                    dstSize = IntSize(dstSize, dstSize)
                                )
                                parentDrawn = true
                                break
                            }
                        }
                        if (!parentDrawn) {
                            drawRect(
                                color = Color(0xFF0C355C).copy(alpha = 0.25f),
                                topLeft = Offset(dstLeft.toFloat(), dstTop.toFloat()),
                                size = androidx.compose.ui.geometry.Size(dstSize.toFloat(), dstSize.toFloat()),
                                style = Stroke(width = 0.8f)
                            )
                        }
                    }
                }
            }

            // Helper to project lat/lon onto screen with wraparound handling
            fun latLonToScreen(lat: Double, lon: Double): Offset {
                val wx = WebMercatorMath.lonToWorldX(lon, zoomLevel)
                val wy = WebMercatorMath.latToWorldY(lat, zoomLevel)
                val worldWidth = 256.0 * 2.0.pow(zoomLevel)
                var dx = wx - centerWorldX
                if (dx > worldWidth / 2.0) dx -= worldWidth
                if (dx < -worldWidth / 2.0) dx += worldWidth
                return Offset((cx + dx).toFloat(), (cy + (wy - centerWorldY)).toFloat())
            }

            // Golden Geodesic Qibla Arc from User to Kaaba
            if (geodesicArc.size >= 2) {
                val path = Path()
                geodesicArc.forEachIndexed { idx, pt ->
                    val off = latLonToScreen(pt.latDeg, pt.lonDeg)
                    if (idx == 0) path.moveTo(off.x, off.y) else path.lineTo(off.x, off.y)
                }
                drawPath(
                    path = path,
                    color = GoldBright.copy(alpha = 0.35f),
                    style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = GoldBright,
                    style = Stroke(
                        width = 3.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                    )
                )
            }

            // User Location Beacon (Cyan / Emerald)
            val userOff = latLonToScreen(userLat, userLon)
            if (userOff.x in -60f..(w + 60f) && userOff.y in -60f..(h + 60f)) {
                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.28f), radius = pulseRadius, center = userOff)
                drawCircle(Color(0xFF38BDF8), radius = 7.5f, center = userOff)
                drawCircle(Color.White, radius = 3.5f, center = userOff)
            }

            // Holy Kaaba Beacon (Glowing Gold & Sacred Black Cube)
            val kaabaOff = latLonToScreen(CosmicEarthGlobeEngine.KAABA_LAT, CosmicEarthGlobeEngine.KAABA_LON)
            if (kaabaOff.x in -60f..(w + 60f) && kaabaOff.y in -60f..(h + 60f)) {
                drawCircle(GoldBright.copy(alpha = 0.32f), radius = pulseRadius * 1.15f, center = kaabaOff)
                drawCircle(GoldBright, radius = 10f, center = kaabaOff)
                drawRect(
                    color = Color(0xFF0A0A0A),
                    topLeft = Offset(kaabaOff.x - 5.5f, kaabaOff.y - 5.5f),
                    size = androidx.compose.ui.geometry.Size(11f, 11f)
                )
                drawLine(
                    color = GoldBright,
                    start = Offset(kaabaOff.x - 5.5f, kaabaOff.y - 1.5f),
                    end = Offset(kaabaOff.x + 5.5f, kaabaOff.y - 1.5f),
                    strokeWidth = 2f
                )
            }
        }

        // Top HUD: Active focus + Live Zoom + Tile Layer Toggle
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC041529))
                    .border(1.dp, GoldBright.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.SatelliteAlt, contentDescription = null, tint = GoldBright, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$focusLabel · Z${String.format(Locale.US, "%.1f", zoomLevel)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC041529))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onCycleLayer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("satellite_cycle_layer_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Layers, contentDescription = null, tint = GoldBright, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = L10n.pick(lang, layer.labelAr, layer.labelEn),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright
                )
            }
        }

        // Right-side Zoom + / - / Refresh Buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SatelliteRoundControl(icon = Icons.Rounded.Add, contentDescription = "Zoom In", onClick = onZoomIn)
            SatelliteRoundControl(icon = Icons.Rounded.Remove, contentDescription = "Zoom Out", onClick = onZoomOut)
            SatelliteRoundControl(icon = Icons.Rounded.Refresh, contentDescription = "Reload Tiles", onClick = onRetry)
        }

        // Bottom live coordinates + streaming indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xC8041529))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.4f°, %.4f°", centerLat, centerLon),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF93C5FD)
                )
            }
            if (loadedCount == 0 && failedCount == 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xC8041529))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp, color = GoldBright)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = L10n.pick(lang, "جاري بث صور القمر الصناعي...", "Streaming Satellite Tiles..."),
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldBright
                    )
                }
            } else if (loadedCount == 0 && failedCount > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC5A1212))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.pick(lang, "لا يوجد إنترنت · اضغط لإعادة المحاولة", "Offline · Tap to Retry"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xC8041529))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = L10n.pick(lang, "اسحب للتحريك · قرّب بإصبعين", "Drag to Pan · Pinch to Zoom"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }
    }
}

/**
 * 100% Real Interactive 3D Rotating Spherical Earth (NO static photo!).
 * Uses CosmicEarthGlobeEngine orthographic 3D projection, real continent polygons,
 * real-time solar Day/Night terminator, golden geodesic Qibla arc, and live Islamic city beacons.
 */
@Composable
private fun InteractiveReal3DGlobeViewport(
    userLatitude: Double,
    userLongitude: Double,
    userCityLabel: String,
    geodesicArc: List<GeodesicPoint>
) {
    val lang = LocalAppLanguage.current
    var camLat by remember { mutableDoubleStateOf(22.0) }
    var camLon by remember { mutableDoubleStateOf(39.8) }
    var autoRotate by remember { mutableStateOf(true) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(autoRotate) {
        while (isActive) {
            delay(45L)
            if (autoRotate) {
                camLon = ((camLon + 0.35 + 180.0) % 360.0) - 180.0
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    val subsolar = remember(nowMillis) { CosmicEarthGlobeEngine.computeSubsolarPoint(nowMillis) }
    val beacons: List<GlobeCityBeacon> = remember(nowMillis) { CosmicEarthGlobeEngine.evaluateGlobalBeacons(nowMillis) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF0A2447), Color(0xFF030B1A), Color(0xFF01040C))
                )
            )
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { autoRotate = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        camLon = ((camLon - dragAmount.x * 0.38 + 540.0) % 360.0) - 180.0
                        camLat = (camLat + dragAmount.y * 0.35).coerceIn(-75.0, 75.0)
                    }
                )
            }
            .testTag("interactive_3d_globe_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (size.minDimension * 0.41f)

            // Outer blue atmospheric halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x5538BDF8), Color(0x221D4ED8), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius * 1.24f
                ),
                radius = radius * 1.24f,
                center = Offset(cx, cy)
            )

            // Deep Realistic Ocean Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1D6FB8), Color(0xFF0E417A), Color(0xFF051C3D)),
                    center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
                    radius = radius * 1.35f
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            // Latitude & Longitude Graticule (3D projected)
            for (lat in -60..60 step 30) {
                val p = Path()
                var started = false
                for (lon in -180..180 step 6) {
                    val proj = CosmicEarthGlobeEngine.projectOrthographic(lat.toDouble(), lon.toDouble(), camLat, camLon)
                    if (proj.isVisible) {
                        val sx = cx + proj.x * radius
                        val sy = cy + proj.y * radius
                        if (!started) {
                            p.moveTo(sx, sy)
                            started = true
                        } else {
                            p.lineTo(sx, sy)
                        }
                    } else {
                        started = false
                    }
                }
                drawPath(p, Color(0xFF93C5FD).copy(alpha = 0.16f), style = Stroke(width = 1f))
            }
            for (lon in -150..180 step 30) {
                val p = Path()
                var started = false
                for (lat in -84..84 step 6) {
                    val proj = CosmicEarthGlobeEngine.projectOrthographic(lat.toDouble(), lon.toDouble(), camLat, camLon)
                    if (proj.isVisible) {
                        val sx = cx + proj.x * radius
                        val sy = cy + proj.y * radius
                        if (!started) {
                            p.moveTo(sx, sy)
                            started = true
                        } else {
                            p.lineTo(sx, sy)
                        }
                    } else {
                        started = false
                    }
                }
                drawPath(p, Color(0xFF93C5FD).copy(alpha = 0.16f), style = Stroke(width = 1f))
            }

            // Real 3D Projected Continents (Emerald/Earth Terrain)
            WorldContinentsGeometry.polygons.forEach { poly ->
                val p = Path()
                var started = false
                var visibleCount = 0
                poly.forEach { (lat, lon) ->
                    val proj = CosmicEarthGlobeEngine.projectOrthographic(lat, lon, camLat, camLon)
                    if (proj.z >= -0.05f) {
                        visibleCount++
                        val sx = cx + proj.x * radius
                        val sy = cy + proj.y * radius
                        if (!started) {
                            p.moveTo(sx, sy)
                            started = true
                        } else {
                            p.lineTo(sx, sy)
                        }
                    } else {
                        started = false
                    }
                }
                if (visibleCount >= 3) {
                    drawPath(p, Color(0xFF1F7A5A).copy(alpha = 0.72f))
                    drawPath(p, Color(0xFF6EE7B7).copy(alpha = 0.85f), style = Stroke(width = 1.6f))
                }
            }

            // Live Astronomical Night-Side Shadow Dots (Real Solar Terminator)
            for (lat in -75..75 step 10) {
                for (lon in -175..175 step 10) {
                    if (!CosmicEarthGlobeEngine.isPointInDaylight(lat.toDouble(), lon.toDouble(), subsolar)) {
                        val proj = CosmicEarthGlobeEngine.projectOrthographic(lat.toDouble(), lon.toDouble(), camLat, camLon)
                        if (proj.isVisible) {
                            drawCircle(
                                color = Color(0x4A020817),
                                radius = radius * 0.085f,
                                center = Offset(cx + proj.x * radius, cy + proj.y * radius)
                            )
                        }
                    }
                }
            }

            // Golden Geodesic Qibla Arc on 3D Sphere
            val arcPath = Path()
            var arcStarted = false
            geodesicArc.forEach { pt ->
                val proj = CosmicEarthGlobeEngine.projectOrthographic(pt.latDeg, pt.lonDeg, camLat, camLon)
                if (proj.isVisible) {
                    val sx = cx + proj.x * radius
                    val sy = cy + proj.y * radius
                    if (!arcStarted) {
                        arcPath.moveTo(sx, sy)
                        arcStarted = true
                    } else {
                        arcPath.lineTo(sx, sy)
                    }
                } else {
                    arcStarted = false
                }
            }
            drawPath(arcPath, GoldBright.copy(alpha = 0.38f), style = Stroke(width = 8f, cap = StrokeCap.Round))
            drawPath(arcPath, GoldBright, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Global Islamic Cities & Adhan Beacons
            beacons.forEach { city ->
                val proj = CosmicEarthGlobeEngine.projectOrthographic(city.lat, city.lon, camLat, camLon)
                if (proj.isVisible) {
                    val off = Offset(cx + proj.x * radius, cy + proj.y * radius)
                    val dotColor = when {
                        city.id == "makkah" -> GoldBright
                        city.isAdhanActiveNow -> Color(0xFF34D399)
                        city.isDaylight -> Color(0xFFFDE68A)
                        else -> Color(0xFF60A5FA)
                    }
                    drawCircle(dotColor.copy(alpha = 0.35f), radius = if (city.id == "makkah") 14f else 8f, center = off)
                    drawCircle(dotColor, radius = if (city.id == "makkah") 6.5f else 3.8f, center = off)
                }
            }

            // User City Beacon
            val uProj = CosmicEarthGlobeEngine.projectOrthographic(userLatitude, userLongitude, camLat, camLon)
            if (uProj.isVisible) {
                val uOff = Offset(cx + uProj.x * radius, cy + uProj.y * radius)
                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.4f), radius = 12f, center = uOff)
                drawCircle(Color.White, radius = 4.5f, center = uOff)
            }

            // Crisp Atmospheric Limb Border
            drawCircle(
                color = Color(0xFF60A5FA).copy(alpha = 0.72f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.2f)
            )
        }

        // Top HUD
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC041529))
                    .border(1.dp, GoldBright.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Public, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = L10n.pick(lang, "كرة أرضية ٣D حية بالليل والنهار", "Live 3D Earth · Real Day/Night"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SatelliteRoundControl(
                    icon = if (autoRotate) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Toggle Rotation",
                    onClick = { autoRotate = !autoRotate }
                )
                SatelliteRoundControl(
                    icon = Icons.Rounded.MyLocation,
                    contentDescription = "Focus Makkah",
                    onClick = {
                        autoRotate = false
                        camLat = CosmicEarthGlobeEngine.KAABA_LAT
                        camLon = CosmicEarthGlobeEngine.KAABA_LON
                    }
                )
            }
        }

        // Bottom info
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC041529))
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = L10n.pick(
                    lang,
                    "اسحب بإصبعك لتدوير الكرة ٣٦٠° · الشمس الآن فوق (${String.format(Locale.US, "%.1f°", subsolar.latitudeDeg)}, ${String.format(Locale.US, "%.1f°", subsolar.longitudeDeg)}) · $userCityLabel",
                    "Drag to rotate 360° · Subsolar (${String.format(Locale.US, "%.1f°", subsolar.latitudeDeg)}, ${String.format(Locale.US, "%.1f°", subsolar.longitudeDeg)}) · $userCityLabel"
                ),
                style = MaterialTheme.typography.labelSmall,
                color = GoldBright
            )
        }
    }
}

@Composable
private fun SatelliteRoundControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xD9041529))
            .border(1.dp, GoldBright.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = GoldBright, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun QuickFlyPill(
    label: String,
    highlight: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) GoldPrimary.copy(alpha = 0.28f) else EmeraldPrimary.copy(alpha = 0.18f))
            .border(1.dp, if (highlight) GoldBright else GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) GoldBright else MaterialTheme.colorScheme.onSurface
        )
    }
}
