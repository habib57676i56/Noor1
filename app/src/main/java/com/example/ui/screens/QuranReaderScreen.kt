package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.audio.AudioPlaybackState
import com.example.audio.LiveRecitationState
import com.example.audio.LiveRecitationStatus
import com.example.data.local.catalog.SurahCatalog
import com.example.domain.model.AudioRepeatMode
import com.example.domain.model.QuranDisplayMode
import com.example.domain.model.QuranReadingTheme
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.ui.components.ChooseReciterButton
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
import com.example.viewmodel.EcosystemViewModel
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
    onNavigateToSurah: (Int) -> Unit = {},
    onOpenInMemorization: (surahNumber: Int, ayahNumber: Int) -> Unit = { _, _ -> },
    ecosystemViewModel: EcosystemViewModel? = null,
    onFullScreenChanged: (Boolean) -> Unit = {},
    openAtLastReadPage: Boolean = false,
    showBackButton: Boolean = true,
    onOpenSurahIndex: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val readerState by viewModel.surahReaderState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.quranBookmarks.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val liveRecitation by viewModel.liveRecitationState.collectAsStateWithLifecycle()
    val globalConfig by viewModel.globalAppConfig.collectAsStateWithLifecycle()
    val highlightStyle by viewModel.mushafHighlightStyle.collectAsStateWithLifecycle()
    val activeWordPosition by viewModel.activeWordPosition.collectAsStateWithLifecycle()
    val hasVerifiedWordTimings by viewModel.hasVerifiedWordTimingsForCurrentPlayback.collectAsStateWithLifecycle()
    val customReciters by viewModel.customReciters.collectAsStateWithLifecycle()
    val selectedCustomReciterId by viewModel.selectedMushafCustomReciterId.collectAsStateWithLifecycle()

    val surahMeta = remember(surahNumber) { SurahCatalog.findSurah(surahNumber) }
    val listState = rememberLazyListState()

    var showTafsir by remember { mutableStateOf(true) }
    var showEnglish by remember { mutableStateOf(true) }
    var repeatPerAyah by remember { mutableIntStateOf(1) }
    var useMushafPageImages by rememberSaveable { mutableStateOf(true) }
    val effectiveMushafImageMode = useMushafPageImages && globalConfig.featureMushafImagesEnabled
    val revealedInMemorize = remember { mutableStateMapOf<Int, Boolean>() }

    // Collapsible Top Controls Bar + Imperative "Last Read Page" Jump Request
    var isTopControlsCollapsed by rememberSaveable { mutableStateOf(false) }
    var showMoreReaderOptions by rememberSaveable { mutableStateOf(false) }
    var activeMushafPage by remember(surahNumber, openAtLastReadPage) {
        mutableIntStateOf(
            if (openAtLastReadPage || prefs.lastReadSurah == surahNumber) {
                prefs.lastReadPage.coerceIn(1, 604)
            } else {
                surahMeta.startPage
            }
        )
    }
    var jumpToPageNonce by remember { mutableIntStateOf(0) }
    var jumpToPageRequest by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Clean Full-Screen Mushaf Mode (toggled by single-tapping an empty Mushaf area)
    var isMushafFullScreen by rememberSaveable { mutableStateOf(false) }
    val isFullScreenActive = isMushafFullScreen &&
        prefs.quranDisplayMode == QuranDisplayMode.MUSHAF &&
        effectiveMushafImageMode

    // Integrated In-Reader "My Recitation & Memorization" Panel (distinct from Sheikh Listening)
    var showRecitationPanel by rememberSaveable { mutableStateOf(false) }
    var lastVisibleAyah by remember(surahNumber) {
        mutableIntStateOf(if (prefs.lastReadSurah == surahNumber) prefs.lastReadAyah.coerceAtLeast(1) else 1)
    }
    var recitationTargetAyah by remember(surahNumber) { mutableIntStateOf(lastVisibleAyah) }
    var restoredInitialPositionForSurah by remember(surahNumber) { mutableStateOf(false) }
    var userHasInteractedWithSurah by remember(surahNumber) { mutableStateOf(false) }

    // Intelligent Auto-Scrolling state (persists seamlessly across Surah transitions!)
    var autoScrollMode by rememberSaveable { mutableStateOf(AutoScrollMode.STOPPED) }
    var showAutoScrollControls by rememberSaveable { mutableStateOf(false) }

    // Microphone permission privacy rationale dialog for Listen & Follow
    var showMicPermissionDialog by remember { mutableStateOf(false) }

    // Sync Clean Full-Screen state to parent NoorAppShell (so bottom dock, top bar, mini-player & system bars hide cleanly)
    LaunchedEffect(isFullScreenActive) {
        onFullScreenChanged(isFullScreenActive)
        if (isFullScreenActive) {
            runCatching { listState.scrollToItem(0) }
        }
    }

    // Pressing system Back while in Clean Full-Screen exits Full-Screen first
    BackHandler(enabled = isFullScreenActive) {
        isMushafFullScreen = false
    }

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

    // Preserve reading position on exit ONLY when user interacted (never overwrite saved lastReadPage with unrelated page)
    DisposableEffect(surahNumber) {
        onDispose {
            if (userHasInteractedWithSurah && lastVisibleAyah > 0) {
                val pageToSave = if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode) {
                    activeMushafPage
                } else {
                    null
                }
                viewModel.saveLastRead(surahNumber, lastVisibleAyah, pageNumber = pageToSave)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            onFullScreenChanged(false)
            viewModel.stopLiveRecitationTracking()
        }
    }

    // Sync Surah verses into the on-device QuranVerseTrackerEngine & restore last-read position
    val headerItemsOffset = if (surahNumber != 1 && surahNumber != 9) 1 else 0

    LaunchedEffect(readerState, surahNumber) {
        val st = readerState
        if (st is SurahReaderState.Ready && st.surahNumber == surahNumber) {
            val savedAyah = if (prefs.lastReadSurah == surahNumber) prefs.lastReadAyah.coerceAtLeast(1) else 1
            if (!restoredInitialPositionForSurah) {
                lastVisibleAyah = savedAyah
                recitationTargetAyah = savedAyah
            }
            viewModel.syncLiveTrackerSurah(surahNumber, st.verses, initialAyah = savedAyah)

            if (!restoredInitialPositionForSurah) {
                restoredInitialPositionForSurah = true
                if (savedAyah > 1 && !(prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode)) {
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

    // Track current visible Ayah in Study/Text/Memorize modes (NEVER overwrite lastVisibleAyah in Mushaf Image mode!)
    LaunchedEffect(listState.firstVisibleItemIndex, readerState, prefs.quranDisplayMode, effectiveMushafImageMode) {
        if (!restoredInitialPositionForSurah) return@LaunchedEffect
        if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode) return@LaunchedEffect
        val st = readerState
        if (st is SurahReaderState.Ready && st.surahNumber == surahNumber) {
            val contentIndex = (listState.firstVisibleItemIndex - headerItemsOffset).coerceAtLeast(0)
            val ayah = if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF) {
                val pages = st.verses.groupBy { it.pageNumber }.entries.toList()
                pages.getOrNull(contentIndex)?.value?.firstOrNull()?.numberInSurah
            } else {
                st.verses.getOrNull(contentIndex)?.numberInSurah
            }
            if (ayah != null && ayah > 0 && listState.firstVisibleItemIndex > 0) {
                lastVisibleAyah = ayah
                userHasInteractedWithSurah = true
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
    val autoMushafPageBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)

    val activeAudioAyah: Int? = if (
        audioState.hasActiveSession &&
        audioState.playingSurah == surahNumber &&
        audioState.playingAyah != null
    ) {
        audioState.playingAyah
    } else {
        null
    }

    // Automatic Quran Page & Ayah Follow-Along Synchronization for in-app Sheikh Recitation
    LaunchedEffect(activeAudioAyah, surahNumber, prefs.quranDisplayMode, readerState) {
        val st = readerState
        val ayahNum = activeAudioAyah
        if (st is SurahReaderState.Ready &&
            st.surahNumber == surahNumber &&
            ayahNum != null
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

    val activeLiveAyah: Int? = if (
        (liveRecitation.isActive || liveRecitation.status == LiveRecitationStatus.PAUSED) &&
        liveRecitation.trackedSurah == surahNumber
    ) {
        liveRecitation.trackedAyah
    } else {
        null
    }

    // Unified synchronized Ayah (Sheikh audio follow-along OR live microphone tracking)
    val activeSyncedAyah: Int? = activeAudioAyah ?: activeLiveAyah

    CompositionLocalProvider(LocalContentColor provides primaryInk) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceBg)
            .testTag("quran_reader_screen")
    ) {
        if (!isFullScreenActive) {
            val activePageMeta = remember(activeMushafPage) {
                com.example.data.local.catalog.MadinahMushafPageCatalog.pageMeta(activeMushafPage)
            }
            val savedLastReadPage = prefs.lastReadPage.coerceIn(1, 604)
            val isMushafImgView = prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode
            val headerTitle = if (isMushafImgView) {
                L10n.pick(
                    lang,
                    "سورة ${activePageMeta.primarySurahNameAr} · ص $activeMushafPage",
                    "${activePageMeta.primarySurahNameEn} · p.$activeMushafPage"
                )
            } else {
                L10n.pick(lang, "سورة ${surahMeta.nameAr}", surahMeta.nameEn)
            }

            // Ultra-Compact Always-Accessible Top Strip (Title + Last-Read Page Icon + Index + Collapse Toggle)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(noorColors.glassSurfaceStrong.copy(alpha = 0.94f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = {
                                if (userHasInteractedWithSurah) {
                                    viewModel.saveLastRead(
                                        surahNumber,
                                        lastVisibleAyah,
                                        pageNumber = if (isMushafImgView) activeMushafPage else null
                                    )
                                }
                                viewModel.stopLiveRecitationTracking()
                                onBack()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quran_reader_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = L10n.pick(lang, "رجوع", "Back"),
                                tint = noorColors.goldAccent
                            )
                        }
                    }

                    if (onOpenSurahIndex != null) {
                        IconButton(
                            onClick = onOpenSurahIndex,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quran_open_index_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FormatListBulleted,
                                contentDescription = L10n.pick(lang, "فهرس السور والصفحات", "Surah & Page Index"),
                                tint = noorColors.goldAccent
                            )
                        }
                    }

                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )

                    // Top "Last Read Page" Shortcut Icon Pill — 1 tap opens your exact last-read page!
                    Surface(
                        onClick = {
                            val targetPage = prefs.lastReadPage.coerceIn(1, 604)
                            useMushafPageImages = true
                            val alreadyOnTarget = prefs.quranDisplayMode == QuranDisplayMode.MUSHAF &&
                                activeMushafPage == targetPage
                            if (prefs.quranDisplayMode != QuranDisplayMode.MUSHAF) {
                                viewModel.setQuranDisplayMode(QuranDisplayMode.MUSHAF)
                            }
                            if (alreadyOnTarget) {
                                viewModel.showBannerMessage(
                                    L10n.pick(
                                        lang,
                                        "أنت الآن في آخر صفحة قرأتها (صفحة $targetPage)",
                                        "Already on your last read page (p.$targetPage)"
                                    )
                                )
                            } else {
                                jumpToPageNonce++
                                jumpToPageRequest = jumpToPageNonce to targetPage
                                activeMushafPage = targetPage
                                viewModel.showBannerMessage(
                                    L10n.pick(
                                        lang,
                                        "آخر صفحة قرأتها: صفحة $targetPage",
                                        "Opened last read page: p.$targetPage"
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldDeep.copy(alpha = 0.88f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.72f)),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("quran_last_read_page_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = L10n.pick(lang, "آخر صفحة قرأتها", "Last read page"),
                                tint = GoldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = L10n.pick(lang, "ص $savedLastReadPage", "p.$savedLastReadPage"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    // Collapsible Top Bar Chevron Toggle ("طي / إظهار")
                    IconButton(
                        onClick = { isTopControlsCollapsed = !isTopControlsCollapsed },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("quran_header_collapse_btn")
                    ) {
                        Icon(
                            imageVector = if (isTopControlsCollapsed) {
                                Icons.Rounded.KeyboardArrowDown
                            } else {
                                Icons.Rounded.KeyboardArrowUp
                            },
                            contentDescription = if (isTopControlsCollapsed) {
                                L10n.pick(lang, "إظهار شريط التحكم", "Expand top controls")
                            } else {
                                L10n.pick(lang, "طي شريط التحكم", "Collapse top controls")
                            },
                            tint = noorColors.goldAccent
                        )
                    }
                }

                // Compact Collapsible Controls Deck: ONLY 3 clean mode tabs + slim action strip
                AnimatedVisibility(visible = !isTopControlsCollapsed) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Row 1: ONLY the 3 Clean Mode Tabs (Madinah Mushaf Images | Tafsir | Memorization)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quran_mode_tabs_row"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val modes = listOf(
                                Triple(
                                    QuranDisplayMode.MUSHAF,
                                    L10n.pick(lang, "مصحف المدينة", "Mushaf"),
                                    "quran_mode_mushaf"
                                ),
                                Triple(
                                    QuranDisplayMode.STUDY,
                                    L10n.pick(lang, "التفسير", "Tafsir"),
                                    "quran_mode_tafsir"
                                ),
                                Triple(
                                    QuranDisplayMode.MEMORIZE,
                                    L10n.pick(lang, "التحفيظ", "Memorize"),
                                    "quran_mode_memorize"
                                )
                            )
                            modes.forEach { (mode, label, tag) ->
                                val isSelected = prefs.quranDisplayMode == mode
                                Surface(
                                    onClick = {
                                        if (mode == QuranDisplayMode.MUSHAF) {
                                            useMushafPageImages = true
                                        }
                                        viewModel.setQuranDisplayMode(mode)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) EmeraldPrimary else noorColors.glassSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) GoldBright else noorColors.goldAccent.copy(alpha = 0.28f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 38.dp)
                                        .testTag(tag)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else primaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Row 2: Slim Single Action Strip (Play Sheikh | Reciter | Options)
                        val isSheikhPlaying = audioState.hasActiveSession &&
                            (audioState.playingSurah == surahNumber || isMushafImgView)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isSheikhPlaying) {
                                        viewModel.toggleAudioPauseResume()
                                    } else {
                                        val startSurah = if (isMushafImgView) activePageMeta.primarySurahNumber else surahNumber
                                        viewModel.playFromMushafAyah(
                                            surahNumber = startSurah,
                                            startAyah = if (startSurah == surahNumber) lastVisibleAyah.coerceAtLeast(1) else 1
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .testTag("play_and_follow_audio_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSheikhPlaying && audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSheikhPlaying && audioState.isPlaying) {
                                        L10n.pick(lang, "إيقاف", "Pause")
                                    } else {
                                        L10n.pick(lang, "استماع", "Listen")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            ChooseReciterButton(
                                selectedReciter = prefs.quranReciter,
                                onSelectReciter = { viewModel.setQuranReciter(it) },
                                customReciters = customReciters,
                                selectedCustomReciterId = selectedCustomReciterId,
                                onSelectCustomReciter = { custom ->
                                    viewModel.selectMushafCustomReciter(custom)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { showMoreReaderOptions = !showMoreReaderOptions },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (showMoreReaderOptions) EmeraldPrimary.copy(alpha = 0.25f) else noorColors.glassSurface)
                                    .border(
                                        0.8.dp,
                                        if (showMoreReaderOptions) GoldBright else noorColors.goldAccent.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .testTag("reader_more_options_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = L10n.pick(lang, "خيارات إضافية", "More Options"),
                                    tint = if (showMoreReaderOptions) GoldBright else noorColors.goldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Optional Collapsible Extra Options (Theme, Font Size for Tafsir/Hifz, Auto-Scroll, Mic Follow)
                        AnimatedVisibility(visible = showMoreReaderOptions) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HorizontalDivider(color = noorColors.goldAccent.copy(alpha = 0.2f))

                                // Reading Theme Chips + Font Size (in Tafsir / Memorize)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(QuranReadingTheme.entries) { theme ->
                                            val label = when (theme) {
                                                QuranReadingTheme.AUTO -> L10n.pick(lang, "تلقائي", "Auto")
                                                QuranReadingTheme.SEPIA -> L10n.pick(lang, "ورقي", "Sepia")
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
                                    if (!isMushafImgView) {
                                        Row {
                                            IconButton(onClick = { viewModel.setQuranFontSize(prefs.quranFontSizeSp - 2) }) {
                                                Icon(Icons.Rounded.TextDecrease, contentDescription = "Smaller text", tint = noorColors.goldAccent)
                                            }
                                            IconButton(onClick = { viewModel.setQuranFontSize(prefs.quranFontSizeSp + 2) }) {
                                                Icon(Icons.Rounded.TextIncrease, contentDescription = "Larger text", tint = noorColors.goldAccent)
                                            }
                                        }
                                    }
                                }

                                // Auto-Scroll + Live Mic Auto-Follow + Recite Panel
                                val isListening = liveRecitation.isActive
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .testTag("auto_scroll_toggle_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapVert,
                                            contentDescription = null,
                                            tint = if (autoScrollMode == AutoScrollMode.RUNNING) GoldBright else noorColors.goldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = L10n.pick(lang, "تمرير", "Scroll"),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }

                                    LiquidGlassChip(
                                        label = if (isListening) {
                                            L10n.pick(lang, "إيقاف التتبع", "Stop Mic")
                                        } else {
                                            L10n.pick(lang, "تتبع صوتي", "Mic Follow")
                                        },
                                        selected = isListening,
                                        onClick = {
                                            if (isListening || liveRecitation.status == LiveRecitationStatus.PAUSED) {
                                                viewModel.stopLiveRecitationTracking()
                                            } else {
                                                val hasMic = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasMic) {
                                                    if (audioState.isPlaying) viewModel.toggleAudioPauseResume()
                                                    autoScrollMode = AutoScrollMode.STOPPED
                                                    viewModel.startLiveRecitationTracking()
                                                } else {
                                                    showMicPermissionDialog = true
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("listen_and_follow_button")
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            if (ecosystemViewModel != null) {
                                                if (!showRecitationPanel) {
                                                    if (audioState.isPlaying) viewModel.toggleAudioPauseResume()
                                                    recitationTargetAyah = lastVisibleAyah.coerceAtLeast(1)
                                                }
                                                showRecitationPanel = !showRecitationPanel
                                            } else {
                                                onOpenInMemorization(surahNumber, lastVisibleAyah.coerceAtLeast(1))
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .testTag("in_reader_recite_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = if (showRecitationPanel) GoldBright else noorColors.goldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = L10n.pick(lang, "تسميع", "Recite"),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (prefs.quranDisplayMode == QuranDisplayMode.STUDY || prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE) {
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
                    }
                }

                HorizontalDivider(color = noorColors.goldAccent.copy(alpha = 0.2f))
            }

            // Sticky Sheikh Audio Follow-Along Bar (real verse-by-verse sync across same or flowed Surah!)
            val bannerSurahNum = audioState.playingSurah ?: surahNumber
            val bannerAyahNum = if (audioState.hasActiveSession) audioState.playingAyah else null
            if (bannerAyahNum != null && (bannerSurahNum == surahNumber || (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode))) {
                val bannerSurahMeta = remember(bannerSurahNum) { SurahCatalog.findSurah(bannerSurahNum) }
                val readyVerses = (readerState as? SurahReaderState.Ready)?.verses.orEmpty()
                SheikhAudioFollowBanner(
                    surahNameAr = bannerSurahMeta.nameAr,
                    surahNameEn = bannerSurahMeta.nameEn,
                    totalAyahs = bannerSurahMeta.verseCount,
                    activeAyah = bannerAyahNum,
                    audioState = audioState,
                    onTogglePauseResume = { viewModel.toggleAudioPauseResume() },
                    onPreviousAyah = { viewModel.playPreviousAudio() },
                    onNextAyah = { viewModel.playNextAudio() },
                    onScrubToAyah = { targetAyah ->
                        if (bannerSurahNum == surahNumber && readyVerses.isNotEmpty()) {
                            viewModel.jumpToQuranAudioAyah(surahNumber, targetAyah, readyVerses)
                        } else {
                            viewModel.playFromMushafAyah(bannerSurahNum, targetAyah)
                        }
                    },
                    onCycleRepeat = { viewModel.cycleAudioRepeatMode() },
                    onCycleSpeed = {
                        val nextSpd = when {
                            audioState.playbackSpeed < 0.95f -> 1.0f
                            audioState.playbackSpeed < 1.2f -> 1.25f
                            audioState.playbackSpeed < 1.45f -> 1.5f
                            else -> 0.75f
                        }
                        viewModel.setAudioPlaybackSpeed(nextSpd)
                    },
                    onStop = { viewModel.stopQuranAudio() }
                )
            }

            // Sticky Live Recitation Tracking Banner (always visible while Live Mic Follow is active or paused!)
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
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isFullScreenActive,
            contentPadding = if (isFullScreenActive) {
                PaddingValues(0.dp)
            } else {
                PaddingValues(
                    start = if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode) 8.dp else 16.dp,
                    end = if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode) 8.dp else 16.dp,
                    top = 4.dp,
                    bottom = 120.dp
                )
            },
            verticalArrangement = Arrangement.spacedBy(if (isFullScreenActive) 0.dp else 10.dp)
        ) {

            // Integrated In-Reader "My Recitation & Memorization" Panel (real Mic Recording + Arabic STT + Word Evaluation)
            val readyForRecitation = readerState as? SurahReaderState.Ready
            if (!isFullScreenActive &&
                ecosystemViewModel != null &&
                readyForRecitation != null &&
                readyForRecitation.surahNumber == surahNumber &&
                (showRecitationPanel || prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE)
            ) {
                item(key = "in_reader_recitation_panel") {
                    InReaderRecitationPanel(
                        surahNumber = surahNumber,
                        verses = readyForRecitation.verses,
                        initialAyah = recitationTargetAyah.coerceIn(1, readyForRecitation.verses.size.coerceAtLeast(1)),
                        ecosystemViewModel = ecosystemViewModel,
                        onOpenFullHifzStudio = { sNum, aNum -> onOpenInMemorization(sNum, aNum) },
                        onClose = {
                            showRecitationPanel = false
                            if (prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE) {
                                viewModel.setQuranDisplayMode(QuranDisplayMode.MUSHAF)
                            }
                        }
                    )
                }
            }

            // Bismillah Header (except At-Tawbah 9, Al-Fatiha 1, and Printed Madinah Mushaf Image Mode which already prints Bismillah)
            if (surahNumber != 1 && surahNumber != 9 && !(prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode)) {
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
                    if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF && effectiveMushafImageMode) {
                        item(key = "madinah_mushaf_image_viewer") {
                            val activeSyncedVerseObj = activeSyncedAyah?.let { ay ->
                                state.verses.firstOrNull { it.numberInSurah == ay }
                            }
                            val activeRecitedPair: Pair<Int, Int>? = when {
                                audioState.hasActiveSession && audioState.playingSurah != null && audioState.playingAyah != null ->
                                    audioState.playingSurah!! to audioState.playingAyah!!
                                (liveRecitation.isActive || liveRecitation.status == LiveRecitationStatus.PAUSED) &&
                                    liveRecitation.trackedSurah != null && liveRecitation.trackedAyah != null ->
                                    liveRecitation.trackedSurah!! to liveRecitation.trackedAyah!!
                                else -> null
                            }
                            val syncedPage = audioState.playingPageNumber
                                ?: activeSyncedVerseObj?.pageNumber
                                ?: liveRecitation.trackedPage
                            val initialPageForSurah = if (openAtLastReadPage) {
                                prefs.lastReadPage.coerceIn(1, 604)
                            } else if (prefs.lastReadSurah == surahNumber) {
                                prefs.lastReadPage.coerceIn(1, 604)
                            } else {
                                state.verses
                                    .firstOrNull { it.numberInSurah == lastVisibleAyah }
                                    ?.pageNumber
                                    ?: surahMeta.startPage
                            }
                            val isDarkReader = prefs.quranReadingTheme == QuranReadingTheme.NIGHT ||
                                (prefs.quranReadingTheme == QuranReadingTheme.AUTO && noorColors.isDark)
                            val bookmarkedPairs = remember(bookmarks) {
                                bookmarks.map { it.surahNumber to it.ayahNumber }.toSet()
                            }

                            MadinahMushafReaderView(
                                imageManager = viewModel.mushafPageImageManager,
                                coordinateRepository = viewModel.mushafCoordinateRepository,
                                linguisticRepository = viewModel.quranLinguisticRepository,
                                initialPage = initialPageForSurah,
                                activeSyncedPage = syncedPage,
                                activeSyncedVerse = activeSyncedVerseObj,
                                activeRecitedAyah = activeRecitedPair,
                                activeWordPosition = activeWordPosition,
                                hasVerifiedWordTiming = hasVerifiedWordTimings,
                                isAudioPlaying = audioState.isPlaying,
                                isNightTheme = isDarkReader,
                                isFullScreen = isFullScreenActive,
                                isToolbarCollapsed = isTopControlsCollapsed,
                                jumpToPageRequest = jumpToPageRequest,
                                onToggleFullScreen = { isMushafFullScreen = !isMushafFullScreen },
                                highlightStyle = highlightStyle,
                                bookmarkedKeys = bookmarkedPairs,
                                loadPageVerses = { pageNum -> viewModel.loadVersesForMushafPage(pageNum) },
                                onPageChanged = { pageNum, primarySurah, isUserNavigation ->
                                    activeMushafPage = pageNum
                                    if (isUserNavigation) {
                                        val firstAyahOnPage = state.verses
                                            .firstOrNull { it.pageNumber == pageNum }
                                            ?.numberInSurah ?: 1
                                        lastVisibleAyah = firstAyahOnPage
                                        userHasInteractedWithSurah = true
                                        viewModel.saveLastRead(primarySurah, firstAyahOnPage, pageNumber = pageNum)
                                    }
                                },
                                onToggleAudio = {
                                    if (audioState.hasActiveSession) {
                                        viewModel.toggleAudioPauseResume()
                                    } else if (state.verses.isNotEmpty()) {
                                        viewModel.playFromMushafAyah(
                                            surahNumber = surahNumber,
                                            startAyah = lastVisibleAyah.coerceAtLeast(1)
                                        )
                                    }
                                },
                                onUpdateHighlightStyle = { viewModel.updateMushafHighlightStyle(it) },
                                onReadFromAyah = { sNum, aNum ->
                                    lastVisibleAyah = aNum
                                    userHasInteractedWithSurah = true
                                    viewModel.saveLastRead(sNum, aNum, pageNumber = activeMushafPage)
                                    viewModel.playFromMushafAyah(surahNumber = sNum, startAyah = aNum)
                                },
                                onPlaySingleAyah = { sNum, aNum, pageNum ->
                                    lastVisibleAyah = aNum
                                    userHasInteractedWithSurah = true
                                    viewModel.saveLastRead(sNum, aNum, pageNumber = pageNum)
                                    viewModel.playSingleMushafAyah(sNum, aNum, pageNum)
                                },
                                onRepeatAyahRange = { sNum, fromA, toA ->
                                    lastVisibleAyah = fromA
                                    userHasInteractedWithSurah = true
                                    viewModel.saveLastRead(sNum, fromA, pageNumber = activeMushafPage)
                                    viewModel.playFromMushafAyah(
                                        surahNumber = sNum,
                                        startAyah = fromA,
                                        endAyah = toA,
                                        repeatRange = true
                                    )
                                },
                                onToggleBookmarkAyah = { sNum, aNum, text ->
                                    viewModel.toggleBookmark(sNum, aNum, text)
                                },
                                onAddToMemorization = { sNum, aNum ->
                                    viewModel.addAyahToMemorizationPlan(sNum, aNum)
                                },
                                onOpenInMemorization = { sNum, aNum ->
                                    if (ecosystemViewModel != null && sNum == surahNumber) {
                                        if (audioState.isPlaying) viewModel.toggleAudioPauseResume()
                                        isMushafFullScreen = false
                                        recitationTargetAyah = aNum
                                        showRecitationPanel = true
                                    } else {
                                        onOpenInMemorization(sNum, aNum)
                                    }
                                },
                                onStatusBanner = { msg -> viewModel.showBannerMessage(msg) },
                                onSwitchToTextMushaf = {
                                    isMushafFullScreen = false
                                    useMushafPageImages = false
                                },
                                modifier = if (isFullScreenActive) Modifier.fillParentMaxSize() else Modifier.fillMaxWidth()
                            )
                        }
                    } else if (prefs.quranDisplayMode == QuranDisplayMode.MUSHAF) {
                        // Continuous Mushaf Page Flow with LIVE inline gold Ayah highlighting (Sheikh audio + Live Mic!)
                        val groupedByPage = state.verses.groupBy { it.pageNumber }
                        val mushafPageBg = when (prefs.quranReadingTheme) {
                            QuranReadingTheme.SEPIA -> IvorySurface
                            QuranReadingTheme.NIGHT -> Color(0xFF0A1B15)
                            QuranReadingTheme.LIGHT -> Color(0xFFFFFFFF)
                            QuranReadingTheme.AUTO -> autoMushafPageBg
                        }
                        items(groupedByPage.entries.toList(), key = { "page_${it.key}" }) { (pageNum, pageVerses) ->
                            val hasTrackedOnPage = activeSyncedAyah != null &&
                                pageVerses.any { it.numberInSurah == activeSyncedAyah }

                            val annotatedPage = remember(pageVerses, activeSyncedAyah, primaryInk) {
                                buildAnnotatedString {
                                    pageVerses.forEachIndexed { idx, v ->
                                        val isTracked = activeSyncedAyah == v.numberInSurah
                                        if (isTracked) {
                                            withStyle(
                                                SpanStyle(
                                                    background = GoldPrimary.copy(alpha = 0.30f),
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
                                    .background(mushafPageBg)
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
                                                if (hasTrackedOnPage) {
                                                    if (activeAudioAyah != null) " · متابعة التلاوة: آية $activeSyncedAyah"
                                                    else " · تتبع مباشر: آية $activeSyncedAyah"
                                                } else "",
                                            "Page $pageNum · Juz ${pageVerses.first().juzNumber}" +
                                                if (hasTrackedOnPage) {
                                                    if (activeAudioAyah != null) " · Audio Follow: Ayah $activeSyncedAyah"
                                                    else " · Live Sync: Ayah $activeSyncedAyah"
                                                } else ""
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
                            val isPlayingVerse = audioState.hasActiveSession &&
                                audioState.playingSurah == verse.surahNumber &&
                                audioState.playingAyah == verse.numberInSurah
                            val isLiveTracked = activeSyncedAyah == verse.numberInSurah
                            val isMemorizeMode = prefs.quranDisplayMode == QuranDisplayMode.MEMORIZE
                            val isRevealed = !isMemorizeMode || (revealedInMemorize[verse.numberInSurah] == true) || isLiveTracked

                            VerseCard(
                                verse = verse,
                                fontSizeSp = prefs.quranFontSizeSp,
                                primaryInk = primaryInk,
                                secondaryInk = secondaryInk,
                                isBookmarked = isBookmarked,
                                isPlaying = isPlayingVerse && audioState.isPlaying,
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
private fun SheikhAudioFollowBanner(
    surahNameAr: String,
    surahNameEn: String,
    totalAyahs: Int,
    activeAyah: Int,
    audioState: AudioPlaybackState,
    onTogglePauseResume: () -> Unit,
    onPreviousAyah: () -> Unit,
    onNextAyah: () -> Unit,
    onScrubToAyah: (Int) -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSpeed: () -> Unit,
    onStop: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var sliderAyah by remember(activeAyah) { mutableStateOf(activeAyah.toFloat()) }

    LiquidGlassCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("sheikh_audio_follow_banner"),
        goldBorder = audioState.isPlaying,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L10n.pick(
                        lang,
                        "متابعة التلاوة · سورة $surahNameAr · آية $activeAyah / $totalAyahs",
                        "Audio Follow · $surahNameEn · Ayah $activeAyah / $totalAyahs"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = L10n.pick(lang, audioState.subtitleAr, audioState.subtitleEn)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = if (audioState.isBuffering) {
                            L10n.pick(lang, "$subtitle · جارٍ التحميل...", "$subtitle · Buffering...")
                        } else {
                            subtitle
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onPreviousAyah,
                enabled = activeAyah > 1,
                modifier = Modifier.size(36.dp).testTag("audio_follow_prev_btn")
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous Ayah", tint = noorColors.goldAccent)
            }
            IconButton(
                onClick = onTogglePauseResume,
                modifier = Modifier.size(38.dp).testTag("audio_follow_play_pause_btn")
            ) {
                Icon(
                    imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play or Pause",
                    tint = GoldBright
                )
            }
            IconButton(
                onClick = onNextAyah,
                enabled = activeAyah < totalAyahs,
                modifier = Modifier.size(36.dp).testTag("audio_follow_next_btn")
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next Ayah", tint = noorColors.goldAccent)
            }
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier.size(34.dp).testTag("audio_follow_repeat_btn")
            ) {
                Icon(
                    imageVector = if (audioState.repeatMode == AudioRepeatMode.REPEAT_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat Mode",
                    tint = if (audioState.repeatMode != AudioRepeatMode.OFF) GoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onCycleSpeed,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.testTag("audio_follow_speed_btn")
            ) {
                Text("${audioState.playbackSpeed}x", style = MaterialTheme.typography.labelSmall, color = noorColors.goldAccent)
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(34.dp).testTag("audio_follow_stop_btn")
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = "Stop Audio", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (totalAyahs > 1) {
            Slider(
                value = sliderAyah.coerceIn(1f, totalAyahs.toFloat()),
                onValueChange = { sliderAyah = it },
                onValueChangeFinished = {
                    val target = sliderAyah.toInt().coerceIn(1, totalAyahs)
                    if (target != activeAyah) {
                        onScrubToAyah(target)
                    }
                },
                valueRange = 1f..totalAyahs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = GoldBright,
                    activeTrackColor = GoldPrimary,
                    inactiveTrackColor = GoldPrimary.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("audio_follow_ayah_slider")
            )
        }
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
