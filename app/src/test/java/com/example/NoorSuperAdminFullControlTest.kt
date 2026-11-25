package com.example

import com.example.data.cloud.CustomReciterDoc
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.UserAccountDoc
import com.example.data.quran.CustomReciterItem
import com.example.ui.theme.NoorThemePreset
import com.example.ui.theme.NoorVisualSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoorSuperAdminFullControlTest {

    @Test
    fun themePresets_allSixResolveAccuratelyWithFallback() {
        assertEquals(6, NoorThemePreset.entries.size)
        assertEquals(NoorThemePreset.EMERALD_GOLD, NoorThemePreset.fromKey("emerald_gold"))
        assertEquals(NoorThemePreset.SAPPHIRE_GOLD, NoorThemePreset.fromKey("sapphire_gold"))
        assertEquals(NoorThemePreset.ROYAL_AMETHYST, NoorThemePreset.fromKey("royal_amethyst"))
        assertEquals(NoorThemePreset.MAKKAH_OBSIDIAN, NoorThemePreset.fromKey("makkah_obsidian"))
        assertEquals(NoorThemePreset.DAMASCUS_RUBY, NoorThemePreset.fromKey("damascus_ruby"))
        assertEquals(NoorThemePreset.ANDALUS_OLIVE, NoorThemePreset.fromKey("andalus_olive"))
        assertEquals(NoorThemePreset.EMERALD_GOLD, NoorThemePreset.fromKey("unknown_preset"))
    }

    @Test
    fun globalAppConfig_defaultsEnableVisualEffectsAndAllFlagshipModules() {
        val cfg = GlobalAppConfigDoc()
        assertEquals(0.85, cfg.glassOpacity, 0.001)
        assertEquals(1.0, cfg.effectsIntensity, 0.001)
        assertTrue(cfg.effectsStarlightEnabled)
        assertTrue(cfg.effectsAuroraEnabled)
        assertTrue(cfg.effects3DSplashEnabled)
        assertTrue(cfg.featureEcosystemHubEnabled)
        assertTrue(cfg.featureCosmicGlobeEnabled)
        assertTrue(cfg.featureCalligraphyEnabled)
        assertTrue(cfg.featureSanctuaryAudioEnabled)
        assertTrue(cfg.featureAstrolabeEnabled)
        assertTrue(cfg.featureSoulRemedyEnabled)
        assertTrue(cfg.featureMiraclesProphetsEnabled)
        assertTrue(cfg.featureQuranUniverseEnabled)
        assertTrue(cfg.featureIrabSarfEnabled)
        assertTrue(cfg.featureHaramainLiveEnabled)
        assertTrue(cfg.featureZakatMirathEnabled)
        assertTrue(cfg.featureMutashabihatSeerahEnabled)
    }

    @Test
    fun userAccountDoc_holdsLiveLocationAndVerificationBadge() {
        val user = UserAccountDoc(
            uid = "u_100",
            email = "member@noor.app",
            displayName = "أحمد القارئ",
            verified = true,
            verifiedBadgeLabel = "قارئ موثّق",
            cityName = "Madinah",
            cityNameAr = "المدينة المنورة",
            latitude = 24.4672,
            longitude = 39.6111
        )
        assertTrue(user.verified)
        assertEquals("قارئ موثّق", user.verifiedBadgeLabel)
        assertEquals("المدينة المنورة", user.cityNameAr)
        assertEquals(24.4672, user.latitude, 0.0001)
        assertEquals(39.6111, user.longitude, 0.0001)
    }

    @Test
    fun customReciterItem_mapsCleanlyToPlayableAudioTrackItem() {
        val item = CustomReciterItem(
            id = "crec_demo01",
            nameAr = "الشيخ عبد الله النوري",
            nameEn = "Sheikh Abdullah Al-Nouri",
            styleAr = "مرتّل (حفص عن عاصم)",
            styleEn = "Murattal (Hafs)",
            surahTitleAr = "سورة الرحمن",
            surahTitleEn = "Surah Ar-Rahman",
            surahNumber = 55,
            localAudioPath = "",
            audioUrl = "https://cdn.example.com/rahman.mp3",
            fileName = "rahman.mp3"
        )
        assertTrue(item.hasPlayableAudio())
        assertEquals("https://cdn.example.com/rahman.mp3", item.resolvePlayableSource())
        val track = item.toAudioTrackItem()
        assertEquals("custom_reciter_crec_demo01", track.id)
        assertEquals(55, track.surahNumber)
        assertTrue(track.subtitleAr.contains("الشيخ عبد الله النوري"))
        assertEquals("سورة الرحمن", track.titleAr)

        val emptyReciter = item.copy(localAudioPath = "", audioUrl = "")
        assertFalse(emptyReciter.hasPlayableAudio())

        val doc = CustomReciterDoc(id = item.id, nameAr = item.nameAr, surahNumber = 55)
        assertEquals("crec_demo01", doc.id)
        assertTrue(doc.active)

        val visuals = NoorVisualSettings(themePreset = "sapphire_gold", glassOpacity = 0.65f, effectsStarlightEnabled = false)
        assertEquals("sapphire_gold", visuals.themePreset)
        assertFalse(visuals.effectsStarlightEnabled)
    }
}
