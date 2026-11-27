package com.example.data.quran

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pixel-exact visual line calibrator specifically for Madinah Mushaf opening Pages 1 & 2
 * (Surah Al-Fatihah and opening of Surah Al-Baqarah).
 *
 * Different editions of `page001.png` / `page002.png` place their 1 Surah banner + 7 calligraphy
 * lines at very different vertical positions (e.g., King Fahd `width_1024` puts lines 1..8 in the
 * upper half with blank paper below, whereas ornate gilded scans place them in a centered inner
 * frame). Scanning the actual decoded bitmap's ink projection locks every line (`2..8`) directly
 * onto the real Arabic calligraphy pixels so highlights NEVER drift onto blank paper!
 */
object OpeningPageBitmapCalibrator {

    internal data class DetectedInkBand(
        val topPx: Int,
        val bottomPx: Int,
        val leftPx: Int,
        val rightPx: Int,
        val avgInkDensity: Float
    ) {
        val heightPx: Int get() = (bottomPx - topPx + 1).coerceAtLeast(1)
        val centerY: Float get() = (topPx + bottomPx) * 0.5f
    }

    /**
     * Verifies whether `layout` (for Page 1 or Page 2) aligns with the real Arabic calligraphy ink
     * inside `bitmap`. If the layout is non-exact OR misaligned (highlights falling on blank paper),
     * remaps every line segment, word box, and Ayah end-marker onto the exact pixel-detected lines.
     */
    fun calibrateIfNeeded(
        layout: MushafPageLayout,
        bitmap: Bitmap
    ): MushafPageLayout {
        if (layout.pageNumber != 1 && layout.pageNumber != 2) return layout
        if (!layout.hasVerifiedCoordinates || layout.ayahs.isEmpty()) return layout
        val w = bitmap.width
        val h = bitmap.height
        if (w < 64 || h < 64) return layout

        // If layout came from exact SQLite glyph DB AND already sits squarely over real ink pixels
        // across all Ayahs, keep the exact glyph DB coordinates untouched!
        if (layout.precision == CoordinatePrecision.EXACT_GLYPH_DB &&
            isLayoutAlignedWithBitmapInk(layout, bitmap)
        ) {
            return layout
        }

        val detectedSevenLines = detectSevenOpeningTextLines(bitmap) ?: return layout
        return remapOpeningLayoutToDetectedLines(
            layout = layout,
            detectedLines = detectedSevenLines,
            bitmapWidth = w,
            bitmapHeight = h
        )
    }

    /**
     * Checks whether at least 85% of the layout's Ayah line boxes actually cover real dark ink
     * inside `bitmap` (detecting vertical drift onto blank paper even when using a mismatched DB).
     */
    internal fun isLayoutAlignedWithBitmapInk(layout: MushafPageLayout, bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 32 || h < 32) return true
        val segments = layout.ayahs.flatMap { it.lineRegions }
        if (segments.isEmpty()) return true

        var coveredCount = 0
        val rowBuf = IntArray(w)
        for (seg in segments) {
            val topY = (seg.bounds.top * h).toInt().coerceIn(0, h - 1)
            val botY = (seg.bounds.bottom * h).toInt().coerceIn(topY, h - 1)
            val leftX = (seg.bounds.left * w).toInt().coerceIn(0, w - 1)
            val rightX = (seg.bounds.right * w).toInt().coerceIn(leftX, w - 1)
            val stepY = maxOf(1, (botY - topY) / 8)
            val stepX = maxOf(1, (rightX - leftX) / 24)
            var samples = 0
            var ink = 0
            var y = topY
            while (y <= botY) {
                bitmap.getPixels(rowBuf, 0, w, 0, y, w, 1)
                var x = leftX
                while (x <= rightX) {
                    samples++
                    if (isInkPixel(rowBuf[x])) ink++
                    x += stepX
                }
                y += stepY
            }
            if (samples > 0 && (ink.toFloat() / samples.toFloat()) >= 0.018f) {
                coveredCount++
            }
        }
        return coveredCount.toFloat() / segments.size.toFloat() >= 0.85f
    }

    /**
     * Scans `bitmap` horizontally and vertically to locate the exact 7 Quran text lines (`lines 2..8`)
     * on Page 1 (Al-Fatihah) or Page 2 (Al-Baqarah 2:1-5).
     */
    internal fun detectSevenOpeningTextLines(bitmap: Bitmap): List<DetectedInkBand>? {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 64 || h < 64) return null

        val yStep = if (h >= 800) 2 else 1
        val sampledRows = (h + yStep - 1) / yStep
        val xStep = maxOf(1, w / 256)

        // Scan central text corridor (24%..76% width) to find vertical line bands cleanly even if
        // an edition has decorative left/right page borders!
        val centerStartX = (w * 0.24f).toInt().coerceIn(0, w - 1)
        val centerEndX = (w * 0.76f).toInt().coerceIn(centerStartX, w - 1)
        val centerSampleSpan = ((centerEndX - centerStartX) / xStep).coerceAtLeast(1)

        val rowCenterInk = IntArray(sampledRows)
        val rowBuf = IntArray(w)

        for (r in 0 until sampledRows) {
            val y = (r * yStep).coerceAtMost(h - 1)
            bitmap.getPixels(rowBuf, 0, w, 0, y, w, 1)
            var ink = 0
            var x = centerStartX
            while (x <= centerEndX) {
                if (isInkPixel(rowBuf[x])) ink++
                x += xStep
            }
            rowCenterInk[r] = ink
        }

        // Build contiguous vertical ink runs from central corridor
        val rawRuns = mutableListOf<IntRange>()
        var runStart = -1
        for (r in 0 until sampledRows) {
            val active = rowCenterInk[r] >= 2
            if (active && runStart < 0) {
                runStart = r
            } else if (!active && runStart >= 0) {
                rawRuns += (runStart * yStep)..((r - 1) * yStep)
                runStart = -1
            }
        }
        if (runStart >= 0) {
            rawRuns += (runStart * yStep)..((sampledRows - 1) * yStep)
        }
        if (rawRuns.isEmpty()) return null

        // Merge tiny diacritic gaps (< 0.75% of page height) so tashkeel stays attached to its line
        val diacriticMergeGapPx = maxOf(3, (h * 0.0075f).toInt())
        val mergedRuns = mutableListOf<IntRange>()
        for (run in rawRuns) {
            if (mergedRuns.isEmpty()) {
                mergedRuns += run
            } else {
                val prev = mergedRuns.last()
                if (run.first - prev.last <= diacriticMergeGapPx) {
                    mergedRuns[mergedRuns.lastIndex] = prev.first..run.last
                } else {
                    mergedRuns += run
                }
            }
        }

        // Attach tiny stray diacritic specks (< 1.1% page height) to their nearest line if close (< 2.2% H)
        val minMajorHeightPx = maxOf(6, (h * 0.011f).toInt())
        val maxAttachDistPx = maxOf(10, (h * 0.022f).toInt())
        val majorRuns = mutableListOf<IntRange>()
        val tinySpecks = mutableListOf<IntRange>()
        for (run in mergedRuns) {
            if ((run.last - run.first + 1) >= minMajorHeightPx) {
                majorRuns += run
            } else {
                tinySpecks += run
            }
        }
        if (majorRuns.isEmpty()) return null

        for (speck in tinySpecks) {
            var bestIdx = -1
            var bestDist = Int.MAX_VALUE
            for (i in majorRuns.indices) {
                val mr = majorRuns[i]
                val dist = when {
                    speck.last < mr.first -> mr.first - speck.last
                    speck.first > mr.last -> speck.first - mr.last
                    else -> 0
                }
                if (dist < bestDist) {
                    bestDist = dist
                    bestIdx = i
                }
            }
            if (bestIdx >= 0 && bestDist <= maxAttachDistPx) {
                val mr = majorRuns[bestIdx]
                majorRuns[bestIdx] = minOf(mr.first, speck.first)..maxOf(mr.last, speck.last)
            }
        }

        // Filter out tiny isolated footer page numbers at extreme bottom (> 91% height)
        val candidateRuns = majorRuns.filterNot { run ->
            val topRatio = run.first.toFloat() / h.toFloat()
            val heightRatio = (run.last - run.first + 1).toFloat() / h.toFloat()
            topRatio > 0.91f && heightRatio < 0.032f
        }
        if (candidateRuns.isEmpty()) return null

        // Determine safe horizontal scan boundaries (exclude outer decorative vertical frame pillars if present)
        val (safeLeftX, safeRightX) = detectSafeHorizontalCorridor(bitmap, candidateRuns, w, h)

        val bands = candidateRuns.map { run ->
            measureHorizontalBoundsForRun(
                bitmap = bitmap,
                topY = run.first,
                bottomY = run.last,
                safeLeftX = safeLeftX,
                safeRightX = safeRightX,
                centerSampleSpan = centerSampleSpan,
                rowCenterInk = rowCenterInk,
                yStep = yStep
            )
        }

        // Select the exact 7 text lines (`lines 2..8`):
        // - Standard `width_1024` has 8 bands (1 Surah banner + 7 text lines) -> bands[1..7]
        // - Ornate framed scans have >= 9 bands (top frame + banner + 7 text lines + bottom frame)
        //   -> highest-scoring contiguous 7-band window with uniform line pitch & calligraphy density!
        // - If tightly spaced lines merged into fewer than 7 bands -> divide measured text span cleanly!
        return when {
            bands.size == 8 -> bands.subList(1, 8)
            bands.size > 8 -> pickBestSevenLineWindow(bands, h)
            bands.size == 7 -> {
                // If band[0] is clearly a tall/dense Surah banner box (> 1.45x median height of remaining 6),
                // distribute bands[1..6]'s span across 7 lines; otherwise all 7 bands are directly lines 2..8!
                val tailHeights = bands.drop(1).map { it.heightPx }.sorted()
                val medianTailH = tailHeights[tailHeights.size / 2].toFloat().coerceAtLeast(1f)
                if (bands[0].heightPx > medianTailH * 1.55f && bands[0].avgInkDensity > 0.35f) {
                    subdivideSpanIntoSevenLines(
                        topPx = bands[1].topPx,
                        bottomPx = bands.last().bottomPx,
                        bitmap = bitmap,
                        safeLeftX = safeLeftX,
                        safeRightX = safeRightX
                    )
                } else {
                    bands
                }
            }
            else -> {
                // Fewer than 7 bands (e.g. tight tashkeel merged adjacent lines): use actual ink top..bottom
                // with slot 0 = Surah banner and slots 1..7 = the 7 text lines (`lines 2..8`)!
                val totalTop = bands.first().topPx
                val totalBottom = bands.last().bottomPx
                val span = (totalBottom - totalTop).coerceAtLeast(14)
                if (span < (h * 0.20f).toInt()) return null
                // If the first band is isolated cleanly as the Surah banner (height ~ 1/8 of span),
                // start text lines right below it; otherwise reserve top 1/8 for Surah banner.
                val textStartPx = if (bands.size >= 2 && bands[0].heightPx <= (span * 0.22f).toInt()) {
                    bands[1].topPx
                } else {
                    totalTop + (span / 8)
                }
                subdivideSpanIntoSevenLines(
                    topPx = textStartPx,
                    bottomPx = totalBottom,
                    bitmap = bitmap,
                    safeLeftX = safeLeftX,
                    safeRightX = safeRightX
                )
            }
        }
    }

    private fun pickBestSevenLineWindow(bands: List<DetectedInkBand>, pageHeight: Int): List<DetectedInkBand> {
        var bestWindow = bands.subList(1, 8)
        var bestScore = Float.NEGATIVE_INFINITY

        for (start in 0..(bands.size - 7)) {
            val window = bands.subList(start, start + 7)
            val totalSpanRatio = (window.last().bottomPx - window.first().topPx).toFloat() / pageHeight.toFloat()
            if (totalSpanRatio < 0.22f || totalSpanRatio > 0.85f) continue

            // 1. Pitch regularity (real Mushaf text lines sit on a strictly regular baseline grid)
            val pitches = FloatArray(6) { i -> (window[i + 1].centerY - window[i].centerY).coerceAtLeast(1f) }
            val avgPitch = pitches.average().toFloat().coerceAtLeast(1f)
            val pitchStd = sqrt(pitches.map { (it - avgPitch) * (it - avgPitch) }.average().toFloat())
            val pitchCv = pitchStd / avgPitch

            // 2. Height consistency (exclude tall decorative borders & thin frame rules)
            val heights = window.map { it.heightPx.toFloat() }
            val avgH = heights.average().toFloat().coerceAtLeast(1f)
            val hStd = sqrt(heights.map { (it - avgH) * (it - avgH) }.average().toFloat())
            val heightCv = hStd / avgH

            // 3. Calligraphy density (Arabic lines have moderate density, not solid 80%+ frame bars)
            val solidFramePenalty = window.count { it.avgInkDensity > 0.72f || it.heightPx > pageHeight * 0.12f } * 1.5f

            // 4. Prefer having a Surah header band directly above the 7 lines
            val headerAboveBonus = if (start >= 1) 0.35f else 0f

            val score = 3.0f - (pitchCv * 3.5f) - (heightCv * 1.6f) - solidFramePenalty + headerAboveBonus
            if (score > bestScore) {
                bestScore = score
                bestWindow = window
            }
        }
        return bestWindow
    }

    private fun subdivideSpanIntoSevenLines(
        topPx: Int,
        bottomPx: Int,
        bitmap: Bitmap,
        safeLeftX: Int,
        safeRightX: Int
    ): List<DetectedInkBand> {
        val span = (bottomPx - topPx).coerceAtLeast(7)
        val lineH = span / 7f
        return List(7) { idx ->
            val lTop = (topPx + idx * lineH).toInt().coerceIn(0, bitmap.height - 1)
            val lBot = (topPx + (idx + 1) * lineH).toInt().coerceIn(lTop, bitmap.height - 1)
            val (lx, rx) = scanHorizontalInkExtents(bitmap, lTop, lBot, safeLeftX, safeRightX)
            DetectedInkBand(lTop, lBot, lx, rx, avgInkDensity = 0.25f)
        }
    }

    /**
     * Checks blank gap rows between detected ink runs to see if continuous decorative side pillars
     * exist near the left/right edges. If so, restricts horizontal measurement strictly inside the
     * clear inner text frame!
     */
    private fun detectSafeHorizontalCorridor(
        bitmap: Bitmap,
        runs: List<IntRange>,
        w: Int,
        h: Int
    ): Pair<Int, Int> {
        val gapRows = mutableListOf<Int>()
        for (i in 0 until runs.lastIndex) {
            val midY = (runs[i].last + runs[i + 1].first) / 2
            if (midY in 0 until h) gapRows += midY
        }
        val defaultLeft = (w * 0.02f).toInt()
        val defaultRight = (w * 0.98f).toInt()
        if (gapRows.size < 3) return defaultLeft to defaultRight

        val rowBuf = IntArray(w)
        val colGapInkHits = IntArray(w)
        for (y in gapRows) {
            bitmap.getPixels(rowBuf, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                if (isInkPixel(rowBuf[x])) colGapInkHits[x]++
            }
        }
        val pillarThreshold = (gapRows.size * 0.65f).toInt().coerceAtLeast(2)
        val maxSideBorder = (w * 0.22f).toInt()

        var safeLeft = defaultLeft
        for (x in defaultLeft..maxSideBorder) {
            if (colGapInkHits[x] >= pillarThreshold) {
                safeLeft = (x + (w * 0.012f).toInt()).coerceAtMost(maxSideBorder)
            }
        }
        var safeRight = defaultRight
        for (x in defaultRight downTo (w - maxSideBorder)) {
            if (colGapInkHits[x] >= pillarThreshold) {
                safeRight = (x - (w * 0.012f).toInt()).coerceAtLeast(w - maxSideBorder)
            }
        }
        return if (safeRight - safeLeft >= (w * 0.40f).toInt()) {
            safeLeft to safeRight
        } else {
            defaultLeft to defaultRight
        }
    }

    private fun measureHorizontalBoundsForRun(
        bitmap: Bitmap,
        topY: Int,
        bottomY: Int,
        safeLeftX: Int,
        safeRightX: Int,
        centerSampleSpan: Int,
        rowCenterInk: IntArray,
        yStep: Int
    ): DetectedInkBand {
        val (lx, rx) = scanHorizontalInkExtents(bitmap, topY, bottomY, safeLeftX, safeRightX)
        val rStart = (topY / yStep).coerceIn(0, rowCenterInk.lastIndex)
        val rEnd = (bottomY / yStep).coerceIn(rStart, rowCenterInk.lastIndex)
        var sumDensity = 0f
        var count = 0
        for (r in rStart..rEnd) {
            sumDensity += rowCenterInk[r].toFloat() / centerSampleSpan.toFloat()
            count++
        }
        return DetectedInkBand(
            topPx = topY,
            bottomPx = bottomY,
            leftPx = lx,
            rightPx = rx,
            avgInkDensity = if (count > 0) sumDensity / count else 0f
        )
    }

    private fun scanHorizontalInkExtents(
        bitmap: Bitmap,
        topY: Int,
        bottomY: Int,
        safeLeftX: Int,
        safeRightX: Int
    ): Pair<Int, Int> {
        val w = bitmap.width
        val h = bitmap.height
        val y0 = topY.coerceIn(0, h - 1)
        val y1 = bottomY.coerceIn(y0, h - 1)
        val yStep = maxOf(1, (y1 - y0) / 18)
        val rowBuf = IntArray(w)
        var minX = safeRightX
        var maxX = safeLeftX
        var found = false

        var y = y0
        while (y <= y1) {
            bitmap.getPixels(rowBuf, 0, w, 0, y, w, 1)
            for (x in safeLeftX..safeRightX) {
                if (isInkPixel(rowBuf[x])) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    found = true
                }
            }
            y += yStep
        }
        return if (found && maxX > minX + 10) {
            minX to maxX
        } else {
            safeLeftX to safeRightX
        }
    }

    private fun remapOpeningLayoutToDetectedLines(
        layout: MushafPageLayout,
        detectedLines: List<DetectedInkBand>,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): MushafPageLayout {
        if (detectedLines.size != 7) return layout
        val w = bitmapWidth.toFloat()
        val h = bitmapHeight.toFloat()

        val allLineNums = layout.ayahs.flatMap { it.lineRegions }.map { it.lineNumber }
        val minAyahLine = allLineNums.minOrNull() ?: return layout
        // On Page 1 (Al-Fatihah), Ayah 1:1 IS line 0 (Bismillah);
        // On Page 2 (Al-Baqarah), line 0 is the unnumbered Bismillah header, so Ayah 2:1 starts at line 1!
        val baseLineNum = if (layout.pageNumber == 2) minAyahLine - 1 else minAyahLine

        // Compute existing normalized horizontal span `[oldMinX..oldMaxX]` per lineNumber
        // (including both text regions and decorative end-marker rosettes!)
        val oldMinXByLine = mutableMapOf<Int, Float>()
        val oldMaxXByLine = mutableMapOf<Int, Float>()
        for (ayah in layout.ayahs) {
            for (seg in ayah.lineRegions) {
                oldMinXByLine[seg.lineNumber] = minOf(oldMinXByLine[seg.lineNumber] ?: 1f, seg.bounds.left)
                oldMaxXByLine[seg.lineNumber] = maxOf(oldMaxXByLine[seg.lineNumber] ?: 0f, seg.bounds.right)
            }
            val marker = ayah.endMarkerBounds
            val finalLine = ayah.lineRegions.maxOfOrNull { it.lineNumber }
            if (marker != null && finalLine != null) {
                oldMinXByLine[finalLine] = minOf(oldMinXByLine[finalLine] ?: 1f, marker.left)
                oldMaxXByLine[finalLine] = maxOf(oldMaxXByLine[finalLine] ?: 0f, marker.right)
            }
        }

        // Convert each detected pixel band into normalized `[0..1]` rect with gentle 4% vertical breathing
        val lineRectsNorm = detectedLines.map { band ->
            val padY = (band.heightPx * 0.04f).coerceIn(1f, h * 0.008f)
            val padX = (w * 0.004f).coerceIn(1f, 6f)
            NormalizedRect(
                left = ((band.leftPx - padX) / w).coerceIn(0f, 1f),
                top = ((band.topPx - padY) / h).coerceIn(0f, 1f),
                right = ((band.rightPx + padX) / w).coerceIn(0f, 1f),
                bottom = ((band.bottomPx + padY) / h).coerceIn(0f, 1f)
            )
        }

        fun remapRect(lineNum: Int, oldRect: NormalizedRect): NormalizedRect {
            val slot = (lineNum - baseLineNum).coerceIn(0, 6)
            val target = lineRectsNorm[slot]
            val oldMinX = oldMinXByLine[lineNum] ?: oldRect.left
            val oldMaxX = oldMaxXByLine[lineNum] ?: oldRect.right
            val oldSpan = (oldMaxX - oldMinX).coerceAtLeast(0.02f)
            val targetSpan = (target.right - target.left).coerceAtLeast(0.02f)
            val relL = ((oldRect.left - oldMinX) / oldSpan).coerceIn(0f, 1f)
            val relR = ((oldRect.right - oldMinX) / oldSpan).coerceIn(relL, 1f)
            return NormalizedRect(
                left = (target.left + relL * targetSpan).coerceIn(0f, 1f),
                top = target.top,
                right = (target.left + relR * targetSpan).coerceIn(0f, 1f),
                bottom = target.bottom
            )
        }

        val remappedAyahs = layout.ayahs.map { ayah ->
            val newLines = ayah.lineRegions.map { seg ->
                seg.copy(bounds = remapRect(seg.lineNumber, seg.bounds))
            }
            val newWords = ayah.words.map { wr ->
                wr.copy(bounds = remapRect(wr.lineNumber, wr.bounds))
            }
            val finalLineNum = ayah.lineRegions.maxOfOrNull { it.lineNumber }
            val newEndMarker = if (ayah.endMarkerBounds != null && finalLineNum != null) {
                remapRect(finalLineNum, ayah.endMarkerBounds)
            } else {
                null
            }
            ayah.copy(
                lineRegions = newLines,
                words = newWords,
                endMarkerBounds = newEndMarker
            )
        }

        return layout.copy(
            imageWidth = bitmapWidth,
            imageHeight = bitmapHeight,
            ayahs = remappedAyahs
        )
    }

    private fun isInkPixel(px: Int): Boolean {
        val alpha = (px ushr 24) and 0xFF
        if (alpha < 48) return false
        val r = (px ushr 16) and 0xFF
        val g = (px ushr 8) and 0xFF
        val b = px and 0xFF
        val luma = (r * 299 + g * 587 + b * 114) / 1000
        return luma < 175
    }
}
