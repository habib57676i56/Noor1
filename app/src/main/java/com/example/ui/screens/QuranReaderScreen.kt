package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.LiveRecitationState
import com.example.audio.LiveRecitationStatus
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
import kotlinx.coroutines.delay

private enum class AutoScrollMode {
    STOPPED,
    RUNNING,
    PAUSED
}

@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    viewModel: NoorViewModel,
    onBack: () -> Unit,
    onNavigateToSurah: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val readerState by viewModel.surahReaderState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.quranBookmarks.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val liveRecitation by viewModel.liveRecitationState.collectAsStateWithLifecycle()

    val surahMeta = remember(surahNumber) { SurahCatalog.findSurah(surahNumber) }
    val listState = rememberLazyListState()

    var showTafsir by remember { mutableStateOf(true) }
    var showEnglish by remember { mutableStateOf(true) }
    var repeatPerAyah by remember { mutableIntStateOf(1) }
    val revealedInMemorize = remember { mutableStateMapOf<Int, Boolean>() }

    // Intelligent Auto-Scrolling state (persists seamlessly across Surah transitions!)
    var autoScrollMode by rememberSaveable { mutableStateOf(AutoScrollMode.STOPPED) }
    var showAutoScrollControls by rememberSaveable { mutableStateOf(false) }

    // Microphone permission privacy rationale dialog for Listen & Follow
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var lastVisibleAyah by remember(surahNumber) { mutableIntStateOf(if (prefs.lastReadSurah == surahNumber) prefs.lastReadAyah else 1) }
    var restoredInitialPositionForSurah by remember(surahNumber) { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            autoScrollMode = AutoScrollMode.STOPPED
            viewModel.startLiveRecitationTracking()
        } else {
            viewModel.showBannerMessage(
                L10n.pick(
                    lang,
                    "يلزم منح إذن الميكروفون لتشغيل التتبع المباشر للآيات.",
                    "Microphone permission is needed for Listen & Follow tracking."
                )
            )
        }
    }

    LaunchedEffect(surahNumber) {
        viewModel.loadSurah(surahNumber)
    }

    // Stop microphone when leaving QuranReaderScreen completely; preserve reading position
    DisposableEffect(surahNumber) {
        onDispose {
            if (lastVisibleAyah > 0) {
                viewModel.saveLastRead(surahNumber, lastVisibleAyah)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLiveRecitationTracking()
        }
    }

    // Sync Surah verses into the on-device QuranVerseTrackerEngine & restore last-read position
    val headerItemsOffset = if (surahNumber != 1 && surahNumber != 9) 2 else 1

    LaunchedEffect(readerState, surahNumber) {
        val st = readerState
        if (st is SurahReaderState.Ready && st.surahNumber == surahNumber) {
            val savedAyah = if (prefs.lastReadSurah == surahNumber) prefs.lastReadAyah else 1
            viewModel.syncLiveTrackerSurah(surahNumber, st.verses, initialAyah = savedAyah)

            if (!restoredInitialPositionForSurah) {
                restoredInitialPositionForSurah = true
                if (savedAyah > 1) {
                    val targetIdx = if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF) {
                        val pages = st.verses.groupBy { it.pageNumber }.entries.toList()
                        val pageIdx = pages.indexOfFirst { (_, pv) -> pv.any { it.numberInSurah == savedAyah } }
                        if (pageIdx >= 0) headerItemsOffset + pageIdx else 0
                    } else {
                        val ayahIdx = st.verses.indexOfFirst { it.numberInSurah == savedAyah }
                        if (ayahIdx >= 0) headerItemsOffset + ayahIdx else 0
                    }
                    if (targetIdx > 0) {
                        runCatching { listState.scrollToItem(targetIdx) }
                    }
                } else {
                    runCatching { listState.scrollToItem(0) }
                }
            }
        }
    }

    // Track current visible Ayah so pausing or leaving always preserves exact reading position
    LaunchedEffect(listState.firstVisibleItemIndex, readerState, prefs.quranDisplayMode) {
        val st = readerState
        if (st is SurahReaderState.Ready && st.surahNumber == surahNumber) {
            val contentIndex = (listState.firstVisibleItemIndex - headerItemsOffset).coerceAtLeast(0)
            val ayah = if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF) {
                val pages = st.verses.groupBy { it.pageNumber }.entries.toList()
                pages.getOrNull(contentIndex)?.value?.firstOrNull()?.numberInSurah
            } else {
                st.verses.getOrNull(contentIndex)?.numberInSurah
            }
            if (ayah != null && ayah > 0) {
                lastVisibleAyah = ayah
            }
        }
    }

    // Smooth, frame-synchronized Intelligent Auto-Scrolling across both Study/Memorize & Mushaf modes
    LaunchedEffect(autoScrollMode, prefs.quranAutoScrollSpeed, readerState, surahNumber) {
        if (autoScrollMode != AutoScrollMode.RUNNING) return@LaunchedEffect
        val st = readerState
        if (st !is SurahReaderState.Ready || st.surahNumber != surahNumber) return@LaunchedEffect

        var lastFrameNanos = 0L
        while (autoScrollMode == AutoScrollMode.RUNNING) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos != 0L) {
                val dtSec = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.08f)
                val pixelsPerSec = 68f * prefs.quranAutoScrollSpeed.coerceIn(0.4f, 3.0f)
                val consumed = listState.scrollBy(pixelsPerSec * dtSec)
                if (!listState.canScrollForward && consumed < 0.25f) {
                    // Reached end of Surah — smoothly continue to the next Surah/page if available
                    viewModel.saveLastRead(surahNumber, st.verses.lastOrNull()?.numberInSurah ?: 1)
                    if (surahNumber < 114) {
                        delay(1100L)
                        if (autoScrollMode == AutoScrollMode.RUNNING) {
                            onNavigateToSurah(surahNumber + 1)
                        }
                    } else {
                        autoScrollMode = AutoScrollMode.STOPPED
                    }
                    break
                }
            }
            lastFrameNanos = frameNanos
        }
    }

    // Automatic Quran Page & Ayah Synchronization when Listen & Follow recognizes a verse
    LaunchedEffect(liveRecitation.trackedSurah, liveRecitation.trackedAyah, liveRecitation.status, prefs.quranDisplayMode) {
        val st = readerState
        val ayahNum = liveRecitation.trackedAyah
        if (st is SurahReaderState.Ready &&
            st.surahNumber == surahNumber &&
            liveRecitation.trackedSurah == surahNumber &&
            ayahNum != null &&
            liveRecitation.status == LiveRecitationStatus.MATCHED
        ) {
            lastVisibleAyah = ayahNum
            viewModel.saveLastRead(surahNumber, ayahNum)

            val targetIndex = if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF) {
                val pages = st.verses.groupBy { it.pageNumber }.entries.toList()
                val pageIdx = pages.indexOfFirst { (_, pv) -> pv.any { it.numberInSurah == ayahNum } }
                if (pageIdx >= 0) headerItemsOffset + pageIdx else -1
            } else {
                val verseIdx = st.verses.indexOfFirst { it.numberInSurah == ayahNum }
                if (verseIdx >= 0) headerItemsOffset + verseIdx else -1
            }
            if (targetIndex >= 0) {
                runCatching { listState.animateScrollToItem(targetIndex) }
            }
        }
    }

    // Cross-Surah Automatic Jump when reciter begins another Surah with high confidence
    LaunchedEffect(liveRecitation.crossSurahJumpTarget) {
        val jump = liveRecitation.crossSurahJumpTarget
        if (jump != null && jump.first != surahNumber && jump.first in 1..114) {
            viewModel.clearCrossSurahJumpTarget()
            viewModel.saveLastRead(jump.first, jump.second)
            onNavigateToSurah(jump.first)
        }
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

    val activeLiveAyah: Int? = if (
        (liveRecitation.isActive || liveRecitation.status == LiveRecitationStatus.PAUSED) &&
        liveRecitation.trackedSurah == surahNumber
    ) {
        liveRecitation.trackedAyah
    } else {
        null
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
            onBack = {
                viewModel.saveLastRead(surahNumber, lastVisibleAyah)
                viewModel.stopLiveRecitationTracking()
                onBack()
            },
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

        // Sticky Live Recitation Tracking Banner (always visible while Listen & Follow is active or paused!)
        if (liveRecitation.status != LiveRecitationStatus.IDLE) {
            LiveRecitationTrackingBanner(
                state = liveRecitation,
                onPause = { viewModel.pauseLiveRecitationTracking() },
                onResume = { viewModel.resumeLiveRecitationTracking() },
                onStop = { viewModel.stopLiveRecitationTracking() }
            )
        }

        // Sticky Auto-Scroll Control Bar (when enabled, provides instant Start/Pause/Resume/Stop + Speed!)
        if (showAutoScrollControls || autoScrollMode != AutoScrollMode.STOPPED) {
            AutoScrollControlBar(
                mode = autoScrollMode,
                speed = prefs.quranAutoScrollSpeed,
                onStart = {
                    viewModel.stopLiveRecitationTracking()
                    autoScrollMode = AutoScrollMode.RUNNING
                },
                onPause = {
                    autoScrollMode = AutoScrollMode.PAUSED
                    viewModel.saveLastRead(surahNumber, lastVisibleAyah)
                },
                onResume = {
                    autoScrollMode = AutoScrollMode.RUNNING
                },
                onStop = {
                    autoScrollMode = AutoScrollMode.STOPPED
                    showAutoScrollControls = false
                    viewModel.saveLastRead(surahNumber, lastVisibleAyah)
                },
                onSpeedChange = { viewModel.setQuranAutoScrollSpeed(it) }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reading Mode + Theme + Reciter + Smart Controls Deck
            item(key = "reader_controls_deck") {
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

                    // Smart Feature Bar: Play Surah · Auto Scroll · Listen & Follow (Live AI Verse Sync)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.playFullSurahWithReciter(surahNumber, prefs.quranReciter) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(L10n.pick(lang, "تلاوة", "Play"), maxLines = 1)
                        }

                        // Listen & Follow AI Live Verse Tracking Button
                        val isListening = liveRecitation.isActive
                        Button(
                            onClick = {
                                if (isListening || liveRecitation.status == LiveRecitationStatus.PAUSED) {
                                    viewModel.stopLiveRecitationTracking()
                                } else {
                                    val hasMic = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasMic) {
                                        autoScrollMode = AutoScrollMode.STOPPED
                                        viewModel.startLiveRecitationTracking()
                                    } else {
                                        showMicPermissionDialog = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("listen_and_follow_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) GoldPrimary else EmeraldDeep,
                                contentColor = if (isListening) Color(0xFF071B14) else GoldBright
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isListening) {
                                    L10n.pick(lang, "إيقاف المتابعة", "Stop Follow")
                                } else {
                                    L10n.pick(lang, "استماع ومتابعة", "Listen & Follow")
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Auto-Scroll Toggle Button
                        OutlinedButton(
                            onClick = {
                                if (autoScrollMode == AutoScrollMode.RUNNING) {
                                    autoScrollMode = AutoScrollMode.PAUSED
                                    viewModel.saveLastRead(surahNumber, lastVisibleAyah)
                                } else {
                                    showAutoScrollControls = true
                                    viewModel.stopLiveRecitationTracking()
                                    autoScrollMode = AutoScrollMode.RUNNING
                                }
                            },
                            modifier = Modifier.testTag("auto_scroll_toggle_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = null,
                                tint = if (autoScrollMode == AutoScrollMode.RUNNING) GoldBright else noorColors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = L10n.pick(lang, "تمرير", "Scroll"),
                                maxLines = 1
                            )
                        }
                    }

                    // Existing Study / Memorize secondary toggles preserved untouched
                    if (prefs.quranDisplayMode == QuranDisplayMode.STUDY || prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
            }

            // Bismillah Header (except At-Tawbah 9 and Al-Fatiha 1 which already contains it as Ayah 1)
            if (surahNumber != 1 && surahNumber != 9) {
                item(key = "bismillah_header") {
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
                        // Continuous Mushaf Page Flow with LIVE inline gold Ayah highlighting (exact Uthmani text preserved!)
                        val groupedByPage = state.verses.groupBy { it.pageNumber }
                        items(groupedByPage.entries.toList(), key = { "page_${it.key}" }) { (pageNum, pageVerses) ->
                            val hasTrackedOnPage = activeLiveAyah != null &&
                                pageVerses.any { it.numberInSurah == activeLiveAyah }

                            val annotatedPage = remember(pageVerses, activeLiveAyah, primaryInk) {
                                buildAnnotatedString {
                                    pageVerses.forEachIndexed { idx, v ->
                                        val isTracked = activeLiveAyah == v.numberInSurah
                                        if (isTracked) {
                                            withStyle(
                                                SpanStyle(
                                                    background = GoldPrimary.copy(alpha = 0.28f),
                                                    color = primaryInk,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append("${v.textUthmani} ﴿${v.numberInSurah}﴾")
                                            }
                                        } else {
                                            append("${v.textUthmani} ﴿${v.numberInSurah}﴾")
                                        }
                                        if (idx < pageVerses.lastIndex) {
                                            append("  ")
                                        }
                                    }
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
                                    .border(
                                        width = if (hasTrackedOnPage) 2.dp else 1.2.dp,
                                        color = if (hasTrackedOnPage) GoldBright else GoldPrimary.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = annotatedPage,
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
                                        text = L10n.pick(
                                            lang,
                                            "صفحة $pageNum · الجزء ${pageVerses.first().juzNumber}" +
                                                if (hasTrackedOnPage) " · تتبع مباشر: آية $activeLiveAyah" else "",
                                            "Page $pageNum · Juz ${pageVerses.first().juzNumber}" +
                                                if (hasTrackedOnPage) " · Live Sync: Ayah $activeLiveAyah" else ""
                                        ),
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
                            val isLiveTracked = activeLiveAyah == verse.numberInSurah
                            val isMemorizeMode = prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE
                            val isRevealed = !isMemorizeMode || (revealedInMemorize[verse.numberInSurah] == true) || isLiveTracked

                            VerseCard(
                                verse = verse,
                                fontSizeSp = prefs.quranFontSizeSp,
                                primaryInk = primaryInk,
                                secondaryInk = secondaryInk,
                                isBookmarked = isBookmarked,
                                isPlaying = isPlayingVerse,
                                isLiveTracked = isLiveTracked,
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
                                    lastVisibleAyah = verse.numberInSurah
                                    viewModel.saveLastRead(verse.surahNumber, verse.numberInSurah)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Privacy-First Microphone Rationale Dialog before requesting RECORD_AUDIO
    if (showMicPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDialog = false },
            icon = { Icon(Icons.Rounded.Mic, contentDescription = null, tint = GoldPrimary) },
            title = {
                Text(
                    L10n.pick(
                        lang,
                        "الاستماع والمتابعة المباشرة للقرآن",
                        "Listen & Follow — Live Quran Sync"
                    )
                )
            },
            text = {
                Text(
                    L10n.pick(
                        lang,
                        "يستخدم تطبيق نور الميكروفون للاستماع إلى التلاوة والتعرف على الآية الحالية وتحريك المصحف تلقائياً.\n\nالخصوصية أولاً: تتم معالجة الصوت محلياً على جهازك ولا يتم تسجيل صوتك أو رفعه إلى أي خادم.",
                        "NOOR uses your microphone to recognize recited verses in real time and automatically highlight and scroll the Mushaf.\n\nPrivacy First: Audio is processed locally on your device and is never recorded or uploaded."
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    showMicPermissionDialog = false
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text(L10n.pick(lang, "سماح والبدء", "Allow & Start"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionDialog = false }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun AutoScrollControlBar(
    mode: AutoScrollMode,
    speed: Float,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    LiquidGlassCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("auto_scroll_control_bar"),
        goldBorder = mode == AutoScrollMode.RUNNING,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = noorColors.goldAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (mode) {
                        AutoScrollMode.RUNNING -> L10n.pick(lang, "التمرير التلقائي يعمل", "Auto-Scrolling")
                        AutoScrollMode.PAUSED -> L10n.pick(lang, "التمرير متوقف مؤقتاً", "Auto-Scroll Paused")
                        AutoScrollMode.STOPPED -> L10n.pick(lang, "التمرير التلقائي", "Auto-Scroll")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (mode) {
                    AutoScrollMode.RUNNING -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(36.dp).testTag("auto_scroll_pause_btn")) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Pause Auto Scroll", tint = GoldBright)
                        }
                    }
                    AutoScrollMode.PAUSED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp).testTag("auto_scroll_resume_btn")) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume Auto Scroll", tint = GoldBright)
                        }
                    }
                    AutoScrollMode.STOPPED -> {
                        IconButton(onClick = onStart, modifier = Modifier.size(36.dp).testTag("auto_scroll_start_btn")) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Auto Scroll", tint = GoldBright)
                        }
                    }
                }
                IconButton(onClick = onStop, modifier = Modifier.size(36.dp).testTag("auto_scroll_stop_btn")) {
                    Icon(Icons.Rounded.Stop, contentDescription = "Stop Auto Scroll", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = L10n.pick(lang, "السرعة:", "Speed:"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { spd ->
                val selected = kotlin.math.abs(speed - spd) < 0.1f
                LiquidGlassChip(
                    label = "${spd}x",
                    selected = selected,
                    onClick = { onSpeedChange(spd) }
                )
            }
        }
    }
}

@Composable
private fun LiveRecitationTrackingBanner(
    state: LiveRecitationState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val badgeColor by animateColorAsState(
        targetValue = when (state.status) {
            LiveRecitationStatus.MATCHED -> GoldBright
            LiveRecitationStatus.LISTENING -> EmeraldPrimary
            LiveRecitationStatus.UNCERTAIN -> GoldPrimary.copy(alpha = 0.75f)
            LiveRecitationStatus.PAUSED -> Color(0xFF90A4AE)
            LiveRecitationStatus.ERROR -> MaterialTheme.colorScheme.error
            LiveRecitationStatus.IDLE -> Color.Transparent
        },
        label = "live_recitation_badge"
    )

    LiquidGlassCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("live_recitation_banner"),
        goldBorder = state.status == LiveRecitationStatus.MATCHED || state.status == LiveRecitationStatus.LISTENING,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.22f))
                    .border(1.dp, badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.status == LiveRecitationStatus.PAUSED) Icons.Rounded.MicOff else Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L10n.pick(lang, state.statusNoteAr, state.statusNoteEn).ifBlank {
                        L10n.pick(lang, "استماع ومتابعة التلاوة...", "Listen & Follow Active...")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.heardText.isNotBlank()) {
                    Text(
                        text = state.heardText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (state.status == LiveRecitationStatus.PAUSED) {
                IconButton(onClick = onResume, modifier = Modifier.size(36.dp).testTag("live_follow_resume_btn")) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume Listening", tint = GoldBright)
                }
            } else if (state.isActive) {
                IconButton(onClick = onPause, modifier = Modifier.size(36.dp).testTag("live_follow_pause_btn")) {
                    Icon(Icons.Rounded.Pause, contentDescription = "Pause Listening", tint = GoldBright)
                }
            }

            IconButton(onClick = onStop, modifier = Modifier.size(36.dp).testTag("live_follow_stop_btn")) {
                Icon(Icons.Rounded.Stop, contentDescription = "Stop Listening", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (state.isActive) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { state.rmsLevel.coerceIn(0.08f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = badgeColor,
                trackColor = badgeColor.copy(alpha = 0.2f)
            )
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
    isLiveTracked: Boolean,
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
        goldBorder = isPlaying || isLiveTracked,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldPrimary.copy(alpha = if (isLiveTracked) 0.35f else 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${verse.surahNumber}:${verse.numberInSurah} · p.${verse.pageNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = noorColors.goldAccent
                    )
                }

                if (isLiveTracked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.28f))
                            .border(1.dp, GoldBright, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = L10n.pick(lang, "تتبع مباشر", "Live Sync"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                    }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLiveTracked) GoldPrimary.copy(alpha = 0.14f) else Color.Transparent)
                    .padding(horizontal = if (isLiveTracked) 8.dp else 0.dp, vertical = if (isLiveTracked) 6.dp else 0.dp)
            ) {
                Text(
                    text = "${verse.textUthmani} ﴿${verse.numberInSurah}﴾",
                    fontFamily = AmiriFontFamily,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.85f).sp,
                    color = primaryInk,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
