package com.example

import com.example.data.cloud.AppVersionGate
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorPlatformConstants
import com.example.ui.screens.ecosystem.AsbabAlNuzulCatalog
import com.example.ui.screens.ecosystem.HifzSpacedRepetitionEngine
import com.example.ui.screens.ecosystem.MakhrajZone
import com.example.ui.screens.ecosystem.QuranMaqamatCatalog
import com.example.ui.screens.ecosystem.QuranicSanadCatalog
import com.example.ui.screens.ecosystem.SacredMeeqatCalculator
import com.example.ui.screens.ecosystem.SacredSatelliteAtlasCatalog
import com.example.ui.screens.ecosystem.SpiritualMuhasabahEngine
import com.example.ui.screens.ecosystem.TajweedAcousticDsp
import com.example.ui.screens.ecosystem.TajweedMakharijCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NoorFinalUpgradeTest {

    @Test
    fun forceUpdateGate_blocksOlderVersionsAndDefaultsToTelegramNoorUi() {
        val defaultConfig = GlobalAppConfigDoc()
        assertEquals("https://t.me/Noor_ui", NoorPlatformConstants.DEFAULT_UPDATE_CHANNEL_URL)
        assertEquals("https://t.me/Noor_ui", defaultConfig.updateChannelUrl)
        assertEquals("3.1.0", NoorPlatformConstants.APP_VERSION_NAME)
        assertEquals("v3.1.0", NoorPlatformConstants.APP_VERSION_TAG)
        // New build v3.1.0 passes the default v3.1.0 gate, while older v3.0.0 APK is blocked!
        assertFalse(AppVersionGate.isForceUpdateRequired("3.1.0", defaultConfig))
        assertTrue(AppVersionGate.isForceUpdateRequired("3.0.0", defaultConfig))
        assertTrue(AppVersionGate.isForceUpdateRequired("2.9.9", defaultConfig))

        // Raising minSupportedVersion to 3.2.0 blocks 3.1.0
        val raisedConfig = defaultConfig.copy(minSupportedVersion = "3.2.0")
        assertTrue(AppVersionGate.isForceUpdateRequired("3.1.0", raisedConfig))
        assertFalse(AppVersionGate.isForceUpdateRequired("3.2.0", raisedConfig))
        assertFalse(AppVersionGate.isForceUpdateRequired("3.5.0", raisedConfig))

        // Explicit emergency forceUpdateEnabled blocks even current version
        val forcedConfig = defaultConfig.copy(forceUpdateEnabled = true)
        assertTrue(AppVersionGate.isForceUpdateRequired("3.1.0", forcedConfig))

        // Safe URL resolver enforces https:// and falls back to https://t.me/Noor_ui
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl(""))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("javascript:alert(1)"))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("http://insecure.example"))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("https://t.me/Noor_ui"))
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
}
