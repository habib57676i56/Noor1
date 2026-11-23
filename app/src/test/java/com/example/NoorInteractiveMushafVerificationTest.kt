package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.media.AudioManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranLiveRecitationListener
import com.example.audio.QuranRecitationRecorder
import com.example.audio.RecitationRecordingStatus
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.quran.CoordinatePrecision
import com.example.data.quran.LinguisticVerificationStatus
import com.example.data.quran.MushafCoordinateRepository
import com.example.data.quran.MushafCoordinateTransform
import com.example.data.quran.QuranLinguisticRepository
import com.example.data.quran.QuranWordTimingRepository
import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioRepeatMode
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.quran.QuranRecitationAssessmentEngine
import com.example.domain.quran.RecitationOverallLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.abs

/**
 * Comprehensive verification suite for NOOR's Interactive Madinah Mushaf Image System:
 * 1. Accurate Ayah & Word Coordinate Hit-Testing (Pages 1, 2, 597 Sajdah, 604 multi-Surah, zoom/pan/resize, SQLite DB).
 * 2. Contextual Menu non-overlapping placement (auto-flip above/below).
 * 3. Audio playback sync, page-turn boundary updates, repeat-one, repeat-range looping, pause/resume, audio focus interruptions.
 * 4. Verified Word-by-Word millisecond timing parsing & strict no-fake-timer honesty.
 * 5. Structured I'rab & Sarf lookup, immutable Uthmani text separation, honest UNAVAILABLE fallback, and offline JSON pack import.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorInteractiveMushafVerificationTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // ============================================================================================
    // 1. MUSHAF INTERACTION & COORDINATE HIT-TESTING TESTS
    // ============================================================================================

    @Test
    fun page1_specialOpeningLayout_hitsFirstMiddleAndLastAyahAccurately() = runBlocking {
        val repo = MushafCoordinateRepository(context)
        val layout = repo.getPageLayout(pageNumber = 1)

        assertTrue("Page 1 must be marked as special opening page", layout.isSpecialOpeningPage)
        assertEquals(7, layout.ayahs.size)

        // First Ayah (1:1)
        val first = layout.findAyah(1, 1)!!
        val hitFirst = MushafCoordinateTransform.hitTestAyah(
            layout,
            first.lineRegions.first().bounds.centerX,
            first.lineRegions.first().bounds.centerY
        )
        assertEquals("1:1", hitFirst?.key)

        // Middle Ayah (1:4)
        val middle = layout.findAyah(1, 4)!!
        val hitMiddle = MushafCoordinateTransform.hitTestAyah(
            layout,
            middle.lineRegions.first().bounds.centerX,
            middle.lineRegions.first().bounds.centerY
        )
        assertEquals("1:4", hitMiddle?.key)

        // Last Ayah (1:7 — spans multiple lines)
        val last = layout.findAyah(1, 7)!!
        assertTrue("Al-Fatiha 1:7 spans multiple lines on Page 1", last.lineRegions.size >= 2)
        for (seg in last.lineRegions) {
            val hitSeg = MushafCoordinateTransform.hitTestAyah(layout, seg.bounds.centerX, seg.bounds.centerY)
            assertEquals("Every line segment of 1:7 must resolve to 1:7", "1:7", hitSeg?.key)
        }

        // Outer ornamental margin must NOT accidentally select an Ayah
        val marginHit = MushafCoordinateTransform.hitTestAyah(layout, 0.03f, 0.05f)
        assertNull("Ornamental page margin must return null", marginHit)
    }

    @Test
    fun page2_alBaqarahSpecialOpeningLayout_hitsFirstMiddleAndLastAyahAccurately() = runBlocking {
        val repo = MushafCoordinateRepository(context)
        val layout = repo.getPageLayout(pageNumber = 2)

        assertTrue(layout.isSpecialOpeningPage)
        assertEquals(5, layout.ayahs.size)

        val hit1 = layout.findAyah(2, 1)!!.let {
            MushafCoordinateTransform.hitTestAyah(layout, it.lineRegions.first().bounds.centerX, it.lineRegions.first().bounds.centerY)
        }
        val hit3 = layout.findAyah(2, 3)!!.let {
            MushafCoordinateTransform.hitTestAyah(layout, it.lineRegions.first().bounds.centerX, it.lineRegions.first().bounds.centerY)
        }
        val hit5 = layout.findAyah(2, 5)!!.let {
            MushafCoordinateTransform.hitTestAyah(layout, it.lineRegions.last().bounds.centerX, it.lineRegions.last().bounds.centerY)
        }

        assertEquals("2:1", hit1?.key)
        assertEquals("2:3", hit3?.key)
        assertEquals("2:5", hit5?.key)
    }

    @Test
    fun page604_multiSurahPage_distinguishesSurahs112_113_114AndIgnoresSurahHeaders() = runBlocking {
        val repo = MushafCoordinateRepository(context)
        val layout = repo.getPageLayout(pageNumber = 604)

        // Page 604 contains Al-Ikhlas (4) + Al-Falaq (5) + An-Nas (6) = 15 Ayahs
        assertEquals(15, layout.ayahs.size)
        assertEquals(setOf(112, 113, 114), layout.ayahs.map { it.surahNumber }.toSet())

        // Every single Ayah on Page 604 must hit ONLY itself (never a neighbor!)
        for (ayah in layout.ayahs) {
            for (seg in ayah.lineRegions) {
                val hit = MushafCoordinateTransform.hitTestAyah(layout, seg.bounds.centerX, seg.bounds.centerY)
                assertEquals(
                    "Segment on line ${seg.lineNumber} must hit exact Ayah ${ayah.key}",
                    ayah.key,
                    hit?.key
                )
            }
        }

        // Touching Surah header / Bismillah lines (e.g. Line 1 top header of Al-Ikhlas, Line 5 header of Al-Falaq)
        // must NOT accidentally select an Ayah!
        val topHeaderY = 0.055f + 0.5f * ((0.945f - 0.055f) / 15f) // Center of line 1
        assertNull("Surah header banner on Line 1 must return null", MushafCoordinateTransform.hitTestAyah(layout, 0.50f, topHeaderY))
    }

    @Test
    fun sajdahVerses_page597AndCanonical15SajdahSet_verified() = runBlocking {
        assertEquals("All 15 Quranic Sajdah verses must be registered", 15, MushafCoordinateRepository.SAJDAH_VERSES.size)
        assertTrue(MushafCoordinateRepository.SAJDAH_VERSES.contains(96 to 19))
        assertTrue(MushafCoordinateRepository.SAJDAH_VERSES.contains(32 to 15))
        assertTrue(MushafCoordinateRepository.SAJDAH_VERSES.contains(53 to 62))

        val repo = MushafCoordinateRepository(context)
        val layout597 = repo.getPageLayout(pageNumber = 597)
        val sajdahAyah = layout597.findAyah(96, 19)
        assertNotNull("96:19 must exist on Page 597", sajdahAyah)
        assertTrue("96:19 must be flagged with hasSajdah=true", sajdahAyah!!.hasSajdah)

        val seg = sajdahAyah.lineRegions.first()
        val hit = MushafCoordinateTransform.hitTestAyah(layout597, seg.bounds.centerX, seg.bounds.centerY)
        assertEquals("96:19", hit?.key)
    }

    @Test
    fun zoomPanAndScreenResize_coordinateTransformRoundTripAndHitTestingExact() = runBlocking {
        val repo = MushafCoordinateRepository(context)
        val layout = repo.getPageLayout(pageNumber = 604)
        val targetAyah = layout.findAyah(113, 3)!! // Middle Surah Al-Falaq Ayah 3
        val targetX = targetAyah.lineRegions.first().bounds.centerX
        val targetY = targetAyah.lineRegions.first().bounds.centerY

        // Test across multiple screen sizes & zoom/pan combinations:
        // (narrow phone, standard phone, wide tablet, zoomed + panned)
        val viewports = listOf(
            Triple(360f to 780f, 1.0f, 0f to 0f),
            Triple(1080f to 1600f, 2.0f, 120f to -85f),
            Triple(900f to 1100f, 1.65f, -95f to 110f)
        )

        for ((dims, scale, offset) in viewports) {
            val (containerW, containerH) = dims
            val (offX, offY) = offset
            val (screenX, screenY) = MushafCoordinateTransform.normalizedPointToScreen(
                nx = targetX,
                ny = targetY,
                containerWidth = containerW,
                containerHeight = containerH,
                imageWidth = layout.imageWidth.toFloat(),
                imageHeight = layout.imageHeight.toFloat(),
                zoomScale = scale,
                zoomOffsetX = offX,
                zoomOffsetY = offY
            )
            val recovered = MushafCoordinateTransform.screenPointToNormalizedPage(
                tapX = screenX,
                tapY = screenY,
                containerWidth = containerW,
                containerHeight = containerH,
                imageWidth = layout.imageWidth.toFloat(),
                imageHeight = layout.imageHeight.toFloat(),
                zoomScale = scale,
                zoomOffsetX = offX,
                zoomOffsetY = offY
            )
            assertNotNull("Inverse transform must succeed for viewport $dims scale=$scale", recovered)
            assertTrue(abs(recovered!!.first - targetX) < 0.0005f)
            assertTrue(abs(recovered.second - targetY) < 0.0005f)

            val hit = MushafCoordinateTransform.hitTestAyah(layout, recovered.first, recovered.second)
            assertEquals("Hit after zoom=$scale pan=$offset must stay locked to 113:3", "113:3", hit?.key)
        }
    }

    @Test
    fun contextualMenuPlacement_autoFlipsAboveOrBelowSoAyahIsNeverCovered() = runBlocking {
        val repo = MushafCoordinateRepository(context)
        val layout604 = repo.getPageLayout(pageNumber = 604)

        // Top Ayah on page (112:1 on line 3, top ~0.17 < 0.24) -> MUST place BELOW
        val topAyah = layout604.findAyah(112, 1)!!
        val topPlacement = MushafCoordinateTransform.computeContextualMenuPlacement(topAyah)
        assertFalse("Top-of-page Ayah must place contextual menu BELOW", topPlacement.placeAboveAyah)

        // Bottom Ayah on page (114:6 on line 15, top ~0.88 >= 0.24) -> MUST place ABOVE
        val bottomAyah = layout604.findAyah(114, 6)!!
        val bottomPlacement = MushafCoordinateTransform.computeContextualMenuPlacement(bottomAyah)
        assertTrue("Bottom-of-page Ayah must place contextual menu ABOVE", bottomPlacement.placeAboveAyah)
    }

    @Test
    fun exactGlyphSqliteDatabase_importAndQueryReturnsExactWordAndLineBounds() = runBlocking {
        val tempDbFile = File(context.cacheDir, "test_ayahinfo_1024.db")
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
        // Insert exact 1024x1656 glyph coordinates for Page 3 (Al-Baqarah 2:6 & 2:7)
        db.execSQL("INSERT INTO glyphs VALUES (1, 3, 1, 2, 6, 1, 700, 920, 120, 210)")
        db.execSQL("INSERT INTO glyphs VALUES (2, 3, 1, 2, 6, 2, 420, 690, 120, 210)")
        db.execSQL("INSERT INTO glyphs VALUES (3, 3, 2, 2, 7, 1, 680, 920, 230, 320)")
        // Pad table slightly so file exceeds 100KB minimum size validation (real ayahinfo_1024.db is ~15MB)
        db.beginTransaction()
        try {
            val stmt = db.compileStatement("INSERT INTO glyphs VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            for (i in 4..4500) {
                stmt.clearBindings()
                stmt.bindLong(1, i.toLong())
                stmt.bindLong(2, 10L)
                stmt.bindLong(3, ((i % 15) + 1).toLong())
                stmt.bindLong(4, 2L)
                stmt.bindLong(5, (i % 50 + 1).toLong())
                stmt.bindLong(6, (i % 10 + 1).toLong())
                stmt.bindLong(7, 100L)
                stmt.bindLong(8, 200L)
                stmt.bindLong(9, 100L)
                stmt.bindLong(10, 180L)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        db.close()

        val repo = MushafCoordinateRepository(context)
        val imported = repo.importGlyphDatabaseFromUri(Uri.fromFile(tempDbFile))
        assertTrue("Valid SQLite glyph DB must import cleanly", imported)
        assertTrue(repo.hasExactGlyphDb())

        val layout3 = repo.getPageLayout(pageNumber = 3)
        assertEquals(CoordinatePrecision.EXACT_GLYPH_DB, layout3.precision)
        assertEquals(2, layout3.ayahs.size)

        val ayah2_6 = layout3.findAyah(2, 6)!!
        assertEquals(2, ayah2_6.words.size)

        // Hit-test exact word 2 of 2:6 (x=555/1024, y=165/1656)
        val wordHit = MushafCoordinateTransform.hitTestWord(layout3, 555f / 1024f, 165f / 1656f)
        assertNotNull(wordHit)
        assertEquals(2, wordHit!!.surahNumber)
        assertEquals(6, wordHit.ayahNumber)
        assertEquals(2, wordHit.wordPosition)
    }

    // ============================================================================================
    // 2. AUDIO PLAYBACK, PAGE TRANSITION, REPEAT RANGE & FOCUS INTERRUPTION TESTS
    // ============================================================================================

    @Test
    fun quranAudioPlayer_continuousTransitionPageFlipRepeatRangeAndInterruptions() {
        val player = QuranAudioPlayer(context)

        // Build a 4-Ayah queue spanning Page 1 (1:6, 1:7) -> Page 2 (2:1, 2:2)
        val queue = listOf(
            AudioTrackItem("t1", "الفاتحة 6", "Fatiha 6", "العفاسي", "Alafasy", "https://everyayah.com/data/Alafasy_128kbps/001006.mp3", AudioCategory.QURAN_RECITERS, 1, 6, pageNumber = 1),
            AudioTrackItem("t2", "الفاتحة 7", "Fatiha 7", "العفاسي", "Alafasy", "https://everyayah.com/data/Alafasy_128kbps/001007.mp3", AudioCategory.QURAN_RECITERS, 1, 7, pageNumber = 1),
            AudioTrackItem("t3", "البقرة 1", "Baqarah 1", "العفاسي", "Alafasy", "https://everyayah.com/data/Alafasy_128kbps/002001.mp3", AudioCategory.QURAN_RECITERS, 2, 1, pageNumber = 2),
            AudioTrackItem("t4", "البقرة 2", "Baqarah 2", "العفاسي", "Alafasy", "https://everyayah.com/data/Alafasy_128kbps/002002.mp3", AudioCategory.QURAN_RECITERS, 2, 2, pageNumber = 2)
        )

        // A. Start from selected Ayah (1:6 on Page 1)
        player.playTrackQueue(queue, startIndex = 0, initialSpeed = 1.0f)
        assertEquals(1, player.state.value.playingSurah)
        assertEquals(6, player.state.value.playingAyah)
        assertEquals(1, player.state.value.playingPageNumber)

        // B. Transition to next Ayah (1:7 on Page 1) -> replaces previous Ayah highlight target
        player.handleTrackCompletion()
        assertEquals(1, player.state.value.playingSurah)
        assertEquals(7, player.state.value.playingAyah)
        assertEquals(1, player.state.value.playingPageNumber)

        // C. Transition across Page & Surah boundary -> automatically advances to Surah 2 Ayah 1 on Page 2!
        player.handleTrackCompletion()
        assertEquals(2, player.state.value.playingSurah)
        assertEquals(1, player.state.value.playingAyah)
        assertEquals(2, player.state.value.playingPageNumber)

        // D. Pause, Resume, Speed & Audio Focus Interruption handling
        player.simulatePreparedPlayingForTest(4000L)
        assertTrue(player.state.value.isPlaying)

        player.setPlaybackSpeed(1.25f)
        assertEquals(1.25f, player.state.value.playbackSpeed, 0.001f)

        // Transient interruption (e.g. incoming notification/call) pauses & resumes cleanly on GAIN
        player.onAudioFocusChanged(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertFalse("Transient focus loss must pause playback", player.state.value.isPlaying)
        player.onAudioFocusChanged(AudioManager.AUDIOFOCUS_GAIN)
        assertTrue("Focus regain after transient loss must resume playback", player.state.value.isPlaying)

        // Permanent audio focus loss pauses and does NOT auto-resume on subsequent GAIN
        player.onAudioFocusChanged(AudioManager.AUDIOFOCUS_LOSS)
        assertFalse("Permanent focus loss must pause playback", player.state.value.isPlaying)
        player.onAudioFocusChanged(AudioManager.AUDIOFOCUS_GAIN)
        assertFalse("Permanent focus loss must NOT auto-resume", player.state.value.isPlaying)

        // E. Repeat One mode loops same Ayah
        assertEquals(AudioRepeatMode.REPEAT_ONE, player.cycleRepeatMode())
        player.handleTrackCompletion()
        assertEquals(2, player.state.value.playingSurah)
        assertEquals(1, player.state.value.playingAyah)

        // Reset repeat mode back to OFF
        player.cycleRepeatMode() // REPEAT_ALL
        assertEquals(AudioRepeatMode.OFF, player.cycleRepeatMode())

        // F. Repeat Ayah Range loops seamlessly across indices [1..2] (1:7 -> 2:1 -> 1:7)
        player.playAyahRange(queue, rangeStartIndex = 1, rangeEndIndex = 2, rangeLabel = "Loop 1:7-2:1")
        assertEquals(7, player.state.value.playingAyah)
        assertEquals("Loop 1:7-2:1", player.state.value.repeatRangeLabel)
        player.handleTrackCompletion()
        assertEquals(1, player.state.value.playingAyah)
        assertEquals(2, player.state.value.playingSurah)
        player.handleTrackCompletion()
        assertEquals("Range loop must wrap back to start Ayah 1:7", 7, player.state.value.playingAyah)
        assertEquals(1, player.state.value.playingSurah)

        player.stop()
        assertFalse(player.state.value.hasActiveSession)
        assertNull(player.state.value.playingAyah)
    }

    @Test
    fun wordTimingRepository_parsesVerifiedSegmentsAndRefusesUnsupportedRecitersHonestly() {
        // Supported reciters with verified Quran.com v4 word timestamps
        assertTrue(QuranWordTimingRepository.quranComReciterIdFor(QuranReciter.ALAFASY) == 7)
        assertTrue(QuranWordTimingRepository.quranComReciterIdFor(QuranReciter.HUSARY) == 6)

        // Reciters without published word segments MUST return null (never fake timers!)
        assertNull(QuranWordTimingRepository.quranComReciterIdFor(QuranReciter.YASSER_AL_DOSARI))
        assertNull(QuranWordTimingRepository.quranComReciterIdFor(QuranReciter.MAHER_AL_MUAIQLY))

        val sampleQuranComJson = """
            {
              "audio_file": {
                "timestamps": [
                  {
                    "verse_key": "112:1",
                    "timestamp_from": 1000,
                    "timestamp_to": 4200,
                    "segments": [
                      [1, 1000, 1600],
                      [2, 1650, 2300],
                      [3, 2350, 3200],
                      [4, 3250, 4150]
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val parsed = QuranWordTimingRepository.parseQuranComSegmentsJson(
            reciterId = QuranReciter.ALAFASY.id,
            surahNumber = 112,
            rawJson = sampleQuranComJson
        )
        val timings112_1 = parsed[1]
        assertNotNull(timings112_1)
        assertEquals(4, timings112_1!!.segments.size)

        // Verify Ayah-relative millisecond resolution (timestamp_from=1000 subtracted)
        assertEquals(1, timings112_1.findWordPositionAt(250L))   // 0..600ms -> Word 1 (قُلْ)
        assertNull("Silence gap between words must return null", timings112_1.findWordPositionAt(625L))
        assertEquals(2, timings112_1.findWordPositionAt(900L))   // 650..1300ms -> Word 2 (هُوَ)
        assertEquals(3, timings112_1.findWordPositionAt(1800L))  // 1350..2200ms -> Word 3 (ٱللَّهُ)
        assertEquals(4, timings112_1.findWordPositionAt(2700L))  // 2250..3150ms -> Word 4 (أَحَدٌ)
        assertNull("Beyond final segment must return null", timings112_1.findWordPositionAt(4500L))
    }

    // ============================================================================================
    // 3. STRUCTURED LINGUISTIC (I'RAB & SARF) VERIFICATION TESTS
    // ============================================================================================

    @Test
    fun linguisticRepository_verifiedIrabAndSarfForFatihaKursiAndPage604_andHonestMissingFallback() = runBlocking {
        val repo = QuranLinguisticRepository(context)

        // 1. Al-Fatiha 1:5 verified I'rab + Sarf
        val exactUthmani1_5 = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ"
        val analysis1_5 = repo.getAyahLinguisticAnalysis(
            surahNumber = 1,
            ayahNumber = 5,
            exactImmutableUthmaniText = exactUthmani1_5,
            cachedTafsirAr = "نخصك وحدك بالعبادة والاستعانة"
        )
        assertEquals(LinguisticVerificationStatus.VERIFIED_SCHOLARLY_CATALOG, analysis1_5.verificationStatus)
        assertEquals("Immutable Uthmani text must remain completely unaltered", exactUthmani1_5, analysis1_5.exactUthmaniAyahText)
        assertEquals(4, analysis1_5.words.size)
        assertEquals(listOf(1, 2, 3, 4), analysis1_5.words.map { it.wordPosition })
        assertTrue(analysis1_5.words[1].sarf!!.rootAr.contains("ع ب د"))
        assertTrue(analysis1_5.words[3].sarf!!.rootAr.contains("ع و ن"))
        assertTrue(analysis1_5.words[0].irab!!.grammaticalRoleAr.contains("مفعول به"))

        // 2. Complete Page 604 coverage: Al-Ikhlas (112:1-4), Al-Falaq (113:1-5), An-Nas (114:1-6)
        for (a in 1..4) {
            val res = repo.getAyahLinguisticAnalysis(112, a, "")
            assertEquals("112:$a must be verified", LinguisticVerificationStatus.VERIFIED_SCHOLARLY_CATALOG, res.verificationStatus)
            assertTrue(res.words.all { it.irab != null && it.sarf != null })
        }
        for (a in 1..5) {
            val res = repo.getAyahLinguisticAnalysis(113, a, "")
            assertEquals("113:$a must be verified", LinguisticVerificationStatus.VERIFIED_SCHOLARLY_CATALOG, res.verificationStatus)
        }
        for (a in 1..6) {
            val res = repo.getAyahLinguisticAnalysis(114, a, "")
            assertEquals("114:$a must be verified", LinguisticVerificationStatus.VERIFIED_SCHOLARLY_CATALOG, res.verificationStatus)
        }

        // 3. Ayat al-Kursi (2:255) verified
        val kursi = repo.getAyahLinguisticAnalysis(2, 255, "")
        assertEquals(LinguisticVerificationStatus.VERIFIED_SCHOLARLY_CATALOG, kursi.verificationStatus)
        assertTrue(kursi.words.isNotEmpty())

        // 4. Missing Ayah (18:50) -> MUST return UNAVAILABLE honestly without fabricating I'rab or Sarf!
        val missingText = "وَإِذْ قُلْنَا لِلْمَلَائِكَةِ اسْجُدُوا لِآدَمَ"
        val missing = repo.getAyahLinguisticAnalysis(
            surahNumber = 18,
            ayahNumber = 50,
            exactImmutableUthmaniText = missingText,
            cachedTafsirAr = "تفسير ميسر محفوظ للآية"
        )
        assertEquals(LinguisticVerificationStatus.UNAVAILABLE, missing.verificationStatus)
        assertEquals("Immutable Uthmani text preserved even when unavailable", missingText, missing.exactUthmaniAyahText)
        assertEquals("Tafsir preserved separately", "تفسير ميسر محفوظ للآية", missing.tafsirMuyassarAr)
        assertTrue("Must NEVER fabricate I'rab when unavailable", missing.words.all { it.irab == null })
        assertTrue("Must NEVER fabricate Sarf when unavailable", missing.words.all { it.sarf == null })
    }

    @Test
    fun linguisticRepository_offlineJsonPackImportResolvesCustomAyahCleanly() = runBlocking {
        val packJson = """
            {
              "sourceAr": "الجدول في إعراب القرآن (حزمة مستوردة)",
              "sourceEn": "Al-Jadwal Imported Offline Pack",
              "ayahs": [
                {
                  "surahNumber": 103,
                  "ayahNumber": 1,
                  "exactUthmaniAyahText": "وَٱلْعَصْرِ",
                  "sentenceSummaryAr": "جملة قسم وجواب القسم في الآية الثانية.",
                  "words": [
                    {
                      "wordPosition": 1,
                      "word": "وَٱلْعَصْرِ",
                      "irab": {
                        "roleAr": "الواو حرف جر وقسم، والعصر اسم مجرور",
                        "roleEn": "Oath Preposition + Genitive Noun",
                        "caseAr": "مجرور وعلامة جره الكسرة الظاهرة",
                        "relationshipAr": "متعلق بفعل قسم محذوف تقديره أقسم",
                        "explanationAr": "أقسم الله تعالى بالدهر والزمان تنبيهاً على قيمته."
                      },
                      "sarf": {
                        "rootAr": "ع ص ر",
                        "patternAr": "فَعْل",
                        "posAr": "اسم",
                        "posEn": "Noun",
                        "verbFormAr": "—",
                        "tenseAr": "—",
                        "numberGenderAr": "مفرد مذكر",
                        "derivationAr": "اسم جامد",
                        "notesAr": "مقترن بأل التعريف وواو القسم."
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val tempJson = File(context.cacheDir, "test_irab_pack.json").apply { writeText(packJson) }
        val repo = QuranLinguisticRepository(context)
        val count = repo.importOfflineLinguisticPack(Uri.fromFile(tempJson))
        assertEquals(1, count)

        val imported = repo.getAyahLinguisticAnalysis(103, 1, "وَٱلْعَصْرِ")
        assertEquals(LinguisticVerificationStatus.IMPORTED_LOCAL_PACK, imported.verificationStatus)
        assertEquals(1, imported.words.size)
        assertEquals("ع ص ر", imported.words.first().sarf?.rootAr)
        assertTrue(imported.words.first().irab!!.grammaticalRoleAr.contains("قسم"))
    }

    // ============================================================================================
    // 4. CONTINUOUS UNINTERRUPTED SURAH STREAM + TIMESTAMP SYNC (ZERO PLAYER RE-CREATION)
    // ============================================================================================

    @Test
    fun continuousSurahAudio_advancesAyah1To4AndPagesWithoutRecreatingMediaPlayer() {
        val sampleManifestJson = """
            {
              "audio_file": {
                "audio_url": "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/112.mp3",
                "timestamps": [
                  {
                    "verse_key": "112:1",
                    "timestamp_from": 0,
                    "timestamp_to": 3000,
                    "segments": [[1, 100, 700], [2, 800, 1400], [3, 1500, 2200], [4, 2300, 2900]]
                  },
                  {
                    "verse_key": "112:2",
                    "timestamp_from": 3100,
                    "timestamp_to": 5800,
                    "segments": [[1, 3200, 4400], [2, 4500, 5700]]
                  },
                  {
                    "verse_key": "112:3",
                    "timestamp_from": 6000,
                    "timestamp_to": 9000,
                    "segments": [[1, 6100, 7200], [2, 7400, 8900]]
                  },
                  {
                    "verse_key": "112:4",
                    "timestamp_from": 9100,
                    "timestamp_to": 13500,
                    "segments": [[1, 9200, 10800], [2, 11000, 13400]]
                  }
                ]
              }
            }
        """.trimIndent()

        val manifest = QuranWordTimingRepository.parseQuranComContinuousManifestJson(
            reciterId = QuranReciter.ALAFASY.id,
            quranComReciterId = 7,
            surahNumber = 112,
            rawJson = sampleManifestJson
        )
        assertNotNull("Continuous Surah manifest must parse audio_url and timestamps", manifest)
        assertEquals(4, manifest!!.ayahs.size)

        // Verify monotonic timestamp lookup including inter-Ayah silence gap (3050ms holds Ayah 1 cleanly!)
        assertEquals(1, manifest.findAyahAt(500L)?.ayahNumber)
        assertEquals(1, manifest.findAyahAt(3050L)?.ayahNumber)
        assertEquals(2, manifest.findAyahAt(3500L)?.ayahNumber)
        assertEquals(3, manifest.findAyahAt(7500L)?.ayahNumber)
        assertEquals(4, manifest.findAyahAt(11500L)?.ayahNumber)
        assertEquals(3, manifest.findAyahRange(1)?.findWordPositionAtStreamMs(1800L))

        // Start continuous playback in QuranAudioPlayer
        val player = QuranAudioPlayer(context)
        val observedAyahChanges = mutableListOf<Int>()
        player.playContinuousSurahManifest(
            manifest = manifest,
            surahTitleAr = "سورة الإخلاص",
            surahTitleEn = "Al-Ikhlas",
            reciterNameAr = QuranReciter.ALAFASY.nameAr,
            reciterNameEn = QuranReciter.ALAFASY.nameEn,
            pageResolver = { _, ayah -> if (ayah <= 2) 604 else 605 },
            startAyah = 1,
            initialSpeed = 1.0f,
            onAyahChanged = { _, ayah, _ -> observedAyahChanges += ayah }
        )

        assertTrue("Continuous stream flag must be true", player.state.value.isContinuousStream)
        assertEquals("MediaPlayer must be created ONCE for the continuous Surah", 1, player.playerCreationCountForTest)
        assertEquals(1, player.state.value.playingAyah)
        assertEquals(604, player.state.value.playingPageNumber)

        // Simulate continuous audio progression across Ayah 1 -> 2 -> 3 -> 4
        player.simulateContinuousPositionForTest(1800L)
        assertEquals(1, player.state.value.playingAyah)
        assertEquals(3, player.state.value.activeWordPosition)

        player.simulateContinuousPositionForTest(4000L)
        assertEquals(2, player.state.value.playingAyah)
        assertEquals(1, player.state.value.activeWordPosition)
        assertEquals(604, player.state.value.playingPageNumber)

        player.simulateContinuousPositionForTest(7600L)
        assertEquals(3, player.state.value.playingAyah)
        assertEquals(2, player.state.value.activeWordPosition)
        assertEquals("Page must advance automatically at page boundary", 605, player.state.value.playingPageNumber)

        player.simulateContinuousPositionForTest(11500L)
        assertEquals(4, player.state.value.playingAyah)
        assertEquals(2, player.state.value.activeWordPosition)

        // CRITICAL GUARANTEE: MediaPlayer was NEVER torn down or recreated across Ayahs 1 -> 2 -> 3 -> 4!
        assertEquals("Zero MediaPlayer re-creations across Ayah transitions", 1, player.playerCreationCountForTest)
        assertEquals(listOf(1, 2, 3, 4), observedAyahChanges)

        // Instant in-stream jump/previous/next must NOT recreate MediaPlayer either
        assertTrue(player.jumpToAyahInQueue(112, 2))
        assertEquals(2, player.state.value.playingAyah)
        player.playNext()
        assertEquals(3, player.state.value.playingAyah)
        player.playPrevious()
        assertEquals(2, player.state.value.playingAyah)
        assertEquals("In-stream seek/next/prev must reuse single MediaPlayer", 1, player.playerCreationCountForTest)

        // In-stream Repeat-One stays locked to Ayah 2
        assertEquals(AudioRepeatMode.REPEAT_ONE, player.cycleRepeatMode())
        player.simulateContinuousPositionForTest(5900L)
        assertEquals("REPEAT_ONE must loop back to Ayah 2 without advancing to 3", 2, player.state.value.playingAyah)

        player.stop()
        assertFalse(player.state.value.isContinuousStream)
    }

    @Test
    fun continuousSurahAudio_repeatAyahRangeLoopsWithinSingleStream() {
        val sampleManifestJson = """
            {
              "audio_file": {
                "audio_url": "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/112.mp3",
                "timestamps": [
                  { "verse_key": "112:1", "timestamp_from": 0, "timestamp_to": 3000, "segments": [] },
                  { "verse_key": "112:2", "timestamp_from": 3000, "timestamp_to": 6000, "segments": [] },
                  { "verse_key": "112:3", "timestamp_from": 6000, "timestamp_to": 9000, "segments": [] },
                  { "verse_key": "112:4", "timestamp_from": 9000, "timestamp_to": 12000, "segments": [] }
                ]
              }
            }
        """.trimIndent()
        val manifest = QuranWordTimingRepository.parseQuranComContinuousManifestJson(
            reciterId = QuranReciter.ALAFASY.id,
            quranComReciterId = 7,
            surahNumber = 112,
            rawJson = sampleManifestJson
        )!!
        val player = QuranAudioPlayer(context)
        player.playContinuousSurahManifest(
            manifest = manifest,
            surahTitleAr = "سورة الإخلاص",
            surahTitleEn = "Al-Ikhlas",
            reciterNameAr = QuranReciter.ALAFASY.nameAr,
            reciterNameEn = QuranReciter.ALAFASY.nameEn,
            pageResolver = { _, _ -> 604 },
            startAyah = 2,
            repeatRangeStartAyah = 2,
            repeatRangeEndAyah = 3,
            repeatRangeLabel = "Loop 2-3"
        )
        assertEquals(2, player.state.value.playingAyah)
        player.simulateContinuousPositionForTest(7200L)
        assertEquals(3, player.state.value.playingAyah)
        // Passing end of Ayah 3 (9000ms) must wrap seamlessly back to Ayah 2 without recreating MediaPlayer
        player.simulateContinuousPositionForTest(9050L)
        assertEquals(2, player.state.value.playingAyah)
        assertEquals(1, player.playerCreationCountForTest)
        player.stop()
    }

    // ============================================================================================
    // 5. VOICE RECORDER, ARABIC STT HONESTY, MEMORIZATION ASSESSMENT & LAST-READ PERSISTENCE
    // ============================================================================================

    @Test
    fun voiceRecorder_rejectsMissingPermissionAndCorruptFilesWithoutFakeWaveforms() {
        val recorder = QuranRecitationRecorder(context)

        // Without RECORD_AUDIO permission, startRecording MUST fail cleanly and keep amplitude == 0f
        val started = recorder.startRecording("test_112_1_4")
        assertFalse("Recording without RECORD_AUDIO permission must fail honestly", started)
        assertEquals(RecitationRecordingStatus.ERROR, recorder.recordingState.value.status)
        assertEquals(0f, recorder.recordingState.value.amplitudeNormalized, 0.0001f)

        // Playing a missing or 0-byte audio file must be rejected cleanly
        val emptyFile = File(context.cacheDir, "empty_recitation.m4a").apply { writeBytes(ByteArray(0)) }
        assertFalse("Zero-byte file must be rejected", recorder.playRecording(emptyFile.absolutePath))
        assertNotNull(recorder.playbackState.value.errorAr)
        recorder.release()
    }

    @Test
    fun liveRecitationListenerAndAssessment_hybridArabicDefaultAndWordLevelEvaluation() {
        val listener = QuranLiveRecitationListener(context)

        // Default MUST be hybrid Arabic (offline-preferred = false) so real devices don't fail without offline pack
        assertFalse("Default STT mode must be hybrid Arabic (not forced offline-only)", listener.state.value.isOfflinePreferred)

        listener.injectRecognizedText("قل هو الله احد الله الصمد")
        assertTrue(listener.state.value.accumulatedTranscript.contains("قل هو الله"))
        listener.stopListening()

        // Evaluate recognized Arabic text against verified Surah Al-Ikhlas Ayahs 1-2 (with deliberate omission)
        val verses = listOf(
            QuranVerse(112, 1, 6222, "قُلْ هُوَ اللَّهُ أَحَدٌ", "", "", pageNumber = 604),
            QuranVerse(112, 2, 6223, "اللَّهُ الصَّمَدُ", "", "", pageNumber = 604)
        )
        val exactReport = QuranRecitationAssessmentEngine.evaluate(verses, "قل هو الله احد الله الصمد")
        assertEquals(RecitationOverallLevel.EXCELLENT, exactReport.overallLevel)
        assertTrue("Exact match must score >= 90%", exactReport.accuracyPercent >= 90)
        assertTrue(exactReport.omittedWords.isEmpty())

        val omittedReport = QuranRecitationAssessmentEngine.evaluate(verses, "قل هو الله الصمد")
        assertTrue("Omitted word 'أَحَدٌ' must be detected", omittedReport.omittedWords.isNotEmpty())
        assertTrue("Omission must reduce accuracy below 100%", omittedReport.accuracyPercent < 100)
    }

    @Test
    fun readingPositionPersistence_preservesExactLastReadAyahAcrossReload() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)
        prefsRepo.saveLastRead(surahNumber = 18, ayahNumber = 10)
        val saved = prefsRepo.getPreferencesOnce()
        assertEquals("Saved Surah must persist accurately", 18, saved.lastReadSurah)
        assertEquals("Saved Ayah must remain 10 and never reset to 1", 10, saved.lastReadAyah)
    }
}

