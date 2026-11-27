package com.example

import com.example.data.cloud.AppVersionGate
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorPlatformConstants
import com.example.data.quran.QuranWordTimingRepository
import com.example.data.remote.OverpassApiService
import com.example.domain.model.AppLanguage
import com.example.domain.model.QuranReciter
import com.example.ui.components.Ios27LiquidGlassDockMath
import com.example.ui.screens.ecosystem.AsbabAlNuzulCatalog
import com.example.ui.screens.ecosystem.HifzSpacedRepetitionEngine
import com.example.ui.screens.ecosystem.LunarAstronomyEngine
import com.example.ui.screens.ecosystem.MakhrajZone
import com.example.ui.screens.ecosystem.NasaMoonSatelliteMath
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
        assertEquals("4.0.0", NoorPlatformConstants.APP_VERSION_NAME)
        assertEquals("v4.0.0", NoorPlatformConstants.APP_VERSION_TAG)

        // New build v4.0.0 passes default v4.0.0 gate, while older v3.9.0 / v3.8.0 APKs are blocked!
        assertFalse(AppVersionGate.isForceUpdateRequired("4.0.0", defaultConfig))
        assertTrue(AppVersionGate.isForceUpdateRequired("3.9.0", defaultConfig))
        assertTrue(AppVersionGate.isForceUpdateRequired("3.8.0", defaultConfig))

        // CRITICAL AUTO-DISMISS AFTER UPDATE: even if forceUpdateEnabled was left ON in Firestore
        // with minSupportedVersion = "4.0.0", the newly updated v4.0.0 (or v4.1.0) APK opens automatically
        // while older v3.9.0 remains blocked!
        val forcedWithMin = defaultConfig.copy(forceUpdateEnabled = true, minSupportedVersion = "4.0.0")
        assertTrue(AppVersionGate.isForceUpdateRequired("3.9.0", forcedWithMin))
        assertFalse(AppVersionGate.isForceUpdateRequired("4.0.0", forcedWithMin))
        assertFalse(AppVersionGate.isForceUpdateRequired("4.1.0", forcedWithMin))

        // Raising minSupportedVersion to 4.1.0 blocks 4.0.0 until user updates to 4.1.0
        val raisedConfig = defaultConfig.copy(minSupportedVersion = "4.1.0")
        assertTrue(AppVersionGate.isForceUpdateRequired("4.0.0", raisedConfig))
        assertFalse(AppVersionGate.isForceUpdateRequired("4.1.0", raisedConfig))

        // Safe URL resolver enforces https:// and falls back to https://t.me/Noor_ui
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl(""))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("javascript:alert(1)"))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("http://insecure.example"))
        assertEquals("https://t.me/Noor_ui", AppVersionGate.resolveSafeUpdateUrl("https://t.me/Noor_ui"))
    }

    @Test
    fun systemLocaleAutoDetection_mapsDeviceLanguageDirectlyOnFirstLaunch() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("ar-SA")))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("ar-SY")))
        assertEquals(AppLanguage.UZBEK, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("uz-UZ")))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("ru-RU")))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("ru")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromSystemLocale(java.util.Locale.US))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromSystemLocale(java.util.Locale.FRANCE))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
        assertEquals(AppLanguage.UZBEK, AppLanguage.fromCode("uz"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("ru"))
        assertFalse(AppLanguage.RUSSIAN.isRtl)
    }

    @Test
    fun overpassMosqueFinder_buildsExpandedQueryAndDeduplicatesNearbyMosques() {
        val q = OverpassApiService.buildOverpassQuery(21.4225, 39.8262, 5000)
        assertTrue(q.contains("""nwr["amenity"="place_of_worship"]["religion"="muslim"]"""))
        assertTrue(q.contains("""nwr["building"="mosque"]"""))
        assertTrue(OverpassApiService.OVERPASS_MIRRORS.size >= 3)

        val mosques = listOf(
            com.example.domain.model.NearbyMosque(
                id = 2L,
                name = "مسجد / Mosque",
                latitude = 21.42305,
                longitude = 39.82652,
                distanceMeters = OverpassApiService.haversineMeters(21.4225, 39.8262, 21.42305, 39.82652),
                bearingDegrees = OverpassApiService.bearingDegrees(21.4225, 39.8262, 21.42305, 39.82652),
                address = ""
            ),
            com.example.domain.model.NearbyMosque(
                id = 1L,
                name = "مسجد النور",
                latitude = 21.4230,
                longitude = 39.8265,
                distanceMeters = OverpassApiService.haversineMeters(21.4225, 39.8262, 21.4230, 39.8265),
                bearingDegrees = OverpassApiService.bearingDegrees(21.4225, 39.8262, 21.4230, 39.8265),
                address = "شارع الملك"
            ),
            com.example.domain.model.NearbyMosque(
                id = 3L,
                name = "Masjid Al-Rahmah",
                latitude = 21.4300,
                longitude = 39.8300,
                distanceMeters = OverpassApiService.haversineMeters(21.4225, 39.8262, 21.4300, 39.8300),
                bearingDegrees = OverpassApiService.bearingDegrees(21.4225, 39.8262, 21.4300, 39.8300),
                address = ""
            )
        )
        val deduped = OverpassApiService.deduplicateAndSort(mosques)
        assertEquals(2, deduped.size)
        assertEquals("مسجد النور", deduped.first().name)
        assertTrue(deduped[0].distanceMeters < deduped[1].distanceMeters)
    }

    @Test
    fun nasaMoonSatelliteMath_buildsValidTrekWmtsAndDialAMoonUrls() {
        assertEquals(2, NasaMoonSatelliteMath.trekCols(0))
        assertEquals(1, NasaMoonSatelliteMath.trekRows(0))
        assertEquals(4, NasaMoonSatelliteMath.trekCols(1))
        assertEquals(2, NasaMoonSatelliteMath.trekRows(1))

        val z0Left = NasaMoonSatelliteMath.nasaTrekTileUrl(0, 0, 0)
        assertTrue(z0Left.startsWith("https://trek.nasa.gov/tiles/Moon/EQ/LRO_WAC_Mosaic_Global_303ppd_v02/"))
        assertTrue(z0Left.endsWith("/0/0/0.jpg"))

        val opmUrl = NasaMoonSatelliteMath.openPlanetaryTileUrl(2, 1, 1)
        assertTrue(opmUrl.startsWith("https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/"))

        val tychoTile = NasaMoonSatelliteMath.latLonToTrekTile(-43.3, -11.2, 2)
        assertEquals(2, tychoTile.zoom)
        assertTrue(tychoTile.col in 0 until 8)
        assertTrue(tychoTile.row in 0 until 4)

        val (iso, frameIdx) = NasaMoonSatelliteMath.buildNasaDialAMoonQuery(1780747200000L) // 2026-06-06T12:00 UTC
        assertTrue(iso.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:00")))
        assertTrue(frameIdx in 1..8760)
        assertTrue(NasaMoonSatelliteMath.directSvs2026FrameUrl(frameIdx).contains("/a005587/"))
    }

    @Test
    fun ios27LiquidGlassDockMath_mapsPointerAccuratelyInBothRtlAndLtr() {
        val dockWidth = 500f
        val count = 5

        // LTR: tab 0 is left (x=50), tab 4 is right (x=450)
        assertEquals(50f, Ios27LiquidGlassDockMath.visualCenterXForTab(0, count, dockWidth, isRtl = false), 0.01f)
        assertEquals(450f, Ios27LiquidGlassDockMath.visualCenterXForTab(4, count, dockWidth, isRtl = false), 0.01f)
        assertEquals(0, Ios27LiquidGlassDockMath.tabIndexForPointerX(40f, count, dockWidth, isRtl = false))
        assertEquals(2, Ios27LiquidGlassDockMath.tabIndexForPointerX(250f, count, dockWidth, isRtl = false))
        assertEquals(4, Ios27LiquidGlassDockMath.tabIndexForPointerX(460f, count, dockWidth, isRtl = false))

        // RTL (Arabic): tab 0 ("الرئيسية") is on the RIGHT (x=450), tab 4 ("المسبحة") is on the LEFT (x=50)
        assertEquals(450f, Ios27LiquidGlassDockMath.visualCenterXForTab(0, count, dockWidth, isRtl = true), 0.01f)
        assertEquals(50f, Ios27LiquidGlassDockMath.visualCenterXForTab(4, count, dockWidth, isRtl = true), 0.01f)
        assertEquals(0, Ios27LiquidGlassDockMath.tabIndexForPointerX(460f, count, dockWidth, isRtl = true))
        assertEquals(2, Ios27LiquidGlassDockMath.tabIndexForPointerX(250f, count, dockWidth, isRtl = true))
        assertEquals(4, Ios27LiquidGlassDockMath.tabIndexForPointerX(40f, count, dockWidth, isRtl = true))

        // Optical magnification proximity peaks at 1.0 when droplet is directly over the tab
        assertEquals(1f, Ios27LiquidGlassDockMath.lensProximity(250f, 250f, 100f), 0.001f)
        assertEquals(0f, Ios27LiquidGlassDockMath.lensProximity(50f, 250f, 100f), 0.001f)
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

    @Test
    fun russianLocalization_translatesPrayersAndCoreNavigationCleanly() {
        assertEquals("Фаджр", com.example.ui.i18n.L10n.prayerName(com.example.domain.model.PrayerName.FAJR, AppLanguage.RUSSIAN))
        assertEquals("Зухр", com.example.ui.i18n.L10n.prayerName(com.example.domain.model.PrayerName.DHUHR, AppLanguage.RUSSIAN))
        assertEquals("Магриб", com.example.ui.i18n.L10n.prayerName(com.example.domain.model.PrayerName.MAGHRIB, AppLanguage.RUSSIAN))
        assertEquals("Настройки", com.example.ui.i18n.L10n.pick(AppLanguage.RUSSIAN, "الإعدادات", "Settings"))
        assertEquals("Тасбих", com.example.ui.i18n.L10n.pick(AppLanguage.RUSSIAN, "المسبحة", "Tasbih"))
        assertEquals("Ближайшие мечети", com.example.ui.i18n.L10n.pick(AppLanguage.RUSSIAN, "المساجد القريبة", "Nearby Mosques"))
    }

    @Test
    fun madinahMushafPage2AndBismillahQueue_fixAlBaqarahOffsetAndYasserDosariShortSurahs() {
        // 1. Page 2 (Al-Baqarah 2:1 الم) must start on slot line 3 BELOW Bismillah line 2 (NOT collapsed to SPECIAL_P12_TOP!)
        val page1 = com.example.data.quran.MushafCoordinateRepository.buildVerifiedBundledPageLayout(1)!!
        val page2 = com.example.data.quran.MushafCoordinateRepository.buildVerifiedBundledPageLayout(2)!!
        val fatiha1_2 = page1.findAyah(1, 2)!!.lineRegions.first().bounds // line 3 on page 1
        val baqarah2_1 = page2.findAyah(2, 1)!!.lineRegions.first().bounds // line 3 on page 2
        assertEquals(
            "Page 2 Ayah 2:1 (الم) must align at exact line 3 vertical level matching Page 1 Ayah 1:2",
            fatiha1_2.top,
            baqarah2_1.top,
            0.003f
        )
        assertTrue(
            "Page 2 must reserve line 2 as Bismillah header line",
            page2.surahHeaderLines.contains(2)
        )
        // Standard 15-line pages align tightly to width_1024 PNGs (no 1-line vertical lag)
        assertTrue(com.example.data.quran.MushafCoordinateRepository.STANDARD_TOP <= 0.03f)
        assertTrue(com.example.data.quran.MushafCoordinateRepository.STANDARD_BOTTOM >= 0.97f)

        // 2. Sheikh Yasser Al-Dosari continuous queue MUST insert Bismillah (001001.mp3, ayahNumber=0) before short Surahs (112, 113, 114),
        //    and NEVER before Surah 1 (Al-Fatihah) or Surah 9 (At-Tawbah)!
        val ikhlasTracks = com.example.viewmodel.NoorViewModel.buildSurahTracksWithBismillah(
            surahNumber = 112,
            verses = listOf(
                com.example.domain.model.QuranVerse(112, 1, 6222, "قُلْ هُوَ اللَّهُ أَحَدٌ", "", "", pageNumber = 604),
                com.example.domain.model.QuranVerse(112, 2, 6223, "اللَّهُ الصَّمَدُ", "", "", pageNumber = 604)
            ),
            reciter = QuranReciter.YASSER_AL_DOSARI,
            includeOpeningBismillah = true
        )
        assertEquals(3, ikhlasTracks.size)
        assertEquals(0, ikhlasTracks.first().ayahNumber)
        assertTrue(ikhlasTracks.first().audioUrl.contains("Yasser_Ad-Dussary_128kbps/001001.mp3"))
        assertEquals(1, ikhlasTracks[1].ayahNumber)

        val tawbahTracks = com.example.viewmodel.NoorViewModel.buildSurahTracksWithBismillah(
            surahNumber = 9,
            verses = listOf(com.example.domain.model.QuranVerse(9, 1, 1236, "بَرَاءَةٌ", "", "", pageNumber = 187)),
            reciter = QuranReciter.YASSER_AL_DOSARI,
            includeOpeningBismillah = true
        )
        assertEquals(1, tawbahTracks.size)
        assertEquals(1, tawbahTracks.first().ayahNumber)
    }

    @Test
    fun mushafV4_seamlessConnectedMultiLineHighlightAndPage12ExactAlignment() {
        // 1. Seamless multi-line Ayah highlight MUST have ZERO vertical gap between consecutive lines
        val fitted = com.example.data.quran.FittedImageRect(left = 0f, top = 0f, width = 1000f, height = 1600f)
        val multiLineRegions = listOf(
            com.example.data.quran.AyahLineRegion(
                surahNumber = 3,
                ayahNumber = 7,
                lineNumber = 8,
                bounds = com.example.data.quran.NormalizedRect(left = 0.05f, top = 0.48f, right = 0.14f, bottom = 0.535f)
            ),
            com.example.data.quran.AyahLineRegion(
                surahNumber = 3,
                ayahNumber = 7,
                lineNumber = 9,
                bounds = com.example.data.quran.NormalizedRect(left = 0.05f, top = 0.545f, right = 0.95f, bottom = 0.600f)
            ),
            com.example.data.quran.AyahLineRegion(
                surahNumber = 3,
                ayahNumber = 7,
                lineNumber = 10,
                bounds = com.example.data.quran.NormalizedRect(left = 0.05f, top = 0.610f, right = 0.95f, bottom = 0.665f)
            ),
            com.example.data.quran.AyahLineRegion(
                surahNumber = 3,
                ayahNumber = 7,
                lineNumber = 11,
                bounds = com.example.data.quran.NormalizedRect(left = 0.62f, top = 0.675f, right = 0.95f, bottom = 0.730f)
            )
        )
        val connected = com.example.ui.screens.buildSeamlessConnectedHighlightRects(
            lineRegions = multiLineRegions,
            endMarkerBounds = null,
            fitted = fitted
        )
        assertEquals(4, connected.size)
        for (i in 0 until connected.lastIndex) {
            assertTrue(
                "Expected zero gap between line ${i + 8} bottom (${connected[i].bottom}) and line ${i + 9} top (${connected[i + 1].top})",
                connected[i].bottom >= connected[i + 1].top
            )
        }

        // 2. Page 1 & Page 2 inner frame starts below the gilded Surah banner (>= 0.34f),
        //    Al-Fatihah 1:1 (Bismillah) exists on line 2 and is highlightable, and Page 2 Ayah 2:1 (الم) sits on the right margin
        assertTrue(com.example.data.quran.MushafCoordinateRepository.SPECIAL_P12_TOP >= 0.34f)
        val page1 = com.example.data.quran.MushafCoordinateRepository.buildVerifiedBundledPageLayout(1)!!
        val fatiha1_1 = page1.findAyah(1, 1)!!
        assertEquals(2, fatiha1_1.lineRegions.first().lineNumber)
        assertTrue(fatiha1_1.lineRegions.first().bounds.top >= 0.34f)

        val page2 = com.example.data.quran.MushafCoordinateRepository.buildVerifiedBundledPageLayout(2)!!
        val baqarah2_1 = page2.findAyah(2, 1)!!
        assertEquals(3, baqarah2_1.lineRegions.first().lineNumber)
        assertTrue(
            "Page 2 Ayah 2:1 (الم) should align near the right margin of the inner frame",
            baqarah2_1.lineRegions.first().bounds.right >= 0.80f
        )

        // Default highlight style uses calm Quran-for-Android sage/emerald without distracting pulse
        val defaultStyle = com.example.data.quran.MushafHighlightStyle()
        assertEquals(com.example.data.quran.MushafHighlightColor.EMERALD, defaultStyle.colorPreset)
        assertFalse(defaultStyle.pulseAnimated)
    }

    @Test
    fun mushafV4_continuousProportionalBismillahLeadIn() {
        // Continuous proportional fallback preserves opening Bismillah lead-in cleanly (0..4200ms = Bismillah, 4200ms+ = Ayah 1)
        val proportional = QuranWordTimingRepository.buildProportionalSurahManifest(
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            surahNumber = 112,
            audioSource = "https://server11.mp3quran.net/yasser/112.mp3",
            ayahWeights = listOf(1 to 25, 2 to 25, 3 to 25, 4 to 25),
            totalDurationMs = 24_200L,
            openingBismillahLeadMs = 4_200L
        )
        assertEquals(4_200L, proportional.ayahs.first().startMs)
        assertEquals(null, proportional.findAyahAt(2_000L))
        assertEquals(1, proportional.findAyahAt(5_000L)?.ayahNumber)
    }
}
