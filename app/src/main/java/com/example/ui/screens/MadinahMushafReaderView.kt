package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ZoomIn
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.data.quran.AyahLinguisticAnalysis
import com.example.data.quran.AyahPageRegion
import com.example.data.quran.MushafCoordinateRepository
import com.example.data.quran.MushafCoordinateTransform
import com.example.data.quran.MushafDownloadProgress
import com.example.data.quran.MushafHighlightStyle
import com.example.data.quran.MushafPageImageManager
import com.example.data.quran.MushafPageLayout
import com.example.data.quran.MushafPageLoadResult
import com.example.data.quran.QuranLinguisticRepository
import com.example.domain.model.QuranVerse
import com.example.ui.components.LiquidGlassCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticSuccess
import kotlinx.coroutines.launch

private val MushafPaperBg = Color(0xFFFCF8EE)
private val MushafBorderGold = Color(0xFFC9A24B)
private val MushafBorderEmerald = Color(0xFF1A5A46)
private val MushafNightPaper = Color(0xFF0A1411)

// Gentle inverted-sepia matrix for reading printed Mushaf page images comfortably at night
private val MushafNightColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -0.88f, 0f, 0f, 0f, 238f,
            0f, -0.88f, 0f, 0f, 235f,
            0f, 0f, -0.85f, 0f, 224f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/**
 * Interactive 604-Page Madinah Mushaf Page-Image Reader.
 *
 * Preserves authentic untouched printed Madinah Mushaf page images while layering:
 * - Accurate Ayah-level & word-level coordinate hit-testing (RTL, multi-Surah, Pages 1-2, Sajdah, zoom/pan aware).
 * - Long-press Ayah selection with haptic feedback and a non-overlapping RTL contextual action menu.
 * - Real-time transparent overlay highlighting for active recited Ayahs and verified word timings.
 * - Structured I'rab (إعراب الآية), Sarf (الصرف), and Tafsir bottom sheet.
 * - Continuous "Read from this Ayah" playback, Repeat Ayah Range, Copy, Share, Bookmark, and Hifz integration.
 */
@Composable
fun MadinahMushafReaderView(
    imageManager: MushafPageImageManager,
    coordinateRepository: MushafCoordinateRepository,
    linguisticRepository: QuranLinguisticRepository,
    initialPage: Int,
    activeSyncedPage: Int?,
    activeSyncedVerse: QuranVerse?,
    activeRecitedAyah: Pair<Int, Int>?,
    activeWordPosition: Int?,
    hasVerifiedWordTiming: Boolean,
    isAudioPlaying: Boolean,
    isNightTheme: Boolean,
    highlightStyle: MushafHighlightStyle,
    bookmarkedKeys: Set<Pair<Int, Int>>,
    loadPageVerses: suspend (Int) -> List<QuranVerse>,
    onPageChanged: (pageNumber: Int, primarySurah: Int) -> Unit,
    onToggleAudio: () -> Unit,
    onUpdateHighlightStyle: (MushafHighlightStyle) -> Unit,
    onReadFromAyah: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onPlaySingleAyah: (surahNumber: Int, ayahNumber: Int, pageNumber: Int) -> Unit,
    onRepeatAyahRange: (surahNumber: Int, fromAyah: Int, toAyah: Int) -> Unit,
    onToggleBookmarkAyah: (surahNumber: Int, ayahNumber: Int, textUthmani: String) -> Unit,
    onAddToMemorization: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onOpenInMemorization: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onStatusBanner: (String) -> Unit,
    onSwitchToTextMushaf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()
    val downloadProgress by imageManager.progress.collectAsStateWithLifecycle()
    val coordPackStatus by coordinateRepository.packStatus.collectAsStateWithLifecycle()

    val safeInitialIndex = (initialPage.coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES) - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { MadinahMushafPageCatalog.TOTAL_PAGES }
    )

    var nightInvertEnabled by remember(isNightTheme) { mutableStateOf(isNightTheme) }
    var showOfflineManagerDialog by remember { mutableStateOf(false) }
    var showHighlightSettingsDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var coordReloadNonce by remember { mutableIntStateOf(0) }

    // Interactive Ayah Selection & Contextual Action Menu State (single active selection at a time)
    var selectedAyah by remember { mutableStateOf<AyahPageRegion?>(null) }
    var showContextualMenu by remember { mutableStateOf(false) }
    var tappedInfoAyah by remember { mutableStateOf<AyahPageRegion?>(null) }
    var currentPageVerses by remember { mutableStateOf<List<QuranVerse>>(emptyList()) }
    var currentPageLayout by remember { mutableStateOf<MushafPageLayout?>(null) }

    // Linguistic Analysis Sheet (I'rab / Sarf / Tafsir) State
    var activeLinguisticAnalysis by remember { mutableStateOf<AyahLinguisticAnalysis?>(null) }
    var activeLinguisticTab by remember { mutableStateOf(LinguisticSheetTab.IRAB) }

    // Repeat Ayah Range Dialog State
    var repeatRangeTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val zipImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { imageManager.importOfflineZipPack(uri) }
        }
    }

    val glyphDbImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = coordinateRepository.importGlyphDatabaseFromUri(uri)
                if (ok) {
                    coordReloadNonce++
                    onStatusBanner(L10n.pick(lang, "تم تفعيل قاعدة إحداثيات المصحف الدقيقة بنجاح", "Exact glyph coordinates DB activated"))
                } else {
                    onStatusBanner(L10n.pick(lang, "الملف غير صالح كقاعدة إحداثيات للمصحف", "Invalid glyph coordinates file"))
                }
            }
        }
    }

    val linguisticPackImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val count = linguisticRepository.importOfflineLinguisticPack(uri)
                if (count > 0) {
                    onStatusBanner(
                        L10n.pick(
                            lang,
                            "تم استيراد $count سجل إعراب وصرف موثّق للعمل دون إنترنت",
                            "Imported $count offline linguistic records"
                        )
                    )
                    // Refresh currently open linguistic sheet if any
                    activeLinguisticAnalysis?.let { current ->
                        activeLinguisticAnalysis = linguisticRepository.getAyahLinguisticAnalysis(
                            surahNumber = current.surahNumber,
                            ayahNumber = current.ayahNumber,
                            exactImmutableUthmaniText = current.exactUthmaniAyahText,
                            cachedTafsirAr = current.tafsirMuyassarAr
                        )
                    }
                } else {
                    onStatusBanner(L10n.pick(lang, "تعذّر قراءة حزمة الإعراب والصرف (JSON)", "Unable to parse linguistic JSON pack"))
                }
            }
        }
    }

    // Automatically turn to the exact Madinah Mushaf page when Sheikh recitation or Live Tracking changes page!
    LaunchedEffect(activeSyncedPage) {
        val targetPage = activeSyncedPage?.coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES) ?: return@LaunchedEffect
        val targetIndex = targetPage - 1
        if (pagerState.currentPage != targetIndex) {
            runCatching { pagerState.animateScrollToPage(targetIndex) }
        }
    }

    val currentPageNumber = pagerState.currentPage + 1
    val currentMeta = remember(currentPageNumber) { MadinahMushafPageCatalog.pageMeta(currentPageNumber) }

    // Clear selection, reset zoom, prefetch images, and preload all Surah verses on the visible page
    LaunchedEffect(currentPageNumber, coordReloadNonce) {
        zoomScale = 1f
        zoomOffset = Offset.Zero
        selectedAyah = null
        showContextualMenu = false
        tappedInfoAyah = null
        onPageChanged(currentPageNumber, currentMeta.primarySurahNumber)
        imageManager.prefetchNeighborPages(currentPageNumber)
        val verses = loadPageVerses(currentPageNumber)
        currentPageVerses = verses
        currentPageLayout = coordinateRepository.getPageLayout(currentPageNumber, verses)
    }

    fun resolveVerseFor(surahNumber: Int, ayahNumber: Int): QuranVerse? =
        currentPageVerses.firstOrNull { it.surahNumber == surahNumber && it.numberInSurah == ayahNumber }

    fun openLinguisticSheet(ayah: AyahPageRegion, tab: LinguisticSheetTab) {
        val verse = resolveVerseFor(ayah.surahNumber, ayah.ayahNumber)
        val immutableText = verse?.textUthmani.orEmpty()
        val tafsir = verse?.tafsirAr.orEmpty()
        showContextualMenu = false
        scope.launch {
            activeLinguisticTab = tab
            activeLinguisticAnalysis = linguisticRepository.getAyahLinguisticAnalysis(
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber,
                exactImmutableUthmaniText = immutableText,
                cachedTafsirAr = tafsir
            )
        }
    }

    fun copyAyahToClipboard(ayah: AyahPageRegion) {
        val verse = resolveVerseFor(ayah.surahNumber, ayah.ayahNumber)
        val surah = SurahCatalog.findSurah(ayah.surahNumber)
        val text = if (!verse?.textUthmani.isNullOrBlank()) {
            "﴿${verse!!.textUthmani}﴾ [${surah.nameAr}: ${ayah.ayahNumber}]"
        } else {
            "سورة ${surah.nameAr} — آية ${ayah.ayahNumber} (صفحة ${ayah.pageNumber})"
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Quran Ayah", text))
        onStatusBanner(L10n.pick(lang, "تم نسخ الآية بنجاح", "Ayah copied to clipboard"))
        showContextualMenu = false
    }

    fun shareAyahIntent(ayah: AyahPageRegion) {
        val verse = resolveVerseFor(ayah.surahNumber, ayah.ayahNumber)
        val surah = SurahCatalog.findSurah(ayah.surahNumber)
        val shareText = if (!verse?.textUthmani.isNullOrBlank()) {
            "﴿${verse!!.textUthmani}﴾\n— سورة ${surah.nameAr}، آية ${ayah.ayahNumber} (ص ${ayah.pageNumber})\nعبر تطبيق نور"
        } else {
            "سورة ${surah.nameAr} — آية ${ayah.ayahNumber} (ص ${ayah.pageNumber})"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, L10n.pick(lang, "مشاركة الآية", "Share Ayah")))
        }
        showContextualMenu = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("madinah_mushaf_image_reader"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Mushaf Toolbar: Page/Juz/Surah Info + Highlight Settings + Jump + Night Filter + Offline Manager
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (nightInvertEnabled) Color(0xFF0E211A) else Color(0xFFF3EAD5))
                .border(1.dp, MushafBorderGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L10n.pick(
                        lang,
                        "سورة ${currentMeta.primarySurahNameAr} · الجزء ${currentMeta.juzNumber} · الحزب ${currentMeta.hizbNumber}",
                        "Surah ${currentMeta.primarySurahNameEn} · Juz ${currentMeta.juzNumber} · Hizb ${currentMeta.hizbNumber}"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (nightInvertEnabled) GoldBright else EmeraldDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = L10n.pick(
                        lang,
                        "صفحة $currentPageNumber من ٦٠٤ · اضغط مطوّلاً على أي آية للتفاعل",
                        "Page $currentPageNumber / 604 · Long-press any Ayah for actions"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (nightInvertEnabled) Color(0xFFB5CEC4) else Color(0xFF5D5242),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showHighlightSettingsDialog = true },
                    modifier = Modifier.testTag("mushaf_highlight_settings_btn")
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = L10n.pick(lang, "إعدادات التظليل", "Highlight Settings"),
                        tint = highlightStyle.colorPreset.ayahFillColor
                    )
                }
                IconButton(
                    onClick = { showJumpDialog = true },
                    modifier = Modifier.testTag("mushaf_jump_page_btn")
                ) {
                    Icon(
                        Icons.Rounded.MenuBook,
                        contentDescription = "Jump to page",
                        tint = if (nightInvertEnabled) GoldBright else EmeraldDeep
                    )
                }
                IconButton(
                    onClick = { nightInvertEnabled = !nightInvertEnabled },
                    modifier = Modifier.testTag("mushaf_night_filter_btn")
                ) {
                    Icon(
                        Icons.Rounded.DarkMode,
                        contentDescription = "Toggle Mushaf Night Filter",
                        tint = if (nightInvertEnabled) GoldBright else EmeraldDeep
                    )
                }
                IconButton(
                    onClick = { showOfflineManagerDialog = true },
                    modifier = Modifier.testTag("mushaf_offline_manager_btn")
                ) {
                    Icon(
                        Icons.Rounded.CloudDownload,
                        contentDescription = "Offline Mushaf Manager",
                        tint = if (downloadProgress.cachedPagesCount >= 604) SemanticSuccess else GoldPrimary
                    )
                }
            }
        }

        // Contextual Menu Placement ABOVE page when selected Ayah is in mid/lower page (never covers selected Ayah!)
        val activeSelected = selectedAyah
        val menuPlacement = remember(activeSelected) {
            activeSelected?.let { MushafCoordinateTransform.computeContextualMenuPlacement(it) }
        }
        if (showContextualMenu && activeSelected != null && menuPlacement?.placeAboveAyah == true) {
            val selectedVerse = resolveVerseFor(activeSelected.surahNumber, activeSelected.ayahNumber)
            MushafAyahContextualMenu(
                selectedAyah = activeSelected,
                ayahTextUthmani = selectedVerse?.textUthmani.orEmpty(),
                isBookmarked = bookmarkedKeys.contains(activeSelected.surahNumber to activeSelected.ayahNumber),
                placeAboveAyah = true,
                nightMode = nightInvertEnabled,
                onReadFromHere = {
                    showContextualMenu = false
                    onReadFromAyah(activeSelected.surahNumber, activeSelected.ayahNumber)
                },
                onOpenIrab = { openLinguisticSheet(activeSelected, LinguisticSheetTab.IRAB) },
                onOpenSarf = { openLinguisticSheet(activeSelected, LinguisticSheetTab.SARF) },
                onPlaySingleAyah = {
                    showContextualMenu = false
                    onPlaySingleAyah(activeSelected.surahNumber, activeSelected.ayahNumber, activeSelected.pageNumber)
                },
                onRepeatRange = {
                    showContextualMenu = false
                    repeatRangeTarget = activeSelected.surahNumber to activeSelected.ayahNumber
                },
                onCopyAyah = { copyAyahToClipboard(activeSelected) },
                onShareAyah = { shareAyahIntent(activeSelected) },
                onToggleBookmark = {
                    onToggleBookmarkAyah(
                        activeSelected.surahNumber,
                        activeSelected.ayahNumber,
                        selectedVerse?.textUthmani.orEmpty()
                    )
                },
                onAddToMemorization = {
                    showContextualMenu = false
                    onAddToMemorization(activeSelected.surahNumber, activeSelected.ayahNumber)
                },
                onOpenInMemorization = {
                    showContextualMenu = false
                    onOpenInMemorization(activeSelected.surahNumber, activeSelected.ayahNumber)
                },
                onDismiss = {
                    showContextualMenu = false
                    selectedAyah = null
                }
            )
        }

        // Authentic Printed Madinah Mushaf 604-Page Interactive Swipeable Viewer
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = zoomScale <= 1.05f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("madinah_mushaf_pager")
        ) { pageIndex ->
            val pageNum = pageIndex + 1
            val meta = remember(pageNum) { MadinahMushafPageCatalog.pageMeta(pageNum) }
            val isCurrentPage = pagerState.currentPage == pageIndex

            MadinahMushafSinglePageCard(
                pageNumber = pageNum,
                surahNameAr = meta.primarySurahNameAr,
                juzNumber = meta.juzNumber,
                isRightPage = meta.isRightPage,
                imageManager = imageManager,
                coordinateRepository = coordinateRepository,
                coordReloadNonce = coordReloadNonce,
                preloadedLayout = if (isCurrentPage) currentPageLayout else null,
                loadPageVerses = loadPageVerses,
                activeRecitedAyah = activeRecitedAyah,
                activeWordPosition = activeWordPosition,
                selectedAyah = if (isCurrentPage) selectedAyah else null,
                highlightStyle = highlightStyle,
                nightInvertEnabled = nightInvertEnabled,
                zoomScale = if (isCurrentPage) zoomScale else 1f,
                zoomOffset = if (isCurrentPage) zoomOffset else Offset.Zero,
                onZoomChange = { newScale, newOffset ->
                    zoomScale = newScale
                    zoomOffset = newOffset
                },
                onAyahTapped = { hit ->
                    if (showContextualMenu) {
                        // Dismiss contextual menu cleanly on outside tap; switch selection if another Ayah tapped
                        if (hit == null || hit.key == selectedAyah?.key) {
                            showContextualMenu = false
                            selectedAyah = null
                        } else {
                            selectedAyah = hit
                            tappedInfoAyah = hit
                        }
                    } else {
                        tappedInfoAyah = hit
                        selectedAyah = hit
                    }
                },
                onAyahLongPressed = { hit ->
                    selectedAyah = hit
                    tappedInfoAyah = hit
                    showContextualMenu = true
                },
                onSwitchToTextMushaf = onSwitchToTextMushaf
            )
        }

        // Contextual Menu Placement BELOW page when selected Ayah is near top of page (auto-flipped, never covers Ayah!)
        if (showContextualMenu && activeSelected != null && menuPlacement?.placeAboveAyah == false) {
            val selectedVerse = resolveVerseFor(activeSelected.surahNumber, activeSelected.ayahNumber)
            MushafAyahContextualMenu(
                selectedAyah = activeSelected,
                ayahTextUthmani = selectedVerse?.textUthmani.orEmpty(),
                isBookmarked = bookmarkedKeys.contains(activeSelected.surahNumber to activeSelected.ayahNumber),
                placeAboveAyah = false,
                nightMode = nightInvertEnabled,
                onReadFromHere = {
                    showContextualMenu = false
                    onReadFromAyah(activeSelected.surahNumber, activeSelected.ayahNumber)
                },
                onOpenIrab = { openLinguisticSheet(activeSelected, LinguisticSheetTab.IRAB) },
                onOpenSarf = { openLinguisticSheet(activeSelected, LinguisticSheetTab.SARF) },
                onPlaySingleAyah = {
                    showContextualMenu = false
                    onPlaySingleAyah(activeSelected.surahNumber, activeSelected.ayahNumber, activeSelected.pageNumber)
                },
                onRepeatRange = {
                    showContextualMenu = false
                    repeatRangeTarget = activeSelected.surahNumber to activeSelected.ayahNumber
                },
                onCopyAyah = { copyAyahToClipboard(activeSelected) },
                onShareAyah = { shareAyahIntent(activeSelected) },
                onToggleBookmark = {
                    onToggleBookmarkAyah(
                        activeSelected.surahNumber,
                        activeSelected.ayahNumber,
                        selectedVerse?.textUthmani.orEmpty()
                    )
                },
                onAddToMemorization = {
                    showContextualMenu = false
                    onAddToMemorization(activeSelected.surahNumber, activeSelected.ayahNumber)
                },
                onOpenInMemorization = {
                    showContextualMenu = false
                    onOpenInMemorization(activeSelected.surahNumber, activeSelected.ayahNumber)
                },
                onDismiss = {
                    showContextualMenu = false
                    selectedAyah = null
                }
            )
        }

        // Quick Tapped Ayah Identification Strip (appears on single tap; 1 tap opens full action menu)
        if (!showContextualMenu && tappedInfoAyah != null) {
            val tapped = tappedInfoAyah!!
            val tappedSurah = remember(tapped.surahNumber) { SurahCatalog.findSurah(tapped.surahNumber) }
            val tappedVerse = resolveVerseFor(tapped.surahNumber, tapped.ayahNumber)
            LiquidGlassCard(
                goldBorder = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("mushaf_tapped_ayah_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "الآية المحددة: سورة ${tappedSurah.nameAr} · آية ${tapped.ayahNumber} (ص ${tapped.pageNumber})",
                                "Selected: ${tappedSurah.nameEn} · Ayah ${tapped.ayahNumber} (p.${tapped.pageNumber})"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = noorColors.goldAccent
                        )
                        if (!tappedVerse?.textUthmani.isNullOrBlank()) {
                            Text(
                                text = "${tappedVerse!!.textUthmani} ﴿${tapped.ayahNumber}﴾",
                                fontFamily = AmiriFontFamily,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TextButton(
                        onClick = { showContextualMenu = true },
                        modifier = Modifier.testTag("mushaf_open_actions_btn")
                    ) {
                        Icon(Icons.Rounded.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text(L10n.pick(lang, "خيارات الآية", "Actions"))
                    }
                }
            }
        }

        // Bottom Page Turn Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.testTag("mushaf_prev_page_btn")
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous page")
                Text(L10n.pick(lang, "السابقة", "Prev"))
            }

            TextButton(onClick = { showJumpDialog = true }) {
                Text(
                    text = L10n.pick(lang, "صفحة $currentPageNumber / ٦٠٤", "Page $currentPageNumber / 604"),
                    fontWeight = FontWeight.Bold,
                    color = noorColors.goldAccent
                )
            }

            OutlinedButton(
                onClick = {
                    if (pagerState.currentPage < MadinahMushafPageCatalog.TOTAL_PAGES - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                enabled = pagerState.currentPage < MadinahMushafPageCatalog.TOTAL_PAGES - 1,
                modifier = Modifier.testTag("mushaf_next_page_btn")
            ) {
                Text(L10n.pick(lang, "التالية", "Next"))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next page")
            }
        }

        // Active Synchronized Verse Strip (Sheikh Recitation / Live Follow Highlight on Page Image)
        val displayedActiveVerse = activeSyncedVerse ?: activeRecitedAyah?.let { (s, a) -> resolveVerseFor(s, a) }
        if (displayedActiveVerse != null || activeRecitedAyah != null) {
            val activeSurahNum = displayedActiveVerse?.surahNumber ?: activeRecitedAyah!!.first
            val activeAyahNum = displayedActiveVerse?.numberInSurah ?: activeRecitedAyah!!.second
            val activeSurahMeta = remember(activeSurahNum) { SurahCatalog.findSurah(activeSurahNum) }
            LiquidGlassCard(
                goldBorder = true,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.testTag("mushaf_active_verse_strip")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "التلاوة المتزامنة: سورة ${activeSurahMeta.nameAr} — آية $activeAyahNum" +
                                    if (hasVerifiedWordTiming && activeWordPosition != null) " · كلمة $activeWordPosition" else "",
                                "Synced Recitation: ${activeSurahMeta.nameEn} — Ayah $activeAyahNum" +
                                    if (hasVerifiedWordTiming && activeWordPosition != null) " · Word $activeWordPosition" else ""
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = noorColors.goldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        if (!displayedActiveVerse?.textUthmani.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${displayedActiveVerse!!.textUthmani} ﴿$activeAyahNum﴾",
                                fontFamily = AmiriFontFamily,
                                fontSize = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onToggleAudio) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Toggle Recitation",
                            tint = GoldBright
                        )
                    }
                }
            }
        }
    }

    // Structured RTL Linguistic Analysis Sheet (I'rab, Sarf, Tafsir)
    activeLinguisticAnalysis?.let { analysis ->
        AyahLinguisticAnalysisSheet(
            analysis = analysis,
            initialTab = activeLinguisticTab,
            onImportLinguisticPack = {
                linguisticPackImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onPlayAyah = {
                onPlaySingleAyah(analysis.surahNumber, analysis.ayahNumber, currentPageNumber)
            },
            onDismiss = { activeLinguisticAnalysis = null }
        )
    }

    // Repeat Ayah Range Dialog
    repeatRangeTarget?.let { (sNum, fromAyah) ->
        MushafRepeatRangeDialog(
            surahNumber = sNum,
            initialFromAyah = fromAyah,
            onConfirmRange = { f, t ->
                repeatRangeTarget = null
                onRepeatAyahRange(sNum, f, t)
            },
            onDismiss = { repeatRangeTarget = null }
        )
    }

    // Highlight Style & Exact Glyph DB Coordinate Manager Dialog
    if (showHighlightSettingsDialog) {
        MushafHighlightSettingsDialog(
            style = highlightStyle,
            coordStatus = coordPackStatus,
            currentPagePrecisionAr = currentPageLayout?.precision?.labelAr ?: "جارٍ التحميل...",
            currentPagePrecisionEn = currentPageLayout?.precision?.labelEn ?: "Loading...",
            onUpdateStyle = onUpdateHighlightStyle,
            onDownloadGlyphDb = {
                scope.launch {
                    val ok = coordinateRepository.downloadExactGlyphDatabase()
                    if (ok) coordReloadNonce++
                }
            },
            onImportGlyphDb = {
                glyphDbImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            },
            onDismiss = { showHighlightSettingsDialog = false }
        )
    }

    // Jump to Page / Juz Dialog
    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf(currentPageNumber.toString()) }
        var juzInput by remember { mutableStateOf(currentMeta.juzNumber.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text(L10n.pick(lang, "الانتقال السريع في مصحف المدينة", "Jump in Madinah Mushaf")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter(Char::isDigit).take(3) },
                        label = { Text(L10n.pick(lang, "رقم الصفحة (1 - 604)", "Page Number (1 - 604)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("mushaf_jump_page_input")
                    )
                    OutlinedTextField(
                        value = juzInput,
                        onValueChange = { juzInput = it.filter(Char::isDigit).take(2) },
                        label = { Text(L10n.pick(lang, "أو رقم الجزء (1 - 30)", "Or Juz Number (1 - 30)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val j = juzInput.toIntOrNull()?.coerceIn(1, 30) ?: 1
                                val startP = MadinahMushafPageCatalog.pagesForJuz(j).first
                                scope.launch { pagerState.animateScrollToPage(startP - 1) }
                                showJumpDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(L10n.pick(lang, "انتقال للجزء", "Go to Juz")) }
                        Button(
                            onClick = {
                                val p = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: 1
                                scope.launch { pagerState.animateScrollToPage(p - 1) }
                                showJumpDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(L10n.pick(lang, "انتقال للصفحة", "Go to Page")) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text(L10n.pick(lang, "إغلاق", "Close")) }
            }
        )
    }

    // Offline Mushaf Pages Download & Local ZIP Import Manager
    if (showOfflineManagerDialog) {
        MushafOfflinePackDialog(
            progress = downloadProgress,
            currentSurahNumber = currentMeta.primarySurahNumber,
            currentSurahNameAr = currentMeta.primarySurahNameAr,
            currentJuz = currentMeta.juzNumber,
            onDownloadCurrentSurah = {
                val range = MadinahMushafPageCatalog.pagesForSurah(currentMeta.primarySurahNumber)
                scope.launch {
                    imageManager.downloadPagesRange(
                        range = range,
                        labelAr = "سورة ${currentMeta.primarySurahNameAr}",
                        labelEn = "Surah ${currentMeta.primarySurahNameEn}"
                    )
                }
            },
            onDownloadCurrentJuz = {
                val range = MadinahMushafPageCatalog.pagesForJuz(currentMeta.juzNumber)
                scope.launch {
                    imageManager.downloadPagesRange(
                        range = range,
                        labelAr = "الجزء ${currentMeta.juzNumber}",
                        labelEn = "Juz ${currentMeta.juzNumber}"
                    )
                }
            },
            onDownloadFull604 = {
                scope.launch {
                    imageManager.downloadPagesRange(
                        range = 1..MadinahMushafPageCatalog.TOTAL_PAGES,
                        labelAr = "مصحف المدينة الكامل (٦٠٤ صفحة)",
                        labelEn = "Full Madinah Mushaf (604 pages)"
                    )
                }
            },
            onImportLocalZip = {
                zipImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            },
            onClearCache = {
                scope.launch { imageManager.clearOfflinePagesCache() }
            },
            onDismiss = { showOfflineManagerDialog = false }
        )
    }
}

@Composable
private fun MadinahMushafSinglePageCard(
    pageNumber: Int,
    surahNameAr: String,
    juzNumber: Int,
    isRightPage: Boolean,
    imageManager: MushafPageImageManager,
    coordinateRepository: MushafCoordinateRepository,
    coordReloadNonce: Int,
    preloadedLayout: MushafPageLayout?,
    loadPageVerses: suspend (Int) -> List<QuranVerse>,
    activeRecitedAyah: Pair<Int, Int>?,
    activeWordPosition: Int?,
    selectedAyah: AyahPageRegion?,
    highlightStyle: MushafHighlightStyle,
    nightInvertEnabled: Boolean,
    zoomScale: Float,
    zoomOffset: Offset,
    onZoomChange: (Float, Offset) -> Unit,
    onAyahTapped: (AyahPageRegion?) -> Unit,
    onAyahLongPressed: (AyahPageRegion) -> Unit,
    onSwitchToTextMushaf: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val haptics = LocalHapticFeedback.current
    var reloadNonce by remember(pageNumber) { mutableIntStateOf(0) }
    var loadResult by remember(pageNumber, reloadNonce) { mutableStateOf<MushafPageLoadResult?>(null) }
    var pageLayout by remember(pageNumber, coordReloadNonce) { mutableStateOf(preloadedLayout) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(preloadedLayout) {
        if (preloadedLayout != null && preloadedLayout.pageNumber == pageNumber) {
            pageLayout = preloadedLayout
        }
    }

    LaunchedEffect(pageNumber, reloadNonce) {
        loadResult = imageManager.loadPageBitmap(pageNumber)
    }

    LaunchedEffect(pageNumber, coordReloadNonce) {
        if (pageLayout == null || pageLayout?.pageNumber != pageNumber) {
            val verses = loadPageVerses(pageNumber)
            pageLayout = coordinateRepository.getPageLayout(pageNumber, verses)
        }
    }

    // Keep latest state accessible inside pointerInput gesture coroutines without restarting gesture recognizers
    val latestZoomScale by rememberUpdatedState(zoomScale)
    val latestZoomOffset by rememberUpdatedState(zoomOffset)
    val latestContainerSize by rememberUpdatedState(containerSize)
    val latestLayout by rememberUpdatedState(pageLayout)
    val latestLoadResult by rememberUpdatedState(loadResult)
    val latestOnTap by rememberUpdatedState(onAyahTapped)
    val latestOnLongPress by rememberUpdatedState(onAyahLongPressed)
    val latestOnZoomChange by rememberUpdatedState(onZoomChange)

    fun resolveHitAt(offset: Offset): AyahPageRegion? {
        val layout = latestLayout ?: return null
        val cSize = latestContainerSize
        if (cSize.width <= 0 || cSize.height <= 0) return null
        val ready = latestLoadResult as? MushafPageLoadResult.Ready
        val imgW = (ready?.bitmap?.width ?: layout.imageWidth).toFloat()
        val imgH = (ready?.bitmap?.height ?: layout.imageHeight).toFloat()

        val normalized = MushafCoordinateTransform.screenPointToNormalizedPage(
            tapX = offset.x,
            tapY = offset.y,
            containerWidth = cSize.width.toFloat(),
            containerHeight = cSize.height.toFloat(),
            imageWidth = imgW,
            imageHeight = imgH,
            zoomScale = latestZoomScale,
            zoomOffsetX = latestZoomOffset.x,
            zoomOffsetY = latestZoomOffset.y
        ) ?: return null

        return MushafCoordinateTransform.hitTestAyah(layout, normalized.first, normalized.second)
    }

    val frameBg = if (nightInvertEnabled) MushafNightPaper else MushafPaperBg
    val outerBorder = if (nightInvertEnabled) MushafBorderGold.copy(alpha = 0.55f) else MushafBorderEmerald.copy(alpha = 0.72f)
    val innerBorder = MushafBorderGold.copy(alpha = 0.78f)
    val headerInk = if (nightInvertEnabled) GoldBright else EmeraldDeep

    // Classic Madinah Mushaf double ornate frame
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(frameBg)
            .border(2.dp, outerBorder, RoundedCornerShape(18.dp))
            .padding(5.dp)
            .border(1.2.dp, innerBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Classic Printed Madinah Mushaf Page Header Row (Surah Name <-> Juz Number)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الجزء $juzNumber",
                    fontFamily = AmiriFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerInk
                )
                Text(
                    text = "سورة $surahNameAr",
                    fontFamily = AmiriFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerInk
                )
            }
            HorizontalDivider(color = innerBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

            // Interactive Page Image + Separate Transparent Highlight Overlay Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(8.dp))
                    .onSizeChanged { containerSize = it }
                    .pointerInput(pageNumber) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val hit = resolveHitAt(tapOffset)
                                latestOnTap(hit)
                            },
                            onDoubleTap = {
                                if (latestZoomScale > 1.1f) {
                                    latestOnZoomChange(1f, Offset.Zero)
                                } else {
                                    latestOnZoomChange(1.85f, Offset.Zero)
                                }
                            },
                            onLongPress = { pressOffset ->
                                val hit = resolveHitAt(pressOffset)
                                if (hit != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    latestOnLongPress(hit)
                                }
                            }
                        )
                    }
                    .pointerInput(pageNumber) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (latestZoomScale * zoom).coerceIn(1f, 2.6f)
                            val nextOffset = if (nextScale > 1.05f) {
                                Offset(
                                    (latestZoomOffset.x + pan.x).coerceIn(-380f, 380f),
                                    (latestZoomOffset.y + pan.y).coerceIn(-480f, 480f)
                                )
                            } else {
                                Offset.Zero
                            }
                            if (nextScale != latestZoomScale || nextOffset != latestZoomOffset) {
                                latestOnZoomChange(nextScale, nextOffset)
                            }
                        }
                    }
                    .testTag("mushaf_interactive_page_$pageNumber"),
                contentAlignment = Alignment.Center
            ) {
                when (val result = loadResult) {
                    null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GoldPrimary)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = L10n.pick(lang, "جارٍ تحميل صفحة المصحف $pageNumber...", "Loading Madinah Mushaf page $pageNumber..."),
                                style = MaterialTheme.typography.bodySmall,
                                color = headerInk
                            )
                        }
                    }

                    is MushafPageLoadResult.Ready -> {
                        val imageBitmap = remember(result.bitmap) { result.bitmap.asImageBitmap() }
                        val sharedTransform = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = zoomOffset.x,
                                translationY = zoomOffset.y
                            )

                        // Layer 1: Untouched raw Madinah Mushaf page bitmap
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Madinah Mushaf Page $pageNumber — Surah $surahNameAr",
                            contentScale = ContentScale.Fit,
                            colorFilter = if (nightInvertEnabled) MushafNightColorFilter else null,
                            modifier = sharedTransform
                        )

                        // Layer 2: Separate transparent Ayah & verified word highlight overlay
                        MushafAyahHighlightOverlay(
                            layout = pageLayout,
                            imageWidth = result.bitmap.width,
                            imageHeight = result.bitmap.height,
                            activeRecitedAyah = activeRecitedAyah,
                            activeWordPosition = activeWordPosition,
                            selectedAyah = selectedAyah,
                            highlightStyle = highlightStyle,
                            nightMode = nightInvertEnabled,
                            modifier = sharedTransform
                        )

                        if (zoomScale > 1.05f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldDeep.copy(alpha = 0.85f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ZoomIn, contentDescription = null, tint = GoldBright, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${(zoomScale * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    is MushafPageLoadResult.Unavailable -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MenuBook,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = L10n.pick(lang, result.reasonAr, result.reasonEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (nightInvertEnabled) Color(0xFFD6E6DF) else IvoryInk,
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { reloadNonce++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(L10n.pick(lang, "إعادة التحميل", "Retry"))
                                }
                                OutlinedButton(onClick = onSwitchToTextMushaf) {
                                    Text(L10n.pick(lang, "النص العثماني", "Uthmani Text"))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = innerBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

            // Authentic Madinah Mushaf Bottom Page Number Medallion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "﴿ $pageNumber ﴾",
                    fontFamily = AmiriFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerInk
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRightPage) L10n.pick(lang, "يمين", "Right") else L10n.pick(lang, "يسار", "Left"),
                    style = MaterialTheme.typography.labelSmall,
                    color = headerInk.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MushafOfflinePackDialog(
    progress: MushafDownloadProgress,
    currentSurahNumber: Int,
    currentSurahNameAr: String,
    currentJuz: Int,
    onDownloadCurrentSurah: () -> Unit,
    onDownloadCurrentJuz: () -> Unit,
    onDownloadFull604: () -> Unit,
    onImportLocalZip: () -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val pct = (progress.cachedPagesCount.toFloat() / progress.totalPages.coerceAtLeast(1)).coerceIn(0f, 1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(L10n.pick(lang, "مدير مصحف المدينة دون إنترنت (٦٠٤ صفحة)", "Offline Madinah Mushaf Manager (604 Pages)"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = L10n.pick(
                        lang,
                        "الصفحات المحفوظة محلياً: ${progress.cachedPagesCount} / ${progress.totalPages} (${String.format("%.1f", progress.storageSizeMb)} م.ب)",
                        "Cached offline: ${progress.cachedPagesCount} / ${progress.totalPages} pages (${String.format("%.1f", progress.storageSizeMb)} MB)"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth(), color = GoldPrimary)

                if (progress.isDownloading) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "${progress.statusAr} (${progress.batchCompleted}/${progress.batchTotal})",
                            "${progress.statusEn} (${progress.batchCompleted}/${progress.batchTotal})"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary
                    )
                } else if (progress.statusAr.isNotBlank()) {
                    Text(
                        text = L10n.pick(lang, progress.statusAr, progress.statusEn),
                        style = MaterialTheme.typography.labelSmall,
                        color = SemanticSuccess
                    )
                }
                if (progress.errorAr.isNotBlank()) {
                    Text(
                        text = L10n.pick(lang, progress.errorAr, progress.errorEn),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = onDownloadCurrentSurah,
                    enabled = !progress.isDownloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(L10n.pick(lang, "تحميل صفحات سورة $currentSurahNameAr ($currentSurahNumber)", "Download Surah $currentSurahNumber Pages"))
                }
                OutlinedButton(
                    onClick = onDownloadCurrentJuz,
                    enabled = !progress.isDownloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(L10n.pick(lang, "تحميل صفحات الجزء $currentJuz بالكامل", "Download Full Juz $currentJuz"))
                }
                OutlinedButton(
                    onClick = onDownloadFull604,
                    enabled = !progress.isDownloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(L10n.pick(lang, "تحميل المصحف الكامل (٦٠٤ صفحة)", "Download All 604 Pages"))
                }
                OutlinedButton(
                    onClick = onImportLocalZip,
                    enabled = !progress.isDownloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(L10n.pick(lang, "استيراد حزمة ZIP محلية (بدون إنترنت)", "Import Local ZIP Pack (Offline)"))
                }
                if (progress.cachedPagesCount > 0 && !progress.isDownloading) {
                    TextButton(onClick = onClearCache, modifier = Modifier.align(Alignment.End)) {
                        Text(L10n.pick(lang, "مسح ذاكرة الصور المؤقتة", "Clear Cached Pages"))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(L10n.pick(lang, "إغلاق", "Close")) }
        }
    )
}
