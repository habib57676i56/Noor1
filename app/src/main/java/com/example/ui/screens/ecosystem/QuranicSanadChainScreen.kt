package com.example.ui.screens.ecosystem

import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

data class QiraahImamInfo(
    val id: String,
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val cityAr: String,
    val cityEn: String,
    val deathHijriAr: String,
    val rawiOneAr: String,
    val rawiTwoAr: String,
    val companionChainAr: String,
    val hallmarkAr: String,
    val hallmarkEn: String,
    val accentColor: Color
)

data class LiveRiwayahAudioSample(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val reciterAr: String,
    val ayahTextAr: String,
    val differenceNoteAr: String,
    val surahNumber: Int,
    val audioUrl: String
)

object QuranicSanadCatalog {
    val imams: List<QiraahImamInfo> = listOf(
        QiraahImamInfo(
            "nafi", 1, "الإمام نافع المدني", "Imam Nafi' al-Madani",
            "المدينة المنورة", "Madinah", "ت ١٦٩ هـ",
            "قالون (عيسى بن مينا)", "ورش (عثمان بن سعيد)",
            "عن ٧٠ من التابعين ← أبي هريرة وابن عباس ← أُبيّ بن كعب ← النبي ﷺ",
            "توسّط وتقليل ذوات الياء عند ورش، وصلة ميم الجمع عند قالون — قراءة أهل المدينة والمغرب العربي.",
            "Madinan recitation: Warsh's Taqleel & Madd al-Badal, Qalun's Silat Mim al-Jam'.",
            Color(0xFF34D399)
        ),
        QiraahImamInfo(
            "ibn_kathir", 2, "الإمام ابن كثير المكي", "Imam Ibn Kathir al-Makki",
            "مكة المكرمة", "Makkah", "ت ١٢٠ هـ",
            "البَزّي (أحمد بن محمد)", "قُنبل (محمد بن عبد الرحمن)",
            "عن مجاهد ودرباس ← عبد الله بن عباس ← أُبيّ وزيد بن ثابت ← النبي ﷺ",
            "صلة ميم الجمع بواو لفظية، وصلة هاء الكناية دائماً — قراءة أهل مكة المكرمة.",
            "Makkan recitation: consistent Silat Mim al-Jam' and Ha al-Kinayah.",
            Color(0xFFF59E0B)
        ),
        QiraahImamInfo(
            "abu_amr", 3, "الإمام أبو عمرو البصري", "Imam Abu 'Amr al-Basri",
            "البصرة", "Basra", "ت ١٥٤ هـ",
            "الدوري (حفص بن عمر)", "السوسي (صالح بن زياد)",
            "عن مجاهد وسعيد بن جبير ← ابن عباس وأُبيّ وعمر بن الخطاب ← النبي ﷺ",
            "الإدغام الكبير عند السوسي وإمالة الألفات المقصورة — منتشرة في السودان وشرق إفريقيا.",
            "Basran recitation: Al-Susi's major assimilation (Idgham Kabir) and Imalah.",
            Color(0xFF38BDF8)
        ),
        QiraahImamInfo(
            "ibn_amir", 4, "الإمام ابن عامر الشامي", "Imam Ibn 'Amir al-Shami",
            "دمشق (الشام)", "Damascus", "ت ١١٨ هـ",
            "هشام (بن عمار)", "ابن ذكوان (عبد الله)",
            "عن المغيرة بن أبي شهاب وأبي الدرداء ← عثمان بن عفان ← النبي ﷺ",
            "أعلى القراء العشرة سنداً؛ تابعي جليل أخذ عن الصحابة مباشرة وكان إمام الجامع الأموي.",
            "Damascene recitation: highest unbroken chain among the 10 Imams, taught at Umayyad Mosque.",
            Color(0xFFA78BFA)
        ),
        QiraahImamInfo(
            "asim", 5, "الإمام عاصم الكوفي", "Imam 'Asim al-Kufi",
            "الكوفة", "Kufa", "ت ١٢٧ هـ",
            "شُعبة (بن عياش)", "حفص (بن سليمان)",
            "عن أبي عبد الرحمن السُّلمي وزِرّ بن حبيش ← علي بن أبي طالب وعثمان وابن مسعود ← النبي ﷺ",
            "رواية حفص عن عاصم هي الأكثر انتشاراً في العالم الإسلامي اليوم لدقّتها ويسر أصولها.",
            "Kufan recitation: Hafs 'an 'Asim is the most widely recited transmission worldwide.",
            GoldBright
        ),
        QiraahImamInfo(
            "hamzah", 6, "الإمام حمزة الكوفي", "Imam Hamzah al-Kufi",
            "الكوفة", "Kufa", "ت ١٥٦ هـ",
            "خلف (بن هشام)", "خلّاد (بن خالد)",
            "عن الأعمش وحمران ومحمد بن أبي ليلى ← ابن مسعود وعلي بن أبي طالب ← النبي ﷺ",
            "إشباع المد المتصل والمنفصل ٦ حركات، والسكت على الساكن قبل الهمز، والوقف على الهمز بالتسهيل.",
            "Kufan recitation: 6-count Madd, Sakta pauses before Hamzah, and specialized Hamzah stops.",
            Color(0xFFF472B6)
        ),
        QiraahImamInfo(
            "kisai", 7, "الإمام الكسائي الكوفي", "Imam al-Kisa'i al-Kufi",
            "الكوفة وبغداد", "Kufa & Baghdad", "ت ١٨٩ هـ",
            "أبو الحارث (الليث)", "الدوري (حفص بن عمر)",
            "عن حمزة ومحمد بن أبي ليلى ← علي بن أبي طالب وابن مسعود ← النبي ﷺ",
            "إمام النحاة والقراء؛ يتميز بإمالة هاء التأنيث عند الوقف وإمالة ذوات الياء.",
            "Master grammarian & Qari: distinctive Imalah on feminine endings at pauses.",
            Color(0xFF2DD4BF)
        ),
        QiraahImamInfo(
            "abu_jafar", 8, "الإمام أبو جعفر المدني", "Imam Abu Ja'far al-Madani",
            "المدينة المنورة", "Madinah", "ت ١٣٠ هـ",
            "ابن وردان (عيسى)", "ابن جمّاز (سليمان)",
            "عن عبد الله بن عياش وأبي هريرة وابن عباس ← أُبيّ بن كعب ← النبي ﷺ",
            "شيخ الإمام نافع؛ يتميز بإبدال الهمز الساكن وصلة ميم الجمع وإخفاء النون عند الخاء والغين.",
            "Teacher of Imam Nafi': replaces quiescent Hamzah and connects Mim al-Jam'.",
            Color(0xFFEAB308)
        ),
        QiraahImamInfo(
            "yaqub", 9, "الإمام يعقوب الحضرمي", "Imam Ya'qub al-Hadrami",
            "البصرة", "Basra", "ت ٢٠٥ هـ",
            "رُويس (محمد بن المتوكل)", "رَوح (بن عبد المؤمن)",
            "عن سلام الطويل وأبي عمرو ← أبي موسى الأشعري وابن عباس ← النبي ﷺ",
            "إمام جامع البصرة؛ يتميز بإثبات هاء السكت في بعض الكلمات وإدغام المثلين.",
            "Basran Imam: preserves Ha al-Sakt in select verses with smooth rhythmic flow.",
            Color(0xFF60A5FA)
        ),
        QiraahImamInfo(
            "khalaf", 10, "الإمام خلف العاشر البزار", "Imam Khalaf al-'Ashir",
            "بغداد", "Baghdad", "ت ٢٢٩ هـ",
            "إسحاق (الوراق)", "إدريس (الحداد)",
            "عن سُليم بن عيسى عن حمزة، وعن الكسائي ← علي وابن مسعود ← النبي ﷺ",
            "جمع بين قراءة الكوفيين باختيار متزن يجمع بين الإتقان والسلاسة — مُتمّم العشر المتواترة.",
            "Completer of the 10 Mutawatir Qira'at: balanced Kufan synthesis from Baghdad.",
            Color(0xFFFB7185)
        )
    )

    val liveAudioSamples: List<LiveRiwayahAudioSample> = listOf(
        LiveRiwayahAudioSample(
            id = "hafs_fatiha",
            titleAr = "رواية حفص عن عاصم",
            titleEn = "Hafs 'an 'Asim",
            reciterAr = "الشيخ محمود خليل الحصري",
            ayahTextAr = "مَالِكِ يَوْمِ الدِّينِ",
            differenceNoteAr = "بإثبات الألف «مَالِكِ» — قراءة عاصم والكسائي ويعقوب وخلف.",
            surahNumber = 1,
            audioUrl = "https://everyayah.com/data/Husary_128kbps/001004.mp3"
        ),
        LiveRiwayahAudioSample(
            id = "warsh_fatiha",
            titleAr = "رواية ورش عن نافع",
            titleEn = "Warsh 'an Nafi'",
            reciterAr = "الشيخ إبراهيم الدوسري (برواية ورش)",
            ayahTextAr = "مَلِكِ يَوْمِ الدِّينِ",
            differenceNoteAr = "بحذف الألف «مَلِكِ» — قراءة نافع وابن كثير وأبي عمرو وابن عامر وحمزة وأبي جعفر.",
            surahNumber = 1,
            audioUrl = "https://everyayah.com/data/warsh/warsh_ibrahim_aldosary_128kbps/001004.mp3"
        ),
        LiveRiwayahAudioSample(
            id = "warsh_duha",
            titleAr = "رواية ورش — التقليل في سورة الضحى",
            titleEn = "Warsh — Taqleel in Al-Duha",
            reciterAr = "الشيخ ياسين الجزائري (برواية ورش)",
            ayahTextAr = "وَالضُّحَىٰ ۝ وَاللَّيْلِ إِذَا سَجَىٰ",
            differenceNoteAr = "يُقلِّل ورش ذوات الياء مثل «الضُّحى» و«سَجى» بين الفتح والإمالة بصوت بديع.",
            surahNumber = 93,
            audioUrl = "https://everyayah.com/data/warsh/warsh_yassin_al_jazaery_64kbps/093001.mp3"
        ),
        LiveRiwayahAudioSample(
            id = "hafs_duha",
            titleAr = "رواية حفص — الفتح في سورة الضحى",
            titleEn = "Hafs — Fath in Al-Duha",
            reciterAr = "الشيخ محمد صديق المنشاوي (حفص)",
            ayahTextAr = "وَالضُّحَىٰ ۝ وَاللَّيْلِ إِذَا سَجَىٰ",
            differenceNoteAr = "يقرأ حفص ذوات الياء بالفتح الخالص دون تقليل أو إمالة (إلا «مجرىها» في هود).",
            surahNumber = 93,
            audioUrl = "https://everyayah.com/data/Minshawy_Murattal_128kbps/093001.mp3"
        )
    )
}

@Composable
fun QuranicSanadChainScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Golden Sanad Tree, 1 = Live Qira'at Audio Comparison
    var selectedImam by remember { mutableStateOf(QuranicSanadCatalog.imams[4]) } // Default: Asim

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSampleId by remember { mutableStateOf<String?>(null) }
    var loadingSampleId by remember { mutableStateOf<String?>(null) }
    var audioError by remember { mutableStateOf<String?>(null) }

    fun stopAudio() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingSampleId = null
        loadingSampleId = null
    }

    fun togglePlay(sample: LiveRiwayahAudioSample) {
        if (playingSampleId == sample.id || loadingSampleId == sample.id) {
            stopAudio()
            return
        }
        stopAudio()
        audioError = null
        loadingSampleId = sample.id
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { p ->
                loadingSampleId = null
                playingSampleId = sample.id
                p.start()
            }
            setOnCompletionListener {
                playingSampleId = null
            }
            setOnErrorListener { _, _, _ ->
                loadingSampleId = null
                playingSampleId = null
                audioError = L10n.pick(lang, "تعذر تشغيل التلاوة — تحقق من الاتصال بالإنترنت", "Audio stream error — check internet")
                true
            }
        }
        mediaPlayer = mp
        try {
            mp.setDataSource(sample.audioUrl)
            mp.prepareAsync()
        } catch (_: Exception) {
            stopAudio()
            audioError = L10n.pick(lang, "تعذر تحميل ملف القراءة", "Failed to load Qira'ah audio")
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopAudio() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "شجرة الإسناد الذهبية والقرّاء العشرة", "Golden Sanad Tree & 10 Qira'at"),
            subtitle = L10n.pick(
                lang,
                "السند المتصل إلى النبي ﷺ · الأئمة العشرة ورواتهم العشرون + مقارنة صوتية حية",
                "Unbroken chain to the Prophet ﷺ · 10 Imams, 20 Rawis & Live Audio"
            ),
            onBack = {
                stopAudio()
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
                label = L10n.pick(lang, "شجرة الإسناد (١٠ أئمة)", "Golden Sanad Tree"),
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f).testTag("sanad_tab_tree")
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "مقارنة الروايات صوتياً", "Live Qira'at Audio"),
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f).testTag("sanad_tab_audio")
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Visual Glowing Golden Lineage Chain Canvas
                item {
                    LiquidGlassCard(strong = true, goldBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L10n.pick(lang, "سلسلة الإسناد المتواتر إلى ربّ العزّة", "Unbroken Mutawatir Chain of Revelation"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldBright
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            SanadLineageNodePill("الله جلّ جلاله ← جبريل عليه السلام ← النبي محمد ﷺ", GoldBright)
                            GoldenConnectorLine()
                            SanadLineageNodePill(selectedImam.companionChainAr, Color(0xFF38BDF8))
                            GoldenConnectorLine()
                            SanadLineageNodePill("${selectedImam.nameAr} (${selectedImam.cityAr} • ${selectedImam.deathHijriAr})", selectedImam.accentColor)
                            GoldenConnectorLine(split = true)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(selectedImam.accentColor.copy(alpha = 0.16f))
                                        .border(1.dp, selectedImam.accentColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = L10n.pick(lang, "الراوي الأول", "1st Rawi"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldBright
                                        )
                                        Text(
                                            text = selectedImam.rawiOneAr,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(selectedImam.accentColor.copy(alpha = 0.16f))
                                        .border(1.dp, selectedImam.accentColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = L10n.pick(lang, "الراوي الثاني", "2nd Rawi"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldBright
                                        )
                                        Text(
                                            text = selectedImam.rawiTwoAr,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = L10n.pick(lang, selectedImam.hallmarkAr, selectedImam.hallmarkEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 10 Imams Selector Grid/Cards
                item {
                    Text(
                        text = L10n.pick(lang, "اختر إماماً من الأئمة العشرة المتواترة:", "Select Any of the 10 Mutawatir Imams:"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }

                items(QuranicSanadCatalog.imams, key = { it.id }) { imam ->
                    val isSelected = imam.id == selectedImam.id
                    LiquidGlassCard(
                        goldBorder = isSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedImam = imam }
                            .testTag("sanad_imam_${imam.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(imam.accentColor.copy(alpha = 0.22f))
                                    .border(1.2.dp, imam.accentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${imam.number}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = imam.accentColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L10n.pick(lang, imam.nameAr, imam.nameEn),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${imam.cityAr} • ${imam.deathHijriAr} • ${imam.rawiOneAr.substringBefore(" (")} و${imam.rawiTwoAr.substringBefore(" (")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = if (isSelected) GoldBright else imam.accentColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        } else {
            // Tab 1: Live Real Audio Comparison across Hafs & Warsh
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (audioError != null) {
                    item {
                        Text(
                            text = audioError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF87171),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                items(QuranicSanadCatalog.liveAudioSamples, key = { it.id }) { sample ->
                    val isPlaying = playingSampleId == sample.id
                    val isLoading = loadingSampleId == sample.id
                    LiquidGlassCard(goldBorder = isPlaying, modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = L10n.pick(lang, sample.titleAr, sample.titleEn),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldBright
                                    )
                                    Text(
                                        text = sample.reciterAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = if (isPlaying) Color(0xFF34D399) else GoldPrimary)
                            }

                            Text(
                                text = sample.ayahTextAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = sample.differenceNoteAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { togglePlay(sample) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("sanad_play_${sample.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlaying) Color(0xFFDC2626) else EmeraldPrimary
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isLoading -> L10n.pick(lang, "جارٍ التحميل...", "Loading...")
                                            isPlaying -> L10n.pick(lang, "إيقاف", "Stop")
                                            else -> L10n.pick(lang, "استمع للرواية حياً", "Play Live Audio")
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EmeraldDeep.copy(alpha = 0.7f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .clickable { onOpenSurah(sample.surahNumber) }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
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
private fun SanadLineageNodePill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoldenConnectorLine(split: Boolean = false) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
    ) {
        val midX = size.width / 2f
        val brush = Brush.verticalGradient(listOf(GoldBright, GoldPrimary))
        if (!split) {
            drawLine(brush, Offset(midX, 0f), Offset(midX, size.height), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        } else {
            drawLine(brush, Offset(midX, 0f), Offset(midX, size.height * 0.45f), strokeWidth = 2.5.dp.toPx())
            drawLine(brush, Offset(size.width * 0.25f, size.height * 0.45f), Offset(size.width * 0.75f, size.height * 0.45f), strokeWidth = 2.5.dp.toPx())
            drawLine(brush, Offset(size.width * 0.25f, size.height * 0.45f), Offset(size.width * 0.25f, size.height), strokeWidth = 2.5.dp.toPx())
            drawLine(brush, Offset(size.width * 0.75f, size.height * 0.45f), Offset(size.width * 0.75f, size.height), strokeWidth = 2.5.dp.toPx())
        }
    }
}
