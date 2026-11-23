package com.example.domain.quran

import com.example.domain.model.QuranVerse
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class WordEvaluationStatus {
    CORRECT,
    OMITTED,
    ADDED,
    REPLACED,
    REPEATED,
    PHONETIC_UNCERTAIN
}

enum class AssessmentStrictness(val labelAr: String, val labelEn: String, val matchThreshold: Float) {
    LENIENT("متساهل (للمبتدئين)", "Lenient (Beginners)", 0.72f),
    BALANCED("متوازن (افتراضي)", "Balanced (Default)", 0.80f),
    STRICT("دقيق (للحفّاظ)", "Strict (Advanced)", 0.88f);

    companion object {
        fun fromKey(key: String?): AssessmentStrictness =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: BALANCED
    }
}

enum class RecitationOverallLevel(val labelAr: String, val labelEn: String) {
    EXCELLENT("ممتاز (متقن)", "Excellent"),
    GOOD_REVIEW("جيد مع مواضع للمراجعة", "Good — Minor Review"),
    NEEDS_REVISION("يحتاج مراجعة وتثبيت", "Needs Revision"),
    RECOGNITION_UNCLEAR("النص الملتقط غير كافٍ — يُنصح بمراجعة المعلم أو إعادة التلاوة", "Recognition Unclear — Retry or Teacher Review")
}

data class RecitationWordEvaluation(
    val expectedWord: String? = null,
    val spokenWord: String? = null,
    val ayahNumber: Int = 0,
    val status: WordEvaluationStatus,
    val similarity: Float = 0f
)

data class RecitationAssessmentReport(
    val surahNumber: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val wordEvaluations: List<RecitationWordEvaluation>,
    val correctCount: Int,
    val omittedWords: List<String>,
    val addedWords: List<String>,
    val replacedWords: List<Pair<String, String>>,
    val repeatedWords: List<String>,
    val skippedVerses: List<Int>,
    val outOfOrderVerses: List<Int>,
    val longPauseCount: Int,
    val longestPauseMs: Long,
    val recognitionUncertainWords: List<Pair<String, String>>,
    val suspectedMistakeWords: List<String>,
    val accuracyPercent: Int,
    val overallLevel: RecitationOverallLevel,
    val summaryAr: String,
    val summaryEn: String
)

/**
 * Pure-Kotlin word-level sequence-alignment Quran recitation assessment engine (100% offline).
 *
 * Design & Islamic Integrity Rules:
 * - Compares recognized Arabic transcript strictly against canonical Uthmani QuranVerse objects.
 * - Uses dynamic programming (Needleman-Wunsch alignment) on phonetically normalized Arabic tokens
 *   to detect omitted words, added words, replaced words, repeated words, skipped verses, and
 *   out-of-order verse recitation.
 * - Explicitly separates PHONETIC_UNCERTAIN (similarity 0.55..matchThreshold) from suspected mistakes
 *   so speech-recognition imperfections are NEVER falsely labeled as confirmed Quranic errors.
 * - Never fabricates Quranic verses or guesses missing audio.
 */
object QuranRecitationAssessmentEngine {

    private data class ExpectedToken(
        val displayUthmani: String,
        val normalized: String,
        val ayahNumber: Int
    )

    private data class SpokenToken(
        val raw: String,
        val normalized: String
    )

    fun evaluate(
        verses: List<QuranVerse>,
        spokenTranscript: String,
        longPauseCount: Int = 0,
        longestPauseMs: Long = 0L,
        strictness: AssessmentStrictness = AssessmentStrictness.BALANCED
    ): RecitationAssessmentReport {
        if (verses.isEmpty()) {
            return RecitationAssessmentReport(
                surahNumber = 1,
                fromAyah = 1,
                toAyah = 1,
                wordEvaluations = emptyList(),
                correctCount = 0,
                omittedWords = emptyList(),
                addedWords = emptyList(),
                replacedWords = emptyList(),
                repeatedWords = emptyList(),
                skippedVerses = emptyList(),
                outOfOrderVerses = emptyList(),
                longPauseCount = longPauseCount,
                longestPauseMs = longestPauseMs,
                recognitionUncertainWords = emptyList(),
                suspectedMistakeWords = emptyList(),
                accuracyPercent = 0,
                overallLevel = RecitationOverallLevel.RECOGNITION_UNCLEAR,
                summaryAr = "لم يتم تحديد آيات للمقارنة.",
                summaryEn = "No target verses selected for comparison."
            )
        }

        val surahNumber = verses.first().surahNumber
        val fromAyah = verses.minOf { it.numberInSurah }
        val toAyah = verses.maxOf { it.numberInSurah }

        val expected = buildExpectedTokens(verses)
        val spoken = buildSpokenTokens(spokenTranscript)

        if (spoken.isEmpty()) {
            return RecitationAssessmentReport(
                surahNumber = surahNumber,
                fromAyah = fromAyah,
                toAyah = toAyah,
                wordEvaluations = expected.map {
                    RecitationWordEvaluation(
                        expectedWord = it.displayUthmani,
                        spokenWord = null,
                        ayahNumber = it.ayahNumber,
                        status = WordEvaluationStatus.OMITTED,
                        similarity = 0f
                    )
                },
                correctCount = 0,
                omittedWords = expected.map { it.displayUthmani },
                addedWords = emptyList(),
                replacedWords = emptyList(),
                repeatedWords = emptyList(),
                skippedVerses = verses.map { it.numberInSurah },
                outOfOrderVerses = emptyList(),
                longPauseCount = longPauseCount,
                longestPauseMs = longestPauseMs,
                recognitionUncertainWords = emptyList(),
                suspectedMistakeWords = expected.map { it.displayUthmani },
                accuracyPercent = 0,
                overallLevel = RecitationOverallLevel.RECOGNITION_UNCLEAR,
                summaryAr = "لم يتم التقاط كلمات عربية للتقييم. تأكد من الميكروفون أو استمع لتسجيلك الصوتي أو جرّب الإملاء البديل.",
                summaryEn = "No Arabic words were captured. Check your microphone, review your audio recording, or try System Voice Fallback."
            )
        }

        val matchThreshold = strictness.matchThreshold
        val uncertainFloor = 0.55f

        // Detect verse-level order and skipped verses first when multiple verses are evaluated
        val (skippedVerses, outOfOrderVerses) = detectVerseCoverageAndOrder(verses, spoken)

        // Dynamic programming Needleman-Wunsch alignment
        val n = expected.size
        val m = spoken.size
        val dp = Array(n + 1) { FloatArray(m + 1) }
        val trace = Array(n + 1) { IntArray(m + 1) } // 1 = diag, 2 = up (omit expected), 3 = left (add spoken)

        val gapPenalty = -0.65f
        for (i in 1..n) {
            dp[i][0] = i * gapPenalty
            trace[i][0] = 2
        }
        for (j in 1..m) {
            dp[0][j] = j * gapPenalty
            trace[0][j] = 3
        }

        for (i in 1..n) {
            val expNorm = expected[i - 1].normalized
            for (j in 1..m) {
                val spkNorm = spoken[j - 1].normalized
                val sim = wordSimilarity(expNorm, spkNorm)
                val diagScore = dp[i - 1][j - 1] + when {
                    sim >= matchThreshold -> 1.0f + (sim * 0.25f)
                    sim >= uncertainFloor -> 0.35f * sim
                    else -> -0.85f
                }
                val upScore = dp[i - 1][j] + gapPenalty
                val leftScore = dp[i][j - 1] + gapPenalty

                if (diagScore >= upScore && diagScore >= leftScore) {
                    dp[i][j] = diagScore
                    trace[i][j] = 1
                } else if (upScore >= leftScore) {
                    dp[i][j] = upScore
                    trace[i][j] = 2
                } else {
                    dp[i][j] = leftScore
                    trace[i][j] = 3
                }
            }
        }

        // Backtrack alignment
        val reversed = mutableListOf<RecitationWordEvaluation>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && trace[i][j] == 1 -> {
                    val exp = expected[i - 1]
                    val spk = spoken[j - 1]
                    val sim = wordSimilarity(exp.normalized, spk.normalized)
                    val status = when {
                        sim >= matchThreshold -> WordEvaluationStatus.CORRECT
                        sim >= uncertainFloor -> WordEvaluationStatus.PHONETIC_UNCERTAIN
                        else -> WordEvaluationStatus.REPLACED
                    }
                    reversed.add(
                        RecitationWordEvaluation(
                            expectedWord = exp.displayUthmani,
                            spokenWord = spk.raw,
                            ayahNumber = exp.ayahNumber,
                            status = status,
                            similarity = sim
                        )
                    )
                    i -= 1
                    j -= 1
                }
                i > 0 && (j == 0 || trace[i][j] == 2) -> {
                    val exp = expected[i - 1]
                    reversed.add(
                        RecitationWordEvaluation(
                            expectedWord = exp.displayUthmani,
                            spokenWord = null,
                            ayahNumber = exp.ayahNumber,
                            status = WordEvaluationStatus.OMITTED,
                            similarity = 0f
                        )
                    )
                    i -= 1
                }
                else -> {
                    val spk = spoken[j - 1]
                    val prevSpokenNorm = if (j >= 2) spoken[j - 2].normalized else null
                    val nextSpokenNorm = if (j < m) spoken[j].normalized else null
                    val currExpNorm = if (i in 1..n) expected[i - 1].normalized else null
                    val nextExpNorm = if (i < n) expected[i].normalized else null
                    val isRepeat = (prevSpokenNorm != null && wordSimilarity(spk.normalized, prevSpokenNorm) >= 0.82f) ||
                        (nextSpokenNorm != null && wordSimilarity(spk.normalized, nextSpokenNorm) >= 0.82f) ||
                        (currExpNorm != null && wordSimilarity(spk.normalized, currExpNorm) >= 0.85f) ||
                        (nextExpNorm != null && wordSimilarity(spk.normalized, nextExpNorm) >= 0.85f)
                    reversed.add(
                        RecitationWordEvaluation(
                            expectedWord = null,
                            spokenWord = spk.raw,
                            ayahNumber = expected.getOrNull((i - 1).coerceIn(0, max(0, n - 1)))?.ayahNumber ?: fromAyah,
                            status = if (isRepeat) WordEvaluationStatus.REPEATED else WordEvaluationStatus.ADDED,
                            similarity = 0f
                        )
                    )
                    j -= 1
                }
            }
        }

        val evaluations = reversed.asReversed()
        val correctCount = evaluations.count { it.status == WordEvaluationStatus.CORRECT }
        val uncertainList = evaluations.filter { it.status == WordEvaluationStatus.PHONETIC_UNCERTAIN }
            .map { (it.expectedWord ?: "") to (it.spokenWord ?: "") }
        val omitted = evaluations.filter { it.status == WordEvaluationStatus.OMITTED }
            .mapNotNull { it.expectedWord }
        val added = evaluations.filter { it.status == WordEvaluationStatus.ADDED }
            .mapNotNull { it.spokenWord }
        val repeated = evaluations.filter { it.status == WordEvaluationStatus.REPEATED }
            .mapNotNull { it.spokenWord }
        val replaced = evaluations.filter { it.status == WordEvaluationStatus.REPLACED }
            .map { (it.expectedWord ?: "") to (it.spokenWord ?: "") }

        val suspectedMistakes = buildList {
            omitted.forEach { add("حذف: $it") }
            replaced.forEach { (exp, spk) -> add("استبدال: $exp ← $spk") }
            added.forEach { add("زيادة: $it") }
            repeated.forEach { add("تكرار: $it") }
        }

        // Credit phonetic-uncertain words at 0.5 weight so minor ASR quirks don't unfairly tank scores
        val effectiveHits = correctCount.toFloat() + (uncertainList.size * 0.5f)
        val rawAccuracy = if (expected.isEmpty()) 0f else (effectiveHits / expected.size.toFloat())
        val extraPenalty = ((added.size + replaced.size) * 0.02f).coerceAtMost(0.20f)
        val accuracyPercent = ((rawAccuracy - extraPenalty).coerceIn(0f, 1f) * 100f).roundToInt()

        val overallLevel = when {
            spoken.size < (expected.size * 0.25f) && accuracyPercent < 35 -> RecitationOverallLevel.RECOGNITION_UNCLEAR
            accuracyPercent >= 90 && skippedVerses.isEmpty() && outOfOrderVerses.isEmpty() -> RecitationOverallLevel.EXCELLENT
            accuracyPercent >= 72 -> RecitationOverallLevel.GOOD_REVIEW
            else -> RecitationOverallLevel.NEEDS_REVISION
        }

        val summaryAr = buildString {
            append("الدقة التقديرية: $accuracyPercent% ($correctCount/${expected.size} كلمة متطابقة).")
            if (omitted.isNotEmpty()) append(" محذوفة: ${omitted.size}.")
            if (replaced.isNotEmpty()) append(" مستبدلة: ${replaced.size}.")
            if (added.isNotEmpty()) append(" زائدة: ${added.size}.")
            if (repeated.isNotEmpty()) append(" مكررة: ${repeated.size}.")
            if (uncertainList.isNotEmpty()) append(" فروق صوتية/تعرف آلي للمراجعة: ${uncertainList.size}.")
            if (skippedVerses.isNotEmpty()) append(" آيات غير متلوة: ${skippedVerses.joinToString("، ")}.")
            if (outOfOrderVerses.isNotEmpty()) append(" ترتيب آيات مختلف: ${outOfOrderVerses.joinToString("، ")}.")
            if (longPauseCount > 0) append(" توقفات طويلة: $longPauseCount (${longestPauseMs / 1000}ث).")
        }

        val summaryEn = buildString {
            append("Estimated accuracy: $accuracyPercent% ($correctCount/${expected.size} matched words).")
            if (omitted.isNotEmpty()) append(" Omitted: ${omitted.size}.")
            if (replaced.isNotEmpty()) append(" Replaced: ${replaced.size}.")
            if (added.isNotEmpty()) append(" Added: ${added.size}.")
            if (repeated.isNotEmpty()) append(" Repeated: ${repeated.size}.")
            if (uncertainList.isNotEmpty()) append(" ASR/phonetic uncertain: ${uncertainList.size}.")
            if (skippedVerses.isNotEmpty()) append(" Skipped Ayahs: ${skippedVerses.joinToString(", ")}.")
            if (outOfOrderVerses.isNotEmpty()) append(" Out-of-order Ayahs: ${outOfOrderVerses.joinToString(", ")}.")
            if (longPauseCount > 0) append(" Long pauses: $longPauseCount (${longestPauseMs / 1000}s).")
        }

        return RecitationAssessmentReport(
            surahNumber = surahNumber,
            fromAyah = fromAyah,
            toAyah = toAyah,
            wordEvaluations = evaluations,
            correctCount = correctCount,
            omittedWords = omitted,
            addedWords = added,
            replacedWords = replaced,
            repeatedWords = repeated,
            skippedVerses = skippedVerses,
            outOfOrderVerses = outOfOrderVerses,
            longPauseCount = longPauseCount,
            longestPauseMs = longestPauseMs,
            recognitionUncertainWords = uncertainList,
            suspectedMistakeWords = suspectedMistakes,
            accuracyPercent = accuracyPercent,
            overallLevel = overallLevel,
            summaryAr = summaryAr,
            summaryEn = summaryEn
        )
    }

    private fun buildExpectedTokens(verses: List<QuranVerse>): List<ExpectedToken> {
        val list = mutableListOf<ExpectedToken>()
        for (v in verses) {
            val rawWords = v.textUthmani.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            for (w in rawWords) {
                val norm = QuranArabicPhoneticNormalizer.normalize(w)
                if (norm.isNotBlank()) {
                    // If normalization produced multiple tokens, map each cleanly
                    for (t in norm.split(' ').filter { it.isNotBlank() }) {
                        list.add(ExpectedToken(displayUthmani = w, normalized = t, ayahNumber = v.numberInSurah))
                    }
                }
            }
        }
        return list
    }

    private fun buildSpokenTokens(transcript: String): List<SpokenToken> {
        val rawWords = transcript.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val list = mutableListOf<SpokenToken>()
        for (w in rawWords) {
            val norm = QuranArabicPhoneticNormalizer.normalize(w)
            if (norm.isNotBlank()) {
                for (t in norm.split(' ').filter { it.isNotBlank() }) {
                    list.add(SpokenToken(raw = w, normalized = t))
                }
            }
        }
        return list
    }

    private fun detectVerseCoverageAndOrder(
        verses: List<QuranVerse>,
        spoken: List<SpokenToken>
    ): Pair<List<Int>, List<Int>> {
        if (verses.size <= 1) return emptyList<Int>() to emptyList()
        val skipped = mutableListOf<Int>()
        val versePositions = mutableListOf<Pair<Int, Int>>() // ayahNumber -> best window start index
        val claimedIndices = HashSet<Int>()

        for (v in verses) {
            val vTokens = QuranArabicPhoneticNormalizer.tokenize(v.textUthmani)
            if (vTokens.isEmpty()) continue
            val minCoverage = if (vTokens.size <= 2) 0.60f else 0.45f
            val windowWidth = vTokens.size + 2

            var bestCoverage = 0f
            var bestScore = -1f
            var bestStartPos = -1
            var bestMatchedIndices: List<Int> = emptyList()

            for (start in spoken.indices) {
                val endExclusive = min(spoken.size, start + windowWidth)
                var cursor = start
                var matchedCount = 0
                var unclaimedCount = 0
                var firstHit = -1
                val hits = mutableListOf<Int>()

                for (vt in vTokens) {
                    var foundAt = -1
                    for (k in cursor until endExclusive) {
                        if (wordSimilarity(vt, spoken[k].normalized) >= 0.78f) {
                            foundAt = k
                            break
                        }
                    }
                    if (foundAt >= 0) {
                        matchedCount += 1
                        if (foundAt !in claimedIndices) unclaimedCount += 1
                        if (firstHit == -1) firstHit = foundAt
                        hits.add(foundAt)
                        cursor = foundAt + 1
                    }
                }

                val coverage = matchedCount.toFloat() / vTokens.size.toFloat()
                val unclaimedRatio = unclaimedCount.toFloat() / vTokens.size.toFloat()
                // Strongly prefer windows with unclaimed spoken tokens so shared words
                // (e.g. "الله" or "أحد" across verses) don't steal an earlier verse's span.
                val compositeScore = coverage + (unclaimedRatio * 0.5f)
                if (compositeScore > bestScore && unclaimedCount > 0) {
                    bestScore = compositeScore
                    bestCoverage = coverage
                    bestStartPos = firstHit
                    bestMatchedIndices = hits
                }
            }

            if (bestCoverage < minCoverage || bestStartPos < 0) {
                skipped.add(v.numberInSurah)
            } else {
                claimedIndices.addAll(bestMatchedIndices)
                versePositions.add(v.numberInSurah to bestStartPos)
            }
        }

        val outOfOrder = mutableListOf<Int>()
        var lastSeenPos = -1
        for ((ayahNum, pos) in versePositions) {
            if (pos < lastSeenPos) {
                outOfOrder.add(ayahNum)
            } else {
                lastSeenPos = pos
            }
        }
        return skipped to outOfOrder
    }

    internal fun wordSimilarity(a: String, b: String): Float {
        if (a == b) return 1.0f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val dist = levenshtein(a, b)
        val maxLen = max(a.length, b.length)
        return (1f - dist.toFloat() / maxLen.toFloat()).coerceAtLeast(0f)
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val prev = IntArray(n + 1) { it }
        val curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, n + 1)
        }
        return prev[n]
    }
}
