package com.example.ui.screens.ecosystem

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class MuhasabahAxis(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val color: Color,
    val remedyAyahAr: String,
    val remedyRefAr: String,
    val remedyActionAr: String,
    val remedyActionEn: String,
    val surahNumber: Int
)

object SpiritualMuhasabahEngine {
    val axes: List<MuhasabahAxis> = listOf(
        MuhasabahAxis(
            id = "salah",
            titleAr = "الصلاة والخشوع",
            titleEn = "Salah & Khushu'",
            subtitleAr = "أداء الصلوات الخمس في وقتها بحضور قلب",
            subtitleEn = "Performing all 5 prayers on time with presence",
            icon = Icons.Rounded.Mosque,
            color = Color(0xFF34D399),
            remedyAyahAr = "قَدْ أَفْلَحَ الْمُؤْمِنُونَ ۝ الَّذِينَ هُمْ فِي صَلَاتِهِمْ خَاشِعُونَ",
            remedyRefAr = "المؤمنون: ١-٢",
            remedyActionAr = "جدّد وضوءك قبل الصلاة القادمة بخمس دقائق، وصلِّ ركعتين بسكينة وتدبّر للفاتحة.",
            remedyActionEn = "Make Wudu 5 minutes before the next prayer and recite Al-Fatihah slowly with presence.",
            surahNumber = 23
        ),
        MuhasabahAxis(
            id = "quran",
            titleAr = "القرآن والتدبّر",
            titleEn = "Quran & Tadabbur",
            subtitleAr = "وِردك اليومي وتأمّل معاني الآيات",
            subtitleEn = "Daily recitation & deep reflection",
            icon = Icons.Rounded.MenuBook,
            color = Color(0xFFFBBF24),
            remedyAyahAr = "كِتَابٌ أَنزَلْنَاهُ إِلَيْكَ مُبَارَكٌ لِّيَدَّبَّرُوا آيَاتِهِ",
            remedyRefAr = "ص: ٢٩",
            remedyActionAr = "افتح المصحف الآن واقرأ وجهاً واحداً فقط بتمهّل مع قراءة تفسير آية واحدة.",
            remedyActionEn = "Open the Mushaf now and read just one page calmly with the meaning of one verse.",
            surahNumber = 38
        ),
        MuhasabahAxis(
            id = "dhikr",
            titleAr = "الأذكار والاستغفار",
            titleEn = "Adhkar & Istighfar",
            subtitleAr = "أذكار الصباح والمساء ودوام ذكر الله",
            subtitleEn = "Morning/Evening Adhkar & remembrance",
            icon = Icons.Rounded.AutoAwesome,
            color = Color(0xFF38BDF8),
            remedyAyahAr = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            remedyRefAr = "الرعد: ٢٨",
            remedyActionAr = "سبّح الله ٣٣ مرة واستغفره ٣٣ مرة الآن لتُنعش قلبك بالطمأنينة.",
            remedyActionEn = "Recite SubhanAllah 33x and Astaghfirullah 33x now to refresh your heart.",
            surahNumber = 13
        ),
        MuhasabahAxis(
            id = "ihsan",
            titleAr = "الإحسان والصدقة",
            titleEn = "Charity & Kindness",
            subtitleAr = "برّ الوالدين، الكلمة الطيبة، والإنفاق",
            subtitleEn = "Serving parents, charity & gentle kindness",
            icon = Icons.Rounded.VolunteerActivism,
            color = Color(0xFFF472B6),
            remedyAyahAr = "إِنَّ رَحْمَتَ اللَّهِ قَرِيبٌ مِّنَ الْمُحْسِنِينَ",
            remedyRefAr = "الأعراف: ٥٦",
            remedyActionAr = "تصدّق اليوم ولو بالقليل أو أدخل السرور برسالة طيبة على والديك أو محتاج.",
            remedyActionEn = "Give even a small charity today or send a warm message of love to your parents.",
            surahNumber = 7
        ),
        MuhasabahAxis(
            id = "akhlaq",
            titleAr = "حفظ اللسان والأخلاق",
            titleEn = "Character & Tongue",
            subtitleAr = "سلامة الصدر وترك الغيبة والغضب",
            subtitleEn = "Pure heart, avoiding backbiting & anger",
            icon = Icons.Rounded.RecordVoiceOver,
            color = Color(0xFFA78BFA),
            remedyAyahAr = "وَقُولُوا لِلنَّاسِ حُسْنًا",
            remedyRefAr = "البقرة: ٨٣",
            remedyActionAr = "اعفُ عمّن أساء إليك بقلبك الآن وادعُ له بظهر الغيب لتنال سلامة الصدر.",
            remedyActionEn = "Forgive someone in your heart right now and make a quiet Du'a for them.",
            surahNumber = 2
        ),
        MuhasabahAxis(
            id = "qiyam",
            titleAr = "قيام الليل والدعاء",
            titleEn = "Night Worship & Du'a",
            subtitleAr = "الوتر والخلوة الصادقة بالمناجاة",
            subtitleEn = "Witr prayer & sincere private Du'a",
            icon = Icons.Rounded.NightsStay,
            color = Color(0xFF2DD4BF),
            remedyAyahAr = "وَبِالْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ",
            remedyRefAr = "الذاريات: ١٨",
            remedyActionAr = "لا تنم الليلة قبل ركعة وتر واحدة ومناجاة صادقة لمدة دقيقتين.",
            remedyActionEn = "Do not sleep tonight before praying at least 1 rak'ah of Witr and 2 minutes of Du'a.",
            surahNumber = 51
        )
    )

    fun computeHeartNoorIndex(scores: Map<String, Int>): Int {
        if (axes.isEmpty()) return 0
        val total = axes.sumOf { (scores[it.id] ?: 5).coerceIn(0, 10) }
        return ((total.toDouble() / (axes.size * 10.0)) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun resolveTier(index: Int): Pair<String, String> = when {
        index >= 85 -> "قلب مطمئن منير" to "Radiant Tranquil Heart"
        index >= 68 -> "قلب منيب متوازن" to "Balanced Devoted Heart"
        index >= 45 -> "قلب مجاهد في الطريق" to "Striving Growing Heart"
        else -> "بداية التزكية واليقظة" to "Awakening & Fresh Renewal"
    }

    fun findWeakestAxis(scores: Map<String, Int>): MuhasabahAxis {
        return axes.minByOrNull { (scores[it.id] ?: 5).coerceIn(0, 10) } ?: axes.first()
    }
}

private const val MUHASABAH_PREFS = "noor_muhasabah_radar"

private fun todayMuhasabahKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

@Composable
fun SpiritualMuhasabahScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(MUHASABAH_PREFS, Context.MODE_PRIVATE) }
    val todayKey = remember { todayMuhasabahKey() }

    val scores = remember {
        mutableStateMapOf<String, Int>().apply {
            SpiritualMuhasabahEngine.axes.forEach { axis ->
                this[axis.id] = prefs.getInt("${todayKey}_${axis.id}", 6).coerceIn(0, 10)
            }
        }
    }

    fun updateScore(axisId: String, value: Int) {
        val clamped = value.coerceIn(0, 10)
        scores[axisId] = clamped
        prefs.edit()
            .putInt("${todayKey}_$axisId", clamped)
            .putInt("${todayKey}_index", SpiritualMuhasabahEngine.computeHeartNoorIndex(scores))
            .apply()
    }

    val heartIndex = SpiritualMuhasabahEngine.computeHeartNoorIndex(scores)
    val animatedIndex by animateFloatAsState(
        targetValue = heartIndex.toFloat(),
        animationSpec = tween(durationMillis = 550),
        label = "heart_index_anim"
    )
    val (tierAr, tierEn) = SpiritualMuhasabahEngine.resolveTier(heartIndex)
    val weakestAxis = SpiritualMuhasabahEngine.findWeakestAxis(scores)

    // 7-day trend
    val last7Days = remember(heartIndex) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("E", Locale.getDefault())
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = fmt.format(cal.time)
            val idx = if (key == todayKey) heartIndex else prefs.getInt("${key}_index", 0)
            labelFmt.format(cal.time) to idx
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("spiritual_muhasabah_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "محاسبة النفس ومؤشر نور القلب", "Spiritual Muhasabah & Heart Radar"),
            subtitle = L10n.pick(
                lang,
                "حاسبوا أنفسكم قبل أن تُحاسبوا · رادار إيماني سداسي الأبعاد ووصفات قرآنية فورية",
                "Daily 6-Axis Self-Accounting Radar with Personalized Quranic Prescriptions"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Radar Card
            item {
                LiquidGlassCard(strong = true, goldBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "مؤشر نور القلب اليوم", "Today's Heart Noor Index"),
                                style = MaterialTheme.typography.labelMedium,
                                color = noorColors.goldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L10n.pick(lang, tierAr, tierEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(GoldPrimary.copy(alpha = 0.35f), EmeraldPrimary.copy(alpha = 0.16f))
                                    )
                                )
                                .border(2.dp, GoldBright, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${animatedIndex.roundToInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    HeartNoorRadarCanvas(
                        values = SpiritualMuhasabahEngine.axes.map { (scores[it.id] ?: 5) / 10f },
                        colors = SpiritualMuhasabahEngine.axes.map { it.color },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    // 6 Compact Axis Legend Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SpiritualMuhasabahEngine.axes.chunked(3).forEach { rowAxes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowAxes.forEach { axis ->
                                    val sc = scores[axis.id] ?: 5
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(axis.color.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = L10n.pick(lang, axis.titleAr.substringBefore(" "), axis.titleEn.substringBefore(" ")),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = axis.color,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "$sc/10",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Personalized Quranic Remedy for Weakest Axis
            item {
                LiquidGlassCard(goldBorder = true) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = weakestAxis.color)
                        Text(
                            text = L10n.pick(
                                lang,
                                "وصية اليوم لتقوية: ${weakestAxis.titleAr}",
                                "Today's Spiritual Prescription: ${weakestAxis.titleEn}"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldBright
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = weakestAxis.remedyAyahAr,
                        fontFamily = AmiriFontFamily,
                        fontSize = 21.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = weakestAxis.remedyRefAr,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, weakestAxis.remedyActionAr, weakestAxis.remedyActionEn),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L10n.pick(lang, "افتح السورة في المصحف ←", "Open Surah in Mushaf →"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldBright,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenSurah(weakestAxis.surahNumber) }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Daily Interactive Self-Accounting Sliders
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = L10n.pick(lang, "تقييمك الصادق ليومك (٠ - ١٠)", "Honest Daily Check-In (0–10)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    SpiritualMuhasabahEngine.axes.forEach { updateScore(it.id, 5) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                            Text(L10n.pick(lang, "تصفير", "Reset"), style = MaterialTheme.typography.labelSmall, color = GoldBright)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SpiritualMuhasabahEngine.axes.forEach { axis ->
                        val current = scores[axis.id] ?: 5
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(axis.icon, contentDescription = null, tint = axis.color, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(
                                            text = L10n.pick(lang, axis.titleAr, axis.titleEn),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = L10n.pick(lang, axis.subtitleAr, axis.subtitleEn),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "$current / 10",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = axis.color
                                )
                            }
                            Slider(
                                value = current.toFloat(),
                                onValueChange = { updateScore(axis.id, it.roundToInt()) },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = axis.color,
                                    activeTrackColor = axis.color
                                )
                            )
                        }
                    }
                }
            }

            // 7-Day Spiritual Trend Bar
            item {
                LiquidGlassCard {
                    Text(
                        text = L10n.pick(lang, "سجل الاستقامة لآخر ٧ أيام", "7-Day Spiritual Consistency"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7Days.forEach { (dayLabel, pct) ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = if (pct > 0) "$pct%" else "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (pct > 0) GoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val barHeight = (pct.coerceIn(6, 100) * 0.65f).dp
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (pct > 0) Brush.verticalGradient(listOf(GoldBright, EmeraldPrimary))
                                            else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f)))
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayLabel.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartNoorRadarCanvas(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = (minOf(size.width, size.height) * 0.44f).coerceAtLeast(40f)
        val count = values.size.coerceAtLeast(3)

        // Concentric glowing hexagonal rings
        listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { ring ->
            val r = maxRadius * ring
            val ringPath = Path()
            for (i in 0 until count) {
                val angle = -PI / 2.0 + (2.0 * PI * i / count)
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(
                path = ringPath,
                color = GoldPrimary.copy(alpha = if (ring == 1f) 0.38f else 0.16f),
                style = Stroke(width = if (ring == 1f) 2f else 1.2f)
            )
        }

        // Spokes
        for (i in 0 until count) {
            val angle = -PI / 2.0 + (2.0 * PI * i / count)
            val x = cx + (maxRadius * cos(angle)).toFloat()
            val y = cy + (maxRadius * sin(angle)).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(cx, cy),
                end = Offset(x, y),
                strokeWidth = 1.2f
            )
        }

        // Filled luminous polygon
        val polyPath = Path()
        val points = mutableListOf<Offset>()
        for (i in 0 until count) {
            val v = values[i].coerceIn(0.08f, 1.0f)
            val angle = -PI / 2.0 + (2.0 * PI * i / count)
            val pt = Offset(
                cx + (maxRadius * v * cos(angle)).toFloat(),
                cy + (maxRadius * v * sin(angle)).toFloat()
            )
            points.add(pt)
            if (i == 0) polyPath.moveTo(pt.x, pt.y) else polyPath.lineTo(pt.x, pt.y)
        }
        polyPath.close()

        drawPath(
            path = polyPath,
            brush = Brush.radialGradient(
                colors = listOf(GoldBright.copy(alpha = 0.42f), EmeraldPrimary.copy(alpha = 0.28f)),
                center = Offset(cx, cy),
                radius = maxRadius
            )
        )
        drawPath(
            path = polyPath,
            color = GoldBright,
            style = Stroke(width = 3f)
        )

        // Glowing vertex nodes
        points.forEachIndexed { idx, pt ->
            val nodeColor = colors.getOrElse(idx) { GoldBright }
            drawCircle(color = nodeColor.copy(alpha = 0.35f), radius = 12f, center = pt)
            drawCircle(color = nodeColor, radius = 6f, center = pt)
        }
    }
}
