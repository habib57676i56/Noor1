package com.example.ui.screens.ecosystem

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class YasserIraqiRecitationItem(
    val id: String,
    val surahNumber: Int,
    val highlightAyahNumber: Int,
    val titleAr: String,
    val titleEn: String,
    val maqamTagAr: String,
    val maqamTagEn: String,
    val iconicAyahAr: String,
    val youtubeQuery: String,
    val eraNoteAr: String,
    val eraNoteEn: String,
    val customAudioSource: String? = null,
    val isAdminUploaded: Boolean = false
) {
    fun fullSurahAudioUrl(): String {
        val custom = customAudioSource?.trim()
        if (!custom.isNullOrBlank()) return custom
        val s = surahNumber.toString().padStart(3, '0')
        return "https://server11.mp3quran.net/yasser/$s.mp3"
    }

    fun climaxAyahAudioUrl(): String {
        val custom = customAudioSource?.trim()
        if (!custom.isNullOrBlank()) return custom
        val s = surahNumber.toString().padStart(3, '0')
        val a = highlightAyahNumber.toString().padStart(3, '0')
        return "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/${s}${a}.mp3"
    }

    fun youtubeSearchUrl(): String {
        val encoded = URLEncoder.encode(youtubeQuery, "UTF-8")
        return "https://www.youtube.com/results?search_query=$encoded"
    }
}

/**
 * Curated Masterpiece Catalog of Sheikh Yasser Al-Dosari's most iconic viral
 * Iraqi-Maqam & Haramain Khashia recitations celebrated across YouTube.
 */
object YasserDosariIraqiCatalog {
    val recitations: List<YasserIraqiRecitationItem> = listOf(
        YasserIraqiRecitationItem(
            id = "ghafir_iraqi",
            surahNumber = 40,
            highlightAyahNumber = 44,
            titleAr = "سورة غافر — ﴿وَأُفَوِّضُ أَمْرِي إِلَى اللَّهِ﴾",
            titleEn = "Surah Ghafir — Iconic Iraqi Maqam",
            maqamTagAr = "مقام العراق الأشهر على يوتيوب",
            maqamTagEn = "Most Famous YouTube Iraqi Maqam",
            iconicAyahAr = "﴿فَسَتَذْكُرُونَ مَا أَقُولُ لَكُمْ ۚ وَأُفَوِّضُ أَمْرِي إِلَى اللَّهِ ۚ إِنَّ اللَّهَ بَصِيرٌ بِالْعِبَادِ﴾",
            youtubeQuery = "ياسر الدوسري مقام العراق سورة غافر وأفوض أمري إلى الله",
            eraNoteAr = "تلاوة خاشعة مهيبة من ليالي التراويح التي أبكت المصلين",
            eraNoteEn = "Iconic emotional Tarawih recitation in pure Iraqi Maqam"
        ),
        YasserIraqiRecitationItem(
            id = "taha_iraqi",
            surahNumber = 20,
            highlightAyahNumber = 14,
            titleAr = "سورة طه — ﴿إِنَّنِي أَنَا اللَّهُ لَا إِلَٰهَ إِلَّا أَنَا﴾",
            titleEn = "Surah Ta-Ha — Makkah Haram Iraqi Maqam",
            maqamTagAr = "مقام العراق الحرمي الخاشع",
            maqamTagEn = "Haramain Pure Iraqi Maqam",
            iconicAyahAr = "﴿إِنَّنِي أَنَا اللَّهُ لَا إِلَٰهَ إِلَّا أَنَا فَاعْبُدْنِي وَأَقِمِ الصَّلَاةَ لِذِكْرِي﴾",
            youtubeQuery = "ياسر الدوسري سورة طه بمقام العراق خاشعة",
            eraNoteAr = "من أروع التلاوات المنتشرة بنبرة مقام العراق المتدرجة بالسكينة",
            eraNoteEn = "Celebrated recitation blending deep serenity with ascending Iraqi cadence"
        ),
        YasserIraqiRecitationItem(
            id = "yusuf_iraqi",
            surahNumber = 12,
            highlightAyahNumber = 86,
            titleAr = "سورة يوسف — ﴿إِنَّمَا أَشْكُو بَثِّي وَحُزْنِي إِلَى اللَّهِ﴾",
            titleEn = "Surah Yusuf — Emotional Iraqi & Saba",
            maqamTagAr = "مقام العراق الممزوج بالصبا",
            maqamTagEn = "Iraqi Maqam blended with Saba",
            iconicAyahAr = "﴿قَالَ إِنَّمَا أَشْكُو بَثِّي وَحُزْنِي إِلَى اللَّهِ وَأَعْلَمُ مِنَ اللَّهِ مَا لَا تَعْلَمُونَ﴾",
            youtubeQuery = "ياسر الدوسري سورة يوسف مقام العراق إنما أشكو بثي وحزني",
            eraNoteAr = "تلاوة باكية شهيرة بمقام العراق الحزين في محراب المسجد الحرام",
            eraNoteEn = "Tearful Haramain recitation capturing Yaqub's supplication"
        ),
        YasserIraqiRecitationItem(
            id = "maryam_iraqi",
            surahNumber = 19,
            highlightAyahNumber = 4,
            titleAr = "سورة مريم — ﴿رَبِّ إِنِّي وَهَنَ الْعَظْمُ مِنِّي﴾",
            titleEn = "Surah Maryam — Night Prayer Iraqi Maqam",
            maqamTagAr = "مقام العراق التراويحي",
            maqamTagEn = "Tarawih Iraqi Maqam",
            iconicAyahAr = "﴿قَالَ رَبِّ إِنِّي وَهَنَ الْعَظْمُ مِنِّي وَاشْتَعَلَ الرَّأْسُ شَيْبًا وَلَمْ أَكُن بِدُعَائِكَ رَبِّ شَقِيًّا﴾",
            youtubeQuery = "ياسر الدوسري سورة مريم مقام العراق خاشعة",
            eraNoteAr = "مناجاة زكريا عليه السلام بنبرة عراقية هادئة تهز القلوب",
            eraNoteEn = "Deeply moving recitation of Prophet Zakariyya's quiet prayer"
        ),
        YasserIraqiRecitationItem(
            id = "muminun_iraqi",
            surahNumber = 23,
            highlightAyahNumber = 99,
            titleAr = "سورة المؤمنون — ﴿رَبِّ ارْجِعُونِ لَعَلِّي أَعْمَلُ صَالِحًا﴾",
            titleEn = "Surah Al-Mu'minun — Viral Khashia Climax",
            maqamTagAr = "مقام العراق المؤثر النادر",
            maqamTagEn = "Rare High-Register Iraqi Maqam",
            iconicAyahAr = "﴿حَتَّىٰ إِذَا جَاءَ أَحَدَهُمُ الْمَوْتُ قَالَ رَبِّ ارْجِعُونِ ۝ لَعَلِّي أَعْمَلُ صَالِحًا فِيمَا تَرَكْتُ﴾",
            youtubeQuery = "ياسر الدوسري سورة المؤمنون رب ارجعون مقام العراق",
            eraNoteAr = "من أقوى المقاطع انتشاراً للشيخ ياسر الدوسري في رمضان",
            eraNoteEn = "One of the most widely shared Ramadan emotional recitations"
        ),
        YasserIraqiRecitationItem(
            id = "zumar_iraqi",
            surahNumber = 39,
            highlightAyahNumber = 53,
            titleAr = "سورة الزمر — ﴿لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ﴾",
            titleEn = "Surah Az-Zumar — Call of Mercy",
            maqamTagAr = "مقام العراق الحاني",
            maqamTagEn = "Compassionate Iraqi Maqam",
            iconicAyahAr = "﴿قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ﴾",
            youtubeQuery = "ياسر الدوسري سورة الزمر لا تقنطوا من رحمة الله مقام العراق",
            eraNoteAr = "أرجى آية في كتاب الله بنغمة عراقية تبعث الأمل والطمأنينة",
            eraNoteEn = "The most hopeful verse in the Quran recited with soothing warmth"
        ),
        YasserIraqiRecitationItem(
            id = "qaf_iraqi",
            surahNumber = 50,
            highlightAyahNumber = 16,
            titleAr = "سورة ق — ﴿وَنَحْنُ أَقْرَبُ إِلَيْهِ مِنْ حَبْلِ الْوَرِيدِ﴾",
            titleEn = "Surah Qaf — Majestic Iraqi Maqam",
            maqamTagAr = "مقام العراق الجليل",
            maqamTagEn = "Majestic Iraqi Maqam",
            iconicAyahAr = "﴿وَلَقَدْ خَلَقْنَا الْإِنسَانَ وَنَعْلَمُ مَا تُوَسْوِسُ بِهِ نَفْسُهُ ۖ وَنَحْنُ أَقْرَبُ إِلَيْهِ مِنْ حَبْلِ الْوَرِيدِ﴾",
            youtubeQuery = "ياسر الدوسري سورة ق مقام العراق",
            eraNoteAr = "قوة في الأداء وخشوع عميق في آيات الحشر والبعث",
            eraNoteEn = "Commanding vocal resonance illuminating the awe of Surah Qaf"
        ),
        YasserIraqiRecitationItem(
            id = "qiyamah_iraqi",
            surahNumber = 75,
            highlightAyahNumber = 26,
            titleAr = "سورة القيامة — ﴿كَلَّا إِذَا بَلَغَتِ التَّرَاقِيَ﴾",
            titleEn = "Surah Al-Qiyamah — Rare Tearful Recitation",
            maqamTagAr = "مقام العراق الباكي",
            maqamTagEn = "Tearful Iraqi Cadence",
            iconicAyahAr = "﴿كَلَّا إِذَا بَلَغَتِ التَّرَاقِيَ ۝ وَقِيلَ مَنْ ۜ رَاقٍ ۝ وَظَنَّ أَنَّهُ الْفِرَاقُ﴾",
            youtubeQuery = "ياسر الدوسري سورة القيامة مقام العراق",
            eraNoteAr = "تلاوة نادرة خالدة تُعد من درر الشيخ ياسر الدوسري الصوتية",
            eraNoteEn = "Timeless rare recording prized by listeners worldwide"
        ),
        YasserIraqiRecitationItem(
            id = "rahman_iraqi",
            surahNumber = 55,
            highlightAyahNumber = 26,
            titleAr = "سورة الرحمن — ﴿كُلُّ مَنْ عَلَيْهَا فَانٍ﴾",
            titleEn = "Surah Ar-Rahman — Melodic Iraqi Maqam",
            maqamTagAr = "مقام العراق العذب",
            maqamTagEn = "Melodic Iraqi Maqam",
            iconicAyahAr = "﴿كُلُّ مَنْ عَلَيْهَا فَانٍ ۝ وَيَبْقَىٰ وَجْهُ رَبِّكَ ذُو الْجَلَالِ وَالْإِكْرَامِ﴾",
            youtubeQuery = "ياسر الدوسري سورة الرحمن مقام العراق",
            eraNoteAr = "نغمة عراقية صافية تتلألأ فيها فواصل سورة الرحمن",
            eraNoteEn = "Crystalline Iraqi melody highlighting the refrains of Ar-Rahman"
        ),
        YasserIraqiRecitationItem(
            id = "fajr_iraqi",
            surahNumber = 89,
            highlightAyahNumber = 27,
            titleAr = "سورة الفجر — ﴿يَا أَيَّتُهَا النَّفْسُ الْمُطْمَئِنَّةُ﴾",
            titleEn = "Surah Al-Fajr — Serene Finale",
            maqamTagAr = "مقام العراق المطمئن",
            maqamTagEn = "Tranquil Iraqi Finale",
            iconicAyahAr = "﴿يَا أَيَّتُهَا النَّفْسُ الْمُطْمَئِنَّةُ ۝ ارْجِعِي إِلَىٰ رَبِّكِ رَاضِيَةً مَّرْضِيَّةً﴾",
            youtubeQuery = "ياسر الدوسري سورة الفجر يا أيتها النفس المطمئنة مقام العراق",
            eraNoteAr = "ختام روحاني يملأ القلب بالسكينة والرضا",
            eraNoteEn = "Peaceful spiritual finale bringing deep stillness to the heart"
        )
    )

    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

/**
 * Viral YouTube-Famous Feature:
 * "خاشعات الشيخ ياسر الدوسري بمقام العراق" (Sheikh Yasser Al-Dosari — Iraqi Maqam Masterpieces Studio)
 * - Real live audio playback (Full Surah MP3 + Climax Ayah mode) with seekbar, speed & repeat.
 * - Animated golden Haramain acoustic halo visualizer.
 * - 1-tap open directly inside the Mushaf with `QuranReciter.YASSER_AL_DOSARI_IRAQI_MAQAM`.
 * - 1-tap open the iconic YouTube video recording safely via `Intent.ACTION_VIEW`.
 */
@Composable
fun YasserDosariIraqiMaqamScreen(
    onBack: () -> Unit,
    onOpenInMushafWithDosariIraqi: (surahNumber: Int) -> Unit,
    adminCustomTracks: List<com.example.data.quran.CustomReciterItem> = emptyList()
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val noorColors = LocalNoorColors.current

    val allRecitations = remember(adminCustomTracks) {
        val uploaded = adminCustomTracks
            .filter { it.hasPlayableAudio() }
            .map { track ->
                val surahMeta = com.example.data.local.catalog.SurahCatalog.findSurah(track.surahNumber.coerceIn(1, 114))
                YasserIraqiRecitationItem(
                    id = "admin_${track.id}",
                    surahNumber = track.surahNumber.coerceIn(1, 114),
                    highlightAyahNumber = 1,
                    titleAr = track.surahTitleAr.ifBlank { "سورة ${surahMeta.nameAr} — مقام العراق" },
                    titleEn = track.surahTitleEn.ifBlank { "Surah ${surahMeta.nameEn} — Iraqi Maqam" },
                    maqamTagAr = "مقام العراق · مضافة من لوحة التحكم",
                    maqamTagEn = "Iraqi Maqam · Admin Studio",
                    iconicAyahAr = "﴿سورة ${surahMeta.nameAr} — تلاوة مرفوعة للشيخ ياسر الدوسري بمقام العراق﴾",
                    youtubeQuery = "ياسر الدوسري سورة ${surahMeta.nameAr} مقام العراق",
                    eraNoteAr = track.fileName.ifBlank { "تلاوة MP3 مضافة مباشرة من لوحة التحكم" },
                    eraNoteEn = track.fileName.ifBlank { "Custom MP3 / audio link published from Admin Control Center" },
                    customAudioSource = track.resolvePlayableSource(),
                    isAdminUploaded = true
                )
            }
        uploaded + YasserDosariIraqiCatalog.recitations
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var fullSurahMode by remember { mutableStateOf(true) } // true = full Surah MP3, false = iconic Ayah clip
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var isRepeatOn by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var playerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    val safeSelectedIndex = selectedIndex.coerceIn(0, allRecitations.lastIndex.coerceAtLeast(0))
    val activeItem = allRecitations[safeSelectedIndex]

    fun releasePlayer() {
        runCatching {
            playerRef?.setOnPreparedListener(null)
            playerRef?.setOnCompletionListener(null)
            playerRef?.setOnErrorListener(null)
            if (playerRef?.isPlaying == true) playerRef?.stop()
            playerRef?.release()
        }
        playerRef = null
        isPlaying = false
        isBuffering = false
    }

    fun applySpeed(mp: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val params = mp.playbackParams ?: PlaybackParams()
                mp.playbackParams = params.setSpeed(speed)
            }
        }
    }

    fun startPlayback(index: Int, fullMode: Boolean) {
        val safeIdx = index.coerceIn(0, allRecitations.lastIndex.coerceAtLeast(0))
        val item = allRecitations[safeIdx]
        val url = if (fullMode) item.fullSurahAudioUrl() else item.climaxAyahAudioUrl()
        releasePlayer()
        selectedIndex = safeIdx
        fullSurahMode = fullMode
        currentPosMs = 0L
        durationMs = 0L
        errorMsg = null
        isBuffering = true

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { ready ->
                    isBuffering = false
                    durationMs = runCatching { ready.duration.coerceAtLeast(0).toLong() }.getOrDefault(0L)
                    ready.start()
                    if (kotlin.math.abs(playbackSpeed - 1.0f) >= 0.01f) {
                        applySpeed(ready, playbackSpeed)
                    }
                    isPlaying = true
                }
                setOnCompletionListener { done ->
                    if (isRepeatOn) {
                        runCatching {
                            done.seekTo(0)
                            done.start()
                        }
                    } else {
                        isPlaying = false
                        currentPosMs = durationMs
                    }
                }
                setOnErrorListener { _, _, _ ->
                    isBuffering = false
                    isPlaying = false
                    errorMsg = L10n.pick(
                        lang,
                        "تعذّر الاتصال بالبث الصوتي — تحقّق من الإنترنت وأعد المحاولة",
                        "Unable to reach audio stream — check internet connection"
                    )
                    true
                }
                prepareAsync()
            }
            playerRef = mp
        } catch (_: Exception) {
            isBuffering = false
            isPlaying = false
            errorMsg = L10n.pick(lang, "تعذّر تشغيل التلاوة الآن", "Unable to start playback")
        }
    }

    DisposableEffect(Unit) {
        onDispose { releasePlayer() }
    }

    LaunchedEffect(isPlaying, playerRef) {
        while (isActive && isPlaying) {
            val mp = playerRef
            if (mp != null) {
                currentPosMs = runCatching { mp.currentPosition.toLong().coerceAtLeast(0L) }.getOrDefault(currentPosMs)
                if (durationMs <= 0L) {
                    durationMs = runCatching { mp.duration.toLong().coerceAtLeast(0L) }.getOrDefault(0L)
                }
            }
            delay(250L)
        }
    }

    val waveTransition = rememberInfiniteTransition(label = "iraqi_maqam_wave")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_phase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("yasser_dosari_iraqi_maqam_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "ياسر الدوسري · مقام العراق", "Yasser Al-Dosari · Iraqi Maqam"),
            subtitle = L10n.pick(lang, "أشهر التلاوات الخاشعة المنتشرة على يوتيوب والحرم المكي", "Iconic Viral YouTube & Haramain Khashia Studio"),
            onBack = {
                releasePlayer()
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Luxury Acoustic Studio Hero Player
            item {
                LiquidGlassCard(contentPadding = PaddingValues(16.dp)) {
                    // Golden Acoustic Halo Visualizer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF153D31), Color(0xFF081F19), Color(0xFF04100D))
                                )
                            )
                            .border(1.2.dp, GoldBright.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val baseR = size.minDimension * 0.28f

                            // Concentric golden Haramain resonance rings
                            for (ring in 1..3) {
                                val amp = if (isPlaying) 6f * sin(wavePhase + ring * 1.1f) else 0f
                                drawCircle(
                                    color = GoldBright.copy(alpha = 0.24f / ring),
                                    radius = baseR + ring * 18f + amp,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 2f)
                                )
                            }

                            // 32 Golden Acoustic Bars circling the emblem
                            val barCount = 32
                            for (i in 0 until barCount) {
                                val angle = (2.0 * PI * i) / barCount
                                val barLen = if (isPlaying) {
                                    10f + 16f * kotlin.math.abs(sin(wavePhase * 1.5f + i * 0.45f))
                                } else {
                                    6f
                                }
                                val r1 = baseR + 4f
                                val r2 = r1 + barLen
                                drawLine(
                                    color = if (i % 2 == 0) GoldBright else EmeraldPrimary,
                                    start = Offset(cx + (cos(angle) * r1).toFloat(), cy + (sin(angle) * r1).toFloat()),
                                    end = Offset(cx + (cos(angle) * r2).toFloat(), cy + (sin(angle) * r2).toFloat()),
                                    strokeWidth = 3.2f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Central Play/Pause Orb
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(GoldBright, GoldPrimary))
                                )
                                .clickable {
                                    val mp = playerRef
                                    if (isBuffering) return@clickable
                                    if (mp != null) {
                                        if (isPlaying) {
                                            runCatching { mp.pause() }
                                            isPlaying = false
                                        } else {
                                            runCatching { mp.start() }
                                            isPlaying = true
                                        }
                                    } else {
                                        startPlayback(selectedIndex, fullSurahMode)
                                    }
                                }
                                .testTag("yasser_iraqi_main_play_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF071B14)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = L10n.pick(lang, "تشغيل وإيقاف", "Play/Pause"),
                                    tint = Color(0xFF071B14),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Top tag badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xBB041410))
                                .border(1.dp, GoldBright.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, activeItem.maqamTagAr, activeItem.maqamTagEn),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title & Iconic Quranic Verse Quote
                    Text(
                        text = L10n.pick(lang, activeItem.titleAr, activeItem.titleEn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GoldPrimary.copy(alpha = 0.10f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = activeItem.iconicAyahAr,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldBright,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Switch: Full Surah vs Viral Climax Ayah
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassChip(
                            label = L10n.pick(lang, "السورة كاملة (الدوسري)", "Full Surah"),
                            selected = fullSurahMode,
                            onClick = { startPlayback(selectedIndex, true) },
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassChip(
                            label = L10n.pick(lang, "مقطع الذروة الخاشعة (آية ${activeItem.highlightAyahNumber})", "Climax Ayah #${activeItem.highlightAyahNumber}"),
                            selected = !fullSurahMode,
                            onClick = { startPlayback(selectedIndex, false) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seekable Progress Slider
                    val sliderVal = if (durationMs > 0L) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                    Slider(
                        value = sliderVal,
                        onValueChange = { frac ->
                            val targetMs = (frac * durationMs).toLong()
                            currentPosMs = targetMs
                            runCatching { playerRef?.seekTo(targetMs.toInt()) }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = GoldBright,
                            activeTrackColor = GoldBright,
                            inactiveTrackColor = GoldPrimary.copy(alpha = 0.25f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = YasserDosariIraqiCatalog.formatDuration(currentPosMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = YasserDosariIraqiCatalog.formatDuration(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Transport Controls: Prev / Speed / Repeat / Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val prev = if (safeSelectedIndex > 0) safeSelectedIndex - 1 else allRecitations.lastIndex
                                startPlayback(prev, fullSurahMode)
                            }
                        ) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = GoldBright)
                        }

                        LiquidGlassChip(
                            label = "${String.format(Locale.US, "%.2gx", playbackSpeed)}",
                            selected = playbackSpeed != 1.0f,
                            onClick = {
                                playbackSpeed = when {
                                    playbackSpeed < 0.95f -> 1.0f
                                    playbackSpeed < 1.10f -> 1.15f
                                    else -> 0.85f
                                }
                                playerRef?.let { mp ->
                                    if (isPlaying) applySpeed(mp, playbackSpeed)
                                }
                            }
                        )

                        IconButton(onClick = { isRepeatOn = !isRepeatOn }) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeatOn) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                val next = (safeSelectedIndex + 1) % allRecitations.size
                                startPlayback(next, fullSurahMode)
                            }
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = GoldBright)
                        }
                    }

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMsg!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Two Killer Action Buttons: Open in Mushaf with Iraqi Maqam + Watch Viral YouTube Recording
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                releasePlayer()
                                onOpenInMushafWithDosariIraqi(activeItem.surahNumber)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("yasser_iraqi_open_mushaf_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "افتح بالمصحف", "Open in Mushaf"),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeItem.youtubeSearchUrl()))
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("yasser_iraqi_youtube_btn"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color(0xFFFF4D4D), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L10n.pick(lang, "شاهد على يوتيوب", "Watch YouTube"),
                                fontWeight = FontWeight.Bold,
                                color = GoldBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Section Title for All 10 Viral Recitations
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(
                            lang,
                            "روائع مقام العراق للشيخ ياسر الدوسري (${allRecitations.size} تلاوات)",
                            "${allRecitations.size} Viral Iraqi-Maqam Masterpieces"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
            }

            itemsIndexed(allRecitations, key = { _, item -> item.id }) { idx, item ->
                val isSelected = idx == safeSelectedIndex
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { startPlayback(idx, fullSurahMode) }
                        .testTag("yasser_iraqi_track_${item.id}"),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) GoldPrimary.copy(alpha = 0.28f) else EmeraldDeep.copy(alpha = 0.55f)
                                )
                                .border(
                                    1.2.dp,
                                    if (isSelected) GoldBright else GoldPrimary.copy(alpha = 0.35f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected && isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, item.titleAr, item.titleEn),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) GoldBright else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = L10n.pick(lang, item.maqamTagAr, item.maqamTagEn),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = L10n.pick(lang, item.eraNoteAr, item.eraNoteEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
