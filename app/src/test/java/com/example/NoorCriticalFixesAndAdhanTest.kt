package com.example

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AdhanCatalog
import com.example.audio.AdhanDuplicateGuard
import com.example.audio.LiveRecitationStatus
import com.example.audio.PerPrayerAdhanConfig
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranLiveRecitationListener
import com.example.audio.QuranRecitationRecorder
import com.example.audio.RecitationRecordingStatus
import com.example.data.local.catalog.EcosystemContentCatalog
import com.example.data.local.db.HalaqahCircleEntity
import com.example.data.local.db.HalaqahStudentEntity
import com.example.data.local.db.LocalAiKnowledgeEntity
import com.example.data.local.db.NoorDatabase
import com.example.data.local.db.StudentRecitationRecordingEntity
import com.example.data.local.prefs.UserPreferences
import com.example.data.repository.EcosystemRepository
import com.example.domain.ai.AiVerificationLevel
import com.example.domain.ai.DocumentContentExtractor
import com.example.domain.ai.DocumentFileFormat
import com.example.domain.ai.NoorLocalAiEngine
import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranVerse
import com.example.domain.quran.AssessmentStrictness
import com.example.domain.quran.QuranRecitationAssessmentEngine
import com.example.domain.quran.RecitationOverallLevel
import com.example.domain.quran.WordEvaluationStatus
import com.example.notifications.NoorAlarmScheduler
import com.example.security.LocalAdminRole
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkInkMuted
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightInk
import com.example.ui.theme.LightInkMuted
import com.example.ui.theme.LightSurface
import com.example.ui.theme.MushafNightBg
import com.example.ui.theme.MushafNightInk
import com.example.ui.theme.MushafSepiaBg
import com.example.ui.theme.MushafSepiaInk
import com.example.ui.theme.NightBlack
import com.example.ui.theme.NightSurface
import com.example.ui.theme.NightSurfaceElevated
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Comprehensive Automated Verification Suite covering:
 * 1. Night Mode & Light Mode WCAG text contrast compliance (Text Colors Only fix)
 * 2. Real Microphone permission guards & Audio Recorder file integrity checks
 * 3. Repaired Arabic STT (preserved full transcript, RecognizerIntent fallback, pause tracking)
 * 4. Word-level Needleman-Wunsch Quran Recitation Assessment (omitted, added, replaced, repeated,
 *    skipped verse, out-of-order verse, phonetic-uncertain separation, strictness tiers)
 * 5. Quran Audio Follow-Along Sync (verse-by-verse queue jump, next/prev, reset without mic)
 * 6. Offline Document Extractor (TXT, JSON, ZIP/DOCX, uncompressed & FlateDecode PDF, scanned PDF warning)
 * 7. Admin-Trainable Local AI (RAG index rebuild, answering newly imported content, colloquial Arabic,
 *    anti-fabrication guardrails, JSON export/import round-trip)
 * 8. 4-Tier RBAC + Room v5 Persistence (Student Recordings, Imported Docs, Admin Settings, Backup/Restore)
 * 9. Complete Automatic Adhan System (Sheikh Nasser Al-Qatami default, offline file header validation,
 *    duplicate-playback guard, per-prayer toggles/overrides, alarm scheduler)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoorCriticalFixesAndAdhanTest {

    // -------------------------------------------------------------------------
    // 1. Night Mode & Light Mode Text Contrast (WCAG AA/AAA)
    // -------------------------------------------------------------------------

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = maxOf(fg.luminance(), bg.luminance()).toDouble()
        val l2 = minOf(fg.luminance(), bg.luminance()).toDouble()
        return (l1 + 0.05) / (l2 + 0.05)
    }

    @Test
    fun nightModeAndLightMode_textColorsAchieveHighReadableContrast() {
        // Night Mode text against preserved dark backgrounds must exceed WCAG AA (4.5:1)
        assertTrue("DarkInk on NightBlack must be >= 10:1", contrastRatio(DarkInk, NightBlack) >= 10.0)
        assertTrue("DarkInk on NightSurface must be >= 10:1", contrastRatio(DarkInk, NightSurface) >= 10.0)
        assertTrue("DarkInk on NightSurfaceElevated must be >= 10:1", contrastRatio(DarkInk, NightSurfaceElevated) >= 10.0)
        assertTrue("DarkInkMuted on NightSurface must be >= 6:1", contrastRatio(DarkInkMuted, NightSurface) >= 6.0)
        assertTrue("DarkInkMuted on NightSurfaceElevated must be >= 5:1", contrastRatio(DarkInkMuted, NightSurfaceElevated) >= 5.0)
        assertTrue("MushafNightInk on MushafNightBg must be >= 12:1", contrastRatio(MushafNightInk, MushafNightBg) >= 12.0)

        // Preserved Light Mode & Sepia Mushaf contrast
        assertTrue("LightInk on LightBackground must be >= 10:1", contrastRatio(LightInk, LightBackground) >= 10.0)
        assertTrue("LightInk on LightSurface must be >= 12:1", contrastRatio(LightInk, LightSurface) >= 12.0)
        assertTrue("LightInkMuted on LightSurface must be >= 5:1", contrastRatio(LightInkMuted, LightSurface) >= 5.0)
        assertTrue("MushafSepiaInk on MushafSepiaBg must be >= 10:1", contrastRatio(MushafSepiaInk, MushafSepiaBg) >= 10.0)
    }

    // -------------------------------------------------------------------------
    // 2. Real Audio Recorder & Microphone Permission Guard
    // -------------------------------------------------------------------------

    @Test
    fun recitationRecorder_enforcesPermissionAndRejectsMissingOrCorruptAudioFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = QuranRecitationRecorder(context)

        // Without RECORD_AUDIO granted in test context -> must fail cleanly with real error
        assertFalse(recorder.hasMicrophonePermission())
        assertFalse(recorder.startRecording("surah_112"))
        assertEquals(RecitationRecordingStatus.ERROR, recorder.recordingState.value.status)
        assertNotNull(recorder.recordingState.value.errorAr)
        assertNotNull(recorder.recordingState.value.errorEn)

        // Playing a nonexistent file -> must fail cleanly
        assertFalse(recorder.playRecording("/nonexistent/path/recitation.m4a"))
        assertNotNull(recorder.playbackState.value.errorAr)

        // Playing a tiny/empty corrupt file (<512 bytes) -> must fail cleanly
        val tinyFile = File.createTempFile("tiny_rec_", ".m4a").apply {
            writeBytes(ByteArray(32))
            deleteOnExit()
        }
        assertFalse(recorder.playRecording(tinyFile.absolutePath))
        assertNotNull(recorder.playbackState.value.errorEn)

        // Stopping while idle returns null without crashing
        assertNull(recorder.stopRecording())
        recorder.release()
    }

    // -------------------------------------------------------------------------
    // 3. Repaired Arabic STT (Permission Guard, Preserved Transcript, Fallback Intent)
    // -------------------------------------------------------------------------

    @Test
    fun liveRecitationListener_preservesTranscriptAfterStopAndBuildsArabicFallbackIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val listener = QuranLiveRecitationListener(context)
        val ikhlas = sampleIkhlasVerses()
        listener.loadSurahContext(112, ikhlas, initialAyah = 1)

        // Permission guard before listening
        listener.startListening()
        assertEquals(LiveRecitationStatus.ERROR, listener.state.value.status)
        assertTrue(listener.state.value.errorMessageAr.isNotBlank())

        // RecognizerIntent fallback includes Arabic locale and offline preference
        listener.setPreferOffline(true)
        val intent: Intent = listener.buildSystemSpeechIntent()
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
        assertEquals("ar-SA", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))

        // Multi-segment recognized text accumulates and is PRESERVED after stopListening(clearTranscript = false)
        listener.injectRecognizedText("قل هو الله احد", isFinal = true)
        listener.injectRecognizedText("الله الصمد", isFinal = true)
        assertEquals("قل هو الله احد الله الصمد", listener.state.value.accumulatedTranscript)
        assertEquals(2, listener.state.value.trackedAyah)

        listener.stopListening(clearTranscript = false)
        assertEquals(LiveRecitationStatus.IDLE, listener.state.value.status)
        assertEquals("قل هو الله احد الله الصمد", listener.state.value.effectiveTranscript)

        // Explicit clearTranscript resets transcript cleanly
        listener.clearTranscript()
        assertEquals("", listener.state.value.effectiveTranscript)
    }

    // -------------------------------------------------------------------------
    // 4. Word-Level Quran Recitation Assessment Engine
    // -------------------------------------------------------------------------

    private fun sampleIkhlasVerses(): List<QuranVerse> = listOf(
        QuranVerse(112, 1, 6222, "قُلْ هُوَ ٱللَّهُ أَحَدٌ", "", "", pageNumber = 604, juzNumber = 30),
        QuranVerse(112, 2, 6223, "ٱللَّهُ ٱلصَّمَدُ", "", "", pageNumber = 604, juzNumber = 30),
        QuranVerse(112, 3, 6224, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "", "", pageNumber = 604, juzNumber = 30),
        QuranVerse(112, 4, 6225, "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ", "", "", pageNumber = 604, juzNumber = 30)
    )

    @Test
    fun recitationAssessment_scoresExactRecitationAsExcellent100Percent() {
        val report = QuranRecitationAssessmentEngine.evaluate(
            verses = sampleIkhlasVerses(),
            spokenTranscript = "قل هو الله احد الله الصمد لم يلد ولم يولد ولم يكن له كفوا احد"
        )
        assertEquals(100, report.accuracyPercent)
        assertEquals(RecitationOverallLevel.EXCELLENT, report.overallLevel)
        assertTrue(report.omittedWords.isEmpty())
        assertTrue(report.addedWords.isEmpty())
        assertTrue(report.replacedWords.isEmpty())
        assertTrue(report.skippedVerses.isEmpty())
        assertTrue(report.outOfOrderVerses.isEmpty())
    }

    @Test
    fun recitationAssessment_detectsOmittedAddedReplacedRepeatedSkippedAndOutOfOrder() {
        val verses = sampleIkhlasVerses()

        // Omitted word ("الصمد")
        val omitted = QuranRecitationAssessmentEngine.evaluate(
            verses = verses.take(2),
            spokenTranscript = "قل هو الله احد الله"
        )
        assertTrue("Expected omitted word", omitted.omittedWords.isNotEmpty())

        // Added word ("العظيم")
        val added = QuranRecitationAssessmentEngine.evaluate(
            verses = verses.take(1),
            spokenTranscript = "قل هو الله العظيم احد"
        )
        assertTrue("Expected added word", added.addedWords.isNotEmpty())

        // Replaced word ("احد" -> "كريم")
        val replaced = QuranRecitationAssessmentEngine.evaluate(
            verses = verses.take(1),
            spokenTranscript = "قل هو الله كريم"
        )
        assertTrue("Expected replaced word", replaced.replacedWords.isNotEmpty())

        // Repeated word ("الله الله")
        val repeated = QuranRecitationAssessmentEngine.evaluate(
            verses = verses.take(1),
            spokenTranscript = "قل هو الله الله احد"
        )
        assertTrue("Expected repeated word", repeated.repeatedWords.isNotEmpty())

        // Skipped verse (skipping Ayah 2 completely)
        val skipped = QuranRecitationAssessmentEngine.evaluate(
            verses = verses,
            spokenTranscript = "قل هو الله احد لم يلد ولم يولد ولم يكن له كفوا احد"
        )
        assertTrue("Expected Ayah 2 in skippedVerses", 2 in skipped.skippedVerses)

        // Out-of-order verse (reciting Ayah 3 before Ayah 2)
        val outOfOrder = QuranRecitationAssessmentEngine.evaluate(
            verses = verses.take(3),
            spokenTranscript = "قل هو الله احد لم يلد ولم يولد الله الصمد"
        )
        assertTrue("Expected out-of-order verse detected", outOfOrder.outOfOrderVerses.isNotEmpty())
    }

    @Test
    fun recitationAssessment_separatesPhoneticUncertainFromConfirmedMistakesAndHandlesEmpty() {
        // Minor phonetic ASR distortion ("المستقيم" -> "المستقين" has similarity ~0.75-0.85)
        val fatiha6 = listOf(
            QuranVerse(1, 6, 6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "", "", pageNumber = 1, juzNumber = 1)
        )
        val report = QuranRecitationAssessmentEngine.evaluate(
            verses = fatiha6,
            spokenTranscript = "اهدنا الصراط المستقين",
            longPauseCount = 2,
            longestPauseMs = 4200L,
            strictness = AssessmentStrictness.STRICT
        )
        assertTrue(
            "Expected PHONETIC_UNCERTAIN bucket for near-match under STRICT mode",
            report.wordEvaluations.any { it.status == WordEvaluationStatus.PHONETIC_UNCERTAIN }
        )
        assertTrue(report.recognitionUncertainWords.isNotEmpty())
        assertEquals(2, report.longPauseCount)
        assertTrue(report.summaryAr.contains("توقفات طويلة"))

        // Empty transcript -> honest RECOGNITION_UNCLEAR
        val empty = QuranRecitationAssessmentEngine.evaluate(fatiha6, "")
        assertEquals(0, empty.accuracyPercent)
        assertEquals(RecitationOverallLevel.RECOGNITION_UNCLEAR, empty.overallLevel)
    }

    // -------------------------------------------------------------------------
    // 5. Quran Audio Follow-Along Sync (Verse-by-Verse Queue Without Mic)
    // -------------------------------------------------------------------------

    @Test
    fun quranAudioPlayer_verseQueueSyncSupportsJumpNextPreviousAndReset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = QuranAudioPlayer(context)
        val queue = (1..4).map { ayah ->
            AudioTrackItem(
                id = "ayah_112_$ayah",
                titleAr = "سورة الإخلاص • آية $ayah",
                titleEn = "Al-Ikhlas • Ayah $ayah",
                subtitleAr = "الشيخ ياسر الدوسري",
                subtitleEn = "Yasser Al-Dosari",
                category = AudioCategory.QURAN_RECITERS,
                audioUrl = "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/11200$ayah.mp3",
                surahNumber = 112,
                ayahNumber = ayah
            )
        }

        player.playTrackQueue(queue, startIndex = 0)
        assertEquals(4, player.currentQueueSnapshot().size)
        assertEquals(112, player.state.value.playingSurah)
        assertEquals(1, player.state.value.playingAyah)

        // Jump directly to Ayah 3 (simulating user tap on verse / scrubber)
        assertTrue(player.jumpToAyahInQueue(112, 3))
        assertEquals(3, player.state.value.playingAyah)

        // Next -> Ayah 4
        player.playNext()
        assertEquals(4, player.state.value.playingAyah)

        // Previous -> Ayah 3
        player.playPrevious()
        assertEquals(3, player.state.value.playingAyah)

        // Jump to invalid ayah returns false
        assertFalse(player.jumpToAyahInQueue(112, 99))

        // Stop resets active ayah cleanly
        player.stop()
        assertNull(player.state.value.playingAyah)
        assertTrue(player.currentQueueSnapshot().isEmpty())
    }

    // -------------------------------------------------------------------------
    // 6. Document Extractor (DOCX, PDF uncompressed & FlateDecode, Scanned PDF, Chunking)
    // -------------------------------------------------------------------------

    private fun buildMinimalDocxBytes(paragraphText: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>$paragraphText</w:t></w:r></w:p>
                  </w:body>
                </w:document>
            """.trimIndent()
            zos.write(xml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun buildMinimalCompressedPdfBytes(asciiText: String): ByteArray {
        val streamContent = "BT /F1 12 Tf 72 720 Td ($asciiText) Tj ET".toByteArray(StandardCharsets.ISO_8859_1)
        val deflater = Deflater()
        deflater.setInput(streamContent)
        deflater.finish()
        val compOut = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            compOut.write(buf, 0, n)
        }
        deflater.end()
        val compressed = compOut.toByteArray()

        val pdf = ByteArrayOutputStream()
        pdf.write("%PDF-1.4\n1 0 obj\n<< /Filter /FlateDecode /Length ${compressed.size} >>\nstream\n".toByteArray(StandardCharsets.ISO_8859_1))
        pdf.write(compressed)
        pdf.write("\nendstream\nendobj\n%%EOF".toByteArray(StandardCharsets.ISO_8859_1))
        return pdf.toByteArray()
    }

    @Test
    fun documentExtractor_extractsDocxCompressedPdfWarnsOnScannedPdfAndChunks() {
        // Real DOCX ZIP container extraction
        val docxBytes = buildMinimalDocxBytes("أحكام التجويد: الإدغام بغنة يكون عند حروف ينمو بمقدار حركتين.")
        val docxRes = DocumentContentExtractor.extract("tajweed_lesson.docx", docxBytes)
        assertEquals(DocumentFileFormat.DOCX, docxRes.format)
        assertEquals("AR", docxRes.detectedLanguage)
        assertEquals("TAJWEED", docxRes.detectedCategory)
        assertTrue(docxRes.extractedText.contains("الإدغام بغنة"))
        assertTrue(docxRes.chunks.isNotEmpty())
        assertNull(docxRes.warningAr)

        // Real FlateDecode compressed PDF text extraction
        val pdfBytes = buildMinimalCompressedPdfBytes("Seerah lesson: The Prophet migrated from Makkah to Madinah in the Hijrah year.")
        val pdfRes = DocumentContentExtractor.extract("seerah_notes.pdf", pdfBytes)
        assertEquals(DocumentFileFormat.PDF, pdfRes.format)
        assertEquals("EN", pdfRes.detectedLanguage)
        assertEquals("SEERAH", pdfRes.detectedCategory)
        assertTrue(pdfRes.extractedText.contains("migrated from Makkah to Madinah"))
        assertNull(pdfRes.warningEn)

        // Scanned image-only PDF with no text -> honest warning
        val scannedPdf = "%PDF-1.4\n1 0 obj\n<< /Subtype /Image >>\nstream\n\u0000\u0001\u0002\nendstream\nendobj\n%%EOF".toByteArray(StandardCharsets.ISO_8859_1)
        val scannedRes = DocumentContentExtractor.extract("scanned_book.pdf", scannedPdf)
        assertEquals(DocumentFileFormat.PDF, scannedRes.format)
        assertNotNull("Scanned PDF must emit clear warning", scannedRes.warningAr)
        assertNotNull(scannedRes.warningEn)
    }

    // -------------------------------------------------------------------------
    // 7. Trainable NOOR AI (RAG Index, Newly Imported Content, Colloquial Arabic, JSON Export)
    // -------------------------------------------------------------------------

    @Test
    fun trainableNoorAi_answersNewlyImportedDocxContentAndUnderstandsColloquialArabic() {
        val baseKb = EcosystemContentCatalog.seedAiKnowledge()
        val books = EcosystemContentCatalog.seedLibraryBooks()

        // 1. Import brand-new custom lesson via DOCX
        val docxBytes = buildMinimalDocxBytes("فضل قيام الليل في السحر: صلاة التهجد في الثلث الأخير من الليل من أعظم القربات وفيها ينزل ربنا إلى السماء الدنيا.")
        val extracted = DocumentContentExtractor.extract("qiyam_lesson.docx", docxBytes, defaultCategory = "WORSHIP")
        val batch = NoorLocalAiEngine.parseExtractedDocumentToKnowledge(
            extracted = extracted,
            existingHashes = baseKb.map { it.contentHash }.toSet(),
            autoApprove = true
        )
        assertEquals(1, batch.items.size)
        val combinedKb = baseKb + batch.items

        // 2. Rebuild index & verify stats
        val stats = NoorLocalAiEngine.rebuildSearchIndex(combinedKb, books)
        assertEquals(combinedKb.size, stats.totalEntries)
        assertTrue(stats.uniqueTokenCount > 50)

        // 3. Query newly imported knowledge -> must retrieve & cite the imported DOCX!
        val ans = NoorLocalAiEngine.answerQuestion(
            query = "ما فضل صلاة التهجد وقيام الليل في الثلث الأخير؟",
            approvedKnowledge = combinedKb,
            approvedBooks = books
        )
        assertTrue(ans.answerAr.contains("الثلث الأخير"))
        assertTrue(ans.sources.any { it.reference.contains("qiyam_lesson.docx") })
        assertTrue("Must honestly disclose Local RAG", ans.disclaimerAr.contains("Local RAG"))

        // 4. Colloquial Levantine/Syrian Arabic question -> still retrieves verified Hifz guidance!
        val colloquial = NoorLocalAiEngine.answerQuestion(
            query = "شلون احفظ القران ومشان ما بنسى؟",
            approvedKnowledge = combinedKb,
            approvedBooks = books
        )
        assertTrue(colloquial.verificationLevel != AiVerificationLevel.UNCERTAIN)
        assertTrue(colloquial.sources.isNotEmpty())

        // 5. Export KB to JSON and re-import cleanly
        val exportedJson = NoorLocalAiEngine.exportKnowledgeBaseJson(batch.items)
        assertTrue(exportedJson.contains("noor_local_ai_kb_v2"))
        val reimported = NoorLocalAiEngine.parseImportedKnowledgeText(
            rawContent = exportedJson,
            fileName = "export.json",
            defaultCategory = "WORSHIP",
            existingHashes = emptySet()
        )
        assertEquals(1, reimported.items.size)
    }

    // -------------------------------------------------------------------------
    // 8. Room v5 Persistence, Recordings, Imported Docs, Admin Settings & Backup/Restore
    // -------------------------------------------------------------------------

    @Test
    fun roomV5EcosystemRepository_persistsRecordingsDocsAdminSettingsAndBackupRoundTrip() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NoorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = EcosystemRepository(db.ecosystemDao(), db.noorDao(), context)
        repo.ensureSeeded()

        // Admin config key/value persistence
        repo.setAdminConfig("assessment_strictness", "STRICT", "admin")
        assertEquals("STRICT", repo.getAllAdminConfigOnce()["assessment_strictness"])

        // Student recitation recording persistence & teacher review
        val recId = repo.saveRecitationRecording(
            StudentRecitationRecordingEntity(
                studentName = "أحمد الشامي",
                surahNumber = 112,
                fromAyah = 1,
                toAyah = 4,
                testMode = "FULL_RECITATION",
                filePath = "/tmp/sample_112.m4a",
                durationMs = 14_500L,
                recognizedTranscript = "قل هو الله احد الله الصمد",
                accuracyPercent = 94,
                suspectedMistakesSummary = "لا توجد أخطاء",
                uncertainCount = 0
            )
        )
        assertTrue(recId > 0L)
        repo.reviewRecitationRecording(recId, "APPROVED", "تلاوة متقنة بارك الله فيك")
        val recs = repo.recitationRecordings.first()
        assertEquals(1, recs.size)
        assertEquals("APPROVED", recs.first().teacherReviewStatus)

        // Document byte import (DOCX) updates KB and imported_ai_documents table
        val docxBytes = buildMinimalDocxBytes("آداب طالب العلم: الإخلاص لله تعالى والعمل بالعلم والتواضع للمعلم.")
        val importRes = repo.importAiDocumentBytes(
            fileName = "adab_talib.docx",
            bytes = docxBytes,
            category = "GENERAL",
            autoApprove = true,
            actorName = "admin"
        )
        assertEquals(1, importRes.items.size)
        assertEquals(1, repo.importedAiDocuments.first().size)

        // Reindex returns real AiIndexStats
        val indexStats = repo.reindexLocalKnowledge()
        assertTrue(indexStats.approvedEntries >= 10)
        assertTrue(indexStats.uniqueTokenCount > 40)

        // Halaqat report text generation
        val circleId = repo.saveCircle(
            HalaqahCircleEntity(
                name = "حلقة النور",
                description = "حفظ جزء عم",
                level = "مبتدئ",
                circleType = "MEMORIZATION",
                scheduleText = "بعد العصر"
            )
        )
        repo.saveStudent(
            HalaqahStudentEntity(
                fullName = "عمر الحلبي",
                age = 12,
                guardianContact = "0999000000",
                circleId = circleId,
                memorizedJuz = 2,
                memorizedPages = 40
            )
        )
        val reportTxt = repo.buildHalaqatReportText()
        assertTrue(reportTxt.contains("حلقة النور"))
        assertTrue(reportTxt.contains("عمر الحلبي"))

        // Backup & restore roundtrip preserves circles and students
        val backupJson = repo.buildFullEcosystemBackupJson("admin")
        assertTrue(backupJson.contains("\"schemaVersion\":4") || backupJson.contains("\"schemaVersion\": 4"))
        val restoreSummary = repo.restoreFromEcosystemBackupJson(backupJson, "admin")
        assertTrue(restoreSummary.circlesImported >= 1)
        assertTrue(restoreSummary.studentsImported >= 1)
        assertTrue(repo.circles.first().any { it.name == "حلقة النور" })

        // Verify 4-tier RBAC role permissions
        assertTrue(LocalAdminRole.SUPER_ADMIN.canManageSettings())
        assertFalse(LocalAdminRole.ADMIN.canManageSettings())
        assertTrue(LocalAdminRole.TEACHER.canViewRecordings())
        assertFalse(LocalAdminRole.STUDENT.canViewRecordings())

        db.close()
    }

    // -------------------------------------------------------------------------
    // 9. Complete Automatic Adhan System (Sheikh Nasser Al-Qatami Default)
    // -------------------------------------------------------------------------

    @Test
    fun automaticAdhanSystem_defaultsToSheikhNasserAlQatamiValidatesHeadersAndGuardsDuplicates() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Default voice is Sheikh Nasser Al-Qatami
        assertEquals("adhan_qatami", AdhanCatalog.DEFAULT_VOICE_ID)
        val prefs = UserPreferences()
        assertEquals("adhan_qatami", prefs.adhanConfig.defaultVoiceId)
        assertTrue(prefs.adhanSoundEnabled)
        assertFalse(prefs.adhanNotificationOnly)
        assertEquals(1.0f, prefs.adhanVolume)
        assertTrue(prefs.notifyFajr)
        assertTrue(prefs.notifyDhuhr)
        assertTrue(prefs.notifyAsr)
        assertTrue(prefs.notifyMaghrib)
        assertTrue(prefs.notifyIsha)

        // Per-prayer override resolves custom Fajr voice and falls back to Al-Qatami for others
        val perPrayer = PerPrayerAdhanConfig(
            defaultVoiceId = AdhanCatalog.DEFAULT_VOICE_ID,
            fajrVoiceId = "adhan_makkah"
        )
        assertEquals("adhan_makkah", perPrayer.voiceIdForPrayer(PrayerName.FAJR))
        assertEquals("adhan_qatami", perPrayer.voiceIdForPrayer(PrayerName.DHUHR))

        // Header validation accepts MP3 (ID3 & MPEG sync), M4A (ftyp), WAV (RIFF), rejects HTML/JSON
        assertTrue(AdhanCatalog.isVerifiedAudioHeader(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0, 0, 0, 0, 0, 0, 0)))
        assertTrue(AdhanCatalog.isVerifiedAudioHeader(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0)))
        assertTrue(AdhanCatalog.isVerifiedAudioHeader(byteArrayOf(0, 0, 0, 20, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), 'M'.code.toByte(), '4'.code.toByte(), 'A'.code.toByte(), ' '.code.toByte())))
        assertTrue(AdhanCatalog.isVerifiedAudioHeader(byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(), 0, 0, 0, 0, 'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte())))
        assertFalse(AdhanCatalog.isVerifiedAudioHeader("<!DOCTYPE html><html>404</html>".toByteArray()))
        assertFalse(AdhanCatalog.isVerifiedAudioHeader("{\"error\":\"not found\"}".toByteArray()))

        // Offline file verification accepts valid >=16KB MP3 and rejects tiny or corrupt file
        val validMp3 = File.createTempFile("qatami_valid_", ".mp3").apply {
            val bytes = ByteArray(20_000)
            bytes[0] = 'I'.code.toByte()
            bytes[1] = 'D'.code.toByte()
            bytes[2] = '3'.code.toByte()
            writeBytes(bytes)
            deleteOnExit()
        }
        assertNotNull(AdhanCatalog.verifyAudioFile(validMp3))

        val corruptMp3 = File.createTempFile("qatami_bad_", ".mp3").apply {
            writeBytes("<!DOCTYPE html>not an audio file".repeat(1000).toByteArray())
            deleteOnExit()
        }
        assertNull(AdhanCatalog.verifyAudioFile(corruptMp3))

        // Duplicate-playback guard prevents duplicate firing within 20 minutes
        AdhanDuplicateGuard.clearAll(context)
        val t0 = 1_750_000_000_000L
        assertTrue(AdhanDuplicateGuard.shouldAllowPlayback(context, PrayerName.DHUHR, t0))
        AdhanDuplicateGuard.markPlayed(context, PrayerName.DHUHR, t0)
        assertFalse(AdhanDuplicateGuard.shouldAllowPlayback(context, PrayerName.DHUHR, t0 + 5 * 60_000L))
        // Different prayer is allowed immediately
        assertTrue(AdhanDuplicateGuard.shouldAllowPlayback(context, PrayerName.ASR, t0 + 5 * 60_000L))
        // Test alarm bypasses dedup guard
        assertTrue(AdhanDuplicateGuard.shouldAllowPlayback(context, PrayerName.DHUHR, t0 + 1000L, isTestAlarm = true))
        // After > 20 minutes, allowed again
        assertTrue(AdhanDuplicateGuard.shouldAllowPlayback(context, PrayerName.DHUHR, t0 + 25 * 60_000L))

        // Rescheduling all prayer alarms executes cleanly without errors
        NoorAlarmScheduler.rescheduleAll(context, prefs)
    }
}
