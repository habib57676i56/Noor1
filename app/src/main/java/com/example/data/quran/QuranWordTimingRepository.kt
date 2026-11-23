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
     * Deterministically resolves the active word position at `positionMs` using ONLY verified
     * millisecond boundaries. Never guesses or simulates words with arbitrary timers!
     */
    fun findWordPositionAt(positionMs: Long): Int? {
        if (segments.isEmpty() || positionMs < 0L) return null
        // Binary search over sorted startMs intervals
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
 * Authentic Word-by-Word Quran Recitation Timing Repository.
 *
 * Uses official Quran.com v4 chapter recitation word-segment timestamps
 * (`https://api.quran.com/api/v4/chapter_recitations/{reciterId}/{surah}?segments=true`),
 * cached locally per `(reciter, surah)` in `filesDir/mushaf_timings/` for offline reuse.
 *
 * Honesty guarantee:
 * - Only reciters with verified word-level timestamp tables are queried.
 * - If a reciter or offline Surah lacks verified timing segments, returns `null` rather than
 *   fabricating word timings with arbitrary timers; Ayah-level synchronization continues seamlessly.
 */
class QuranWordTimingRepository(private val appContext: Context) {

    private val timingsDir: File = File(appContext.filesDir, "mushaf_timings").apply { mkdirs() }
    private val memoryCache = LruCache<String, Map<Int, AyahWordTimings>>(12)

    fun supportsVerifiedWordTimings(reciter: QuranReciter): Boolean =
        quranComReciterIdFor(reciter) != null

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
         * word-by-word millisecond segment timestamps.
         */
        fun quranComReciterIdFor(reciter: QuranReciter): Int? = when (reciter) {
            QuranReciter.ALAFASY -> 7          // Mishary Rashid Alafasy
            QuranReciter.HUSARY -> 6           // Mahmoud Khalil Al-Husary
            QuranReciter.MINSHAWI -> 9         // Mohamed Siddiq al-Minshawi (Murattal)
            QuranReciter.ABDUL_BASIT -> 2      // AbdulBaset AbdulSamad (Murattal)
            QuranReciter.YASSER_AL_DOSARI -> null // Verified chapter segments not published -> Ayah-level sync only
            QuranReciter.MAHER_AL_MUAIQLY -> null // Verified chapter segments not published -> Ayah-level sync only
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
