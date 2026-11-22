package com.example.domain.ai

import com.example.data.local.db.LibraryBookEntity
import com.example.data.local.db.LocalAiKnowledgeEntity
import com.example.domain.quran.QuranArabicPhoneticNormalizer
import com.example.security.LocalAdminSecurityManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min

enum class AiVerificationLevel {
    VERIFIED,
    PARTIAL_MATCH,
    UNCERTAIN
}

data class AiCitedSource(
    val title: String,
    val reference: String,
    val kind: String // KNOWLEDGE_BASE, LIBRARY, QURAN_TEXT
)

data class NoorAiAnswer(
    val query: String,
    val answerAr: String,
    val answerEn: String,
    val verificationLevel: AiVerificationLevel,
    val sources: List<AiCitedSource>,
    val scholarlyNote: String,
    val disclaimerAr: String,
    val disclaimerEn: String,
    val confidencePercent: Int
)

data class ImportedKnowledgeBatch(
    val items: List<LocalAiKnowledgeEntity>,
    val skippedDuplicates: Int,
    val parseErrors: Int
)

/**
 * 100% Offline Local Islamic Intelligence & Knowledge Retrieval Engine (NOOR AI).
 *
 * Strict Islamic Accuracy & Transparency Architecture:
 * - Operates completely locally on device without any external paid API keys or internet requirement.
 * - Uses Arabic phonetic normalization, stop-word filtering, token/bigram overlap, and character-trigram
 *   relevance ranking over the verified Local AI Knowledge Base and Local Islamic Library.
 * - Never fabricates Quranic verses or Hadith.
 * - Always separates verified matched answers from uncertain/unmatched questions.
 * - Always attaches source references, scholarly difference notes where applicable, and an explicit
 *   reminder that this local engine is an educational search assistant — NOT a Mufti — and complex
 *   Fiqh rulings must be referred to qualified scholars.
 * - Transparently treats imported TXT/JSON documents as indexed local search/retrieval knowledge
 *   (RAG index), NEVER claiming to retrain a neural language model on device.
 */
object NoorLocalAiEngine {

    private const val MUFTI_DISCLAIMER_AR =
        "تنبيه شرعي: محرك «نور الذكي المحلي» أداة بحث تعليمية غير متصلة بالإنترنت تعتمد على قاعدة المعرفة المحلية الموثّقة فقط، وليس مفتياً شرعياً. في المسائل الفقهية المعقدة أو الفتاوى الخاصة يُرجى الرجوع إلى العلماء والمفتين المعتمدين."

    private const val MUFTI_DISCLAIMER_EN =
        "Notice: Local NOOR AI is an offline educational retrieval assistant powered only by verified local knowledge — it is NOT a qualified Mufti. Please consult qualified Islamic scholars for personal or complex Fiqh rulings."

    private val arabicStopWords = setOf(
        "ما", "هل", "من", "في", "على", "عن", "الى", "إلى", "ان", "أن", "إن",
        "هذا", "هذه", "التي", "الذي", "كيف", "لماذا", "متى", "كم", "او", "أو",
        "هو", "هي", "هم", "مع", "كل", "بعد", "قبل", "عند", "بين", "حول",
        "the", "is", "are", "what", "how", "why", "who", "in", "of", "to", "for", "and", "a", "an"
    )

    fun answerQuestion(
        query: String,
        approvedKnowledge: List<LocalAiKnowledgeEntity>,
        approvedBooks: List<LibraryBookEntity>
    ): NoorAiAnswer {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return NoorAiAnswer(
                query = trimmed,
                answerAr = "يرجى كتابة سؤالك الإسلامي أو القرآني بوضوح للبحث في قاعدة المعرفة المحلية الموثّقة.",
                answerEn = "Please enter an Islamic or Quranic question to search the verified offline knowledge base.",
                verificationLevel = AiVerificationLevel.UNCERTAIN,
                sources = emptyList(),
                scholarlyNote = "",
                disclaimerAr = MUFTI_DISCLAIMER_AR,
                disclaimerEn = MUFTI_DISCLAIMER_EN,
                confidencePercent = 0
            )
        }

        val queryTokens = extractSearchTokens(trimmed)
        val queryTrigrams = buildTrigrams(normalizeMixed(trimmed))

        // 1. Score verified Q&A knowledge base entries
        val scoredKnowledge = approvedKnowledge.map { entry ->
            val docText = "${entry.titleAr} ${entry.titleEn} ${entry.questionAr} ${entry.questionEn} ${entry.answerAr} ${entry.category} ${entry.sourceRef}"
            val score = scoreMatch(queryTokens, queryTrigrams, docText, titleBoostText = "${entry.titleAr} ${entry.questionAr} ${entry.questionEn}")
            entry to score
        }.filter { it.second >= 0.16f }.sortedByDescending { it.second }

        // 2. Score Islamic library books for supplemental authentic excerpts
        val scoredBooks = approvedBooks.filter { it.approvalStatus == "APPROVED" }.map { book ->
            val docText = "${book.titleAr} ${book.titleEn} ${book.summaryAr} ${book.contentText.take(2400)}"
            val score = scoreMatch(queryTokens, queryTrigrams, docText, titleBoostText = "${book.titleAr} ${book.summaryAr}")
            book to score
        }.filter { it.second >= 0.18f }.sortedByDescending { it.second }

        val topKb = scoredKnowledge.firstOrNull()
        val topBook = scoredBooks.firstOrNull()

        if (topKb != null && topKb.second >= 0.30f) {
            val item = topKb.first
            val sources = mutableListOf<AiCitedSource>()
            sources.add(AiCitedSource(title = item.titleAr, reference = item.sourceRef.ifBlank { "قاعدة المعرفة المحلية" }, kind = "KNOWLEDGE_BASE"))
            scoredKnowledge.drop(1).take(2).filter { it.second >= 0.25f }.forEach { (extra, _) ->
                sources.add(AiCitedSource(title = extra.titleAr, reference = extra.sourceRef, kind = "KNOWLEDGE_BASE"))
            }
            if (topBook != null && topBook.second >= 0.24f) {
                sources.add(AiCitedSource(title = topBook.first.titleAr, reference = topBook.first.sourceReference.ifBlank { topBook.first.authorAr }, kind = "LIBRARY"))
            }
            val level = if (item.verificationStatus == "VERIFIED" && topKb.second >= 0.42f) {
                AiVerificationLevel.VERIFIED
            } else {
                AiVerificationLevel.PARTIAL_MATCH
            }
            return NoorAiAnswer(
                query = trimmed,
                answerAr = item.answerAr,
                answerEn = item.answerEn.ifBlank { item.answerAr },
                verificationLevel = level,
                sources = sources,
                scholarlyNote = item.scholarlyNotes,
                disclaimerAr = MUFTI_DISCLAIMER_AR,
                disclaimerEn = MUFTI_DISCLAIMER_EN,
                confidencePercent = (topKb.second * 100).toInt().coerceIn(35, 98)
            )
        }

        // Fall back to Library excerpt if found
        if (topBook != null && topBook.second >= 0.24f) {
            val book = topBook.first
            val excerpt = extractBestExcerpt(book.contentText, queryTokens)
            return NoorAiAnswer(
                query = trimmed,
                answerAr = "من المكتبة الإسلامية المحلية (${book.titleAr}):\n\n$excerpt",
                answerEn = "From Local Islamic Library (${book.titleEn}):\n\n$excerpt",
                verificationLevel = AiVerificationLevel.PARTIAL_MATCH,
                sources = listOf(
                    AiCitedSource(
                        title = book.titleAr,
                        reference = book.sourceReference.ifBlank { book.authorAr },
                        kind = "LIBRARY"
                    )
                ),
                scholarlyNote = "هذا مقتطف مباشر من كتاب مخزّن محلياً؛ راجع الكتاب كاملاً في «مكتبة نور» أو اسأل أهل العلم للتفصيل.",
                disclaimerAr = MUFTI_DISCLAIMER_AR,
                disclaimerEn = MUFTI_DISCLAIMER_EN,
                confidencePercent = (topBook.second * 100).toInt().coerceIn(25, 82)
            )
        }

        // Safe Islamic Guardrail: Never fabricate an answer, verse, or Hadith when uncertain!
        return NoorAiAnswer(
            query = trimmed,
            answerAr = "لم يتم العثور على إجابة موثّقة مطابقة لهذا السؤال في قاعدة المعرفة المحلية الحالية.\n\nالتزاماً بالأمانة العلمية، لا يقوم «نور» بتأليف إجابات شرعية أو نسب آيات وأحاديث دون مصدر موثّق. يمكنك:\n• صياغة السؤال بكلمات أبسط (مثل: الصلاة، الصيام، الزكاة، التجويد، حفظ القرآن، السيرة).\n• الرجوع إلى أهل العلم والفتوى المعتمدين.\n• أو إضافة مراجع موثّقة جديدة إلى الفهرس المحلي عبر لوحة الإدارة المحلية.",
            answerEn = "No verified matching entry was found in the local offline knowledge base. To uphold Islamic scholarly accuracy, NOOR never invents verses, Hadith, or rulings. Try simpler keywords (Salah, Fasting, Zakah, Tajweed, Hifz, Seerah), consult a qualified scholar, or add verified references via Local Admin.",
            verificationLevel = AiVerificationLevel.UNCERTAIN,
            sources = emptyList(),
            scholarlyNote = "الوقوف عند حد العلم الموثّق أمانة؛ ومن قال لا أعلم فقد أفتى.",
            disclaimerAr = MUFTI_DISCLAIMER_AR,
            disclaimerEn = MUFTI_DISCLAIMER_EN,
            confidencePercent = 0
        )
    }

    /**
     * Parses local TXT or JSON files imported by Admin/Editor via Storage Access Framework.
     * Note: Imported items update the local offline search index (they do NOT retrain an LLM).
     */
    fun parseImportedKnowledgeText(
        rawContent: String,
        fileName: String,
        defaultCategory: String,
        existingHashes: Set<String>
    ): ImportedKnowledgeBatch {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return ImportedKnowledgeBatch(emptyList(), 0, 1)

        val parsed = mutableListOf<LocalAiKnowledgeEntity>()
        val seenHashes = existingHashes.toMutableSet()
        var duplicates = 0
        var errors = 0

        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                val array = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("items") ?: obj.optJSONArray("knowledge") ?: JSONArray().put(obj)
                }
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val qAr = obj.optString("questionAr", obj.optString("question", "")).trim()
                    val aAr = obj.optString("answerAr", obj.optString("answer", "")).trim()
                    if (qAr.isBlank() || aAr.isBlank()) {
                        errors++
                        continue
                    }
                    val hash = LocalAdminSecurityManager.sha256Hex("$qAr|$aAr")
                    if (!seenHashes.add(hash)) {
                        duplicates++
                        continue
                    }
                    parsed.add(
                        LocalAiKnowledgeEntity(
                            id = "ai_imp_${UUID.randomUUID().toString().take(10)}",
                            category = obj.optString("category", defaultCategory).uppercase(),
                            titleAr = obj.optString("titleAr", obj.optString("title", qAr.take(60))).trim(),
                            titleEn = obj.optString("titleEn", "").trim(),
                            questionAr = qAr,
                            questionEn = obj.optString("questionEn", "").trim(),
                            answerAr = aAr,
                            answerEn = obj.optString("answerEn", "").trim(),
                            sourceRef = obj.optString("sourceRef", obj.optString("source", fileName)).trim(),
                            verificationStatus = obj.optString("verificationStatus", "REVIEW").uppercase(),
                            scholarlyNotes = obj.optString("scholarlyNotes", "").trim(),
                            contentHash = hash,
                            approvalStatus = "PENDING",
                            isUserAdded = true
                        )
                    )
                }
                return ImportedKnowledgeBatch(parsed, duplicates, errors)
            } catch (_: Exception) {
                // Fall through to plain TXT parser
            }
        }

        // Plain TXT parser: supports "Q: ... / A: ..." or "س: ... / ج: ..." blocks separated by blank lines
        val blocks = trimmed.split(Regex("\\r?\\n\\s*\\r?\\n")).map { it.trim() }.filter { it.isNotBlank() }
        for ((idx, block) in blocks.withIndex()) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            val question = lines.first().removePrefix("س:").removePrefix("Q:").removePrefix("السؤال:").trim()
            val answer = if (lines.size > 1) {
                lines.drop(1).joinToString("\n").removePrefix("ج:").removePrefix("A:").removePrefix("الجواب:").trim()
            } else {
                block
            }
            val title = question.take(65).ifBlank { "$fileName #${idx + 1}" }
            val hash = LocalAdminSecurityManager.sha256Hex("$question|$answer")
            if (!seenHashes.add(hash)) {
                duplicates++
                continue
            }
            parsed.add(
                LocalAiKnowledgeEntity(
                    id = "ai_txt_${UUID.randomUUID().toString().take(10)}",
                    category = defaultCategory.uppercase(),
                    titleAr = title,
                    titleEn = title,
                    questionAr = question,
                    answerAr = answer,
                    sourceRef = "ملف مستورد: $fileName",
                    verificationStatus = "REVIEW",
                    contentHash = hash,
                    approvalStatus = "PENDING",
                    isUserAdded = true
                )
            )
        }

        return ImportedKnowledgeBatch(parsed, duplicates, errors)
    }

    internal fun normalizeMixed(input: String): String {
        val arabicNorm = QuranArabicPhoneticNormalizer.normalize(input)
        val latinNorm = input.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
        return "$arabicNorm $latinNorm".trim()
    }

    private fun extractSearchTokens(input: String): List<String> {
        return normalizeMixed(input)
            .split(' ')
            .map { it.trim() }
            .filter { it.length >= 2 && it !in arabicStopWords }
            .distinct()
    }

    private fun scoreMatch(
        queryTokens: List<String>,
        queryTrigrams: Set<String>,
        docText: String,
        titleBoostText: String
    ): Float {
        if (queryTokens.isEmpty()) return 0f
        val docNorm = normalizeMixed(docText)
        val docTokens = docNorm.split(' ').filter { it.length >= 2 }.toSet()
        val titleNorm = normalizeMixed(titleBoostText)

        var tokenHits = 0f
        for (qt in queryTokens) {
            when {
                qt in docTokens -> tokenHits += 1.0f
                docNorm.contains(qt) -> tokenHits += 0.78f
                qt.length >= 4 && docTokens.any { it.startsWith(qt.take(4)) || qt.startsWith(it.take(4)) } -> tokenHits += 0.5f
            }
        }
        val tokenScore = (tokenHits / queryTokens.size.coerceAtLeast(1)).coerceAtMost(1f)

        var titleHits = 0f
        for (qt in queryTokens) {
            if (titleNorm.contains(qt)) titleHits += 1f
        }
        val titleScore = (titleHits / queryTokens.size.coerceAtLeast(1)).coerceAtMost(1f)

        val docTrigrams = buildTrigrams(docNorm.take(600))
        val trigramScore = if (queryTrigrams.isEmpty() || docTrigrams.isEmpty()) {
            0f
        } else {
            val overlap = queryTrigrams.count { it in docTrigrams }
            overlap.toFloat() / min(queryTrigrams.size, docTrigrams.size).coerceAtLeast(1)
        }

        return (0.50f * tokenScore) + (0.32f * titleScore) + (0.18f * trigramScore)
    }

    private fun extractBestExcerpt(content: String, queryTokens: List<String>): String {
        val paragraphs = content.split(Regex("\\r?\\n\\s*\\r?\\n")).map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) return content.take(400)
        val best = paragraphs.maxByOrNull { p ->
            val norm = normalizeMixed(p)
            queryTokens.count { norm.contains(it) }
        } ?: paragraphs.first()
        return best.take(550)
    }

    private fun buildTrigrams(norm: String): Set<String> {
        val compact = norm.replace(" ", "")
        if (compact.length < 3) return emptySet()
        val set = HashSet<String>(compact.length)
        for (i in 0..compact.length - 3) {
            set.add(compact.substring(i, i + 3))
        }
        return set
    }
}
