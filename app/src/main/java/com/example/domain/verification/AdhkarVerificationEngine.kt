package com.example.domain.verification

import com.example.domain.model.AdhkarCategory
import com.example.domain.model.DhikrItem

enum class VerificationSeverity {
    ERROR,
    WARNING
}

data class AdhkarVerificationIssue(
    val dhikrId: String,
    val categoryId: String,
    val code: String,
    val severity: VerificationSeverity,
    val messageAr: String,
    val messageEn: String
)

data class AdhkarVerificationReport(
    val totalChecked: Int,
    val passedCount: Int,
    val errorCount: Int,
    val warningCount: Int,
    val issues: List<AdhkarVerificationIssue>,
    val morningCount: Int = 0,
    val eveningCount: Int = 0
) {
    val isFullyVerified: Boolean get() = errorCount == 0
    val isClean: Boolean get() = errorCount == 0
}

/**
 * NOOR 3.0 Content Verification System.
 *
 * Audits every Dhikr or CMS Adhkar record for:
 * - Missing or blank Arabic text
 * - Missing Arabic diacritics (Tashkeel)
 * - Missing Arabic or English scholarly source reference
 * - Missing Hadith grading
 * - Invalid repetition count (< 1 or > 1000)
 * - Duplicate IDs or duplicate normalized Arabic texts within the same category
 * - Morning/Evening category mismatch (e.g., "أمسينا" in Morning or "أصبحنا" leading Evening)
 * - Missing explanation / English translation
 */
object AdhkarVerificationEngine {

    private val diacriticsRegex = Regex("[\u064B-\u0652\u0670]")
    private val whitespaceRegex = Regex("\\s+")

    fun normalizeArabic(text: String): String =
        text.replace(diacriticsRegex, "")
            .replace("إ", "ا")
            .replace("أ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace(whitespaceRegex, " ")
            .trim()

    fun hasDiacritics(text: String): Boolean = diacriticsRegex.containsMatchIn(text)

    fun verifyCollection(items: List<DhikrItem>): AdhkarVerificationReport {
        val issues = mutableListOf<AdhkarVerificationIssue>()
        val seenIds = mutableSetOf<String>()
        val seenTextsByCategory = mutableMapOf<AdhkarCategory, MutableSet<String>>()
        val failedIds = mutableSetOf<String>()

        fun addIssue(
            item: DhikrItem,
            code: String,
            severity: VerificationSeverity,
            ar: String,
            en: String
        ) {
            issues.add(
                AdhkarVerificationIssue(
                    dhikrId = item.id,
                    categoryId = item.category.id,
                    code = code,
                    severity = severity,
                    messageAr = ar,
                    messageEn = en
                )
            )
            if (severity == VerificationSeverity.ERROR) {
                failedIds.add(item.id)
            }
        }

        for (item in items) {
            // 1. Duplicate ID check
            if (!seenIds.add(item.id)) {
                addIssue(
                    item, "DUPLICATE_ID", VerificationSeverity.ERROR,
                    "معرّف الذكر مكرر: ${item.id}",
                    "Duplicate Dhikr ID detected: ${item.id}"
                )
            }

            // 2. Missing Arabic text
            if (item.textAr.trim().length < 6) {
                addIssue(
                    item, "MISSING_ARABIC_TEXT", VerificationSeverity.ERROR,
                    "النص العربي مفقود أو غير مكتمل",
                    "Arabic text is missing or incomplete"
                )
            } else if (!hasDiacritics(item.textAr)) {
                addIssue(
                    item, "MISSING_DIACRITICS", VerificationSeverity.WARNING,
                    "النص العربي يفتقر إلى التشكيل الكامل",
                    "Arabic text is missing diacritics (Tashkeel)"
                )
            }

            // 3. Missing scholarly source references
            if (item.sourceAr.isBlank() || item.sourceEn.isBlank()) {
                addIssue(
                    item, "MISSING_SOURCE", VerificationSeverity.ERROR,
                    "المصدر الحديثي أو القرآني غير مكتمل",
                    "Missing Arabic or English source reference"
                )
            }

            // 4. Missing Hadith grading
            if (item.gradingAr.isBlank() || item.gradingEn.isBlank()) {
                addIssue(
                    item, "MISSING_GRADING", VerificationSeverity.ERROR,
                    "درجة الحديث غير محددة",
                    "Hadith authentication grading is missing"
                )
            }

            // 5. Invalid repetition count
            if (item.targetCount < 1 || item.targetCount > 1000) {
                addIssue(
                    item, "INVALID_REPETITION", VerificationSeverity.ERROR,
                    "عدد التكرار غير صالح: ${item.targetCount}",
                    "Invalid repetition count: ${item.targetCount}"
                )
            }

            // 6. Missing translation / meaning
            if (item.meaningAr.isBlank() || item.meaningEn.isBlank()) {
                addIssue(
                    item, "MISSING_TRANSLATION", VerificationSeverity.ERROR,
                    "الشرح أو الترجمة الإنجليزية مفقودة",
                    "Missing Arabic explanation or English translation"
                )
            }

            // 7. Duplicate normalized text within the same category (allowing distinct verified repetition narrations like 10x vs 100x)
            val norm = "${normalizeArabic(item.textAr)}::${item.targetCount}"
            val catSet = seenTextsByCategory.getOrPut(item.category) { mutableSetOf() }
            if (!catSet.add(norm)) {
                addIssue(
                    item, "DUPLICATE_ENTRY", VerificationSeverity.ERROR,
                    "نص مكرر داخل نفس القسم (${item.category.nameAr})",
                    "Duplicate Dhikr text within the same category (${item.category.nameEn})"
                )
            }

            // 8. Morning vs Evening textual classification consistency
            val cleanPrefix = normalizeArabic(item.textAr).take(24)
            if (item.category == AdhkarCategory.MORNING &&
                (cleanPrefix.startsWith("امسينا") || cleanPrefix.contains("اللهم بك امسينا") || cleanPrefix.contains("اني امسيت"))
            ) {
                addIssue(
                    item, "CATEGORY_MISMATCH", VerificationSeverity.ERROR,
                    "ذكر مسائي مصنّف خطأً ضمن أذكار الصباح",
                    "Evening phrasing misclassified under Morning Adhkar"
                )
            }
            if (item.category == AdhkarCategory.EVENING &&
                (cleanPrefix.startsWith("اصبحنا") || cleanPrefix.contains("اللهم بك اصبحنا") || cleanPrefix.contains("اني اصبحت"))
            ) {
                addIssue(
                    item, "CATEGORY_MISMATCH", VerificationSeverity.ERROR,
                    "ذكر صباحي مصنّف خطأً ضمن أذكار المساء",
                    "Morning phrasing misclassified under Evening Adhkar"
                )
            }
        }

        val errors = issues.count { it.severity == VerificationSeverity.ERROR }
        val warnings = issues.count { it.severity == VerificationSeverity.WARNING }
        return AdhkarVerificationReport(
            totalChecked = items.size,
            passedCount = (items.size - failedIds.size).coerceAtLeast(0),
            errorCount = errors,
            warningCount = warnings,
            issues = issues,
            morningCount = items.count { it.category == AdhkarCategory.MORNING },
            eveningCount = items.count { it.category == AdhkarCategory.EVENING }
        )
    }
}
