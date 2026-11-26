package com.example.data.quran

import android.content.Context
import android.util.LruCache
import com.example.domain.model.QuranReciter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Verified millisecond timing span for a single word inside an Ayah recitation.
 * Offsets are normalized relative to the start of the Ayah (`0ms`).
 */
data class WordTimingSegment(
    val wordPosition: Int, // 1-indexed RTL word position in the Ayah
    val startMs: Long,
    val endMs: Long
)

data class AyahWordTimings(
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val segments: List<WordTimingSegment>,
    val sourceAttribution: String = "Quran.com Verified Recitation Segments"
) {
    /**
     * Deterministically resolves the active word position at `positionMs` (relative to Ayah start)
     * using ONLY verified millisecond boundaries. Never guesses or simulates words!
     */
    fun findWordPositionAt(positionMs: Long): Int? {
        if (segments.isEmpty() || positionMs < 0L) return null
        var low = 0
        var high = segments.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val seg = segments[mid]
            when {
                positionMs < seg.startMs -> high = mid - 1
                positionMs > seg.endMs -> low = mid + 1
                else -> return seg.wordPosition
            }
        }
        return null
    }
}

/**
 * Verified continuous-stream timestamp range for a single Ayah inside a full-Surah MP3 recording.
 * `startMs` and `endMs` are absolute millisecond positions inside the continuous Surah audio stream.
 * `segments` are normalized relative to `startMs` (`0ms` = Ayah start).
 */
data class AyahTimestampRange(
    val surahNumber: Int,
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long,
    val segments: List<WordTimingSegment> = emptyList()
) {
    fun findWordPositionAtStreamMs(streamPositionMs: Long): Int? {
        if (segments.isEmpty()) return null
        val relMs = (streamPositionMs - startMs).coerceAtLeast(0L)
        var low = 0
        var high = segments.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val seg = segments[mid]
            when {
                relMs < seg.startMs -> high = mid - 1
                relMs > seg.endMs -> low = mid + 1
                else -> return seg.wordPosition
            }
        }
        return null
    }
}

/**
 * Full-Surah continuous audio manifest containing:
 * - One continuous chapter MP3 stream (`audioUrl`) so Sheikh recitation NEVER pauses or re-buffers
 *   between Ayahs!
 * - Verified millisecond `startMs..endMs` boundaries + optional word segments for every Ayah in
 *   the Surah so visual Ayah highlighting tracks audio position cleanly and independently.
 */
data class SurahContinuousAudioManifest(
    val reciterId: String,
    val quranComReciterId: Int,
    val surahNumber: Int,
    val audioUrl: String,
    val ayahs: List<AyahTimestampRange>,
    val sourceAttribution: String = "Quran.com v4 Continuous Chapter Recitation"
) {
    private val ayahIndexByNumber: Map<Int, Int> =
        ayahs.withIndex().associate { (index, item) -> item.ayahNumber to index }

    fun findAyahRange(ayahNumber: Int): AyahTimestampRange? {
        val idx = ayahIndexByNumber[ayahNumber] ?: return null
        return ayahs.getOrNull(idx)
    }

    fun nextAyahRange(currentAyahNumber: Int): AyahTimestampRange? {
        val idx = ayahIndexByNumber[currentAyahNumber] ?: return null
        return ayahs.getOrNull(idx + 1)
    }

    fun previousAyahRange(currentAyahNumber: Int): AyahTimestampRange? {
        val idx = ayahIndexByNumber[currentAyahNumber] ?: return null
        return ayahs.getOrNull(idx - 1)
    }

    /**
     * Deterministically resolves which Ayah is playing at `streamPositionMs` inside the continuous
     * Surah audio stream via binary search over verified timestamps.
     * During short natural breath pauses between `ayah[i].endMs` and `ayah[i+1].startMs`, holds
     * cleanly on `ayah[i]` until `ayah[i+1].startMs` begins (never flickers or skips!).
     */
    fun findAyahAt(streamPositionMs: Long): AyahTimestampRange? {
        if (ayahs.isEmpty()) return null
        val pos = streamPositionMs.coerceAtLeast(0L)
        if (pos < ayahs.first().startMs) return ayahs.first()
        if (pos >= ayahs.last().startMs) return ayahs.last()

        var low = 0
        var high = ayahs.lastIndex
        var candidateIdx = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            val item = ayahs[mid]
            when {
                pos < item.startMs -> high = mid - 1
                pos >= item.endMs -> {
                    candidateIdx = mid
                    low = mid + 1
                }
                else -> return item
            }
        }
        return ayahs[candidateIdx]
    }
}

/**
 * Authentic Word-by-Word & Continuous-Stream Quran Recitation Timing Repository.
 *
 * Uses official Quran.com v4 chapter recitation audio + verified millisecond timestamps
 * (`https://api.quran.com/api/v4/chapter_recitations/{reciterId}/{surah}?segments=true`),
 * cached locally per `(reciter, surah)` in `filesDir/mushaf_timings/` for offline reuse.
 *
 * Honesty guarantee:
 * - Only reciters with verified chapter timestamp tables are queried.
 * - Never fabricates Ayah or word timestamps with arbitrary timers; when unavailable offline,
 *   the player falls back cleanly to gapless per-Ayah pre-buffered playback.
 */
class QuranWordTimingRepository(private val appContext: Context) {

    private val timingsDir: File = File(appContext.filesDir, "mushaf_timings").apply { mkdirs() }
    private val memoryCache = LruCache<String, Map<Int, AyahWordTimings>>(12)
    private val manifestCache = LruCache<String, SurahContinuousAudioManifest>(12)

    fun supportsVerifiedWordTimings(reciter: QuranReciter): Boolean =
        quranComReciterIdFor(reciter) != null

    /**
     * Loads the continuous full-Surah MP3 URL + verified Ayah/word timestamp manifest for
     * `(reciter, surahNumber)`, using memory cache -> local disk cache -> Quran.com v4 API.
     */
    suspend fun getContinuousSurahManifest(
        reciter: QuranReciter,
        surahNumber: Int
    ): SurahContinuousAudioManifest? = withContext(Dispatchers.IO) {
        val quranComId = quranComReciterIdFor(reciter) ?: return@withContext null
        val cacheKey = "${reciter.id}_$surahNumber"
        manifestCache.get(cacheKey)?.let { return@withContext it }

        val localFile = File(timingsDir, "timings_${quranComId}_s${surahNumber.toString().padStart(3, '0')}.json")
        if (localFile.exists() && localFile.length() > 32L) {
            val parsed = runCatching {
                parseQuranComContinuousManifestJson(reciter.id, quranComId, surahNumber, localFile.readText())
            }.getOrNull()
            if (parsed != null && parsed.ayahs.isNotEmpty() && parsed.audioUrl.isNotBlank()) {
                manifestCache.put(cacheKey, parsed)
                return@withContext parsed
            }
        }

        val remoteJson = fetchQuranComChapterSegments(quranComId, surahNumber) ?: return@withContext null
        val parsed = runCatching {
            parseQuranComContinuousManifestJson(reciter.id, quranComId, surahNumber, remoteJson)
        }.getOrNull()

        if (parsed != null && parsed.ayahs.isNotEmpty() && parsed.audioUrl.isNotBlank()) {
            runCatching { localFile.writeText(remoteJson) }
            manifestCache.put(cacheKey, parsed)
            val wordMap = parsed.ayahs
                .filter { it.segments.isNotEmpty() }
                .associate { range ->
                    range.ayahNumber to AyahWordTimings(
                        reciterId = reciter.id,
                        surahNumber = surahNumber,
                        ayahNumber = range.ayahNumber,
                        segments = range.segments
                    )
                }
            if (wordMap.isNotEmpty()) {
                memoryCache.put(cacheKey, wordMap)
            }
            return@withContext parsed
        }
        null
    }

    /**
     * Loads verified word timing segments for every Ayah in `(reciter, surahNumber)`.
     * Returns an empty map cleanly if the reciter has no verified segment dataset or device is offline
     * without a previously cached timing file.
     */
    suspend fun getSurahWordTimings(
        reciter: QuranReciter,
        surahNumber: Int
    ): Map<Int, AyahWordTimings> = withContext(Dispatchers.IO) {
        val quranComId = quranComReciterIdFor(reciter) ?: return@withContext emptyMap()
        val cacheKey = "${reciter.id}_$surahNumber"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        val localFile = File(timingsDir, "timings_${quranComId}_s${surahNumber.toString().padStart(3, '0')}.json")
        if (localFile.exists() && localFile.length() > 32L) {
            val parsed = runCatching {
                parseQuranComSegmentsJson(reciter.id, surahNumber, localFile.readText())
            }.getOrDefault(emptyMap())
            if (parsed.isNotEmpty()) {
                memoryCache.put(cacheKey, parsed)
                return@withContext parsed
            }
        }

        val remoteJson = fetchQuranComChapterSegments(quranComId, surahNumber) ?: return@withContext emptyMap()
        val parsed = runCatching {
            parseQuranComSegmentsJson(reciter.id, surahNumber, remoteJson)
        }.getOrDefault(emptyMap())

        if (parsed.isNotEmpty()) {
            runCatching { localFile.writeText(remoteJson) }
            memoryCache.put(cacheKey, parsed)
        }
        parsed
    }

    private fun fetchQuranComChapterSegments(quranComReciterId: Int, surahNumber: Int): String? {
        return runCatching {
            val url = "https://api.quran.com/api/v4/chapter_recitations/$quranComReciterId/$surahNumber?segments=true"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 9_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NOOR-Islamic-App/6.0")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        /**
         * Maps NOOR reciters to official Quran.com v4 reciter IDs that publish verified
         * continuous chapter audio + Ayah/word millisecond timestamps.
         */
        fun quranComReciterIdFor(reciter: QuranReciter): Int? = when (reciter) {
            QuranReciter.ALAFASY -> 7          // Mishary Rashid Alafasy
            QuranReciter.HUSARY -> 6           // Mahmoud Khalil Al-Husary
            QuranReciter.MINSHAWI -> 9         // Mohamed Siddiq al-Minshawi (Murattal)
            QuranReciter.ABDUL_BASIT -> 2      // AbdulBaset AbdulSamad (Murattal)
            QuranReciter.YASSER_AL_DOSARI -> null // Uses gapless pre-buffered per-Ayah stream
            QuranReciter.YASSER_AL_DOSARI_IRAQI_MAQAM -> null // Uses gapless pre-buffered per-Ayah stream
            QuranReciter.MAHER_AL_MUAIQLY -> null // Uses gapless pre-buffered per-Ayah stream
        }

        const val CUSTOM_PROPORTIONAL_RECITER_ID = -1

        /**
         * Builds a proportional continuous-stream manifest for an Admin-uploaded custom reciter
         * so Ayah highlighting advances smoothly across the Mushaf proportional to each Ayah's text
         * weight, and automatically rescales to exact MediaPlayer duration once prepared.
         */
        fun buildProportionalSurahManifest(
            reciterId: String,
            surahNumber: Int,
            audioSource: String,
            ayahWeights: List<Pair<Int, Int>>,
            totalDurationMs: Long = 180_000L
        ): SurahContinuousAudioManifest {
            val safeWeights = ayahWeights.ifEmpty { listOf(1 to 40) }
            val totalWeight = safeWeights.sumOf { it.second.coerceAtLeast(10).toLong() }.coerceAtLeast(1L)
            val safeDur = totalDurationMs.coerceAtLeast(1_000L)
            var cursor = 0L
            val ranges = safeWeights.mapIndexed { idx, (ayahNum, weight) ->
                val slice = if (idx == safeWeights.lastIndex) {
                    (safeDur - cursor).coerceAtLeast(250L)
                } else {
                    ((safeDur * weight.coerceAtLeast(10).toLong()) / totalWeight).coerceAtLeast(250L)
                }
                val start = cursor
                val end = (cursor + slice).coerceAtMost(safeDur)
                cursor = end
                AyahTimestampRange(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNum,
                    startMs = start,
                    endMs = end.coerceAtLeast(start + 100L)
                )
            }
            return SurahContinuousAudioManifest(
                reciterId = reciterId,
                quranComReciterId = CUSTOM_PROPORTIONAL_RECITER_ID,
                surahNumber = surahNumber,
                audioUrl = audioSource,
                ayahs = ranges,
                sourceAttribution = "NOOR Custom Uploaded Reciter (Auto-Ayah Sync)"
            )
        }

        /**
         * Parses Quran.com v4 `chapter_recitations/{id}/{surah}?segments=true` JSON into a
         * continuous Surah audio manifest (single MP3 URL + verified Ayah & word timestamps).
         */
        fun parseQuranComContinuousManifestJson(
            reciterId: String,
            quranComReciterId: Int,
            surahNumber: Int,
            rawJson: String
        ): SurahContinuousAudioManifest? {
            val root = JSONObject(rawJson)
            val audioFile = root.optJSONObject("audio_file") ?: return null
            val rawUrl = audioFile.optString("audio_url", "").trim()
            if (rawUrl.isBlank()) return null
            val resolvedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                rawUrl
            } else {
                "https://download.quranicaudio.com/qdc/$rawUrl"
            }

            val timestamps = audioFile.optJSONArray("timestamps") ?: return null
            val ranges = mutableListOf<AyahTimestampRange>()
            for (i in 0 until timestamps.length()) {
                val ts = timestamps.optJSONObject(i) ?: continue
                val verseKey = ts.optString("verse_key", "")
                val parts = verseKey.split(":")
                val sNum = parts.getOrNull(0)?.toIntOrNull() ?: continue
                val aNum = parts.getOrNull(1)?.toIntOrNull() ?: continue
                if (sNum != surahNumber) continue

                val verseFromMs = ts.optLong("timestamp_from", -1L)
                val verseToMs = ts.optLong("timestamp_to", -1L)
                if (verseFromMs < 0L || verseToMs <= verseFromMs) continue

                val segArr = ts.optJSONArray("segments") ?: JSONArray()
                val segments = mutableListOf<WordTimingSegment>()
                for (j in 0 until segArr.length()) {
                    val seg = segArr.optJSONArray(j) ?: continue
                    if (seg.length() < 3) continue
                    val wordPos = seg.optInt(0, -1)
                    val absStart = seg.optLong(1, -1L)
                    val absEnd = seg.optLong(2, -1L)
                    if (wordPos <= 0 || absStart < 0L || absEnd <= absStart) continue

                    val relStart = (absStart - verseFromMs).coerceAtLeast(0L)
                    val relEnd = (absEnd - verseFromMs).coerceAtLeast(relStart + 1L)
                    segments += WordTimingSegment(
                        wordPosition = wordPos,
                        startMs = relStart,
                        endMs = relEnd
                    )
                }

                ranges += AyahTimestampRange(
                    surahNumber = surahNumber,
                    ayahNumber = aNum,
                    startMs = verseFromMs,
                    endMs = verseToMs,
                    segments = segments.sortedBy { it.startMs }
                )
            }

            if (ranges.isEmpty()) return null
            return SurahContinuousAudioManifest(
                reciterId = reciterId,
                quranComReciterId = quranComReciterId,
                surahNumber = surahNumber,
                audioUrl = resolvedUrl,
                ayahs = ranges.sortedBy { it.ayahNumber }
            )
        }

        /**
         * Parses Quran.com v4 `audio_file.timestamps[]` JSON into per-Ayah, Ayah-relative
         * (`0ms`-based) word timing segments.
         */
        fun parseQuranComSegmentsJson(
            reciterId: String,
            surahNumber: Int,
            rawJson: String
        ): Map<Int, AyahWordTimings> {
            val root = JSONObject(rawJson)
            val audioFile = root.optJSONObject("audio_file") ?: return emptyMap()
            val timestamps = audioFile.optJSONArray("timestamps") ?: return emptyMap()

            val result = mutableMapOf<Int, AyahWordTimings>()
            for (i in 0 until timestamps.length()) {
                val ts = timestamps.optJSONObject(i) ?: continue
                val verseKey = ts.optString("verse_key", "")
                val parts = verseKey.split(":")
                val sNum = parts.getOrNull(0)?.toIntOrNull() ?: continue
                val aNum = parts.getOrNull(1)?.toIntOrNull() ?: continue
                if (sNum != surahNumber) continue

                val verseFromMs = ts.optLong("timestamp_from", 0L)
                val segArr = ts.optJSONArray("segments") ?: JSONArray()
                val segments = mutableListOf<WordTimingSegment>()

                for (j in 0 until segArr.length()) {
                    val seg = segArr.optJSONArray(j) ?: continue
                    if (seg.length() < 3) continue
                    val wordPos = seg.optInt(0, -1)
                    val absStart = seg.optLong(1, -1L)
                    val absEnd = seg.optLong(2, -1L)
                    if (wordPos <= 0 || absStart < 0L || absEnd <= absStart) continue

                    val relStart = (absStart - verseFromMs).coerceAtLeast(0L)
                    val relEnd = (absEnd - verseFromMs).coerceAtLeast(relStart + 1L)
                    segments += WordTimingSegment(
                        wordPosition = wordPos,
                        startMs = relStart,
                        endMs = relEnd
                    )
                }

                if (segments.isNotEmpty()) {
                    result[aNum] = AyahWordTimings(
                        reciterId = reciterId,
                        surahNumber = surahNumber,
                        ayahNumber = aNum,
                        segments = segments.sortedBy { it.startMs }
                    )
                }
            }
            return result
        }
    }
}
