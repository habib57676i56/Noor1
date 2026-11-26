package com.example

import com.example.data.cloud.AppVersionGate
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorPlatformConstants
import com.example.data.quran.QuranWordTimingRepository
import com.example.domain.model.QuranReciter
import com.example.ui.screens.ecosystem.AsbabAlNuzulCatalog
import com.example.ui.screens.ecosystem.HifzSpacedRepetitionEngine
import com.example.ui.screens.ecosystem.LunarAstronomyEngine
import com.example.ui.screens.ecosystem.MakhrajZone
import com.example.ui.screens.ecosystem.PropheticSundialEngine
import com.example.ui.screens.ecosystem.QuranMaqamatCatalog
import com.example.ui.screens.ecosystem.QuranWaqfCatalog
import com.example.ui.screens.ecosystem.QuranicSanadCatalog
import com.example.ui.screens.ecosystem.SacredMeeqatCalculator
import com.example.ui.screens.ecosystem.SacredSatelliteAtlasCatalog
import com.example.ui.screens.ecosystem.SpiritualMuhasabahEngine
import com.example.ui.screens.ecosystem.TajweedAcousticDsp
import com.example.ui.screens.ecosystem.TajweedMakharijCatalog
import com.example.ui.screens.ecosystem.YasserDosariIraqiCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NoorFinalUpgradeTest {

    @Test
    fun forceUpdateGate_blocksOlderVersionsAndAutoDismissesOnceUpdated() {
        val defaultConfig = GlobalAppConfigDoc()
        assertEquals("https://t.me/Noor_ui", NoorPlatformConstants.DEFAULT_UPDATE_CHANNEL_URL)
        assertEquals("https://t.me/Noor_ui", defaultConfig.updateChannelUrl)
        assertEquals("3.5.0", NoorPlatformConstants.APP_VERSION_NAME)
        assertEquals("v3.5.0", NoorPlatformConstants.APP_VERSION_TAG)

        // New build v3.5.0 passes default v3.5.0 gate, while older v3.4.0 / v3.3.0 APKs are blocked!
        assertFalse(AppVersionGate.isForceUpdateRequired("3.5.0", defaultConfig))
        assertTrue(AppVersionGate.isForceUpdateRequired("3.4.0", defaultConfig))
        assertTrue(AppVersionGate.isForceUpdateRequired("3.3.0", defaultConfig))

        // CRITICAL AUTO-DISMISS AFTER UPDATE: even if forceUpdateEnabled was left ON in Firestore
        // with minSupportedVersion = "3.5.0", the newly updated v3.5.0 (or v3.6.0) APK opens automatically
        // while older v3.4.0 remains blocked!
        val forcedWithMin = defaultConfig.copy(forceUpdateEnabled = true, minSupportedVersion = "3.5.0")
        assertTrue(AppVersionGate.isForceUpdateRequired("3.4.0", forcedWithMin))
        assertFalse(AppVersionGate.isForceUpdateRequired("3.5.0", forcedWithMin))
        assertFalse(AppVersionGate.isForceUpdateRequired("3.6.0", forcedWithMin))

        // Raising minSupportedVersion to 3.6.0 blocks 3.5.0 until user updates to 3.6.0
        val raisedConfig = defaultConfig.copy(minSupportedVersion = "3.6.0")
        assertTrue(AppVersionGate.isForceUpdateRequired("3.5.0", raisedConfig))
        assertFalse(AppVersionGate.isForceUpdateRequired("3.6.0", raisedConfig))

        // Safe URL resolver enforces https:// and falls back to https://t.me/Noor_ui
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl(""))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("javascript:alert(1)"))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("http://insecure.example"))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("https://t.me/Noor_ui"))
    }

    @Test
    fun quranWaqfCatalog_hasAll7MushafSignsValidQuizAndBreathEstimator() {
        assertEquals(7, QuranWaqfCatalog.signs.size)
        assertTrue(QuranWaqfCatalog.signs.all { it.audioUrl().startsWith("https://everyayah.com/data/") })
        assertTrue(QuranWaqfCatalog.quizQuestions.size >= 4)
        assertTrue(QuranWaqfCatalog.quizQuestions.all { it.correctIndex in it.optionsAr.indices })
        assertEquals(2.0, QuranWaqfCatalog.estimateRecitableLines(15.0), 0.01)
    }

    @Test
    fun lunarAstronomyEngine_computesAccurateMoonPhaseAndVerifiedEclipses() {
        // Reference New Moon epoch (2000-01-06 18:14 UTC) should yield ~0% illumination
        val newMoon = LunarAstronomyEngine.computeLunarPhase(947182440000L)
        assertTrue(newMoon.illuminationPercent < 1.0)
        assertTrue(newMoon.ageDays < 0.2)

        // Half a synodic month later should yield ~100% Full Moon (Badr) & Ayyam al-Bid active!
        val halfMonthMs = (LunarAstronomyEngine.SYNODIC_MONTH_DAYS / 2.0 * 86_400_000.0).toLong()
        val fullMoon = LunarAstronomyEngine.computeLunarPhase(947182440000L + halfMonthMs)
        assertTrue("Expected >99% illumination, got ${fullMoon.illuminationPercent}", fullMoon.illuminationPercent > 99.0)
        assertTrue(fullMoon.isAyyamAlBidNow)
        assertTrue(fullMoon.distanceKm in 350_000..415_000)

        assertTrue(LunarAstronomyEngine.landmarks.size >= 6)
        assertTrue(LunarAstronomyEngine.upcomingEclipses.size >= 6)
        assertTrue(LunarAstronomyEngine.upcomingEclipses.any { it.id == "solar_2027_aug_makkah" })
    }

    @Test
    fun yasserAlDosariIraqiMaqam_inMushafRecitersAndViralStudioCatalog() {
        val iraqi = QuranReciter.YASSER_AL_DOSARI_IRAQI_MAQAM
        assertEquals("yasser_dosari_iraqi", iraqi.id)
        assertTrue(iraqi.nameAr.contains("العراق"))
        assertTrue(iraqi.fullSurahUrl(40).endsWith("/040.mp3"))
        assertTrue(iraqi.ayahAudioUrl(40, 44).endsWith("/040044.mp3"))
        assertEquals(iraqi, QuranReciter.fromId("yasser_dosari_iraqi"))

        assertEquals(10, YasserDosariIraqiCatalog.recitations.size)
        assertTrue(YasserDosariIraqiCatalog.recitations.all {
            it.fullSurahAudioUrl().startsWith("https://server11.mp3quran.net/yasser/") &&
                it.climaxAyahAudioUrl().startsWith("https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/") &&
                it.youtubeSearchUrl().startsWith("https://www.youtube.com/results?search_query=")
        })
    }

    @Test
    fun customUploadedReciter_proportionalManifestTracksAyahsAcrossSurah() {
        val manifest = QuranWordTimingRepository.buildProportionalSurahManifest(
            reciterId = "custom_sheikh_1",
            surahNumber = 1,
            audioSource = "/data/user/0/custom/fatihah.mp3",
            ayahWeights = listOf(1 to 20, 2 to 20, 3 to 20, 4 to 40),
            totalDurationMs = 100_000L
        )
        assertEquals(QuranWordTimingRepository.CUSTOM_PROPORTIONAL_RECITER_ID, manifest.quranComReciterId)
        assertEquals(4, manifest.ayahs.size)
        assertEquals(1, manifest.findAyahAt(5_000L)?.ayahNumber)
        assertEquals(2, manifest.findAyahAt(25_000L)?.ayahNumber)
        assertEquals(3, manifest.findAyahAt(45_000L)?.ayahNumber)
        assertEquals(4, manifest.findAyahAt(75_000L)?.ayahNumber)
    }

    @Test
    fun tajweedMakharijCatalog_coversAll28LettersAnd5MajorZones() {
        assertEquals(28, TajweedMakharijCatalog.letters.size)
        val coveredZones = TajweedMakharijCatalog.letters.map { it.zone }.toSet()
        assertEquals(MakhrajZone.entries.toSet(), coveredZones)
        assertTrue(TajweedMakharijCatalog.rules.size >= 6)
        assertTrue(TajweedMakharijCatalog.quizQuestions.all { it.correctIndex in it.optionsAr.indices })
    }

    @Test
    fun spiritualMuhasabahEngine_computesHeartIndexAndWeakestAxis() {
        assertEquals(6, SpiritualMuhasabahEngine.axes.size)
        val fullScores = SpiritualMuhasabahEngine.axes.associate { it.id to 10 }
        assertEquals(100, SpiritualMuhasabahEngine.computeHeartNoorIndex(fullScores))

        val mixed = mapOf(
            "salah" to 9,
            "quran" to 2,
            "dhikr" to 8,
            "ihsan" to 7,
            "akhlaq" to 8,
            "qiyam" to 6
        )
        assertEquals(67, SpiritualMuhasabahEngine.computeHeartNoorIndex(mixed))
        assertEquals("quran", SpiritualMuhasabahEngine.findWeakestAxis(mixed).id)
    }

    @Test
    fun asbabAlNuzulCatalog_containsVerifiedMakkiAndMadaniEntries() {
        val entries = AsbabAlNuzulCatalog.entries
        assertTrue(entries.size >= 10)
        assertTrue(entries.any { it.isMakki })
        assertTrue(entries.any { !it.isMakki })
        assertTrue(entries.all { it.surahNumber in 1..114 && it.sourceAr.isNotBlank() && it.occasionAr.isNotBlank() })
    }

    @Test
    fun sacredSatelliteAtlasCatalog_has10LandmarksAndAccurateHaversineDistance() {
        val landmarks = SacredSatelliteAtlasCatalog.landmarks
        assertEquals(10, landmarks.size)
        val kaaba = landmarks.first { it.id == "kaaba" }
        val nabawi = landmarks.first { it.id == "nabawi" }
        val makkahToMadinahKm = SacredSatelliteAtlasCatalog.distanceKm(kaaba.lat, kaaba.lon, nabawi.lat, nabawi.lon)
        assertTrue("Expected ~339 km, got $makkahToMadinahKm", makkahToMadinahKm in 330.0..350.0)
    }

    @Test
    fun tajweedAcousticDsp_measuresRmsDbAndClassifiesHarakaatCorrectly() {
        val silence = ShortArray(512) { 0 }
        assertEquals(-90.0, TajweedAcousticDsp.computeRmsDb(silence, silence.size), 0.1)

        val loudTone = ShortArray(512) { 16384 }
        val db = TajweedAcousticDsp.computeRmsDb(loudTone, loudTone.size)
        assertTrue("Expected around -6 dB, got $db", db in -7.5..-4.5)

        val twoHarakaat = TajweedAcousticDsp.durationMsToHarakaat(720L, 360.0)
        assertEquals(2.0, twoHarakaat, 0.01)
        assertTrue(TajweedAcousticDsp.classifyMaddType(twoHarakaat).first.contains("طبيعي"))

        val sixHarakaat = TajweedAcousticDsp.durationMsToHarakaat(2160L, 360.0)
        assertEquals(6.0, sixHarakaat, 0.01)
        assertTrue(TajweedAcousticDsp.classifyMaddType(sixHarakaat).first.contains("لازم"))
    }

    @Test
    fun quranMaqamatCatalog_hasAll7ClassicalMaqamatAndSynthesizesValidPcm() {
        val maqamat = QuranMaqamatCatalog.maqamat
        assertEquals(7, maqamat.size)
        assertTrue(maqamat.all { it.frequenciesHz.size == 8 && it.frequenciesHz.all { f -> f > 150.0 } })
        assertTrue(maqamat.all { it.recitationAudioUrl.startsWith("https://everyayah.com/data/") })

        val pcm = QuranMaqamatCatalog.synthesizeTonePcm(320.24, durationMs = 200)
        assertTrue(pcm.size > 4000)
        assertTrue(pcm.any { abs(it.toInt()) > 5000 })
    }

    @Test
    fun quranicSanadCatalog_coversAll10MutawatirImamsAndLiveAudioSamples() {
        assertEquals(10, QuranicSanadCatalog.imams.size)
        assertTrue(QuranicSanadCatalog.imams.all { it.rawiOneAr.isNotBlank() && it.rawiTwoAr.isNotBlank() })
        assertTrue(QuranicSanadCatalog.liveAudioSamples.size >= 4)
        assertTrue(QuranicSanadCatalog.liveAudioSamples.all { it.audioUrl.startsWith("https://everyayah.com/data/") })
    }

    @Test
    fun sacredMeeqatCalculator_computesValidNightThirdsAndCountdownFormat() {
        val snap = SacredMeeqatCalculator.computeSnapshot(21.4225, 39.8262)
        assertTrue(snap.lastThirdStartMillis > snap.islamicMidnightMillis)
        assertTrue(snap.nextFajrMillis > snap.lastThirdStartMillis)
        assertTrue(snap.duhaPeakMillis > snap.duhaStartMillis)
        assertEquals("01:05:09", SacredMeeqatCalculator.formatCountdownHms(3909L))
    }

    @Test
    fun hifzSpacedRepetitionEngine_updatesIntervalsAndMasteryCorrectly() {
        val starter = HifzSpacedRepetitionEngine.starterCards.first()
        val firstReview = HifzSpacedRepetitionEngine.reviewCard(starter, quality = 5, nowMillis = 1_000_000L)
        assertEquals(1, firstReview.repetitions)
        assertEquals(1, firstReview.intervalDays)

        val secondReview = HifzSpacedRepetitionEngine.reviewCard(firstReview, quality = 5, nowMillis = 2_000_000L)
        assertEquals(2, secondReview.repetitions)
        assertEquals(6, secondReview.intervalDays)

        val thirdReview = HifzSpacedRepetitionEngine.reviewCard(secondReview, quality = 5, nowMillis = 3_000_000L)
        assertEquals(3, thirdReview.repetitions)
        assertTrue(thirdReview.intervalDays >= 12)

        val hardReset = HifzSpacedRepetitionEngine.reviewCard(thirdReview, quality = 1, nowMillis = 4_000_000L)
        assertEquals(0, hardReset.repetitions)
        assertEquals(1, hardReset.intervalDays)

        assertEquals(50, HifzSpacedRepetitionEngine.computeMasteryPercent(listOf(secondReview, hardReset)))
    }

    @Test
    fun propheticSundialEngine_computesValidDaylightShadowAndAsrThresholds() {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Riyadh")).apply {
            set(2026, java.util.Calendar.MAY, 28, 12, 18, 0)
        }
        val makkahNoon = PropheticSundialEngine.computeReading(21.4225, 39.8262, cal, staffHeightMeters = 1.0)
        assertTrue("Expected high noon sun in Makkah on May 28, got ${makkahNoon.solarAltitudeDeg}", makkahNoon.solarAltitudeDeg > 80.0)
        assertTrue(makkahNoon.isDaylight)
        assertTrue((makkahNoon.shadowLengthMeters ?: 99.0) < 0.25)
        assertTrue(makkahNoon.shafiAsrThresholdMeters > makkahNoon.meridianShadowMeters)
        assertTrue(makkahNoon.hanafiAsrThresholdMeters > makkahNoon.shafiAsrThresholdMeters)
        assertTrue(makkahNoon.daysToNextKaabaZenith in 0..365)
    }
}
