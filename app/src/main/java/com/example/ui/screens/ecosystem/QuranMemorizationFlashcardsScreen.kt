package com.example.ui.screens.ecosystem

import android.content.Context
import android.media.AudioAttributes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class HifzFlashcard(
    val id: String,
    val deckAr: String,
    val promptAr: String,
    val answerAyahAr: String,
    val surahRefAr: String,
    val surahNumber: Int,
    val audioUrl: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val nextDueAtMillis: Long = 0L
)

/**
 * Pure SM-2 Spaced Repetition Algorithm tailored for Quran Hifz Mastery.
 * Quality scale: 1 = Hard/Forgot (reset), 3 = Good, 5 = Perfect Recall.
 */
object HifzSpacedRepetitionEngine {
    const val DAY_MS = 86_400_000L

    val starterCards: List<HifzFlashcard> = listOf(
        HifzFlashcard(
            "mulk_1", "روابط جزء تبارك",
            "أكمل مطلع سورة الملك: ﴿تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ ...﴾",
            "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            "سورة الملك — ١", 67,
            "https://everyayah.com/data/Alafasy_128kbps/067001.mp3"
        ),
        HifzFlashcard(
            "kahf_1", "روابط يوم الجمعة",
            "أكمل الآية الأولى من سورة الكهف: ﴿الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ ...﴾",
            "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجَا",
            "سورة الكهف — ١", 18,
            "https://everyayah.com/data/Alafasy_128kbps/018001.mp3"
        ),
        HifzFlashcard(
            "mutashabih_baqarah_1", "متشابهات الحفّاظ",
            "ما الفرق بين «تَجْرِي مِن تَحْتِهَا الْأَنْهَارُ» و«تَجْرِي تَحْتَهَا الْأَنْهَارُ» في التوبة ١٠٠؟",
            "في سورة التوبة آية ١٠٠ وحدها جاءت بحذف «مِن»: ﴿جَنَّاتٍ تَجْرِي تَحْتَهَا الْأَنْهَارُ خَالِدِينَ فِيهَا أَبَدًا﴾",
            "سورة التوبة — ١٠٠", 9,
            "https://everyayah.com/data/Husary_128kbps/009100.mp3"
        ),
        HifzFlashcard(
            "naba_1", "روابط جزء عمّ",
            "أكمل الآيات الثلاث الأولى من سورة النبأ: ﴿عَمَّ يَتَسَاءَلُونَ ...﴾",
            "عَمَّ يَتَسَاءَلُونَ ۝ عَنِ النَّبَإِ الْعَظِيمِ ۝ الَّذِي هُمْ فِيهِ مُخْتَلِفُونَ",
            "سورة النبأ — ١–٣", 78,
            "https://everyayah.com/data/Alafasy_128kbps/078001.mp3"
        ),
        HifzFlashcard(
            "hashr_22", "خواتيم السور العظيمة",
            "أكمل أواخر سورة الحشر: ﴿هُوَ اللَّهُ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ عَالِمُ الْغَيْبِ ...﴾",
            "هُوَ اللَّهُ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ عَالِمُ الْغَيْبِ وَالشَّهَادَةِ هُوَ الرَّحْمَٰنُ الرَّحِيمُ",
            "سورة الحشر — ٢٢", 59,
            "https://everyayah.com/data/Minshawy_Murattal_128kbps/059022.mp3"
        ),
        HifzFlashcard(
            "baqarah_285", "خواتيم سورة البقرة",
            "أكمل الآية: ﴿آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ ...﴾",
            "آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ وَالْمُؤْمِنُونَ ۚ كُلٌّ آمَنَ بِاللَّهِ وَمَلَائِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ",
            "سورة البقرة — ٢٨٥", 2,
            "https://everyayah.com/data/Alafasy_128kbps/002285.mp3"
        ),
        HifzFlashcard(
            "yasin_1", "قلب القرآن — يس",
            "أكمل مطلع سورة يس: ﴿يس ۝ وَالْقُرْآنِ الْحَكِيمِ ...﴾",
            "يس ۝ وَالْقُرْآنِ الْحَكِيمِ ۝ إِنَّكَ لَمِنَ الْمُرْسَلِينَ ۝ عَلَىٰ صِرَاطٍ مُّسْتَقِيمٍ",
            "سورة يس — ١–٤", 36,
            "https://everyayah.com/data/Minshawy_Murattal_128kbps/036001.mp3"
        ),
        HifzFlashcard(
            "duha_1", "جزء عمّ — السكينة",
            "أكمل الآية الثالثة من الضحى: ﴿مَا وَدَّعَكَ رَبُّكَ ...﴾",
            "مَا وَدَّعَكَ رَبُّكَ وَمَا قَلَىٰ ۝ وَلَلْآخِرَةُ خَيْرٌ لَّكَ مِنَ الْأُولَىٰ",
            "سورة الضحى — ٣–٤", 93,
            "https://everyayah.com/data/Alafasy_128kbps/093003.mp3"
        )
    )

    fun reviewCard(card: HifzFlashcard, quality: Int, nowMillis: Long = System.currentTimeMillis()): HifzFlashcard {
        val q = quality.coerceIn(0, 5)
        return if (q < 3) {
            card.copy(
                repetitions = 0,
                intervalDays = 1,
                easeFactor = (card.easeFactor - 0.2).coerceAtLeast(1.3),
                nextDueAtMillis = nowMillis + DAY_MS
            )
        } else {
            val nextReps = card.repetitions + 1
            val nextInterval = when (nextReps) {
                1 -> 1
                2 -> 6
                else -> (card.intervalDays * card.easeFactor).roundToInt().coerceAtLeast(1)
            }
            val nextEase = (card.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)
            card.copy(
                repetitions = nextReps,
                intervalDays = nextInterval,
                easeFactor = nextEase,
                nextDueAtMillis = nowMillis + nextInterval * DAY_MS
            )
        }
    }

    fun computeMasteryPercent(cards: List<HifzFlashcard>): Int {
        if (cards.isEmpty()) return 0
        val mastered = cards.count { it.repetitions >= 2 }
        return ((mastered * 100.0) / cards.size).roundToInt().coerceIn(0, 100)
    }
}

private object HifzFlashcardStorage {
    private const val PREFS = "noor_hifz_sm2_prefs"
    private const val KEY_CARDS = "cards_json"

    fun load(context: Context): List<HifzFlashcard> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CARDS, null)
        if (raw.isNullOrBlank()) return HifzSpacedRepetitionEngine.starterCards
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<HifzFlashcard>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += HifzFlashcard(
                    id = o.getString("id"),
                    deckAr = o.optString("deckAr", "بطاقاتي"),
                    promptAr = o.getString("promptAr"),
                    answerAyahAr = o.getString("answerAyahAr"),
                    surahRefAr = o.optString("surahRefAr", ""),
                    surahNumber = o.optInt("surahNumber", 1),
                    audioUrl = o.optString("audioUrl", ""),
                    repetitions = o.optInt("repetitions", 0),
                    intervalDays = o.optInt("intervalDays", 0),
                    easeFactor = o.optDouble("easeFactor", 2.5),
                    nextDueAtMillis = o.optLong("nextDueAtMillis", 0L)
                )
            }
            if (list.isEmpty()) HifzSpacedRepetitionEngine.starterCards else list
        }.getOrElse { HifzSpacedRepetitionEngine.starterCards }
    }

    fun save(context: Context, cards: List<HifzFlashcard>) {
        val arr = JSONArray()
        cards.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("deckAr", c.deckAr)
                put("promptAr", c.promptAr)
                put("answerAyahAr", c.answerAyahAr)
                put("surahRefAr", c.surahRefAr)
                put("surahNumber", c.surahNumber)
                put("audioUrl", c.audioUrl)
                put("repetitions", c.repetitions)
                put("intervalDays", c.intervalDays)
                put("easeFactor", c.easeFactor)
                put("nextDueAtMillis", c.nextDueAtMillis)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CARDS, arr.toString()).apply()
    }
}

@Composable
fun QuranMemorizationFlashcardsScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var cards by remember { mutableStateOf(HifzFlashcardStorage.load(context)) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var reviewedToday by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var isAudioLoading by remember { mutableStateOf(false) }

    fun stopAudio() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        isAudioPlaying = false
        isAudioLoading = false
    }

    fun playAyahAudio(url: String) {
        if (url.isBlank()) return
        if (isAudioPlaying || isAudioLoading) {
            stopAudio()
            return
        }
        stopAudio()
        isAudioLoading = true
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { p ->
                isAudioLoading = false
                isAudioPlaying = true
                p.start()
            }
            setOnCompletionListener { isAudioPlaying = false }
            setOnErrorListener { _, _, _ ->
                isAudioLoading = false
                isAudioPlaying = false
                true
            }
        }
        mediaPlayer = mp
        runCatching {
            mp.setDataSource(url)
            mp.prepareAsync()
        }.onFailure { stopAudio() }
    }

    DisposableEffect(Unit) {
        onDispose { stopAudio() }
    }

    val safeIndex = currentIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    val currentCard = cards.getOrNull(safeIndex)
    val masteryPct = HifzSpacedRepetitionEngine.computeMasteryPercent(cards)

    fun gradeCurrent(quality: Int) {
        val card = currentCard ?: return
        stopAudio()
        val updatedCard = HifzSpacedRepetitionEngine.reviewCard(card, quality)
        val nextList = cards.toMutableList().apply { set(safeIndex, updatedCard) }
        cards = nextList
        HifzFlashcardStorage.save(context, nextList)
        reviewedToday += 1
        revealed = false
        currentIndex = (safeIndex + 1) % nextList.size
    }

    Column(modifier = Modifier.fillMaxSize().testTag("hifz_sm2_screen")) {
        NoorScreenHeader(
            title = L10n.pick(lang, "المراجعة المتباعدة الذكية للحفظ", "SM-2 Spaced Repetition Hifz"),
            subtitle = L10n.pick(
                lang,
                "خوارزمية SM-2 العلمية لترسيخ الآيات والمتشابهات مع التلاوة الصوتية",
                "Real SM-2 scientific memory engine + live Quran audio check"
            ),
            onBack = {
                stopAudio()
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mastery & Queue Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Sm2StatPill(L10n.pick(lang, "نسبة الإتقان", "Mastery"), "$masteryPct%", GoldBright, Modifier.weight(1f))
                    Sm2StatPill(L10n.pick(lang, "البطاقات", "Deck Size"), "${cards.size}", Color(0xFF38BDF8), Modifier.weight(1f))
                    Sm2StatPill(L10n.pick(lang, "تمت مراجعتها", "Reviewed"), "$reviewedToday", Color(0xFF34D399), Modifier.weight(1f))
                }
            }

            // Active SM-2 Flashcard
            if (currentCard != null) {
                item {
                    LiquidGlassCard(strong = true, goldBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${currentCard.deckAr} • ${safeIndex + 1}/${cards.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBright
                                )
                                Text(
                                    text = if (currentCard.intervalDays > 0) {
                                        L10n.pick(lang, "الفاصل: ${currentCard.intervalDays} يوم", "Interval: ${currentCard.intervalDays}d")
                                    } else {
                                        L10n.pick(lang, "بطاقة جديدة", "New Card")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF7DD3FC),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(EmeraldDeep.copy(alpha = 0.65f))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentCard.promptAr,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 34.sp
                                )
                            }

                            if (!revealed) {
                                Button(
                                    onClick = { revealed = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("sm2_reveal_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                ) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = null, tint = EmeraldDeep)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = L10n.pick(lang, "إظهار الآية والتحقق من الحفظ", "Reveal Ayah & Check Recall"),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldDeep
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.2f))
                                        .border(1.2.dp, Color(0xFF34D399).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = currentCard.answerAyahAr,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 23.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldBright,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 36.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentCard.surahRefAr,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF7DD3FC)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (currentCard.audioUrl.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = { playAyahAudio(currentCard.audioUrl) },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    if (isAudioLoading) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    } else {
                                                        Icon(
                                                            if (isAudioPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                                            contentDescription = null,
                                                            tint = GoldBright
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isAudioPlaying) L10n.pick(lang, "إيقاف", "Stop") else L10n.pick(lang, "استمع للآية", "Listen"),
                                                        color = GoldBright
                                                    )
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { onOpenSurah(currentCard.surahNumber) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = Color(0xFF34D399))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(L10n.pick(lang, "المصحف", "Mushaf"), color = Color(0xFF34D399))
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = L10n.pick(lang, "قيّم حفظك لجدولة المراجعة الذكية القادمة:", "Rate recall to schedule next SM-2 review:"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = noorColors.goldAccent
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { gradeCurrent(1) },
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("sm2_grade_hard"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                    ) {
                                        Text(L10n.pick(lang, "صعب (غداً)", "Hard (1d)"), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { gradeCurrent(3) },
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("sm2_grade_good"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text(L10n.pick(lang, "جيد", "Good"), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { gradeCurrent(5) },
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("sm2_grade_easy"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Text(L10n.pick(lang, "متقن ★", "Mastered"), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add Custom Card Action
            item {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sm2_add_card_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = GoldBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L10n.pick(lang, "إضافة بطاقة حفظ قرآني خاصة بك", "Add Custom Hifz Flashcard"),
                        color = GoldBright,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var promptInput by remember { mutableStateOf("") }
        var answerInput by remember { mutableStateOf("") }
        var surahRefInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(L10n.pick(lang, "إضافة بطاقة حفظ جديدة", "Add New Hifz Card"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text(L10n.pick(lang, "السؤال / بداية الآية", "Prompt / Opening Words")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        label = { Text(L10n.pick(lang, "الآية الكاملة (الإجابة)", "Full Ayah (Answer)")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = surahRefInput,
                        onValueChange = { surahRefInput = it },
                        label = { Text(L10n.pick(lang, "السورة والآية (اختياري)", "Surah Reference")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (promptInput.isNotBlank() && answerInput.isNotBlank()) {
                            val newCard = HifzFlashcard(
                                id = "custom_${System.currentTimeMillis()}",
                                deckAr = "بطاقاتي الخاصة",
                                promptAr = promptInput.trim(),
                                answerAyahAr = answerInput.trim(),
                                surahRefAr = surahRefInput.trim().ifBlank { "القرآن الكريم" },
                                surahNumber = 1,
                                audioUrl = ""
                            )
                            val updated = listOf(newCard) + cards
                            cards = updated
                            HifzFlashcardStorage.save(context, updated)
                            currentIndex = 0
                            revealed = false
                            showAddDialog = false
                        }
                    }
                ) {
                    Text(L10n.pick(lang, "حفظ البطاقة", "Save Card"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun Sm2StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
