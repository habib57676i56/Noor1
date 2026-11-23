package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.quran.MushafDownloadProgress
import com.example.data.quran.MushafPageImageManager
import com.example.data.quran.MushafPageLoadResult
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
 * Authentic 604-Page Madinah Mushaf Page-Image Reader.
 *
 * Displays exact verified King Fahd Complex Madinah Mushaf page images with:
 * - Authentic page borders, Surah/Juz/Page headers, and printed proportions.
 * - Full 604-page RTL HorizontalPager navigation + direct Jump to Page / Juz.
 * - Live audio follow-along synchronization (auto-navigates to the verse's page as Sheikh recites).
 * - Pinch-to-zoom, Night Mode reading filter, Offline Pack Downloader & local ZIP importer.
 * - Graceful Uthmani text fallback if a page is not cached offline yet and the device is offline.
 */
@Composable
fun MadinahMushafReaderView(
    imageManager: MushafPageImageManager,
    initialPage: Int,
    activeSyncedPage: Int?,
    activeSyncedVerse: QuranVerse?,
    isAudioPlaying: Boolean,
    isNightTheme: Boolean,
    onPageChanged: (pageNumber: Int, primarySurah: Int) -> Unit,
    onToggleAudio: () -> Unit,
    onSwitchToTextMushaf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val scope = rememberCoroutineScope()
    val downloadProgress by imageManager.progress.collectAsStateWithLifecycle()

    val safeInitialIndex = (initialPage.coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES) - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { MadinahMushafPageCatalog.TOTAL_PAGES }
    )

    var nightInvertEnabled by remember(isNightTheme) { mutableStateOf(isNightTheme) }
    var showOfflineManagerDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }

    val zipImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { imageManager.importOfflineZipPack(uri) }
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

    // Notify parent & prefetch neighbor pages whenever the visible page changes
    val currentPageNumber = pagerState.currentPage + 1
    val currentMeta = remember(currentPageNumber) { MadinahMushafPageCatalog.pageMeta(currentPageNumber) }

    LaunchedEffect(currentPageNumber) {
        zoomScale = 1f
        zoomOffset = Offset.Zero
        onPageChanged(currentPageNumber, currentMeta.primarySurahNumber)
        imageManager.prefetchNeighborPages(currentPageNumber)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("madinah_mushaf_image_reader"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Mushaf Toolbar: Page/Juz/Surah Info + Offline Manager + Night Filter + Jump
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
                        "صفحة $currentPageNumber من ٦٠٤ · محفوظ دون إنترنت: ${downloadProgress.cachedPagesCount}/٦٠٤",
                        "Page $currentPageNumber / 604 · Offline cached: ${downloadProgress.cachedPagesCount}/604"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (nightInvertEnabled) Color(0xFFB5CEC4) else Color(0xFF5D5242)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Authentic Printed Madinah Mushaf 604-Page Swipeable Viewer
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = zoomScale <= 1.05f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("madinah_mushaf_pager")
        ) { pageIndex ->
            val pageNum = pageIndex + 1
            val meta = remember(pageNum) { MadinahMushafPageCatalog.pageMeta(pageNum) }
            MadinahMushafSinglePageCard(
                pageNumber = pageNum,
                surahNameAr = meta.primarySurahNameAr,
                juzNumber = meta.juzNumber,
                isRightPage = meta.isRightPage,
                imageManager = imageManager,
                nightInvertEnabled = nightInvertEnabled,
                zoomScale = if (pagerState.currentPage == pageIndex) zoomScale else 1f,
                zoomOffset = if (pagerState.currentPage == pageIndex) zoomOffset else Offset.Zero,
                onZoomChange = { newScale, newOffset ->
                    zoomScale = newScale
                    zoomOffset = newOffset
                },
                onSwitchToTextMushaf = onSwitchToTextMushaf
            )
        }

        // Bottom Page Turn Controls + Synchronized Verse Highlight Strip
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
        if (activeSyncedVerse != null) {
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
                                "الآية الحالية المتزامنة: سورة ${currentMeta.primarySurahNameAr} — آية ${activeSyncedVerse.numberInSurah} (ص ${activeSyncedVerse.pageNumber})",
                                "Active Synced Verse: Ayah ${activeSyncedVerse.numberInSurah} (p.${activeSyncedVerse.pageNumber})"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = noorColors.goldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${activeSyncedVerse.textUthmani} ﴿${activeSyncedVerse.numberInSurah}﴾",
                            fontFamily = AmiriFontFamily,
                            fontSize = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
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
    nightInvertEnabled: Boolean,
    zoomScale: Float,
    zoomOffset: Offset,
    onZoomChange: (Float, Offset) -> Unit,
    onSwitchToTextMushaf: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var reloadNonce by remember(pageNumber) { mutableStateOf(0) }
    var loadResult by remember(pageNumber, reloadNonce) { mutableStateOf<MushafPageLoadResult?>(null) }

    LaunchedEffect(pageNumber, reloadNonce) {
        loadResult = imageManager.loadPageBitmap(pageNumber)
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

            // Page Image Area (Authentic Madinah Mushaf proportion ~ 1 : 1.46)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(pageNumber) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoomScale > 1.1f) onZoomChange(1f, Offset.Zero)
                                else onZoomChange(1.85f, Offset.Zero)
                            }
                        )
                    }
                    .pointerInput(pageNumber) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (zoomScale * zoom).coerceIn(1f, 2.6f)
                            val nextOffset = if (nextScale > 1.05f) {
                                Offset(
                                    (zoomOffset.x + pan.x).coerceIn(-350f, 350f),
                                    (zoomOffset.y + pan.y).coerceIn(-450f, 450f)
                                )
                            } else {
                                Offset.Zero
                            }
                            onZoomChange(nextScale, nextOffset)
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
                                color = headerInk
                            )
                        }
                    }

                    is MushafPageLoadResult.Ready -> {
                        val imageBitmap = remember(result.bitmap) { result.bitmap.asImageBitmap() }
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Madinah Mushaf Page $pageNumber — Surah $surahNameAr",
                            contentScale = ContentScale.Fit,
                            colorFilter = if (nightInvertEnabled) MushafNightColorFilter else null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = zoomOffset.x,
                                    translationY = zoomOffset.y
                                )
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
