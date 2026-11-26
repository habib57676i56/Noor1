package com.example.ui.screens.ecosystem

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness2
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.LocalNoorColors
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

/**
 * Astronomical Lunar Phase & Eclipse Observatory Engine (Synodic Month = 29.530588853 days).
 * Computes real-time Lunar Age, Illumination %, Distance (km), Ayyam al-Bid countdown,
 * and NASA-verified Solar & Lunar Eclipse calendar (2026–2028) + Salat al-Kusuf/Khusuf guide.
 */
object LunarAstronomyEngine {
    const val SYNODIC_MONTH_DAYS = 29.530588853
    // Known reference New Moon epoch: 2000-01-06 18:14 UTC
    private const val REFERENCE_NEW_MOON_MS = 947182440000L

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
 * Real-Time 3D-Shaded Lunar Phase Observatory + Synodic 30-Day Scrubber +
 * NASA-Verified Solar/Lunar Eclipse Calendar (including the Historic Aug 2, 2027 Makkah Total Eclipse!)
 * + Prophetic Salat al-Kusuf & Khusuf Guide.
 */
@Composable
fun LunarEclipseObservatoryScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val baseNowMillis = remember { System.currentTimeMillis() }
    var dayOffsetSlider by remember { mutableFloatStateOf(0f) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Live Moon & 30-Day Lab, 1 = Eclipses, 2 = Salat al-Kusuf

    val inspectedMillis = baseNowMillis + (dayOffsetSlider * 86_400_000.0).toLong()
    val moon = remember(inspectedMillis) { LunarAstronomyEngine.computeLunarPhase(inspectedMillis) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lunar_eclipse_observatory_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مرصد القمر والكسوف والخسوف", "Moon & Eclipse Observatory"),
            subtitle = L10n.pick(lang, "أطوار القمر الحية + الأيام البيض + كسوف مكة ٢٠٢٧ وصلاة الكسوف", "Live Shaded Moon + Ayyam al-Bid + Verified Eclipses & Kusuf Guide"),
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
                        label = L10n.pick(lang, "القمر الحي", "Live Moon"),
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "مواعيد الكسوف", "Eclipses"),
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "صلاة الكسوف", "Kusuf Guide"),
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (activeTab == 0) {
                item {
                    LiquidGlassCard(contentPadding = PaddingValues(16.dp)) {
                        // Real-time 3D Shaded Moon Disc Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF0E2342), Color(0xFF040B1A), Color(0xFF01040B))
                                    )
                                )
                                .border(1.2.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val r = size.minDimension * 0.36f

                                // Subtle starfield
                                val starSeeds = listOf(
                                    0.12f to 0.18f, 0.85f to 0.15f, 0.22f to 0.78f, 0.88f to 0.72f,
                                    0.15f to 0.48f, 0.78f to 0.35f, 0.35f to 0.14f, 0.65f to 0.86f
                                )
                                starSeeds.forEach { (sx, sy) ->
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.55f),
                                        radius = 2.2f,
                                        center = Offset(size.width * sx, size.height * sy)
                                    )
                                }

                                // Soft lunar atmospheric halo proportional to illumination
                                val haloAlpha = (moon.illuminationPercent / 100.0).toFloat() * 0.38f + 0.08f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFFFDE68A).copy(alpha = haloAlpha), Color.Transparent),
                                        center = Offset(cx, cy),
                                        radius = r * 1.48f
                                    ),
                                    radius = r * 1.48f,
                                    center = Offset(cx, cy)
                                )

                                // Unlit dark hemisphere (earthshine)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFF1F2937), Color(0xFF0F172A)),
                                        center = Offset(cx, cy),
                                        radius = r
                                    ),
                                    radius = r,
                                    center = Offset(cx, cy)
                                )

                                // Lit lunar hemisphere using true elliptical terminator projection
                                val phase = moon.phaseFraction // 0..1
                                val litColor = Brush.radialGradient(
                                    listOf(Color(0xFFFFFBEB), Color(0xFFFEF08A), Color(0xFFD6C7A1)),
                                    center = Offset(cx - r * 0.15f, cy - r * 0.15f),
                                    radius = r * 1.2f
                                )
                                val litPath = Path()
                                val steps = 64
                                val termXScale = cos(2.0 * PI * phase).toFloat() // +1 new, 0 quarter, -1 full
                                if (phase <= 0.5) {
                                    // Waxing: right limb lit
                                    for (i in 0..steps) {
                                        val theta = -PI / 2.0 + (PI * i / steps)
                                        val x = cx + (r * cos(theta)).toFloat()
                                        val y = cy + (r * sin(theta)).toFloat()
                                        if (i == 0) litPath.moveTo(x, y) else litPath.lineTo(x, y)
                                    }
                                    for (i in steps downTo 0) {
                                        val theta = -PI / 2.0 + (PI * i / steps)
                                        val x = cx + (r * termXScale * cos(theta)).toFloat()
                                        val y = cy + (r * sin(theta)).toFloat()
                                        litPath.lineTo(x, y)
                                    }
                                    litPath.close()
                                } else {
                                    // Waning: left limb lit
                                    for (i in 0..steps) {
                                        val theta = -PI / 2.0 + (PI * i / steps)
                                        val x = cx - (r * cos(theta)).toFloat()
                                        val y = cy + (r * sin(theta)).toFloat()
                                        if (i == 0) litPath.moveTo(x, y) else litPath.lineTo(x, y)
                                    }
                                    for (i in steps downTo 0) {
                                        val theta = -PI / 2.0 + (PI * i / steps)
                                        val x = cx - (r * termXScale * cos(theta)).toFloat()
                                        val y = cy + (r * sin(theta)).toFloat()
                                        litPath.lineTo(x, y)
                                    }
                                    litPath.close()
                                }
                                drawPath(litPath, brush = litColor)

                                // Subtle lunar maria / craters overlay for realism
                                val craters = listOf(
                                    Triple(-0.26f, -0.18f, 0.22f),
                                    Triple(0.18f, -0.25f, 0.16f),
                                    Triple(0.28f, 0.14f, 0.19f),
                                    Triple(-0.15f, 0.30f, 0.14f)
                                )
                                craters.forEach { (ox, oy, cr) ->
                                    drawCircle(
                                        color = Color(0x20334155),
                                        radius = r * cr,
                                        center = Offset(cx + r * ox, cy + r * oy)
                                    )
                                }

                                // Crisp lunar rim
                                drawCircle(
                                    color = GoldBright.copy(alpha = 0.45f),
                                    radius = r,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 1.6f)
                                )
                            }

                            // Top Phase Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xCC061529))
                                    .border(1.dp, GoldBright.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = L10n.pick(lang, moon.phaseNameAr, moon.phaseNameEn),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBright
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Telemetry Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarMetricTile(
                                label = L10n.pick(lang, "الإضاءة", "Illumination"),
                                value = String.format(Locale.US, "%.1f%%", moon.illuminationPercent),
                                modifier = Modifier.weight(1f)
                            )
                            LunarMetricTile(
                                label = L10n.pick(lang, "عمر القمر", "Lunar Age"),
                                value = String.format(Locale.US, "%.1f ي", moon.ageDays),
                                modifier = Modifier.weight(1f)
                            )
                            LunarMetricTile(
                                label = L10n.pick(lang, "البُعد عن الأرض", "Distance"),
                                value = String.format(Locale.US, "%,d كم", moon.distanceKm),
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
                                text = L10n.pick(lang, "محاكي الشهر القمري (٣٠ يوماً)", "30-Day Lunar Cycle Scrubber"),
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
                            onValueChange = { dayOffsetSlider = it },
                            valueRange = -15f..15f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldBright,
                                activeTrackColor = GoldBright,
                                inactiveTrackColor = GoldPrimary.copy(alpha = 0.25f)
                            )
                        )

                        // Ayyam al-Bid Fasting Banner
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
                                Icon(
                                    imageVector = Icons.Rounded.NightsStay,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (moon.isAyyamAlBidNow) {
                                        L10n.pick(
                                            lang,
                                            "أنت الآن في الأيام البيض المباركة (١٣–١٤–١٥) — يُسن صيامها كصيام الدهر!",
                                            "Ayyam al-Bid (White Days 13–15) are active now — Sunnah fasting!"
                                        )
                                    } else {
                                        L10n.pick(
                                            lang,
                                            "متبقي تقريباً ${moon.daysToAyyamAlBid} يوماً على صيام الأيام البيض القادمة",
                                            "~${moon.daysToAyyamAlBid} days until the next Sunnah White Days (13–15)"
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else if (activeTab == 1) {
                items(LunarAstronomyEngine.upcomingEclipses, key = { it.id }) { ev ->
                    val daysLeft = remember(ev.id) { LunarAstronomyEngine.daysUntil(ev, baseNowMillis) }
                    val accent = if (ev.isSolar) GoldBright else Color(0xFFF87171)
                    LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
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
