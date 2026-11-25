package com.example.domain.quran

import com.example.data.quran.QuranLinguisticCatalog
import com.example.data.remote.QuranWordToken

data class QuranWordIrabSarf(
    val position: Int,
    val uthmaniWord: String,
    val transliteration: String,
    val meaningEn: String,
    val partOfSpeechAr: String,
    val partOfSpeechEn: String,
    val irabSummaryAr: String,
    val irabCaseAr: String,
    val sarfRootAr: String,
    val sarfPatternAr: String,
    val sarfDetailsAr: String,
    val isScholarlyVerified: Boolean
)

data class AyahIrabSarfResult(
    val surahNumber: Int,
    val ayahNumber: Int,
    val fullAyahText: String,
    val sentenceNoteAr: String,
    val tafsirMuyassarAr: String,
    val words: List<QuranWordIrabSarf>,
    val isScholarlyVerified: Boolean,
    val sourceLabelAr: String
)

/**
 * Full-Quran Arabic I'rab & Sarf Morphology Analyzer:
 * 1. Uses Verified Classical Scholarly Catalog (`QuranLinguisticCatalog`) first whenever available.
 * 2. For all other 6,236 Ayahs, analyzes every Uthmani word using classical Arabic grammatical
 *    and morphological rules (prepositions, nasb/jazm/inna particles, pronouns, demonstratives,
 *    vowel endings, plural/dual markers, verb tense prefixes, attached pronouns, and triliteral roots).
 */
object QuranMorphologyIrabEngine {

    private val prepositions = setOf(
        "في", "من", "الي", "إلى", "علي", "على", "عن", "حتي", "حتى", "منذ", "مذ", "رب"
    )
    private val conjunctions = setOf("و", "ف", "ثم", "او", "أو", "بل", "لكن", "ام", "أم")
    private val innaSisters = setOf("ان", "إن", "أن", "كان", "كأن", "لكن", "لعل", "ليت")
    private val jazmParticles = setOf("لم", "لما", "لا", "ان", "إن", "من", "ما", "مهما")
    private val nasbParticles = setOf("لن", "كي", "اذن", "إذن", "حتي", "حتى")
    private val relativePronouns = setOf(
        "الذي", "التي", "الذين", "اللاتي", "اللائي", "اللذان", "اللتان", "ما", "من"
    )
    private val demonstratives = setOf(
        "هذا", "هذه", "هذان", "هؤلاء", "ذلك", "ذلكم", "تلك", "اولئك", "أولئك", "هاؤلاء"
    )
    private val detachedPronouns = setOf(
        "هو", "هي", "هما", "هم", "هن", "انت", "أنت", "انتما", "انتم", "أنتم", "انتن", "انا", "أنا", "نحن",
        "اياك", "إياك", "اياه", "إياه", "ايانا", "إيانا"
    )
    private val knownPastVerbs = setOf(
        "قال", "قالوا", "قالت", "كان", "كانوا", "كانت", "جاء", "آمنوا", "امنوا", "كفروا",
        "خلق", "انزل", "أنزل", "جعل", "علم", "هدي", "هدى", "انعمت", "أنعمت", "كتب", "ضرب",
        "سمع", "نظر", "راي", "رأى", "دخل", "خرج", "ارسلنا", "أرسلنا", "قلنا", "اتقوا"
    )
    private val knownImperatives = setOf(
        "قل", "اقرا", "اقرأ", "اهدنا", "اعبدوا", "اتقوا", "اذكروا", "ادعوا", "اصبر", "اصبروا",
        "استغفروا", "صل", "انفقوا", "أنفقوا", "كلوا", "اشربوا", "توبوا", "اسجدوا"
    )

    fun analyzeAyah(
        surahNumber: Int,
        ayahNumber: Int,
        fullAyahText: String,
        wordTokens: List<QuranWordToken>,
        tafsirMuyassarAr: String = ""
    ): AyahIrabSarfResult {
        // 1. Check Verified Classical Scholarly Catalog first
        val verified = QuranLinguisticCatalog.findVerified(surahNumber, ayahNumber)
        if (verified != null) {
            val mapped = verified.words.map { w ->
                val liveToken = wordTokens.getOrNull(w.wordPosition - 1)
                QuranWordIrabSarf(
                    position = w.wordPosition,
                    uthmaniWord = w.exactUthmaniWord,
                    transliteration = liveToken?.transliteration.orEmpty(),
                    meaningEn = liveToken?.meaningEn.orEmpty(),
                    partOfSpeechAr = w.sarf?.partOfSpeechAr ?: "كلمة قرآنية",
                    partOfSpeechEn = w.sarf?.partOfSpeechEn ?: "Word",
                    irabSummaryAr = buildString {
                        w.irab?.grammaticalRoleAr?.let { append(it) }
                        w.irab?.caseEndingAr?.takeIf { it.isNotBlank() }?.let { append(" — $it") }
                        w.irab?.relationshipAr?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
                        w.irab?.explanationAr?.takeIf { it.isNotBlank() }?.let { append("\n$it") }
                    }.ifBlank { "تحليل نحوي موثق" },
                    irabCaseAr = w.irab?.caseEndingAr?.ifBlank { w.irab.grammaticalRoleAr } ?: "—",
                    sarfRootAr = w.sarf?.rootAr ?: "—",
                    sarfPatternAr = w.sarf?.patternAr ?: "—",
                    sarfDetailsAr = listOfNotNull(
                        w.sarf?.derivationAr?.takeIf { it.isNotBlank() && it != "—" }?.let { "الاشتقاق: $it" },
                        w.sarf?.verbFormAr?.takeIf { it.isNotBlank() && it != "—" }?.let { "البناء الصرفي: $it" },
                        w.sarf?.tenseAr?.takeIf { it.isNotBlank() && it != "—" }?.let { "الزمن: $it" },
                        w.sarf?.numberGenderAr?.takeIf { it.isNotBlank() && it != "—" }?.let { "العدد والنوع: $it" },
                        w.sarf?.notesAr?.takeIf { it.isNotBlank() }
                    ).joinToString(" · ").ifBlank { "تحليل صرفي موثق" },
                    isScholarlyVerified = true
                )
            }
            return AyahIrabSarfResult(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                fullAyahText = fullAyahText.ifBlank { verified.exactUthmaniAyahText },
                sentenceNoteAr = verified.sentenceSummaryAr,
                tafsirMuyassarAr = tafsirMuyassarAr.ifBlank { verified.tafsirMuyassarAr },
                words = mapped,
                isScholarlyVerified = true,
                sourceLabelAr = verified.sourceAr
            )
        }

        // 2. Live Rule-Based Classical Arabic Morphology & I'rab Analysis for any of the 6,236 Ayahs
        val tokens = if (wordTokens.isNotEmpty()) {
            wordTokens
        } else {
            fullAyahText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.mapIndexed { idx, w ->
                QuranWordToken(idx + 1, w, "", "")
            }
        }

        val analyzed = mutableListOf<QuranWordIrabSarf>()
        var prevPlain = ""
        tokens.forEachIndexed { idx, token ->
            val word = token.uthmaniWord.trim()
            val plain = stripDiacritics(word)
            val result = analyzeSingleWord(
                position = idx + 1,
                uthmani = word,
                plain = plain,
                prevPlain = prevPlain,
                transliteration = token.transliteration,
                meaningEn = token.meaningEn
            )
            analyzed += result
            prevPlain = plain
        }

        return AyahIrabSarfResult(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            fullAyahText = fullAyahText,
            sentenceNoteAr = "تحليل نحوي وصرفي تفاعلي لكلمات الآية (${analyzed.size} كلمة) مع بيان نوع الكلمة وعلامة الإعراب والجذر الصرفي.",
            tafsirMuyassarAr = tafsirMuyassarAr,
            words = analyzed,
            isScholarlyVerified = false,
            sourceLabelAr = "تحليل نحوي وصرفي حيّ + معاني الكلمات من Quran.com"
        )
    }

    internal fun analyzeSingleWord(
        position: Int,
        uthmani: String,
        plain: String,
        prevPlain: String,
        transliteration: String,
        meaningEn: String
    ): QuranWordIrabSarf {
        val lastVowel = detectEndingVowel(uthmani)
        val isAfterPrep = prevPlain in prepositions || prevPlain.startsWith("ب") && prevPlain.length == 1

        // 1. Lafz Al-Jalalah (الله)
        if (plain == "الله" || plain == "لله" || plain == "بالله" || plain == "تالله" || plain == "والله") {
            val (caseAr, roleAr) = when {
                plain.startsWith("ل") || plain.startsWith("ب") || lastVowel == "الكسرة" ->
                    "مجرور" to "لفظ الجلالة مجرور وعلامة جره الكسرة الظاهرة"
                lastVowel == "الفتحة" ->
                    "منصوب" to "لفظ الجلالة منصوب على التعظيم وعلامة نصبه الفتحة الظاهرة"
                else ->
                    "مرفوع" to "لفظ الجلالة مرفوع (مبتدأ أو فاعل) وعلامة رفعه الضمة الظاهرة"
            }
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "اسم عَلَم (لفظ الجلالة)", "Proper Noun (Divine Name)",
                roleAr, caseAr, "أ ل هـ", "فَعَال / عَلَم",
                "اسم جليل جامد للتعظيم" + if (plain.startsWith("ل")) " · متصل بلام الجر" else "",
                false
            )
        }

        // 2. Prepositions
        if (plain in prepositions) {
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "حرف جر", "Preposition",
                "حرف جر مبني لا محل له من الإعراب، يجرّ الاسم الذي بعده",
                "مبني", "حرف (غير مشتق)", "حرف مبني",
                "حرف جرّ أصلي لا يدخله التصريف", false
            )
        }

        // 3. Inna & sisters
        if (plain in innaSisters) {
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "حرف توكيد ونصب (ناسخ)", "Emphatic Particle",
                "حرف ناسخ مبني لا محل له من الإعراب، ينصب المبتدأ ويرفع الخبر",
                "مبني", "حرف (غير مشتق)", "حرف ناسخ",
                "من الحروف المشبّهة بالفعل (إنّ وأخواتها)", false
            )
        }

        // 4. Detached pronouns
        if (plain in detachedPronouns) {
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "ضمير منفصل", "Pronoun",
                "ضمير منفصل مبني في محل رفع مبتدأ أو نصب مفعول به بحسب السياق",
                "مبني في محل", "ضمير جامد", "ضمير منفصل",
                "من المعارف المبنية التي لا تتغير حركة آخرها", false
            )
        }

        // 5. Relative pronouns & Demonstratives
        if (plain in relativePronouns) {
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "اسم موصول", "Relative Pronoun",
                "اسم موصول مبني في محل رفع أو نصب أو جر بحسب موقعه، والجملة بعده صلة الموصول",
                "مبني في محل", "اسم مبني", "اسم موصول",
                "من الأسماء الموصولة المعرفة", false
            )
        }
        if (plain in demonstratives) {
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "اسم إشارة", "Demonstrative",
                "اسم إشارة مبني في محل رفع أو نصب أو جر بحسب السياق",
                "مبني في محل", "اسم مبني", "اسم إشارة",
                "اسم معرفة يُشار به إلى القريب أو البعيد", false
            )
        }

        // 6. Imperative Verbs
        if (plain in knownImperatives) {
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "فعل أمر", "Imperative Verb",
                "فعل أمر مبني (على السكون أو حذف النون أو حذف حرف العلة) والفاعل ضمير مستتر أو واو الجماعة",
                "مبني", extractApproximateRoot(plain), "اِفْعَلْ / فُلْ",
                "فعل دالّ على الطلب في المستقبل", false
            )
        }

        // 7. Known Past Verbs
        if (plain in knownPastVerbs) {
            val pluralWav = plain.endsWith("وا")
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "فعل ماضٍ", "Past Verb",
                if (pluralWav) "فعل ماضٍ مبني على الضم لاتصاله بواو الجماعة، والواو ضمير متصل في محل رفع فاعل"
                else "فعل ماضٍ مبني على الفتح (أو السكون لاتصاله بضمير رفع متحرك)",
                "مبني", extractApproximateRoot(plain), "فَعَلَ",
                if (pluralWav) "ماضٍ متصرف متصل بواو الجماعة" else "فعل ماضٍ متصرف", false
            )
        }

        // 8. Prefixed Conjunction / Preposition detection
        val prefixes = mutableListOf<String>()
        var stem = plain
        if (stem.length > 3 && (stem.startsWith("و") || stem.startsWith("ف"))) {
            prefixes += if (stem.startsWith("و")) "واو العطف/الاستئناف" else "فاء العطف/السببية"
            stem = stem.drop(1)
        }
        var hasPrefixedPrep = false
        if (stem.length > 3 && (stem.startsWith("بال") || stem.startsWith("لل") || stem.startsWith("كال"))) {
            hasPrefixedPrep = true
            prefixes += when {
                stem.startsWith("بال") -> "باء الجر + أل التعريف"
                stem.startsWith("لل") -> "لام الجر + أل التعريف"
                else -> "كاف التشبيه + أل التعريف"
            }
            stem = stem.drop(1)
        } else if (stem.startsWith("ال") && stem.length > 3) {
            prefixes += "أل التعريف"
            stem = stem.drop(2)
        }

        // Suffixes (attached pronouns / plurals)
        val suffixes = mutableListOf<String>()
        val isSoundMascNom = stem.endsWith("ون") && stem.length >= 4
        val isSoundMascAccGen = stem.endsWith("ين") && stem.length >= 4
        val isSoundFem = stem.endsWith("ات") && stem.length >= 4
        when {
            stem.endsWith("هم") && stem.length > 3 -> { suffixes += "ضمير متصل (هم)"; stem = stem.dropLast(2) }
            stem.endsWith("كم") && stem.length > 3 -> { suffixes += "ضمير متصل (كم)"; stem = stem.dropLast(2) }
            stem.endsWith("نا") && stem.length > 3 -> { suffixes += "ضمير متصل (نا)"; stem = stem.dropLast(2) }
            stem.endsWith("ها") && stem.length > 3 -> { suffixes += "ضمير متصل (ها)"; stem = stem.dropLast(2) }
            isSoundMascNom -> suffixes += "واو ونون (جمع مذكر سالم / أفعال خمسة)"
            isSoundMascAccGen -> suffixes += "ياء ونون (جمع مذكر سالم / مثنى)"
            isSoundFem -> suffixes += "ألف وتاء (جمع مؤنث سالم)"
        }

        // 9. Present-tense verb heuristic (starts with ي / ت / ن / أ without أل and without prefixed prep)
        val looksLikePresentVerb = !hasPrefixedPrep && !prefixes.any { it.contains("أل") } &&
            stem.length in 3..6 &&
            (stem.startsWith("ي") || stem.startsWith("ت") || stem.startsWith("ن")) &&
            !plain.endsWith("ة")

        if (looksLikePresentVerb) {
            val isJazm = prevPlain in jazmParticles || lastVowel == "السكون"
            val isNasb = prevPlain in nasbParticles || lastVowel == "الفتحة"
            val (caseAr, irabAr) = when {
                isSoundMascNom -> "مرفوع بثبوت النون" to "فعل مضارع مرفوع وعلامة رفعه ثبوت النون لأنه من الأفعال الخمسة، والواو فاعل"
                isJazm -> "مجزوم" to "فعل مضارع مجزوم وعلامة جزمه السكون (أو حذف النون/حرف العلة)"
                isNasb -> "منصوب" to "فعل مضارع منصوب وعلامة نصبه الفتحة الظاهرة"
                else -> "مرفوع" to "فعل مضارع مرفوع وعلامة رفعه الضمة الظاهرة (أو المقدرة)"
            }
            val root = extractApproximateRoot(stem.drop(1))
            return QuranWordIrabSarf(
                position, uthmani, transliteration, meaningEn,
                "فعل مضارع", "Present Verb",
                irabAr, caseAr, root, "يَفْعَلُ / تَفْعَلُ",
                buildSarfSummary("مضارع مبدوء بحرف المضارعة (${stem.first()})", prefixes, suffixes),
                false
            )
        }

        // 10. Noun Analysis (الأسماء)
        val (caseAr, signAr) = when {
            isAfterPrep || hasPrefixedPrep -> {
                if (isSoundMascAccGen) "مجرور بالياء" to "اسم مجرور وعلامة جره الياء لأنه جمع مذكر سالم أو مثنى"
                else "مجرور" to "اسم مجرور بحرف الجر وعلامة جره الكسرة الظاهرة (أو المقدرة)"
            }
            isSoundMascNom -> "مرفوع بالواو" to "اسم مرفوع وعلامة رفعه الواو لأنه جمع مذكر سالم"
            isSoundMascAccGen -> "منصوب أو مجرور بالياء" to "اسم منصوب أو مجرور وعلامة نصبه/جره الياء"
            lastVowel == "الضمة" -> "مرفوع" to "اسم مرفوع (مبتدأ / خبر / فاعل / نعت) وعلامة رفعه الضمة"
            lastVowel == "الفتحة" -> "منصوب" to "اسم منصوب (مفعول به / حال / تمييز / اسم إنّ) وعلامة نصبه الفتحة"
            lastVowel == "الكسرة" -> "مجرور" to "اسم مجرور (مضاف إليه / نعت مجرور / بحرف جر) وعلامة جره الكسرة"
            else -> "مُعرب بحسب السياق" to "اسم يُعرب حسب موقعه في الجملة (مبتدأ أو خبر أو فاعل أو مفعول أو مضاف إليه)"
        }

        val root = extractApproximateRoot(stem)
        val pattern = inferApproximatePattern(stem)
        val baseNote = when {
            plain.endsWith("ة") -> "اسم مؤنث بتاء التأنيث المربوطة"
            isSoundFem -> "جمع مؤنث سالم"
            isSoundMascNom || isSoundMascAccGen -> "جمع مذكر سالم"
            else -> "اسم مُعرب"
        }

        return QuranWordIrabSarf(
            position, uthmani, transliteration, meaningEn,
            "اسم", "Noun",
            signAr, caseAr, root, pattern,
            buildSarfSummary(baseNote, prefixes, suffixes),
            false
        )
    }

    private fun buildSarfSummary(base: String, prefixes: List<String>, suffixes: List<String>): String {
        val parts = mutableListOf(base)
        if (prefixes.isNotEmpty()) parts += "السوابق: ${prefixes.joinToString(" + ")}"
        if (suffixes.isNotEmpty()) parts += "اللواحق: ${suffixes.joinToString(" + ")}"
        return parts.joinToString(" · ")
    }

    internal fun stripDiacritics(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val code = ch.code
            // Skip Arabic harakat, Quranic annotation marks, and tatweel
            if (code in 0x064B..0x065F || code == 0x0670 || code in 0x06D6..0x06ED || code == 0x0640) continue
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ى' -> sb.append('ي')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun detectEndingVowel(uthmani: String): String {
        for (i in uthmani.length - 1 downTo 0) {
            when (uthmani[i]) {
                '\u064F', '\u064C' -> return "الضمة"
                '\u064E', '\u064B' -> return "الفتحة"
                '\u0650', '\u064D' -> return "الكسرة"
                '\u0652' -> return "السكون"
            }
        }
        return ""
    }

    private fun extractApproximateRoot(stem: String): String {
        val clean = stem.trimEnd('ة', 'ا', 'و', 'ي', 'ن', 'ت')
            .ifBlank { stem }
        val letters = clean.filter { it in '\u0621'..'\u064A' }
        val rootChars = when {
            letters.length <= 3 -> letters
            else -> letters.filterNot { it in setOf('ا', 'و', 'ي', 'م', 'ت') }.take(3).ifEmpty { letters.take(3) }
        }
        return rootChars.toCharArray().joinToString(" ").ifBlank { "—" }
    }

    private fun inferApproximatePattern(stem: String): String = when {
        stem.length == 4 && stem.startsWith("م") -> "مَفْعَل / مُفْعِل"
        stem.length == 5 && stem.startsWith("م") && stem[3] == 'و' -> "مَفْعُول"
        stem.length == 4 && stem[1] == 'ا' -> "فَاعِل"
        stem.length == 4 && stem[2] == 'ي' -> "فَعِيل"
        stem.length == 4 && stem[2] == 'ا' -> "فَعَال / فِعَال"
        stem.length == 3 -> "فَعْل / فَعَل"
        else -> "وزن صرفي مشتق"
    }
}
