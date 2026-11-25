package com.example.ui.screens.ecosystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.remote.NoorLiveOnlineService
import com.example.data.remote.QuranSearchHit
import com.example.domain.quran.MutashabihGroup
import com.example.domain.quran.QuranMutashabihatEngine
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
import kotlinx.coroutines.launch

/**
 * Rare Flagship Feature: Quran Mutashabihat (Similar Verses) Master Lab.
 * 1. Scholarly Curated Side-by-Side Diff Catalog with Golden Hifz Mnemonic Rules (الضوابط الذهبية).
 * 2. Live 6,236-Ayah Online Phrase Comparator with Automatic Word-Level Diff Highlighting.
 * 3. Interactive Mutashabihat Mastery Quiz to eliminate Hifz confusion permanently.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranMutashabihatScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Curated, 1 = Live Scanner, 2 = Quiz

    // Live scanner state
    var scanQuery by remember { mutableStateOf("أولئك هم المفلحون") }
    var isScanning by remember { mutableStateOf(false) }
    var scanHits by remember { mutableStateOf<List<QuranSearchHit>>(emptyList()) }
    var scanError by remember { mutableStateOf<String?>(null) }

    // Quiz state
    var quizIndex by remember { mutableIntStateOf(0) }
    var quizSelectedSurah by remember { mutableStateOf<Int?>(null) }
    var quizCorrectCount by remember { mutableIntStateOf(0) }
    var quizAnsweredCount by remember { mutableIntStateOf(0) }

    fun runLiveScan(q: String) {
        val clean = q.trim()
        if (clean.isEmpty()) return
        scope.launch {
            isScanning = true
            scanError = null
            val res = NoorLiveOnlineService.searchFullQuran(clean)
            res.onSuccess { hits ->
                scanHits = hits.take(25)
                if (hits.isEmpty()) {
                    scanError = L10n.pick(lang, "لم يُعثر على مواضع مطابقة لهذه العبارة.", "No matching verses found.", "Mos oyat topilmadi.")
                }
            }.onFailure { err ->
                scanError = err.message ?: L10n.pick(lang, "تعذر الاتصال بالخادم القرآني.", "Unable to reach Quran API.", "Internet aloqasi xatosi.")
            }
            isScanning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_mutashabihat_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "متشابهات القرآن والضوابط الذهبية", "Quran Mutashabihat Lab", "Qur'on Mutashobihotlari"),
            subtitle = L10n.pick(
                lang,
                "مقارنة الفروق اللفظية الدقيقة + الضوابط الذهبية للحفاظ + ماسح ٦٢٣٦ آية",
                "Word-Level Verse Diffs · Classical Hifz Mnemonics · Live 6,236-Ayah Scanner",
                "So'zma-so'z farqlar · Oltin yodlash qoidalari · Jonli 6236 oyat skaneri"
            ),
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassChip(
                label = L10n.pick(lang, "المتشابهات الموثقة", "Curated Diffs", "Asosiy farqlar"),
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f)
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "ماسح حي", "Live Scanner", "Jonli skaner"),
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f)
            )
            LiquidGlassChip(
                label = L10n.pick(lang, "اختبار الحفاظ", "Hifz Quiz", "Hifz testi"),
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                0 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true, contentPadding = PaddingValues(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CompareArrows, contentDescription = null, tint = GoldBright, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "الكلمات الملوّنة بالذهبي هي الفروق الدقيقة بين السورتين — مع الضابط الذهبي لعدم الالتباس أثناء التلاوة والحفظ.",
                                        "Gold-highlighted words show exact subtle differences between similar Surahs alongside classical memorization anchors.",
                                        "Oltin rang bilan ajratilgan so'zlar suralar orasidagi nozik farqlarni ko'rsatadi."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(QuranMutashabihatEngine.curatedGroups, key = { it.id }) { group ->
                        MutashabihGroupCard(group = group, onOpenSurah = onOpenSurah)
                    }
                }

                1 -> {
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "ابحث عن أي جملة قرآنية لمقارنة جميع مواضعها المتشابهة في ٦٢٣٦ آية",
                                        "Scan any Quranic phrase across all 6,236 Ayahs and compare exact diffs",
                                        "6236 oyat ichidan istalgan iboraning barcha o'xshash o'rinlarini solishtiring"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                OutlinedTextField(
                                    value = scanQuery,
                                    onValueChange = { scanQuery = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("mutashabihat_scan_input"),
                                    label = { Text(L10n.pick(lang, "اكتب عبارة قرآنية (بدون تشكيل)", "Arabic phrase (plain)", "Arabcha ibora")) }
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "أولئك هم المفلحون",
                                        "خالدين فيها أبدا",
                                        "إن الله على كل شيء قدير",
                                        "وسارعوا",
                                        "لعلهم يتقون"
                                    ).forEach { preset ->
                                        LiquidGlassChip(
                                            label = preset,
                                            selected = scanQuery == preset,
                                            onClick = {
                                                scanQuery = preset
                                                runLiveScan(preset)
                                            }
                                        )
                                    }
                                }
                                Button(
                                    onClick = { runLiveScan(scanQuery) },
                                    enabled = !isScanning && scanQuery.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("mutashabihat_scan_btn")
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Icon(Icons.Rounded.Search, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(L10n.pick(lang, "فحص ومقارنة المواضع الحية", "Scan & Compare Live", "Jonli solishtirish"))
                                }
                            }
                        }
                    }

                    scanError?.let { err ->
                        item {
                            LiquidGlassCard {
                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (scanHits.isNotEmpty()) {
                        item {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "تم العثور على ${scanHits.size} موضع متشابه — الكلمات المميزة لكل آية ملوّنة بالذهبي:",
                                    "Found ${scanHits.size} occurrences — unique words highlighted in gold:",
                                    "${scanHits.size} ta o'xshash oyat topildi:"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                        }
                        val allTexts = scanHits.map { it.matchedText }
                        items(scanHits, key = { "${it.surahNumber}:${it.ayahNumberInSurah}" }) { hit ->
                            val peerTexts = allTexts.filter { it != hit.matchedText }
                            val diffWords = remember(hit.matchedText, peerTexts.size) {
                                QuranMutashabihatEngine.computeWordDiffs(hit.matchedText, peerTexts)
                            }
                            LiquidGlassCard(onClick = { onOpenSurah(hit.surahNumber) }) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${hit.surahNameAr} · آية ${hit.ayahNumberInSurah}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = noorColors.goldAccent
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = L10n.pick(lang, "المصحف", "Mushaf", "Mushaf"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        diffWords.forEach { dw ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (dw.isDistinctive) GoldPrimary.copy(alpha = 0.24f) else Color.Transparent)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = dw.word,
                                                    fontFamily = AmiriFontFamily,
                                                    fontSize = 19.sp,
                                                    fontWeight = if (dw.isDistinctive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (dw.isDistinctive) GoldBright else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val groups = QuranMutashabihatEngine.curatedGroups
                    val currentGroup = groups[quizIndex % groups.size]
                    val targetAyah = currentGroup.ayahs.first()
                    item {
                        LiquidGlassCard(strong = true, goldBorder = true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                            text = L10n.pick(lang, "تحدي إتقان المتشابهات", "Mutashabihat Mastery Challenge", "Mutashobihot sinovi"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = noorColors.goldAccent
                                        )
                                    }
                                    Text(
                                        text = "$quizCorrectCount / $quizAnsweredCount",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldPrimary
                                    )
                                }

                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "في أي سورة وردت هذه الآية بهذا اللفظ الدقيق؟",
                                        "Which Surah contains this EXACT wording?",
                                        "Ushbu aniq lafz qaysi surada kelgan?"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(EmeraldDeep.copy(alpha = 0.45f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = targetAyah.ayahTextAr,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 22.sp,
                                        lineHeight = 38.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color.White,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    currentGroup.ayahs.distinctBy { it.surahNumber to it.ayahNumber }.forEach { option ->
                                        val isChosen = quizSelectedSurah == option.surahNumber
                                        val isRight = option.surahNumber == targetAyah.surahNumber
                                        val bg = when {
                                            quizSelectedSurah == null -> noorColors.glassSurfaceStrong
                                            isRight -> Color(0xFF1F7A4D).copy(alpha = 0.35f)
                                            isChosen -> Color(0xFFB71C1C).copy(alpha = 0.32f)
                                            else -> noorColors.glassSurface
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(bg)
                                                .border(
                                                    1.dp,
                                                    if (quizSelectedSurah != null && isRight) Color(0xFF4ADE80) else noorColors.glassBorderBottom,
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable(enabled = quizSelectedSurah == null) {
                                                    quizSelectedSurah = option.surahNumber
                                                    quizAnsweredCount += 1
                                                    if (option.surahNumber == targetAyah.surahNumber) quizCorrectCount += 1
                                                }
                                                .padding(horizontal = 14.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = L10n.pick(
                                                    lang,
                                                    "سورة ${option.surahNameAr} (آية ${option.ayahNumber})",
                                                    "Surah ${option.surahNameEn} (${option.surahNumber}:${option.ayahNumber})",
                                                    "${option.surahNameEn} surasi (${option.ayahNumber}-oyat)"
                                                ),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (quizSelectedSurah != null && isRight) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80))
                                            }
                                        }
                                    }
                                }

                                if (quizSelectedSurah != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(GoldPrimary.copy(alpha = 0.14f))
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = L10n.pick(lang, currentGroup.mnemonicRuleAr, currentGroup.mnemonicRuleEn, currentGroup.mnemonicRuleUz),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            quizSelectedSurah = null
                                            quizIndex = (quizIndex + 1) % groups.size
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(L10n.pick(lang, "السؤال التالي ←", "Next Question →", "Keyingi savol →"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MutashabihGroupCard(
    group: MutashabihGroup,
    onOpenSurah: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    LiquidGlassCard(goldBorder = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L10n.pick(lang, group.titleAr, group.titleEn, group.titleUz),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Text(
                        text = "${group.categoryAr} · ${group.sourceBookAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
            }

            group.ayahs.forEach { item ->
                val highlightSet = item.uniqueWordsHighlight.toSet()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(noorColors.glassSurfaceStrong)
                        .border(1.dp, noorColors.glassBorderBottom, RoundedCornerShape(14.dp))
                        .clickable { onOpenSurah(item.surahNumber) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "سورة ${item.surahNameAr} — آية ${item.ayahNumber}",
                                "Surah ${item.surahNameEn} (${item.surahNumber}:${item.ayahNumber})",
                                "${item.surahNameEn} (${item.surahNumber}:${item.ayahNumber})"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = L10n.pick(lang, "فتح بالمصحف ←", "Open →", "Ochish →"),
                            style = MaterialTheme.typography.labelSmall,
                            color = noorColors.goldAccent
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.ayahTextAr.split(" ").forEach { w ->
                            val isUnique = w in highlightSet
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isUnique) GoldPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = w,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 19.sp,
                                    fontWeight = if (isUnique) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isUnique) GoldBright else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Golden Mnemonic Rule Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.14f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = L10n.pick(lang, group.mnemonicRuleAr, group.mnemonicRuleEn, group.mnemonicRuleUz),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
