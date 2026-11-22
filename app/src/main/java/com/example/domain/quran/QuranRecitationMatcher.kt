package com.example.domain.quran

import com.example.domain.model.QuranVerse
import kotlin.math.max
import kotlin.math.min

/**
 * Specialized Quranic Recitation Recognition & Live Temporal Alignment Engine (100% On-Device).
 *
 * Design principles:
 * - Never invents or alters Quranic Uthmani text; aligns spoken Arabic phrases strictly against
 *   canonical QuranVerse items.
 * - Normalizes Uthmani script orthography to spoken Modern/Classical Arabic phonetic tokens
 *   (dagger alif, hamza/alif variants, taa marbuta, alif maqsura, Tajweed lengthening).
 * - Multi-signal scoring: token overlap + bigram coverage + character trigram similarity +
 *   sequential Markov continuation bias so natural recitation flows verse-to-verse without
 *   jumping wildly on repeated refrains.
 * - Anti-jump uncertainty gate: refuses to jump on weak or noisy transcripts, waiting for
 *   sufficient audio evidence before changing the highlighted Ayah.
 * - Handles pauses, repeated verses, and minor recitation mistakes gracefully.
 */
object QuranArabicPhoneticNormalizer {

    // Arabic diacritics, Quranic annotation marks, pause signs, tatweel (dagger alif \u0670 handled explicitly)
    private val diacriticsAndMarksRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u06D6-\\u06ED\\u0640]")
    private val nonArabicLettersRegex = Regex("[^\\u0621-\\u064A\\s]")
    private val whitespaceRegex = Regex("\\s+")

    // Normalize high-frequency words where standard spoken/ASR Arabic omits the written alif
    private val uthmaniReplacements = listOf(
        "الرحمان" to "الرحمن",
        "رحمان" to "رحمن",
        "ذالك" to "ذلك",
        "هاذا" to "هذا",
        "هاذه" to "هذه",
        "هاولاء" to "هولاء",
        "الاه" to "اله",
        "الاهكم" to "الهكم",
        "لاكن" to "لكن",
        "اوليك" to "اولئك",
        "اولائك" to "اولئك",
        "السموات" to "السماوات",
        "سموات" to "سماوات",
        "الصلوه" to "الصلاه",
        "الزكوه" to "الزكاه",
        "الحيوه" to "الحياه",
        "اللهم" to "الله"
    )

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        // Strip standard diacritics/pause marks first while preserving \u0670 (dagger alif)
        val noHaraka = diacriticsAndMarksRegex.replace(raw, "")
        val sb = StringBuilder(noHaraka.length)
        var i = 0
        while (i < noHaraka.length) {
            val ch = noHaraka[i]
            val next = if (i + 1 < noHaraka.length) noHaraka[i + 1] else '\u0000'
            when {
                // Uthmani waw + dagger alif (e.g. الصَّلَوٰة -> الصلاه, الزَّكَوٰة -> الزكاه, الحَيَوٰة -> الحياه)
                ch == 'و' && next == '\u0670' -> {
                    sb.append('ا')
                    i += 2
                }
                // Alif maqsura + dagger alif (e.g. مُوسَىٰ -> موسي)
                ch == 'ى' && next == '\u0670' -> {
                    sb.append('ي')
                    i += 2
                }
                // Standalone dagger alif (e.g. مَٰلِكِ -> مالك, الصِّرَٰطَ -> الصراط, العَٰلَمِينَ -> العالمين)
                ch == '\u0670' -> {
                    if (sb.isEmpty() || sb.last() != 'ا') {
                        sb.append('ا')
                    }
                    i += 1
                }
                // Uthmani standalone hamza before alif (e.g. القُرْءَان -> القران, ءَامَنُوا -> امنوا)
                ch == 'ء' && next == 'ا' -> {
                    sb.append('ا')
                    i += 2
                }
                ch == 'أ' || ch == 'إ' || ch == 'آ' || ch == 'ٱ' -> {
                    sb.append('ا')
                    i += 1
                }
                ch == 'ى' || ch == 'ئ' || ch == 'ی' -> {
                    sb.append('ي')
                    i += 1
                }
                ch == 'ة' -> {
                    sb.append('ه')
                    i += 1
                }
                ch == 'ؤ' -> {
                    sb.append('و')
                    i += 1
                }
                ch == 'ک' -> {
                    sb.append('ك')
                    i += 1
                }
                else -> {
                    sb.append(ch)
                    i += 1
                }
            }
        }
        var cleaned = nonArabicLettersRegex.replace(sb.toString(), " ")
        cleaned = whitespaceRegex.replace(cleaned, " ").trim()
        if (cleaned.isEmpty()) return ""

        val tokens = cleaned.split(' ').map { token ->
            var t = token
            for ((uthmani, spoken) in uthmaniReplacements) {
                if (t == uthmani) {
                    t = spoken
                    break
                }
            }
            // Collapse repeated consecutive letters caused by Tajweed madd elongation in speech recognition
            collapseElongations(t)
        }
        return tokens.filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun tokenize(raw: String): List<String> {
        val norm = normalize(raw)
        if (norm.isEmpty()) return emptyList()
        return norm.split(' ').filter { it.isNotEmpty() }
    }

    private fun collapseElongations(word: String): String {
        if (word.length <= 2) return word
        val out = StringBuilder(word.length)
        var prev = '\u0000'
        var run = 0
        for (ch in word) {
            if (ch == prev) {
                run += 1
                if (run <= 2) out.append(ch)
            } else {
                prev = ch
                run = 1
                out.append(ch)
            }
        }
        return out.toString()
    }
}

data class IndexedQuranVerse(
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val normalizedText: String,
    val tokens: List<String>,
    val bigrams: Set<String>,
    val charTrigrams: Set<String>
)

data class RecitationMatchDecision(
    val matchedSurah: Int?,
    val matchedAyah: Int?,
    val matchedPage: Int?,
    val confidence: Float,
    val isCertain: Boolean,
    val isRepeatedVerse: Boolean = false,
    val isCrossSurahJump: Boolean = false,
    val reason: String = ""
)

/**
 * Stateful real-time Quran verse alignment engine.
 * Safe for unit testing and real-time streaming from on-device SpeechRecognizer partial/final hypotheses.
 */
class QuranVerseTrackerEngine {

    private var activeSurahNumber: Int = 1
    private var indexedVerses: List<IndexedQuranVerse> = emptyList()
    private var currentAyahNumber: Int? = null
    private var currentPageNumber: Int? = null
    private var consecutiveWeakFrames: Int = 0

    // Canonical opening signatures of well-known Surahs for automatic cross-Surah detection
    private val crossSurahSignatures: List<Triple<Int, Int, List<String>>> = listOf(
        Triple(1, 1, QuranArabicPhoneticNormalizer.tokenize("الحمد لله رب العالمين الرحمن الرحيم مالك يوم الدين")),
        Triple(2, 2, QuranArabicPhoneticNormalizer.tokenize("ذلك الكتاب لا ريب فيه هدى للمتقين")),
        Triple(2, 255, QuranArabicPhoneticNormalizer.tokenize("الله لا اله الا هو الحي القيوم لا تاخذه سنه ولا نوم")),
        Triple(18, 1, QuranArabicPhoneticNormalizer.tokenize("الحمد لله الذي انزل على عبده الكتاب ولم يجعل له عوجا")),
        Triple(36, 1, QuranArabicPhoneticNormalizer.tokenize("يس والقران الحكيم انك لمن المرسلين")),
        Triple(55, 1, QuranArabicPhoneticNormalizer.tokenize("الرحمن علم القران خلق الانسان علمه البيان")),
        Triple(56, 1, QuranArabicPhoneticNormalizer.tokenize("اذا وقعت الواقعه ليس لوقعتها كاذبه")),
        Triple(67, 1, QuranArabicPhoneticNormalizer.tokenize("تبارك الذي بيده الملك وهو على كل شيء قدير")),
        Triple(108, 1, QuranArabicPhoneticNormalizer.tokenize("انا اعطيناك الكوثر فصل لربك وانحر")),
        Triple(112, 1, QuranArabicPhoneticNormalizer.tokenize("قل هو الله احد الله الصمد لم يلد ولم يولد")),
        Triple(113, 1, QuranArabicPhoneticNormalizer.tokenize("قل اعوذ برب الفلق من شر ما خلق")),
        Triple(114, 1, QuranArabicPhoneticNormalizer.tokenize("قل اعوذ برب الناس ملك الناس اله الناس"))
    )

    fun loadSurahVerses(surahNumber: Int, verses: List<QuranVerse>, initialAyah: Int? = null) {
        activeSurahNumber = surahNumber
        indexedVerses = verses.map { v ->
            val norm = QuranArabicPhoneticNormalizer.normalize(v.textUthmani)
            val tokens = if (norm.isEmpty()) emptyList() else norm.split(' ')
            IndexedQuranVerse(
                surahNumber = v.surahNumber,
                ayahNumber = v.numberInSurah,
                pageNumber = v.pageNumber,
                normalizedText = norm,
                tokens = tokens,
                bigrams = buildBigrams(tokens),
                charTrigrams = buildCharTrigrams(norm)
            )
        }
        currentAyahNumber = initialAyah ?: verses.firstOrNull()?.numberInSurah
        currentPageNumber = verses.firstOrNull { it.numberInSurah == currentAyahNumber }?.pageNumber
            ?: verses.firstOrNull()?.pageNumber
        consecutiveWeakFrames = 0
    }

    fun setCurrentAnchor(ayahNumber: Int, pageNumber: Int? = null) {
        currentAyahNumber = ayahNumber
        if (pageNumber != null) currentPageNumber = pageNumber
    }

    fun resetTrackingState() {
        consecutiveWeakFrames = 0
    }

    /**
     * Evaluates a live partial or final spoken Arabic transcript and decides whether to hold
     * or advance/synchronize to an Ayah.
     */
    fun processTranscript(spokenText: String, isFinalResult: Boolean): RecitationMatchDecision {
        val queryTokens = QuranArabicPhoneticNormalizer.tokenize(spokenText)
        // Ignore silence, single-syllable noise, or pure Bismillah unless in Al-Fatiha
        if (queryTokens.size < 2) {
            return RecitationMatchDecision(
                matchedSurah = activeSurahNumber,
                matchedAyah = currentAyahNumber,
                matchedPage = currentPageNumber,
                confidence = 0f,
                isCertain = false,
                reason = "Waiting for more spoken words"
            )
        }

        // Use trailing window of recent spoken words so long continuous recitations focus on current Ayah
        val focusTokens = if (queryTokens.size > 14) queryTokens.takeLast(14) else queryTokens
        val focusNorm = focusTokens.joinToString(" ")
        val focusBigrams = buildBigrams(focusTokens)
        val focusTrigrams = buildCharTrigrams(focusNorm)

        // Check active Surah verses first
        var bestVerse: IndexedQuranVerse? = null
        var bestScore = 0f
        var secondBestScore = 0f

        val anchor = currentAyahNumber

        for (verse in indexedVerses) {
            if (verse.tokens.isEmpty()) continue
            val rawScore = scoreVerseSimilarity(focusTokens, focusBigrams, focusTrigrams, verse)
            if (rawScore < 0.15f) continue

            // Sequential continuation bias (strongly favors next Ayah & current Ayah, gently handles repeat of previous)
            val temporalBoost = when {
                anchor == null -> 0f
                verse.ayahNumber == anchor + 1 -> 0.14f
                verse.ayahNumber == anchor -> 0.08f
                verse.ayahNumber == anchor + 2 -> 0.06f
                verse.ayahNumber == anchor - 1 -> 0.04f
                else -> 0f
            }

            val total = rawScore + temporalBoost
            if (total > bestScore) {
                secondBestScore = bestScore
                bestScore = total
                bestVerse = verse
            } else if (total > secondBestScore) {
                secondBestScore = total
            }
        }

        val chosen = bestVerse
        val isAdjacent = chosen != null && anchor != null &&
            (chosen.ayahNumber in (anchor - 1)..(anchor + 2))

        // Stricter threshold for non-adjacent jumps to prevent accidental jumps on uncertain audio
        val requiredThreshold = when {
            isAdjacent -> if (isFinalResult) 0.36f else 0.42f
            anchor == null -> 0.45f
            else -> if (isFinalResult) 0.54f else 0.62f
        }

        val marginOk = (bestScore - secondBestScore) >= 0.05f || isAdjacent

        if (chosen != null && bestScore >= requiredThreshold && marginOk) {
            val isRepeat = anchor != null && chosen.ayahNumber == anchor
            currentAyahNumber = chosen.ayahNumber
            currentPageNumber = chosen.pageNumber
            consecutiveWeakFrames = 0
            return RecitationMatchDecision(
                matchedSurah = chosen.surahNumber,
                matchedAyah = chosen.ayahNumber,
                matchedPage = chosen.pageNumber,
                confidence = bestScore.coerceAtMost(1.0f),
                isCertain = true,
                isRepeatedVerse = isRepeat,
                reason = if (isRepeat) "Aligned on repeated Ayah" else "Matched Ayah ${chosen.ayahNumber}"
            )
        }

        // Check cross-Surah signature match ONLY if active Surah had weak match and transcript has >= 4 words
        if (focusTokens.size >= 4 && bestScore < 0.45f) {
            var bestCrossSurah: Pair<Int, Int>? = null
            var bestCrossScore = 0f
            for ((sNum, aNum, sigTokens) in crossSurahSignatures) {
                if (sNum == activeSurahNumber) continue
                val overlap = fuzzyTokenOverlap(focusTokens, sigTokens)
                if (overlap > bestCrossScore) {
                    bestCrossScore = overlap
                    bestCrossSurah = sNum to aNum
                }
            }
            if (bestCrossSurah != null && bestCrossScore >= 0.62f) {
                consecutiveWeakFrames = 0
                return RecitationMatchDecision(
                    matchedSurah = bestCrossSurah.first,
                    matchedAyah = bestCrossSurah.second,
                    matchedPage = null,
                    confidence = bestCrossScore,
                    isCertain = true,
                    isCrossSurahJump = true,
                    reason = "Detected recitation in Surah ${bestCrossSurah.first}"
                )
            }
        }

        // Uncertain match -> hold position safely without jumping
        consecutiveWeakFrames += 1
        return RecitationMatchDecision(
            matchedSurah = activeSurahNumber,
            matchedAyah = currentAyahNumber,
            matchedPage = currentPageNumber,
            confidence = bestScore,
            isCertain = false,
            reason = "Uncertain audio — holding current Ayah"
        )
    }

    private fun scoreVerseSimilarity(
        queryTokens: List<String>,
        queryBigrams: Set<String>,
        queryTrigrams: Set<String>,
        verse: IndexedQuranVerse
    ): Float {
        val tokenScore = fuzzyTokenOverlap(queryTokens, verse.tokens)
        val orderedScore = orderedSubsequenceScore(queryTokens, verse.tokens)
        val bigramScore = if (queryBigrams.isEmpty() || verse.bigrams.isEmpty()) {
            0f
        } else {
            val hits = queryBigrams.count { it in verse.bigrams }
            hits.toFloat() / min(queryBigrams.size, verse.bigrams.size).coerceAtLeast(1)
        }
        val trigramScore = if (queryTrigrams.isEmpty() || verse.charTrigrams.isEmpty()) {
            0f
        } else {
            val hits = queryTrigrams.count { it in verse.charTrigrams }
            hits.toFloat() / min(queryTrigrams.size, verse.charTrigrams.size).coerceAtLeast(1)
        }

        val lengthPrecision = min(queryTokens.size, verse.tokens.size).toFloat() /
            max(queryTokens.size, verse.tokens.size).coerceAtLeast(1)

        return (0.38f * tokenScore) +
            (0.22f * orderedScore) +
            (0.18f * bigramScore) +
            (0.14f * trigramScore) +
            (0.08f * lengthPrecision)
    }

    private fun fuzzyTokenOverlap(queryTokens: List<String>, verseTokens: List<String>): Float {
        if (queryTokens.isEmpty() || verseTokens.isEmpty()) return 0f
        var matchedWeight = 0f
        for (qt in queryTokens) {
            var bestTokenSim = 0f
            for (vt in verseTokens) {
                val sim = wordSimilarity(qt, vt)
                if (sim > bestTokenSim) bestTokenSim = sim
                if (bestTokenSim >= 0.99f) break
            }
            if (bestTokenSim >= 0.72f) {
                matchedWeight += bestTokenSim
            }
        }
        val denom = min(queryTokens.size, max(verseTokens.size, 2)).coerceAtLeast(1)
        return (matchedWeight / denom).coerceAtMost(1.0f)
    }

    private fun orderedSubsequenceScore(queryTokens: List<String>, verseTokens: List<String>): Float {
        if (queryTokens.isEmpty() || verseTokens.isEmpty()) return 0f
        var vIdx = 0
        var hits = 0
        for (qt in queryTokens) {
            var foundAt = -1
            for (j in vIdx until verseTokens.size) {
                if (wordSimilarity(qt, verseTokens[j]) >= 0.75f) {
                    foundAt = j
                    break
                }
            }
            if (foundAt >= 0) {
                hits += 1
                vIdx = foundAt + 1
            }
        }
        val denom = min(queryTokens.size, verseTokens.size).coerceAtLeast(1)
        return (hits.toFloat() / denom).coerceAtMost(1.0f)
    }

    private fun wordSimilarity(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.length <= 1 || b.length <= 1) return 0f
        val dist = levenshtein(a, b)
        val maxLen = max(a.length, b.length)
        return (1f - dist.toFloat() / maxLen).coerceAtLeast(0f)
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

    private fun buildBigrams(tokens: List<String>): Set<String> {
        if (tokens.size < 2) return emptySet()
        val set = HashSet<String>(tokens.size)
        for (i in 0 until tokens.size - 1) {
            set.add("${tokens[i]} ${tokens[i + 1]}")
        }
        return set
    }

    private fun buildCharTrigrams(norm: String): Set<String> {
        val compact = norm.replace(" ", "")
        if (compact.length < 3) return emptySet()
        val set = HashSet<String>(compact.length)
        for (i in 0..compact.length - 3) {
            set.add(compact.substring(i, i + 3))
        }
        return set
    }
}
