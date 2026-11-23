package com.example.domain.ai

import com.example.domain.quran.QuranArabicPhoneticNormalizer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

enum class DocumentFileFormat(val label: String) {
    TXT("TXT"),
    JSON("JSON"),
    DOCX("DOCX"),
    PDF("PDF"),
    UNKNOWN("UNKNOWN")
}

data class DocumentChunk(
    val index: Int,
    val title: String,
    val content: String,
    val keywords: List<String>
)

data class ExtractedDocumentResult(
    val fileName: String,
    val format: DocumentFileFormat,
    val extractedText: String,
    val chunks: List<DocumentChunk>,
    val detectedLanguage: String, // AR, EN, MIXED
    val detectedCategory: String, // QURAN, HADITH, FIQH, AQEEDAH, SEERAH, TAJWEED, WORSHIP, HIFZ, GENERAL
    val extractedKeywords: List<String>,
    val warningAr: String? = null,
    val warningEn: String? = null
)

/**
 * 100% Offline, pure-Kotlin document extractor & chunker for NOOR Trainable Local AI (RAG).
 *
 * Supported formats:
 * - TXT / MD: Direct UTF-8 text decoding.
 * - JSON: Structured Q&A or knowledge arrays (also preserves raw text for fallback).
 * - DOCX: Real ZIP container unpacking of `word/document.xml` via ZipInputStream + XmlPullParser.
 * - PDF: Pure-Kotlin stream parser supporting uncompressed & FlateDecode compressed content streams
 *   (`BT...ET`, `Tj`, `TJ`, UTF-16BE hex strings) with an honest warning when a PDF is a scanned
 *   image-only file without embedded text.
 */
object DocumentContentExtractor {

    private val stopWords = setOf(
        "من", "في", "على", "عن", "إلى", "الى", "أن", "ان", "إن", "هذا", "هذه", "التي", "الذي",
        "التي", "ما", "هل", "كيف", "متى", "أو", "او", "مع", "كل", "بعد", "قبل", "عند", "بين",
        "كان", "كانت", "يكون", "تكون", "ذلك", "تلك", "كما", "أي", "اي", "له", "لها", "بها",
        "the", "and", "for", "with", "that", "this", "from", "are", "was", "were", "have", "has"
    )

    fun detectFormat(fileName: String, bytes: ByteArray): DocumentFileFormat {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".json") -> DocumentFileFormat.JSON
            lower.endsWith(".docx") -> DocumentFileFormat.DOCX
            lower.endsWith(".pdf") -> DocumentFileFormat.PDF
            lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") -> DocumentFileFormat.TXT
            bytes.size >= 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
                bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte() -> DocumentFileFormat.PDF
            bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> DocumentFileFormat.DOCX
            else -> DocumentFileFormat.TXT
        }
    }

    fun extract(fileName: String, bytes: ByteArray, defaultCategory: String = "AUTO"): ExtractedDocumentResult {
        val format = detectFormat(fileName, bytes)
        var warnAr: String? = null
        var warnEn: String? = null

        val text = when (format) {
            DocumentFileFormat.TXT, DocumentFileFormat.JSON, DocumentFileFormat.UNKNOWN -> {
                String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "").trim()
            }
            DocumentFileFormat.DOCX -> {
                val docText = extractDocxText(bytes)
                if (docText.isBlank()) {
                    warnAr = "لم يتم العثور على نص قابل للقراءة داخل ملف DOCX."
                    warnEn = "No readable text found inside DOCX document."
                }
                docText
            }
            DocumentFileFormat.PDF -> {
                val pdfText = extractPdfText(bytes)
                if (pdfText.length < 25) {
                    warnAr = "ملف PDF هذا يبدو مصوّراً (Scanned) أو مشفّراً بخطوط مخصّصة دون نص مضمّن. يُفضّل استيراد نسخة نصية (TXT/DOCX/JSON) أو PDF نصي للحصول على فهرسة دقيقة."
                    warnEn = "This PDF appears to be a scanned image or uses custom glyph encoding without embedded selectable text. Import a text-based PDF, DOCX, TXT, or JSON file for accurate indexing."
                }
                pdfText
            }
        }

        val cleaned = normalizeWhitespace(text).take(400_000)
        val language = detectLanguage(cleaned)
        val category = if (defaultCategory.equals("AUTO", ignoreCase = true) || defaultCategory.isBlank()) {
            detectCategory(cleaned, fileName)
        } else {
            defaultCategory.uppercase()
        }
        val keywords = extractTopKeywords(cleaned, maxCount = 12)
        val chunks = if (format == DocumentFileFormat.JSON) {
            emptyList() // JSON items are parsed directly by NoorLocalAiEngine
        } else {
            chunkDocument(fileName, cleaned)
        }

        return ExtractedDocumentResult(
            fileName = fileName,
            format = format,
            extractedText = cleaned,
            chunks = chunks,
            detectedLanguage = language,
            detectedCategory = category,
            extractedKeywords = keywords,
            warningAr = warnAr,
            warningEn = warnEn
        )
    }

    fun extractDocxText(bytes: ByteArray): String {
        val sb = StringBuilder()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.equals("word/document.xml", ignoreCase = true)) {
                        val xmlBytes = zis.readBytes()
                        val factory = XmlPullParserFactory.newInstance()
                        factory.isNamespaceAware = false
                        val parser = factory.newPullParser()
                        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
                        var event = parser.eventType
                        while (event != XmlPullParser.END_DOCUMENT) {
                            when (event) {
                                XmlPullParser.START_TAG -> {
                                    val tag = parser.name.substringAfter(':')
                                    if (tag == "t") {
                                        val txt = runCatching { parser.nextText() }.getOrDefault("")
                                        if (txt.isNotEmpty()) sb.append(txt)
                                    } else if (tag == "br" || tag == "cr") {
                                        sb.append('\n')
                                    } else if (tag == "tab") {
                                        sb.append(' ')
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    val tag = parser.name.substringAfter(':')
                                    if (tag == "p") {
                                        sb.append('\n')
                                    }
                                }
                            }
                            event = parser.next()
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
            return ""
        }
        return sb.toString().trim()
    }

    fun extractPdfText(bytes: ByteArray): String {
        val rawLatin = String(bytes, StandardCharsets.ISO_8859_1)
        val collected = StringBuilder()

        // 1. Scan all stream ... endstream blocks (both uncompressed and FlateDecode)
        var cursor = 0
        while (cursor < rawLatin.length) {
            val streamIdx = rawLatin.indexOf("stream", cursor)
            if (streamIdx < 0) break
            var dataStart = streamIdx + 6
            if (dataStart < rawLatin.length && rawLatin[dataStart] == '\r') dataStart++
            if (dataStart < rawLatin.length && rawLatin[dataStart] == '\n') dataStart++
            val endIdx = rawLatin.indexOf("endstream", dataStart)
            if (endIdx < 0) break

            val headerWindow = rawLatin.substring((streamIdx - 220).coerceAtLeast(0), streamIdx)
            val streamSlice = bytes.copyOfRange(dataStart.coerceAtMost(bytes.size), endIdx.coerceAtMost(bytes.size))

            val decodedStreamText = if (headerWindow.contains("/FlateDecode")) {
                inflatePdfStream(streamSlice)
            } else {
                String(streamSlice, StandardCharsets.ISO_8859_1)
            }

            if (decodedStreamText.isNotBlank()) {
                val extractedFromBlock = parsePdfTextOperators(decodedStreamText)
                if (extractedFromBlock.isNotBlank()) {
                    collected.append(extractedFromBlock).append('\n')
                }
            }
            cursor = endIdx + 9
        }

        // 2. Fallback: parse top-level BT...ET blocks outside streams if any
        if (collected.isBlank()) {
            val direct = parsePdfTextOperators(rawLatin)
            if (direct.isNotBlank()) collected.append(direct)
        }

        return collected.toString().trim()
    }

    private fun inflatePdfStream(compressed: ByteArray): String {
        if (compressed.isEmpty()) return ""
        return try {
            val inflater = Inflater()
            inflater.setInput(compressed)
            val out = ByteArrayOutputStream(compressed.size * 2)
            val buf = ByteArray(4096)
            var guard = 0
            while (!inflater.finished() && guard < 200) {
                val count = inflater.inflate(buf)
                if (count <= 0) break
                out.write(buf, 0, count)
                guard++
            }
            inflater.end()
            String(out.toByteArray(), StandardCharsets.ISO_8859_1)
        } catch (_: Exception) {
            ""
        }
    }

    private fun parsePdfTextOperators(stream: String): String {
        val out = StringBuilder()
        var i = 0
        val len = stream.length
        while (i < len) {
            val ch = stream[i]
            if (ch == '(') {
                val str = StringBuilder()
                i++
                var depth = 1
                while (i < len && depth > 0) {
                    val c = stream[i]
                    if (c == '\\' && i + 1 < len) {
                        val next = stream[i + 1]
                        when (next) {
                            'n' -> str.append('\n')
                            'r' -> str.append('\r')
                            't' -> str.append('\t')
                            '(', ')', '\\' -> str.append(next)
                            else -> str.append(next)
                        }
                        i += 2
                    } else if (c == '(') {
                        depth++
                        str.append(c)
                        i++
                    } else if (c == ')') {
                        depth--
                        if (depth > 0) str.append(c)
                        i++
                    } else {
                        str.append(c)
                        i++
                    }
                }
                val candidate = decodePossibleUtf16OrUtf8(str.toString())
                if (candidate.isNotBlank()) {
                    out.append(candidate).append(' ')
                }
            } else if (ch == '<' && i + 1 < len && stream[i + 1] != '<') {
                val close = stream.indexOf('>', i + 1)
                if (close > i + 1 && close - i < 2048) {
                    val hex = stream.substring(i + 1, close).replace(Regex("\\s+"), "")
                    if (hex.length >= 4 && hex.length % 2 == 0 && hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                        val decoded = decodeHexPdfString(hex)
                        if (decoded.isNotBlank()) out.append(decoded).append(' ')
                    }
                    i = close + 1
                } else {
                    i++
                }
            } else {
                i++
            }
        }
        return out.toString()
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " ")
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    private fun decodePossibleUtf16OrUtf8(latinRaw: String): String {
        val rawBytes = latinRaw.toByteArray(StandardCharsets.ISO_8859_1)
        if (rawBytes.size >= 2 && (rawBytes[0].toInt() and 0xFF) == 0xFE && (rawBytes[1].toInt() and 0xFF) == 0xFF) {
            return String(rawBytes, 2, rawBytes.size - 2, StandardCharsets.UTF_16BE)
        }
        val utf8 = String(rawBytes, StandardCharsets.UTF_8)
        return if (utf8.any { it in '\u0600'..'\u06FF' || it.isLetterOrDigit() }) utf8 else ""
    }

    private fun decodeHexPdfString(hex: String): String {
        val bytes = ByteArray(hex.length / 2)
        for (idx in bytes.indices) {
            bytes[idx] = hex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
        return if (bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0xFE && (bytes[1].toInt() and 0xFF) == 0xFF) {
            String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        } else if (bytes.size % 2 == 0 && bytes.size >= 2 && (bytes[0].toInt() == 0x06 || bytes[0].toInt() == 0x00)) {
            String(bytes, StandardCharsets.UTF_16BE)
        } else {
            String(bytes, StandardCharsets.UTF_8)
        }.filter { it.isLetterOrDigit() || it.isWhitespace() || it in '\u0600'..'\u06FF' || it in ".,:;!?-()،؛؟" }
    }

    fun detectLanguage(text: String): String {
        var arabicCount = 0
        var latinCount = 0
        for (ch in text.take(4000)) {
            when {
                ch in '\u0600'..'\u06FF' -> arabicCount++
                ch in 'a'..'z' || ch in 'A'..'Z' -> latinCount++
            }
        }
        return when {
            arabicCount > 0 && latinCount > 0 && minOf(arabicCount, latinCount) * 5 >= maxOf(arabicCount, latinCount) -> "MIXED"
            arabicCount >= latinCount -> "AR"
            else -> "EN"
        }
    }

    fun detectCategory(text: String, fileName: String = ""): String {
        val sample = QuranArabicPhoneticNormalizer.normalize("$fileName ${text.take(3500)}") + " " +
            "$fileName ${text.take(3500)}".lowercase()
        val scores = mapOf(
            "TAJWEED" to countMatches(sample, listOf("تجويد", "ادغام", "اخفاء", "اقلاب", "قلقله", "مد", "غنه", "مخارج", "tajweed")),
            "HIFZ" to countMatches(sample, listOf("حفظ", "مراجعه", "تثبيت", "تسميع", "حلقه", "حلقات", "hifz", "memorization")),
            "QURAN" to countMatches(sample, listOf("قران", "سوره", "ايه", "تفسير", "مصحف", "quran", "tafsir", "surah")),
            "HADITH" to countMatches(sample, listOf("حديث", "بخاري", "مسلم", "صحيح", "اسناد", "رواه", "hadith", "sunnah")),
            "FIQH" to countMatches(sample, listOf("فقه", "صلاه", "زكاه", "صيام", "حج", "وضوء", "طهاره", "حكم", "fiqh", "salah", "zakah")),
            "AQEEDAH" to countMatches(sample, listOf("عقيده", "توحيد", "ايمان", "اسماء الله", "صفات", "aqeedah", "tawheed")),
            "SEERAH" to countMatches(sample, listOf("سيره", "غزوه", "هجره", "صحابه", "مكه", "مدينه", "seerah", "prophet")),
            "WORSHIP" to countMatches(sample, listOf("اذكار", "دعاء", "تسبيح", "قيام", "استغفار", "adhkar", "dua"))
        )
        val best = scores.maxByOrNull { it.value }
        return if (best != null && best.value > 0) best.key else "GENERAL"
    }

    fun extractTopKeywords(text: String, maxCount: Int = 10): List<String> {
        val arabicWords = QuranArabicPhoneticNormalizer.tokenize(text.take(12_000))
        val latinWords = text.take(12_000).lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }

        val freq = HashMap<String, Int>()
        for (w in arabicWords + latinWords) {
            if (w.length < 3 || w in stopWords) continue
            freq[w] = (freq[w] ?: 0) + 1
        }
        return freq.entries
            .sortedByDescending { it.value }
            .take(maxCount)
            .map { it.key }
    }

    /**
     * Splits large documents into overlapping searchable chunks (500–850 chars) on paragraph/sentence
     * boundaries so local RAG search retrieves precise answers instead of whole raw files.
     */
    fun chunkDocument(
        fileName: String,
        text: String,
        maxChunkChars: Int = 760,
        overlapChars: Int = 110
    ): List<DocumentChunk> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val baseTitle = fileName.substringBeforeLast('.').ifBlank { "مستند محلي" }
        val paragraphs = trimmed.split(Regex("\\r?\\n\\s*\\r?\\n")).map { it.trim() }.filter { it.isNotBlank() }

        val rawChunks = mutableListOf<String>()
        val buffer = StringBuilder()

        fun flushBuffer() {
            val s = buffer.toString().trim()
            if (s.isNotEmpty()) rawChunks.add(s)
            buffer.clear()
        }

        for (para in paragraphs) {
            if (para.length > maxChunkChars) {
                flushBuffer()
                var start = 0
                while (start < para.length) {
                    val end = (start + maxChunkChars).coerceAtMost(para.length)
                    val splitAt = if (end < para.length) {
                        val space = para.lastIndexOf(' ', end)
                        if (space > start + 200) space else end
                    } else end
                    rawChunks.add(para.substring(start, splitAt).trim())
                    if (splitAt >= para.length) break
                    start = (splitAt - overlapChars).coerceAtLeast(start + 1)
                }
            } else {
                if (buffer.isNotEmpty() && buffer.length + para.length + 2 > maxChunkChars) {
                    val tail = buffer.toString().takeLast(overlapChars).trim()
                    flushBuffer()
                    if (tail.length >= 30) {
                        buffer.append(tail).append("\n")
                    }
                }
                if (buffer.isNotEmpty()) buffer.append("\n\n")
                buffer.append(para)
            }
        }
        flushBuffer()

        return rawChunks.mapIndexed { idx, content ->
            val firstLine = content.lines().firstOrNull { it.trim().length >= 6 }?.trim()?.take(65)
                ?: "$baseTitle (${idx + 1})"
            DocumentChunk(
                index = idx,
                title = firstLine,
                content = content,
                keywords = extractTopKeywords(content, maxCount = 6)
            )
        }
    }

    private fun countMatches(text: String, keywords: List<String>): Int {
        var hits = 0
        for (kw in keywords) {
            if (text.contains(kw)) hits++
        }
        return hits
    }

    private fun normalizeWhitespace(input: String): String {
        return input
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
