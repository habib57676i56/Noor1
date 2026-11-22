package com.example

import com.example.data.cloud.AdminRole
import com.example.data.cloud.AdminSecurityRateLimiter
import com.example.data.cloud.CmsContentCategory
import com.example.data.cloud.CmsWorkflowStatus
import com.example.data.cloud.NoorPlatformConstants
import com.example.data.local.catalog.AdhkarCatalog
import com.example.data.local.catalog.MorningEveningAdhkarCatalog
import com.example.domain.model.AdhkarCategory
import com.example.domain.model.DhikrItem
import com.example.domain.verification.AdhkarVerificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive NOOR 3.0 Automated Verification Tests covering:
 * - Authentic Morning (24+) & Evening (23+) Adhkar completeness, diacritics, and zero verification errors
 * - Negative Adhkar verification engine detection (missing text, missing source, invalid count, duplicates, category mismatch)
 * - Admin RBAC permission matrix & 16 CMS content categories
 * - Admin security rate limiter lockout & reset semantics
 * - Verified Founder (Osama) & Official Support Contact invariants (no invented Telegram username)
 */
class Noor3UpgradeUnitTest {

    @Test
    fun morningAndEveningAdhkar_areComprehensiveAndPassVerificationCleanly() {
        val morning = MorningEveningAdhkarCatalog.morningAdhkar
        val evening = MorningEveningAdhkarCatalog.eveningAdhkar

        assertTrue("Expected at least 20 authentic Morning Adhkar, found ${morning.size}", morning.size >= 20)
        assertTrue("Expected at least 20 authentic Evening Adhkar, found ${evening.size}", evening.size >= 20)

        // Ensure key established Adhkar are present in both collections (glyph/diacritic agnostic)
        val normMorning = morning.map { AdhkarVerificationEngine.normalizeArabic(it.textAr) }
        val normEvening = evening.map { AdhkarVerificationEngine.normalizeArabic(it.textAr) }
        assertTrue(normMorning.any { it.contains("الحي القيوم") }) // Ayat al-Kursi
        assertTrue(normEvening.any { it.contains("الحي القيوم") }) // Ayat al-Kursi
        assertTrue(normMorning.any { it.contains("انت ربي لا اله الا انت") }) // Sayyid al-Istighfar
        assertTrue(normEvening.any { it.contains("انت ربي لا اله الا انت") }) // Sayyid al-Istighfar
        assertTrue(normEvening.any { it.contains("امن الرسول") }) // Last 2 verses of Al-Baqarah

        val report = AdhkarVerificationEngine.verifyCollection(morning + evening)
        assertEquals("Expected 0 verification errors in Morning & Evening Adhkar: ${report.issues}", 0, report.errorCount)
        assertTrue("Expected clean Morning & Evening Adhkar verification report", report.isClean)
    }

    @Test
    fun fullAdhkarCatalog_preservesAllExistingCategoriesAndHasZeroErrors() {
        val report = AdhkarVerificationEngine.verifyCollection(AdhkarCatalog.allAdhkar)
        assertTrue(report.morningCount >= 20)
        assertTrue(report.eveningCount >= 20)
        assertEquals("Expected 0 errors across entire AdhkarCatalog: ${report.issues}", 0, report.errorCount)
    }

    @Test
    fun adhkarVerificationEngine_detectsDefectsAndCategoryMismatches() {
        val defective = listOf(
            DhikrItem(
                id = "bad_1",
                category = AdhkarCategory.MORNING,
                textAr = "أَمْسَيْنَا وَأَمْسَى المُلْكُ لِلَّهِ", // Evening wording placed in MORNING
                targetCount = 0, // Invalid count
                sourceAr = "", // Missing source
                sourceEn = "",
                gradingAr = "", // Missing grading
                gradingEn = "",
                meaningAr = "",
                meaningEn = ""
            ),
            DhikrItem(
                id = "dup_1",
                category = AdhkarCategory.EVENING,
                textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                targetCount = 100,
                sourceAr = "صحيح مسلم",
                sourceEn = "Sahih Muslim",
                gradingAr = "صحيح",
                gradingEn = "Sahih",
                meaningAr = "تسبيح",
                meaningEn = "Glory be to Allah"
            ),
            DhikrItem(
                id = "dup_2",
                category = AdhkarCategory.EVENING,
                textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", // Duplicate text in same category
                targetCount = 100,
                sourceAr = "صحيح مسلم",
                sourceEn = "Sahih Muslim",
                gradingAr = "صحيح",
                gradingEn = "Sahih",
                meaningAr = "تسبيح",
                meaningEn = "Glory be to Allah"
            )
        )
        val report = AdhkarVerificationEngine.verifyCollection(defective)
        assertFalse(report.isClean)
        val codes = report.issues.map { it.code }.toSet()
        assertTrue("Should flag INVALID_REPETITION", "INVALID_REPETITION" in codes)
        assertTrue("Should flag MISSING_SOURCE", "MISSING_SOURCE" in codes)
        assertTrue("Should flag MISSING_GRADING", "MISSING_GRADING" in codes)
        assertTrue("Should flag CATEGORY_MISMATCH", "CATEGORY_MISMATCH" in codes)
        assertTrue("Should flag DUPLICATE_ENTRY", "DUPLICATE_ENTRY" in codes)
    }

    @Test
    fun adminSecurityRateLimiter_locksAfterFiveFailuresAndResetsOnSuccess() {
        val limiter = AdminSecurityRateLimiter()
        val t0 = 1_000_000L
        repeat(4) { i ->
            val locked = limiter.recordFailure(t0 + i * 1000L)
            assertFalse("Should not lock before 5th failure", locked)
        }
        val lockedOnFifth = limiter.recordFailure(t0 + 5000L)
        assertTrue("Must lock on 5th consecutive failure", lockedOnFifth)
        assertTrue(limiter.isLocked(t0 + 10_000L))
        assertTrue(limiter.remainingLockSeconds(t0 + 10_000L) > 0)

        // Lock expires after 120s
        assertFalse(limiter.isLocked(t0 + 130_000L))

        limiter.recordSuccess()
        assertEquals(0, limiter.failedAttemptCount(t0 + 130_000L))
    }

    @Test
    fun rbacRolesAndCmsCategories_matchEnterpriseSpecification() {
        assertEquals(16, CmsContentCategory.entries.size)
        assertEquals(4, CmsWorkflowStatus.entries.size)

        assertFalse(AdminRole.USER.isPrivileged)
        assertFalse(AdminRole.USER.canManageContent)
        assertFalse(AdminRole.USER.canAssignRoles)

        assertTrue(AdminRole.CONTENT_MODERATOR.canManageContent)
        assertFalse(AdminRole.CONTENT_MODERATOR.canManageUsers)
        assertFalse(AdminRole.CONTENT_MODERATOR.canAssignRoles)

        assertTrue(AdminRole.SUPPORT_AGENT.canManageSupport)
        assertFalse(AdminRole.SUPPORT_AGENT.canManageContent)

        assertTrue(AdminRole.ADMIN.canManageUsers)
        assertFalse(AdminRole.ADMIN.canAssignRoles)

        assertTrue(AdminRole.SUPER_ADMIN.canAssignRoles)
        assertTrue(AdminRole.SUPER_ADMIN.canManageUsers)
        assertTrue(AdminRole.SUPER_ADMIN.canManageContent)
        assertTrue(AdminRole.SUPER_ADMIN.canManageSupport)
    }

    @Test
    fun founderProfileAndOfficialSupportContacts_areStrictlyVerifiedWithoutFabrication() {
        assertEquals("Osama", NoorPlatformConstants.FOUNDER_NAME)
        assertEquals("Founder & Creator of NOOR", NoorPlatformConstants.FOUNDER_TITLE_EN)
        assertEquals("NOOR — The Complete Islamic Companion", NoorPlatformConstants.FOUNDER_PROJECT_EN)
        assertTrue(NoorPlatformConstants.FOUNDER_BIO_EN.contains("authenticity, simplicity, privacy, and continuous improvement"))

        assertEquals("habiybullohy@gmail.com", NoorPlatformConstants.SUPPORT_EMAIL)
        assertEquals("+963 984 215 420", NoorPlatformConstants.SUPPORT_WHATSAPP)
        assertEquals("+963 984 215 420", NoorPlatformConstants.SUPPORT_TELEGRAM)
        assertEquals("https://wa.me/963984215420", NoorPlatformConstants.SUPPORT_WHATSAPP_URL)
        assertEquals("https://t.me/+963984215420", NoorPlatformConstants.SUPPORT_TELEGRAM_URL)
        // Verify Telegram URL uses official phone number, NOT an invented @username
        assertFalse(NoorPlatformConstants.SUPPORT_TELEGRAM.contains("@"))
    }
}
