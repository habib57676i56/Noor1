package com.example.ui.screens.ecosystem

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

data class WaqfSignRule(
    val id: String,
    val symbol: String,
    val nameAr: String,
    val nameEn: String,
    val rulingAr: String,
    val rulingEn: String,
    val meaningImpactAr: String,
    val meaningImpactEn: String,
    val exampleAyahAr: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahRefAr: String,
    val surahRefEn: String,
    val badgeColorHex: Long
) {
    fun audioUrl(): String {
        val s = surahNumber.toString().padStart(3, '0')
        val a = ayahNumber.toString().padStart(3, '0')
        return "https://everyayah.com/data/Husary_128kbps/${s}${a}.mp3"
    }
}

data class WaqfQuizQuestion(
    val id: String,
    val promptAr: String,
    val promptEn: String,
    val ayahAr: String,
    val optionsAr: List<String>,
    val optionsEn: List<String>,
    val correctIndex: Int,
    val explanationAr: String,
    val explanationEn: String
)

/**
 * Authentic Madinah Mushaf Waqf & Ibtida (Pausing & Starting) Rules + Breath-Span Calculator.
 */
object QuranWaqfCatalog {
    val signs: List<WaqfSignRule> = listOf(
        WaqfSignRule(
            id = "lazim",
            symbol = "مـ",
            nameAr = "الوقف اللازم",
            nameEn = "Mandatory Stop (Lazim)",
            rulingAr = "يلزم الوقف هنا — الوصل يُوهم معنىً غير مراد!",
            rulingEn = "Must stop here — continuing alters or distorts the intended meaning.",
            meaningImpactAr = "في ﴿إِنَّمَا يَسْتَجِيبُ الَّذِينَ يَسْمَعُونَ ۘ وَالْمَوْتَىٰ يَبْعَثُهُمُ اللَّهُ﴾ لو وصلتَ لأوهمتَ أن الموتى يسمعون ويستجيبون!",
            meaningImpactEn = "In 6:36, continuing without stopping makes it sound as if the dead also respond!",
            exampleAyahAr = "﴿إِنَّمَا يَسْتَجِيبُ الَّذِينَ يَسْمَعُونَ ۘ وَالْمَوْتَىٰ يَبْعَثُهُمُ اللَّهُ﴾",
            surahNumber = 6,
            ayahNumber = 36,
            surahRefAr = "الأنعام · آية ٣٦",
            surahRefEn = "Al-An'am · 6:36",
            badgeColorHex = 0xFFEF4444
        ),
        WaqfSignRule(
            id = "mamnu",
            symbol = "لا",
            nameAr = "الوقف الممنوع",
            nameEn = "Prohibited Stop (La)",
            rulingAr = "لا تقف هنا — الكلام لا يزال متصلاً بما بعده في المعنى والإعراب.",
            rulingEn = "Do not stop here — the phrase is grammatically tied to what follows.",
            meaningImpactAr = "الوقف على موضع النهي يقطع المعنى قبل اكتماله، وإن اضطررت لانقطاع النَّفَس فارجع كلمةً أو كلمتين.",
            meaningImpactEn = "Stopping breaks the sentence mid-thought; if out of breath, resume 1–2 words earlier.",
            exampleAyahAr = "﴿الَّذِينَ تَتَوَفَّاهُمُ الْمَلَائِكَةُ طَيِّبِينَ ۙ يَقُولُونَ سَلَامٌ عَلَيْكُمُ﴾",
            surahNumber = 16,
            ayahNumber = 32,
            surahRefAr = "النحل · آية ٣٢",
            surahRefEn = "An-Nahl · 16:32",
            badgeColorHex = 0xFFF97316
        ),
        WaqfSignRule(
            id = "qala",
            symbol = "قلى",
            nameAr = "الوقف أولى",
            nameEn = "Stopping Preferred (Qala)",
            rulingAr = "يجوز الوصل والوقف، ولكن الوقف هنا أفضل وأتم للمعنى.",
            rulingEn = "Both are valid, yet stopping is clearer and preferred.",
            meaningImpactAr = "يُبرز المعنى المستقل للجملة ويجعل التلاوة أهدأ وأقرب للتدبر.",
            meaningImpactEn = "Highlights the complete clause and enriches reflection.",
            exampleAyahAr = "﴿قُل رَّبِّي أَعْلَمُ بِعِدَّتِهِم مَّا يَعْلَمُهُمْ إِلَّا قَلِيلٌ ۗ فَلَا تُمَارِ فِيهِمْ﴾",
            surahNumber = 18,
            ayahNumber = 22,
            surahRefAr = "الكهف · آية ٢٢",
            surahRefEn = "Al-Kahf · 18:22",
            badgeColorHex = 0xFF10B981
        ),
        WaqfSignRule(
            id = "sala",
            symbol = "صلى",
            nameAr = "الوصل أولى",
            nameEn = "Continuing Preferred (Sala)",
            rulingAr = "يجوز الوقف، لكن الوصل أولى لارتباط الجملتين في السياق.",
            rulingEn = "Stopping is permitted, though continuing smoothly is preferred.",
            meaningImpactAr = "يحفظ ترابط السياق البلاغي مع جواز أخذ النَّفَس عند الحاجة.",
            meaningImpactEn = "Preserves rhetorical flow while permitting a breath if needed.",
            exampleAyahAr = "﴿وَإِن يَمْسَسْكَ اللَّهُ بِضُرٍّ فَلَا كَاشِفَ لَهُ إِلَّا هُوَ ۖ وَإِن يَمْسَسْكَ بِخَيْرٍ﴾",
            surahNumber = 6,
            ayahNumber = 17,
            surahRefAr = "الأنعام · آية ١٧",
            surahRefEn = "Al-An'am · 6:17",
            badgeColorHex = 0xFF38BDF8
        ),
        WaqfSignRule(
            id = "jaiz",
            symbol = "ج",
            nameAr = "الوقف الجائز المستوي",
            nameEn = "Permissible Stop (Jaiz)",
            rulingAr = "يستوي فيه الوقف والوصل — اختر ما يناسب نَفَسك وتدبّرك.",
            rulingEn = "Stopping and continuing are equally balanced.",
            meaningImpactAr = "علامة مرنة تمنح القارئ حرية التوقف أو المتابعة دون تغيّر المعنى.",
            meaningImpactEn = "Flexible pause point without affecting the verse meaning.",
            exampleAyahAr = "﴿نَحْنُ نَقُصُّ عَلَيْكَ نَبَأَهُم بِالْحَقِّ ۚ إِنَّهُمْ فِتْيَةٌ آمَنُوا بِرَبِّهِمْ﴾",
            surahNumber = 18,
            ayahNumber = 13,
            surahRefAr = "الكهف · آية ١٣",
            surahRefEn = "Al-Kahf · 18:13",
            badgeColorHex = 0xFFF5C85B
        ),
        WaqfSignRule(
            id = "muanaqah",
            symbol = "ۛ  ۛ",
            nameAr = "وقف المعانقة",
            nameEn = "Embracing Pause (Mu'anaqah)",
            rulingAr = "تقف على أحد الموضعين فقط — لا تقف عليهما معاً!",
            rulingEn = "Stop at ONE of the two triple-dot marks — never both!",
            meaningImpactAr = "إما ﴿لَا رَيْبَ ۛ﴾ ثم تبدأ ﴿فِيهِ هُدًى لِّلْمُتَّقِينَ﴾، أو ﴿لَا رَيْبَ فِيهِ ۛ﴾ ثم تبدأ ﴿هُدًى لِّلْمُتَّقِينَ﴾.",
            meaningImpactEn = "Either pause after 'La Rayb' OR after 'Fihi', never both together.",
            exampleAyahAr = "﴿ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ﴾",
            surahNumber = 2,
            ayahNumber = 2,
            surahRefAr = "البقرة · آية ٢",
            surahRefEn = "Al-Baqarah · 2:2",
            badgeColorHex = 0xFFA78BFA
        ),
        WaqfSignRule(
            id = "sakt",
            symbol = "س",
            nameAr = "السكتة اللطيفة (حفص)",
            nameEn = "Breathless Sakt (Hafs)",
            rulingAr = "توقف صوتي لطيف بمقدار حركتين بدون أخذ نَفَس جديد.",
            rulingEn = "Brief 2-beat silent pause WITHOUT taking a breath.",
            meaningImpactAr = "من السكتات الواجبة الأربع في حفص عن عاصم: عِوَجَا (الكهف)، مَرْقَدِنَا (يس)، مَنْ رَاقٍ (القيامة)، بَلْ رَانَ (المطففين).",
            meaningImpactEn = "One of the 4 obligatory Sakt pauses in Hafs 'an 'Asim.",
            exampleAyahAr = "﴿وَقِيلَ مَنْ ۜ رَاقٍ ۝ وَظَنَّ أَنَّهُ الْفِرَاقُ﴾",
            surahNumber = 75,
            ayahNumber = 27,
            surahRefAr = "القيامة · آية ٢٧",
            surahRefEn = "Al-Qiyamah · 75:27",
            badgeColorHex = 0xFF34D399
        )
    )

    val quizQuestions: List<WaqfQuizQuestion> = listOf(
        WaqfQuizQuestion(
            id = "q1",
            promptAr = "ما حكم العلامة (ۛ  ۛ) في قوله تعالى: ﴿لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ﴾؟",
            promptEn = "What is the rule of the double three-dot sign (ۛ  ۛ) in 2:2?",
            ayahAr = "﴿ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ﴾",
            optionsAr = listOf("تقف على الموضعين معاً", "تقف على أحد الموضعين فقط (المعانقة)", "يُمنع الوقف تماماً"),
            optionsEn = listOf("Stop at both marks", "Stop at ONE mark only (Mu'anaqah)", "Never stop at either"),
            correctIndex = 1,
            explanationAr = "أحسنت! هذا وقف المعانقة: إذا وقفت على الأول لم تقف على الثاني، والعكس صحيح.",
            explanationEn = "Correct! In Mu'anaqah you pause at either the first or the second mark, not both."
        ),
        WaqfQuizQuestion(
            id = "q2",
            promptAr = "ماذا تفعل عند رؤية حرف (س) الصغير فوق ﴿مَنْ ۜ رَاقٍ﴾ برواية حفص؟",
            promptEn = "What should you do when you see the small (س) on 'Man Raq' in Hafs?",
            ayahAr = "﴿وَقِيلَ مَنْ ۜ رَاقٍ﴾",
            optionsAr = listOf("سكتة لطيفة حركتين بدون تنفّس", "إدغام النون في الراء مباشرة", "وقف طويل مع أخذ نفس عميق"),
            optionsEn = listOf("Brief 2-beat pause without breathing", "Merge Nun directly into Ra", "Long stop with deep breath"),
            correctIndex = 0,
            explanationAr = "ممتاز! السكت هو قطع الصوت زمناً يسيراً من غير تنفّس بنية استئناف القراءة.",
            explanationEn = "Spot on! Sakt is a crisp brief pause without inhaling before continuing."
        ),
        WaqfQuizQuestion(
            id = "q3",
            promptAr = "ما الفرق بين (قلى) و(صلى) في المصحف الشريف؟",
            promptEn = "What is the difference between (Qala) and (Sala) in the Mushaf?",
            ayahAr = "قلى ⇄ صلى",
            optionsAr = listOf("قلى: الوصل أولى · صلى: الوقف أولى", "قلى: الوقف أولى · صلى: الوصل أولى", "كلاهما وقف لازم"),
            optionsEn = listOf("Qala: continue · Sala: stop", "Qala: stop preferred · Sala: continue preferred", "Both are mandatory stops"),
            correctIndex = 1,
            explanationAr = "صحيح! (قلى) اختصار «الوقف أولى» و(صلى) اختصار «الوصل أولى».",
            explanationEn = "Correct! Qala means stopping is preferable, while Sala means continuing is preferable."
        ),
        WaqfQuizQuestion(
            id = "q4",
            promptAr = "لماذا وُضعت علامة الوقف اللازم (مـ) بعد ﴿يَسْمَعُونَ﴾ في سورة الأنعام ٣٦؟",
            promptEn = "Why is the Mandatory Stop (مـ) placed after 'Yasma'un' in 6:36?",
            ayahAr = "﴿إِنَّمَا يَسْتَجِيبُ الَّذِينَ يَسْمَعُونَ ۘ وَالْمَوْتَىٰ يَبْعَثُهُمُ اللَّهُ﴾",
            optionsAr = listOf("لأن الوصل يُوهم أن الموتى يستجيبون!", "لأن الآية انتهت", "لإظهار الغنة فقط"),
            optionsEn = listOf("Continuing wrongly implies the dead respond!", "Because the verse ends there", "Only for Ghunnah"),
            correctIndex = 0,
            explanationAr = "رائع! الوقف اللازم يحمي المعنى القرآني من اللبس والفساد.",
            explanationEn = "Well done! Waqf Lazim protects the verse from unintended meaning."
        )
    )

    /**
     * Estimates how many medium Madinah Mushaf lines can be recited comfortably in one breath
     * (~7.5 seconds per standard Murattal line).
     */
    fun estimateRecitableLines(breathSeconds: Double): Double =
        (breathSeconds / 7.5).coerceAtLeast(0.0)

    fun classifyBreathLevelAr(breathSeconds: Double): String = when {
        breathSeconds < 8.0 -> "نَفَس مبتدئ — يُنصح بتمرين الاستنشاق البطيء"
        breathSeconds < 15.0 -> "نَفَس متوسط — يكفي لآيتين قصيرتين براحة"
        breathSeconds < 24.0 -> "نَفَس قرآني متقن — يكفي لسطرين إلى ثلاثة أسطر"
        else -> "نَفَس القُرّاء الكبار — ما شاء الله، تحكّم ممتاز!"
    }
}

/**
 * Interactive Quranic Waqf & Ibtida Lab + Live Recitation Breath-Span Trainer + Meaning Quiz.
 */
@Composable
fun QuranWaqfLabScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val noorColors = LocalNoorColors.current
    val prefs = remember { context.getSharedPreferences("noor_waqf_breath", Context.MODE_PRIVATE) }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = 7 Signs Lab, 1 = Breath Trainer, 2 = Meaning Quiz
    var playingRuleId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Breath-Span Trainer state
    var breathRunning by remember { mutableStateOf(false) }
    var breathStartMs by remember { mutableLongStateOf(0L) }
    var breathElapsedMs by remember { mutableLongStateOf(0L) }
    var bestBreathMs by remember { mutableLongStateOf(prefs.getLong("best_breath_ms", 0L)) }

    // Quiz state
    var quizIdx by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var quizScore by remember { mutableIntStateOf(0) }

    fun stopAudio() {
        runCatching {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        playingRuleId = null
    }

    fun playRuleSample(rule: WaqfSignRule) {
        if (playingRuleId == rule.id) {
            stopAudio()
            return
        }
        stopAudio()
        playingRuleId = rule.id
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(rule.audioUrl())
                setOnPreparedListener { it.start() }
                setOnCompletionListener { playingRuleId = null }
                setOnErrorListener { _, _, _ ->
                    playingRuleId = null
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        }.onFailure { playingRuleId = null }
    }

    DisposableEffect(Unit) {
        onDispose { stopAudio() }
    }

    LaunchedEffect(breathRunning) {
        while (isActive && breathRunning) {
            breathElapsedMs = (System.currentTimeMillis() - breathStartMs).coerceAtLeast(0L)
            delay(50L)
        }
    }

    val breathAnim = rememberInfiniteTransition(label = "waqf_breath_orb")
    val orbPulse by breathAnim.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_waqf_lab_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "معمل الوقف والابتداء والنَّفَس", "Waqf, Ibtida & Breath Lab"),
            subtitle = L10n.pick(lang, "علامات الوقف السبع في المصحف + مدرب النَّفَس + اختبار المعاني", "7 Mushaf Pause Signs + Breath Trainer + Meaning Quiz"),
            onBack = {
                stopAudio()
                onBack()
            }
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
                        label = L10n.pick(lang, "علامات الوقف الـ٧", "7 Signs"),
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "مُدرّب النَّفَس", "Breath"),
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "اختبار الوقف", "Quiz"),
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (activeTab == 0) {
                items(QuranWaqfCatalog.signs, key = { it.id }) { rule ->
                    val badgeColor = Color(rule.badgeColorHex)
                    val isPlaying = playingRuleId == rule.id
                    LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.22f))
                                    .border(1.5.dp, badgeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rule.symbol,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, rule.nameAr, rule.nameEn),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = L10n.pick(lang, rule.rulingAr, rule.rulingEn),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = badgeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldPrimary.copy(alpha = 0.10f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = rule.exampleAyahAr,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoldBright,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 28.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L10n.pick(lang, rule.surahRefAr, rule.surahRefEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = L10n.pick(lang, rule.meaningImpactAr, rule.meaningImpactEn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { playRuleSample(rule) },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) GoldPrimary else EmeraldPrimary,
                                    contentColor = if (isPlaying) Color(0xFF071B14) else Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaying) L10n.pick(lang, "إيقاف", "Stop") else L10n.pick(lang, "اسمع التلاوة (الحصري)", "Listen (Husary)"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { onOpenSurah(rule.surahNumber) },
                                modifier = Modifier.heightIn(min = 44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(L10n.pick(lang, "المصحف", "Mushaf"), color = GoldBright)
                            }
                        }
                    }
                }
            } else if (activeTab == 1) {
                item {
                    val elapsedSec = breathElapsedMs / 1000.0
                    val bestSec = bestBreathMs / 1000.0
                    val linesEst = QuranWaqfCatalog.estimateRecitableLines(elapsedSec)

                    LiquidGlassCard(contentPadding = PaddingValues(18.dp)) {
                        Text(
                            text = L10n.pick(lang, "مُدرّب طول النَّفَس القرآني", "Quranic Recitation Breath-Span Trainer"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "خذ نَفَساً عميقاً واضغط «ابدأ التلاوة» ثم اقرأ بصوتٍ هادئ، وعند انتهاء نَفَسك اضغط «أوقف» لقياس مدى نَفَسك بالسطور.",
                                "Inhale deeply, tap Start and recite calmly; tap Stop when your breath ends to measure your Mushaf line span."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(200.dp)) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val r = (size.minDimension * 0.38f) * (if (breathRunning) orbPulse else 1f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(EmeraldPrimary.copy(alpha = 0.45f), GoldBright.copy(alpha = 0.15f), Color.Transparent),
                                        center = Offset(cx, cy),
                                        radius = r * 1.35f
                                    ),
                                    radius = r * 1.35f,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = EmeraldDeep,
                                    radius = r,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = GoldBright,
                                    radius = r,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 3f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Air, contentDescription = null, tint = GoldBright, modifier = Modifier.size(24.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f s", elapsedSec),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "≈ ${String.format(Locale.US, "%.1f", linesEst)} سطر مصحف",
                                        "≈ ${String.format(Locale.US, "%.1f", linesEst)} Mushaf lines"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldBright
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (elapsedSec > 0.5) {
                            Text(
                                text = QuranWaqfCatalog.classifyBreathLevelAr(elapsedSec),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!breathRunning) {
                                        breathStartMs = System.currentTimeMillis()
                                        breathElapsedMs = 0L
                                        breathRunning = true
                                    } else {
                                        breathRunning = false
                                        if (breathElapsedMs > bestBreathMs) {
                                            bestBreathMs = breathElapsedMs
                                            prefs.edit().putLong("best_breath_ms", bestBreathMs).apply()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("waqf_breath_toggle_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (breathRunning) Color(0xFFDC2626) else EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (breathRunning) L10n.pick(lang, "إيقاف القياس", "Stop") else L10n.pick(lang, "ابدأ قياس النَّفَس", "Start Breath Test"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    breathRunning = false
                                    breathElapsedMs = 0L
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = GoldBright)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldPrimary.copy(alpha = 0.14f))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = L10n.pick(lang, "أفضل رقم قياسي مسجّل لك", "Your Personal Best Breath"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f ثانية", bestSec),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldBright
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    val q = QuranWaqfCatalog.quizQuestions[quizIdx]
                    LiquidGlassCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Quiz, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(lang, "سؤال ${quizIdx + 1} من ${QuranWaqfCatalog.quizQuestions.size}", "Question ${quizIdx + 1}/${QuranWaqfCatalog.quizQuestions.size}"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBright
                                )
                            }
                            Text(
                                text = L10n.pick(lang, "النقاط: $quizScore", "Score: $quizScore"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = L10n.pick(lang, q.promptAr, q.promptEn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldPrimary.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = q.ayahAr,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldBright,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        q.optionsAr.forEachIndexed { idx, optAr ->
                            val isChosen = selectedOption == idx
                            val isCorrect = idx == q.correctIndex
                            val borderCol = when {
                                selectedOption == null -> GoldPrimary.copy(alpha = 0.35f)
                                isCorrect -> EmeraldPrimary
                                isChosen -> Color(0xFFEF4444)
                                else -> GoldPrimary.copy(alpha = 0.2f)
                            }
                            val bgCol = when {
                                selectedOption != null && isCorrect -> EmeraldPrimary.copy(alpha = 0.2f)
                                selectedOption != null && isChosen -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                else -> Color.Transparent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgCol)
                                    .border(1.2.dp, borderCol, RoundedCornerShape(12.dp))
                                    .clickable(enabled = selectedOption == null) {
                                        selectedOption = idx
                                        if (idx == q.correctIndex) quizScore++
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = L10n.pick(lang, optAr, q.optionsEn[idx]),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedOption != null && isCorrect) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        if (selectedOption != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = L10n.pick(lang, q.explanationAr, q.explanationEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldBright
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    selectedOption = null
                                    quizIdx = (quizIdx + 1) % QuranWaqfCatalog.quizQuestions.size
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(L10n.pick(lang, "السؤال التالي", "Next Question"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
