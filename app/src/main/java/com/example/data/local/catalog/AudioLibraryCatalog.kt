package com.example.data.local.catalog

import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.QuranReciter

/**
 * Legitimate, authorized open Islamic audio catalog.
 * - Full Surahs: MP3Quran.net official public streaming servers (including Sheikh Yasser Al-Dosari's complete 114-Surah Murattal collection).
 * - Verse-level playback: EveryAyah.com open Quran recitation archive.
 * - Adhan & Takbeerat: Public open Islamic audio archives (IslamCan / IslamicNetwork).
 */
object AudioLibraryCatalog {

    fun yasserAlDosariTracks(): List<AudioTrackItem> =
        surahTracksForReciter(QuranReciter.YASSER_AL_DOSARI, AudioCategory.YASSER_RECITATIONS)

    fun surahTracksForReciter(
        reciter: QuranReciter,
        category: AudioCategory = AudioCategory.QURAN_RECITERS
    ): List<AudioTrackItem> = SurahCatalog.allSurahs.map { surah ->
        AudioTrackItem(
            id = "${reciter.id}_surah_${surah.number}",
            titleAr = "سورة ${surah.nameAr}",
            titleEn = "Surah ${surah.nameEn}",
            subtitleAr = "${reciter.nameAr} · ${surah.verseCount} آية",
            subtitleEn = "${reciter.nameEn} · ${surah.verseCount} Ayahs",
            audioUrl = reciter.fullSurahUrl(surah.number),
            category = category,
            surahNumber = surah.number,
            reciterId = reciter.id,
            sourceAttribution = "MP3Quran.net Public Streaming Archive",
            allowOfflineCache = true
        )
    }

    val adhanAndTakbeeratTracks: List<AudioTrackItem> = listOf(
        AudioTrackItem(
            id = "adhan_makkah",
            titleAr = "أذان المسجد الحرام — مكة المكرمة",
            titleEn = "Adhan of Masjid al-Haram (Makkah)",
            subtitleAr = "الأذان المكي الكامل · بث مباشر",
            subtitleEn = "Traditional Makkah Adhan · Open Stream",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan1.mp3",
            category = AudioCategory.ADHAN_TAKBEERAT,
            sourceAttribution = "IslamCan Public Audio Archive",
            allowOfflineCache = false
        ),
        AudioTrackItem(
            id = "adhan_madinah",
            titleAr = "أذان المسجد النبوي — المدينة المنورة",
            titleEn = "Adhan of Al-Masjid an-Nabawi (Madinah)",
            subtitleAr = "الأذان المدني · بث مباشر",
            subtitleEn = "Traditional Madinah Adhan · Open Stream",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan2.mp3",
            category = AudioCategory.ADHAN_TAKBEERAT,
            sourceAttribution = "IslamCan Public Audio Archive",
            allowOfflineCache = false
        ),
        AudioTrackItem(
            id = "adhan_aqsa",
            titleAr = "أذان المسجد الأقصى المبارك",
            titleEn = "Adhan of Al-Aqsa Mosque",
            subtitleAr = "القدس الشريف · بث مباشر",
            subtitleEn = "Al-Quds Adhan · Open Stream",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan3.mp3",
            category = AudioCategory.ADHAN_TAKBEERAT,
            sourceAttribution = "IslamCan Public Audio Archive",
            allowOfflineCache = false
        ),
        AudioTrackItem(
            id = "adhan_egypt",
            titleAr = "الأذان المصري التراثي",
            titleEn = "Classic Egyptian Adhan",
            subtitleAr = "تلاوة أذان هادئة · بث مباشر",
            subtitleEn = "Classic Cairo Style · Open Stream",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan15.mp3",
            category = AudioCategory.ADHAN_TAKBEERAT,
            sourceAttribution = "IslamCan Public Audio Archive",
            allowOfflineCache = false
        ),
        AudioTrackItem(
            id = "takbeerat_eid",
            titleAr = "تكبيرات العيد — المسجد الحرام",
            titleEn = "Eid Takbeerat — Masjid al-Haram",
            subtitleAr = "الله أكبر الله أكبر لا إله إلا الله",
            subtitleEn = "Allahu Akbar, Allahu Akbar, La ilaha illAllah",
            audioUrl = "https://www.islamcan.com/audio/takbeer/takbeer-makkah.mp3",
            category = AudioCategory.ADHAN_TAKBEERAT,
            sourceAttribution = "IslamCan Public Audio Archive",
            allowOfflineCache = false
        )
    )

    /**
     * Authentic Quranic Supplications & Ruqyah / Remembrance recitations by Sheikh Yasser Al-Dosari (EveryAyah verified verses).
     */
    val curatedSupplicationRecitations: List<AudioTrackItem> = listOf(
        AudioTrackItem(
            id = "dua_kursi_yasser",
            titleAr = "آية الكرسي — ياسر الدوسري",
            titleEn = "Ayat al-Kursi — Yasser Al-Dosari",
            subtitleAr = "سورة البقرة · الآية ٢٥٥",
            subtitleEn = "Surah Al-Baqarah 2:255",
            audioUrl = QuranReciter.YASSER_AL_DOSARI.ayahAudioUrl(2, 255),
            category = AudioCategory.DUAS_DHIKR,
            surahNumber = 2,
            ayahNumber = 255,
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            sourceAttribution = "EveryAyah Open Archive",
            allowOfflineCache = true
        ),
        AudioTrackItem(
            id = "dua_rabbana_201",
            titleAr = "ربنا آتنا في الدنيا حسنة — ياسر الدوسري",
            titleEn = "Rabbana Atina fid-Dunya — Yasser Al-Dosari",
            subtitleAr = "سورة البقرة · الآية ٢٠١",
            subtitleEn = "Surah Al-Baqarah 2:201",
            audioUrl = QuranReciter.YASSER_AL_DOSARI.ayahAudioUrl(2, 201),
            category = AudioCategory.DUAS_DHIKR,
            surahNumber = 2,
            ayahNumber = 201,
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            sourceAttribution = "EveryAyah Open Archive",
            allowOfflineCache = true
        ),
        AudioTrackItem(
            id = "dua_baqarah_286",
            titleAr = "خواتيم سورة البقرة — ياسر الدوسري",
            titleEn = "Closing Dua of Al-Baqarah — Yasser Al-Dosari",
            subtitleAr = "سورة البقرة · الآية ٢٨٦",
            subtitleEn = "Surah Al-Baqarah 2:286",
            audioUrl = QuranReciter.YASSER_AL_DOSARI.ayahAudioUrl(2, 286),
            category = AudioCategory.DUAS_DHIKR,
            surahNumber = 2,
            ayahNumber = 286,
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            sourceAttribution = "EveryAyah Open Archive",
            allowOfflineCache = true
        ),
        AudioTrackItem(
            id = "dua_yunus_87",
            titleAr = "دعاء ذي النون — ياسر الدوسري",
            titleEn = "Supplication of Prophet Yunus — Yasser Al-Dosari",
            subtitleAr = "سورة الأنبياء · الآية ٨٧",
            subtitleEn = "Surah Al-Anbiya 21:87",
            audioUrl = QuranReciter.YASSER_AL_DOSARI.ayahAudioUrl(21, 87),
            category = AudioCategory.DUAS_DHIKR,
            surahNumber = 21,
            ayahNumber = 87,
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            sourceAttribution = "EveryAyah Open Archive",
            allowOfflineCache = true
        ),
        AudioTrackItem(
            id = "dua_ikhlas_yasser",
            titleAr = "سورة الإخلاص — ياسر الدوسري",
            titleEn = "Surah Al-Ikhlas — Yasser Al-Dosari",
            subtitleAr = "تعدل ثلث القرآن",
            subtitleEn = "Equivalent to one-third of the Quran",
            audioUrl = QuranReciter.YASSER_AL_DOSARI.fullSurahUrl(112),
            category = AudioCategory.DUAS_DHIKR,
            surahNumber = 112,
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            sourceAttribution = "MP3Quran.net Public Archive",
            allowOfflineCache = true
        ),
        AudioTrackItem(
            id = "dua_falaq_yasser",
            titleAr = "سورة الفلق — ياسر الدوسري",
            titleEn = "Surah Al-Falaq — Yasser Al-Dosari",
            subtitleAr = "المعوذتان · التحصين اليومي",
            subtitleEn = "Daily Protection Recitation",
            audioUrl = QuranReciter.YASSER_AL_DOSARI.fullSurahUrl(113),
            category = AudioCategory.DUAS_DHIKR,
            surahNumber = 113,
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            sourceAttribution = "MP3Quran.net Public Archive",
            allowOfflineCache = true
        ),
        AudioTrackItem(
            id = "dua_nas_yasser",
            titleAr = "سورة الناس — ياسر الدوسري",
            titleEn = "Surah An-Nas — Yasser Al-Dosari",
            subtitleAr = "المعوذتان · التحصين اليومي",
            subtitleEn = "Daily Protection Recitation",
            audioUrl = QuranReciter.YASSER_AL_DOSARI.fullSurahUrl(114),
            category = AudioCategory.DUAS_DHIKR,
            surahNumber = 114,
            reciterId = QuranReciter.YASSER_AL_DOSARI.id,
            sourceAttribution = "MP3Quran.net Public Archive",
            allowOfflineCache = true
        )
    )
}
