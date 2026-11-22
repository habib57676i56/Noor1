package com.example

import com.example.audio.AdhanCatalog
import com.example.audio.CustomAdhanRecord
import com.example.audio.PerPrayerAdhanConfig
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranVerse
import com.example.domain.quran.QuranArabicPhoneticNormalizer
import com.example.domain.quran.QuranVerseTrackerEngine
import com.example.shortcuts.NoorShortcutManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoorSmartUpdateUnitTest {

    // --- 1. Advanced Adhan Sound Customization Tests ---

    @Test
    fun adhanCatalog_containsSheikhMaherAsDefaultAndSystemFallback() {
        val defaultVoice = AdhanCatalog.builtInVoices.first()
        assertEquals(AdhanCatalog.DEFAULT_VOICE_ID, defaultVoice.id)
        assertTrue(defaultVoice.nameEn.contains("Maher Al-Muaiqly"))
        assertTrue(defaultVoice.nameAr.contains("ماهر المعيقلي"))
        assertTrue(AdhanCatalog.builtInVoices.any { it.isSystemDefault && it.id == AdhanCatalog.SYSTEM_DEFAULT_ID })
    }

    @Test
    fun adhanCatalog_validatesSupportedAudioFormatsOnly() {
        assertEquals("mp3", AdhanCatalog.validateFormat("maher_adhan.MP3", null))
        assertEquals("m4a", AdhanCatalog.validateFormat("fajr_call.m4a", null))
        assertEquals("wav", AdhanCatalog.validateFormat("makkah_live.wav", null))
        assertEquals("mp3", AdhanCatalog.validateFormat("unknown", "audio/mpeg"))
        assertEquals("m4a", AdhanCatalog.validateFormat("unknown", "audio/x-m4a"))
        assertEquals("wav", AdhanCatalog.validateFormat("unknown", "audio/wav"))

        assertNull(AdhanCatalog.validateFormat("notes.txt", "text/plain"))
        assertNull(AdhanCatalog.validateFormat("clip.ogg", "audio/ogg"))
        assertNull(AdhanCatalog.validateFormat("document.pdf", "application/pdf"))
    }

    @Test
    fun adhanConfig_supportsPerPrayerOverridesAndDefaultFallback() {
        val config = PerPrayerAdhanConfig(
            defaultVoiceId = "adhan_maher_muaiqly",
            fajrVoiceId = "adhan_madinah",
            maghribVoiceId = "adhan_aqsa"
        )
        assertEquals("adhan_madinah", config.voiceIdForPrayer(PrayerName.FAJR))
        assertTrue(config.hasCustomOverride(PrayerName.FAJR))
        assertEquals("adhan_maher_muaiqly", config.voiceIdForPrayer(PrayerName.DHUHR))
        assertFalse(config.hasCustomOverride(PrayerName.DHUHR))
        assertEquals("adhan_aqsa", config.voiceIdForPrayer(PrayerName.MAGHRIB))
        assertEquals("adhan_maher_muaiqly", config.voiceIdForPrayer(PrayerName.ISHA))
    }

    @Test
    fun adhanCatalog_customRecordsEncodeDecodeAndFallbackIfFileMissing() {
        val tempFile = File.createTempFile("noor_test_adhan_", ".mp3").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val rec = CustomAdhanRecord(
            id = "adhan_custom_1001",
            displayName = "My Fajr Adhan",
            localFilePath = tempFile.absolutePath,
            extension = "mp3",
            importedAtMillis = 1700000000000L
        )
        val encoded = AdhanCatalog.encodeCustomRecords(listOf(rec))
        val decoded = AdhanCatalog.decodeCustomRecords(encoded)
        assertEquals(1, decoded.size)
        assertEquals(rec.id, decoded.first().id)
        assertEquals(rec.displayName, decoded.first().displayName)
        assertEquals(rec.localFilePath, decoded.first().localFilePath)

        // When file exists -> resolves to custom voice
        val resolvedExisting = AdhanCatalog.resolveVoice(rec.id, decoded)
        assertEquals(rec.id, resolvedExisting.id)
        assertTrue(resolvedExisting.isCustomImported)

        // When file is deleted -> safely falls back to built-in default Adhan!
        tempFile.delete()
        val resolvedFallback = AdhanCatalog.resolveVoice(rec.id, decoded)
        assertEquals(AdhanCatalog.DEFAULT_VOICE_ID, resolvedFallback.id)
        assertFalse(resolvedFallback.isCustomImported)
    }

    // --- 2. On-Device Quran Audio Recognition & Live Verse Tracking Tests ---

    private fun sampleFatihaVerses(): List<QuranVerse> = listOf(
        QuranVerse(surahNumber = 1, numberInSurah = 1, globalNumber = 1, textUthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", tafsirAr = "", audioUrl = "", pageNumber = 1, juzNumber = 1),
        QuranVerse(surahNumber = 1, numberInSurah = 2, globalNumber = 2, textUthmani = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", tafsirAr = "", audioUrl = "", pageNumber = 1, juzNumber = 1),
        QuranVerse(surahNumber = 1, numberInSurah = 3, globalNumber = 3, textUthmani = "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", tafsirAr = "", audioUrl = "", pageNumber = 1, juzNumber = 1),
        QuranVerse(surahNumber = 1, numberInSurah = 4, globalNumber = 4, textUthmani = "مَٰلِكِ يَوْمِ ٱلدِّينِ", tafsirAr = "", audioUrl = "", pageNumber = 1, juzNumber = 1),
        QuranVerse(surahNumber = 1, numberInSurah = 5, globalNumber = 5, textUthmani = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", tafsirAr = "", audioUrl = "", pageNumber = 1, juzNumber = 1),
        QuranVerse(surahNumber = 1, numberInSurah = 6, globalNumber = 6, textUthmani = "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", tafsirAr = "", audioUrl = "", pageNumber = 1, juzNumber = 1),
        QuranVerse(surahNumber = 1, numberInSurah = 7, globalNumber = 7, textUthmani = "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", tafsirAr = "", audioUrl = "", pageNumber = 1, juzNumber = 1)
    )

    @Test
    fun phoneticNormalizer_normalizesUthmaniScriptToSpokenArabic() {
        val norm = QuranArabicPhoneticNormalizer.normalize("ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ")
        assertEquals("الحمد لله رب العالمين", norm)

        val prayerNorm = QuranArabicPhoneticNormalizer.normalize("وَأَقِيمُوا۟ ٱلصَّلَوٰةَ")
        assertTrue(prayerNorm.contains("الصلاه"))
    }

    @Test
    fun verseTracker_tracksSequentialRecitationRepeatsAndMistakesAccurately() {
        val engine = QuranVerseTrackerEngine()
        engine.loadSurahVerses(1, sampleFatihaVerses(), initialAyah = 1)

        // Ayah 2
        val d2 = engine.processTranscript("الحمد لله رب العالمين", isFinalResult = true)
        assertTrue(d2.isCertain)
        assertEquals(2, d2.matchedAyah)
        assertFalse(d2.isRepeatedVerse)

        // Ayah 2 repeated by reciter
        val d2Repeat = engine.processTranscript("الحمد لله رب العالمين", isFinalResult = true)
        assertTrue(d2Repeat.isCertain)
        assertEquals(2, d2Repeat.matchedAyah)
        assertTrue(d2Repeat.isRepeatedVerse)

        // Ayah 3 -> Ayah 4 -> Ayah 5 (with minor pronunciation/STT slip "نستعين" -> "نستعيين")
        val d3 = engine.processTranscript("الرحمن الرحيم", isFinalResult = true)
        assertTrue(d3.isCertain)
        assertEquals(3, d3.matchedAyah)

        val d4 = engine.processTranscript("مالك يوم الدين", isFinalResult = true)
        assertTrue(d4.isCertain)
        assertEquals(4, d4.matchedAyah)

        val d5 = engine.processTranscript("اياك نعبد واياك نستعيييين", isFinalResult = true)
        assertTrue(d5.isCertain)
        assertEquals(5, d5.matchedAyah)

        // Ayah 6 & 7
        val d6 = engine.processTranscript("اهدنا الصراط المستقيم", isFinalResult = true)
        assertTrue(d6.isCertain)
        assertEquals(6, d6.matchedAyah)

        val d7 = engine.processTranscript("صراط الذين انعمت عليهم غير المغضوب عليهم ولا الضالين", isFinalResult = true)
        assertTrue(d7.isCertain)
        assertEquals(7, d7.matchedAyah)
    }

    @Test
    fun verseTracker_holdsCurrentAyahOnUncertainOrNoisyInput() {
        val engine = QuranVerseTrackerEngine()
        engine.loadSurahVerses(1, sampleFatihaVerses(), initialAyah = 4)

        val shortNoise = engine.processTranscript("آه", isFinalResult = false)
        assertFalse(shortNoise.isCertain)
        assertEquals(4, shortNoise.matchedAyah)

        val unrelated = engine.processTranscript("مرحبا كيف حالك اليوم في المنزل", isFinalResult = true)
        assertFalse(unrelated.isCertain)
        assertEquals(4, unrelated.matchedAyah)
    }

    @Test
    fun verseTracker_detectsCrossSurahJumpOnHighConfidenceOpening() {
        val engine = QuranVerseTrackerEngine()
        engine.loadSurahVerses(1, sampleFatihaVerses(), initialAyah = 7)

        val jump = engine.processTranscript("قل هو الله احد الله الصمد لم يلد ولم يولد", isFinalResult = true)
        assertTrue(jump.isCertain)
        assertTrue(jump.isCrossSurahJump)
        assertEquals(112, jump.matchedSurah)
        assertEquals(1, jump.matchedAyah)
    }

    // --- 3. Home Screen Shortcuts Registry Tests ---

    @Test
    fun shortcutManager_exposesAllSixRequiredQuickActionRoutes() {
        val routes = NoorShortcutManager.allSupportedTargetRoutes()
        assertEquals(6, routes.size)
        assertTrue(routes.contains(NoorShortcutManager.TARGET_QURAN))
        assertTrue(routes.contains(NoorShortcutManager.TARGET_RESUME_READING))
        assertTrue(routes.contains(NoorShortcutManager.TARGET_START_LISTENING))
        assertTrue(routes.contains(NoorShortcutManager.TARGET_PRAYER_TIMES))
        assertTrue(routes.contains(NoorShortcutManager.TARGET_QIBLA))
        assertTrue(routes.contains(NoorShortcutManager.TARGET_ADHAN_SETTINGS))
        assertNotNull(routes.distinct().takeIf { it.size == routes.size })
    }
}
