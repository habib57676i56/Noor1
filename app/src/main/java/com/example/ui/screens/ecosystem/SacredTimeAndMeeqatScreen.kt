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
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AsrMadhhab
import com.example.domain.model.CalculationMethod
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalIs24HourFormat
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class SacredMeeqatSnapshot(
    val maghribMillis: Long,
    val nextFajrMillis: Long,
    val islamicMidnightMillis: Long,
    val lastThirdStartMillis: Long,
    val duhaStartMillis: Long,
    val duhaPeakMillis: Long,
    val zawalStartMillis: Long,
    val dhuhrMillis: Long,
    val asrMillis: Long,
    val fridayGoldenHourStartMillis: Long,
    val isInLastThirdNow: Boolean,
    val secondsUntilLastThird: Long,
    val activeWorshipLabelAr: String,
    val activeWorshipLabelEn: String,
    val activeWorshipAdviceAr: String,
    val activeWorshipAdviceEn: String
)

object SacredMeeqatCalculator {
    /**
     * Computes exact Islamic Night Thirds, Islamic Midnight, Duha, Zawal,
     * and Friday last-hour golden window from true astronomical prayer timestamps.
     */
    fun computeSnapshot(
        latitude: Double,
        longitude: Double,
        nowMillis: Long = System.currentTimeMillis(),
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA
    ): SacredMeeqatSnapshot {
        val tz = java.util.TimeZone.getDefault()
        fun calcDay(cal: Calendar) = PrayerTimeCalculator.calculateForDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = latitude,
            longitude = longitude,
            timeZone = tz,
            method = method,
            madhhab = AsrMadhhab.SHAFI
        )

        val todayCal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        val todayTimes = calcDay(todayCal)

        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowTimes = calcDay(tomorrowCal)

        val yesterdayCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayTimes = calcDay(yesterdayCal)

        // Determine active night interval (either last night before today's Fajr, or tonight's Maghrib -> tomorrow's Fajr)
        val (nightStart, nightEnd) = if (nowMillis < todayTimes.fajrMillis) {
            yesterdayTimes.maghribMillis to todayTimes.fajrMillis
        } else {
            todayTimes.maghribMillis to tomorrowTimes.fajrMillis
        }

        val nightDuration = (nightEnd - nightStart).coerceAtLeast(3600_000L)
        val islamicMidnight = nightStart + (nightDuration / 2L)
        val lastThirdStart = nightEnd - (nightDuration / 3L)

        val duhaStart = todayTimes.sunriseMillis + 15L * 60_000L
        val zawalStart = todayTimes.dhuhrMillis - 10L * 60_000L
        val duhaPeak = duhaStart + ((zawalStart - duhaStart).coerceAtLeast(600_000L) * 2L / 3L)
        val fridayGoldenStart = todayTimes.maghribMillis - 60L * 60_000L

        val isInLastThird = nowMillis in lastThirdStart until nightEnd
        val secsToLastThird = if (isInLastThird) {
            ((nightEnd - nowMillis) / 1000L).coerceAtLeast(0L)
        } else {
            ((lastThirdStart - nowMillis) / 1000L).coerceAtLeast(0L)
        }

        val (labelAr, labelEn, adviceAr, adviceEn) = when {
            isInLastThird -> Quad(
                "أنت الآن في الثلث الأخير من الليل!",
                "You Are in the Last Third of the Night!",
                "وقت النزول الإلهي والاستغفار بالأسحار — صلِّ ركعتين وألحّ في الدعاء فإنه وقت إجابة عظيم.",
                "The blessed Tahajjud & Istighfar hour — pray two rak'ahs and supplicate earnestly."
            )
            nowMillis in todayTimes.fajrMillis until todayTimes.sunriseMillis -> Quad(
                "وقت الفجر وأذكار الصباح الذهبية",
                "Fajr & Golden Morning Adhkar",
                "مَن صلّى الفجر في جماعة ثم قعد يذكر الله حتى تطلع الشمس ثم صلّى ركعتين كانت له كأجر حجة وعمرة.",
                "Recite Morning Adhkar and Quran until 15 minutes after sunrise for Ishraq prayer."
            )
            nowMillis in duhaStart until zawalStart -> Quad(
                "وقت صلاة الضحى (صلاة الأوّابين)",
                "Duha Prayer Window (Salat al-Awwabin)",
                "يصبح على كل سُلامى من أحدكم صدقة، ويجزئ من ذلك ركعتان يركعهما من الضحى.",
                "A blessed window for Duha prayer — two rak'ahs suffice as charity for every joint."
            )
            nowMillis in zawalStart until todayTimes.dhuhrMillis -> Quad(
                "وقت الزوال (كراهة النافلة المطلقة)",
                "Solar Zenith (Avoid Voluntary Prayer)",
                "تتوسط الشمس كبد السماء — استعد لصلاة الظهر بذكر الله والوضوء.",
                "Sun is at meridian zenith — prepare for Dhuhr with dhikr and wudu."
            )
            nowMillis in todayTimes.asrMillis until todayTimes.maghribMillis -> Quad(
                "أذكار المساء والساعة المباركة قبل المغرب",
                "Evening Adhkar & Pre-Maghrib Sanctuary",
                "اقرأ أذكار المساء وأكثر من التسبيح قبل غروب الشمس ﴿وَسَبِّحْ بِحَمْدِ رَبِّكَ قَبْلَ طُلُوعِ الشَّمْسِ وَقَبْلَ الْغُرُوبِ﴾.",
                "Recite Evening Adhkar and glorify Allah before sunset."
            )
            else -> Quad(
                "أول الليل والوتر وقيام الليل",
                "Night Worship & Witr Preparation",
                "جدّد نية قيام الثلث الأخير من الليل واقرأ سورة الملك قبل النوم.",
                "Recite Surah Al-Mulk before sleep and set your intention for Tahajjud."
            )
        }

        return SacredMeeqatSnapshot(
            maghribMillis = nightStart,
            nextFajrMillis = nightEnd,
            islamicMidnightMillis = islamicMidnight,
            lastThirdStartMillis = lastThirdStart,
            duhaStartMillis = duhaStart,
            duhaPeakMillis = duhaPeak,
            zawalStartMillis = zawalStart,
            dhuhrMillis = todayTimes.dhuhrMillis,
            asrMillis = todayTimes.asrMillis,
            fridayGoldenHourStartMillis = fridayGoldenStart,
            isInLastThirdNow = isInLastThird,
            secondsUntilLastThird = secsToLastThird,
            activeWorshipLabelAr = labelAr,
            activeWorshipLabelEn = labelEn,
            activeWorshipAdviceAr = adviceAr,
            activeWorshipAdviceEn = adviceEn
        )
    }

    fun formatCountdownHms(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val h = safe / 3600L
        val m = (safe % 3600L) / 60L
        val s = safe % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}

private data class Quad(val a: String, val b: String, val c: String, val d: String)

private data class MeeqatWindowItem(
    val titleAr: String,
    val titleEn: String,
    val timeMillis: Long,
    val noteAr: String,
    val color: Color
)

@Composable
fun SacredTimeAndMeeqatScreen(
    latitude: Double,
    longitude: Double,
    cityLabel: String,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val snapshot = remember(latitude, longitude, nowMillis / 1000L) {
        SacredMeeqatCalculator.computeSnapshot(latitude, longitude, nowMillis)
    }

    val is24Hour = LocalIs24HourFormat.current
    val timeFmt = remember(is24Hour) {
        SimpleDateFormat(if (is24Hour) "HH:mm:ss" else "h:mm:ss a", Locale.US)
    }
    val cal = remember(nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis } }
    val dayProgressFraction = remember(nowMillis) {
        val secs = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
        secs / 86400f
    }

    val windows = remember(snapshot) {
        listOf(
            MeeqatWindowItem(
                "بداية الثلث الأخير من الليل (التهجد)",
                "Last Third of Night Begins",
                snapshot.lastThirdStartMillis,
                "ينزل ربنا إلى السماء الدنيا كل ليلة حين يبقى ثلث الليل الآخر",
                GoldBright
            ),
            MeeqatWindowItem(
                "نصف الليل الشرعي",
                "Islamic Midnight (Half-Night)",
                snapshot.islamicMidnightMillis,
                "آخر الوقت الاختياري لصلاة العشاء ومنتصف المسافة بين المغرب والفجر",
                Color(0xFFA78BFA)
            ),
            MeeqatWindowItem(
                "بداية وقت صلاة الضحى (الإشراق)",
                "Duha / Ishraq Start",
                snapshot.duhaStartMillis,
                "بعد ارتفاع الشمس قيد رمح (١٥ دقيقة بعد الشروق)",
                Color(0xFF34D399)
            ),
            MeeqatWindowItem(
                "أفضل وقت للضحى (حين ترمض الفصال)",
                "Peak Duha (Al-Awwabin)",
                snapshot.duhaPeakMillis,
                "أفضل أوقات صلاة الضحى عند اشتداد حرارة الضحى",
                Color(0xFFF59E0B)
            ),
            MeeqatWindowItem(
                "وقت الزوال (قبل الظهر بـ ١٠ دقائق)",
                "Zawal (Forbidden Window)",
                snapshot.zawalStartMillis,
                "استواء الشمس في كبد السماء — يُنهى فيه عن النافلة المطلقة",
                Color(0xFFF87171)
            ),
            MeeqatWindowItem(
                "الساعة الأخيرة قبل المغرب (ساعة إجابة الجمعة)",
                "Pre-Maghrib Golden Hour",
                snapshot.fridayGoldenHourStartMillis,
                "أرجى أوقات ساعة الإجابة يوم الجمعة ووقت أذكار المساء",
                Color(0xFF38BDF8)
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize().testTag("sacred_meeqat_screen")) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مقياس الثلث الأخير والمواقيت الفلكية", "Last Third of Night & Sacred Meeqat"),
            subtitle = L10n.pick(
                lang,
                "حساب فلكي حي بالثانية لمدينة $cityLabel · النزول الإلهي، الضحى، والزوال",
                "Live astronomical calculation for $cityLabel · Tahajjud, Duha & Zawal"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live 24-Hour Celestial Islamic Dial & Countdown
            item {
                LiquidGlassCard(strong = true, goldBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(216.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 16.dp.toPx()
                                val inset = stroke
                                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                                val topLeft = Offset(inset, inset)

                                // Outer 24h night/day celestial ring
                                drawArc(
                                    color = Color(0xFF1E3A8A).copy(alpha = 0.55f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke)
                                )
                                // Daytime golden-emerald arc (approx 06:00 -> 18:00)
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(EmeraldPrimary, GoldBright, Color(0xFF38BDF8), EmeraldPrimary)),
                                    startAngle = 0f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                // Blessed Last Third of Night Golden Glow (approx 02:00 -> 05:00)
                                drawArc(
                                    color = GoldBright,
                                    startAngle = -60f,
                                    sweepAngle = 45f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke + 4.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Current-time glowing marker on the 24-hour celestial dial
                                val angleDeg = -90.0 + (dayProgressFraction * 360.0)
                                val rad = Math.toRadians(angleDeg)
                                val radius = arcSize.width / 2f
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val px = cx + (radius * cos(rad)).toFloat()
                                val py = cy + (radius * sin(rad)).toFloat()
                                drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(px, py))
                                drawCircle(GoldPrimary, radius = 5.dp.toPx(), center = Offset(px, py))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (snapshot.isInLastThirdNow) Icons.Rounded.NightsStay else Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (snapshot.isInLastThirdNow) {
                                        L10n.pick(lang, "متبقي حتى الفجر", "Left Until Fajr")
                                    } else {
                                        L10n.pick(lang, "حتى الثلث الأخير", "To Last Third")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SacredMeeqatCalculator.formatCountdownHms(snapshot.secondsUntilLastThird),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldBright
                                )
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "يبدأ: ${timeFmt.format(Date(snapshot.lastThirdStartMillis))}",
                                        "Starts: ${timeFmt.format(Date(snapshot.lastThirdStartMillis))}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF7DD3FC),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.18f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = L10n.pick(lang, snapshot.activeWorshipLabelAr, snapshot.activeWorshipLabelEn),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldBright,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.pick(lang, snapshot.activeWorshipAdviceAr, snapshot.activeWorshipAdviceEn),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = L10n.pick(lang, "المواقيت النورانية الدقيقة لهذا اليوم والليلة", "Exact Sacred Windows Today & Tonight"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
            }

            items(windows) { item ->
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(item.color.copy(alpha = 0.2f))
                                .border(1.dp, item.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.color == GoldBright || item.color == Color(0xFFA78BFA)) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny,
                                contentDescription = null,
                                tint = item.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, item.titleAr, item.titleEn),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.noteAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = timeFmt.format(Date(item.timeMillis)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = item.color
                        )
                    }
                }
            }

            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "﴿ وَبِالْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ ﴾",
                        fontFamily = AmiriFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldBright,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
