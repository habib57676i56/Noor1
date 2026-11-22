package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.model.QuranDisplayMode
import com.example.domain.model.QuranReadingTheme
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
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
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.IvorySurface
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel
import com.example.viewmodel.SurahReaderState

@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    viewModel: NoorViewModel,
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val readerState by viewModel.surahReaderState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.quranBookmarks.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

    val surahMeta = remember(surahNumber) { SurahCatalog.findSurah(surahNumber) }

    var showTafsir by remember { mutableStateOf(true) }
    var showEnglish by remember { mutableStateOf(true) }
    var repeatPerAyah by remember { mutableIntStateOf(1) }
    val revealedInMemorize = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(surahNumber) {
        viewModel.loadSurah(surahNumber)
    }

    val (surfaceBg, primaryInk, secondaryInk) = when (prefs.quranReadingTheme) {
        QuranReadingTheme.SEPIA -> Triple(IvorySurface, IvoryInk, Color(0xFF5D5242))
        QuranReadingTheme.NIGHT -> Triple(Color(0xFF040D0A), Color(0xFFEDF6F1), Color(0xFF93B3A5))
        QuranReadingTheme.LIGHT -> Triple(Color(0xFFFAFDFB), Color(0xFF10241D), Color(0xFF4B665B))
        QuranReadingTheme.AUTO -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onBackground,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceBg)
            .testTag("quran_reader_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "سورة ${surahMeta.nameAr}", surahMeta.nameEn),
            subtitle = L10n.pick(
                lang,
                "${if (surahMeta.isMeccan) "مكية" else "مدنية"} · ${surahMeta.verseCount} آية · الجزء ${surahMeta.startJuz} · ص ${surahMeta.startPage}",
                "${surahMeta.meaningEn} · ${surahMeta.verseCount} Ayahs · Juz ${surahMeta.startJuz} · p.${surahMeta.startPage}"
            ),
            onBack = onBack,
            trailing = {
                Row {
                    IconButton(onClick = { viewModel.setQuranFontSize(prefs.quranFontSizeSp - 2) }) {
                        Icon(Icons.Rounded.TextDecrease, contentDescription = "Smaller text", tint = noorColors.goldAccent)
                    }
                    IconButton(onClick = { viewModel.setQuranFontSize(prefs.quranFontSizeSp + 2) }) {
                        Icon(Icons.Rounded.TextIncrease, contentDescription = "Larger text", tint = noorColors.goldAccent)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reading Mode + Theme + Reciter Control Deck
            item {
                LiquidGlassCard(contentPadding = PaddingValues(14.dp)) {
                    // Display Modes: Study, Mushaf, Memorize
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            QuranDisplayMode.STUDY to L10n.pick(lang, "تفسير ودراسة", "Study"),
                            QuranDisplayMode.MUSHAF to L10n.pick(lang, "المصحف", "Mushaf"),
                            QuranDisplayMode.MEMORIZE to L10n.pick(lang, "حفظ ومراجعة", "Memorize")
                        )
                        modes.forEach { (mode, label) ->
                            LiquidGlassChip(
                                label = label,
                                selected = prefs.quranDisplayMode == mode,
                                onClick = { viewModel.setQuranDisplayMode(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reading Theme Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QuranReadingTheme.entries) { theme ->
                            val label = when (theme) {
                                QuranReadingTheme.AUTO -> L10n.pick(lang, "تلقائي", "Auto")
                                QuranReadingTheme.SEPIA -> L10n.pick(lang, "ورقي مريح", "Sepia")
                                QuranReadingTheme.NIGHT -> L10n.pick(lang, "ليلي", "Night")
                                QuranReadingTheme.LIGHT -> L10n.pick(lang, "نهاري", "Light")
                            }
                            LiquidGlassChip(
                                label = label,
                                selected = prefs.quranReadingTheme == theme,
                                onClick = { viewModel.setQuranReadingTheme(theme) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reciter Picker (Yasser Al-Dosari featured first)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QuranReciter.entries) { rec ->
                            LiquidGlassChip(
                                label = L10n.pick(lang, rec.nameAr, rec.nameEn),
                                selected = prefs.quranReciter == rec,
                                onClick = { viewModel.setQuranReciter(rec) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Play Full Surah + Repeat / Tafsir toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.playFullSurahWithReciter(surahNumber, prefs.quranReciter) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(L10n.pick(lang, "تشغيل السورة", "Play Surah"))
                        }

                        if (prefs.quranDisplayMode == QuranDisplayMode.STUDY) {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "التفسير", "Tafsir"),
                                selected = showTafsir,
                                onClick = { showTafsir = !showTafsir }
                            )
                            LiquidGlassChip(
                                label = "EN",
                                selected = showEnglish,
                                onClick = { showEnglish = !showEnglish }
                            )
                        } else if (prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE) {
                            LiquidGlassChip(
                                label = L10n.pick(lang, "تكرار الآية ${repeatPerAyah}x", "Repeat ${repeatPerAyah}x"),
                                selected = repeatPerAyah > 1,
                                onClick = {
                                    repeatPerAyah = when (repeatPerAyah) {
                                        1 -> 3
                                        3 -> 5
                                        else -> 1
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bismillah Header (except At-Tawbah 9 and Al-Fatiha 1 which already contains it as Ayah 1)
            if (surahNumber != 1 && surahNumber != 9) {
                item {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontFamily = AmiriFontFamily,
                        fontSize = (prefs.quranFontSizeSp + 2).sp,
                        color = noorColors.goldAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }

            when (val state = readerState) {
                is SurahReaderState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = noorColors.goldAccent)
                        }
                    }
                }

                is SurahReaderState.Error -> {
                    item {
                        LiquidGlassCard(goldBorder = true, contentPadding = PaddingValues(22.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = L10n.pick(lang, state.messageAr, state.messageEn),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { viewModel.loadSurah(surahNumber) }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(L10n.pick(lang, "إعادة المحاولة", "Retry"))
                                }
                            }
                        }
                    }
                }

                is SurahReaderState.Ready -> {
                    if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF) {
                        // Continuous Mushaf Page Flow
                        val groupedByPage = state.verses.groupBy { it.pageNumber }
                        items(groupedByPage.entries.toList(), key = { "page_${it.key}" }) { (pageNum, pageVerses) ->
                            val combinedText = remember(pageVerses) {
                                pageVerses.joinToString(separator = "  ") { v ->
                                    "${v.textUthmani} ﴿${v.numberInSurah}﴾"
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        if (prefs.quranReadingTheme == QuranReadingTheme.SEPIA) IvorySurface
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    )
                                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = combinedText,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = prefs.quranFontSizeSp.sp,
                                        lineHeight = (prefs.quranFontSizeSp * 1.95f).sp,
                                        color = primaryInk,
                                        textAlign = TextAlign.Justify,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = GoldPrimary.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = L10n.pick(lang, "صفحة $pageNum · الجزء ${pageVerses.first().juzNumber}", "Page $pageNum · Juz ${pageVerses.first().juzNumber}"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = noorColors.goldAccent,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.verses, key = { it.numberInSurah }) { verse ->
                            val isBookmarked = bookmarks.any { it.surahNumber == verse.surahNumber && it.ayahNumber == verse.numberInSurah }
                            val isPlayingVerse = audioState.isPlaying &&
                                audioState.playingSurah == verse.surahNumber &&
                                audioState.playingAyah == verse.numberInSurah
                            val isMemorizeMode = prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE
                            val isRevealed = !isMemorizeMode || (revealedInMemorize[verse.numberInSurah] == true)

                            VerseCard(
                                verse = verse,
                                fontSizeSp = prefs.quranFontSizeSp,
                                primaryInk = primaryInk,
                                secondaryInk = secondaryInk,
                                isBookmarked = isBookmarked,
                                isPlaying = isPlayingVerse,
                                isMemorizeMode = isMemorizeMode,
                                isRevealed = isRevealed,
                                showTafsir = showTafsir && !isMemorizeMode,
                                showEnglish = showEnglish && !isMemorizeMode,
                                onToggleReveal = {
                                    revealedInMemorize[verse.numberInSurah] = !(revealedInMemorize[verse.numberInSurah] ?: false)
                                },
                                onPlay = {
                                    if (isPlayingVerse) {
                                        viewModel.toggleAudioPauseResume()
                                    } else {
                                        viewModel.playQuranVerse(verse, state.verses, repeatPerAyah)
                                    }
                                },
                                onBookmark = {
                                    viewModel.toggleBookmark(verse.surahNumber, verse.numberInSurah, verse.textUthmani)
                                },
                                onSavePosition = {
                                    viewModel.saveLastRead(verse.surahNumber, verse.numberInSurah)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseCard(
    verse: QuranVerse,
    fontSizeSp: Int,
    primaryInk: Color,
    secondaryInk: Color,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    isMemorizeMode: Boolean,
    isRevealed: Boolean,
    showTafsir: Boolean,
    showEnglish: Boolean,
    onToggleReveal: () -> Unit,
    onPlay: () -> Unit,
    onBookmark: () -> Unit,
    onSavePosition: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    LiquidGlassCard(
        goldBorder = isPlaying,
        onClick = {
            if (isMemorizeMode) onToggleReveal() else onSavePosition()
        },
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldPrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${verse.surahNumber}:${verse.numberInSurah} · p.${verse.pageNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMemorizeMode) {
                    IconButton(onClick = onToggleReveal, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "Reveal or Hide",
                            tint = noorColors.goldAccent
                        )
                    }
                }
                IconButton(onClick = onPlay, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play Ayah",
                        tint = noorColors.goldAccent
                    )
                }
                IconButton(onClick = onBookmark, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) noorColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isRevealed) {
            Text(
                text = "${verse.textUthmani} ﴿${verse.numberInSurah}﴾",
                fontFamily = AmiriFontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.85f).sp,
                color = primaryInk,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EmeraldDeep.copy(alpha = 0.35f))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = L10n.pick(lang, "اضغط لإظهار الآية ${verse.numberInSurah} واختبار حفظك", "Tap to reveal Ayah ${verse.numberInSurah} for memorization check"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldBright,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showEnglish && verse.translationEn.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = verse.translationEn,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryInk
            )
        }

        if (showTafsir && verse.tafsirAr.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = L10n.pick(lang, "التفسير الميسر", "Tafsir Al-Muyassar"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = verse.tafsirAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = primaryInk.copy(alpha = 0.9f)
                    )
                }
            }
        }

        if (showTafsir && verse.wordMeaningsAr.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = L10n.pick(lang, "معاني الكلمات: ${verse.wordMeaningsAr}", "Word Meanings: ${verse.wordMeaningsAr}"),
                style = MaterialTheme.typography.labelSmall,
                color = noorColors.goldAccent
            )
        }

        if (showTafsir && (verse.asbabAlNuzulAr.isNotBlank() || verse.asbabAlNuzulEn.isNotBlank())) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GoldPrimary.copy(alpha = 0.12f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = L10n.pick(lang, "سبب النزول الموثق", "Verified Asbab al-Nuzul"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                    Text(
                        text = L10n.pick(lang, verse.asbabAlNuzulAr, verse.asbabAlNuzulEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = primaryInk.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
