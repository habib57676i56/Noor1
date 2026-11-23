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
    val parseErrors: Int,
    val detectedLanguage: String = "AR",
    val detectedCategory: String = "GENERAL",
    val warningAr: String? = null,
    val warningEn: String? = null
)

data class AiIndexStats(
    val totalEntries: Int = 0,
    val approvedEntries: Int = 0,
    val pendingEntries: Int = 0,
    val indexedLibraryBooks: Int = 0,
    val uniqueTokenCount: Int = 0,
    val lastReindexedAtMillis: Long = 0L
)

/**
 * 100% Offline Local Islamic Intelligence & Knowledge Retrieval Engine (NOOR AI — Local RAG).
 *
 * Strict Islamic Accuracy & Transparency Architecture:
 * - Operates completely locally on device without any external paid API keys or internet requirement.
 * - Understands Classical Arabic, Modern Standard Arabic, English, and common colloquial Arabic
 *   (Levantine/Syrian/Gulf/Egyptian) phrasing by normalizing dialectal expressions before retrieval.
 * - Uses Arabic phonetic normalization, keyword/tag boosting, token/bigram overlap, character trigrams,
 *   and optional recent conversation context over the verified Local AI Knowledge Base & Library.
 * - Never fabricates Quranic verses, Hadith, or Fatwas.
 * - Explicitly discloses that imported TXT/JSON/DOCX/PDF documents update the local offline retrieval
 *   index (RAG) and do NOT retrain a neural LLM weight file on device.
 */
object NoorLocalAiEngine {

    private const val MUFTI_DISCLAIMER_AR =
        "تنبيه شرعي وتقني: محرك «نور الذكي المحلي» يعتمد على البحث والاسترجاع المحلي الموثّق (Local RAG) دون إنترنت، وليس مفتياً شرعياً ولا يعيد تدريب نموذج لغوي عصبي عند استيراد الملفات. في المسائل الفقهية المعقدة يُرجى الرجوع إلى العلماء والمفتين المعتمدين."

    private const val MUFTI_DISCLAIMER_EN =
        "Notice: Local NOOR AI is an offline retrieval assistant (Local RAG) powered strictly by verified local knowledge — it is NOT a Mufti and importing files updates the local search index rather than retraining a neural LLM. Please consult qualified scholars for complex Fiqh rulings."

    private val arabicStopWords = setOf(
        "ما", "هل", "من", "في", "على", "عن", "الى", "إلى", "ان", "أن", "إن",
        "هذا", "هذه", "التي", "الذي", "كيف", "لماذا", "متى", "كم", "او", "أو",
        "هو", "هي", "هم", "مع", "كل", "بعد", "قبل", "عند", "بين", "حول",
        "the", "is", "are", "what", "how", "why", "who", "in", "of", "to", "for", "and", "a", "an"
    )

    // Colloquial Arabic -> Standard Arabic normalization (Syrian/Levantine, Gulf, Egyptian)
    private val colloquialReplacements = mapOf(
        "شلون" to "كيف",
        "ازاي" to "كيف",
        "كيفاش" to "كيف",
        "شو" to "ما",
        "ايش" to "ما",
        "شنو" to "ما",
        "ليش" to "لماذا",
        "عشان" to "لماذا",
        "مشان" to "من اجل",
        "بدي" to "اريد",
        "ابغى" to "اريد",
        "عايز" to "اريد",
        "احفظ" to "حفظ القران",
        "انسى" to "نسيان الحفظ",
        "بنسى" to "نسيان الحفظ",
        "بصلي" to "الصلاه",
        "اصلي" to "الصلاه",
        "بصوم" to "الصيام",
        "اصوم" to "الصيام",
        "تجويد" to "احكام التجويد",
        "قراءه" to "تلاوه القران"
    )

    fun rebuildSearchIndex(
        allKnowledge: List<LocalAiKnowledgeEntity>,
        books: List<LibraryBookEntity>
    ): AiIndexStats {
        val vocab = HashSet<String>(1024)
        for (entry in allKnowledge) {
            val combined = "${entry.titleAr} ${entry.titleEn} ${entry.questionAr} ${entry.questionEn} ${entry.answerAr} ${entry.keywords} ${entry.category}"
            vocab.addAll(extractSearchTokens(combined))
        }
        val approvedBooks = books.filter { it.approvalStatus == "APPROVED" }
        for (book in approvedBooks) {
            val combined = "${book.titleAr} ${book.summaryAr} ${book.contentText.take(3000)}"
            vocab.addAll(extractSearchTokens(combined))
        }
        return AiIndexStats(
            totalEntries = allKnowledge.size,
            approvedEntries = allKnowledge.count { it.approvalStatus == "APPROVED" },
            pendingEntries = allKnowledge.count { it.approvalStatus == "PENDING" },
            indexedLibraryBooks = approvedBooks.size,
            uniqueTokenCount = vocab.size,
            lastReindexedAtMillis = System.currentTimeMillis()
        )
    }

    fun answerQuestion(
        query: String,
        approvedKnowledge: List<LocalAiKnowledgeEntity>,
        approvedBooks: List<LibraryBookEntity>,
        conversationHistory: List<NoorAiAnswer> = emptyList(),
        minConfidenceThreshold: Float = 0.28f
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

        // Expand short follow-up questions with context from the previous turn if helpful
        val expandedQuery = expandColloquialAndContext(trimmed, conversationHistory)
        val queryTokens = extractSearchTokens(expandedQuery)
        val queryTrigrams = buildTrigrams(normalizeMixed(expandedQuery))

        // 1. Score verified Q&A + imported knowledge chunks (including explicit keywords/tags boost!)
        val scoredKnowledge = approvedKnowledge.map { entry ->
            val docText = "${entry.titleAr} ${entry.titleEn} ${entry.questionAr} ${entry.questionEn} ${entry.answerAr} ${entry.answerEn} ${entry.keywords} ${entry.category} ${entry.sourceRef}"
            val titleAndKeywords = "${entry.titleAr} ${entry.questionAr} ${entry.questionEn} ${entry.keywords}"
            val score = scoreMatch(queryTokens, queryTrigrams, docText, titleBoostText = titleAndKeywords)
            entry to score
        }.filter { it.second >= 0.15f }.sortedByDescending { it.second }

        // 2. Score Islamic library books for supplemental authentic excerpts
        val scoredBooks = approvedBooks.filter { it.approvalStatus == "APPROVED" }.map { book ->
            val docText = "${book.titleAr} ${book.titleEn} ${book.summaryAr} ${book.contentText.take(3000)}"
            val score = scoreMatch(queryTokens, queryTrigrams, docText, titleBoostText = "${book.titleAr} ${book.summaryAr}")
            book to score
        }.filter { it.second >= 0.17f }.sortedByDescending { it.second }

        val topKb = scoredKnowledge.firstOrNull()
        val topBook = scoredBooks.firstOrNull()

        if (topKb != null && topKb.second >= minConfidenceThreshold) {
            val item = topKb.first
            val sources = mutableListOf<AiCitedSource>()
            sources.add(
                AiCitedSource(
                    title = item.titleAr,
                    reference = item.sourceRef.ifBlank { "قاعدة المعرفة المحلية" },
                    kind = "KNOWLEDGE_BASE"
                )
            )
            scoredKnowledge.drop(1).take(2).filter { it.second >= (minConfidenceThreshold - 0.04f).coerceAtLeast(0.20f) }
                .forEach { (extra, _) ->
                    sources.add(AiCitedSource(title = extra.titleAr, reference = extra.sourceRef, kind = "KNOWLEDGE_BASE"))
                }
            if (topBook != null && topBook.second >= 0.24f) {
                sources.add(
                    AiCitedSource(
                        title = topBook.first.titleAr,
                        reference = topBook.first.sourceReference.ifBlank { topBook.first.authorAr },
                        kind = "LIBRARY"
                    )
                )
            }
            val level = if (item.verificationStatus == "VERIFIED" && topKb.second >= 0.40f) {
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
        if (topBook != null && topBook.second >= (minConfidenceThreshold - 0.05f).coerceAtLeast(0.22f)) {
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
            answerAr = "لم يتم العثور على إجابة موثّقة مطابقة لهذا السؤال في قاعدة المعرفة المحلية الحالية.\n\nالتزاماً بالأمانة العلمية، لا يقوم «نور» بتأليف إجابات شرعية أو نسب آيات وأحاديث دون مصدر موثّق. يمكنك:\n• صياغة السؤال بكلمات أبسط (مثل: الصلاة، الصيام، الزكاة، التجويد، حفظ القرآن، السيرة).\n• الرجوع إلى أهل العلم والفتوى المعتمدين.\n• أو إضافة مراجع وملفات موثّقة جديدة (TXT / JSON / DOCX / PDF) إلى الفهرس المحلي عبر لوحة الإدارة.",
            answerEn = "No verified matching entry was found in the local offline knowledge base. To uphold Islamic scholarly accuracy, NOOR never invents verses, Hadith, or rulings. Try simpler keywords (Salah, Fasting, Zakah, Tajweed, Hifz, Seerah), consult a qualified scholar, or import verified references (TXT/JSON/DOCX/PDF) via Local Admin.",
            verificationLevel = AiVerificationLevel.UNCERTAIN,
            sources = emptyList(),
            scholarlyNote = "الوقوف عند حد العلم الموثّق أمانة؛ ومن قال لا أعلم فقد أفتى.",
            disclaimerAr = MUFTI_DISCLAIMER_AR,
            disclaimerEn = MUFTI_DISCLAIMER_EN,
            confidencePercent = 0
        )
    }

    /**
     * Converts an ExtractedDocumentResult (TXT, JSON, DOCX, or PDF) into deduplicated searchable
     * LocalAiKnowledgeEntity entries ready for local RAG retrieval.
     */
    fun parseExtractedDocumentToKnowledge(
        extracted: ExtractedDocumentResult,
        existingHashes: Set<String>,
        autoApprove: Boolean
    ): ImportedKnowledgeBatch {
        if (extracted.format == DocumentFileFormat.JSON) {
            val base = parseImportedKnowledgeText(
                rawContent = extracted.extractedText,
                fileName = extracted.fileName,
                defaultCategory = extracted.detectedCategory,
                existingHashes = existingHashes
            )
            val items = if (autoApprove) base.items.map { it.copy(approvalStatus = "APPROVED") } else base.items
            return base.copy(
                items = items,
                detectedLanguage = extracted.detectedLanguage,
                detectedCategory = extracted.detectedCategory,
                warningAr = extracted.warningAr,
                warningEn = extracted.warningEn
            )
        }

        // If TXT looks like explicit Q/A blocks ("س:"/"ج:" or "Q:"/"A:"), preserve Q&A structure first
        if (extracted.format == DocumentFileFormat.TXT &&
            (extracted.extractedText.contains("س:") || extracted.extractedText.contains("Q:") || extracted.extractedText.contains("السؤال:"))
        ) {
            val qa = parseImportedKnowledgeText(
                rawContent = extracted.extractedText,
                fileName = extracted.fileName,
                defaultCategory = extracted.detectedCategory,
                existingHashes = existingHashes
            )
            val items = if (autoApprove) qa.items.map { it.copy(approvalStatus = "APPROVED") } else qa.items
            return qa.copy(
                items = items,
                detectedLanguage = extracted.detectedLanguage,
                detectedCategory = extracted.detectedCategory,
                warningAr = extracted.warningAr,
                warningEn = extracted.warningEn
            )
        }

        val parsed = mutableListOf<LocalAiKnowledgeEntity>()
        val seenHashes = existingHashes.toMutableSet()
        var duplicates = 0
        val approval = if (autoApprove) "APPROVED" else "PENDING"

        for (chunk in extracted.chunks) {
            val hash = LocalAdminSecurityManager.sha256Hex("${chunk.title}|${chunk.content}")
            if (!seenHashes.add(hash)) {
                duplicates++
                continue
            }
            parsed.add(
                LocalAiKnowledgeEntity(
                    id = "ai_doc_${UUID.randomUUID().toString().take(10)}",
                    category = extracted.detectedCategory,
                    titleAr = chunk.title,
                    titleEn = chunk.title,
                    questionAr = chunk.title,
                    questionEn = if (extracted.detectedLanguage != "AR") chunk.title else "",
                    answerAr = chunk.content,
                    answerEn = if (extracted.detectedLanguage != "AR") chunk.content else "",
                    sourceRef = "مستند مستورد (${extracted.format.label}): ${extracted.fileName} #${chunk.index + 1}",
                    verificationStatus = if (autoApprove) "VERIFIED" else "REVIEW",
                    scholarlyNotes = "مستخرج تلقائياً من مستند محلي للفهرسة المحلية (Local RAG).",
                    keywords = chunk.keywords.joinToString(", "),
                    contentHash = hash,
                    approvalStatus = approval,
                    isUserAdded = true
                )
            )
        }

        return ImportedKnowledgeBatch(
            items = parsed,
            skippedDuplicates = duplicates,
            parseErrors = if (parsed.isEmpty() && duplicates == 0) 1 else 0,
            detectedLanguage = extracted.detectedLanguage,
            detectedCategory = extracted.detectedCategory,
            warningAr = extracted.warningAr,
            warningEn = extracted.warningEn
        )
    }

    /**
     * Parses local TXT or JSON files imported by Admin via Storage Access Framework.
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
        val effectiveDefaultCategory = if (defaultCategory.equals("AUTO", true) || defaultCategory.isBlank()) {
            DocumentContentExtractor.detectCategory(trimmed, fileName)
        } else {
            defaultCategory.uppercase()
        }

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
                    val kw = obj.optString("keywords", "").trim().ifBlank {
                        DocumentContentExtractor.extractTopKeywords("$qAr $aAr", 6).joinToString(", ")
                    }
                    parsed.add(
                        LocalAiKnowledgeEntity(
                            id = "ai_imp_${UUID.randomUUID().toString().take(10)}",
                            category = obj.optString("category", effectiveDefaultCategory).uppercase(),
                            titleAr = obj.optString("titleAr", obj.optString("title", qAr.take(60))).trim(),
                            titleEn = obj.optString("titleEn", "").trim(),
                            questionAr = qAr,
                            questionEn = obj.optString("questionEn", "").trim(),
                            answerAr = aAr,
                            answerEn = obj.optString("answerEn", "").trim(),
                            sourceRef = obj.optString("sourceRef", obj.optString("source", fileName)).trim(),
                            verificationStatus = obj.optString("verificationStatus", "REVIEW").uppercase(),
                            scholarlyNotes = obj.optString("scholarlyNotes", "").trim(),
                            keywords = kw,
                            contentHash = hash,
                            approvalStatus = "PENDING",
                            isUserAdded = true
                        )
                    )
                }
                return ImportedKnowledgeBatch(parsed, duplicates, errors, detectedCategory = effectiveDefaultCategory)
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
            val kw = DocumentContentExtractor.extractTopKeywords("$question $answer", 6).joinToString(", ")
            parsed.add(
                LocalAiKnowledgeEntity(
                    id = "ai_txt_${UUID.randomUUID().toString().take(10)}",
                    category = effectiveDefaultCategory,
                    titleAr = title,
                    titleEn = title,
                    questionAr = question,
                    answerAr = answer,
                    sourceRef = "ملف مستورد: $fileName",
                    verificationStatus = "REVIEW",
                    keywords = kw,
                    contentHash = hash,
                    approvalStatus = "PENDING",
                    isUserAdded = true
                )
            )
        }

        return ImportedKnowledgeBatch(parsed, duplicates, errors, detectedCategory = effectiveDefaultCategory)
    }

    fun exportKnowledgeBaseJson(entries: List<LocalAiKnowledgeEntity>): String {
        val arr = JSONArray()
        for (e in entries) {
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("category", e.category)
                    put("titleAr", e.titleAr)
                    put("titleEn", e.titleEn)
                    put("questionAr", e.questionAr)
                    put("questionEn", e.questionEn)
                    put("answerAr", e.answerAr)
                    put("answerEn", e.answerEn)
                    put("sourceRef", e.sourceRef)
                    put("verificationStatus", e.verificationStatus)
                    put("scholarlyNotes", e.scholarlyNotes)
                    put("keywords", e.keywords)
                    put("approvalStatus", e.approvalStatus)
                }
            )
        }
        return JSONObject().apply {
            put("schema", "noor_local_ai_kb_v2")
            put("exportedAtMillis", System.currentTimeMillis())
            put("count", entries.size)
            put("items", arr)
        }.toString(2)
    }

    private fun expandColloquialAndContext(
        query: String,
        history: List<NoorAiAnswer>
    ): String {
        val normalizedWords = normalizeMixed(query).split(' ').map { w ->
            colloquialReplacements[w] ?: w
        }
        val base = normalizedWords.joinToString(" ")
        val tokens = base.split(' ').filter { it.length >= 2 && it !in arabicStopWords }
        if (tokens.size <= 2 && history.isNotEmpty()) {
            val prev = history.first()
            return "$base ${prev.query}"
        }
        return base
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
                docNorm.contains(qt) -> tokenHits += 0.80f
                qt.length >= 4 && docTokens.any { it.startsWith(qt.take(4)) || qt.startsWith(it.take(4)) } -> tokenHits += 0.52f
            }
        }
        val tokenScore = (tokenHits / queryTokens.size.coerceAtLeast(1)).coerceAtMost(1f)

        var titleHits = 0f
        for (qt in queryTokens) {
            if (titleNorm.contains(qt)) titleHits += 1f
        }
        val titleScore = (titleHits / queryTokens.size.coerceAtLeast(1)).coerceAtMost(1f)

        val docTrigrams = buildTrigrams(docNorm.take(700))
        val trigramScore = if (queryTrigrams.isEmpty() || docTrigrams.isEmpty()) {
            0f
        } else {
            val overlap = queryTrigrams.count { it in docTrigrams }
            overlap.toFloat() / min(queryTrigrams.size, docTrigrams.size).coerceAtLeast(1)
        }

        return (0.48f * tokenScore) + (0.34f * titleScore) + (0.18f * trigramScore)
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
