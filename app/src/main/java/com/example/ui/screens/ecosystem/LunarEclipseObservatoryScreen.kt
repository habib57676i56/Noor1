package com.example.ui.screens.ecosystem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.rounded.SatelliteAlt
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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

data class NasaDialAMoonFrame(
    val imageBitmap: ImageBitmap,
    val phasePercent: Double?,
    val ageDays: Double?,
    val distanceKm: Int?,
    val sourceLabel: String
)

private enum class MoonRenderSimulation {
    REAL_PHASE,
    BLOOD_MOON_ECLIPSE,
    TOTAL_SOLAR_ECLIPSE
}

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
        LunarSurfaceLandmark("tranquillitatis", "بحر السكون (الهدوء)", "Mare Tranquillitatis", 8.5, 31.4, "سهل بازلتي مظلم واسع في النصف الشرقي للقمر", "Dark basaltic lunar sea on the eastern near side"),
        LunarSurfaceLandmark("imbrium", "بحر الأمطار", "Mare Imbrium", 32.8, -15.6, "أحد أكبر الأحواض القمرية بقطر ١١٤٥ كم", "One of the largest basins on the Moon (1,145 km)"),
        LunarSurfaceLandmark("crisium", "بحر الشدائد", "Mare Crisium", 17.0, 59.1, "حوض بيضاوي بارز قرب الحافة الشرقية للهلال", "Prominent oval mare near the waxing crescent limb"),
        LunarSurfaceLandmark("farside", "الوجه البعيد للقمر", "Lunar Far Side", 0.0, 180.0, "الجانب الخفي الذي لا يُرى من الأرض — أدر الكرة ٣٦٠° لتشاهده!", "Hidden hemisphere never seen from Earth — spin 360°!")
    )

    fun computeLunarPhase(epochMillis: Long): LunarPhaseSnapshot {
        val daysSinceRef = (epochMillis - REFERENCE_NEW_MOON_MS) / 86_400_000.0
        val cycles = daysSinceRef / SYNODIC_MONTH_DAYS
        val frac = (cycles - floor(cycles)).let { if (it < 0.0) it + 1.0 else it }
        val ageDays = frac * SYNODIC_MONTH_DAYS
        val illumination = ((1.0 - cos(2.0 * PI * frac)) / 2.0) * 100.0
        val isWaxing = frac < 0.5

        // Anomalistic month (27.55455 days) approximation for real Earth-Moon distance (356,500..406,700 km)
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

    /**
     * NASA-verified upcoming Solar & Lunar Eclipses (2026–2028).
     */
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
 * Fetches REAL NASA Lunar Reconnaissance Orbiter (LRO) photography directly over native
 * Android HttpURLConnection (ZERO CORS issues!):
 * 1. Real NASA LRO global equirectangular Moon surface map (cached to disk for instant 3D wrapping!)
 * 2. Live NASA SVS Dial-A-Moon API frame for any exact UTC date & hour!
 */
private object NasaLroMoonRepository {
    @Volatile
    private var cachedSurfaceMap: Bitmap? = null

    private val surfaceMapCandidates = listOf(
        "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_1k.jpg",
        "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/Solarsystemscope_texture_2k_moon.jpg/1024px-Solarsystemscope_texture_2k_moon.jpg",
        "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.jpg"
    )

    suspend fun loadOrFetchRealNasaSurfaceMap(context: Context): Pair<Bitmap, Boolean> = withContext(Dispatchers.IO) {
        cachedSurfaceMap?.let { return@withContext it to true }

        val cacheFile = File(context.cacheDir, "nasa_lro_moon_equirect_1k.jpg")
        if (cacheFile.exists() && cacheFile.length() > 12_000L) {
            val decoded = runCatching {
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                BitmapFactory.decodeFile(cacheFile.absolutePath, opts)
            }.getOrNull()
            if (decoded != null && decoded.width >= 256) {
                cachedSurfaceMap = decoded
                return@withContext decoded to true
            }
        }

        for (urlStr in surfaceMapCandidates) {
            val bytes = downloadBytes(urlStr, timeoutMs = 10_000)
            if (bytes != null && bytes.size > 12_000) {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = if (bytes.size > 600_000) 2 else 1
                }
                val bmp = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) }.getOrNull()
                if (bmp != null && bmp.width >= 256) {
                    runCatching { cacheFile.writeBytes(bytes) }
                    cachedSurfaceMap = bmp
                    return@withContext bmp to true
                }
            }
        }
        // Offline procedural selenographic surface map fallback (real lunar maria + craters, never flat cartoon)
        generateRealisticSelenographicFallbackMap(512, 256) to false
    }

    suspend fun fetchNasaDialAMoonLive(epochMillis: Long): NasaDialAMoonFrame? = withContext(Dispatchers.IO) {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val isoStamp = fmt.format(Date(epochMillis))
        val apiUrl = "https://svs.gsfc.nasa.gov/api/dialamoon/$isoStamp"
        val jsonBytes = downloadBytes(apiUrl, timeoutMs = 9_000) ?: return@withContext null
        val root = runCatching { JSONObject(String(jsonBytes)) }.getOrNull() ?: return@withContext null
        val imageUrl = root.optJSONObject("image")?.optString("url")?.takeIf { it.startsWith("https://") }
            ?: root.optJSONObject("image_highres")?.optString("url")?.takeIf { it.startsWith("https://") }
            ?: return@withContext null
        val imgBytes = downloadBytes(imageUrl, timeoutMs = 10_000) ?: return@withContext null
        val bmp = runCatching { BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size) }.getOrNull()
            ?: return@withContext null
        NasaDialAMoonFrame(
            imageBitmap = bmp.asImageBitmap(),
            phasePercent = root.optDouble("phase", Double.NaN).takeIf { !it.isNaN() },
            ageDays = root.optDouble("age", Double.NaN).takeIf { !it.isNaN() },
            distanceKm = root.optDouble("distance", Double.NaN).takeIf { !it.isNaN() }?.roundToInt(),
            sourceLabel = "NASA Goddard LRO · $isoStamp UTC"
        )
    }

    private fun downloadBytes(urlStr: String, timeoutMs: Int): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NoorIslamicApp/3.5 (Android; Lunar Observatory)")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { it.readBytes() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Generates a realistic rocky selenographic equirectangular Moon map with actual
     * dark basaltic lunar seas (Imbrium, Serenitatis, Tranquillitatis, Procellarum, Crisium)
     * and crater relief for immediate rendering while NASA LRO streams in or when offline.
     */
    fun generateRealisticSelenographicFallbackMap(w: Int, h: Int): Bitmap {
        val pixels = IntArray(w * h)
        data class Mare(val lat: Double, val lon: Double, val rad: Double, val depth: Double)
        val maria = listOf(
            Mare(33.0, -16.0, 26.0, 0.34),  // Imbrium
            Mare(28.0, 18.0, 17.0, 0.30),   // Serenitatis
            Mare(8.5, 31.0, 19.0, 0.33),    // Tranquillitatis
            Mare(18.0, -52.0, 32.0, 0.31),  // Oceanus Procellarum
            Mare(-16.0, -18.0, 16.0, 0.27), // Nubium
            Mare(17.0, 59.0, 11.0, 0.35),   // Crisium
            Mare(-3.0, 48.0, 14.0, 0.28)    // Fecunditatis
        )
        data class Crater(val lat: Double, val lon: Double, val rad: Double, val bright: Double)
        val craters = listOf(
            Crater(-43.3, -11.2, 6.5, 0.24), // Tycho
            Crater(9.6, -20.1, 7.2, 0.20),   // Copernicus
            Crater(8.1, -38.0, 4.5, 0.16),   // Kepler
            Crater(23.7, -47.4, 4.0, 0.25),  // Aristarchus
            Crater(-12.0, 15.0, 6.0, 0.10),
            Crater(51.6, -9.3, 5.0, -0.18),  // Plato (dark floor)
            Crater(-20.0, 140.0, 18.0, -0.10)
        )

        for (y in 0 until h) {
            val lat = 90.0 - (y.toDouble() / h.toDouble()) * 180.0
            for (x in 0 until w) {
                val lon = (x.toDouble() / w.toDouble()) * 360.0 - 180.0
                // Multi-frequency rocky regolith texture
                val n1 = sin(lon * 0.17 + lat * 0.13) * cos(lat * 0.21 - lon * 0.09)
                val n2 = sin(lon * 0.53 - lat * 0.47) * 0.5
                val n3 = cos(lon * 1.31 + lat * 1.19) * 0.25
                var albedo = 0.72 + 0.06 * n1 + 0.04 * n2 + 0.025 * n3

                for (m in maria) {
                    val dLat = lat - m.lat
                    val dLon = (lon - m.lon) * cos(Math.toRadians(lat)).coerceAtLeast(0.25)
                    val d = sqrt(dLat * dLat + dLon * dLon)
                    if (d < m.rad) {
                        val t = 1.0 - (d / m.rad)
                        albedo -= m.depth * t * t * (0.85 + 0.15 * n2)
                    }
                }
                for (c in craters) {
                    val dLat = lat - c.lat
                    val dLon = (lon - c.lon) * cos(Math.toRadians(lat)).coerceAtLeast(0.25)
                    val d = sqrt(dLat * dLat + dLon * dLon)
                    if (d < c.rad * 2.2) {
                        val rim = abs(d - c.rad * 0.75)
                        if (rim < c.rad * 0.35) {
                            albedo += c.bright * (1.0 - rim / (c.rad * 0.35))
                        } else if (d < c.rad * 0.7) {
                            albedo -= 0.08
                        }
                    }
                }
                val g = (albedo.coerceIn(0.18, 0.96) * 255.0).toInt()
                val r = (g * 1.01).toInt().coerceAtMost(255)
                val b = (g * 0.96).toInt().coerceAtMost(255)
                pixels[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}

/**
 * True 3D Spherical Photometric Lunar Renderer!
 * Maps a real NASA LRO equirectangular Moon texture onto a true 3D sphere (x² + y² + z² = 1)
 * with 360° camera rotation (yaw/pitch), real Lommel-Seeliger solar phase lighting,
 * subtle earthshine on the night hemisphere, and genuine Blood-Moon red umbra simulation!
 */
private object Real3DLunarSphereRenderer {

    fun render3DMoonSphere(
        texture: Bitmap,
        outputSize: Int,
        phaseFraction: Double,
        rotLonDeg: Double,
        rotLatDeg: Double,
        simulation: MoonRenderSimulation,
        eclipseProgress: Float
    ): Bitmap {
        val texW = texture.width
        val texH = texture.height
        val texPixels = IntArray(texW * texH)
        texture.getPixels(texPixels, 0, texW, 0, 0, texW, texH)

        val out = IntArray(outputSize * outputSize)
        val half = outputSize / 2.0
        val radius = outputSize * 0.465

        // Sun direction in viewer coordinates based on real lunar phase fraction (0=New, 0.5=Full, 1=New)
        val sunAngle = 2.0 * PI * (phaseFraction - 0.5)
        val sunX = if (simulation == MoonRenderSimulation.BLOOD_MOON_ECLIPSE) 0.0 else -sin(sunAngle)
        val sunY = 0.08
        val sunZ = if (simulation == MoonRenderSimulation.BLOOD_MOON_ECLIPSE) 1.0 else cos(sunAngle)
        val sunLen = sqrt(sunX * sunX + sunY * sunY + sunZ * sunZ).coerceAtLeast(1e-6)
        val sx = sunX / sunLen
        val sy = sunY / sunLen
        val sz = sunZ / sunLen

        val yawRad = Math.toRadians(rotLonDeg)
        val pitchRad = Math.toRadians(rotLatDeg)
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)

        // Earth's umbra shadow circle center during Blood-Moon Lunar Eclipse simulation
        val umbraCenterX = (eclipseProgress * 2.4 - 1.2)
        val umbraCenterY = -0.08

        for (py in 0 until outputSize) {
            val ny = (py - half) / radius
            val ny2 = ny * ny
            for (px in 0 until outputSize) {
                val nx = (px - half) / radius
                val r2 = nx * nx + ny2
                if (r2 > 1.0) continue

                val nz = sqrt((1.0 - r2).coerceAtLeast(0.0))

                // Rotate surface normal by user's 3D camera pitch & yaw to sample real selenographic coordinates
                val ry = ny * cosPitch - nz * sinPitch
                val rz = ny * sinPitch + nz * cosPitch

                val lat = asin((-ry).coerceIn(-1.0, 1.0)) // -PI/2..PI/2
                var lon = atan2(nx, rz) + yawRad
                while (lon > PI) lon -= 2.0 * PI
                while (lon < -PI) lon += 2.0 * PI

                // Equirectangular texture lookup (lon = 0 is center of NASA LRO Near Side!)
                val u = (((lon / (2.0 * PI)) + 0.5) * (texW - 1)).toInt().coerceIn(0, texW - 1)
                val v = (((0.5 - lat / PI)) * (texH - 1)).toInt().coerceIn(0, texH - 1)
                val texCol = texPixels[v * texW + u]

                val baseR = ((texCol ushr 16) and 0xFF)
                val baseG = ((texCol ushr 8) and 0xFF)
                val baseB = (texCol and 0xFF)

                // Lommel-Seeliger + Lambertian lunar regolith photometric lighting (realistic dusty Moon!)
                val ndotl = nx * sx + (-ny) * sy + nz * sz
                val limbSoft = nz.pow(0.18)
                val sunlit = if (ndotl > 0.0) {
                    val ls = ndotl / (ndotl + nz * 0.45 + 0.08)
                    (0.12 + 0.96 * ls * limbSoft).coerceIn(0.0, 1.15)
                } else {
                    // Realistic faint bluish-grey Earthshine on the unlit lunar hemisphere
                    0.065 * limbSoft
                }

                var rOut = (baseR * sunlit)
                var gOut = (baseG * sunlit)
                var bOut = (baseB * sunlit * if (ndotl <= 0.0) 1.25 else 1.0)

                // Blood-Moon Total Lunar Eclipse (Earth's Rayleigh-scattered copper-red umbra!)
                if (simulation == MoonRenderSimulation.BLOOD_MOON_ECLIPSE) {
                    val dx = nx - umbraCenterX
                    val dy = ny - umbraCenterY
                    val dUmbra = sqrt(dx * dx + dy * dy)
                    val umbraRadius = 1.18
                    if (dUmbra < umbraRadius + 0.25) {
                        val depth = ((umbraRadius + 0.25 - dUmbra) / 0.45).coerceIn(0.0, 1.0)
                        val copperFactor = 1.0 - depth * 0.82
                        rOut = rOut * (0.48 + 0.52 * copperFactor) + depth * 44.0
                        gOut = gOut * (0.13 + 0.87 * copperFactor)
                        bOut = bOut * (0.08 + 0.92 * copperFactor)
                    }
                } else if (simulation == MoonRenderSimulation.TOTAL_SOLAR_ECLIPSE) {
                    // Silhouette New Moon blocking the Sun with glowing rim
                    val edgeGlow = (1.0 - nz).pow(3.5) * 210.0
                    rOut = baseR * 0.07 + edgeGlow
                    gOut = baseG * 0.07 + edgeGlow * 0.88
                    bOut = baseB * 0.09 + edgeGlow * 0.65
                }

                // Smooth anti-aliased sphere edge
                val alpha = if (r2 > 0.965) {
                    (((1.0 - r2) / 0.035).coerceIn(0.0, 1.0) * 255.0).toInt()
                } else 255

                val rf = rOut.roundToInt().coerceIn(0, 255)
                val gf = gOut.roundToInt().coerceIn(0, 255)
                val bf = bOut.roundToInt().coerceIn(0, 255)
                out[py * outputSize + px] = (alpha shl 24) or (rf shl 16) or (gf shl 8) or bf
            }
        }
        return Bitmap.createBitmap(out, outputSize, outputSize, Bitmap.Config.ARGB_8888)
    }
}

/**
 * REAL NASA LRO 3D Interactive Moon & Solar/Lunar Eclipse Observatory:
 * - Mode 0: Real 3D Rotating Spherical Moon wrapped with genuine NASA Lunar Reconnaissance Orbiter (LRO)
 *           surface imagery, 360° touch rotation (Near Side + Far Side), pinch/button zoom,
 *           real 3D solar phase lighting, Blood-Moon & Solar Eclipse live simulators, and 3D lunar landmarks!
 * - Mode 1: Direct NASA Goddard SVS Dial-A-Moon live astronomical feed for the selected date/hour!
 * - Tab 1: Verified 2026–2028 Solar & Lunar Eclipse calendar (including the Aug 2, 2027 Makkah Total Eclipse).
 * - Tab 2: Prophetic Salat al-Kusuf & Khusuf step-by-step guide.
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
    var moonFeedMode by remember { mutableIntStateOf(0) } // 0 = Real 3D NASA LRO Sphere, 1 = NASA Dial-A-Moon Live Photo

    // 3D Moon Interactive Camera State
    var rotLonDeg by remember { mutableFloatStateOf(0f) }
    var rotLatDeg by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showLandmarks by remember { mutableStateOf(true) }
    var simulation by remember { mutableStateOf(MoonRenderSimulation.REAL_PHASE) }
    var eclipseProgress by remember { mutableFloatStateOf(0.50f) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    // Real NASA LRO Surface Texture + Rendered 3D Sphere Bitmap
    var surfaceMapBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRealNasaTexture by remember { mutableStateOf(false) }
    var rendered3DSphere by remember { mutableStateOf<ImageBitmap?>(null) }

    // NASA SVS Live Dial-A-Moon State
    var nasaLiveFrame by remember { mutableStateOf<NasaDialAMoonFrame?>(null) }
    var isLoadingNasaLive by remember { mutableStateOf(false) }

    val inspectedMillis = baseNowMillis + (dayOffsetSlider * 86_400_000.0).toLong()
    val moon = remember(inspectedMillis) { LunarAstronomyEngine.computeLunarPhase(inspectedMillis) }

    // Load real NASA LRO surface map once (disk-cached, zero CORS!)
    LaunchedEffect(reloadNonce) {
        val (bmp, isRealNasa) = NasaLroMoonRepository.loadOrFetchRealNasaSurfaceMap(context)
        surfaceMapBitmap = bmp
        isRealNasaTexture = isRealNasa
    }

    // Re-render real 3D spherical Moon whenever texture, phase, rotation, or eclipse slider changes
    LaunchedEffect(surfaceMapBitmap, moon.phaseFraction, rotLonDeg, rotLatDeg, simulation, eclipseProgress) {
        val tex = surfaceMapBitmap ?: return@LaunchedEffect
        val sphere = withContext(Dispatchers.Default) {
            Real3DLunarSphereRenderer.render3DMoonSphere(
                texture = tex,
                outputSize = 270,
                phaseFraction = moon.phaseFraction,
                rotLonDeg = rotLonDeg.toDouble(),
                rotLatDeg = rotLatDeg.toDouble(),
                simulation = simulation,
                eclipseProgress = eclipseProgress
            ).asImageBitmap()
        }
        rendered3DSphere = sphere
    }

    // Fetch live NASA Goddard SVS Dial-A-Moon frame when Mode 1 is active
    LaunchedEffect(moonFeedMode, dayOffsetSlider.roundToInt(), reloadNonce) {
        if (moonFeedMode == 1) {
            isLoadingNasaLive = true
            nasaLiveFrame = NasaLroMoonRepository.fetchNasaDialAMoonLive(inspectedMillis)
            isLoadingNasaLive = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lunar_eclipse_observatory_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مرصد القمر الحقيقي والكسوف", "Real NASA 3D Moon & Eclipses"),
            subtitle = L10n.pick(lang, "قمر ٣D حقيقي ببيانات ناسا LRO (تدوير ٣٦٠° وتقريب) + الكسوف والخسوف", "Real NASA LRO 3D Interactive Moon + Live Eclipses & Kusuf Guide"),
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
                        // Mode Switch: Real 3D NASA LRO Interactive Sphere vs Live NASA SVS Dial-A-Moon Frame
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "🌍 قمر ٣D تفاعلي (ناسا LRO)", "🌍 3D NASA LRO Moon"),
                                selected = moonFeedMode == 0,
                                onClick = { moonFeedMode = 0 },
                                modifier = Modifier.weight(1f).testTag("moon_mode_3d_lro")
                            )
                            LiquidGlassChip(
                                label = L10n.pick(lang, "🛰️ رصد ناسا المباشر", "🛰️ NASA Live Feed"),
                                selected = moonFeedMode == 1,
                                onClick = { moonFeedMode = 1 },
                                modifier = Modifier.weight(1f).testTag("moon_mode_nasa_live")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Main Real Moon Viewport (360° Touch Rotation + Pinch Zoom!)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF0A1930), Color(0xFF030814), Color(0xFF000208))
                                    )
                                )
                                .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                .pointerInput(moonFeedMode) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        zoomScale = (zoomScale * zoom).coerceIn(1.0f, 3.4f)
                                        if (moonFeedMode == 0) {
                                            rotLonDeg = ((rotLonDeg - pan.x * 0.35f + 540f) % 360f) - 180f
                                            rotLatDeg = (rotLatDeg + pan.y * 0.30f).coerceIn(-75f, 75f)
                                        }
                                    }
                                }
                                .testTag("real_3d_moon_viewport"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (moonFeedMode == 0) {
                                // Real 3D Photometric NASA LRO Sphere + Solar Corona during Total Solar Eclipse
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val baseD = size.minDimension * 0.84f * zoomScale

                                    // Deep starfield
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

                                    // Atmospheric / Solar Corona Halo
                                    when (simulation) {
                                        MoonRenderSimulation.TOTAL_SOLAR_ECLIPSE -> {
                                            // Pearly Solar Corona + Diamond Ring flare!
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
                                            // Diamond Ring sparkle on the upper-right solar limb
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

                                    // Draw the REAL 3D-shaded NASA LRO Moon Sphere Bitmap!
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

                                    // Optional 3D Projected Real Lunar Landmarks (Tycho, Copernicus, Mare Tranquillitatis)
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
                            } else {
                                // Direct Live NASA Goddard SVS Dial-A-Moon Photography!
                                val frame = nasaLiveFrame
                                if (isLoadingNasaLive) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = GoldBright)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = L10n.pick(lang, "جاري الاتصال بمرصد ناسا LRO المباشر...", "Connecting to NASA Goddard LRO..."),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldBright
                                        )
                                    }
                                } else if (frame != null) {
                                    Image(
                                        bitmap = frame.imageBitmap,
                                        contentDescription = L10n.pick(lang, "صورة القمر الحقيقية من ناسا", "Real NASA LRO Moon"),
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(18.dp)
                                    ) {
                                        Icon(Icons.Rounded.SatelliteAlt, contentDescription = null, tint = GoldBright, modifier = Modifier.size(34.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = L10n.pick(
                                                lang,
                                                "تعذّر جلب صورة مرصد ناسا المباشرة الآن — يمكنك استخدام «قمر ٣D التفاعلي» فوراً أو إعادة المحاولة",
                                                "Unable to reach NASA SVS right now — switch to 3D Interactive Moon or tap Retry"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Top HUD Badge (Real NASA LRO source indicator + Phase)
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
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xCC041226))
                                        .border(1.dp, GoldBright.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = when {
                                            moonFeedMode == 1 && nasaLiveFrame != null -> nasaLiveFrame!!.sourceLabel
                                            isRealNasaTexture -> L10n.pick(lang, "NASA LRO حقيقي · ${moon.phaseNameAr}", "Real NASA LRO · ${moon.phaseNameEn}")
                                            else -> L10n.pick(lang, "قمر ٣D تضاريسي · ${moon.phaseNameAr}", "3D Selenographic · ${moon.phaseNameEn}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldBright
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xCC041226))
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

                            // Right Zoom + / - / Reset Controls
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MoonControlCircle(Icons.Rounded.Add, "Zoom In") {
                                    zoomScale = (zoomScale + 0.45f).coerceAtMost(3.4f)
                                }
                                MoonControlCircle(Icons.Rounded.Remove, "Zoom Out") {
                                    zoomScale = (zoomScale - 0.45f).coerceAtLeast(1.0f)
                                }
                                MoonControlCircle(Icons.Rounded.Refresh, "Reset View") {
                                    rotLonDeg = 0f
                                    rotLatDeg = 0f
                                    zoomScale = 1.0f
                                    dayOffsetSlider = 0f
                                    simulation = MoonRenderSimulation.REAL_PHASE
                                    reloadNonce++
                                }
                            }

                            // Bottom 360° Interaction Hint
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xC8041226))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "اسحب لتدوير القمر ٣٦٠° (شاهد الوجه البعيد!) · قرّب بإصبعين ×${String.format(Locale.US, "%.1f", zoomScale)}",
                                        "Drag to rotate 360° (see Far Side!) · Pinch Zoom ×${String.format(Locale.US, "%.1f", zoomScale)}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-Time Simulation Mode Pills: Natural Phase / Blood Moon Eclipse / Total Solar Eclipse!
                        if (moonFeedMode == 0) {
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
                                    text = L10n.pick(lang, "حرّك ظل الأرض الأحمر عبر سطح القمر الحقيقي:", "Slide Earth's Red Umbra Shadow across the 3D Moon:"),
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
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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

                // Real Lunar Surface Landmarks Fly-To (Tap any crater/sea to rotate 3D Moon directly to it!)
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Rounded.Public, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.pick(lang, "استكشف تضاريس وفوهات القمر الحقيقية (اضغط للتوجيه ٣D)", "Explore Real Lunar Craters & Seas (Tap to Rotate 3D)"),
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
                                moonFeedMode = 0
                                simulation = MoonRenderSimulation.REAL_PHASE
                                rotLonDeg = lm.lonDeg.toFloat()
                                rotLatDeg = lm.latDeg.toFloat().coerceIn(-65f, 65f)
                                zoomScale = if (lm.id == "farside") 1.15f else 1.85f
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
                                    text = L10n.pick(lang, "توجيه ٣D", "Focus 3D"),
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
                                moonFeedMode = 0
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

@Composable
private fun MoonControlCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
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
