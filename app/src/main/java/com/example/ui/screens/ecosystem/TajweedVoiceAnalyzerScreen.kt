package com.example.ui.screens.ecosystem

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

enum class RecitationPace(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val msPerHarakah: Double
) {
    TAHQIQ("tahqiq", "التحقيق (بطيء)", "Tahqiq (Slow)", 420.0),
    TADWIR("tadwir", "التدوير (متوسط)", "Tadwir (Medium)", 360.0),
    HADR("hadr", "الحدر (سريع)", "Hadr (Fast)", 280.0)
}

data class TajweedDrillTarget(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val minHarakaat: Double,
    val maxHarakaat: Double,
    val ayahExampleAr: String,
    val focusWordAr: String,
    val surahRefAr: String,
    val surahNumber: Int,
    val guidanceAr: String,
    val guidanceEn: String,
    val accentColor: Color
)

object TajweedAcousticDsp {
    const val SAMPLE_RATE_HZ = 16000
    const val VOICE_THRESHOLD_DB = -36.0

    val drills: List<TajweedDrillTarget> = listOf(
        TajweedDrillTarget(
            id = "madd_tabii",
            titleAr = "المد الطبيعي (٢ حركة)",
            titleEn = "Natural Madd (2 Harakaat)",
            minHarakaat = 1.6,
            maxHarakaat = 2.6,
            ayahExampleAr = "قَالَ نُوحٌ رَّبِّ إِنَّهُمْ عَصَوْنِي",
            focusWordAr = "قَا / نُو / نِي",
            surahRefAr = "سورة نوح — ٢١",
            surahNumber = 71,
            guidanceAr = "مُدَّ الألف أو الواو أو الياء بمقدار حركتين طبيعيتين دون زيادة أو نقصان.",
            guidanceEn = "Elongate the natural vowel smoothly for exactly 2 counts without over-stretching.",
            accentColor = Color(0xFF34D399)
        ),
        TajweedDrillTarget(
            id = "ghunnah_mushaddadah",
            titleAr = "غنّة النون والميم المشددتين (٢ حركة)",
            titleEn = "Nasal Ghunnah (2 Harakaat)",
            minHarakaat = 1.7,
            maxHarakaat = 2.7,
            ayahExampleAr = "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ",
            focusWordAr = "إِنَّا — ثُمَّ",
            surahRefAr = "سورة الكوثر — ١",
            surahNumber = 108,
            guidanceAr = "أخرج صوت الغنّة من الخيشوم (الأنف) واستمر فيه بمقدار حركتين كاملتين.",
            guidanceEn = "Resonate the nasal sound through the nose smoothly for 2 full harakaat.",
            accentColor = Color(0xFFA78BFA)
        ),
        TajweedDrillTarget(
            id = "madd_muttasil",
            titleAr = "المد الواجب المتصل (٤–٥ حركات)",
            titleEn = "Connected Madd (4–5 Harakaat)",
            minHarakaat = 3.8,
            maxHarakaat = 5.3,
            ayahExampleAr = "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ",
            focusWordAr = "جَاءَ — السَّمَاءِ",
            surahRefAr = "سورة النصر — ١",
            surahNumber = 110,
            guidanceAr = "عند مجيء الهمزة بعد حرف المد في كلمة واحدة، حافظ على صوت متوازن لـ ٤ إلى ٥ حركات.",
            guidanceEn = "Hold steady vocal airflow for 4 to 5 counts before pronouncing the hamzah.",
            accentColor = Color(0xFF38BDF8)
        ),
        TajweedDrillTarget(
            id = "madd_lazim",
            titleAr = "المد اللازم الكلمي المثقّل (٦ حركات)",
            titleEn = "Compulsory Madd Lazim (6 Harakaat)",
            minHarakaat = 5.5,
            maxHarakaat = 6.8,
            ayahExampleAr = "غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
            focusWordAr = "الضَّالِّينَ — الْحَاقَّةُ",
            surahRefAr = "سورة الفاتحة — ٧",
            surahNumber = 1,
            guidanceAr = "مدّ كامل مشبع بمقدار ٦ حركات متصلة ثم انطق اللام المشددة بقوة ووضوح.",
            guidanceEn = "Sustain a full 6-count elongation smoothly before landing crisply on the doubled letter.",
            accentColor = Color(0xFFF59E0B)
        )
    )

    fun computeRmsDb(samples: ShortArray, count: Int): Double {
        if (count <= 0) return -90.0
        val limit = count.coerceAtMost(samples.size)
        var sumSq = 0.0
        for (i in 0 until limit) {
            val normalized = samples[i] / 32768.0
            sumSq += normalized * normalized
        }
        val rms = sqrt(sumSq / limit.toDouble())
        if (rms <= 1e-7) return -90.0
        return (20.0 * log10(rms)).coerceIn(-90.0, 0.0)
    }

    fun computeBandEnergies(samples: ShortArray, count: Int, bands: Int = 36): List<Float> {
        if (count <= 0 || bands <= 0) return List(bands.coerceAtLeast(1)) { 0f }
        val limit = count.coerceAtMost(samples.size)
        val chunkSize = (limit / bands).coerceAtLeast(1)
        return List(bands) { b ->
            val start = (b * chunkSize).coerceAtMost(limit - 1)
            val end = ((b + 1) * chunkSize).coerceAtMost(limit)
            var peak = 0
            for (i in start until end) {
                val amp = abs(samples[i].toInt())
                if (amp > peak) peak = amp
            }
            ((peak / 20000f).coerceIn(0f, 1f))
        }
    }

    fun durationMsToHarakaat(durationMs: Long, msPerHarakah: Double = 360.0): Double {
        if (durationMs <= 0L || msPerHarakah <= 0.0) return 0.0
        return (durationMs.toDouble() / msPerHarakah).coerceIn(0.0, 9.0)
    }

    fun classifyMaddType(harakaat: Double): Pair<String, String> = when {
        harakaat < 0.7 -> "صمت / استعداد" to "Idle / Ready"
        harakaat < 1.5 -> "حركة قصيرة (أقل من مد)" to "Short Vowel (< 2 counts)"
        harakaat < 3.1 -> "مد طبيعي / غنّة (٢ حركة)" to "Natural Madd / Ghunnah (2)"
        harakaat < 5.4 -> "مد متصل / منفصل (٤–٥ حركات)" to "Muttasil / Munfasil (4–5)"
        else -> "مد لازم مثقّل (٦ حركات)" to "Compulsory Madd Lazim (6)"
    }

    fun isWithinTarget(harakaat: Double, target: TajweedDrillTarget): Boolean {
        return harakaat in target.minHarakaat..target.maxHarakaat
    }
}

@SuppressLint("MissingPermission")
@Composable
fun TajweedVoiceAnalyzerScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedMessage by remember { mutableStateOf(false) }

    var isListening by remember { mutableStateOf(false) }
    var selectedPace by remember { mutableStateOf(RecitationPace.TADWIR) }
    var selectedDrill by remember { mutableStateOf(TajweedAcousticDsp.drills[2]) }

    var currentDb by remember { mutableDoubleStateOf(-90.0) }
    var liveHarakaat by remember { mutableDoubleStateOf(0.0) }
    var peakHarakaat by remember { mutableDoubleStateOf(0.0) }
    var liveVoicedMs by remember { mutableLongStateOf(0L) }
    var longestBreathMs by remember { mutableLongStateOf(0L) }
    var matchedCount by remember { mutableIntStateOf(0) }
    var lastStatusAr by remember { mutableStateOf("اضغط «بدء التحليل الصوتي الحي» واتلُ الكلمة بوضوح") }
    var lastStatusEn by remember { mutableStateOf("Tap Start Live Mic Analyzer and recite clearly") }
    var spectrumBars by remember { mutableStateOf(List(36) { 0.04f }) }
    var micError by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        permissionDeniedMessage = !granted
        if (granted) {
            isListening = true
        }
    }

    // Real Android AudioRecord PCM DSP loop on Dispatchers.IO
    LaunchedEffect(isListening, hasMicPermission, selectedPace, selectedDrill) {
        if (!isListening || !hasMicPermission) {
            spectrumBars = List(36) { 0.04f }
            currentDb = -90.0
            liveHarakaat = 0.0
            liveVoicedMs = 0L
            return@LaunchedEffect
        }

        micError = null
        withContext(Dispatchers.IO) {
            val minBuffer = AudioRecord.getMinBufferSize(
                TajweedAcousticDsp.SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) {
                withContext(Dispatchers.Main) {
                    micError = L10n.pick(lang, "تعذر تهيئة الميكروفون على هذا الجهاز", "Unable to initialize microphone on this device")
                    isListening = false
                }
                return@withContext
            }

            val readSize = (minBuffer / 2).coerceAtLeast(1024)
            var recorder: AudioRecord? = null
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    TajweedAcousticDsp.SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer * 2
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    withContext(Dispatchers.Main) {
                        micError = L10n.pick(lang, "الميكروفون غير متاح حالياً", "Microphone is currently unavailable")
                        isListening = false
                    }
                    return@withContext
                }
                recorder.startRecording()
                val pcmBuffer = ShortArray(readSize)
                var voicedStartMs = 0L
                var silentGraceFrames = 0
                var sessionPeakHarakaat = 0.0

                while (isActive && isListening) {
                    val read = recorder.read(pcmBuffer, 0, pcmBuffer.size)
                    if (read > 0) {
                        val db = TajweedAcousticDsp.computeRmsDb(pcmBuffer, read)
                        val bars = TajweedAcousticDsp.computeBandEnergies(pcmBuffer, read, 36)
                        val now = System.currentTimeMillis()
                        val isVoiced = db >= TajweedAcousticDsp.VOICE_THRESHOLD_DB

                        if (isVoiced) {
                            silentGraceFrames = 0
                            if (voicedStartMs == 0L) {
                                voicedStartMs = now
                                sessionPeakHarakaat = 0.0
                            }
                            val elapsed = now - voicedStartMs
                            val harakaat = TajweedAcousticDsp.durationMsToHarakaat(elapsed, selectedPace.msPerHarakah)
                            if (harakaat > sessionPeakHarakaat) {
                                sessionPeakHarakaat = harakaat
                            }
                            val (clsAr, clsEn) = TajweedAcousticDsp.classifyMaddType(harakaat)
                            withContext(Dispatchers.Main) {
                                currentDb = db
                                spectrumBars = bars
                                liveVoicedMs = elapsed
                                liveHarakaat = harakaat
                                if (elapsed > longestBreathMs) longestBreathMs = elapsed
                                if (harakaat > peakHarakaat) peakHarakaat = harakaat
                                lastStatusAr = clsAr
                                lastStatusEn = clsEn
                            }
                        } else {
                            silentGraceFrames++
                            if (voicedStartMs > 0L && silentGraceFrames >= 4) {
                                val finalHarakaat = sessionPeakHarakaat
                                val hit = TajweedAcousticDsp.isWithinTarget(finalHarakaat, selectedDrill)
                                voicedStartMs = 0L
                                withContext(Dispatchers.Main) {
                                    if (hit) {
                                        matchedCount += 1
                                        lastStatusAr = "أحسنت! مقدار المد متقن (${String.format("%.1f", finalHarakaat)} حركة)"
                                        lastStatusEn = "Excellent! Accurate Madd (${String.format("%.1f", finalHarakaat)} harakaat)"
                                    } else if (finalHarakaat >= 0.8) {
                                        val (clsAr, clsEn) = TajweedAcousticDsp.classifyMaddType(finalHarakaat)
                                        lastStatusAr = "آخر مدّ: $clsAr (${String.format("%.1f", finalHarakaat)} حركة)"
                                        lastStatusEn = "Last Madd: $clsEn (${String.format("%.1f", finalHarakaat)} counts)"
                                    }
                                    liveVoicedMs = 0L
                                    liveHarakaat = 0.0
                                }
                            }
                            withContext(Dispatchers.Main) {
                                currentDb = db
                                spectrumBars = bars.map { (it * 0.4f).coerceAtLeast(0.04f) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    micError = e.localizedMessage ?: L10n.pick(lang, "خطأ في قراءة الميكروفون", "Microphone read error")
                    isListening = false
                }
            } finally {
                try {
                    recorder?.stop()
                } catch (_: Exception) {
                }
                try {
                    recorder?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { isListening = false }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "ميزان المدود والتجويد الصوتي الحي", "Live Mic Tajweed & Madd Analyzer"),
            subtitle = L10n.pick(
                lang,
                "تحليل حقيقي فوري لصوتك بالميكروفون لقياس حركات المد والنَّفَس",
                "Real-time hardware microphone DSP for Madd Harakaat & breath"
            ),
            onBack = {
                isListening = false
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pace selector
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = L10n.pick(lang, "مرتبة التلاوة (معيار طول الحركة)", "Recitation Speed Standard"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(RecitationPace.entries) { pace ->
                                LiquidGlassChip(
                                    label = L10n.pick(lang, pace.labelAr, pace.labelEn),
                                    selected = selectedPace == pace,
                                    onClick = { selectedPace = pace },
                                    modifier = Modifier.testTag("voice_pace_${pace.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Live Harakaat Semicircular Gauge + Real-time Spectrum Canvas
            item {
                val targetHit = TajweedAcousticDsp.isWithinTarget(
                    if (liveHarakaat > 0.1) liveHarakaat else peakHarakaat,
                    selectedDrill
                )
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isListening) Color(0xFF34D399) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isListening) {
                                        L10n.pick(lang, "الميكروفون الحقيقي يعمل الآن", "Live Mic Active")
                                    } else {
                                        L10n.pick(lang, "الميكروفون متوقف", "Mic Standby")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isListening) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${currentDb.toInt()} dB",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Semicircular Harakaat Arc Gauge (0.0 -> 7.0)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val displayHarakaat = if (liveHarakaat > 0.05) liveHarakaat else peakHarakaat
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 18.dp.toPx()
                                val diameter = (size.width * 0.72f).coerceAtMost(size.height * 1.65f)
                                val topLeft = Offset(
                                    (size.width - diameter) / 2f,
                                    size.height - diameter / 2f - 12.dp.toPx()
                                )
                                val arcSize = Size(diameter, diameter)

                                // Background arc
                                drawArc(
                                    color = Color.White.copy(alpha = 0.12f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )

                                // Target zone highlight arc
                                val targetStartAngle = 180f + ((selectedDrill.minHarakaat / 7.0).coerceIn(0.0, 1.0) * 180.0).toFloat()
                                val targetSweep = (((selectedDrill.maxHarakaat - selectedDrill.minHarakaat) / 7.0).coerceIn(0.04, 1.0) * 180.0).toFloat()
                                drawArc(
                                    color = selectedDrill.accentColor.copy(alpha = 0.45f),
                                    startAngle = targetStartAngle,
                                    sweepAngle = targetSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokePx + 6.dp.toPx(), cap = StrokeCap.Butt)
                                )

                                // Live measured Harakaat arc
                                val progress = (displayHarakaat / 7.0).coerceIn(0.0, 1.0).toFloat()
                                if (progress > 0.005f) {
                                    drawArc(
                                        brush = Brush.horizontalGradient(
                                            listOf(EmeraldPrimary, GoldBright, Color(0xFFF59E0B))
                                        ),
                                        startAngle = 180f,
                                        sweepAngle = 180f * progress,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                    )
                                }

                                // Needle indicator
                                val angleRad = Math.toRadians(180.0 + progress * 180.0)
                                val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
                                val radius = diameter / 2f
                                val tip = Offset(
                                    center.x + (radius * cos(angleRad)).toFloat(),
                                    center.y + (radius * sin(angleRad)).toFloat()
                                )
                                drawCircle(
                                    color = GoldBright,
                                    radius = 8.dp.toPx(),
                                    center = tip
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                Text(
                                    text = String.format("%.1f", if (liveHarakaat > 0.05) liveHarakaat else peakHarakaat),
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (targetHit) Color(0xFF34D399) else GoldBright
                                )
                                Text(
                                    text = L10n.pick(lang, "حركة مدّ", "Harakaat"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real-time 36-bar PCM Waveform Spectrum Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF041512))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            val count = spectrumBars.size.coerceAtLeast(1)
                            val slotW = size.width / count
                            val barW = (slotW * 0.62f).coerceAtLeast(3f)
                            val midY = size.height / 2f
                            spectrumBars.forEachIndexed { idx, level ->
                                val barH = (level.coerceIn(0.05f, 1f) * size.height * 0.9f)
                                val x = idx * slotW + (slotW - barW) / 2f
                                val barColor = when {
                                    level > 0.72f -> GoldBright
                                    level > 0.35f -> Color(0xFF34D399)
                                    else -> Color(0xFF38BDF8).copy(alpha = 0.75f)
                                }
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, midY - barH / 2f),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = L10n.pick(lang, lastStatusAr, lastStatusEn),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (targetHit) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (permissionDeniedMessage) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "يرجى منح إذن الميكروفون لتفعيل تحليل المدود الصوتي الحقيقي.",
                                    "Please grant microphone permission to enable real voice analysis."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF87171),
                                textAlign = TextAlign.Center
                            )
                        }
                        if (micError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = micError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF87171),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isListening) {
                                        isListening = false
                                    } else if (hasMicPermission) {
                                        isListening = true
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("voice_analyzer_toggle_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) Color(0xFFDC2626) else EmeraldPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isListening) {
                                        L10n.pick(lang, "إيقاف التحليل", "Stop Mic")
                                    } else {
                                        L10n.pick(lang, "بدء التحليل الصوتي الحي", "Start Live Mic Analyzer")
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EmeraldDeep.copy(alpha = 0.7f))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        peakHarakaat = 0.0
                                        longestBreathMs = 0L
                                        matchedCount = 0
                                    }
                                    .testTag("voice_analyzer_reset_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = L10n.pick(lang, "تصفير القياس", "Reset"),
                                    tint = GoldBright
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Real Telemetry Readout Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VoiceStatMiniTile(
                                label = L10n.pick(lang, "أعلى مدّ", "Peak Madd"),
                                value = "${String.format("%.1f", peakHarakaat)}",
                                color = GoldBright,
                                modifier = Modifier.weight(1f)
                            )
                            VoiceStatMiniTile(
                                label = L10n.pick(lang, "أطول نَفَس", "Best Breath"),
                                value = "${String.format("%.1f", longestBreathMs / 1000.0)}s",
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                            VoiceStatMiniTile(
                                label = L10n.pick(lang, "محاولات متقنة", "Accurate Hits"),
                                value = "$matchedCount",
                                color = Color(0xFF34D399),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Guided Tajweed Practice Drills
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = GoldBright)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.pick(lang, "اختر تمرين المدّ للتدريب العملي المباشر", "Select Live Practice Drill"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TajweedAcousticDsp.drills) { drill ->
                                LiquidGlassChip(
                                    label = L10n.pick(lang, drill.titleAr, drill.titleEn),
                                    selected = selectedDrill.id == drill.id,
                                    onClick = {
                                        selectedDrill = drill
                                        peakHarakaat = 0.0
                                    },
                                    modifier = Modifier.testTag("voice_drill_${drill.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(selectedDrill.accentColor.copy(alpha = 0.14f))
                                .border(1.dp, selectedDrill.accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = selectedDrill.ayahExampleAr,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBright,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "موضع التطبيق: ${selectedDrill.focusWordAr} • ${selectedDrill.surahRefAr}",
                                        "Focus: ${selectedDrill.focusWordAr} • ${selectedDrill.surahRefAr}"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = selectedDrill.accentColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = L10n.pick(lang, selectedDrill.guidanceAr, selectedDrill.guidanceEn),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.22f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { onOpenSurah(selectedDrill.surahNumber) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = L10n.pick(lang, "افتح السورة في المصحف", "Open Surah in Mushaf"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldBright
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tajweed Harakaat Reference Legend
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "دليل مقادير المدود الصوتية", "Madd Harakaat Reference"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        MaddGuideRow("٢ حركة", "المد الطبيعي، مد البدل، الغنّة المشددة", Color(0xFF34D399))
                        MaddGuideRow("٤–٥ حركات", "المد الواجب المتصل والمد الجائز المنفصل", Color(0xFF38BDF8))
                        MaddGuideRow("٦ حركات", "المد اللازم الكلمي والحرفي المثقّل والمخفّف", Color(0xFFF59E0B))
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceStatMiniTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val noorColors = LocalNoorColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun MaddGuideRow(counts: String, desc: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$counts: ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
