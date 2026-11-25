package com.example.domain.quran

import com.example.domain.model.QuranVerse

/**
 * Single immutable Uthmani Quranic word belonging to the active Mushaf page or Surah scope.
 * Never mutates or rewrites canonical Quranic text.
 */
data class ExpectedPageWord(
    val globalIndex: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordIndexInAyah: Int,
    val displayUthmani: String,
    val normalizedToken: String,
    val isLastWordOfAyah: Boolean,
    val isFirstWordOfSurah: Boolean
)

/**
 * Reactive snapshot for the "Write-As-I-Recite" (يكتب وراي) progressive Quran memorization UI.
 */
data class ProgressiveRecitationState(
    val scopeTitleAr: String = "سورة الفاتحة",
    val scopeTitleEn: String = "Al-Fatihah",
    val words: List<ExpectedPageWord> = emptyList(),
    val verses: List<QuranVerse> = emptyList(),
    val revealedCount: Int = 0,
    val totalWords: Int = 0,
    val currentSurahNumber: Int = 1,
    val currentAyahNumber: Int = 1,
    val mistakeCount: Int = 0,
    val mistakeEventId: Int = 0,
    val isMistakeActive: Boolean = false,
    val lastMistakeSpoken: String = "",
    val lastMistakeAyah: Int = 1,
    val lastMatchedWord: String = ""
) {
    val isCompleted: Boolean
        get() = totalWords > 0 && revealedCount >= totalWords

    val progressFraction: Float
        get() = if (totalWords <= 0) 0f else (revealedCount.toFloat() / totalWords.toFloat()).coerceIn(0f, 1f)

    val hiddenRemainingCount: Int
        get() = (totalWords - revealedCount).coerceAtLeast(0)

    val currentExpectedWord: ExpectedPageWord?
        get() = words.getOrNull(revealedCount)
}

/**
 * Pure-Kotlin word-by-word progressive Quran dictation tracker across ALL 114 Surahs and
 * ALL 604 Madinah Mushaf pages.
 *
 * Behavior guarantees:
 * 1. All words in the loaded Mushaf page / Surah start hidden (`revealedCount = 0`).
 * 2. Each correctly recited Arabic word reveals and advances the cursor by 1 Uthmani word
 *    ("أنا أقول وهو يكتب من ورائي").
 * 3. When a spoken word does NOT match the next expected word at `words[revealedCount]`:
 *    - `revealedCount` NEVER advances (does NOT reveal/write the hidden word).
 *    - `mistakeEventId` & `mistakeCount` increment so the UI flashes feedback and plays the
 *      audible mistake alert sound ("عندما أخطأ فيه لا يكتب يطلع صوت").
 * 4. Handles live partial STT prefixes gracefully without false-positive beeps mid-word, while
 *    strictly enforcing word accuracy on completed tokens and final transcripts.
 */
class ProgressiveRecitationTracker {

    private var currentTitleAr: String = "سورة الفاتحة"
    private var currentTitleEn: String = "Al-Fatihah"
    private var loadedVerses: List<QuranVerse> = emptyList()
    private var expectedWords: List<ExpectedPageWord> = emptyList()
    private var revealedCount: Int = 0
    private var mistakeCount: Int = 0
    private var mistakeEventId: Int = 0
    private var isMistakeActive: Boolean = false
    private var lastMistakeSpoken: String = ""
    private var lastMistakeAyah: Int = 1
    private var lastMatchedWord: String = ""

    private var lastTokens: List<String> = emptyList()
    private var consumedTokenCount: Int = 0

    fun getState(): ProgressiveRecitationState {
        val currentTarget = expectedWords.getOrNull(revealedCount) ?: expectedWords.lastOrNull()
        return ProgressiveRecitationState(
            scopeTitleAr = currentTitleAr,
            scopeTitleEn = currentTitleEn,
            words = expectedWords,
            verses = loadedVerses,
            revealedCount = revealedCount,
            totalWords = expectedWords.size,
            currentSurahNumber = currentTarget?.surahNumber ?: loadedVerses.firstOrNull()?.surahNumber ?: 1,
            currentAyahNumber = currentTarget?.ayahNumber ?: loadedVerses.firstOrNull()?.numberInSurah ?: 1,
            mistakeCount = mistakeCount,
            mistakeEventId = mistakeEventId,
            isMistakeActive = isMistakeActive,
            lastMistakeSpoken = lastMistakeSpoken,
            lastMistakeAyah = lastMistakeAyah,
            lastMatchedWord = lastMatchedWord
        )
    }

    fun loadVerses(
        verses: List<QuranVerse>,
        titleAr: String,
        titleEn: String,
        startAtSurah: Int? = null,
        startAtAyah: Int? = null
    ): ProgressiveRecitationState {
        currentTitleAr = titleAr
        currentTitleEn = titleEn
        loadedVerses = verses

        val flat = mutableListOf<ExpectedPageWord>()
        var lastSurahSeen = -1
        for (v in verses) {
            val rawTokens = v.textUthmani.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (rawTokens.isEmpty()) continue
            val isFirstSurahStart = (v.surahNumber != lastSurahSeen) && v.numberInSurah == 1
            lastSurahSeen = v.surahNumber

            rawTokens.forEachIndexed { wordIdx, rawWord ->
                val norm = QuranArabicPhoneticNormalizer.normalize(rawWord)
                    .replace(" ", "")
                    .ifBlank { rawWord }
                flat.add(
                    ExpectedPageWord(
                        globalIndex = flat.size,
                        surahNumber = v.surahNumber,
                        ayahNumber = v.numberInSurah,
                        wordIndexInAyah = wordIdx,
                        displayUthmani = rawWord,
                        normalizedToken = norm,
                        isLastWordOfAyah = wordIdx == rawTokens.lastIndex,
                        isFirstWordOfSurah = isFirstSurahStart && wordIdx == 0
                    )
                )
            }
        }

        expectedWords = flat
        revealedCount = if (startAtAyah != null) {
            val idx = expectedWords.indexOfFirst { word ->
                word.ayahNumber == startAtAyah && (startAtSurah == null || word.surahNumber == startAtSurah)
            }
            if (idx > 0) idx else 0
        } else {
            0
        }
        mistakeCount = 0
        mistakeEventId = 0
        isMistakeActive = false
        lastMistakeSpoken = ""
        lastMistakeAyah = expectedWords.getOrNull(revealedCount)?.ayahNumber ?: 1
        lastMatchedWord = ""
        lastTokens = emptyList()
        consumedTokenCount = 0
        return getState()
    }

    fun jumpToAyah(surahNumber: Int, ayahNumber: Int): ProgressiveRecitationState {
        val idx = expectedWords.indexOfFirst { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
        if (idx >= 0) {
            revealedCount = idx
            isMistakeActive = false
            lastMistakeSpoken = ""
            lastMistakeAyah = ayahNumber
            consumedTokenCount = lastTokens.size
        }
        return getState()
    }

    fun reset(): ProgressiveRecitationState {
        revealedCount = 0
        mistakeCount = 0
        isMistakeActive = false
        lastMistakeSpoken = ""
        lastMistakeAyah = expectedWords.firstOrNull()?.ayahNumber ?: 1
        lastMatchedWord = ""
        lastTokens = emptyList()
        consumedTokenCount = 0
        return getState()
    }

    /**
     * Reveals only up to [revealedCount] without skipping unrecited words. Useful if a user asks
     * for a single-word nudge when genuinely stuck (explicit user click, never automatic).
     */
    fun nudgeRevealSingleWord(): ProgressiveRecitationState {
        if (revealedCount < expectedWords.size) {
            lastMatchedWord = expectedWords[revealedCount].displayUthmani
            revealedCount += 1
            isMistakeActive = false
            lastMistakeSpoken = ""
        }
        return getState()
    }

    /**
     * Evaluates an incoming Arabic speech transcript against the unrevealed cursor frontier.
     *
     * @param fullTranscript accumulated transcript from STT or manual dictation.
     * @param isPartialStreaming true while Android SpeechRecognizer is still emitting partial hypotheses
     *        mid-utterance; prevents premature mistake beeps on an incomplete final syllable prefix.
     */
    fun processTranscript(
        fullTranscript: String,
        isPartialStreaming: Boolean = false
    ): ProgressiveRecitationState {
        if (expectedWords.isEmpty()) return getState()
        val newTokens = QuranArabicPhoneticNormalizer.tokenize(fullTranscript)
        if (newTokens.isEmpty()) {
            lastTokens = emptyList()
            consumedTokenCount = 0
            return getState()
        }

        // Compute how far the already-consumed prefix still agrees with newTokens
        var commonPrefix = 0
        val limit = minOf(lastTokens.size, newTokens.size, consumedTokenCount)
        while (commonPrefix < limit && lastTokens[commonPrefix] == newTokens[commonPrefix]) {
            commonPrefix += 1
        }
        var idx = commonPrefix

        while (idx < newTokens.size) {
            if (revealedCount >= expectedWords.size) {
                idx = newTokens.size
                break
            }

            val spoken = newTokens[idx]
            val target = expectedWords[revealedCount]

            // 1) Direct 1-to-1 phonetic/Uthmani word match
            if (wordsMatch(spoken, target.normalizedToken)) {
                lastMatchedWord = target.displayUthmani
                revealedCount += 1
                isMistakeActive = false
                lastMistakeSpoken = ""
                idx += 1
                continue
            }

            // 2) Two spoken STT tokens merged into one Uthmani compound word (e.g. "يا أيها" -> "يأيها")
            if (idx + 1 < newTokens.size) {
                val combinedSpoken = spoken + newTokens[idx + 1]
                if (wordsMatch(combinedSpoken, target.normalizedToken)) {
                    lastMatchedWord = target.displayUthmani
                    revealedCount += 1
                    isMistakeActive = false
                    lastMistakeSpoken = ""
                    idx += 2
                    continue
                }
            }

            // 3) One spoken STT token covering two consecutive short Uthmani tokens (e.g. "ما لكم" vs "مالكم")
            if (revealedCount + 1 < expectedWords.size) {
                val nextTarget = expectedWords[revealedCount + 1]
                val combinedExpected = target.normalizedToken + nextTarget.normalizedToken
                if (wordsMatch(spoken, combinedExpected)) {
                    lastMatchedWord = "${target.displayUthmani} ${nextTarget.displayUthmani}"
                    revealedCount += 2
                    isMistakeActive = false
                    lastMistakeSpoken = ""
                    idx += 1
                    continue
                }
            }

            // 4) If still actively speaking partial tail and spoken is a valid prefix of target word, wait cleanly
            val isTrailingToken = idx == newTokens.lastIndex
            if (isPartialStreaming && isTrailingToken && isLikelyPartialPrefix(spoken, target.normalizedToken)) {
                // Leave unconsumed until the recognizer finishes the word
                break
            }

            // 5) Gracefully absorb an accidental immediate repetition of the just-revealed word without advancing or beeping
            if (revealedCount > 0 && wordsMatch(spoken, expectedWords[revealedCount - 1].normalizedToken)) {
                idx += 1
                continue
            }

            // 6) Genuine mismatch: DO NOT advance/reveal expectedWords[revealedCount], emit mistake event!
            isMistakeActive = true
            mistakeCount += 1
            mistakeEventId += 1
            lastMistakeSpoken = spoken
            lastMistakeAyah = target.ayahNumber
            idx += 1
        }

        lastTokens = newTokens
        consumedTokenCount = idx
        return getState()
    }

    private fun wordsMatch(spokenNorm: String, expectedNorm: String): Boolean {
        if (spokenNorm == expectedNorm) return true
        if (spokenNorm.isBlank() || expectedNorm.isBlank()) return false

        val sim = QuranRecitationAssessmentEngine.wordSimilarity(spokenNorm, expectedNorm)
        val threshold = when {
            expectedNorm.length <= 2 -> 0.99f
            expectedNorm.length <= 4 -> 0.78f
            else -> 0.76f
        }
        if (sim >= threshold) return true

        // Optional leading vocative/conjunction Uthmani Alif flexibility (e.g., يا vs يأ, ال vs ل)
        val trimmedExpected = expectedNorm.removePrefix("و").removePrefix("ف")
        val trimmedSpoken = spokenNorm.removePrefix("و").removePrefix("ف")
        if (spokenNorm.firstOrNull() == expectedNorm.firstOrNull() &&
            trimmedSpoken.isNotEmpty() &&
            trimmedExpected.isNotEmpty() &&
            QuranRecitationAssessmentEngine.wordSimilarity(trimmedSpoken, trimmedExpected) >= 0.85f
        ) {
            return true
        }
        return false
    }

    private fun isLikelyPartialPrefix(spokenPartial: String, expectedNorm: String): Boolean {
        if (spokenPartial.length < 2 || spokenPartial.length >= expectedNorm.length) return false
        return expectedNorm.startsWith(spokenPartial)
    }
}
