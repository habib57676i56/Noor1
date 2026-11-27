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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.data.quran.AyahLinguisticAnalysis
import com.example.data.quran.AyahPageRegion
import com.example.data.quran.CoordinatePrecision
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

// Warm King Fahd Complex parchment matching width_1024 PNGs seamlessly edge-to-edge (zero bottom void!)
private val MushafPaperBg = Color(0xFFFDF7E2)
private val MushafBorderGold = Color(0xFFC9A24B)
private val MushafNightPaper = Color(0xFF0A1411)

// Exact aspect ratio of King Fahd Complex Madinah Mushaf width_1024 PNGs (1024 x 1656)
private const val MADINAH_MUSHAF_ASPECT_RATIO = 1024f / 1656f

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
 * Key engineering guarantees:
 * - Authentic untouched printed Madinah Mushaf page images without redundant outer frames/borders.
 * - Touch-anywhere vertical scrolling & horizontal page swiping (pinch-zoom only consumes 2-finger
 *   or zoomed-pan gestures, never blocking single-finger drags at 1.0x zoom).
 * - Clean gesture separation:
 *     * Single tap on an **empty area** -> toggles Clean Full-Screen Mushaf Mode (or dismisses open menu).
 *     * Single tap or long press on an **Ayah** -> accurately selects & highlights that exact Ayah
 *       with a clean translucent fill and opens the non-overlapping RTL contextual toolbar.
 *     * Vertical drag / horizontal swipe -> scrolls or turns page without accidental Ayah selection.
 * - Auto-downloads `ayahinfo_1024.db` once silently in the background when online for pixel-exact
 *   604-page glyph coordinates synced to `width_1024` Madinah PNGs.
 * - Independent live audio highlight tracking that NEVER interrupts or restarts Sheikh audio.
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
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    highlightStyle: MushafHighlightStyle,
    bookmarkedKeys: Set<Pair<Int, Int>>,
    loadPageVerses: suspend (Int) -> List<QuranVerse>,
    onPageChanged: (pageNumber: Int, primarySurah: Int, isUserNavigation: Boolean) -> Unit,
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
    var hasEmittedInitialPage by remember(initialPage) { mutableStateOf(false) }
    var lastAutoSyncedPage by remember { mutableStateOf<Int?>(null) }

    // Interactive Ayah Selection & Contextual Action Menu State (single active selection at a time)
    var selectedAyah by remember { mutableStateOf<AyahPageRegion?>(null) }
    var showContextualMenu by remember { mutableStateOf(false) }
    var currentPageVerses by remember { mutableStateOf<List<QuranVerse>>(emptyList()) }
    var currentPageLayout by remember { mutableStateOf<MushafPageLayout?>(null) }

    // Linguistic Analysis Sheet (I'rab / Sarf / Tafsir) State
    var activeLinguisticAnalysis by remember { mutableStateOf<AyahLinguisticAnalysis?>(null) }
    var activeLinguisticTab by remember { mutableStateOf(LinguisticSheetTab.IRAB) }

    // Repeat Ayah Range Dialog State
    var repeatRangeTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Silently upgrade to exact 604-page King Fahd glyph coordinates DB in background when online
    LaunchedEffect(Unit) {
        if (!coordinateRepository.hasExactGlyphDb()) {
            val upgraded = coordinateRepository.ensureExactGlyphDbInBackground()
            if (upgraded) {
                coordReloadNonce++
            }
        }
    }

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

    // Automatically turn to the exact Madinah Mushaf page ONLY when activeSyncedPage transitions
    // to a new page — never fights the user if they manually browse another page while audio plays!
    LaunchedEffect(activeSyncedPage) {
        val targetPage = activeSyncedPage?.coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES)
        if (targetPage == null) {
            lastAutoSyncedPage = null
            return@LaunchedEffect
        }
        if (lastAutoSyncedPage != targetPage) {
            lastAutoSyncedPage = targetPage
            val targetIndex = targetPage - 1
            if (pagerState.currentPage != targetIndex) {
                runCatching { pagerState.animateScrollToPage(targetIndex) }
            }
        }
    }

    val currentPageNumber = pagerState.currentPage + 1
    val currentMeta = remember(currentPageNumber) { MadinahMushafPageCatalog.pageMeta(currentPageNumber) }

    // Clear selection, reset zoom, prefetch neighbor images, and load coordinates for visible page
    LaunchedEffect(currentPageNumber, coordReloadNonce) {
        zoomScale = 1f
        zoomOffset = Offset.Zero
        selectedAyah = null
        showContextualMenu = false
        val isUserNav = hasEmittedInitialPage
        hasEmittedInitialPage = true
        onPageChanged(currentPageNumber, currentMeta.primarySurahNumber, isUserNav)
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

    val fullScreenPaperBg = if (nightInvertEnabled) MushafNightPaper else MushafPaperBg

    Column(
        modifier = modifier
            .then(
                if (isFullScreen) {
                    Modifier
                        .fillMaxSize()
                        .background(fullScreenPaperBg)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .testTag("madinah_mushaf_image_reader"),
        verticalArrangement = Arrangement.spacedBy(if (isFullScreen) 0.dp else 8.dp)
    ) {
        // Top Mushaf Toolbar — hidden cleanly in Full-Screen Mushaf Mode
        if (!isFullScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (nightInvertEnabled) Color(0xFF0E211A) else Color(0xFFF3EAD5))
                    .border(1.dp, MushafBorderGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "سورة ${currentMeta.primarySurahNameAr} · الجزء ${currentMeta.juzNumber} · ص $currentPageNumber",
                            "${currentMeta.primarySurahNameEn} · Juz ${currentMeta.juzNumber} · p.$currentPageNumber"
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
                            "اضغط على آية للخيارات · اضغط مساحة فارغة لملء الشاشة",
                            "Tap an Ayah for actions · Tap empty space for full screen"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (nightInvertEnabled) Color(0xFFB5CEC4) else Color(0xFF5D5242),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier.testTag("mushaf_fullscreen_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Fullscreen,
                            contentDescription = L10n.pick(lang, "وضع ملء الشاشة", "Full-Screen Mode"),
                            tint = if (nightInvertEnabled) GoldBright else EmeraldDeep
                        )
                    }
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
        }

        // Honest Missing-Coordinates Notice: never guess or fake verse selection regions!
        if (!isFullScreen && currentPageLayout?.precision == CoordinatePrecision.MISSING_COORDINATES) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (nightInvertEnabled) Color(0xFF172721) else Color(0xFFFFF4DC))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("mushaf_missing_coords_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = L10n.pick(
                        lang,
                        "بيانات إحداثيات الآيات لهذه الصفحة غير مثبّتة بعد — نزّل قاعدة الإحداثيات لتحديد الآيات بدقة.",
                        "Verse coordinate metadata is missing for page $currentPageNumber — download coordinates for accurate selection.",
                        "$currentPageNumber-sahifa uchun oyat koordinatalari hali yuklanmagan — aniq tanlash uchun koordinatalarni yuklab oling."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (nightInvertEnabled) Color(0xFFE5DFC8) else EmeraldDeep,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    enabled = !coordPackStatus.isDownloading,
                    onClick = {
                        scope.launch {
                            val ok = coordinateRepository.downloadExactGlyphDatabase()
                            if (ok) coordReloadNonce++
                        }
                    },
                    modifier = Modifier.testTag("mushaf_download_coords_inline_btn")
                ) {
                    Text(
                        text = if (coordPackStatus.isDownloading) {
                            L10n.pick(lang, "جارٍ التنزيل...", "Downloading...", "Yuklanmoqda...")
                        } else {
                            L10n.pick(lang, "تنزيل الإحداثيات", "Download Coordinates")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
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
                onOpenTafsir = { openLinguisticSheet(activeSelected, LinguisticSheetTab.TAFSIR) },
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
        // In full-screen mode fills 100% of screen height edge-to-edge (ZERO empty void at bottom!)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = zoomScale <= 1.05f,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullScreen) Modifier.weight(1f) else Modifier)
                .testTag("madinah_mushaf_pager")
        ) { pageIndex ->
            val pageNum = pageIndex + 1
            val meta = remember(pageNum) { MadinahMushafPageCatalog.pageMeta(pageNum) }
            val isCurrentPage = pagerState.currentPage == pageIndex

            MadinahMushafSinglePageCard(
                pageNumber = pageNum,
                juzNumber = meta.juzNumber,
                surahNameAr = meta.primarySurahNameAr,
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
                isFullScreen = isFullScreen,
                isAudioPlaying = isAudioPlaying,
                onToggleAudio = onToggleAudio,
                zoomScale = if (isCurrentPage) zoomScale else 1f,
                zoomOffset = if (isCurrentPage) zoomOffset else Offset.Zero,
                onZoomChange = { newScale, newOffset ->
                    zoomScale = newScale
                    zoomOffset = newOffset
                },
                onSwipeNextPage = {
                    if (pagerState.currentPage < MadinahMushafPageCatalog.TOTAL_PAGES - 1) {
                        zoomScale = 1f
                        zoomOffset = Offset.Zero
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                onSwipePrevPage = {
                    if (pagerState.currentPage > 0) {
                        zoomScale = 1f
                        zoomOffset = Offset.Zero
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                onAyahTapped = { hit ->
                    if (hit == null) {
                        // Single tap on EMPTY AREA:
                        // 1. If a contextual menu or Ayah selection is open, dismiss it cleanly first.
                        // 2. Otherwise toggle Clean Full-Screen Mushaf Mode (without interrupting audio!).
                        if (showContextualMenu || selectedAyah != null) {
                            showContextualMenu = false
                            selectedAyah = null
                        } else {
                            onToggleFullScreen()
                        }
                    } else {
                        // Single tap on an AYAH:
                        // Never toggles full-screen! Accurately selects that exact Ayah & opens its toolbar.
                        if (showContextualMenu && selectedAyah?.key == hit.key) {
                            showContextualMenu = false
                            selectedAyah = null
                        } else {
                            selectedAyah = hit
                            showContextualMenu = true
                        }
                    }
                },
                onAyahLongPressed = { hit ->
                    // Long press on an AYAH: selects exact Ayah + opens full contextual toolbar.
                    selectedAyah = hit
                    showContextualMenu = true
                },
                onExitFullScreen = onToggleFullScreen,
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
                onOpenTafsir = { openLinguisticSheet(activeSelected, LinguisticSheetTab.TAFSIR) },
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

        // Bottom Page Turn Controls + Active Synced Verse Strip — hidden cleanly in Full-Screen Mode
        if (!isFullScreen) {
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
    juzNumber: Int,
    surahNameAr: String,
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
    isFullScreen: Boolean,
    isAudioPlaying: Boolean,
    onToggleAudio: () -> Unit,
    zoomScale: Float,
    zoomOffset: Offset,
    onZoomChange: (Float, Offset) -> Unit,
    onSwipeNextPage: () -> Unit,
    onSwipePrevPage: () -> Unit,
    onAyahTapped: (AyahPageRegion?) -> Unit,
    onAyahLongPressed: (AyahPageRegion) -> Unit,
    onExitFullScreen: () -> Unit,
    onSwitchToTextMushaf: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptics = LocalHapticFeedback.current
    var reloadNonce by remember(pageNumber) { mutableIntStateOf(0) }
    var loadResult by remember(pageNumber, reloadNonce) { mutableStateOf<MushafPageLoadResult?>(null) }
    var pageLayout by remember(pageNumber) { mutableStateOf(preloadedLayout) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val readyImage = loadResult as? MushafPageLoadResult.Ready
    val actualOriginalWidth = readyImage?.originalWidth ?: 0
    val actualOriginalHeight = readyImage?.originalHeight ?: 0

    LaunchedEffect(preloadedLayout, actualOriginalWidth, actualOriginalHeight, readyImage?.bitmap) {
        if (preloadedLayout != null && preloadedLayout.pageNumber == pageNumber) {
            val current = pageLayout
            val hasRealDimensions = actualOriginalWidth > 0 && actualOriginalHeight > 0
            val matchesRealHeight = !hasRealDimensions ||
                (preloadedLayout.imageWidth == actualOriginalWidth && preloadedLayout.imageHeight == actualOriginalHeight)
            val isOpeningWithBitmap = (pageNumber == 1 || pageNumber == 2) && readyImage?.bitmap != null
            if (current == null || current.pageNumber != pageNumber) {
                pageLayout = preloadedLayout
            } else if (!isOpeningWithBitmap && matchesRealHeight && preloadedLayout.precision == CoordinatePrecision.EXACT_GLYPH_DB &&
                current.precision != CoordinatePrecision.EXACT_GLYPH_DB
            ) {
                pageLayout = preloadedLayout
            }
        }
    }

    LaunchedEffect(pageNumber, reloadNonce) {
        loadResult = imageManager.loadPageBitmap(pageNumber)
    }

    // CRITICAL FIX: Always re-resolve pageLayout whenever coordReloadNonce increments (e.g. after
    // ayahinfo_1024.db downloads) OR once the true PNG originalWidth x originalHeight / bitmap is ready!
    // Passing `loadedBitmap` allows Pages 1 & 2 to lock pixel-accurately onto the real Arabic ink lines.
    LaunchedEffect(pageNumber, coordReloadNonce, actualOriginalWidth, actualOriginalHeight, readyImage?.bitmap) {
        val verses = loadPageVerses(pageNumber)
        pageLayout = coordinateRepository.getPageLayout(
            pageNumber = pageNumber,
            fallbackVerses = verses,
            actualImageWidth = actualOriginalWidth,
            actualImageHeight = actualOriginalHeight,
            loadedBitmap = readyImage?.bitmap
        )
    }

    // Keep latest state accessible inside pointerInput gesture coroutines without restarting recognizers
    val latestZoomScale by rememberUpdatedState(zoomScale)
    val latestZoomOffset by rememberUpdatedState(zoomOffset)
    val latestContainerSize by rememberUpdatedState(containerSize)
    val latestLayout by rememberUpdatedState(pageLayout)
    val latestLoadResult by rememberUpdatedState(loadResult)
    val latestOnTap by rememberUpdatedState(onAyahTapped)
    val latestOnLongPress by rememberUpdatedState(onAyahLongPressed)
    val latestOnZoomChange by rememberUpdatedState(onZoomChange)
    val latestOnSwipeNext by rememberUpdatedState(onSwipeNextPage)
    val latestOnSwipePrev by rememberUpdatedState(onSwipePrevPage)

    fun resolveHitAt(offset: Offset): AyahPageRegion? {
        val layout = latestLayout ?: return null
        if (!layout.hasVerifiedCoordinates) return null
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

    val paperBg = if (nightInvertEnabled) MushafNightPaper else MushafPaperBg
    val statusInk = if (nightInvertEnabled) GoldBright else EmeraldDeep
    val runningMetaInk = if (nightInvertEnabled) Color(0xFFB5C7BF) else Color(0xFF6B5D4A)
    val effectiveAspectRatio = remember(actualOriginalWidth, actualOriginalHeight) {
        if (actualOriginalWidth > 0 && actualOriginalHeight > 0) {
            (actualOriginalWidth.toFloat() / actualOriginalHeight.toFloat()).coerceIn(0.52f, 0.80f)
        } else {
            MADINAH_MUSHAF_ASPECT_RATIO
        }
    }

    val pageSurfaceModifier = Modifier
        .fillMaxWidth()
        .then(if (isFullScreen) Modifier.fillMaxHeight() else Modifier.aspectRatio(effectiveAspectRatio))
        .clip(RoundedCornerShape(if (isFullScreen) 0.dp else 12.dp))
        .background(paperBg)
        .testTag("mushaf_interactive_page_$pageNumber")

    Box(modifier = pageSurfaceModifier) {
        // Authentic subtle Mushaf book-spine edge shadow (matching Quran for Android page feel)
        val spineGradient = remember(nightInvertEnabled, isRightPage) {
            val darkEdge = if (nightInvertEnabled) Color(0x88000000) else Color(0x2A3D3428)
            val midEdge = if (nightInvertEnabled) Color(0x33000000) else Color(0x103D3428)
            if (isRightPage) {
                Brush.horizontalGradient(listOf(darkEdge, midEdge, Color.Transparent))
            } else {
                Brush.horizontalGradient(listOf(Color.Transparent, midEdge, darkEdge))
            }
        }
        Box(
            modifier = Modifier
                .align(if (isRightPage) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .width(12.dp)
                .background(spineGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isFullScreen) Modifier.navigationBarsPadding() else Modifier)
        ) {
            // In Full-Screen Mode, show authentic subtle Quran-for-Android running Mushaf header
            // (`الجزء X` on one side, `سورة ...` on the other) + unobtrusive audio/exit icon buttons
            if (isFullScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onExitFullScreen,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(runningMetaInk.copy(alpha = 0.12f))
                                .testTag("mushaf_exit_fullscreen_btn")
                        ) {
                            Icon(
                                Icons.Rounded.FullscreenExit,
                                contentDescription = L10n.pick(lang, "إظهار الأدوات", "Exit Full-Screen"),
                                tint = runningMetaInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "سورة $surahNameAr",
                            fontFamily = AmiriFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = runningMetaInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "الجزء $juzNumber",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = runningMetaInk
                        )
                        IconButton(
                            onClick = onToggleAudio,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(runningMetaInk.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = L10n.pick(lang, "تشغيل/إيقاف التلاوة", "Play/Pause"),
                                tint = runningMetaInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Page Canvas (Image + Seamless Connected Highlight Overlay)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { containerSize = it }
                    // Tap / Double-Tap / Long-Press handler (never blocks horizontal page swiping!)
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
                                    latestOnZoomChange(1.75f, Offset.Zero)
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
                    // Pinch-to-Zoom, Zoomed-Pan, AND Edge-Swipe Page Turning while zoomed!
                    // NEVER consumes single-finger swipes at 1.0x so HorizontalPager flips smoothly anywhere!
                    .pointerInput(pageNumber, isRtl) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var edgeOverscrollX = 0f
                            var flippedThisGesture = false
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                val isMultiTouch = pressedCount >= 2
                                val isZoomedPan = pressedCount == 1 && latestZoomScale > 1.02f

                                if (isMultiTouch || isZoomedPan) {
                                    val zoomChange = if (isMultiTouch) event.calculateZoom() else 1f
                                    val panChange = event.calculatePan()
                                    val nextScale = (latestZoomScale * zoomChange).coerceIn(1f, 2.8f)
                                    val cSize = latestContainerSize
                                    val maxPanX = ((cSize.width * (nextScale - 1f)) * 0.5f).coerceAtLeast(0f)
                                    val maxPanY = ((cSize.height * (nextScale - 1f)) * 0.5f).coerceAtLeast(0f)
                                    val rawNextX = latestZoomOffset.x + panChange.x
                                    val clampedX = rawNextX.coerceIn(-maxPanX, maxPanX)
                                    val clampedY = (latestZoomOffset.y + panChange.y).coerceIn(-maxPanY, maxPanY)
                                    val nextOffset = if (nextScale > 1.02f) {
                                        Offset(clampedX, clampedY)
                                    } else {
                                        Offset.Zero
                                    }

                                    // If zoomed and dragging horizontally past the page edge, smoothly flip page!
                                    if (!isMultiTouch && !flippedThisGesture && nextScale > 1.02f && kotlin.math.abs(panChange.x) > kotlin.math.abs(panChange.y)) {
                                        val overflowX = rawNextX - clampedX
                                        if (kotlin.math.abs(overflowX) > 0.5f) {
                                            edgeOverscrollX += overflowX
                                            val thresholdPx = (cSize.width * 0.14f).coerceIn(85f, 160f)
                                            if (kotlin.math.abs(edgeOverscrollX) >= thresholdPx) {
                                                flippedThisGesture = true
                                                val goNext = if (isRtl) edgeOverscrollX > 0f else edgeOverscrollX < 0f
                                                if (goNext) latestOnSwipeNext() else latestOnSwipePrev()
                                            }
                                        } else {
                                            edgeOverscrollX = 0f
                                        }
                                    }

                                    if (!flippedThisGesture && (nextScale != latestZoomScale || nextOffset != latestZoomOffset)) {
                                        latestOnZoomChange(nextScale, nextOffset)
                                    }
                                    if (isMultiTouch || panChange != Offset.Zero) {
                                        event.changes.forEach { change ->
                                            if (change.pressed) change.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
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
                                color = statusInk
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

                        // Layer 1: Untouched authentic printed Madinah Mushaf page image
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Madinah Mushaf Page $pageNumber — Surah $surahNameAr",
                            contentScale = ContentScale.Fit,
                            colorFilter = if (nightInvertEnabled) MushafNightColorFilter else null,
                            modifier = sharedTransform
                        )

                        // Layer 2: Seamless connected translucent Ayah highlight overlay (zero inter-line gaps!)
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
                                    .padding(8.dp)
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

            // Authentic centered page number footer in Full-Screen Mode (matches Quran for Android)
            if (isFullScreen) {
                Text(
                    text = pageNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = runningMetaInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
