package com.example.ui.screens.ecosystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.local.catalog.SurahCatalog
import com.example.data.remote.NoorLiveOnlineService
import com.example.domain.quran.AyahIrabSarfResult
import com.example.domain.quran.QuranMorphologyIrabEngine
import com.example.domain.quran.QuranWordIrabSarf
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranIrabSarfScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    var selectedSurahNum by remember { mutableIntStateOf(1) }
    var selectedAyahNum by remember { mutableIntStateOf(1) }
    val surahInfo = remember(selectedSurahNum) { SurahCatalog.findSurah(selectedSurahNum) }

    var loading by remember { mutableStateOf(false) }
    var analysis by remember { mutableStateOf<AyahIrabSarfResult?>(null) }
    var selectedWordIndex by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSurahNum, selectedAyahNum) {
        loading = true
        loadError = null
        selectedWordIndex = 0
        val wbwResult = NoorLiveOnlineService.fetchQuranWordByWord(selectedSurahNum, selectedAyahNum).getOrNull()
        val tafsirResult = NoorLiveOnlineService.fetchAyahMultiTafsir(selectedSurahNum, selectedAyahNum).getOrNull()

        val fullText = wbwResult?.fullUthmaniText?.ifBlank { tafsirResult?.uthmaniText.orEmpty() }
            ?: tafsirResult?.uthmaniText.orEmpty()
        val computed = QuranMorphologyIrabEngine.analyzeAyah(
            surahNumber = selectedSurahNum,
            ayahNumber = selectedAyahNum,
            fullAyahText = fullText,
            wordTokens = wbwResult?.words.orEmpty(),
            tafsirMuyassarAr = tafsirResult?.tafsirMuyassarAr.orEmpty()
        )
        if (computed.words.isEmpty()) {
            loadError = L10n.pick(
                lang,
                "تعذّر تحميل هذه الآية — تأكد من الاتصال بالإنترنت أو اختر الفاتحة والإخلاص وآية الكرسي للعمل بدون إنترنت",
                "Unable to load verse — check internet, or browse Al-Fatiha / Al-Ikhlas / Ayat al-Kursi offline",
                "Oyat yuklanmadi — internetni tekshiring yoki Fotiha / Ixlos suralarini oflayn oching"
            )
            analysis = null
        } else {
            analysis = computed
        }
        loading = false
    }

    val currentWord: QuranWordIrabSarf? = analysis?.words?.getOrNull(selectedWordIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_irab_sarf_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "إعراب وصرف كلمات القرآن", "Quran I'rab & Sarf Studio", "Qur'on so'zlari E'robi va Sarfi"),
            subtitle = L10n.pick(
                lang,
                "اضغط على أي كلمة في أي سورة (١–١١٤) لعرض إعرابها وجذرها ووزنها الصرفي",
                "Tap any Quranic word across all 114 Surahs for I'rab grammar & Sarf morphology",
                "Istalgan suradagi har bir so'zning e'robi, ildizi va sarf vaznini ko'ring"
            ),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Surah & Ayah Selector Card
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = L10n.pick(lang, "اختر السورة والآية (١١٤ سورة)", "Select Surah & Ayah (All 114)", "Sura va oyatni tanlang (114 sura)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SurahCatalog.allSurahs, key = { it.number }) { s ->
                                LiquidGlassChip(
                                    label = "${s.number}. ${s.nameAr}",
                                    selected = selectedSurahNum == s.number,
                                    onClick = {
                                        selectedSurahNum = s.number
                                        selectedAyahNum = 1
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${surahInfo.nameAr} (${surahInfo.nameEn})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = L10n.pick(
                                        lang,
                                        "الآية $selectedAyahNum من ${surahInfo.verseCount}",
                                        "Ayah $selectedAyahNum of ${surahInfo.verseCount}",
                                        "${surahInfo.verseCount} oyatdan $selectedAyahNum-oyat"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (selectedAyahNum > 1) selectedAyahNum -= 1 },
                                    enabled = selectedAyahNum > 1,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(noorColors.glassSurfaceStrong)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Previous Ayah")
                                }
                                Text(
                                    text = "$selectedAyahNum",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = noorColors.goldAccent
                                )
                                IconButton(
                                    onClick = { if (selectedAyahNum < surahInfo.verseCount) selectedAyahNum += 1 },
                                    enabled = selectedAyahNum < surahInfo.verseCount,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(noorColors.glassSurfaceStrong)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Next Ayah")
                                }
                            }
                        }
                    }
                }
            }

            if (loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                }
            } else if (loadError != null) {
                item {
                    LiquidGlassCard {
                        Text(text = loadError.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (analysis != null) {
                val res = analysis!!

                // 2. Interactive Tappable Ayah Words Box
                item {
                    LiquidGlassCard(strong = true, goldBorder = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = L10n.pick(lang, "اضغط على أي كلمة لإعرابها وصرفها", "Tap any word for I'rab & Sarf", "E'rob va sarf uchun so'zni bosing"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = noorColors.goldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                if (res.isScholarlyVerified) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = L10n.pick(lang, "موثّق", "Verified", "Tasdiqlangan"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                res.words.forEachIndexed { idx, w ->
                                    val isSelected = idx == selectedWordIndex
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) GoldPrimary.copy(alpha = 0.28f)
                                                else noorColors.glassSurfaceStrong
                                            )
                                            .border(
                                                width = if (isSelected) 1.8.dp else 1.dp,
                                                color = if (isSelected) GoldBright else GoldPrimary.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedWordIndex = idx }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("irab_word_$idx")
                                    ) {
                                        Text(
                                            text = w.uthmaniWord,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 24.sp,
                                            color = if (isSelected) GoldBright else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Detailed Selected Word I'rab & Sarf Breakdown Card
                if (currentWord != null) {
                    item {
                        LiquidGlassCard(goldBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = currentWord.uthmaniWord,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 32.sp,
                                        color = noorColors.goldAccent
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(EmeraldPrimary.copy(alpha = 0.18f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${currentWord.partOfSpeechAr} · ${currentWord.irabCaseAr}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        }
                                        if (currentWord.transliteration.isNotBlank() || currentWord.meaningEn.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = listOf(currentWord.transliteration, currentWord.meaningEn)
                                                    .filter { it.isNotBlank() }
                                                    .joinToString(" — "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // الإعراب (Grammar / Syntax)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.10f))
                                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = L10n.pick(lang, "الإعراب النحوي", "Grammatical I'rab", "Nahviy E'rob"),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = currentWord.irabSummaryAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 22.sp
                                    )
                                }

                                // الصرف (Morphology)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GoldPrimary.copy(alpha = 0.10f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = L10n.pick(lang, "التحليل الصرفي (الجذر والوزن)", "Morphological Sarf (Root & Pattern)", "Sarf tahlili (Ildiz va vazn)"),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = noorColors.goldAccent
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SarfPill(
                                            label = L10n.pick(lang, "الجذر", "Root", "Ildiz"),
                                            value = currentWord.sarfRootAr,
                                            modifier = Modifier.weight(1f)
                                        )
                                        SarfPill(
                                            label = L10n.pick(lang, "الوزن الصرفي", "Pattern", "Vazn"),
                                            value = currentWord.sarfPatternAr,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = currentWord.sarfDetailsAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Complete Ayah Word-by-Word I'rab Table
                item {
                    NoorSectionHeader(
                        title = L10n.pick(lang, "الإعراب الكامل لجميع كلمات الآية", "Full Ayah Word-by-Word Table", "Oyatdagi barcha so'zlar e'robi")
                    )
                }

                items(res.words, key = { "${res.surahNumber}_${res.ayahNumber}_${it.position}" }) { w ->
                    LiquidGlassCard(
                        onClick = { selectedWordIndex = w.position - 1 },
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = w.uthmaniWord,
                                fontFamily = AmiriFontFamily,
                                fontSize = 23.sp,
                                color = noorColors.goldAccent,
                                modifier = Modifier.width(96.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${w.partOfSpeechAr} · ${w.irabCaseAr} · ${L10n.pick(lang, "الجذر:", "Root:", "Ildiz:")} ${w.sarfRootAr}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = w.irabSummaryAr.lineSequence().firstOrNull().orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 5. Source, Tafsir & Action Row
                item {
                    LiquidGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (res.tafsirMuyassarAr.isNotBlank()) {
                                Text(
                                    text = L10n.pick(lang, "التفسير الميسر للآية", "Tafsir Al-Muyassar", "Tafsiri Muyassar"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = noorColors.goldAccent
                                )
                                Text(
                                    text = res.tafsirMuyassarAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp
                                )
                            }
                            Text(
                                text = res.sourceLabelAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenSurah(selectedSurahNum) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "المصحف", "Mushaf", "Mushaf"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val shareBody = buildString {
                                            append("﴿ ${res.fullAyahText} ﴾\n")
                                            append("${surahInfo.nameAr} (${res.ayahNumber})\n\n")
                                            res.words.forEach { w ->
                                                append("• ${w.uthmaniWord}: ${w.irabSummaryAr.lineSequence().first()} [الجذر: ${w.sarfRootAr}]\n")
                                            }
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareBody)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "NOOR"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "مشاركة الإعراب", "Share I'rab", "Ulashish"))
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
private fun SarfPill(label: String, value: String, modifier: Modifier = Modifier) {
    val noorColors = LocalNoorColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(noorColors.glassSurfaceStrong)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = noorColors.goldAccent)
    }
}
