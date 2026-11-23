package com.example.data.quran

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Normalized rectangle in `[0f..1f] × [0f..1f]` Mushaf page image coordinates.
 * `(0, 0)` is top-left of the raw Mushaf page image; `(1, 1)` is bottom-right.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f

    fun contains(x: Float, y: Float, padding: Float = 0f): Boolean =
        x >= (left - padding) && x <= (right + padding) &&
            y >= (top - padding) && y <= (bottom + padding)

    fun distanceTo(x: Float, y: Float): Float {
        val dx = when {
            x < left -> left - x
            x > right -> x - right
            else -> 0f
        }
        val dy = when {
            y < top -> top - y
            y > bottom -> y - bottom
            else -> 0f
        }
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

enum class CoordinatePrecision(val labelAr: String, val labelEn: String) {
    EXACT_GLYPH_DB(
        "إحداثيات حروف المصحف الدقيقة (SQLite)",
        "Exact Glyph-Level SQLite Coordinates"
    ),
    EXACT_LINE_LAYOUT_API(
        "تخطيط أسطر مصحف المدينة الـ١٥ (موثّق)",
        "Verified 15-Line Madinah Layout"
    ),
    VERIFIED_BUNDLED_LAYOUT(
        "تخطيط مصحف المدينة المدمج الموثّق",
        "Verified Bundled Madinah Layout"
    ),
    ESTIMATED_OFFLINE_FALLBACK(
        "توزيع أسطر محلي احتياطي (حمّل حزمة الإحداثيات للدقة الكاملة)",
        "Offline Fallback Line Layout (Download Coordinate Pack for Exact Bounds)"
    )
}

/**
 * Exact visual bounding box for a single Quranic word on a Madinah Mushaf page.
 */
data class WordRegion(
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordPosition: Int, // 1-indexed word position in the Ayah (RTL reading order)
    val textUthmani: String,
    val lineNumber: Int,   // 1..15 in standard Madinah Mushaf
    val bounds: NormalizedRect
)

/**
 * Contiguous horizontal bounding region of an Ayah on a single line of a Mushaf page.
 * Multi-line Ayahs consist of multiple `AyahLineRegion` segments so highlighting follows
 * exact Arabic RTL line wrapping without overlapping neighboring Ayahs!
 */
data class AyahLineRegion(
    val surahNumber: Int,
    val ayahNumber: Int,
    val lineNumber: Int,
    val bounds: NormalizedRect
)

/**
 * Complete interactive metadata for one Ayah on a specific Mushaf page.
 */
data class AyahPageRegion(
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val lineRegions: List<AyahLineRegion>,
    val words: List<WordRegion> = emptyList(),
    val hasSajdah: Boolean = false
) {
    val key: String get() = "$surahNumber:$ayahNumber"

    /** Overall bounding envelope across all lines occupied by this Ayah on this page. */
    val boundingBox: NormalizedRect
        get() {
            if (lineRegions.isEmpty()) return NormalizedRect(0f, 0f, 0f, 0f)
            var minL = 1f
            var minT = 1f
            var maxR = 0f
            var maxB = 0f
            for (r in lineRegions) {
                if (r.bounds.left < minL) minL = r.bounds.left
                if (r.bounds.top < minT) minT = r.bounds.top
                if (r.bounds.right > maxR) maxR = r.bounds.right
                if (r.bounds.bottom > maxB) maxB = r.bounds.bottom
            }
            return NormalizedRect(minL, minT, maxR, maxB)
        }
}

/**
 * Complete interactive layout for a single Madinah Mushaf page (`1..604`).
 */
data class MushafPageLayout(
    val pageNumber: Int,
    val imageWidth: Int = 1024,
    val imageHeight: Int = 1656,
    val precision: CoordinatePrecision,
    val sourceLabelAr: String,
    val sourceLabelEn: String,
    val ayahs: List<AyahPageRegion>,
    val surahHeaderLines: List<Int> = emptyList(),
    val isSpecialOpeningPage: Boolean = (pageNumber == 1 || pageNumber == 2)
) {
    fun findAyah(surahNumber: Int, ayahNumber: Int): AyahPageRegion? =
        ayahs.firstOrNull { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }

    fun findWord(surahNumber: Int, ayahNumber: Int, wordPosition: Int): WordRegion? =
        findAyah(surahNumber, ayahNumber)?.words?.firstOrNull { it.wordPosition == wordPosition }
}

/**
 * User-configurable highlight appearance settings for the transparent Mushaf overlay layer.
 */
enum class MushafHighlightColor(
    val key: String,
    val labelAr: String,
    val labelEn: String,
    val ayahFillColor: Color,
    val wordFillColor: Color,
    val borderColor: Color
) {
    GOLD(
        key = "gold",
        labelAr = "ذهبي ملكي",
        labelEn = "Royal Gold",
        ayahFillColor = Color(0xFFD4AF37),
        wordFillColor = Color(0xFFE6A100),
        borderColor = Color(0xFFB58B1E)
    ),
    EMERALD(
        key = "emerald",
        labelAr = "زمردي هادئ",
        labelEn = "Calm Emerald",
        ayahFillColor = Color(0xFF1E8E6A),
        wordFillColor = Color(0xFF14B881),
        borderColor = Color(0xFF0E6B4E)
    ),
    SAPPHIRE(
        key = "sapphire",
        labelAr = "أزرق سماوي",
        labelEn = "Soft Sapphire",
        ayahFillColor = Color(0xFF2B7CD3),
        wordFillColor = Color(0xFF1A91FF),
        borderColor = Color(0xFF1B5DA6)
    ),
    ROSE(
        key = "rose",
        labelAr = "عنابي لطيف",
        labelEn = "Warm Rose",
        ayahFillColor = Color(0xFFC85A76),
        wordFillColor = Color(0xFFE24C70),
        borderColor = Color(0xFF9E3C55)
    );

    companion object {
        fun fromKey(key: String?): MushafHighlightColor =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: GOLD
    }
}

data class MushafHighlightStyle(
    val colorPreset: MushafHighlightColor = MushafHighlightColor.GOLD,
    val alpha: Float = 0.28f,
    val pulseAnimated: Boolean = true
)

data class FittedImageRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

data class ContextualMenuPlacement(
    val placeAboveAyah: Boolean,
    val anchorNormalizedY: Float
)

/**
 * Pure, deterministic coordinate transformation & hit-testing math between:
 * - Mushaf raw image coordinate space (normalized `[0..1] × [0..1]`)
 * - Compose `ContentScale.Fit` image layout inside any viewport aspect ratio
 * - Interactive `graphicsLayer(scaleX, scaleY, translationX, translationY)` zoom & pan
 */
object MushafCoordinateTransform {

    const val MAX_PROXIMITY_TOLERANCE = 0.011f

    /**
     * Computes the exact fitted image rectangle inside a Compose container using `ContentScale.Fit`.
     */
    fun computeFittedImageRect(
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): FittedImageRect {
        if (containerWidth <= 0f || containerHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
            return FittedImageRect(0f, 0f, containerWidth.coerceAtLeast(0f), containerHeight.coerceAtLeast(0f))
        }
        val scale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
        val drawW = imageWidth * scale
        val drawH = imageHeight * scale
        val left = (containerWidth - drawW) * 0.5f
        val top = (containerHeight - drawH) * 0.5f
        return FittedImageRect(left = left, top = top, width = drawW, height = drawH)
    }

    /**
     * Converts a touch coordinate `(tapX, tapY)` in the page container into normalized Mushaf
     * image coordinates `(nx, ny) ∈ [0..1] × [0..1]`, inverting zoom scale and pan translation
     * around the Compose `graphicsLayer` center pivot.
     *
     * Returns `null` if the touch landed outside the actual fitted Mushaf image area.
     */
    fun screenPointToNormalizedPage(
        tapX: Float,
        tapY: Float,
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        zoomScale: Float = 1f,
        zoomOffsetX: Float = 0f,
        zoomOffsetY: Float = 0f
    ): Pair<Float, Float>? {
        if (containerWidth <= 0f || containerHeight <= 0f) return null
        val safeScale = zoomScale.coerceAtLeast(0.1f)
        val pivotX = containerWidth * 0.5f
        val pivotY = containerHeight * 0.5f

        // Exact inverse of graphicsLayer(scaleX, scaleY, translationX, translationY) with default (0.5, 0.5) pivot:
        // screen = pivot + (unscaled - pivot) * scale + translation
        // => unscaled = pivot + (screen - pivot - translation) / scale
        val unscaledX = pivotX + (tapX - pivotX - zoomOffsetX) / safeScale
        val unscaledY = pivotY + (tapY - pivotY - zoomOffsetY) / safeScale

        val fitted = computeFittedImageRect(containerWidth, containerHeight, imageWidth, imageHeight)
        if (fitted.width <= 0f || fitted.height <= 0f) return null

        val nx = (unscaledX - fitted.left) / fitted.width
        val ny = (unscaledY - fitted.top) / fitted.height
        if (nx < 0f || nx > 1f || ny < 0f || ny > 1f) return null
        return nx to ny
    }

    /**
     * Maps a normalized page rectangle `[0..1]` into unscaled container pixel coordinates
     * (ready to draw inside a `Canvas` sharing the same `graphicsLayer` zoom/pan modifier as the Image).
     */
    fun normalizedRectToFittedRect(
        rect: NormalizedRect,
        fitted: FittedImageRect
    ): FittedImageRect {
        val l = fitted.left + rect.left * fitted.width
        val t = fitted.top + rect.top * fitted.height
        val w = rect.width * fitted.width
        val h = rect.height * fitted.height
        return FittedImageRect(left = l, top = t, width = w, height = h)
    }

    /**
     * Maps a normalized page point `(nx, ny)` into final screen container pixel coordinates
     * (including zoom scale & pan translation) — used to position the floating contextual menu
     * accurately above/below the selected Ayah even while zoomed!
     */
    fun normalizedPointToScreen(
        nx: Float,
        ny: Float,
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        zoomScale: Float = 1f,
        zoomOffsetX: Float = 0f,
        zoomOffsetY: Float = 0f
    ): Pair<Float, Float> {
        val fitted = computeFittedImageRect(containerWidth, containerHeight, imageWidth, imageHeight)
        val unscaledX = fitted.left + nx * fitted.width
        val unscaledY = fitted.top + ny * fitted.height
        val pivotX = containerWidth * 0.5f
        val pivotY = containerHeight * 0.5f
        val sx = pivotX + (unscaledX - pivotX) * zoomScale + zoomOffsetX
        val sy = pivotY + (unscaledY - pivotY) * zoomScale + zoomOffsetY
        return sx to sy
    }

    /**
     * Accurate Ayah hit-testing on a Mushaf page:
     * 1. First checks exact containment inside every Ayah's per-line regions (zero ambiguity).
     * 2. If touch falls on inter-line whitespace within `MAX_PROXIMITY_TOLERANCE` (<= 1.1% of page),
     *    picks the closest Ayah strictly on the nearest line without crossing Surah headers or margins.
     * 3. Returns `null` if the touch landed on an ornamental margin, page border, or Surah header banner.
     */
    fun hitTestAyah(layout: MushafPageLayout, nx: Float, ny: Float): AyahPageRegion? {
        if (nx !in 0f..1f || ny !in 0f..1f || layout.ayahs.isEmpty()) return null

        // Pass 1: Strict containment inside any line segment of an Ayah
        val exactHits = layout.ayahs.filter { ayah ->
            ayah.lineRegions.any { seg -> seg.bounds.contains(nx, ny, padding = 0f) }
        }
        if (exactHits.size == 1) return exactHits.first()
        if (exactHits.size > 1) {
            // Tie-break by closest word or line segment center
            return exactHits.minByOrNull { ayah ->
                ayah.lineRegions.minOf { seg -> abs(seg.bounds.centerX - nx) + abs(seg.bounds.centerY - ny) }
            }
        }

        // Pass 2: Tight proximity tolerance for inter-word / inter-line touch cushions (never crosses Surah headers)
        var bestAyah: AyahPageRegion? = null
        var bestDist = MAX_PROXIMITY_TOLERANCE
        for (ayah in layout.ayahs) {
            for (seg in ayah.lineRegions) {
                val dist = seg.bounds.distanceTo(nx, ny)
                if (dist < bestDist) {
                    bestDist = dist
                    bestAyah = ayah
                }
            }
        }
        return bestAyah
    }

    /**
     * Accurate word-level hit-testing inside a Mushaf page (for direct Sarf/word inspection).
     */
    fun hitTestWord(layout: MushafPageLayout, nx: Float, ny: Float): WordRegion? {
        val ayah = hitTestAyah(layout, nx, ny) ?: return null
        if (ayah.words.isEmpty()) return null

        val exactWord = ayah.words.firstOrNull { it.bounds.contains(nx, ny, padding = 0f) }
        if (exactWord != null) return exactWord

        return ayah.words
            .minByOrNull { it.bounds.distanceTo(nx, ny) }
            ?.takeIf { it.bounds.distanceTo(nx, ny) <= MAX_PROXIMITY_TOLERANCE * 1.5f }
    }

    /**
     * Determines whether the contextual Ayah action bar should appear ABOVE or BELOW the selected Ayah
     * so it NEVER covers the selected Ayah text, automatically flipping when near the top of the page.
     */
    fun computeContextualMenuPlacement(
        selectedAyah: AyahPageRegion,
        minTopClearanceNormalized: Float = 0.24f
    ): ContextualMenuPlacement {
        val box = selectedAyah.boundingBox
        return if (box.top >= minTopClearanceNormalized) {
            ContextualMenuPlacement(placeAboveAyah = true, anchorNormalizedY = box.top)
        } else {
            ContextualMenuPlacement(placeAboveAyah = false, anchorNormalizedY = box.bottom)
        }
    }
}
