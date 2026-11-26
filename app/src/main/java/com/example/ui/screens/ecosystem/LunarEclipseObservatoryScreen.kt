package com.example.ui.screens.ecosystem

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Brightness2
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

data class LunarPhaseSnapshot(
    val ageDays: Double,
    val phaseFraction: Double, // 0.0 = New Moon, 0.5 = Full Moon, 1.0 = Next New Moon
    val illuminationPercent: Double,
    val isWaxing: Boolean,
    val phaseNameAr: String,
    val phaseNameEn: String,
    val distanceKm: Int,
    val daysToAyyamAlBid: Int,
    val isAyyamAlBidNow: Boolean
)

data class UpcomingEclipseEvent(
    val id: String,
    val isSolar: Boolean,
    val titleAr: String,
    val titleEn: String,
    val dateLabelAr: String,
    val dateLabelEn: String,
    val utcYear: Int,
    val utcMonth: Int, // 1..12
    val utcDay: Int,
    val visibilityAr: String,
    val visibilityEn: String
)

data class LunarSurfaceLandmark(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val latDeg: Double,
    val lonDeg: Double, // selenographic lon (-180..180, 0 = center of Near Side)
    val noteAr: String,
    val noteEn: String
)

/**
 * Astronomical Lunar Phase & Eclipse Observatory Engine (Synodic Month = 29.530588853 days).
 * Computes real-time Lunar Age, Illumination %, Distance (km), Ayyam al-Bid countdown,
 * real selenographic landmarks, and NASA-verified Solar & Lunar Eclipses (2026–2028).
 */
object LunarAstronomyEngine {
    const val SYNODIC_MONTH_DAYS = 29.530588853
    // Known reference New Moon epoch: 2000-01-06 18:14 UTC
    private const val REFERENCE_NEW_MOON_MS = 947182440000L

    val landmarks: List<LunarSurfaceLandmark> = listOf(
        LunarSurfaceLandmark("tycho", "فوهة تيخو العظيمة", "Tycho Crater", -43.3, -11.2, "أشهر فوهة قمرية بأشعة ساطعة تمتد ١٥٠٠ كم", "Brightest ray crater spanning 1,500 km"),
        LunarSurfaceLandmark("copernicus", "فوهة كوبرنيكوس", "Copernicus Crater", 9.6, -20.1, "فوهة بركانية ضخمة بقطر ٩٣ كم", "Massive 93 km terraced impact crater"),
        LunarSurfaceLandmark("tranquillitatis", "بحر السكون (موقع أبولو ١١)", "Mare Tranquillitatis (Apollo 11)", 0.7, 23.5, "سهل بازلتي مظلم واسع وموقع هبوط أبولو ١١ التاريخي", "Dark basaltic lunar sea & historic Apollo 11 site"),
        LunarSurfaceLandmark("imbrium", "بحر الأمطار", "Mare Imbrium", 32.8, -15.6, "أحد أكبر الأحواض القمرية بقطر ١١٤٥ كم", "One of the largest basins on the Moon (1,145 km)"),
        LunarSurfaceLandmark("crisium", "بحر الشدائد", "Mare Crisium", 17.0, 59.1, "حوض بيضاوي بارز قرب الحافة الشرقية للهلال", "Prominent oval mare near the waxing crescent limb"),
        LunarSurfaceLandmark("clavius", "فوهة كلافيوس العملاقة", "Clavius Crater", -58.4, -14.4, "من أضخم الفوهات القمرية القديمة بقطر ٢٣١ كم قرب القطب الجنوبي", "Ancient 231 km mega-crater near the lunar South Pole"),
        LunarSurfaceLandmark("farside", "الوجه البعيد للقمر", "Lunar Far Side", 0.0, 180.0, "الجانب الخفي الذي لا يُرى من الأرض — التقطته مركبة ناسا LRO!", "Hidden hemisphere never seen from Earth — imaged by NASA LRO!")
    )

    fun computeLunarPhase(epochMillis: Long): LunarPhaseSnapshot {
        val daysSinceRef = (epochMillis - REFERENCE_NEW_MOON_MS) / 86_400_000.0
        val cycles = daysSinceRef / SYNODIC_MONTH_DAYS
        val frac = (cycles - floor(cycles)).let { if (it < 0.0) it + 1.0 else it }
        val ageDays = frac * SYNODIC_MONTH_DAYS
        val illumination = ((1.0 - cos(2.0 * PI * frac)) / 2.0) * 100.0
        val isWaxing = frac < 0.5

        val anomAngle = 2.0 * PI * ((daysSinceRef / 27.55455) % 1.0)
        val distKm = (384_400.0 - 20_900.0 * cos(anomAngle)).roundToInt()

        val (nameAr, nameEn) = when {
            ageDays < 1.2 || ageDays >= 28.4 -> "محاق (ولادة الهلال)" to "New Moon (Al-Mahaq)"
            ageDays < 6.4 -> "هلال متزايد" to "Waxing Crescent (Hilal)"
            ageDays < 8.4 -> "التربيع الأول" to "First Quarter"
            ageDays < 13.6 -> "أحدب متزايد" to "Waxing Gibbous"
            ageDays < 16.0 -> "بدر مكتمل" to "Full Moon (Badr)"
            ageDays < 21.2 -> "أحدب متناقص" to "Waning Gibbous"
            ageDays < 23.2 -> "التربيع الأخير" to "Last Quarter"
            else -> "هلال متناقص" to "Waning Crescent"
        }

        val roundedDay = floor(ageDays).toInt() + 1
        val isBid = roundedDay in 13..15
        val daysToBid = if (isBid) {
            0
        } else if (roundedDay < 13) {
            13 - roundedDay
        } else {
            (30 - roundedDay) + 13
        }

        return LunarPhaseSnapshot(
            ageDays = ageDays,
            phaseFraction = frac,
            illuminationPercent = illumination.coerceIn(0.0, 100.0),
            isWaxing = isWaxing,
            phaseNameAr = nameAr,
            phaseNameEn = nameEn,
            distanceKm = distKm,
            daysToAyyamAlBid = daysToBid,
            isAyyamAlBidNow = isBid
        )
    }

    val upcomingEclipses: List<UpcomingEclipseEvent> = listOf(
        UpcomingEclipseEvent(
            id = "solar_2026_aug",
            isSolar = true,
            titleAr = "كسوف شمسي كلي عظيم",
            titleEn = "Total Solar Eclipse",
            dateLabelAr = "١٢ أغسطس ٢٠٢٦ م",
            dateLabelEn = "August 12, 2026",
            utcYear = 2026, utcMonth = 8, utcDay = 12,
            visibilityAr = "أوروبا وشمال أفريقيا والمغرب العربي والمحيط الأطلسي",
            visibilityEn = "Europe, North Africa, Maghreb & Atlantic"
        ),
        UpcomingEclipseEvent(
            id = "lunar_2026_aug",
            isSolar = false,
            titleAr = "خسوف قمري جزئي عميق (٩٣٪)",
            titleEn = "Deep Partial Lunar Eclipse (93%)",
            dateLabelAr = "٢٨ أغسطس ٢٠٢٦ م",
            dateLabelEn = "August 28, 2026",
            utcYear = 2026, utcMonth = 8, utcDay = 28,
            visibilityAr = "أفريقيا وأوروبا والأمريكتان والشرق الأوسط جزئياً",
            visibilityEn = "Africa, Europe, Americas & Western Middle East"
        ),
        UpcomingEclipseEvent(
            id = "solar_2027_feb",
            isSolar = true,
            titleAr = "كسوف شمسي حلقي (حلقة النار)",
            titleEn = "Annular Solar Eclipse (Ring of Fire)",
            dateLabelAr = "٦ فبراير ٢٠٢٧ م",
            dateLabelEn = "February 6, 2027",
            utcYear = 2027, utcMonth = 2, utcDay = 6,
            visibilityAr = "أمريكا الجنوبية وغرب أفريقيا والمحيط الأطلسي",
            visibilityEn = "South America, West Africa & Atlantic"
        ),
        UpcomingEclipseEvent(
            id = "solar_2027_aug_makkah",
            isSolar = true,
            titleAr = "كسوف القرن الكلي (يمر فوق مكة المكرمة ومصر!)",
            titleEn = "Great Total Solar Eclipse over Makkah & Egypt!",
            dateLabelAr = "٢ أغسطس ٢٠٢٧ م",
            dateLabelEn = "August 2, 2027",
            utcYear = 2027, utcMonth = 8, utcDay = 2,
            visibilityAr = "يمر مسار الكسوف الكلي مباشرة فوق المغرب ومصر وجدة ومكة المكرمة واليمن لمدة تفوق ٦ دقائق!",
            visibilityEn = "Totality passes directly across Morocco, Egypt, Jeddah, Makkah & Yemen for over 6 minutes!"
        ),
        UpcomingEclipseEvent(
            id = "lunar_2028_jan",
            isSolar = false,
            titleAr = "خسوف قمري جزئي",
            titleEn = "Partial Lunar Eclipse",
            dateLabelAr = "١٢ يناير ٢٠٢٨ م",
            dateLabelEn = "January 12, 2028",
            utcYear = 2028, utcMonth = 1, utcDay = 12,
            visibilityAr = "أوروبا وأفريقيا والأمريكتان",
            visibilityEn = "Europe, Africa & Americas"
        ),
        UpcomingEclipseEvent(
            id = "lunar_2028_dec",
            isSolar = false,
            titleAr = "خسوف قمري كلي (القمر الدامي)",
            titleEn = "Total Lunar Eclipse",
            dateLabelAr = "٣١ ديسمبر ٢٠٢٨ م",
            dateLabelEn = "December 31, 2028",
            utcYear = 2028, utcMonth = 12, utcDay = 31,
            visibilityAr = "الوطن العربي وآسيا وأفريقيا وأوروبا كاملة",
            visibilityEn = "Full Arab World, Asia, Africa & Europe"
        )
    )

    fun daysUntil(event: UpcomingEclipseEvent, nowMillis: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(event.utcYear, event.utcMonth - 1, event.utcDay, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return ((cal.timeInMillis - nowMillis) / 86_400_000L)
    }

    val kusufPrayerStepsAr: List<Pair<String, String>> = listOf(
        "١. النية والنداء «الصلاة جامعة»" to "سنة مؤكدة عند رؤية كسوف الشمس أو خسوف القمر، ويُستحب الإكثار من الذكر والتكبير والاستغفار والصدقة.",
        "٢. الركعة الأولى (ركوعان طويلان)" to "تقرأ الفاتحة وسورة طويلة ثم تركع ركوعاً طويلاً، ثم ترفع وتقول «سمع الله لمن حمده» ثم تقرأ الفاتحة وسورة أقصر ثم تركع ركوعاً ثانياً ثم تسجد سجدتين طويلتين.",
        "٣. الركعة الثانية (مثل الأولى)" to "تقوم للركعة الثانية بقيامين وركوعين وسجدتين، وتكون أخف قليلاً من الركعة الأولى، ثم تتشهد وتسلم.",
        "٤. الدعاء حتى ينجلي الكسوف" to "قال ﷺ: «إنَّ الشَّمسَ والقَمَرَ آيَتانِ مِن آياتِ اللَّهِ، لا يَنْخَسِفانِ لِمَوتِ أحَدٍ ولا لِحَياتِهِ، فإذا رَأَيْتُمْ ذلكَ فادْعُوا اللَّهَ وكَبِّرُوا وصَلُّوا وتَصَدَّقُوا» (متفق عليه)."
    )
}

/**
 * REAL NASA Moon Satellite Photography & Live Eclipse Observatory:
 * - Mode 0 (Default): REAL NASA Moon Trek LRO WAC Satellite Explorer (zoom 1..6 down to real craters!)
 * - Mode 1: Fixed Live NASA Goddard SVS Dial-A-Moon Observatory (multi-hop redirects + NASA Trek fallback -> never fails!)
 * - Mode 2: Real 3D Rotating Moon wrapped in genuine stitched NASA Moon Trek LRO mosaic + Blood-Moon & Solar Eclipse simulators.
 */
@Composable
fun LunarEclipseObservatoryScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current

    val baseNowMillis = remember { System.currentTimeMillis() }
    var dayOffsetSlider by remember { mutableFloatStateOf(0f) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Real Moon, 1 = Eclipses, 2 = Salat al-Kusuf
    // 0 = Real NASA LRO Satellite Explorer (Zoomable Craters!), 1 = NASA Live Observatory, 2 = 3D NASA Moon + Eclipses
    var moonFeedMode by remember { mutableIntStateOf(0) }

    // Mode 0: Real NASA Moon Trek Satellite Camera State (Lat/Lon + Zoom 1..6)
    var satCenterLat by remember { mutableDoubleStateOf(6.0) }
    var satCenterLon by remember { mutableDoubleStateOf(-8.0) }
    var satZoom by remember { mutableIntStateOf(2) }
    var satSourceLabel by remember { mutableStateOf("NASA Moon Trek LRO WAC") }

    // Mode 1 & 2: Live Zoom & 3D Camera State
    var rotLonDeg by remember { mutableFloatStateOf(0f) }
    var rotLatDeg by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showLandmarks by remember { mutableStateOf(true) }
    var simulation by remember { mutableStateOf(MoonRenderSimulation.REAL_PHASE) }
    var eclipseProgress by remember { mutableFloatStateOf(0.50f) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    // Real NASA Global Surface Mosaic + Rendered 3D Sphere Bitmap
    var surfaceMapResult by remember { mutableStateOf<NasaSurfaceMapResult?>(null) }
    var rendered3DSphere by remember { mutableStateOf<ImageBitmap?>(null) }

    // NASA Live Observatory Frame State (NEVER fails!)
    var nasaLiveFrame by remember { mutableStateOf<NasaLiveMoonResult?>(null) }
    var isLoadingNasaLive by remember { mutableStateOf(false) }

    val inspectedMillis = baseNowMillis + (dayOffsetSlider * 86_400_000.0).toLong()
    val moon = remember(inspectedMillis) { LunarAstronomyEngine.computeLunarPhase(inspectedMillis) }

    // Load REAL stitched NASA Moon Trek LRO WAC Global Mosaic once (cached to disk + memory!)
    LaunchedEffect(reloadNonce) {
        surfaceMapResult = NasaRealMoonRepository.loadOrFetchRealNasaGlobalMap(
            context = context,
            forceRefresh = reloadNonce > 0
        )
    }

    // Re-render real 3D NASA LRO sphere whenever Mode 2 is active or parameters change
    LaunchedEffect(moonFeedMode, surfaceMapResult, moon.phaseFraction, rotLonDeg, rotLatDeg, simulation, eclipseProgress) {
        val tex = surfaceMapResult?.bitmap ?: return@LaunchedEffect
        if (moonFeedMode != 2 && rendered3DSphere != null) return@LaunchedEffect
        val sphere = withContext(Dispatchers.Default) {
            Real3DLunarSphereRenderer.render3DMoonSphere(
                texture = tex,
                outputSize = 300,
                phaseFraction = moon.phaseFraction,
                rotLonDeg = rotLonDeg.toDouble(),
                rotLatDeg = rotLatDeg.toDouble(),
                simulation = simulation,
                eclipseProgress = eclipseProgress
            ).asImageBitmap()
        }
        rendered3DSphere = sphere
    }

    // Fetch LIVE NASA Goddard SVS Dial-A-Moon Observatory (with guaranteed NASA Trek LRO live fallback!)
    LaunchedEffect(moonFeedMode, dayOffsetSlider.roundToInt(), reloadNonce) {
        if (moonFeedMode == 1) {
            isLoadingNasaLive = true
            nasaLiveFrame = NasaRealMoonRepository.fetchLiveNasaObservatoryFrame(
                context = context,
                epochMillis = inspectedMillis,
                phaseSnapshot = moon
            )
            isLoadingNasaLive = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lunar_eclipse_observatory_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مرصد القمر الحقيقي وناسا", "Real NASA Moon & Eclipses"),
            subtitle = L10n.pick(
                lang,
                "تصوير قمر صناعي حقيقي من مركبة ناسا LRO + رصد ناسا المباشر + الكسوف",
                "Real NASA LRO Satellite Moon Tiles + Live NASA Observatory + Eclipses"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "🌕 القمر الحقيقي", "🌕 Real Moon"),
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "🌑 الكسوف", "🌑 Eclipses"),
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "🕌 صلاة الكسوف", "🕌 Kusuf"),
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (activeTab == 0) {
                item {
                    LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                        // 3 Real NASA Modes: Satellite Tile Explorer / NASA Live Observatory / 3D NASA Globe
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "🛰️ تصوير ناسا الحقيقي (قمر صناعي)", "🛰️ Real NASA Satellite"),
                                    selected = moonFeedMode == 0,
                                    onClick = { moonFeedMode = 0 },
                                    modifier = Modifier.testTag("moon_mode_real_satellite")
                                )
                            }
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "🔭 رصد ناسا المباشر", "🔭 NASA Live Feed"),
                                    selected = moonFeedMode == 1,
                                    onClick = { moonFeedMode = 1 },
                                    modifier = Modifier.testTag("moon_mode_nasa_live")
                                )
                            }
                            item {
                                LiquidGlassChip(
                                    label = L10n.pick(lang, "🌕 قمر ٣D حقيقي (خسوف/كسوف)", "🌕 3D NASA Globe"),
                                    selected = moonFeedMode == 2,
                                    onClick = { moonFeedMode = 2 },
                                    modifier = Modifier.testTag("moon_mode_3d_lro")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Main Real Moon Viewport
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(355.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF0A1930), Color(0xFF030814), Color(0xFF000208))
                                    )
                                )
                                .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                .testTag("real_3d_moon_viewport"),
                            contentAlignment = Alignment.Center
                        ) {
                            when (moonFeedMode) {
                                0 -> {
                                    // MODE 0: REAL NASA MOON TREK LRO WAC SATELLITE TILE EXPLORER!
                                    RealNasaMoonSatelliteMapCanvas(
                                        centerLat = satCenterLat,
                                        centerLon = satCenterLon,
                                        zoom = satZoom,
                                        showLandmarks = showLandmarks,
                                        reloadNonce = reloadNonce,
                                        fallbackGlobalMap = surfaceMapResult?.bitmap,
                                        onPan = { dLat, dLon ->
                                            satCenterLat = (satCenterLat + dLat).coerceIn(-82.0, 82.0)
                                            satCenterLon = ((satCenterLon + dLon + 540.0) % 360.0) - 180.0
                                        },
                                        onPinchZoom = { zoomDelta ->
                                            if (zoomDelta > 1.24f && satZoom < NasaMoonSatelliteMath.MAX_TREK_ZOOM) {
                                                satZoom += 1
                                            } else if (zoomDelta < 0.80f && satZoom > 0) {
                                                satZoom -= 1
                                            }
                                        },
                                        onSourceDetected = { src ->
                                            if (src.isNotBlank()) satSourceLabel = src
                                        }
                                    )
                                }

                                1 -> {
                                    // MODE 1: FIXED REAL LIVE NASA OBSERVATORY (SVS Dial-A-Moon + NASA Trek LRO Fallback!)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectTransformGestures { _, _, zoom, _ ->
                                                    zoomScale = (zoomScale * zoom).coerceIn(1.0f, 3.6f)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val frame = nasaLiveFrame
                                        if (isLoadingNasaLive && frame == null) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = GoldBright, strokeWidth = 3.dp)
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = L10n.pick(
                                                        lang,
                                                        "جاري الاتصال المباشر بمرصد ناسا (NASA SVS & LRO)...",
                                                        "Connecting live to NASA SVS & LRO Observatory..."
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GoldBright
                                                )
                                            }
                                        } else if (frame != null) {
                                            Image(
                                                bitmap = frame.imageBitmap,
                                                contentDescription = L10n.pick(lang, "رصد القمر الحقيقي المباشر من ناسا", "Live Real NASA Moon Observation"),
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp)
                                                    .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
                                            )
                                            if (isLoadingNasaLive) {
                                                CircularProgressIndicator(
                                                    color = GoldBright,
                                                    strokeWidth = 2.5.dp,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(14.dp)
                                                        .size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    // MODE 2: REAL 3D SPHERICAL NASA LRO MOON + BLOOD MOON & SOLAR ECLIPSE SIMULATOR
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectTransformGestures { _, pan, zoom, _ ->
                                                    zoomScale = (zoomScale * zoom).coerceIn(1.0f, 3.4f)
                                                    rotLonDeg = ((rotLonDeg - pan.x * 0.35f + 540f) % 360f) - 180f
                                                    rotLatDeg = (rotLatDeg + pan.y * 0.30f).coerceIn(-75f, 75f)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val baseD = size.minDimension * 0.84f * zoomScale

                                            val stars = listOf(
                                                0.08f to 0.14f, 0.90f to 0.16f, 0.18f to 0.82f, 0.86f to 0.78f,
                                                0.12f to 0.45f, 0.82f to 0.38f, 0.32f to 0.10f, 0.68f to 0.88f,
                                                0.48f to 0.08f, 0.92f to 0.56f, 0.06f to 0.68f, 0.74f to 0.14f
                                            )
                                            stars.forEach { (sx, sy) ->
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    radius = 2f,
                                                    center = Offset(size.width * sx, size.height * sy)
                                                )
                                            }

                                            when (simulation) {
                                                MoonRenderSimulation.TOTAL_SOLAR_ECLIPSE -> {
                                                    drawCircle(
                                                        brush = Brush.radialGradient(
                                                            listOf(
                                                                Color(0xFFFFFFFF),
                                                                Color(0xCCFEF08A),
                                                                Color(0x55F59E0B),
                                                                Color.Transparent
                                                            ),
                                                            center = Offset(cx, cy),
                                                            radius = baseD * 0.78f
                                                        ),
                                                        radius = baseD * 0.78f,
                                                        center = Offset(cx, cy)
                                                    )
                                                    val diamondPos = Offset(cx + baseD * 0.34f, cy - baseD * 0.30f)
                                                    drawCircle(
                                                        brush = Brush.radialGradient(
                                                            listOf(Color.White, Color(0xBBFEF08A), Color.Transparent),
                                                            center = diamondPos,
                                                            radius = baseD * 0.20f
                                                        ),
                                                        radius = baseD * 0.20f,
                                                        center = diamondPos
                                                    )
                                                }

                                                MoonRenderSimulation.BLOOD_MOON_ECLIPSE -> {
                                                    drawCircle(
                                                        brush = Brush.radialGradient(
                                                            listOf(Color(0x66DC2626), Color(0x22991B1B), Color.Transparent),
                                                            center = Offset(cx, cy),
                                                            radius = baseD * 0.65f
                                                        ),
                                                        radius = baseD * 0.65f,
                                                        center = Offset(cx, cy)
                                                    )
                                                }

                                                MoonRenderSimulation.REAL_PHASE -> {
                                                    val haloAlpha = (moon.illuminationPercent / 100.0).toFloat() * 0.32f + 0.06f
                                                    drawCircle(
                                                        brush = Brush.radialGradient(
                                                            listOf(Color(0xFFE2E8F0).copy(alpha = haloAlpha), Color.Transparent),
                                                            center = Offset(cx, cy),
                                                            radius = baseD * 0.64f
                                                        ),
                                                        radius = baseD * 0.64f,
                                                        center = Offset(cx, cy)
                                                    )
                                                }
                                            }

                                            val bmp = rendered3DSphere
                                            if (bmp != null) {
                                                val drawSize = baseD.roundToInt().coerceAtLeast(32)
                                                drawImage(
                                                    image = bmp,
                                                    dstOffset = IntOffset((cx - drawSize / 2f).roundToInt(), (cy - drawSize / 2f).roundToInt()),
                                                    dstSize = IntSize(drawSize, drawSize),
                                                    filterQuality = FilterQuality.High
                                                )
                                            }

                                            if (showLandmarks && simulation != MoonRenderSimulation.TOTAL_SOLAR_ECLIPSE) {
                                                val sphereR = baseD * 0.465f
                                                val yawRad = Math.toRadians(rotLonDeg.toDouble())
                                                val pitchRad = Math.toRadians(rotLatDeg.toDouble())
                                                val cosP = cos(pitchRad)
                                                val sinP = sin(pitchRad)

                                                LunarAstronomyEngine.landmarks.forEach { lm ->
                                                    val latR = Math.toRadians(lm.latDeg)
                                                    val dLon = Math.toRadians(lm.lonDeg) - yawRad
                                                    val x0 = cos(latR) * sin(dLon)
                                                    val y0 = -sin(latR)
                                                    val z0 = cos(latR) * cos(dLon)

                                                    val yr = y0 * cosP + z0 * sinP
                                                    val zr = -y0 * sinP + z0 * cosP
                                                    if (zr > 0.12) {
                                                        val px = cx + (x0 * sphereR).toFloat()
                                                        val py = cy + (yr * sphereR).toFloat()
                                                        drawCircle(GoldBright.copy(alpha = 0.32f), radius = 10f, center = Offset(px, py))
                                                        drawCircle(GoldBright, radius = 3.8f, center = Offset(px, py))
                                                        drawCircle(Color.White, radius = 1.6f, center = Offset(px, py))
                                                    }
                                                }
                                            }
                                        }

                                        if (rendered3DSphere == null) {
                                            CircularProgressIndicator(color = GoldBright, strokeWidth = 3.dp)
                                        }
                                    }
                                }
                            }

                            // Top HUD Badge (Live NASA Spacecraft Source Indicator)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xD8041226))
                                        .border(1.dp, GoldBright.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = when (moonFeedMode) {
                                            0 -> L10n.pick(
                                                lang,
                                                "🛰️ $satSourceLabel · تقريب Z$satZoom",
                                                "🛰️ $satSourceLabel · Zoom Z$satZoom"
                                            )
                                            1 -> nasaLiveFrame?.let { L10n.pick(lang, it.sourceLabelAr, it.sourceLabelEn) }
                                                ?: L10n.pick(lang, "🔭 رصد ناسا المباشر...", "🔭 NASA Live Feed...")
                                            else -> surfaceMapResult?.let {
                                                L10n.pick(lang, "${it.sourceLabelAr} · ${moon.phaseNameAr}", "${it.sourceLabelEn} · ${moon.phaseNameEn}")
                                            } ?: L10n.pick(lang, "جاري تحميل تصوير ناسا LRO...", "Loading NASA LRO...")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldBright,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                if (moonFeedMode != 1) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xD8041226))
                                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            .clickable { showLandmarks = !showLandmarks }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (showLandmarks) L10n.pick(lang, "الفوهات: مفعّلة", "Craters: ON") else L10n.pick(lang, "الفوهات: إخفاء", "Craters: OFF"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Right Zoom + / - / Refresh Controls
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MoonControlCircle(Icons.Rounded.Add, "Zoom In") {
                                    if (moonFeedMode == 0) {
                                        satZoom = (satZoom + 1).coerceAtMost(NasaMoonSatelliteMath.MAX_TREK_ZOOM)
                                    } else {
                                        zoomScale = (zoomScale + 0.45f).coerceAtMost(3.6f)
                                    }
                                }
                                MoonControlCircle(Icons.Rounded.Remove, "Zoom Out") {
                                    if (moonFeedMode == 0) {
                                        satZoom = (satZoom - 1).coerceAtLeast(0)
                                    } else {
                                        zoomScale = (zoomScale - 0.45f).coerceAtLeast(1.0f)
                                    }
                                }
                                MoonControlCircle(Icons.Rounded.Refresh, "Reset & Reload") {
                                    if (moonFeedMode == 0) {
                                        satCenterLat = 6.0
                                        satCenterLon = -8.0
                                        satZoom = 2
                                    } else {
                                        rotLonDeg = 0f
                                        rotLatDeg = 0f
                                        zoomScale = 1.0f
                                        dayOffsetSlider = 0f
                                        simulation = MoonRenderSimulation.REAL_PHASE
                                    }
                                    reloadNonce++
                                }
                            }

                            // Bottom Context Hint
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xD0041226))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = when (moonFeedMode) {
                                        0 -> L10n.pick(
                                            lang,
                                            String.format(Locale.US, "اسحب لتصفح سطح القمر الحقيقي · عرض %.1f° طول %.1f° · Z%d", satCenterLat, satCenterLon, satZoom),
                                            String.format(Locale.US, "Pan real NASA Moon surface · Lat %.1f° Lon %.1f° · Z%d", satCenterLat, satCenterLon, satZoom)
                                        )
                                        1 -> L10n.pick(
                                            lang,
                                            "صورة رصد ناسا المباشر للطور القمري الحالي · قرّب بإصبعين ×${String.format(Locale.US, "%.1f", zoomScale)}",
                                            "Live NASA Observatory Lunar Phase · Pinch Zoom ×${String.format(Locale.US, "%.1f", zoomScale)}"
                                        )
                                        else -> L10n.pick(
                                            lang,
                                            "اسحب لتدوير القمر ٣٦٠° (شاهد الوجه البعيد!) · تقريب ×${String.format(Locale.US, "%.1f", zoomScale)}",
                                            "Drag to spin 360° (see Far Side!) · Zoom ×${String.format(Locale.US, "%.1f", zoomScale)}"
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Eclipse Simulator Pills (visible in 3D NASA Globe mode)
                        if (moonFeedMode == 2) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    LiquidGlassChip(
                                        label = L10n.pick(lang, "🌕 الطور الحقيقي", "🌕 Real Phase"),
                                        selected = simulation == MoonRenderSimulation.REAL_PHASE,
                                        onClick = { simulation = MoonRenderSimulation.REAL_PHASE }
                                    )
                                }
                                item {
                                    LiquidGlassChip(
                                        label = L10n.pick(lang, "🔴 محاكاة الخسوف الدموي", "🔴 Blood Moon Eclipse"),
                                        selected = simulation == MoonRenderSimulation.BLOOD_MOON_ECLIPSE,
                                        onClick = { simulation = MoonRenderSimulation.BLOOD_MOON_ECLIPSE }
                                    )
                                }
                                item {
                                    LiquidGlassChip(
                                        label = L10n.pick(lang, "☀️ كسوف الشمس الكلي", "☀️ Total Solar Eclipse"),
                                        selected = simulation == MoonRenderSimulation.TOTAL_SOLAR_ECLIPSE,
                                        onClick = { simulation = MoonRenderSimulation.TOTAL_SOLAR_ECLIPSE }
                                    )
                                }
                            }

                            if (simulation == MoonRenderSimulation.BLOOD_MOON_ECLIPSE) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = L10n.pick(lang, "حرّك ظل الأرض الأحمر عبر سطح القمر الحقيقي:", "Slide Earth's Red Umbra Shadow across the real Moon:"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF87171),
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = eclipseProgress,
                                    onValueChange = { eclipseProgress = it },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFEF4444),
                                        activeTrackColor = Color(0xFFEF4444),
                                        inactiveTrackColor = GoldPrimary.copy(alpha = 0.25f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Live Telemetry Row
                        val shownIllum = if (moonFeedMode == 1 && nasaLiveFrame?.phasePercent != null) nasaLiveFrame!!.phasePercent!! else moon.illuminationPercent
                        val shownAge = if (moonFeedMode == 1 && nasaLiveFrame?.ageDays != null) nasaLiveFrame!!.ageDays!! else moon.ageDays
                        val shownDist = if (moonFeedMode == 1 && nasaLiveFrame?.distanceKm != null) nasaLiveFrame!!.distanceKm!! else moon.distanceKm

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarMetricTile(
                                label = L10n.pick(lang, "الإضاءة", "Illumination"),
                                value = String.format(Locale.US, "%.1f%%", shownIllum),
                                modifier = Modifier.weight(1f)
                            )
                            LunarMetricTile(
                                label = L10n.pick(lang, "عمر القمر", "Lunar Age"),
                                value = String.format(Locale.US, "%.1f ي", shownAge),
                                modifier = Modifier.weight(1f)
                            )
                            LunarMetricTile(
                                label = L10n.pick(lang, "البُعد عن الأرض", "Distance"),
                                value = String.format(Locale.US, "%,d كم", shownDist),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Interactive 30-Day Synodic Month Scrubber
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(lang, "محاكي الشهر القمري الحقيقي (٣٠ يوماً)", "30-Day Real Lunar Cycle Scrubber"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldBright
                            )
                            Text(
                                text = if (abs(dayOffsetSlider) < 0.25f) {
                                    L10n.pick(lang, "الليلة (الآن)", "Tonight (Live)")
                                } else {
                                    val d = dayOffsetSlider.roundToInt()
                                    if (d > 0) L10n.pick(lang, "بعد $d يوم", "+$d days")
                                    else L10n.pick(lang, "قبل ${abs(d)} يوم", "$d days")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }

                        Slider(
                            value = dayOffsetSlider,
                            onValueChange = {
                                dayOffsetSlider = it
                                simulation = MoonRenderSimulation.REAL_PHASE
                                if (moonFeedMode == 0) moonFeedMode = 1
                            },
                            valueRange = -15f..15f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldBright,
                                activeTrackColor = GoldBright,
                                inactiveTrackColor = GoldPrimary.copy(alpha = 0.25f)
                            )
                        )

                        // Ayyam al-Bid Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (moon.isAyyamAlBidNow) EmeraldPrimary.copy(alpha = 0.24f)
                                    else GoldPrimary.copy(alpha = 0.12f)
                                )
                                .border(
                                    1.dp,
                                    if (moon.isAyyamAlBidNow) EmeraldPrimary else GoldPrimary.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = GoldBright, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (moon.isAyyamAlBidNow) {
                                        L10n.pick(lang, "أنت الآن في الأيام البيض المباركة (١٣–١٤–١٥) — يُسن صيامها!", "Ayyam al-Bid (White Days 13–15) are active now!")
                                    } else {
                                        L10n.pick(lang, "متبقي تقريباً ${moon.daysToAyyamAlBid} يوماً على صيام الأيام البيض القادمة", "~${moon.daysToAyyamAlBid} days until the next Sunnah White Days (13–15)")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Real Lunar Surface Landmarks Fly-To (Tap any crater/sea to zoom right into REAL NASA satellite imagery!)
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Rounded.Public, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(lang, "استكشف فوهات القمر الحقيقية بالقمر الصناعي (اضغط للتقريب)", "Fly to Real Lunar Craters via NASA Satellite (Tap)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                    }
                }

                items(LunarAstronomyEngine.landmarks, key = { it.id }) { lm ->
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (moonFeedMode == 2) {
                                    simulation = MoonRenderSimulation.REAL_PHASE
                                    rotLonDeg = lm.lonDeg.toFloat()
                                    rotLatDeg = lm.latDeg.toFloat().coerceIn(-65f, 65f)
                                    zoomScale = if (lm.id == "farside") 1.15f else 1.95f
                                } else {
                                    moonFeedMode = 0
                                    satCenterLat = lm.latDeg
                                    satCenterLon = lm.lonDeg
                                    satZoom = if (lm.id == "farside") 2 else 4
                                }
                            },
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, lm.nameAr, lm.nameEn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = L10n.pick(lang, lm.noteAr, lm.noteEn),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GoldPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = L10n.pick(lang, "تقريب ناسا", "NASA Zoom"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBright
                                )
                            }
                        }
                    }
                }
            } else if (activeTab == 1) {
                items(LunarAstronomyEngine.upcomingEclipses, key = { it.id }) { ev ->
                    val daysLeft = remember(ev.id) { LunarAstronomyEngine.daysUntil(ev, baseNowMillis) }
                    val accent = if (ev.isSolar) GoldBright else Color(0xFFF87171)
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeTab = 0
                                moonFeedMode = 2
                                simulation = if (ev.isSolar) MoonRenderSimulation.TOTAL_SOLAR_ECLIPSE else MoonRenderSimulation.BLOOD_MOON_ECLIPSE
                            },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.18f))
                                    .border(1.2.dp, accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (ev.isSolar) Icons.Rounded.WbSunny else Icons.Rounded.Brightness2,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, ev.titleAr, ev.titleEn),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Event, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = L10n.pick(lang, ev.dateLabelAr, ev.dateLabelEn),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.pick(lang, ev.visibilityAr, ev.visibilityEn),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (daysLeft >= 0) {
                                        L10n.pick(lang, "بعد $daysLeft ي", "In ${daysLeft}d")
                                    } else {
                                        L10n.pick(lang, "مضى", "Past")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    LiquidGlassCard(contentPadding = PaddingValues(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Mosque, contentDescription = null, tint = GoldBright, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "صفة صلاة الكسوف والخسوف النبوية", "Prophetic Guide to Salat al-Kusuf & Khusuf"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldBright
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LunarAstronomyEngine.kusufPrayerStepsAr.forEach { (stepTitle, stepBody) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.12f))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = stepTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldBright
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stepBody,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
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

/**
 * Native Android Slippy Satellite Tile Map streaming REAL NASA Moon Trek LRO WAC
 * Global Mosaic photography (100m/pixel craters) with seamless OpenPlanetary Fastly fallback.
 */
@Composable
private fun RealNasaMoonSatelliteMapCanvas(
    centerLat: Double,
    centerLon: Double,
    zoom: Int,
    showLandmarks: Boolean,
    reloadNonce: Int,
    fallbackGlobalMap: Bitmap?,
    onPan: (dLat: Double, dLon: Double) -> Unit,
    onPinchZoom: (zoomFactor: Float) -> Unit,
    onSourceDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val loadedTiles = remember { mutableStateMapOf<NasaMoonTileKey, ImageBitmap>() }
    var viewportSize by remember { mutableStateOf(IntSize(600, 600)) }
    var isFetchingTiles by remember { mutableStateOf(false) }

    val tilePx = 256f
    val cols = NasaMoonSatelliteMath.trekCols(zoom)
    val rows = NasaMoonSatelliteMath.trekRows(zoom)
    val worldW = cols * tilePx
    val worldH = rows * tilePx

    // Compute visible tile keys around centerLat / centerLon
    val centerWorldX = (((centerLon + 180.0) / 360.0) * worldW).toFloat()
    val centerWorldY = (((90.0 - centerLat) / 180.0) * worldH).toFloat()

    val visibleKeys = remember(zoom, (centerLat * 2).roundToInt(), (centerLon * 2).roundToInt(), viewportSize) {
        val halfW = viewportSize.width / 2f + tilePx
        val halfH = viewportSize.height / 2f + tilePx
        val minCol = floor((centerWorldX - halfW) / tilePx).toInt()
        val maxCol = floor((centerWorldX + halfW) / tilePx).toInt()
        val minRow = floor((centerWorldY - halfH) / tilePx).toInt().coerceAtLeast(0)
        val maxRow = floor((centerWorldY + halfH) / tilePx).toInt().coerceAtMost(rows - 1)
        buildList {
            for (r in minRow..maxRow) {
                for (c in minCol..maxCol) {
                    val wrappedCol = ((c % cols) + cols) % cols
                    add(NasaMoonTileKey(zoom, r, wrappedCol))
                }
            }
        }.distinct()
    }

    // Stream all visible real NASA LRO satellite tiles concurrently!
    LaunchedEffect(visibleKeys, reloadNonce) {
        val missing = visibleKeys.filter { !loadedTiles.containsKey(it) }
        if (missing.isEmpty()) return@LaunchedEffect
        isFetchingTiles = true
        missing.forEach { key ->
            launch {
                val (bmp, source) = NasaRealMoonRepository.fetchMoonSatelliteTile(
                    context = context,
                    zoom = key.zoom,
                    row = key.row,
                    col = key.col
                )
                if (bmp != null) {
                    loadedTiles[key] = bmp.asImageBitmap()
                    if (source.isNotBlank()) onSourceDetected(source)
                }
            }
        }
        isFetchingTiles = false
    }

    val globalFallbackImage = remember(fallbackGlobalMap) { fallbackGlobalMap?.asImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(zoom) {
                var accumZoom = 1f
                detectTransformGestures { _, pan, zoomChange, _ ->
                    val degPerPxLon = 360.0 / (NasaMoonSatelliteMath.trekCols(zoom) * tilePx)
                    val degPerPxLat = 180.0 / (NasaMoonSatelliteMath.trekRows(zoom) * tilePx)
                    onPan(pan.y * degPerPxLat, -pan.x * degPerPxLon)
                    accumZoom *= zoomChange
                    if (accumZoom > 1.24f || accumZoom < 0.80f) {
                        onPinchZoom(accumZoom)
                        accumZoom = 1f
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val vw = size.width
            val vh = size.height
            val originX = vw / 2f - centerWorldX
            val originY = vh / 2f - centerWorldY

            // Base full-world NASA mosaic underneath while high-zoom tiles stream in (zero black gaps!)
            if (globalFallbackImage != null) {
                for (wrapOffset in -1..1) {
                    val left = (originX + wrapOffset * worldW).roundToInt()
                    val top = originY.roundToInt()
                    drawImage(
                        image = globalFallbackImage,
                        dstOffset = IntOffset(left, top),
                        dstSize = IntSize(worldW.roundToInt().coerceAtLeast(1), worldH.roundToInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.Medium
                    )
                }
            }

            // Draw crisp real NASA Moon Trek LRO WAC tiles
            val minCol = floor((centerWorldX - vw / 2f - tilePx) / tilePx).toInt()
            val maxCol = floor((centerWorldX + vw / 2f + tilePx) / tilePx).toInt()
            val minRow = floor((centerWorldY - vh / 2f - tilePx) / tilePx).toInt().coerceAtLeast(0)
            val maxRow = floor((centerWorldY + vh / 2f + tilePx) / tilePx).toInt().coerceAtMost(rows - 1)

            for (r in minRow..maxRow) {
                for (c in minCol..maxCol) {
                    val wrappedCol = ((c % cols) + cols) % cols
                    val tileBmp = loadedTiles[NasaMoonTileKey(zoom, r, wrappedCol)] ?: continue
                    val drawX = (originX + c * tilePx).roundToInt()
                    val drawY = (originY + r * tilePx).roundToInt()
                    drawImage(
                        image = tileBmp,
                        dstOffset = IntOffset(drawX, drawY),
                        dstSize = IntSize(tilePx.roundToInt() + 1, tilePx.roundToInt() + 1),
                        filterQuality = FilterQuality.High
                    )
                }
            }

            // Draw real lunar crater & Apollo landmark pins directly on the NASA satellite mosaic
            if (showLandmarks) {
                LunarAstronomyEngine.landmarks.forEach { lm ->
                    val lmWorldX = (((lm.lonDeg + 180.0) / 360.0) * worldW).toFloat()
                    val lmWorldY = (((90.0 - lm.latDeg) / 180.0) * worldH).toFloat()
                    for (wrap in -1..1) {
                        val sx = originX + lmWorldX + wrap * worldW
                        val sy = originY + lmWorldY
                        if (sx in -20f..(vw + 20f) && sy in -20f..(vh + 20f)) {
                            drawCircle(GoldBright.copy(alpha = 0.28f), radius = 13f, center = Offset(sx, sy))
                            drawCircle(GoldBright, radius = 4.8f, center = Offset(sx, sy))
                            drawCircle(Color.White, radius = 2.0f, center = Offset(sx, sy))
                        }
                    }
                }
            }

            // Subtle target crosshair at camera center
            drawCircle(
                color = GoldBright.copy(alpha = 0.35f),
                radius = 12f,
                center = Offset(vw / 2f, vh / 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
            )
        }

        if (isFetchingTiles && loadedTiles.isEmpty() && globalFallbackImage == null) {
            CircularProgressIndicator(
                color = GoldBright,
                strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun MoonControlCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xD9041226))
            .border(1.dp, GoldBright.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = GoldBright, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun LunarMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GoldPrimary.copy(alpha = 0.10f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = GoldBright,
            textAlign = TextAlign.Center
        )
    }
}
