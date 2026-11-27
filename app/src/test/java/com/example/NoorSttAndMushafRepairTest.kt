package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import com.example.audio.LiveRecitationStatus
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranLiveRecitationListener
import com.example.audio.QuranRecitationRecorder
import com.example.audio.RecitationRecordingStatus
import com.example.audio.RecognitionProviderInfo
import com.example.audio.SpeechErrorAction
import com.example.audio.SpeechErrorReason
import com.example.audio.SpeechErrorRecoveryPolicy
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.quran.CoordinatePrecision
import com.example.data.quran.MushafCoordinateRepository
import com.example.data.quran.MushafCoordinateTransform
import com.example.domain.model.AppLanguage
import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.quran.QuranRecitationAssessmentEngine
import com.example.domain.quran.RecitationOverallLevel
import com.example.domain.quran.WordEvaluationStatus
import com.example.ui.i18n.L10n
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.abs

/**
 * Targeted Robolectric verification suite for the NOOR Arabic STT (Error 12),
 * Mushaf Image Verse-Selection precision repair, and Uzbek localization.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorSttAndMushafRepairTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun cleanPersistedCoordsDir() {
        val coordsDir = File(context.filesDir, "mushaf_coords")
        if (coordsDir.exists()) {
            coordsDir.listFiles()?.forEach { it.delete() }
        }
    }

    // ============================================================================================
    // PART 1: ARABIC SPEECH-TO-TEXT (ONLINE, ERROR 12, OFFLINE, DIAGNOSTICS, RECORDER, ASSESSMENT)
    // ============================================================================================

    @Test
    fun sttIntentConfig_onlineModeOmitsError12TriggeringExtrasAndConfiguresArabicProperly() {
        val listener = QuranLiveRecitationListener(context)
        val onlineIntent = listener.buildRecognizerIntent(locale = "ar-SA", preferOffline = false)

        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, onlineIntent.action)
        assertEquals(
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            onlineIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL)
        )
        assertEquals("ar-SA", onlineIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals("ar-SA", onlineIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))
        assertEquals(context.packageName, onlineIntent.getStringExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE))
        assertTrue(onlineIntent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        assertEquals(5, onlineIntent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0))

        // CRITICAL REGRESSION GUARDS FOR ERROR 12:
        assertFalse(
            "Online intent MUST NOT set EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE (causes Error 12)",
            onlineIntent.hasExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE)
        )
        assertFalse(
            "Online intent MUST NOT force EXTRA_PREFER_OFFLINE",
            onlineIntent.hasExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE)
        )

        // Offline mode explicitly sets EXTRA_PREFER_OFFLINE = true while still omitting strict-return flag
        val offlineIntent = listener.buildRecognizerIntent(locale = "ar", preferOffline = true)
        assertTrue(offlineIntent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
        assertFalse(offlineIntent.hasExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE))
    }

    @Test
    fun sttOnlineArabicRecognition_handlesPartialAndFinalResultsAndRejectsEmptyTranscript() {
        val listener = QuranLiveRecitationListener(context)
        val fatiha = listOf(
            QuranVerse(1, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "", "", pageNumber = 1),
            QuranVerse(1, 2, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "", "", pageNumber = 1),
            QuranVerse(1, 3, 3, "الرَّحْمَٰنِ الرَّحِيمِ", "", "", pageNumber = 1)
        )
        val googleProvider = RecognitionProviderInfo(
            packageName = "com.google.android.tts",
            serviceName = "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService",
            label = "Google Speech Services",
            isPreferredOnlineProvider = true
        )
        listener.setTestOverrides(
            networkAvailable = { true },
            providers = { listOf(googleProvider) },
            serviceAvailable = { true },
            micPermissionGranted = { true },
            offlineArabicInstalled = false
        )
        listener.loadSurahContext(1, fatiha, initialAyah = 1)
        listener.startListening(resetTranscript = true)

        assertEquals(LiveRecitationStatus.LISTENING, listener.state.value.status)
        assertTrue(listener.state.value.usingOnlineMode)
        assertTrue(listener.state.value.isInternetAvailable)
        assertEquals("Google Speech Services", listener.state.value.activeProviderName)

        // Partial Arabic result updates effectiveTranscript immediately without finalizing duplicates
        listener.simulateRecognizerResultsForTest(listOf("بسم الله الرحمن"), isFinal = false)
        assertTrue(listener.state.value.effectiveTranscript.contains("بسم الله"))
        assertFalse(listener.state.value.hasReceivedEmptyResult)

        // Final Arabic result matches Ayah 1 cleanly
        listener.simulateRecognizerResultsForTest(listOf("بسم الله الرحمن الرحيم"), isFinal = true)
        assertEquals(LiveRecitationStatus.MATCHED, listener.state.value.status)
        assertEquals(1, listener.state.value.trackedAyah)
        assertTrue(listener.state.value.accumulatedTranscript.contains("بسم الله الرحمن الرحيم"))

        // Empty / blank final result MUST NOT be reported as successful recognition!
        listener.simulateRecognizerResultsForTest(listOf("   ", ""), isFinal = true)
        assertTrue("Empty result must set hasReceivedEmptyResult=true", listener.state.value.hasReceivedEmptyResult)
        assertNotEquals("Empty result must never report MATCHED", LiveRecitationStatus.MATCHED, listener.state.value.status)

        listener.stopListening()
    }

    @Test
    fun sttError12WithInternet_stepsThroughArabicLocalesAndAlternateProvidersBeforeReportingUnsupported() {
        val listener = QuranLiveRecitationListener(context)
        val providerA = RecognitionProviderInfo(
            packageName = "com.google.android.tts",
            serviceName = "GoogleOnlineService",
            label = "Google Speech Services",
            isPreferredOnlineProvider = true
        )
        val providerB = RecognitionProviderInfo(
            packageName = "com.vendor.speech",
            serviceName = "VendorSpeechService",
            label = "Vendor Speech Service",
            isPreferredOnlineProvider = false
        )
        listener.setTestOverrides(
            networkAvailable = { true },
            providers = { listOf(providerA, providerB) },
            serviceAvailable = { true },
            micPermissionGranted = { true },
            offlineArabicInstalled = false
        )
        listener.startListening(resetTranscript = true)
        assertEquals("ar-SA", listener.state.value.activeLocale)
        assertEquals("Google Speech Services", listener.state.value.activeProviderName)

        // First Error 12 on ar-SA -> steps to "ar" on same provider (does NOT immediately fail!)
        listener.simulateRecognizerErrorForTest(SpeechErrorRecoveryPolicy.ERROR_LANGUAGE_NOT_SUPPORTED)
        assertEquals("ar", listener.state.value.activeLocale)
        assertEquals("Google Speech Services", listener.state.value.activeProviderName)
        assertNotEquals(LiveRecitationStatus.ERROR, listener.state.value.status)

        // Step through remaining Arabic locales on providerA until switching to providerB
        repeat(SpeechErrorRecoveryPolicy.ARABIC_LOCALE_CANDIDATES.size - 1) {
            listener.simulateRecognizerErrorForTest(SpeechErrorRecoveryPolicy.ERROR_LANGUAGE_NOT_SUPPORTED)
        }
        assertEquals("Vendor Speech Service", listener.state.value.activeProviderName)
        assertEquals("ar-SA", listener.state.value.activeLocale)

        // Exhaust remaining locales on providerB
        repeat(SpeechErrorRecoveryPolicy.ARABIC_LOCALE_CANDIDATES.size) {
            listener.simulateRecognizerErrorForTest(SpeechErrorRecoveryPolicy.ERROR_LANGUAGE_NOT_SUPPORTED)
        }

        // Now with internet STILL ON, final failure MUST be ARABIC_UNSUPPORTED — NEVER NO_INTERNET!
        assertEquals(LiveRecitationStatus.ERROR, listener.state.value.status)
        assertEquals(SpeechErrorReason.ARABIC_UNSUPPORTED, listener.state.value.errorReason)
        assertTrue(listener.state.value.isInternetAvailable)
        assertTrue(listener.state.value.errorMessageEn.contains("Arabic", ignoreCase = true))
        listener.stopListening()
    }

    @Test
    fun sttFailureModes_explicitlyDistinguishAllSevenDiagnosticCauses() {
        // 1. Permission denied
        run {
            val l = QuranLiveRecitationListener(context)
            l.setTestOverrides(
                networkAvailable = { true },
                serviceAvailable = { true },
                micPermissionGranted = { false }
            )
            l.startListening()
            assertEquals(LiveRecitationStatus.ERROR, l.state.value.status)
            assertEquals(SpeechErrorReason.PERMISSION_DENIED, l.state.value.errorReason)
        }

        // 2. Recognition service unavailable
        run {
            val l = QuranLiveRecitationListener(context)
            l.setTestOverrides(
                networkAvailable = { true },
                providers = { emptyList() },
                serviceAvailable = { false },
                micPermissionGranted = { true }
            )
            l.startListening()
            assertEquals(LiveRecitationStatus.ERROR, l.state.value.status)
            assertEquals(SpeechErrorReason.SERVICE_UNAVAILABLE, l.state.value.errorReason)
        }

        // 3. Internet disabled AND offline Arabic pack missing -> NO_INTERNET (not ARABIC_UNSUPPORTED)
        run {
            val l = QuranLiveRecitationListener(context)
            l.setTestOverrides(
                networkAvailable = { false },
                serviceAvailable = { true },
                micPermissionGranted = { true },
                offlineArabicInstalled = false
            )
            l.startListening()
            assertEquals(LiveRecitationStatus.ERROR, l.state.value.status)
            assertEquals(SpeechErrorReason.NO_INTERNET, l.state.value.errorReason)
            assertFalse(l.state.value.isInternetAvailable)
        }

        // 4. Offline language pack missing when offline mode requested WITH internet -> auto-switches to online!
        val switchDecision = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechErrorRecoveryPolicy.ERROR_LANGUAGE_NOT_SUPPORTED,
            consecutiveSilence = 0,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = true,
            isInternetAvailable = true
        )
        assertEquals(SpeechErrorAction.SWITCH_TO_HYBRID_AND_RESTART, switchDecision.action)
        assertEquals(SpeechErrorReason.OFFLINE_PACK_MISSING, switchDecision.reason)

        // 5. Offline language pack missing when offline mode requested WITHOUT internet -> OFFLINE_PACK_MISSING
        val offlineMissingDecision = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechErrorRecoveryPolicy.ERROR_LANGUAGE_NOT_SUPPORTED,
            consecutiveSilence = 0,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = true,
            isInternetAvailable = false
        )
        assertEquals(SpeechErrorAction.STOP_WITH_ERROR, offlineMissingDecision.action)
        assertEquals(SpeechErrorReason.OFFLINE_PACK_MISSING, offlineMissingDecision.reason)

        // 6. Recognition timeout -> pauses cleanly after 3 timeouts with RECOGNITION_TIMEOUT
        val timeoutDecision = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            consecutiveSilence = SpeechErrorRecoveryPolicy.MAX_CONSECUTIVE_SILENCE_TIMEOUTS,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = false,
            isInternetAvailable = true
        )
        assertEquals(SpeechErrorAction.PAUSE_CLEANLY, timeoutDecision.action)
        assertEquals(SpeechErrorReason.RECOGNITION_TIMEOUT, timeoutDecision.reason)

        // 7. Network / provider failure: distinguishes offline NO_INTERNET vs online NETWORK_OR_PROVIDER_FAILURE
        val netOffline = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechRecognizer.ERROR_NETWORK,
            consecutiveSilence = 0,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 0,
            sessionOfflineFlag = false,
            isInternetAvailable = false
        )
        assertEquals(SpeechErrorReason.NO_INTERNET, netOffline.reason)

        val providerFailOnline = SpeechErrorRecoveryPolicy.decide(
            errorCode = SpeechRecognizer.ERROR_SERVER,
            consecutiveSilence = 0,
            consecutiveRateLimits = 0,
            consecutiveOtherErrors = 1,
            sessionOfflineFlag = false,
            isInternetAvailable = true
        )
        assertEquals(SpeechErrorAction.STOP_WITH_ERROR, providerFailOnline.action)
        assertEquals(SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE, providerFailOnline.reason)
    }

    @Test
    fun audioRecorderFallback_enforcesPermissionIntegrityAndCleanDelete() {
        val recorder = QuranRecitationRecorder(context)
        assertFalse(recorder.startRecording("hifz_test"))
        assertEquals(RecitationRecordingStatus.ERROR, recorder.recordingState.value.status)

        // Missing file rejected
        assertFalse(recorder.playRecording(File(context.cacheDir, "does_not_exist.m4a").absolutePath))
        assertNotNull(recorder.playbackState.value.errorEn)

        // Tiny / corrupt file (<512 bytes) rejected
        val tiny = File(context.cacheDir, "tiny.m4a").apply { writeBytes(ByteArray(128)) }
        assertFalse(recorder.playRecording(tiny.absolutePath))

        // Delete existing file succeeds cleanly
        assertTrue(recorder.deleteRecordingFile(tiny.absolutePath))
        assertFalse(tiny.exists())
        recorder.release()
    }

    @Test
    fun recitationAssessmentEngine_detectsExactOmittedAddedReplacedRepeatedSkippedAndOutOfOrder() {
        val verses = listOf(
            QuranVerse(112, 1, 6222, "قُلْ هُوَ اللَّهُ أَحَدٌ", "", "", pageNumber = 604),
            QuranVerse(112, 2, 6223, "اللَّهُ الصَّمَدُ", "", "", pageNumber = 604),
            QuranVerse(112, 3, 6224, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "", "", pageNumber = 604)
        )

        // Exact recitation
        val exact = QuranRecitationAssessmentEngine.evaluate(
            verses,
            "قل هو الله احد الله الصمد لم يلد ولم يولد"
        )
        assertEquals(RecitationOverallLevel.EXCELLENT, exact.overallLevel)
        assertTrue(exact.accuracyPercent >= 95)
        assertTrue(exact.omittedWords.isEmpty())
        assertTrue(exact.skippedVerses.isEmpty())

        // Omitted word + added word
        val omitAdd = QuranRecitationAssessmentEngine.evaluate(
            verses,
            "قل هو الله زيادة الصمد لم يلد ولم يولد"
        )
        assertTrue("Must detect omitted/replaced/added discrepancy", omitAdd.accuracyPercent < 100)
        assertTrue(omitAdd.wordEvaluations.any {
            it.status == WordEvaluationStatus.OMITTED ||
                it.status == WordEvaluationStatus.ADDED ||
                it.status == WordEvaluationStatus.REPLACED
        })

        // Repeated word
        val repeated = QuranRecitationAssessmentEngine.evaluate(
            verses,
            "قل هو هو الله احد الله الصمد لم يلد ولم يولد"
        )
        assertTrue("Must detect repeated word", repeated.repeatedWords.isNotEmpty() || repeated.addedWords.isNotEmpty())

        // Skipped verse (verse 2 omitted completely)
        val skipped = QuranRecitationAssessmentEngine.evaluate(
            verses,
            "قل هو الله احد لم يلد ولم يولد"
        )
        assertTrue("Verse 2 must be flagged as skipped", skipped.skippedVerses.contains(2))

        // Out-of-order verses (verse 3 recited before verse 1)
        val outOfOrder = QuranRecitationAssessmentEngine.evaluate(
            verses,
            "لم يلد ولم يولد قل هو الله احد الله الصمد"
        )
        assertTrue("Out-of-order recitation must be detected", outOfOrder.outOfOrderVerses.isNotEmpty())
    }

    // ============================================================================================
    // PART 2: MUSHAF IMAGE VERSE SELECTION, ACTUAL PNG DIMENSION ALIGNMENT & NO FAKE REGIONS
    // ============================================================================================

    @Test
    fun mushafImageCatalog_usesStrictKingFahdWidth1024MirrorsMatchingAyahInfoDb() {
        for (page in listOf(1, 2, 50, 293, 604)) {
            val urls = MadinahMushafPageCatalog.imageUrlsForPage(page)
            assertTrue("Page $page must have at least 2 width_1024 mirrors", urls.size >= 2)
            assertTrue(
                "Every mirror for page $page must be King Fahd width_1024 matching ayahinfo_1024.db",
                urls.all { it.contains("width_1024") && !it.contains("GovarJabbar", ignoreCase = true) }
            )
        }
    }

    @Test
    fun mushafCoordinateRepository_missingMetadataPageNeverFabricatesFakeSelectionRegions() = runBlocking {
        // Use a clean repository with NO ayahinfo_1024.db and NO cached page_250.json.
        // Page 250 is not one of the 4 bundled landmark pages (1, 2, 597, 604).
        val layout = MushafCoordinateRepository.buildMissingCoordinatesLayout(250)
        assertEquals(CoordinatePrecision.MISSING_COORDINATES, layout.precision)
        assertFalse("Missing metadata page must report hasVerifiedCoordinates=false", layout.hasVerifiedCoordinates)
        assertTrue("Missing metadata page MUST have empty ayahs list (never fake boxes!)", layout.ayahs.isEmpty())
        assertNull(
            "Hit testing missing-coordinates page must always return null",
            MushafCoordinateTransform.hitTestAyah(layout, 0.50f, 0.50f)
        )

        // Calling buildOfflineFallbackLayout with empty verses also returns MISSING_COORDINATES (never fake line-4 box!)
        val emptyFallback = MushafCoordinateRepository.buildOfflineFallbackLayout(250, emptyList())
        assertEquals(CoordinatePrecision.MISSING_COORDINATES, emptyFallback.precision)
        assertTrue(emptyFallback.ayahs.isEmpty())
    }

    @Test
    fun sqliteGlyphDb_normalizesAccuratelyForBoth1656And1638PngHeightsWithZeroVerticalDrift() = runBlocking {
        val tempDbFile = File(context.cacheDir, "test_aspect_ayahinfo_1024.db")
        if (tempDbFile.exists()) tempDbFile.delete()

        val db = SQLiteDatabase.openOrCreateDatabase(tempDbFile, null)
        db.execSQL(
            """
            CREATE TABLE glyphs (
                glyph_id INTEGER PRIMARY KEY,
                page_number INTEGER,
                line_number INTEGER,
                sura_number INTEGER,
                ayah_number INTEGER,
                position INTEGER,
                min_x INTEGER,
                max_x INTEGER,
                min_y INTEGER,
                max_y INTEGER
            )
            """.trimIndent()
        )
        // Page 3: Ayah 2:6 (2 words + decorative circular rosette at left edge of line 1), Ayah 2:7 (line 2)
        db.execSQL("INSERT INTO glyphs VALUES (1, 3, 1, 2, 6, 1, 700, 910, 140, 220)")
        db.execSQL("INSERT INTO glyphs VALUES (2, 3, 1, 2, 6, 2, 420, 690, 140, 220)")
        db.execSQL("INSERT INTO glyphs VALUES (3, 3, 1, 2, 6, 3, 345, 415, 145, 215)") // Compact circular end marker (70x70)
        db.execSQL("INSERT INTO glyphs VALUES (4, 3, 2, 2, 7, 1, 500, 910, 1400, 1490)")
        // Pad DB > 100KB
        db.beginTransaction()
        try {
            val stmt = db.compileStatement("INSERT INTO glyphs VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            for (i in 5..4500) {
                stmt.clearBindings()
                stmt.bindLong(1, i.toLong())
                stmt.bindLong(2, 99L)
                stmt.bindLong(3, ((i % 15) + 1).toLong())
                stmt.bindLong(4, 4L)
                stmt.bindLong(5, (i % 40 + 1).toLong())
                stmt.bindLong(6, (i % 8 + 1).toLong())
                stmt.bindLong(7, 100L)
                stmt.bindLong(8, 220L)
                stmt.bindLong(9, 120L)
                stmt.bindLong(10, 190L)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        db.close()

        val repo = MushafCoordinateRepository(context)
        assertTrue(repo.importGlyphDatabaseFromUri(Uri.fromFile(tempDbFile)))

        // Case A: PNG is 1024x1656
        val layout1656 = repo.getPageLayout(pageNumber = 3, actualImageWidth = 1024, actualImageHeight = 1656)
        assertEquals(CoordinatePrecision.EXACT_GLYPH_DB, layout1656.precision)
        assertEquals(1656, layout1656.imageHeight)
        val ayah2_7_1656 = layout1656.findAyah(2, 7)!!
        assertEquals(1400f / 1656f, ayah2_7_1656.lineRegions.first().bounds.top, 0.0005f)

        // Case B: PNG is 1024x1638 (different height -> normalized Y MUST adapt to 1638, NOT stay drifted at 1656!)
        val layout1638 = repo.getPageLayout(pageNumber = 3, actualImageWidth = 1024, actualImageHeight = 1638)
        assertEquals(1638, layout1638.imageHeight)
        val ayah2_7_1638 = layout1638.findAyah(2, 7)!!
        assertEquals(1400f / 1638f, ayah2_7_1638.lineRegions.first().bounds.top, 0.0005f)

        // Decorative end-marker rosette of 2:6 MUST be separated into endMarkerBounds and excluded from highlight rect!
        val ayah2_6 = layout1638.findAyah(2, 6)!!
        assertNotNull("End-marker circle must be separated", ayah2_6.endMarkerBounds)
        assertEquals(2, ayah2_6.words.size)
        val segBounds = ayah2_6.lineRegions.first().bounds
        assertTrue(
            "Highlight segment left edge must stay to the RIGHT of endMarkerBounds right edge",
            segBounds.left >= ayah2_6.endMarkerBounds!!.right - 0.001f
        )

        // Tapping blank space far away from lines 1 and 2 returns null
        assertNull(
            "Tapping blank paper between Ayah lines must return null",
            MushafCoordinateTransform.hitTestAyah(layout1638, 0.50f, 0.50f)
        )
        // Tapping exact word center of 2:7 selects 2:7
        val hit2_7 = MushafCoordinateTransform.hitTestAyah(layout1638, 700f / 1024f, 1445f / 1638f)
        assertEquals("2:7", hit2_7?.key)
    }

    @Test
    fun openingPages1And2_blankPaperNotSelectedAndMultiLineEndMarkerOnlyOnFinalLine() {
        val page1 = MushafCoordinateRepository.buildVerifiedBundledPageLayout(1)!!
        assertEquals(CoordinatePrecision.VERIFIED_BUNDLED_LAYOUT, page1.precision)
        assertTrue(page1.hasVerifiedCoordinates)
        assertEquals(7, page1.ayahs.size)

        // Outer margins, top Surah banner (above SPECIAL_P12_TOP), and blank bottom half MUST NOT select any Ayah!
        assertNull(MushafCoordinateTransform.hitTestAyah(page1, 0.50f, 0.05f))
        assertNull(MushafCoordinateTransform.hitTestAyah(page1, 0.02f, 0.30f))
        assertNull(MushafCoordinateTransform.hitTestAyah(page1, 0.98f, 0.30f))
        assertNull(MushafCoordinateTransform.hitTestAyah(page1, 0.50f, 0.80f))

        // Multi-line Ayah 1:7 spans lines 7 and 8: endMarkerBounds MUST sit strictly on line 8 (bottom > line 7 bottom)!
        val ayah1_7 = page1.findAyah(1, 7)!!
        assertEquals(2, ayah1_7.lineRegions.size)
        assertNotNull(ayah1_7.endMarkerBounds)
        val line7 = ayah1_7.lineRegions.first { it.lineNumber == 7 }.bounds
        val line8 = ayah1_7.lineRegions.first { it.lineNumber == 8 }.bounds
        assertTrue(
            "1:7 end marker must sit on final line 8, NOT intermediate line 7",
            ayah1_7.endMarkerBounds!!.top >= line7.bottom - 0.005f &&
                abs(ayah1_7.endMarkerBounds!!.centerY - line8.centerY) < 0.01f
        )

        // Short centered final line 8 MUST NOT stretch across the blank margins on the far left/right of line 8
        assertTrue("Line 8 should be centered narrower than full inner frame", line8.left > MushafCoordinateRepository.SPECIAL_P12_LEFT + 0.05f)
        assertNull(
            "Tapping blank paper to the far left of centered short line 8 must return null",
            MushafCoordinateTransform.hitTestAyah(page1, MushafCoordinateRepository.SPECIAL_P12_LEFT + 0.01f, line8.centerY)
        )

        // Page 2 (Al-Baqarah 2:1-5): multi-line Ayahs 2:2..2:5 must have end markers strictly on their final line
        val page2 = MushafCoordinateRepository.buildVerifiedBundledPageLayout(2)!!
        assertEquals(5, page2.ayahs.size)
        for (ayahNum in 2..5) {
            val ayah = page2.findAyah(2, ayahNum)!!
            assertTrue("2:$ayahNum spans 2 lines on Page 2", ayah.lineRegions.size == 2)
            val finalLine = ayah.lineRegions.maxBy { it.lineNumber }.bounds
            assertNotNull(ayah.endMarkerBounds)
            assertTrue(
                "2:$ayahNum end marker must sit strictly on its final line",
                abs(ayah.endMarkerBounds!!.centerY - finalLine.centerY) < 0.01f
            )
        }
    }

    @Test
    fun mushafTransform_maintainsExactAccuracyAcrossCompactPhoneTabletZoomPanAndRtlWordOrder() {
        val page604 = MushafCoordinateRepository.buildVerifiedBundledPageLayout(604)!!
        val ayah112_1 = page604.findAyah(112, 1)!!
        // Verify RTL word ordering: word 1 (قُلْ) sits to the RIGHT of word 4 (أَحَدٌ)
        assertTrue(
            "Arabic RTL word 1 must have larger X than last word on the same line",
            ayah112_1.words.first().bounds.centerX > ayah112_1.words.last().bounds.centerX
        )

        val target = ayah112_1.lineRegions.first().bounds
        val configs = listOf(
            // Compact phone, tall phone, tablet/expanded, zoomed & panned
            Triple(360f to 640f, 1.0f, 0f to 0f),
            Triple(412f to 915f, 1.0f, 0f to 0f),
            Triple(1200f to 900f, 1.0f, 0f to 0f),
            Triple(1080f to 1920f, 2.0f, 80f to -120f)
        )
        for ((size, zoom, pan) in configs) {
            val (sx, sy) = MushafCoordinateTransform.normalizedPointToScreen(
                nx = target.centerX,
                ny = target.centerY,
                containerWidth = size.first,
                containerHeight = size.second,
                imageWidth = 1024f,
                imageHeight = 1656f,
                zoomScale = zoom,
                zoomOffsetX = pan.first,
                zoomOffsetY = pan.second
            )
            val norm = MushafCoordinateTransform.screenPointToNormalizedPage(
                tapX = sx,
                tapY = sy,
                containerWidth = size.first,
                containerHeight = size.second,
                imageWidth = 1024f,
                imageHeight = 1656f,
                zoomScale = zoom,
                zoomOffsetX = pan.first,
                zoomOffsetY = pan.second
            )
            assertNotNull(norm)
            val hit = MushafCoordinateTransform.hitTestAyah(page604, norm!!.first, norm.second)
            assertEquals("112:1", hit?.key)
        }
    }

    @Test
    fun singleAudioSession_preservedAcrossReciterSwitchesWithoutDualPlayback() {
        val player = QuranAudioPlayer(context)
        val track1 = AudioTrackItem(
            id = "a1", titleAr = "الفاتحة 1", titleEn = "Fatiha 1",
            subtitleAr = "العفاسي", subtitleEn = "Alafasy",
            audioUrl = "https://everyayah.com/data/Alafasy_128kbps/001001.mp3",
            category = AudioCategory.QURAN_RECITERS,
            surahNumber = 1, ayahNumber = 1, pageNumber = 1,
            reciterId = QuranReciter.ALAFASY.id
        )
        val track2 = track1.copy(
            id = "h1", subtitleAr = "الحصري", subtitleEn = "Husary",
            audioUrl = "https://everyayah.com/data/Husary_128kbps/001001.mp3",
            reciterId = QuranReciter.HUSARY.id
        )
        player.playTrackQueue(listOf(track1), startIndex = 0)
        val s1 = player.currentSessionIdForTest
        player.playTrackQueue(listOf(track2), startIndex = 0)
        val s2 = player.currentSessionIdForTest
        assertTrue("Switching reciter must strictly replace prior session", s2 > s1)
        assertEquals(QuranReciter.HUSARY.id, player.state.value.currentTrack?.reciterId)
        player.stop()
        assertFalse(player.state.value.hasActiveSession)
    }

    // ============================================================================================
    // PART 3: UZBEK LOCALIZATION VERIFICATION (PRESERVING ARABIC RTL & ENGLISH LTR)
    // ============================================================================================

    @Test
    fun uzbekLocalization_supportedAlongsideArabicAndEnglishWithProperRtlAndTranslations() {
        assertEquals(AppLanguage.UZBEK, AppLanguage.fromCode("uz"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))

        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertFalse(AppLanguage.UZBEK.isRtl)

        assertEquals("Bomdod", L10n.prayerName(PrayerName.FAJR, AppLanguage.UZBEK))
        assertEquals("Quyosh chiqishi", L10n.prayerName(PrayerName.SUNRISE, AppLanguage.UZBEK))
        assertEquals("Peshin", L10n.prayerName(PrayerName.DHUHR, AppLanguage.UZBEK))
        assertEquals("Asr", L10n.prayerName(PrayerName.ASR, AppLanguage.UZBEK))
        assertEquals("Shom", L10n.prayerName(PrayerName.MAGHRIB, AppLanguage.UZBEK))
        assertEquals("Xufton", L10n.prayerName(PrayerName.ISHA, AppLanguage.UZBEK))

        assertEquals("Sozlamalar", L10n.pick(AppLanguage.UZBEK, "الإعدادات", "Settings"))
        assertEquals("Qur'oni Karim", L10n.pick(AppLanguage.UZBEK, "القرآن الكريم", "Holy Quran"))
        assertEquals("O'zbekcha", L10n.pick(AppLanguage.UZBEK, "العربية", "English", "O'zbekcha"))
    }
}
