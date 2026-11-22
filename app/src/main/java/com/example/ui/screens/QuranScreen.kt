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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.SurahCatalog
import com.example.ui.components.IslamicRosetteOrnament
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.OrnamentalSurahBadge
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldForest
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.viewmodel.NoorViewModel

@Composable
fun QuranScreen(
    viewModel: NoorViewModel,
    onOpenSurah: (Int) -> Unit,
    onOpenAudioLibrary: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val bookmarks by viewModel.quranBookmarks.collectAsStateWithLifecycle()
    val verseResults by viewModel.verseSearchResults.collectAsStateWithLifecycle()

    // 0: Surahs, 1: Juz & Hizb, 2: Mushaf Pages, 3: Bookmarks
    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var pageInput by remember { mutableStateOf("1") }

    LaunchedEffect(searchQuery) {
        viewModel.searchQuranVerses(searchQuery)
    }

    val filteredSurahs = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) SurahCatalog.allSurahs
        else SurahCatalog.allSurahs.filter {
            it.nameAr.contains(q, ignoreCase = true) ||
                it.nameEn.contains(q, ignoreCase = true) ||
                it.meaningEn.contains(q, ignoreCase = true) ||
                it.number.toString() == q
        }
    }

    val lastReadSurah = remember(prefs.lastReadSurah) { SurahCatalog.findSurah(prefs.lastReadSurah) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_screen")
    ) {
        NoorScreenHeader(
            title = L10n.pick(lang, "القرآن الكريم", "The Holy Quran"),
            subtitle = L10n.pick(lang, "١١٤ سورة · ٣٠ جزءاً · ٦٠٤ صفحة مع التفسير والتلاوة", "114 Surahs · 30 Juz · 604 Mushaf Pages & Tafsir"),
            trailing = {
                IconButton(
                    onClick = onOpenAudioLibrary,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Rounded.Headphones, contentDescription = "Audio Library", tint = noorColors.goldAccent)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ornamental Resume Reading Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldForest, Color(0xFF0A2A20))))
                        .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .clickable { onOpenSurah(lastReadSurah.number) }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IslamicRosetteOrnament(size = 42.dp, tint = GoldBright)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "متابعة القراءة من حيث توقفت", "Continue Last Reading"),
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldBright
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "سورة ${lastReadSurah.nameAr} — الآية ${prefs.lastReadAyah}",
                                    "${lastReadSurah.nameEn} — Ayah ${prefs.lastReadAyah}"
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "الجزء ${lastReadSurah.startJuz} · صفحة ${lastReadSurah.startPage}",
                                    "Juz ${lastReadSurah.startJuz} · Page ${lastReadSurah.startPage}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                        Icon(
                            Icons.Rounded.AutoStories,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Yasser Al-Dosari Audio Shortcut Strip
            item {
                LiquidGlassCard(
                    goldBorder = true,
                    onClick = onOpenAudioLibrary,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Headphones, contentDescription = null, tint = noorColors.goldAccent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, "تلاوات الشيخ ياسر الدوسري والمكتبة الصوتية", "Yasser Al-Dosari — Quran Recitations"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Text(
                                text = L10n.pick(lang, "استمع للمصحف المرتل الكامل مع مؤقت النوم والسرعة", "Full 114-Surah streaming, sleep timer, playlists & offline cache"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Open Audio", tint = noorColors.goldAccent)
                    }
                }
            }

            // Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = {
                        Text(
                            L10n.pick(lang, "ابحث عن سورة أو رقم أو آية محفوظة...", "Search Surah, number, or cached Ayah..."),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            // Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = L10n.pick(lang, "السور", "Surahs"),
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الأجزاء", "Juz"),
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "الصفحات", "Pages"),
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassChip(
                        label = L10n.pick(lang, "العلامات", "Saved"),
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Cached Verse Search Matches
            if (searchQuery.trim().length >= 2 && verseResults.isNotEmpty()) {
                item {
                    Text(
                        text = L10n.pick(lang, "نتائج الآيات (${verseResults.size})", "Matching Ayahs (${verseResults.size})"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }
                items(verseResults.take(10), key = { "v_${it.surahNumber}_${it.numberInSurah}" }) { v ->
                    val sMeta = SurahCatalog.findSurah(v.surahNumber)
                    LiquidGlassCard(
                        onClick = { onOpenSurah(v.surahNumber) },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = L10n.pick(lang, "سورة ${sMeta.nameAr} · الآية ${v.numberInSurah}", "${sMeta.nameEn} · Ayah ${v.numberInSurah}"),
                            style = MaterialTheme.typography.labelMedium,
                            color = noorColors.goldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = v.textUthmani,
                            fontFamily = AmiriFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            when (activeTab) {
                0 -> {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        LiquidGlassCard(
                            onClick = { onOpenSurah(surah.number) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("surah_item_${surah.number}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OrnamentalSurahBadge(number = surah.number, size = 44.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = L10n.pick(lang, "سورة ${surah.nameAr}", surah.nameEn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = L10n.pick(
                                            lang,
                                            "${if (surah.isMeccan) "مكية" else "مدنية"} · ${surah.verseCount} آية · ص ${surah.startPage}",
                                            "${surah.meaningEn} · ${surah.verseCount} Ayahs · p.${surah.startPage}"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = surah.nameAr,
                                    fontFamily = AmiriFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = noorColors.goldAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.playFullSurahWithReciter(surah.number) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = "Play Surah",
                                        tint = noorColors.goldAccent
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    items((1..30).toList(), key = { it }) { juz ->
                        val surahsInJuz = SurahCatalog.allSurahs.filter { it.startJuz == juz }
                        val firstSurah = surahsInJuz.firstOrNull() ?: SurahCatalog.findSurah(1)
                        val startHizb = (juz - 1) * 2 + 1
                        LiquidGlassCard(
                            onClick = { onOpenSurah(firstSurah.number) },
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OrnamentalSurahBadge(number = juz, size = 44.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = L10n.pick(lang, "الجزء $juz · الحزب $startHizb - ${startHizb + 1}", "Juz $juz · Hizb $startHizb - ${startHizb + 1}"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = L10n.pick(
                                            lang,
                                            "يبدأ من سورة ${firstSurah.nameAr} · ص ${firstSurah.startPage} · ٤ أرباع لكل حزب",
                                            "Starts at ${firstSurah.nameEn} · Page ${firstSurah.startPage} · 8 Rub sections"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = noorColors.goldAccent)
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        LiquidGlassCard(goldBorder = true, contentPadding = PaddingValues(18.dp)) {
                            Text(
                                text = L10n.pick(lang, "الانتقال السريع برقم صفحة المصحف (١ — ٦٠٤)", "Mushaf Page Navigation (1 — 604)"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = pageInput,
                                    onValueChange = { s -> pageInput = s.filter { it.isDigit() }.take(3) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    label = { Text(L10n.pick(lang, "رقم الصفحة", "Page Number")) }
                                )
                                Button(
                                    onClick = {
                                        val page = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: 1
                                        val targetSurah = SurahCatalog.findSurahByPage(page)
                                        onOpenSurah(targetSurah.number)
                                    },
                                    modifier = Modifier.height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text(L10n.pick(lang, "فتح", "Go"))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val quickPages = listOf(
                                1 to L10n.pick(lang, "الفاتحة (ص ١)", "Al-Fatiha (p.1)"),
                                293 to L10n.pick(lang, "الكهف (ص ٢٩٣)", "Al-Kahf (p.293)"),
                                440 to L10n.pick(lang, "يس (ص ٤٤٠)", "Ya-Sin (p.440)"),
                                562 to L10n.pick(lang, "الملك (ص ٥٦٢)", "Al-Mulk (p.562)"),
                                582 to L10n.pick(lang, "جزء عمّ (ص ٥٨٢)", "Juz Amma (p.582)")
                            )
                            quickPages.forEach { (pNum, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenSurah(SurahCatalog.findSurahByPage(pNum).number) }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("p.$pNum", style = MaterialTheme.typography.labelMedium, color = noorColors.goldAccent)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    if (bookmarks.isEmpty()) {
                        item {
                            LiquidGlassCard(contentPadding = PaddingValues(24.dp)) {
                                Text(
                                    text = L10n.pick(lang, "لا توجد علامات مرجعية محفوظة بعد. اضغط على أيقونة العلامة بجانب أي آية أثناء القراءة.", "No saved bookmarks yet. Tap the bookmark icon next to any verse while reading."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(bookmarks, key = { "${it.surahNumber}_${it.ayahNumber}" }) { bm ->
                            LiquidGlassCard(
                                onClick = { onOpenSurah(bm.surahNumber) },
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = noorColors.goldAccent)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = L10n.pick(lang, "سورة ${bm.surahNameAr} · الآية ${bm.ayahNumber}", "${bm.surahNameEn} · Ayah ${bm.ayahNumber}"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = bm.ayahPreview,
                                            fontFamily = AmiriFontFamily,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
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
    }
}
