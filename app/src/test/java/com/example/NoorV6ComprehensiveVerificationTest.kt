package com.example

import android.content.Context
import android.speech.SpeechRecognizer
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SpeechErrorAction
import com.example.audio.SpeechErrorRecoveryPolicy
import com.example.data.cloud.AdCardDoc
import com.example.data.cloud.AdPlacement
import com.example.data.cloud.AdminMessageDoc
import com.example.data.cloud.AdminMessagePriority
import com.example.data.cloud.AdminMessageType
import com.example.data.cloud.AdminRole
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorPlatformConstants
import com.example.data.cloud.NoorSecurityValidators
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.local.db.AdminAdCardEntity
import com.example.data.local.db.AdminMessageEntity
import com.example.data.local.db.FastingLogEntity
import com.example.data.local.db.KhatmPlanEntity
import com.example.data.local.db.NoorDatabase
import com.example.data.quran.MushafPageImageManager
import com.example.data.repository.EcosystemRepository
import com.example.domain.model.QuranVerse
import com.example.domain.quran.AssessmentStrictness
import com.example.domain.quran.QuranRecitationAssessmentEngine
import com.example.domain.quran.RecitationOverallLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive Verification Suite for NOOR v6 Release:
 * 1. Exact 604-Page Madinah Mushaf Catalog & Offline Image Manager validation
 * 2. Khatmah & Fasting Crash-Fix, Duplicate Same-Day Guard & Room Persistence
 * 3. Recitation & Memorization Assessment Error 10 Recovery Policy & Accuracy Engine
 * 4. Super Admin RBAC Enforcement & Global Feature Flags
 * 5. Administrative Advertisement Cards, Strict Ad-Free Worship Screens & URL Security
 * 6. Admin-to-User Messaging, Read Tracking & Non-Destructive Room MIGRATION_5_6
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorV6ComprehensiveVerificationTest {

    // -------------------------------------------------------------------------
    // 1. Exact 604-Page Madinah Mushaf Catalog & Image Validation
    // -------------------------------------------------------------------------

    @Test
    fun madinahMushafCatalog_mapsAll604PagesWithContinuousJuzHizbAndMirrors() {
        assertEquals(604, MadinahMushafPageCatalog.TOTAL_PAGES)
        assertEquals(30, MadinahMushafPageCatalog.JUZ_START_PAGES.size)
        assertEquals(1, MadinahMushafPageCatalog.JUZ_START_PAGES.first())
        assertEquals(582, MadinahMushafPageCatalog.JUZ_START_PAGES.last())

        var previousJuz = 1
        var previousHizb = 1
        for (page in 1..604) {
            val meta = MadinahMushafPageCatalog.pageMeta(page)
            assertEquals(page, meta.pageNumber)
            assertTrue("Juz out of range on page $page", meta.juzNumber in 1..30)
            assertTrue("Hizb out of range on page $page", meta.hizbNumber in 1..60)
            assertTrue("Juz must be monotonic on page $page", meta.juzNumber >= previousJuz)
            assertTrue("Hizb must be monotonic on page $page", meta.hizbNumber >= previousHizb)
            assertTrue("Primary surah must be 1..114 on page $page", meta.primarySurahNumber in 1..114)
            assertTrue("At least 3 verified mirrors expected on page $page", meta.imageUrls.size >= 3)
            assertTrue(
                "All Mushaf mirror URLs must use HTTPS on page $page",
                meta.imageUrls.all { it.startsWith("https://") }
            )
            assertEquals("page_${page.toString().padStart(3, '0')}.png", meta.fileName)
            previousJuz = meta.juzNumber
            previousHizb = meta.hizbNumber
        }
        assertEquals(30, previousJuz)
        assertEquals(60, previousHizb)

        // Spot-check key Mushaf anchors
        assertEquals(1, MadinahMushafPageCatalog.pageMeta(1).primarySurahNumber) // Al-Fatiha
        assertEquals(2, MadinahMushafPageCatalog.pageMeta(2).primarySurahNumber) // Al-Baqarah
        assertEquals(30, MadinahMushafPageCatalog.juzForPage(604)) // Juz Amma
        assertEquals(1..21, MadinahMushafPageCatalog.pagesForJuz(1))
        assertEquals(582..604, MadinahMushafPageCatalog.pagesForJuz(30))
    }

    @Test
    fun mushafPageImageManager_validatesRealImageHeadersAndFilenames() {
        // Valid PNG magic bytes with >= 2048 bytes
        val validPng = ByteArray(4096).apply {
            this[0] = 0x89.toByte()
            this[1] = 0x50.toByte()
            this[2] = 0x4E.toByte()
            this[3] = 0x47.toByte()
        }
        assertTrue(MushafPageImageManager.isValidImagePayload(validPng))

        // Valid JPEG magic bytes
        val validJpeg = ByteArray(4096).apply {
            this[0] = 0xFF.toByte()
            this[1] = 0xD8.toByte()
            this[2] = 0xFF.toByte()
        }
        assertTrue(MushafPageImageManager.isValidImagePayload(validJpeg))

        // Valid WebP magic bytes
        val validWebp = ByteArray(4096).apply {
            "RIFF".toByteArray().copyInto(this, 0)
            "WEBP".toByteArray().copyInto(this, 8)
        }
        assertTrue(MushafPageImageManager.isValidImagePayload(validWebp))

        // Reject HTML 404/error page even if > 2048 bytes
        val fakeHtml = ("<html><body>404 Not Found</body></html>" + "x".repeat(3000)).toByteArray()
        assertFalse(MushafPageImageManager.isValidImagePayload(fakeHtml))

        // Reject truncated PNG below minimum threshold
        val tinyPng = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertFalse(MushafPageImageManager.isValidImagePayload(tinyPng))

        // Filename extractor for local ZIP archive import
        assertEquals(1, MushafPageImageManager.extractPageNumberFromFileName("001.png"))
        assertEquals(293, MushafPageImageManager.extractPageNumberFromFileName("mushaf/page_293.jpg"))
        assertEquals(604, MushafPageImageManager.extractPageNumberFromFileName("page604.webp"))
        assertNull(MushafPageImageManager.extractPageNumberFromFileName("page605.png"))
        assertNull(MushafPageImageManager.extractPageNumberFromFileName("readme.txt"))
    }

    // -------------------------------------------------------------------------
    // 2. Khatmah & Fasting Crash-Fix, Duplicate Guard & Persistence
    // -------------------------------------------------------------------------

    @Test
    fun khatmahAndFasting_preventSameDayDuplicateAndPersistReliablyInRoomV6() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NoorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = EcosystemRepository(db.ecosystemDao(), db.noorDao(), context)

            // Create Khatmah plan
            val planId = repo.saveKhatmPlan(
                KhatmPlanEntity(
                    title = "ختمة رمضان المبارك",
                    startDateKey = "2026-11-22",
                    targetDateKey = "2026-12-22",
                    currentPage = 1,
                    currentJuz = 1,
                    dailyTargetPages = 20,
                    totalPages = 604
                )
            )
            assertTrue("Inserted Khatmah plan ID must be > 0", planId > 0L)

            val savedPlan = repo.khatmPlans.first().first { it.id == planId }
            assertEquals(1, savedPlan.currentPage)

            // First Daily Wird completion succeeds
            val firstAdvance = repo.advanceKhatmPlan(savedPlan, pagesAdded = 20, allowExtraSameDay = false)
            assertTrue("First daily wird completion must succeed", firstAdvance)

            val afterFirst = repo.khatmPlans.first().first { it.id == planId }
            assertEquals(21, afterFirst.currentPage)
            assertEquals(2, afterFirst.currentJuz)

            // Second Daily Wird tap on the same day without extra flag is blocked safely (no duplicate)
            val duplicateBlocked = repo.advanceKhatmPlan(afterFirst, pagesAdded = 20, allowExtraSameDay = false)
            assertFalse("Same-day duplicate daily wird must be prevented", duplicateBlocked)
            assertEquals(21, repo.khatmPlans.first().first { it.id == planId }.currentPage)

            // Explicit Extra Wird on same day succeeds and updates page/Juz cleanly
            val extraAdvance = repo.advanceKhatmPlan(afterFirst, pagesAdded = 20, allowExtraSameDay = true)
            assertTrue("Explicit extra same-day wird must succeed", extraAdvance)
            assertEquals(41, repo.khatmPlans.first().first { it.id == planId }.currentPage)

            // Fasting log creation, persistence, and safe completion
            repo.saveFastingLog(
                FastingLogEntity(
                    dateKey = "2026-11-22",
                    fastType = "SUNNAH",
                    completed = true,
                    notes = "صيام الإثنين"
                )
            )
            val fastingLogs = repo.fastingLogs.first()
            assertEquals(1, fastingLogs.size)
            assertEquals("2026-11-22", fastingLogs.first().dateKey)
            assertEquals("SUNNAH", fastingLogs.first().fastType)
            assertTrue(fastingLogs.first().completed)

            // Deleting fasting log works cleanly
            repo.deleteFastingLog("2026-11-22")
            assertTrue(repo.fastingLogs.first().isEmpty())
        } finally {
            db.close()
        }
    }

    // -------------------------------------------------------------------------
    // 3. Recitation Assessment Error 10 Recovery Policy & Honesty
    // -------------------------------------------------------------------------

    @Test
    fun speechErrorRecoveryPolicy_fixesError10WithExponentialBackoffAndCleanPause() {
        // 1st Error 10 -> destroy recognizer & cool down 3.5s
        val d0 = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechErrorRecoveryPolicy.ERROR_TOO_MANY_REQUESTS,
            consecutiveSilence = 0,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = true
        )
        assertEquals(SpeechErrorAction.RESTART_AFTER_DELAY, d0.action)
        assertEquals(3500L, d0.delayMs)
        assertTrue(d0.destroyRecognizerFirst)
        assertTrue(d0.isRateLimited)

        // 2nd Error 10 -> destroy recognizer & cool down 7.0s
        val d1 = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechErrorRecoveryPolicy.ERROR_TOO_MANY_REQUESTS,
            consecutiveSilence = 0,
            consecutiveRateLimits = 1,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = true
        )
        assertEquals(SpeechErrorAction.RESTART_AFTER_DELAY, d1.action)
        assertEquals(7000L, d1.delayMs)
        assertTrue(d1.destroyRecognizerFirst)
        assertTrue(d1.isRateLimited)

        // 3rd Error 10 -> pause cleanly preserving transcript (never infinite rapid retry!)
        val d2 = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechErrorRecoveryPolicy.ERROR_TOO_MANY_REQUESTS,
            consecutiveSilence = 0,
            consecutiveRateLimits = 2,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = true
        )
        assertEquals(SpeechErrorAction.PAUSE_CLEANLY, d2.action)
        assertTrue(d2.destroyRecognizerFirst)
        assertTrue(d2.isRateLimited)

        // Silence auto-pause after 3 timeouts prevents Error 10 in the first place
        val silence1 = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechRecognizer.ERROR_NO_MATCH,
            consecutiveSilence = 0,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = false
        )
        assertEquals(SpeechErrorAction.RESTART_AFTER_DELAY, silence1.action)
        assertTrue(silence1.delayMs >= SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS)

        val silencePause = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            consecutiveSilence = SpeechErrorRecoveryPolicy.MAX_CONSECUTIVE_SILENCE_TIMEOUTS,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = false
        )
        assertEquals(SpeechErrorAction.PAUSE_CLEANLY, silencePause.action)
        assertTrue(silencePause.destroyRecognizerFirst)

        // Offline language unavailable -> switch seamlessly to hybrid recognizer
        val langFallback = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechErrorRecoveryPolicy.ERROR_LANGUAGE_UNAVAILABLE,
            consecutiveSilence = 0,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = true
        )
        assertEquals(SpeechErrorAction.SWITCH_TO_HYBRID_AND_RESTART, langFallback.action)
    }

    @Test
    fun recitationAssessmentEngine_evaluatesAccurateAndFlawedRecitationsHonestly() {
        val ikhlas = listOf(
            QuranVerse(112, 1, 6222, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Say, He is Allah, One", "", pageNumber = 604, juzNumber = 30),
            QuranVerse(112, 2, 6223, "اللَّهُ الصَّمَدُ", "Allah, the Eternal Refuge", "", pageNumber = 604, juzNumber = 30),
            QuranVerse(112, 3, 6224, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born", "", pageNumber = 604, juzNumber = 30),
            QuranVerse(112, 4, 6225, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "Nor is there to Him any equivalent", "", pageNumber = 604, juzNumber = 30)
        )

        val exact = QuranRecitationAssessmentEngine.evaluate(
            verses = ikhlas,
            spokenTranscript = "قل هو الله أحد الله الصمد لم يلد ولم يولد ولم يكن له كفوا أحد",
            strictness = AssessmentStrictness.BALANCED
        )
        assertTrue("Exact recitation should score >= 95%, got ${exact.accuracyPercent}", exact.accuracyPercent >= 95)
        assertEquals(RecitationOverallLevel.EXCELLENT, exact.overallLevel)
        assertEquals(0, exact.omittedWords.size)

        // Empty / unintelligible transcript must NEVER pretend high accuracy
        val empty = QuranRecitationAssessmentEngine.evaluate(
            verses = ikhlas,
            spokenTranscript = "",
            strictness = AssessmentStrictness.BALANCED
        )
        assertEquals(0, empty.accuracyPercent)
        assertEquals(RecitationOverallLevel.RECOGNITION_UNCLEAR, empty.overallLevel)
    }

    // -------------------------------------------------------------------------
    // 4. Super Admin RBAC & Global Feature Controls
    // -------------------------------------------------------------------------

    @Test
    fun rbacRoles_strictlyRestrictSuperAdminExclusivePermissions() {
        assertEquals("mustafa20090520@gmail.com", NoorPlatformConstants.PRIMARY_OWNER_EMAIL)

        // Super Admin has full authority
        assertTrue(AdminRole.SUPER_ADMIN.canAssignRoles)
        assertTrue(AdminRole.SUPER_ADMIN.canManageAds)
        assertTrue(AdminRole.SUPER_ADMIN.canControlGlobalFeatures)
        assertTrue(AdminRole.SUPER_ADMIN.canSendAdminMessages)
        assertTrue(AdminRole.SUPER_ADMIN.canManageUsers)

        // Ordinary Admin can manage users/messages but CANNOT manage ads, global flags, or roles
        assertFalse(AdminRole.ADMIN.canAssignRoles)
        assertFalse(AdminRole.ADMIN.canManageAds)
        assertFalse(AdminRole.ADMIN.canControlGlobalFeatures)
        assertTrue(AdminRole.ADMIN.canSendAdminMessages)
        assertTrue(AdminRole.ADMIN.canManageUsers)

        // Regular User has ZERO admin privileges
        assertFalse(AdminRole.USER.isPrivileged)
        assertFalse(AdminRole.USER.canManageAds)
        assertFalse(AdminRole.USER.canControlGlobalFeatures)
        assertFalse(AdminRole.USER.canSendAdminMessages)
        assertFalse(AdminRole.USER.canManageUsers)

        // GlobalAppConfigDoc exposes all v6 feature toggles enabled by default
        val defaultConfig = GlobalAppConfigDoc()
        assertTrue(defaultConfig.featureMushafImagesEnabled)
        assertTrue(defaultConfig.featureMemorizationEnabled)
        assertTrue(defaultConfig.featureListeningAssessmentEnabled)
        assertTrue(defaultConfig.featureKhatmahEnabled)
        assertTrue(defaultConfig.featureFastingEnabled)
        assertTrue(defaultConfig.featureDailyWirdEnabled)
        assertTrue(defaultConfig.featureNotificationsEnabled)
        assertTrue(defaultConfig.featureAdsEnabled)
    }

    // -------------------------------------------------------------------------
    // 5. Administrative Advertisement Cards, Ad-Free Worship Screens & Security
    // -------------------------------------------------------------------------

    @Test
    fun adCardSecurityAndPlacement_blockWorshipScreensAndUnsafeUrls() {
        // Approved placements only
        assertTrue(AdPlacement.isAllowedPlacement("HOME"))
        assertTrue(AdPlacement.isAllowedPlacement("ECOSYSTEM_HUB"))
        assertTrue(AdPlacement.isAllowedPlacement("AUDIO_LIBRARY"))

        // Forbidden worship placements rejected
        assertFalse(AdPlacement.isAllowedPlacement("QURAN_READER"))
        assertFalse(AdPlacement.isAllowedPlacement("HIFZ"))
        assertFalse(AdPlacement.isAllowedPlacement("PRAYER_TIMES"))

        // Forbidden screen route detector catches every worship screen
        assertTrue(AdPlacement.isForbiddenScreenRoute("quran_reader"))
        assertTrue(AdPlacement.isForbiddenScreenRoute("mushaf_page"))
        assertTrue(AdPlacement.isForbiddenScreenRoute("hifz_ai"))
        assertTrue(AdPlacement.isForbiddenScreenRoute("recitation_assessment"))
        assertTrue(AdPlacement.isForbiddenScreenRoute("prayer_times"))
        assertFalse(AdPlacement.isForbiddenScreenRoute("home"))
        assertFalse(AdPlacement.isForbiddenScreenRoute("ecosystem_hub"))
        assertFalse(AdPlacement.isForbiddenScreenRoute("audio_library"))

        // Destination URL validator strictly requires https:// or empty
        assertTrue(NoorSecurityValidators.isSafeOptionalHttpsUrl(""))
        assertTrue(NoorSecurityValidators.isSafeOptionalHttpsUrl("https://noor.example.org/ramadan"))
        assertFalse(NoorSecurityValidators.isSafeOptionalHttpsUrl("http://insecure.example.org"))
        assertFalse(NoorSecurityValidators.isSafeOptionalHttpsUrl("javascript:alert(1)"))
        assertFalse(NoorSecurityValidators.isSafeOptionalHttpsUrl("file:///etc/passwd"))

        // Image URI validator allows https://, content:// (Photo Picker), file://, or empty
        assertTrue(NoorSecurityValidators.isSafeOptionalImageUri(""))
        assertTrue(NoorSecurityValidators.isSafeOptionalImageUri("content://media/picker/0/123"))
        assertTrue(NoorSecurityValidators.isSafeOptionalImageUri("https://cdn.example.org/banner.png"))
        assertFalse(NoorSecurityValidators.isSafeOptionalImageUri("javascript:evil()"))

        // Schedule window enforcement on AdCardDoc
        val now = 1_750_000_000_000L
        val activeCard = AdCardDoc(
            id = "ad_1",
            titleAr = "مسابقة حفظ القرآن",
            placement = AdPlacement.HOME.key,
            enabled = true,
            startAtMillis = now - 60_000L,
            endAtMillis = now + 60_000L
        )
        assertTrue(activeCard.isCurrentlyActive(now))
        assertFalse(activeCard.copy(enabled = false).isCurrentlyActive(now))
        assertFalse(activeCard.copy(endAtMillis = now - 1_000L).isCurrentlyActive(now))
        assertFalse(activeCard.copy(placement = "QURAN_READER").isCurrentlyActive(now))
    }

    // -------------------------------------------------------------------------
    // 6. Local Room v6 Ad Cards, Admin Messages & MIGRATION_5_6 Preservation
    // -------------------------------------------------------------------------

    @Test
    fun roomV6AdCardsAndAdminMessages_supportCrudTelemetryAndReadTracking() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NoorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = EcosystemRepository(db.ecosystemDao(), db.noorDao(), context)

            // Ad card CRUD + impression/click telemetry
            repo.saveAdCardLocal(
                AdminAdCardEntity(
                    id = "ad_ramadan_1",
                    titleAr = "دورة التجويد المجانية",
                    titleEn = "Free Tajweed Course",
                    descriptionAr = "انضم إلى حلقات نور القرآنية",
                    placement = AdPlacement.HOME.key,
                    enabled = true,
                    displayOrder = 1
                )
            )
            repo.recordAdImpressionLocal("ad_ramadan_1")
            repo.recordAdImpressionLocal("ad_ramadan_1")
            repo.recordAdClickLocal("ad_ramadan_1")

            val savedAds = repo.adminAdCards.first()
            assertEquals(1, savedAds.size)
            assertEquals(2L, savedAds.first().impressionsCount)
            assertEquals(1L, savedAds.first().clicksCount)

            // Admin-to-User message CRUD + read tracking
            repo.saveAdminMessageLocal(
                AdminMessageEntity(
                    id = "msg_welcome_1",
                    targetType = "ALL",
                    titleAr = "أهلاً بكم في تحديث نور",
                    titleEn = "Welcome to NOOR Update",
                    bodyAr = "تمت إضافة مصحف المدينة المصوّر (٦٠٤ صفحة).",
                    messageType = AdminMessageType.FEATURE_UPDATE.key,
                    priority = AdminMessagePriority.HIGH.key,
                    readByCurrentUser = false
                )
            )
            assertEquals(1, repo.adminMessages.first().size)
            assertFalse(repo.adminMessages.first().first().readByCurrentUser)

            repo.markAdminMessageReadLocal("msg_welcome_1")
            assertTrue(repo.adminMessages.first().first().readByCurrentUser)

            // Delete cleanup
            repo.deleteAdCardLocal("ad_ramadan_1")
            repo.deleteAdminMessageLocal("msg_welcome_1")
            assertTrue(repo.adminAdCards.first().isEmpty())
            assertTrue(repo.adminMessages.first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun roomMigration5To6_createsNewTablesWithoutDroppingExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_noor_migration_5_6.db"
        context.deleteDatabase(dbName)

        // Create a raw v5 SQLite database with an existing Khatmah row
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS khatm_plans (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            startDateKey TEXT NOT NULL,
                            targetDateKey TEXT NOT NULL,
                            currentPage INTEGER NOT NULL,
                            currentJuz INTEGER NOT NULL,
                            dailyTargetPages INTEGER NOT NULL,
                            totalPages INTEGER NOT NULL,
                            isCompleted INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "INSERT INTO khatm_plans (title, startDateKey, targetDateKey, currentPage, currentJuz, dailyTargetPages, totalPages, isCompleted) " +
                            "VALUES ('Preserved Khatm', '2026-11-01', '2026-12-01', 120, 6, 20, 604, 0)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqliteDb = helper.writableDatabase
        try {
            // Execute actual production MIGRATION_5_6
            NoorDatabase.MIGRATION_5_6.migrate(sqliteDb)

            // Verify existing Khatmah row is 100% preserved
            sqliteDb.query("SELECT title, currentPage FROM khatm_plans").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Preserved Khatm", c.getString(0))
                assertEquals(120, c.getInt(1))
            }

            // Verify new admin_ad_cards table exists and accepts inserts
            sqliteDb.execSQL(
                "INSERT INTO admin_ad_cards (id, titleAr, titleEn, descriptionAr, descriptionEn, imageUriOrUrl, destinationUrl, placement, enabled, startAtMillis, endAtMillis, displayOrder, impressionsCount, clicksCount, createdBy, updatedAtMillis) " +
                    "VALUES ('ad_mig', 'إعلان', 'Ad', '', '', '', '', 'HOME', 1, 0, 0, 1, 0, 0, 'owner', 1000)"
            )
            sqliteDb.query("SELECT id FROM admin_ad_cards WHERE id = 'ad_mig'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("ad_mig", c.getString(0))
            }

            // Verify new admin_messages table exists and accepts inserts
            sqliteDb.execSQL(
                "INSERT INTO admin_messages (id, targetType, targetGroup, targetUserUid, targetUserEmail, titleAr, titleEn, bodyAr, bodyEn, messageType, priority, imageUriOrUrl, scheduledAtMillis, sentByUid, sentByEmail, delivered, readByCurrentUser, readAtMillis, createdAtMillis) " +
                    "VALUES ('msg_mig', 'ALL', 'ALL', '', '', 'رسالة', 'Msg', 'محتوى', 'Body', 'SYSTEM', 'NORMAL', '', 0, 'u1', 'owner@noor', 1, 0, 0, 1000)"
            )
            sqliteDb.query("SELECT id FROM admin_messages WHERE id = 'msg_mig'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("msg_mig", c.getString(0))
            }
        } finally {
            sqliteDb.close()
            helper.close()
            context.deleteDatabase(dbName)
        }
    }
}
