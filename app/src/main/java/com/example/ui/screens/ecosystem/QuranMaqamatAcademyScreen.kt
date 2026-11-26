package com.example.ui.screens.ecosystem

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

data class QuranMaqamInfo(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val mnemonicLetterAr: String,
    val moodAr: String,
    val moodEn: String,
    val quranContextAr: String,
    val quranContextEn: String,
    val reciterStyleAr: String,
    val accentColor: Color,
    val noteNamesAr: List<String>,
    val frequenciesHz: List<Double>,
    val exampleAyahAr: String,
    val surahRefAr: String,
    val surahNumber: Int,
    val recitationAudioUrl: String
)

object QuranMaqamatCatalog {
    const val SYNTH_SAMPLE_RATE = 22050

    /**
     * All 7 Classical Quranic Maqamat ("صُنِعَ بِسِحْر"):
     * ص: الصبا | ن: النهاوند | ع: العجم | ب: البياتي | س: السيكاه | ح: الحجاز | ر: الرست
     * Frequencies preserve genuine Arabic microtonal quarter-tones (3/4-tone intervals).
     */
    val maqamat: List<QuranMaqamInfo> = listOf(
        QuranMaqamInfo(
            id = "bayati",
            nameAr = "مقام البياتي",
            nameEn = "Maqam Bayati",
            mnemonicLetterAr = "ب",
            moodAr = "الخشوع والوقار والبساطة",
            moodEn = "Reverence, Serenity & Clarity",
            quranContextAr = "أكثر المقامات افتتاحاً للتلاوة القرآنية؛ يناسب الفواتح والآيات الهادئة والمواعظ.",
            quranContextEn = "The classic opening Maqam for Quran recitation; ideal for calm reflection and openings.",
            reciterStyleAr = "المنشاوي • الحصري • عبد الباسط",
            accentColor = Color(0xFF34D399),
            noteNamesAr = listOf("ري", "مي نصف بيمول", "فا", "صول", "لا", "سي بيمول", "دو", "ري"),
            frequenciesHz = listOf(293.66, 320.24, 349.23, 392.00, 440.00, 466.16, 523.25, 587.33),
            exampleAyahAr = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمَٰنِ الرَّحِيمِ",
            surahRefAr = "سورة الفاتحة — ٢–٣",
            surahNumber = 1,
            recitationAudioUrl = "https://everyayah.com/data/Minshawy_Murattal_128kbps/001002.mp3"
        ),
        QuranMaqamInfo(
            id = "rast",
            nameAr = "مقام الرست",
            nameEn = "Maqam Rast",
            mnemonicLetterAr = "ر",
            moodAr = "الفخامة والقوة والاستقامة",
            moodEn = "Majesty, Dignity & Balance",
            quranContextAr = "مقام القصص القرآني وآيات العظمة والتشريع والأحكام؛ يُشعر السامع بالهيبة والاستقامة.",
            quranContextEn = "Known as the crown of Maqamat; excels in Quranic stories, majesty, and divine law.",
            reciterStyleAr = "الطبلاوي • مصطفى إسماعيل • السديس",
            accentColor = Color(0xFFF59E0B),
            noteNamesAr = listOf("دو", "ري", "مي نصف بيمول", "فا", "صول", "لا", "سي نصف بيمول", "دو"),
            frequenciesHz = listOf(261.63, 293.66, 320.24, 349.23, 392.00, 440.00, 480.36, 523.25),
            exampleAyahAr = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
            surahRefAr = "آية الكرسي — البقرة ٢٥٥",
            surahNumber = 2,
            recitationAudioUrl = "https://everyayah.com/data/Abdul_Basit_Murattal_192kbps/002255.mp3"
        ),
        QuranMaqamInfo(
            id = "nahawand",
            nameAr = "مقام النهاوند",
            nameEn = "Maqam Nahawand",
            mnemonicLetterAr = "ن",
            moodAr = "الرقّة والسكينة والعاطفة الصادقة",
            moodEn = "Tenderness, Peace & Reflection",
            quranContextAr = "يناسب آيات الرحمة والمغفرة والدعاء ونداءات المؤمنين لما فيه من شجن لطيف مريح للقلب.",
            quranContextEn = "Suited for verses of mercy, forgiveness, and heartfelt supplication.",
            reciterStyleAr = "مشاري العفاسي • ماهر المعيقلي • علي جابر",
            accentColor = Color(0xFF38BDF8),
            noteNamesAr = listOf("دو", "ري", "مي بيمول", "فا", "صول", "لا بيمول", "سي", "دو"),
            frequenciesHz = listOf(261.63, 293.66, 311.13, 349.23, 392.00, 415.30, 493.88, 523.25),
            exampleAyahAr = "الرَّحْمَٰنُ ۝ عَلَّمَ الْقُرْآنَ ۝ خَلَقَ الْإِنسَانَ",
            surahRefAr = "سورة الرحمن — ١–٣",
            surahNumber = 55,
            recitationAudioUrl = "https://everyayah.com/data/Alafasy_128kbps/055001.mp3"
        ),
        QuranMaqamInfo(
            id = "hijaz",
            nameAr = "مقام الحجاز",
            nameEn = "Maqam Hijaz",
            mnemonicLetterAr = "ح",
            moodAr = "الروحانية العميقة والهيبة والتفكّر",
            moodEn = "Deep Spirituality & Awe",
            quranContextAr = "مقام الأذان الحجازي المشهور وآيات الكون والقيامة والرهبة؛ يملأ النفس روحانيةً ووقاراً.",
            quranContextEn = "Famous in the Hijazi Adhan; evokes profound spiritual awe in cosmic and Hereafter verses.",
            reciterStyleAr = "الحذيفي • ياسر الدوسري • عبد الباسط",
            accentColor = Color(0xFFA78BFA),
            noteNamesAr = listOf("ري", "مي بيمول", "فا دييز", "صول", "لا", "سي بيمول", "دو", "ري"),
            frequenciesHz = listOf(293.66, 311.13, 369.99, 392.00, 440.00, 466.16, 523.25, 587.33),
            exampleAyahAr = "وَمَا أَرْسَلْنَاكَ إِلَّا رَحْمَةً لِّلْعَالَمِينَ",
            surahRefAr = "سورة الأنبياء — ١٠٧",
            surahNumber = 21,
            recitationAudioUrl = "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/021107.mp3"
        ),
        QuranMaqamInfo(
            id = "saba",
            nameAr = "مقام الصبا",
            nameEn = "Maqam Saba",
            mnemonicLetterAr = "ص",
            moodAr = "الحزن الشديد والإنكسار والتضرّع",
            moodEn = "Deep Humility, Grief & Supplication",
            quranContextAr = "أكثر المقامات تأثيراً في آيات التوبة والإنابة ومشاهد الحساب والمواساة.",
            quranContextEn = "The most poignant Maqam; resonates deeply in verses of repentance and accountability.",
            reciterStyleAr = "المنشاوي • ناصر القطامي • محمد أيوب",
            accentColor = Color(0xFFF472B6),
            noteNamesAr = listOf("ري", "مي نصف بيمول", "فا", "صول بيمول", "لا", "سي بيمول", "دو", "ري بيمول"),
            frequenciesHz = listOf(293.66, 320.24, 349.23, 369.99, 440.00, 466.16, 523.25, 554.37),
            exampleAyahAr = "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ",
            surahRefAr = "سورة الزمر — ٥٣",
            surahNumber = 39,
            recitationAudioUrl = "https://everyayah.com/data/Minshawy_Murattal_128kbps/039053.mp3"
        ),
        QuranMaqamInfo(
            id = "sika",
            nameAr = "مقام السيكاه",
            nameEn = "Maqam Sika",
            mnemonicLetterAr = "س",
            moodAr = "السرور الهادئ والارتياح والبشرى",
            moodEn = "Joyful Serenity & Glad Tidings",
            quranContextAr = "يتميز بدرجته الميكروتونية الفريدة، ويليق بآيات البشرى والنعيم وأهل الجنة والتسبيح.",
            quranContextEn = "Defined by its signature quarter-tone tonic; illuminates verses of Paradise and glad tidings.",
            reciterStyleAr = "الحصري • مصطفى إسماعيل • الشاطري",
            accentColor = Color(0xFF2DD4BF),
            noteNamesAr = listOf("مي نصف بيمول", "فا", "صول", "لا", "سي نصف بيمول", "دو", "ري", "مي نصف بيمول"),
            frequenciesHz = listOf(320.24, 349.23, 392.00, 440.00, 480.36, 523.25, 587.33, 640.48),
            exampleAyahAr = "إِنَّ الْمُتَّقِينَ فِي جَنَّاتٍ وَنَعِيمٍ",
            surahRefAr = "سورة الطور — ١٧",
            surahNumber = 52,
            recitationAudioUrl = "https://everyayah.com/data/Husary_128kbps/052017.mp3"
        ),
        QuranMaqamInfo(
            id = "ajam",
            nameAr = "مقام العجم",
            nameEn = "Maqam Ajam",
            mnemonicLetterAr = "ع",
            moodAr = "النصر والبهجة والإشراق والقوة",
            moodEn = "Triumph, Radiance & Clarity",
            quranContextAr = "مقام واضح مشرق يلائم آيات النصر والفتح والتمكين وتكبيرات العيد.",
            quranContextEn = "Bright and uplifting; naturally conveys verses of victory, strength, and celebration.",
            reciterStyleAr = "عبد الباسط • الشريم • العفاسي",
            accentColor = Color(0xFFEAB308),
            noteNamesAr = listOf("سي بيمول", "دو", "ري", "مي بيمول", "فا", "صول", "لا", "سي بيمول"),
            frequenciesHz = listOf(233.08, 261.63, 293.66, 311.13, 349.23, 392.00, 440.00, 466.16),
            exampleAyahAr = "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ",
            surahRefAr = "سورة النصر — ١",
            surahNumber = 110,
            recitationAudioUrl = "https://everyayah.com/data/Alafasy_128kbps/110001.mp3"
        )
    )

    /**
     * Synthesizes a warm acoustic harmonic tone (fundamental + 2nd + 3rd harmonic)
     * with smooth attack/release envelope to play authentic Arabic microtonal pitches.
     */
    fun synthesizeTonePcm(
        frequencyHz: Double,
        durationMs: Int = 340,
        sampleRate: Int = SYNTH_SAMPLE_RATE
    ): ShortArray {
        val safeFreq = frequencyHz.coerceIn(80.0, 2400.0)
        val numSamples = ((sampleRate * durationMs) / 1000).coerceAtLeast(1)
        val output = ShortArray(numSamples)
        val attackSamples = (sampleRate * 0.025).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleRate * 0.06).toInt().coerceAtLeast(1)
        val twoPi = 2.0 * PI

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val env = when {
                i < attackSamples -> i.toDouble() / attackSamples.toDouble()
                i > numSamples - releaseSamples -> ((numSamples - i).toDouble() / releaseSamples.toDouble()).coerceAtLeast(0.0)
                else -> 1.0
            }
            val wave = 0.65 * sin(twoPi * safeFreq * t) +
                0.24 * sin(twoPi * safeFreq * 2.0 * t) +
                0.11 * sin(twoPi * safeFreq * 3.0 * t)
            val scaled = (wave * env * 22000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[i] = scaled.toShort()
        }
        return output
    }
}

private suspend fun playSynthesizedPcmTone(frequencyHz: Double, durationMs: Int = 360) {
    withContext(Dispatchers.IO) {
        val pcm = QuranMaqamatCatalog.synthesizeTonePcm(frequencyHz, durationMs)
        val minBuf = AudioTrack.getMinBufferSize(
            QuranMaqamatCatalog.SYNTH_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(QuranMaqamatCatalog.SYNTH_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            kotlinx.coroutines.delay(durationMs.toLong() + 30L)
        } catch (_: Exception) {
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            try {
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun QuranMaqamatAcademyScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 7 Maqamat Studio, 1 = Ear-Training Quiz
    var selectedMaqam by remember { mutableStateOf(QuranMaqamatCatalog.maqamat.first()) }
    var activeNoteIndex by remember { mutableIntStateOf(-1) }
    var synthJob by remember { mutableStateOf<Job?>(null) }

    // Real MediaPlayer for streaming authentic Quran recitations
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isStreamingAudio by remember { mutableStateOf(false) }
    var isAudioLoading by remember { mutableStateOf(false) }
    var audioError by remember { mutableStateOf<String?>(null) }

    // Ear-Training Quiz state
    var quizTargetIdx by remember { mutableIntStateOf(0) }
    var quizAnsweredId by remember { mutableStateOf<String?>(null) }
    var quizScore by remember { mutableIntStateOf(0) }
    var quizStreak by remember { mutableIntStateOf(0) }

    fun stopRecitationStream() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        isStreamingAudio = false
        isAudioLoading = false
    }

    fun playRecitationStream(url: String) {
        stopRecitationStream()
        audioError = null
        isAudioLoading = true
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { player ->
                isAudioLoading = false
                isStreamingAudio = true
                player.start()
            }
            setOnCompletionListener {
                isStreamingAudio = false
            }
            setOnErrorListener { _, _, _ ->
                isAudioLoading = false
                isStreamingAudio = false
                audioError = L10n.pick(
                    lang,
                    "تعذر تشغيل البث الصوتي — تحقق من الاتصال بالإنترنت",
                    "Unable to stream audio — please check internet connection"
                )
                true
            }
        }
        mediaPlayer = mp
        try {
            mp.setDataSource(url)
            mp.prepareAsync()
        } catch (_: Exception) {
            stopRecitationStream()
            audioError = L10n.pick(lang, "تعذر تحميل ملف التلاوة", "Failed to load recitation stream")
        }
    }

    fun playFullMaqamScale(maqam: QuranMaqamInfo) {
        synthJob?.cancel()
        synthJob = scope.launch {
            maqam.frequenciesHz.forEachIndexed { idx, freq ->
                if (!isActive) return@launch
                activeNoteIndex = idx
                playSynthesizedPcmTone(freq, durationMs = 280)
            }
            activeNoteIndex = -1
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            synthJob?.cancel()
            stopRecitationStream()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "أكاديمية المقامات القرآنية الـ٧", "Quranic Maqamat Academy"),
            subtitle = L10n.pick(
                lang,
                "سلم صوتي حقيقي بربع التون + تلاوات قرآنية حية + اختبار الأذن",
                "Real microtonal audio synth, live recitations & ear training"
            ),
            onBack = {
                synthJob?.cancel()
                stopRecitationStream()
                onBack()
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassChip(
                label = L10n.pick(lang, "المقامات الـ٧ (صُنِعَ بِسِحْر)", "7 Maqamat Studio"),
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("maqamat_tab_studio")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "اختبار الأذن السمعي", "Ear-Training Quiz"),
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("maqamat_tab_quiz")
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Horizontal 7 Maqamat Selector
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QuranMaqamatCatalog.maqamat) { maqam ->
                            val isSelected = maqam.id == selectedMaqam.id
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) maqam.accentColor.copy(alpha = 0.24f)
                                        else EmeraldDeep.copy(alpha = 0.55f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) maqam.accentColor else GoldPrimary.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        stopRecitationStream()
                                        selectedMaqam = maqam
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("maqam_select_${maqam.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = maqam.mnemonicLetterAr,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = maqam.accentColor
                                )
                                Text(
                                    text = L10n.pick(lang, maqam.nameAr, maqam.nameEn),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Selected Maqam Interactive Microtonal Keyboard & Scale Synthesizer
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = L10n.pick(lang, selectedMaqam.nameAr, selectedMaqam.nameEn),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = selectedMaqam.accentColor
                                    )
                                    Text(
                                        text = L10n.pick(lang, selectedMaqam.moodAr, selectedMaqam.moodEn),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldBright,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(selectedMaqam.accentColor.copy(alpha = 0.18f))
                                        .border(1.dp, selectedMaqam.accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "صُنِعَ بِسِحْر (${selectedMaqam.mnemonicLetterAr})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = selectedMaqam.accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = L10n.pick(lang, selectedMaqam.quranContextAr, selectedMaqam.quranContextEn),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = L10n.pick(lang, "أشهر القرّاء: ${selectedMaqam.reciterStyleAr}", "Notable Qaris: ${selectedMaqam.reciterStyleAr}"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = L10n.pick(lang, "السلم الصوتي الميكروتوني (اضغط على أي نغمة للاستماع)", "Interactive Microtonal Scale (Tap any note)"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Interactive 8-Note Microtonal Scale Keys
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                selectedMaqam.frequenciesHz.forEachIndexed { idx, freq ->
                                    val isLit = activeNoteIndex == idx
                                    val noteLabel = selectedMaqam.noteNamesAr.getOrElse(idx) { "${idx + 1}" }
                                    val isQuarterTone = noteLabel.contains("نصف")
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(74.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isLit -> GoldBright
                                                    isQuarterTone -> selectedMaqam.accentColor.copy(alpha = 0.28f)
                                                    else -> Color(0xFF07211C)
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isLit) Color.White else selectedMaqam.accentColor.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                synthJob?.cancel()
                                                synthJob = scope.launch {
                                                    activeNoteIndex = idx
                                                    playSynthesizedPcmTone(freq, durationMs = 340)
                                                    activeNoteIndex = -1
                                                }
                                            }
                                            .padding(horizontal = 2.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${freq.toInt()}",
                                            fontSize = 9.sp,
                                            color = if (isLit) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = noteLabel.replace(" نصف بيمول", "½♭").replace(" بيمول", "♭").replace(" دييز", "♯"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLit) Color.Black else Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { playFullMaqamScale(selectedMaqam) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("maqam_play_scale_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = L10n.pick(lang, "استمع للسلم الصوتي الحقيقي للمقام", "Play Real Microtonal Scale"),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Live Quran Recitation Example Card
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = GoldBright)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = L10n.pick(lang, "تطبيق قرآني صوتي حي", "Live Quran Recitation Example"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = selectedMaqam.exampleAyahAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldBright,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedMaqam.surahRefAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (audioError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = audioError.orEmpty(),
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
                                        if (isStreamingAudio || isAudioLoading) {
                                            stopRecitationStream()
                                        } else {
                                            playRecitationStream(selectedMaqam.recitationAudioUrl)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("maqam_play_ayah_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isStreamingAudio) Color(0xFFDC2626) else GoldPrimary
                                    )
                                ) {
                                    if (isAudioLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isStreamingAudio) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isStreamingAudio) Color.White else Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isAudioLoading -> L10n.pick(lang, "جارٍ التحميل...", "Loading...")
                                            isStreamingAudio -> L10n.pick(lang, "إيقاف التلاوة", "Stop Audio")
                                            else -> L10n.pick(lang, "استمع للتلاوة الحية", "Stream Live Ayah")
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStreamingAudio) Color.White else Color.Black
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EmeraldDeep.copy(alpha = 0.7f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        .clickable { onOpenSurah(selectedMaqam.surahNumber) }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = L10n.pick(lang, "المصحف", "Mushaf"),
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
            }
        } else {
            // Tab 1: Interactive Ear-Training Challenge
            val targetMaqam = QuranMaqamatCatalog.maqamat[quizTargetIdx % QuranMaqamatCatalog.maqamat.size]
            val quizOptions = remember(quizTargetIdx) {
                val others = QuranMaqamatCatalog.maqamat.filter { it.id != targetMaqam.id }
                val picked = listOf(
                    targetMaqam,
                    others[quizTargetIdx % others.size],
                    others[(quizTargetIdx + 2) % others.size],
                    others[(quizTargetIdx + 4) % others.size]
                )
                picked.sortedBy { (it.id.hashCode() xor quizTargetIdx) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Quiz, contentDescription = null, tint = GoldBright)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = L10n.pick(lang, "تحدي تمييز المقامات بالسمع", "Maqam Ear-Training Challenge"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = noorColors.goldAccent
                                    )
                                }
                                Text(
                                    text = L10n.pick(lang, "النقاط: $quizScore • سلسلة: $quizStreak", "Score: $quizScore • Streak: $quizStreak"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "استمع جيداً للسلم الصوتي المجهول ثم اختر اسم المقام الصحيح:",
                                    "Listen closely to the mystery microtonal scale and pick the matching Maqam:"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { playFullMaqamScale(targetMaqam) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("maqam_quiz_listen_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = L10n.pick(lang, "استمع للسلم المجهول الآن", "Play Mystery Maqam Scale"),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            quizOptions.forEach { option ->
                                val answered = quizAnsweredId != null
                                val isChosen = quizAnsweredId == option.id
                                val isCorrect = option.id == targetMaqam.id
                                val borderColor = when {
                                    !answered -> GoldPrimary.copy(alpha = 0.25f)
                                    isCorrect -> Color(0xFF34D399)
                                    isChosen -> Color(0xFFF87171)
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                                val bgColor = when {
                                    !answered -> EmeraldDeep.copy(alpha = 0.55f)
                                    isCorrect -> Color(0xFF34D399).copy(alpha = 0.22f)
                                    isChosen -> Color(0xFFF87171).copy(alpha = 0.20f)
                                    else -> EmeraldDeep.copy(alpha = 0.35f)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(bgColor)
                                        .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
                                        .clickable(enabled = !answered) {
                                            quizAnsweredId = option.id
                                            if (option.id == targetMaqam.id) {
                                                quizScore += 10
                                                quizStreak += 1
                                            } else {
                                                quizStreak = 0
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 14.dp)
                                        .testTag("maqam_quiz_opt_${option.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = L10n.pick(lang, option.nameAr, option.nameEn),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = L10n.pick(lang, option.moodAr, option.moodEn),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (answered && isCorrect) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF34D399))
                                    }
                                }
                            }

                            if (quizAnsweredId != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        quizAnsweredId = null
                                        quizTargetIdx = (quizTargetIdx + 1) % QuranMaqamatCatalog.maqamat.size
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("maqam_quiz_next_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = L10n.pick(lang, "المقام التالي", "Next Mystery Maqam"),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
