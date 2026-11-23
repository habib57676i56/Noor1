package com.example

import com.example.data.local.catalog.EcosystemContentCatalog
import com.example.domain.ai.AiVerificationLevel
import com.example.domain.ai.NoorLocalAiEngine
import com.example.security.LocalAdminRole
import com.example.security.LocalAdminSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the core security, offline AI accuracy, content catalog integrity,
 * and TXT/JSON knowledge import logic of the NOOR Complete Islamic Ecosystem.
 */
class NoorEcosystemUnitTest {

    @Test
    fun pbkdf2SaltedHashing_verifiesCorrectAndRejectsWrongPasscode() {
        val digest1 = LocalAdminSecurityManager.hashPasscode("NoorAdmin2026!")
        val digest2 = LocalAdminSecurityManager.hashPasscode("NoorAdmin2026!")

        // Random salt ensures two hashes of the same passcode differ
        assertNotEquals(digest1.saltHex, digest2.saltHex)
        assertNotEquals(digest1.hashHex, digest2.hashHex)

        assertTrue(LocalAdminSecurityManager.verifyPasscode("NoorAdmin2026!", digest1.hashHex, digest1.saltHex))
        assertFalse(LocalAdminSecurityManager.verifyPasscode("WrongPasscode", digest1.hashHex, digest1.saltHex))
        assertFalse(LocalAdminSecurityManager.verifyPasscode("", digest1.hashHex, digest1.saltHex))
        assertFalse(LocalAdminSecurityManager.isStrongEnoughPasscode("12345"))
        assertTrue(LocalAdminSecurityManager.isStrongEnoughPasscode("123456"))
    }

    @Test
    fun rbacPermissionMatrix_enforcesLeastPrivilege() {
        assertTrue(LocalAdminRole.SUPER_ADMIN.canManageAccounts())
        assertTrue(LocalAdminRole.SUPER_ADMIN.canBackupAndRestore())
        assertTrue(LocalAdminRole.SUPER_ADMIN.canViewAuditLogs())

        assertFalse(LocalAdminRole.TEACHER.canManageAccounts())
        assertFalse(LocalAdminRole.TEACHER.canBackupAndRestore())
        assertTrue(LocalAdminRole.TEACHER.canManageHalaqat())
        assertTrue(LocalAdminRole.TEACHER.canManageQuizzes())

        assertFalse(LocalAdminRole.ADMIN.canManageAccounts())
        assertTrue(LocalAdminRole.ADMIN.canManageHalaqat())
        assertTrue(LocalAdminRole.ADMIN.canManageAiKnowledge())
        assertTrue(LocalAdminRole.ADMIN.canApproveContent())
        assertEquals(LocalAdminRole.ADMIN, LocalAdminRole.fromKey("EDITOR"))

        assertFalse(LocalAdminRole.STUDENT.canManageAccounts())
        assertFalse(LocalAdminRole.STUDENT.canManageHalaqat())
        assertFalse(LocalAdminRole.STUDENT.canManageAiKnowledge())
    }

    @Test
    fun localNoorAiEngine_returnsVerifiedCitedAnswerForKnownIslamicTopic() {
        val kb = EcosystemContentCatalog.seedAiKnowledge()
        val books = EcosystemContentCatalog.seedLibraryBooks()

        val result = NoorLocalAiEngine.answerQuestion(
            query = "ما هي أحكام النون الساكنة في التجويد؟",
            approvedKnowledge = kb,
            approvedBooks = books
        )

        assertEquals(AiVerificationLevel.VERIFIED, result.verificationLevel)
        assertTrue(result.sources.isNotEmpty())
        assertTrue(result.answerAr.contains("الإظهار"))
        assertTrue(result.disclaimerAr.contains("ليس مفتياً"))
    }

    @Test
    fun localNoorAiEngine_refusesToInventAnswerForUnknownQuery() {
        val kb = EcosystemContentCatalog.seedAiKnowledge()
        val books = EcosystemContentCatalog.seedLibraryBooks()

        val result = NoorLocalAiEngine.answerQuestion(
            query = "ما سعر تذكرة السفر إلى القمر في عام 2099؟",
            approvedKnowledge = kb,
            approvedBooks = books
        )

        assertEquals(AiVerificationLevel.UNCERTAIN, result.verificationLevel)
        assertTrue(result.sources.isEmpty())
        assertTrue(result.answerAr.contains("لم يتم العثور"))
        assertTrue(result.disclaimerAr.contains("العلماء"))
    }

    @Test
    fun localNoorAiEngine_parsesTxtImportAndDeduplicatesEntries() {
        val sampleTxt = """
            س: ما هو أفضل وقت لقراءة أذكار الصباح؟
            ج: من طلوع الفجر الصادق حتى طلوع الشمس، ويمتد وقتها عند جمع من العلماء إلى الزوال.

            س: ما هو أفضل وقت لقراءة أذكار الصباح؟
            ج: من طلوع الفجر الصادق حتى طلوع الشمس، ويمتد وقتها عند جمع من العلماء إلى الزوال.
        """.trimIndent()

        val batch = NoorLocalAiEngine.parseImportedKnowledgeText(
            rawContent = sampleTxt,
            fileName = "adhkar_notes.txt",
            defaultCategory = "WORSHIP",
            existingHashes = emptySet()
        )

        assertEquals(1, batch.items.size)
        assertEquals(1, batch.skippedDuplicates)
        assertEquals("PENDING", batch.items.first().approvalStatus)
    }

    @Test
    fun ecosystemContentCatalog_providesCompleteAuthenticOfflineSeedData() {
        assertTrue(EcosystemContentCatalog.tajweedRules.size >= 8)
        assertTrue(EcosystemContentCatalog.seedAiKnowledge().size >= 10)
        assertTrue(EcosystemContentCatalog.seedLibraryBooks().size >= 8)
        assertTrue(EcosystemContentCatalog.seedQuizQuestions().size >= 18)
        assertTrue(EcosystemContentCatalog.verifiedDuas.size >= 8)
        assertTrue(EcosystemContentCatalog.quranRadioStations.size >= 5)
        assertTrue(EcosystemContentCatalog.kidsStories.size >= 4)
        assertTrue(EcosystemContentCatalog.kidsDuas.size >= 6)
    }
}
