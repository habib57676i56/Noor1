package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.catalog.SurahCatalog
import com.example.data.quran.AyahLineRegion
import com.example.data.quran.AyahLinguisticAnalysis
import com.example.data.quran.AyahPageRegion
import com.example.data.quran.FittedImageRect
import com.example.data.quran.LinguisticVerificationStatus
import com.example.data.quran.MushafCoordinatePackStatus
import com.example.data.quran.MushafCoordinateTransform
import com.example.data.quran.MushafHighlightColor
import com.example.data.quran.MushafHighlightStyle
import com.example.data.quran.MushafPageLayout
import com.example.data.quran.NormalizedRect
import com.example.data.quran.WordLinguisticAnalysis
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticSuccess

enum class LinguisticSheetTab(val index: Int) {
    IRAB(0),
    SARF(1),
    TAFSIR(2)
}

/**
 * Computes seamless, vertically-connected pixel rectangles for a single- or multi-line Ayah
 * so consecutive lines touch cleanly at their exact midpoint with ZERO white gap between lines
 * (matching the clean unified connected-block highlight style of King Fahd / Quran for Android).
 */
internal fun buildSeamlessConnectedHighlightRects(
    lineRegions: List<AyahLineRegion>,
    endMarkerBounds: NormalizedRect?,
    fitted: FittedImageRect
): List<FittedImageRect> {
    if (lineRegions.isEmpty() || fitted.width <= 0f || fitted.height <= 0f) return emptyList()
    val sorted = lineRegions.sortedBy { it.lineNumber }
    val lineNumbers = mutableListOf<Int>()
    val rawRects = mutableListOf<FittedImageRect>()

    for ((idx, seg) in sorted.withIndex()) {
        // Encompass the Ayah's ornamental end-rosette circle (﴿١﴾) cleanly when it sits on the
        // Ayah's final line, matching Quran-for-Android's even end-to-end highlight block.
        var effectiveBounds = seg.bounds
        if (idx == sorted.lastIndex && endMarkerBounds != null && endMarkerBounds.width > 0f && endMarkerBounds.height > 0f) {
            val sameLineBand = endMarkerBounds.bottom >= seg.bounds.top - 0.015f &&
                endMarkerBounds.top <= seg.bounds.bottom + 0.015f
            val adjacentOnLeft = endMarkerBounds.left >= seg.bounds.left - 0.065f &&
                endMarkerBounds.right <= seg.bounds.right + 0.02f
            if (sameLineBand && adjacentOnLeft) {
                effectiveBounds = NormalizedRect(
                    left = minOf(seg.bounds.left, endMarkerBounds.left),
                    top = minOf(seg.bounds.top, endMarkerBounds.top),
                    right = maxOf(seg.bounds.right, endMarkerBounds.right),
                    bottom = maxOf(seg.bounds.bottom, endMarkerBounds.bottom)
                )
            }
        }
        val pxRect = MushafCoordinateTransform.normalizedRectToFittedRect(effectiveBounds, fitted)
        if (pxRect.width <= 1f || pxRect.height <= 1f) continue
        lineNumbers += seg.lineNumber
        rawRects += pxRect
    }
    if (rawRects.isEmpty()) return emptyList()

    val avgLineH = rawRects.map { it.height }.average().toFloat().coerceAtLeast(8f)
    val outerPadY = (avgLineH * 0.06f).coerceIn(1.5f, 6f)
    val padX = (fitted.width * 0.0045f).coerceIn(1.5f, 5.5f)

    // First expand horizontal edges gently and outer top/bottom edges
    val tops = FloatArray(rawRects.size) { i -> (rawRects[i].top - outerPadY).coerceAtLeast(fitted.top) }
    val bottoms = FloatArray(rawRects.size) { i -> (rawRects[i].bottom + outerPadY).coerceAtMost(fitted.bottom) }
    val lefts = FloatArray(rawRects.size) { i -> (rawRects[i].left - padX).coerceAtLeast(fitted.left) }
    val rights = FloatArray(rawRects.size) { i -> (rawRects[i].right + padX).coerceAtMost(fitted.right) }

    // Bridge EVERY consecutive pair of lines in the same Ayah seamlessly at their exact vertical midpoint
    // with a 1px overlap so PathOperation.Union produces ONE solid connected shape with ZERO gaps!
    for (i in 0 until rawRects.lastIndex) {
        val consecutiveLine = (lineNumbers[i + 1] - lineNumbers[i]) <= 1
        if (consecutiveLine) {
            val midY = (rawRects[i].bottom + rawRects[i + 1].top) * 0.5f
            bottoms[i] = maxOf(bottoms[i], midY + 1.0f)
            tops[i + 1] = minOf(tops[i + 1], midY - 1.0f)
        }
    }

    return List(rawRects.size) { i ->
        FittedImageRect(
            left = lefts[i],
            top = tops[i],
            width = (rights[i] - lefts[i]).coerceAtLeast(1f),
            height = (bottoms[i] - tops[i]).coerceAtLeast(1f)
        )
    }
}

private fun buildUnifiedHighlightPath(rects: List<FittedImageRect>): Path {
    val combined = Path()
    for (r in rects) {
        val segPath = Path().apply {
            addRect(Rect(r.left, r.top, r.right, r.bottom))
        }
        if (combined.isEmpty) {
            combined.addPath(segPath)
        } else {
            combined.op(combined, segPath, PathOperation.Union)
        }
    }
    return combined
}

/**
 * Separate transparent highlight overlay layer drawn ABOVE the untouched Madinah Mushaf page bitmap.
 *
 * Shares the exact same `graphicsLayer` zoom & pan transform as the Image, so highlights remain
 * locked pixel-accurately to Ayah & word bounds across zooming, panning, and different aspect ratios.
 */
@Composable
fun MushafAyahHighlightOverlay(
    layout: MushafPageLayout?,
    imageWidth: Int,
    imageHeight: Int,
    activeRecitedAyah: Pair<Int, Int>?,
    activeWordPosition: Int?,
    selectedAyah: AyahPageRegion?,
    highlightStyle: MushafHighlightStyle,
    nightMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (layout == null || !layout.hasVerifiedCoordinates || imageWidth <= 0 || imageHeight <= 0) return

    val pulseTransition = rememberInfiniteTransition(label = "mushaf_highlight_pulse")
    val pulseFactor by pulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val effectivePulse = if (highlightStyle.pulseAnimated && activeRecitedAyah != null) pulseFactor else 1f
    val baseAlpha = highlightStyle.alpha.coerceIn(0.16f, 0.55f)
    val preset = highlightStyle.colorPreset

    val activeRegion = remember(layout, activeRecitedAyah) {
        activeRecitedAyah?.let { (s, a) ->
            // In Surah Al-Fatihah (Surah 1), Bismillah IS Ayah 1 (1:1) and MUST be highlighted!
            // On Surahs 2..114, opening Bismillah headers use ayahNumber <= 0 so they are NOT highlighted.
            if (a <= 0) null else layout.findAyah(s, a)
        }
    }
    val activeWordRegion = remember(activeRegion, activeWordPosition) {
        if (activeRegion != null && activeWordPosition != null && activeWordPosition > 0) {
            activeRegion.words.firstOrNull { it.wordPosition == activeWordPosition }
        } else {
            null
        }
    }

    // Graceful, continuous Ayah-to-Ayah transition state (no abrupt disappear/reappear blink!)
    var displayedRegion by remember(layout.pageNumber) { mutableStateOf(activeRegion) }
    var outgoingRegion by remember(layout.pageNumber) { mutableStateOf<AyahPageRegion?>(null) }
    val transitionProgress = remember(layout.pageNumber) { Animatable(1f) }

    LaunchedEffect(activeRegion?.key, layout.pageNumber) {
        val nextKey = activeRegion?.key
        val currKey = displayedRegion?.key
        if (nextKey != currKey) {
            val previous = displayedRegion
            outgoingRegion = previous
            displayedRegion = activeRegion
            when {
                previous != null && activeRegion != null -> {
                    // Silky liquid crossfade + flowing glide between consecutive Ayahs (420ms)
                    transitionProgress.snapTo(0f)
                    transitionProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                    outgoingRegion = null
                }
                activeRegion != null -> {
                    transitionProgress.snapTo(0f)
                    transitionProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    )
                    outgoingRegion = null
                }
                else -> {
                    transitionProgress.snapTo(0f)
                    transitionProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                    outgoingRegion = null
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("mushaf_highlight_overlay")
    ) {
        val fitted = MushafCoordinateTransform.computeFittedImageRect(
            containerWidth = size.width,
            containerHeight = size.height,
            imageWidth = imageWidth.toFloat(),
            imageHeight = imageHeight.toFloat()
        )
        val cornerPx = (fitted.width * 0.005f).coerceIn(2f, 6f)
        val targetAlpha = (baseAlpha * effectivePulse * (if (nightMode) 1.15f else 0.96f)).coerceIn(0.16f, 0.44f)
        val p = transitionProgress.value.coerceIn(0f, 1f)
        // Smoothstep easing curve for overlapping glow handoff
        val easeIn = p * p * (3f - 2f * p)
        val easeOut = 1f - easeIn

        // 1A. Outgoing Ayah — softly fades out while handing off light to the next Ayah
        val outReg = outgoingRegion
        val outRects = if (outReg != null && easeOut > 0.01f) {
            val rects = buildSeamlessConnectedHighlightRects(
                lineRegions = outReg.lineRegions,
                endMarkerBounds = outReg.endMarkerBounds,
                fitted = fitted
            )
            if (rects.isNotEmpty()) {
                val unifiedOut = buildUnifiedHighlightPath(rects)
                drawPath(path = unifiedOut, color = preset.ayahFillColor.copy(alpha = targetAlpha * easeOut))
            }
            rects
        } else {
            emptyList()
        }

        // 1B. Incoming / Current Recited Ayah — glides in smoothly as ONE unified connected block
        val inReg = displayedRegion
        val inRects = if (inReg != null && easeIn > 0.01f) {
            val rects = buildSeamlessConnectedHighlightRects(
                lineRegions = inReg.lineRegions,
                endMarkerBounds = inReg.endMarkerBounds,
                fitted = fitted
            )
            if (rects.isNotEmpty()) {
                val unifiedIn = buildUnifiedHighlightPath(rects)
                drawPath(path = unifiedIn, color = preset.ayahFillColor.copy(alpha = targetAlpha * easeIn))
            }
            rects
        } else {
            emptyList()
        }

        // 1C. Soft flowing light bridge gliding from end of outgoing Ayah to start of incoming Ayah
        if (outRects.isNotEmpty() && inRects.isNotEmpty() && p in 0.02f..0.98f) {
            val fromRect = outRects.last()
            val toRect = inRects.first()
            val fromAnchor = Offset(fromRect.left + fromRect.width * 0.15f, fromRect.top + fromRect.height * 0.5f)
            val toAnchor = Offset(toRect.right - toRect.width * 0.15f, toRect.top + toRect.height * 0.5f)
            val flowCenter = Offset(
                x = fromAnchor.x + (toAnchor.x - fromAnchor.x) * easeIn,
                y = fromAnchor.y + (toAnchor.y - fromAnchor.y) * easeIn
            )
            val glowRadius = (maxOf(fromRect.height, toRect.height) * 1.15f).coerceIn(18f, 68f)
            val bridgeAlpha = (targetAlpha * 0.65f * sin(p * PI).toFloat()).coerceIn(0f, 0.30f)
            if (bridgeAlpha > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            preset.ayahFillColor.copy(alpha = bridgeAlpha),
                            preset.ayahFillColor.copy(alpha = 0f)
                        ),
                        center = flowCenter,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = flowCenter
                )
            }
        }

        // 2. Verified Word-by-Word Audio Highlight (ONLY when verified millisecond timestamps exist)
        if (inReg != null && activeWordRegion != null) {
            val safeWordBounds = MushafCoordinateTransform.clipBoundsExcludingEndMarker(
                segBounds = activeWordRegion.bounds,
                endMarkerBounds = inReg.endMarkerBounds
            )
            if (safeWordBounds != null) {
                val wRect = MushafCoordinateTransform.normalizedRectToFittedRect(safeWordBounds, fitted)
                if (wRect.width > 1f && wRect.height > 1f) {
                    drawRoundRect(
                        color = preset.wordFillColor.copy(alpha = ((baseAlpha + 0.14f).coerceAtMost(0.48f)) * easeIn),
                        topLeft = Offset(wRect.left, wRect.top),
                        size = Size(wRect.width, wRect.height),
                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                    )
                }
            }
        }

        // 3. User-Selected Ayah Highlight — unified seamless connected block (zero gaps between lines!)
        if (selectedAyah != null) {
            val isSameAsActive = activeRegion != null &&
                activeRegion.surahNumber == selectedAyah.surahNumber &&
                activeRegion.ayahNumber == selectedAyah.ayahNumber
            val selectionFill = preset.ayahFillColor.copy(alpha = if (nightMode) 0.30f else 0.24f)
            val connectedRects = buildSeamlessConnectedHighlightRects(
                lineRegions = selectedAyah.lineRegions,
                endMarkerBounds = selectedAyah.endMarkerBounds,
                fitted = fitted
            )
            if (connectedRects.isNotEmpty() && !isSameAsActive) {
                val unifiedPath = buildUnifiedHighlightPath(connectedRects)
                drawPath(path = unifiedPath, color = selectionFill)
            }
        }
    }
}

/**
 * Floating RTL Contextual Ayah Action Menu positioned cleanly ABOVE or BELOW the selected Ayah
 * so it never covers the Ayah text. Only one contextual menu is active at a time.
 */
@Composable
fun MushafAyahContextualMenu(
    selectedAyah: AyahPageRegion,
    ayahTextUthmani: String,
    isBookmarked: Boolean,
    placeAboveAyah: Boolean,
    nightMode: Boolean,
    onReadFromHere: () -> Unit,
    onOpenTafsir: () -> Unit = {},
    onOpenIrab: () -> Unit,
    onOpenSarf: () -> Unit,
    onPlaySingleAyah: () -> Unit,
    onRepeatRange: () -> Unit,
    onCopyAyah: () -> Unit,
    onShareAyah: () -> Unit,
    onToggleBookmark: () -> Unit,
    onAddToMemorization: () -> Unit,
    onOpenInMemorization: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val surahMeta = remember(selectedAyah.surahNumber) { SurahCatalog.findSurah(selectedAyah.surahNumber) }
    val cardBg = if (nightMode) Color(0xFF0C1E18) else Color(0xFFFFFCF5)
    val ink = if (nightMode) Color(0xFFF1F7F4) else Color(0xFF10241D)
    val mutedInk = if (nightMode) Color(0xFFAFC8BE) else Color(0xFF51635B)
    val borderCol = if (nightMode) GoldBright.copy(alpha = 0.72f) else GoldPrimary.copy(alpha = 0.85f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .testTag("mushaf_ayah_contextual_menu"),
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.3.dp, borderCol)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: Surah · Ayah · Page + Sajdah badge + Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = L10n.pick(
                                    lang,
                                    "سورة ${surahMeta.nameAr} · آية ${selectedAyah.ayahNumber} · ص ${selectedAyah.pageNumber}",
                                    "${surahMeta.nameEn} · Ayah ${selectedAyah.ayahNumber} · p.${selectedAyah.pageNumber}"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (nightMode) GoldBright else EmeraldDeep,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedAyah.hasSajdah) {
                                Text(
                                    text = L10n.pick(lang, "۩ سجدة", "۩ Sajdah"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBright,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldDeep)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (ayahTextUthmani.isNotBlank()) {
                            Text(
                                text = "$ayahTextUthmani ﴿${selectedAyah.ayahNumber}﴾",
                                fontFamily = AmiriFontFamily,
                                fontSize = 15.sp,
                                color = ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mushaf_menu_close_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = L10n.pick(lang, "إغلاق القائمة", "Close menu"),
                            tint = mutedInk
                        )
                    }
                }

                // Primary Actions: Start Reading from here | Listen to this Ayah | Memorize this Ayah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onReadFromHere,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 48.dp)
                            .testTag("mushaf_menu_read_from_ayah")
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = L10n.pick(lang, "بدء القراءة", "Start Reading"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onPlaySingleAyah,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("mushaf_menu_play_single")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = L10n.pick(lang, "استماع للآية", "Listen"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenInMemorization,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("mushaf_menu_open_hifz")
                    ) {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = L10n.pick(lang, "تحفيظ", "Memorize"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            maxLines = 1
                        )
                    }
                }

                // Secondary Real Actions Row: Tafsir, I'rab, Sarf, Repeat Range, Bookmark, Add to Hifz Plan, Copy, Share
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContextualMiniActionChip(
                        icon = Icons.Rounded.ContentCopy,
                        label = L10n.pick(lang, "نسخ", "Copy"),
                        ink = ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_copy",
                        onClick = onCopyAyah
                    )
                    ContextualMiniActionChip(
                        icon = Icons.Rounded.Share,
                        label = L10n.pick(lang, "مشاركة", "Share"),
                        ink = ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_share",
                        onClick = onShareAyah
                    )
                    ContextualMiniActionChip(
                        icon = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        label = if (isBookmarked) L10n.pick(lang, "محفوظة", "Saved") else L10n.pick(lang, "مفضلة", "Favorite"),
                        ink = if (isBookmarked) GoldPrimary else ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_bookmark",
                        onClick = onToggleBookmark
                    )
                    ContextualMiniActionChip(
                        icon = Icons.Rounded.AutoStories,
                        label = L10n.pick(lang, "التفسير", "Tafsir"),
                        ink = ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_tafsir",
                        onClick = onOpenTafsir
                    )
                    ContextualMiniActionChip(
                        icon = Icons.Rounded.School,
                        label = L10n.pick(lang, "إعراب الآية", "I'rab"),
                        ink = ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_irab",
                        onClick = onOpenIrab
                    )
                    ContextualMiniActionChip(
                        icon = Icons.Rounded.Translate,
                        label = L10n.pick(lang, "الصرف", "Sarf"),
                        ink = ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_sarf",
                        onClick = onOpenSarf
                    )
                    ContextualMiniActionChip(
                        icon = Icons.Rounded.Repeat,
                        label = L10n.pick(lang, "تكرار نطاق", "Repeat Range"),
                        ink = ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_repeat_range",
                        onClick = onRepeatRange
                    )
                    ContextualMiniActionChip(
                        icon = Icons.Rounded.PlaylistAdd,
                        label = L10n.pick(lang, "خطة حفظ", "Add Hifz"),
                        ink = ink,
                        nightMode = nightMode,
                        testTag = "mushaf_menu_add_hifz",
                        onClick = onAddToMemorization
                    )
                }

                // Subtle placement indicator so screen readers know position relative to Ayah
                Text(
                    text = if (placeAboveAyah) {
                        L10n.pick(lang, "اضغط خارج الآية لإغلاق القائمة", "Tap outside the Ayah to dismiss")
                    } else {
                        L10n.pick(lang, "القائمة أسفل الآية حتى لا تحجب النص", "Positioned below Ayah to keep text clear")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedInk,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ContextualMiniActionChip(
    icon: ImageVector,
    label: String,
    ink: Color,
    nightMode: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (nightMode) Color(0xFF143127) else Color(0xFFF3EBDA)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = GoldPrimary, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            maxLines = 1
        )
    }
}

/**
 * Structured RTL Linguistic Analysis Bottom Sheet (I'rab, Sarf, Tafsir).
 *
 * - Strictly separates immutable Uthmani Quran text from linguistic annotations.
 * - Clearly displays scholarly source & verification status.
 * - Never invents I'rab or Sarf when verified records are unavailable; offers SAF offline pack import.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AyahLinguisticAnalysisSheet(
    analysis: AyahLinguisticAnalysis,
    initialTab: LinguisticSheetTab,
    onImportLinguisticPack: () -> Unit,
    onPlayAyah: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialTab, analysis.surahNumber, analysis.ayahNumber) {
        mutableIntStateOf(initialTab.index)
    }
    var focusedWordPosition by remember(analysis.surahNumber, analysis.ayahNumber) {
        mutableStateOf<Int?>(null)
    }

    val surahMeta = remember(analysis.surahNumber) { SurahCatalog.findSurah(analysis.surahNumber) }
    val visibleWords = remember(analysis.words, focusedWordPosition) {
        val pos = focusedWordPosition
        if (pos == null) analysis.words else analysis.words.filter { it.wordPosition == pos }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("ayah_linguistic_sheet")
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "التحليل اللغوي · سورة ${surahMeta.nameAr} — آية ${analysis.ayahNumber}",
                                "Linguistic Analysis · ${surahMeta.nameEn} — Ayah ${analysis.ayahNumber}"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val isVerifiedLinguistics = analysis.verificationStatus != LinguisticVerificationStatus.UNAVAILABLE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isVerifiedLinguistics) Icons.Rounded.Verified else Icons.Rounded.Info,
                                contentDescription = null,
                                tint = if (isVerifiedLinguistics) SemanticSuccess else GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = L10n.pick(lang, analysis.sourceAr, analysis.sourceEn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onPlayAyah,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = "Play Ayah", tint = GoldPrimary)
                    }
                }

                // Immutable Uthmani Ayah Text Box (strictly separated from annotations)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${analysis.exactUthmaniAyahText} ﴿${analysis.ayahNumber}﴾",
                        fontFamily = AmiriFontFamily,
                        fontSize = 21.sp,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Tabs: I'rab | Sarf | Tafsir
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("linguistic_tab_irab"),
                        text = { Text(L10n.pick(lang, "إعراب الآية", "I'rab")) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("linguistic_tab_sarf"),
                        text = { Text(L10n.pick(lang, "الصرف", "Sarf")) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("linguistic_tab_tafsir"),
                        text = { Text(L10n.pick(lang, "التفسير", "Tafsir")) }
                    )
                }

                // Interactive RTL Word Selector Strip (for I'rab and Sarf tabs)
                if (selectedTab != 2 && analysis.words.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            WordFilterChip(
                                label = L10n.pick(lang, "جميع الكلمات (${analysis.words.size})", "All (${analysis.words.size})"),
                                selected = focusedWordPosition == null,
                                onClick = { focusedWordPosition = null }
                            )
                        }
                        items(analysis.words, key = { "chip_${it.wordPosition}" }) { w ->
                            WordFilterChip(
                                label = "${w.wordPosition}. ${w.exactUthmaniWord}",
                                selected = focusedWordPosition == w.wordPosition,
                                onClick = {
                                    focusedWordPosition = if (focusedWordPosition == w.wordPosition) null else w.wordPosition
                                }
                            )
                        }
                    }
                }

                // Tab Content Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // I'RAB TAB
                            if (analysis.verificationStatus == LinguisticVerificationStatus.UNAVAILABLE) {
                                item {
                                    UnavailableLinguisticCard(
                                        modeAr = "الإعراب",
                                        modeEn = "I'rab (Grammatical Analysis)",
                                        onImportPack = onImportLinguisticPack
                                    )
                                }
                            } else {
                                if (analysis.sentenceSummaryAr.isNotBlank()) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = L10n.pick(lang, "ملخص الجملة النحوية:", "Sentence Structure Summary:"),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = noorColors.goldAccent
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = analysis.sentenceSummaryAr,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                                items(visibleWords, key = { "irab_${it.wordPosition}" }) { word ->
                                    WordIrabCard(word = word)
                                }
                            }
                        }

                        1 -> {
                            // SARF TAB
                            if (analysis.verificationStatus == LinguisticVerificationStatus.UNAVAILABLE) {
                                item {
                                    UnavailableLinguisticCard(
                                        modeAr = "التحليل الصرفي",
                                        modeEn = "Sarf (Morphological Analysis)",
                                        onImportPack = onImportLinguisticPack
                                    )
                                }
                            } else {
                                items(visibleWords, key = { "sarf_${it.wordPosition}" }) { word ->
                                    WordSarfCard(word = word)
                                }
                            }
                        }

                        else -> {
                            // TAFSIR TAB (strictly separated from I'rab/Sarf)
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = L10n.pick(lang, "التفسير الميسّر", "Tafsir Al-Muyassar"),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = analysis.tafsirMuyassarAr.ifBlank {
                                                L10n.pick(
                                                    lang,
                                                    "التفسير الميسر لهذه الآية غير محفوظ محلياً بعد. افتح السورة أثناء الاتصال بالإنترنت لحفظه دون إنترنت.",
                                                    "Tafsir Al-Muyassar for this Ayah is not cached offline yet. Open this Surah once online to cache it."
                                                )
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 24.sp
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

@Composable
private fun WordFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontFamily = AmiriFontFamily,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WordIrabCard(word: WordLinguisticAnalysis) {
    val irab = word.irab
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("irab_word_card_${word.wordPosition}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${word.wordPosition}. ${word.exactUthmaniWord}",
                    fontFamily = AmiriFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                if (irab != null && irab.grammaticalRoleEn.isNotBlank()) {
                    Text(
                        text = irab.grammaticalRoleEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (irab != null) {
                Text(
                    text = "الموقع الإعرابي: ${irab.grammaticalRoleAr}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (irab.caseEndingAr.isNotBlank()) {
                    Text(
                        text = "الحكم والعلامة: ${irab.caseEndingAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (irab.relationshipAr.isNotBlank()) {
                    Text(
                        text = "التعلّق والعلاقة: ${irab.relationshipAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (irab.explanationAr.isNotBlank()) {
                    Text(
                        text = irab.explanationAr,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "لا يتوفر سجل إعراب موثّق لهذه الكلمة في القاعدة المحلية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordSarfCard(word: WordLinguisticAnalysis) {
    val sarf = word.sarf
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sarf_word_card_${word.wordPosition}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${word.wordPosition}. ${word.exactUthmaniWord}",
                    fontFamily = AmiriFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                if (sarf != null && sarf.partOfSpeechEn.isNotBlank()) {
                    Text(
                        text = sarf.partOfSpeechEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (sarf != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SarfTagPill("الجذر", sarf.rootAr)
                    SarfTagPill("الوزن", sarf.patternAr)
                    SarfTagPill("النوع", sarf.partOfSpeechAr)
                    if (sarf.verbFormAr != "—" && sarf.verbFormAr.isNotBlank()) {
                        SarfTagPill("الباب/الصيغة", sarf.verbFormAr)
                    }
                    if (sarf.tenseAr != "—" && sarf.tenseAr.isNotBlank()) {
                        SarfTagPill("الزمن", sarf.tenseAr)
                    }
                    if (sarf.numberGenderAr != "—" && sarf.numberGenderAr.isNotBlank()) {
                        SarfTagPill("العدد والنوع", sarf.numberGenderAr)
                    }
                    if (sarf.derivationAr != "—" && sarf.derivationAr.isNotBlank()) {
                        SarfTagPill("الاشتقاق", sarf.derivationAr)
                    }
                }
                if (sarf.notesAr.isNotBlank()) {
                    Text(
                        text = sarf.notesAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "لا يتوفر سجل صرفي موثّق لهذه الكلمة في القاعدة المحلية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SarfTagPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UnavailableLinguisticCard(
    modeAr: String,
    modeEn: String,
    onImportPack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("linguistic_unavailable_notice")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = GoldPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = L10n.pick(
                        lang,
                        "بيانات $modeAr الموثّقة غير متوفرة لهذه الآية محلياً",
                        "Verified $modeEn is not yet available locally for this Ayah"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = L10n.pick(
                    lang,
                    "حفاظاً على الدقة العلمية للقرآن الكريم، لا يقوم التطبيق بتوليد أو تخمين الإعراب أو الصرف عشوائياً. يمكنك الاطلاع على التفسير الميسّر من التبويب المجاور، أو استيراد حزمة إعراب وصرف موثّقة بصيغة JSON للعمل دون إنترنت.",
                    "To preserve Quranic scholarly accuracy, NOOR never invents or guesses I'rab or Sarf. You can view Tafsir Al-Muyassar in the next tab, or import a verified local JSON linguistic pack for offline use."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onImportPack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("import_linguistic_pack_btn")
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(L10n.pick(lang, "استيراد حزمة إعراب وصرف محلية (JSON)", "Import Offline Linguistic Pack (JSON)"))
            }
        }
    }
}

/**
 * Dialog for configuring Mushaf Ayah Highlight Appearance (Color, Transparency, Animation)
 * AND managing the Exact 604-Page Glyph Coordinates DB (`ayahinfo_1024.db`).
 */
@Composable
fun MushafHighlightSettingsDialog(
    style: MushafHighlightStyle,
    coordStatus: MushafCoordinatePackStatus,
    currentPagePrecisionAr: String,
    currentPagePrecisionEn: String,
    onUpdateStyle: (MushafHighlightStyle) -> Unit,
    onDownloadGlyphDb: () -> Unit,
    onImportGlyphDb: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(L10n.pick(lang, "إعدادات التظليل وتفاعل المصحف", "Mushaf Highlight & Interaction Settings"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = L10n.pick(lang, "لون تظليل الآية والكلمة:", "Highlight Color:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MushafHighlightColor.entries.forEach { preset ->
                        val selected = style.colorPreset == preset
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onUpdateStyle(style.copy(colorPreset = preset)) }
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) preset.borderColor else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("highlight_color_${preset.key}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(preset.ayahFillColor)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = L10n.pick(lang, preset.labelAr, preset.labelEn),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Transparency Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = L10n.pick(lang, "شفافية التظليل", "Highlight Opacity"),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${(style.alpha * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = style.alpha,
                        onValueChange = { onUpdateStyle(style.copy(alpha = it)) },
                        valueRange = 0.15f..0.50f,
                        colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                    )
                }

                // Pulse Animation Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = L10n.pick(lang, "نبض لطيف للآية المتلوّة", "Subtle Pulse Animation"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = style.pulseAnimated,
                        onCheckedChange = { onUpdateStyle(style.copy(pulseAnimated = it)) }
                    )
                }

                HorizontalDivider()

                // Coordinate Precision & Exact 604-page Glyph DB Manager
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = L10n.pick(lang, "دقة إحداثيات المصحف الحالية:", "Current Page Coordinate Precision:"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = L10n.pick(lang, currentPagePrecisionAr, currentPagePrecisionEn),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (coordStatus.hasExactGlyphDb) SemanticSuccess else GoldPrimary
                    )
                    if (coordStatus.statusAr.isNotBlank()) {
                        Text(
                            text = L10n.pick(lang, coordStatus.statusAr, coordStatus.statusEn),
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticSuccess
                        )
                    }
                    if (coordStatus.errorAr.isNotBlank()) {
                        Text(
                            text = L10n.pick(lang, coordStatus.errorAr, coordStatus.errorEn),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onDownloadGlyphDb,
                        enabled = !coordStatus.isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            L10n.pick(
                                lang,
                                if (coordStatus.hasExactGlyphDb) "تحديث قاعدة إحداثيات المصحف (٦٠٤ ص)" else "تحميل إحداثيات الحروف الدقيقة (٦٠٤ ص)",
                                if (coordStatus.hasExactGlyphDb) "Refresh Exact Glyph DB (604p)" else "Download Exact Glyph DB (604p)"
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = onImportGlyphDb,
                        enabled = !coordStatus.isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(L10n.pick(lang, "استيراد ملف ayahinfo_1024.db محلياً", "Import Local ayahinfo_1024.db / ZIP"))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "تم", "Done"))
            }
        }
    )
}

/**
 * Dialog allowing the user to continuously repeat an exact Ayah range (From Ayah -> To Ayah)
 * directly from the Mushaf long-press contextual menu.
 */
@Composable
fun MushafRepeatRangeDialog(
    surahNumber: Int,
    initialFromAyah: Int,
    onConfirmRange: (fromAyah: Int, toAyah: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val surah = remember(surahNumber) { SurahCatalog.findSurah(surahNumber) }
    val maxAyah = surah.verseCount.coerceAtLeast(1)
    var fromText by remember(initialFromAyah) { mutableStateOf(initialFromAyah.coerceIn(1, maxAyah).toString()) }
    var toText by remember(initialFromAyah, maxAyah) {
        mutableStateOf((initialFromAyah + 2).coerceAtMost(maxAyah).toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                L10n.pick(
                    lang,
                    "تكرار نطاق آيات · سورة ${surah.nameAr}",
                    "Repeat Ayah Range · ${surah.nameEn}"
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = L10n.pick(
                        lang,
                        "سيتم تلاوة وتكرار الآيات المختارة تلقائياً مع تظليل كل آية متلوّة.",
                        "Selected Ayahs will loop continuously with live Ayah highlighting."
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = it.filter(Char::isDigit).take(3) },
                        label = { Text(L10n.pick(lang, "من آية (1-$maxAyah)", "From (1-$maxAyah)")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = it.filter(Char::isDigit).take(3) },
                        label = { Text(L10n.pick(lang, "إلى آية (1-$maxAyah)", "To (1-$maxAyah)")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = (fromText.toIntOrNull() ?: initialFromAyah).coerceIn(1, maxAyah)
                    val t = (toText.toIntOrNull() ?: f).coerceIn(f, maxAyah)
                    onConfirmRange(f, t)
                }
            ) {
                Text(L10n.pick(lang, "بدء التكرار", "Start Loop"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel"))
            }
        }
    )
}
