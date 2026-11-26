package com.example.ui.screens.ecosystem

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.astronomy.CosmicEarthGlobeEngine
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
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class PropheticSundialReading(
    val solarAltitudeDeg: Double,
    val solarAzimuthDeg: Double,
    val shadowAzimuthDeg: Double,
    val shadowLengthMeters: Double?, // null when sun is below horizon
    val meridianShadowMeters: Double,
    val shafiAsrThresholdMeters: Double,
    val hanafiAsrThresholdMeters: Double,
    val isDaylight: Boolean,
    val fiqhPhaseAr: String,
    val fiqhPhaseEn: String,
    val fiqhDetailAr: String,
    val fiqhDetailEn: String,
    val daysToNextKaabaZenith: Int,
    val nextKaabaZenithLabelAr: String,
    val nextKaabaZenithLabelEn: String
)

/**
 * Pure astronomical Prophetic Sundial & Shadow-Length Engine.
 * Implements NOAA/Jean Meeus solar declination, equation of time, solar altitude/azimuth,
 * real gnomon shadow length (Fay' al-Zawal, Shafi'i 1x Asr, Hanafi 2x Asr),
 * and the annual Istiwa al-Shams (Solar Zenith over the Holy Kaaba) countdown.
 */
object PropheticSundialEngine {

    fun computeReading(
        latitudeDeg: Double,
        longitudeDeg: Double,
        calendar: Calendar,
        staffHeightMeters: Double = 1.0
    ): PropheticSundialReading {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val localHours = hour + minute / 60.0 + second / 3600.0
        val tzOffsetHours = calendar.timeZone.getOffset(calendar.timeInMillis) / 3_600_000.0

        // Fractional year in radians
        val gamma = (2.0 * PI / 365.0) * (dayOfYear - 1 + (localHours - 12.0) / 24.0)

        // Equation of time (minutes)
        val eqTimeMin = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) -
                0.040849 * sin(2.0 * gamma)
            )

        // Solar declination (radians)
        val declRad = 0.006918 -
            0.399912 * cos(gamma) +
            0.070257 * sin(gamma) -
            0.006758 * cos(2.0 * gamma) +
            0.000907 * sin(2.0 * gamma) -
            0.002697 * cos(3.0 * gamma) +
            0.00148 * sin(3.0 * gamma)

        val timeOffsetMin = eqTimeMin + 4.0 * longitudeDeg - 60.0 * tzOffsetHours
        val trueSolarTimeMin = ((localHours * 60.0 + timeOffsetMin) % 1440.0 + 1440.0) % 1440.0
        val hourAngleDeg = trueSolarTimeMin / 4.0 - 180.0
        val hourAngleRad = Math.toRadians(hourAngleDeg)
        val latRad = Math.toRadians(latitudeDeg.coerceIn(-89.5, 89.5))

        // Solar altitude
        val sinAlt = (sin(latRad) * sin(declRad) + cos(latRad) * cos(declRad) * cos(hourAngleRad)).coerceIn(-1.0, 1.0)
        val altRad = asin(sinAlt)
        val altDeg = Math.toDegrees(altRad)

        // Solar azimuth (clockwise from true North 0..360)
        val cosAz = ((sin(declRad) - sin(latRad) * sinAlt) / (cos(latRad) * cos(altRad).coerceAtLeast(1e-6))).coerceIn(-1.0, 1.0)
        var azDeg = Math.toDegrees(acos(cosAz))
        if (hourAngleDeg > 0.0) {
            azDeg = (360.0 - azDeg) % 360.0
        }
        val shadowAzDeg = (azDeg + 180.0) % 360.0

        // Meridian (Zawal) minimum shadow length for the given date & latitude
        val meridianZenithRad = abs(latRad - declRad).coerceAtMost(Math.toRadians(89.0))
        val meridianShadow = staffHeightMeters * tan(meridianZenithRad)
        val shafiAsrShadow = meridianShadow + staffHeightMeters
        val hanafiAsrShadow = meridianShadow + 2.0 * staffHeightMeters

        val isDaylight = altDeg > 0.5
        val shadowLen = if (isDaylight) {
            (staffHeightMeters / tan(altRad)).coerceAtMost(99.9)
        } else {
            null
        }

        val isAfternoon = hourAngleDeg > 0.0
        val (phaseAr, phaseEn, detailAr, detailEn) = when {
            altDeg <= -18.0 -> QuadSundial(
                "الليل العميق (العشاء والتهجد)",
                "Deep Night (Isha & Tahajjud)",
                "الشمس تحت الأفق بأكثر من ١٨° — لا ظلّ الآن، وهو وقت السكون وقيام الليل.",
                "Sun is >18° below the horizon — true astronomical night."
            )
            altDeg <= 0.5 && !isAfternoon -> QuadSundial(
                "الفجر الصادق والإسفار",
                "Dawn Twilight (Fajr)",
                "الشمس تقترب من الشروق تحت الأفق الشرقي — وقت صلاة الفجر وأذكار الصباح.",
                "Morning twilight before sunrise — Fajr prayer window."
            )
            altDeg <= 0.5 && isAfternoon -> QuadSundial(
                "الغروب والشفق الأحمر (المغرب)",
                "Sunset & Dusk (Maghrib)",
                "غربت الشمس واختفى الظل — دخل وقت صلاة المغرب وأذكار المساء.",
                "Sun has set below the western horizon — Maghrib window."
            )
            abs(hourAngleDeg) <= 2.5 -> QuadSundial(
                "فيء الزوال (استواء الشمس)",
                "Solar Meridian (Fay' al-Zawal)",
                "الظل في أقصر طول له اليوم (${String.format(Locale.US, "%.2f", meridianShadow)} م) — بمجرد أن يبدأ في الزيادة شرقاً يدخل الظهر.",
                "Shadow is at today's absolute minimum (${String.format(Locale.US, "%.2f", meridianShadow)} m) — Dhuhr begins as it lengthens east."
            )
            !isAfternoon -> QuadSundial(
                "وقت الضحى (الظل يتقلص غرباً)",
                "Duha Morning (Shadow Shrinking West)",
                "الشمس ترتفع شرقاً والظل ينكمش نحو الغرب حتى يبلغ فيء الزوال.",
                "Sun rises in the east while the shadow shrinks westward toward noon."
            )
            shadowLen != null && shadowLen < shafiAsrShadow -> QuadSundial(
                "وقت الظهر (الظل يمتد نحو الشرق)",
                "Dhuhr Time (Shadow Growing East)",
                "زالَت الشمس ويمتد الظل شرقاً — يبقى حتى يبلغ مثل طول الشاخص + فيء الزوال (${String.format(Locale.US, "%.2f", shafiAsrShadow)} م).",
                "Post-meridian Dhuhr window until shadow reaches 1x height + meridian shadow."
            )
            shadowLen != null && shadowLen < hanafiAsrShadow -> QuadSundial(
                "دخل العصر (الجمهور ١×) — قبل الحنفي (٢×)",
                "Asr Entered (Standard 1x) — Pre-Hanafi (2x)",
                "بلغ ظلّ كل شيء مثله مع فيء الزوال! دخل وقت العصر عند الشافعي ومالك وأحمد.",
                "Shadow passed 1x staff height + meridian shadow — Shafi'i/Maliki/Hanbali Asr."
            )
            else -> QuadSundial(
                "العصر المتفق عليه (تجاوز ٢× الظل)",
                "Universal Asr Window (> 2x Shadow)",
                "تجاوز الظل مثلَي طول الشاخص مع فيء الزوال (${String.format(Locale.US, "%.2f", hanafiAsrShadow)} م) — وقت العصر بالإجماع.",
                "Shadow exceeds 2x height + meridian shadow — Asr entered in all schools."
            )
        }

        // Compute days until next Solar Zenith directly over the Holy Kaaba (May 28 = day ~148, July 16 = day ~197)
        val (daysToZenith, zenithAr, zenithEn) = computeNextKaabaZenith(dayOfYear)

        return PropheticSundialReading(
            solarAltitudeDeg = altDeg,
            solarAzimuthDeg = azDeg,
            shadowAzimuthDeg = shadowAzDeg,
            shadowLengthMeters = shadowLen,
            meridianShadowMeters = meridianShadow,
            shafiAsrThresholdMeters = shafiAsrShadow,
            hanafiAsrThresholdMeters = hanafiAsrShadow,
            isDaylight = isDaylight,
            fiqhPhaseAr = phaseAr,
            fiqhPhaseEn = phaseEn,
            fiqhDetailAr = detailAr,
            fiqhDetailEn = detailEn,
            daysToNextKaabaZenith = daysToZenith,
            nextKaabaZenithLabelAr = zenithAr,
            nextKaabaZenithLabelEn = zenithEn
        )
    }

    private fun computeNextKaabaZenith(dayOfYear: Int): Triple<Int, String, String> {
        val may28 = 148
        val jul16 = 197
        return when {
            dayOfYear <= may28 -> Triple(may28 - dayOfYear, "٢٨ مايو (١٢:١٨ ظهراً بتوقيت مكة)", "May 28 (12:18 PM Makkah)")
            dayOfYear <= jul16 -> Triple(jul16 - dayOfYear, "١٦ يوليو (١٢:٢٧ ظهراً بتوقيت مكة)", "July 16 (12:27 PM Makkah)")
            else -> Triple((365 - dayOfYear) + may28, "٢٨ مايو القادم (١٢:١٨ ظهراً بتوقيت مكة)", "Next May 28 (12:18 PM Makkah)")
        }
    }
}

private data class QuadSundial(val a: String, val b: String, val c: String, val d: String)

@Composable
fun PropheticSundialScreen(
    latitude: Double,
    longitude: Double,
    cityLabel: String,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var useLiveTime by remember { mutableStateOf(true) }
    var scrubbedMinutesOfDay by remember { mutableFloatStateOf(780f) } // 13:00 default when scrubbing
    var staffHeightMeters by remember { mutableFloatStateOf(1.0f) }
    var compassAlignEnabled by remember { mutableStateOf(false) }
    var deviceHeadingDeg by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(useLiveTime) {
        while (useLiveTime) {
            nowMillis = System.currentTimeMillis()
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis }
            scrubbedMinutesOfDay = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)).toFloat()
            delay(1000L)
        }
    }

    // Real hardware compass sensor (rotates sundial live to match physical North when toggled on)
    DisposableEffect(compassAlignEnabled) {
        if (!compassAlignEnabled) {
            deviceHeadingDeg = 0f
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            val rotMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                SensorManager.getOrientation(rotMatrix, orientation)
                val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                deviceHeadingDeg = deg
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotSensor != null) {
            sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val effectiveCalendar = remember(nowMillis, useLiveTime, scrubbedMinutesOfDay) {
        Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = nowMillis
            if (!useLiveTime) {
                val totalMin = scrubbedMinutesOfDay.toInt().coerceIn(0, 1439)
                set(Calendar.HOUR_OF_DAY, totalMin / 60)
                set(Calendar.MINUTE, totalMin % 60)
                set(Calendar.SECOND, 0)
            }
        }
    }

    val reading = remember(latitude, longitude, effectiveCalendar.timeInMillis, staffHeightMeters) {
        PropheticSundialEngine.computeReading(
            latitudeDeg = latitude,
            longitudeDeg = longitude,
            calendar = effectiveCalendar,
            staffHeightMeters = staffHeightMeters.toDouble()
        )
    }
    val qiblaBearing = remember(latitude, longitude) {
        CosmicEarthGlobeEngine.qiblaBearingDegrees(latitude, longitude)
    }

    val displayHour = effectiveCalendar.get(Calendar.HOUR_OF_DAY)
    val displayMin = effectiveCalendar.get(Calendar.MINUTE)
    val timeString = String.format(Locale.US, "%02d:%02d", displayHour, displayMin)

    Column(modifier = Modifier.fillMaxSize().testTag("prophetic_sundial_screen")) {
        NoorScreenHeader(
            title = L10n.pick(lang, "المِزْولة الشمسية النبوية والظل الحي", "Live Prophetic Sundial & Shadow Lab"),
            subtitle = L10n.pick(
                lang,
                "حساب فلكي حقيقي لطول الظل وفيء الزوال ودخول الظهر والعصر في $cityLabel",
                "Real astronomical shadow length, Fay' al-Zawal & Qibla Sun zenith in $cityLabel"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Controls Row (Live vs Time Machine + Hardware Compass Alignment)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = if (useLiveTime) {
                            L10n.pick(lang, "الوقت الحقيقي الآن ($timeString)", "Live Now ($timeString)")
                        } else {
                            L10n.pick(lang, "رجوع للوقت الحقيقي", "Reset to Live Now")
                        },
                        selected = useLiveTime,
                        onClick = { useLiveTime = true },
                        modifier = Modifier.weight(1f).testTag("sundial_live_btn")
                    )
                    LiquidGlassChip(
                        label = L10n.pick(
                            lang,
                            if (compassAlignEnabled) "البوصلة الحية: مفعّلة" else "محاذاة ببوصلة الهاتف",
                            if (compassAlignEnabled) "Live Compass: ON" else "Align with Compass"
                        ),
                        selected = compassAlignEnabled,
                        onClick = { compassAlignEnabled = !compassAlignEnabled },
                        modifier = Modifier.weight(1f).testTag("sundial_compass_btn")
                    )
                }
            }

            // Royal Brass & Marble Interactive Sundial Canvas
            item {
                LiquidGlassCard(strong = true, goldBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF10372E),
                                            Color(0xFF071D19),
                                            Color(0xFF030D0B)
                                        )
                                    )
                                )
                                .border(2.5.dp, Brush.sweepGradient(listOf(GoldPrimary, GoldBright, Color(0xFF38BDF8), GoldPrimary)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val maxR = size.minDimension / 2f

                                rotate(degrees = if (compassAlignEnabled) -deviceHeadingDeg else 0f, pivot = Offset(cx, cy)) {
                                    // Outer Engraved Degree Ring
                                    drawCircle(
                                        color = GoldPrimary.copy(alpha = 0.4f),
                                        radius = maxR,
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    for (deg in 0 until 360 step 15) {
                                        val rad = Math.toRadians(deg - 90.0)
                                        val isMajor = deg % 90 == 0
                                        val inner = maxR * (if (isMajor) 0.86f else 0.93f)
                                        drawLine(
                                            color = if (isMajor) GoldBright else GoldPrimary.copy(alpha = 0.35f),
                                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                                            end = Offset(cx + (maxR * cos(rad)).toFloat(), cy + (maxR * sin(rad)).toFloat()),
                                            strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
                                        )
                                    }

                                    // Shadow Scale Mapping (0..4x staff height -> 0..0.82*maxR)
                                    val maxDisplayMeters = (staffHeightMeters * 3.8).coerceAtLeast(3.5)
                                    fun metersToRadius(m: Double): Float {
                                        return ((m / maxDisplayMeters).coerceIn(0.06, 0.86) * maxR).toFloat()
                                    }

                                    // Concentric Fiqh Shadow Rings:
                                    // 1) Meridian Shadow (Fay' al-Zawal)
                                    drawCircle(
                                        color = Color(0xFF38BDF8).copy(alpha = 0.42f),
                                        radius = metersToRadius(reading.meridianShadowMeters),
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                                    )
                                    // 2) Shafi'i Asr Ring (1x + Zawal)
                                    drawCircle(
                                        color = Color(0xFF34D399).copy(alpha = 0.52f),
                                        radius = metersToRadius(reading.shafiAsrThresholdMeters),
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                                    )
                                    // 3) Hanafi Asr Ring (2x + Zawal)
                                    drawCircle(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.52f),
                                        radius = metersToRadius(reading.hanafiAsrThresholdMeters),
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                                    )

                                    // Qibla Golden Sightline
                                    val qiblaRad = Math.toRadians(qiblaBearing - 90.0)
                                    drawLine(
                                        color = GoldBright.copy(alpha = 0.55f),
                                        start = Offset(cx, cy),
                                        end = Offset(cx + (maxR * 0.88f * cos(qiblaRad)).toFloat(), cy + (maxR * 0.88f * sin(qiblaRad)).toFloat()),
                                        strokeWidth = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                                    )

                                    // Real Cast Shadow Ray from Central Gnomon (during daylight)
                                    val shadowM = reading.shadowLengthMeters
                                    if (shadowM != null && reading.isDaylight) {
                                        val shadowRad = Math.toRadians(reading.shadowAzimuthDeg - 90.0)
                                        val sRadius = metersToRadius(shadowM)
                                        val endX = cx + (sRadius * cos(shadowRad)).toFloat()
                                        val endY = cy + (sRadius * sin(shadowRad)).toFloat()

                                        // Soft shadow halo
                                        drawLine(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            start = Offset(cx, cy),
                                            end = Offset(endX, endY),
                                            strokeWidth = 14.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        // Crisp shadow edge indicator
                                        drawLine(
                                            color = Color(0xFF94A3B8),
                                            start = Offset(cx, cy),
                                            end = Offset(endX, endY),
                                            strokeWidth = 4.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        drawCircle(
                                            color = GoldBright,
                                            radius = 5.dp.toPx(),
                                            center = Offset(endX, endY)
                                        )
                                    }

                                    // Real Live Sun Orb on its true Azimuth & Altitude!
                                    val sunAzRad = Math.toRadians(reading.solarAzimuthDeg - 90.0)
                                    val sunDistFromCenter = ((1.0 - (reading.solarAltitudeDeg.coerceIn(0.0, 90.0) / 90.0)) * maxR * 0.84f).toFloat()
                                    val sunX = cx + (sunDistFromCenter * cos(sunAzRad)).toFloat()
                                    val sunY = cy + (sunDistFromCenter * sin(sunAzRad)).toFloat()
                                    if (reading.isDaylight) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color.Transparent),
                                                center = Offset(sunX, sunY),
                                                radius = 22.dp.toPx()
                                            ),
                                            radius = 22.dp.toPx(),
                                            center = Offset(sunX, sunY)
                                        )
                                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(sunX, sunY))
                                    }

                                    // Central Royal Brass Gnomon Pin (الشاخص)
                                    drawCircle(color = GoldPrimary, radius = 9.dp.toPx(), center = Offset(cx, cy))
                                    drawCircle(color = EmeraldDeep, radius = 4.dp.toPx(), center = Offset(cx, cy))
                                }
                            }

                            // Cardinal Labels Overlay (when not compass-rotated)
                            if (!compassAlignEnabled) {
                                Text("ش N", color = GoldBright, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
                                Text("ج S", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
                                Text("شرق E", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
                                Text("غرب W", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Current Prophetic Fiqh Phase Banner
                        Text(
                            text = L10n.pick(lang, reading.fiqhPhaseAr, reading.fiqhPhaseEn),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldBright,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = L10n.pick(lang, reading.fiqhDetailAr, reading.fiqhDetailEn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Interactive Solar Time-Scrubber (Watch the shadow move from Sunrise to Sunset!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(lang, "آلة الزمن الشمسية (حرّك الوقت لمشاهدة الظل):", "Solar Time Scrubber:"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = timeString,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7DD3FC)
                            )
                        }
                        Slider(
                            value = scrubbedMinutesOfDay,
                            onValueChange = {
                                useLiveTime = false
                                scrubbedMinutesOfDay = it
                            },
                            valueRange = 300f..1200f, // 05:00 to 20:00
                            colors = SliderDefaults.colors(
                                thumbColor = GoldBright,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier.testTag("sundial_time_slider")
                        )

                        // Staff Height Selector (الشاخص)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.pick(lang, "طول الشاخص:", "Staff:"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            listOf(1.0f to "١ متر", 1.7f to "١.٧ م (قامة)", 2.0f to "٢ متر").forEach { (h, label) ->
                                LiquidGlassChip(
                                    label = label,
                                    selected = abs(staffHeightMeters - h) < 0.05f,
                                    onClick = { staffHeightMeters = h },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Real Telemetry Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SundialMetricCard(
                        label = L10n.pick(lang, "طول الظل الحي", "Live Shadow"),
                        value = reading.shadowLengthMeters?.let { String.format(Locale.US, "%.2f م", it) } ?: "—",
                        color = GoldBright,
                        modifier = Modifier.weight(1f)
                    )
                    SundialMetricCard(
                        label = L10n.pick(lang, "ارتفاع الشمس", "Sun Altitude"),
                        value = String.format(Locale.US, "%.1f°", reading.solarAltitudeDeg),
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    SundialMetricCard(
                        label = L10n.pick(lang, "فيء الزوال", "Noon Shadow"),
                        value = String.format(Locale.US, "%.2f م", reading.meridianShadowMeters),
                        color = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Annual Kaaba Solar Zenith (Istiwa al-Shams) Observatory Card
            item {
                LiquidGlassCard(goldBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "مرصد تعامد الشمس على الكعبة المشرفة", "Holy Kaaba Solar Zenith Observatory"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldBright
                            )
                        }
                        Text(
                            text = L10n.pick(
                                lang,
                                "متبقي ${reading.daysToNextKaabaZenith} يوماً على التعامد القادم: ${reading.nextKaabaZenithLabelAr}",
                                "${reading.daysToNextKaabaZenith} days until next Kaaba Zenith: ${reading.nextKaabaZenithLabelEn}"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7DD3FC)
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "تتعامد الشمس عمودياً فوق الكعبة المشرفة مرتين كل عام فيختفي ظلّها تماماً، ويتجه ظلّ أي شاخص في نصف الكرة المضاء عكس اتجاه القبلة بدقة ١٠٠٪ دون حاجة لبوصلة.",
                                "Twice each year the Sun passes directly overhead the Kaaba (0° shadow in Makkah), allowing anyone on the sunlit hemisphere to verify Qibla by watching a simple stick's shadow."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Authentic Hadith of Jibril on Prayer Shadows
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "الأساس النبوي لقياس أوقات الصلاة بالظل", "Prophetic Basis of Shadow Prayer Timing"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        Text(
                            text = "«أَمَّنِي جِبْرِيلُ عِنْدَ الْبَيْتِ مَرَّتَيْنِ: فَصَلَّى الظُّهْرَ حِينَ زَالَتِ الشَّمْسُ، وَصَلَّى الْعَصْرَ حِينَ كَانَ ظِلُّ كُلِّ شَيْءٍ مِثْلَهُ»",
                            fontFamily = AmiriFontFamily,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = L10n.pick(
                                lang,
                                "الحلقة الزرقاء = فيء الزوال • الخضراء = عصر الجمهور (١×) • الذهبية = العصر الحنفي (٢×) • الخط المتقطع = اتجاه القبلة.",
                                "Blue ring = Noon shadow • Green = Standard Asr (1x) • Gold = Hanafi Asr (2x) • Dashed ray = Qibla bearing."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SundialMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
