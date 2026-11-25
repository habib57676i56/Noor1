package com.example.domain.quran

import androidx.compose.ui.graphics.Color

data class CalligraphyMasterpiece(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val titleUz: String,
    val calligraphyTextAr: String,
    val surahRefAr: String,
    val surahRefEn: String,
    val surahNumber: Int,
    val globalAyahNumber: Int
)

data class IlluminationTheme(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val goldAccent: Color,
    val secondaryGlow: Color,
    val inkColor: Color
)

/**
 * Interactive 3D Golden Islamic Calligraphy & Mamluk/Ottoman Illumination Studio Engine.
 * Provides authentic Quranic illumination presets (with verified global Ayah audio numbers)
 * and 5 museum-grade manuscript gold-leaf color palettes.
 */
object QuranCalligraphyEngine {

    val masterpieces: List<CalligraphyMasterpiece> = listOf(
        CalligraphyMasterpiece(
            "bismillah", "البسملة المباركة", "The Blessed Basmalah", "Muborak Basmala",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "سورة الفاتحة · الآية ١", "Al-Fatihah · Ayah 1",
            surahNumber = 1, globalAyahNumber = 1
        ),
        CalligraphyMasterpiece(
            "noor_ayah", "آية النور العظيمة", "The Verse of Light", "Nur oyati",
            "اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ ۚ مَثَلُ نُورِهِ كَمِشْكَاةٍ فِيهَا مِصْبَاحٌ",
            "سورة النور · الآية ٣٥", "An-Nur · Ayah 35",
            surahNumber = 24, globalAyahNumber = 2826
        ),
        CalligraphyMasterpiece(
            "kursi_opening", "آية الكرسي", "Ayat al-Kursi", "Oyatal Kursiy",
            "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ",
            "سورة البقرة · الآية ٢٥٥", "Al-Baqarah · Ayah 255",
            surahNumber = 2, globalAyahNumber = 262
        ),
        CalligraphyMasterpiece(
            "ikhlas", "سورة الإخلاص", "Surah Al-Ikhlas", "Ixlos surasi",
            "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ",
            "سورة الإخلاص · ١-٣", "Al-Ikhlas · 1-3",
            surahNumber = 112, globalAyahNumber = 6222
        ),
        CalligraphyMasterpiece(
            "fadhkuruni", "فاذكروني أذكركم", "Remember Me, I Will Remember You", "Meni zikr qiling",
            "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            "سورة البقرة · الآية ١٥٢", "Al-Baqarah · Ayah 152",
            surahNumber = 2, globalAyahNumber = 159
        ),
        CalligraphyMasterpiece(
            "inshirah", "رب اشرح لي صدري", "Expand For Me My Chest", "Qalbimni keng qil",
            "قَالَ رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي",
            "سورة طه · ٢٥-٢٦", "Ta-Ha · 25-26",
            surahNumber = 20, globalAyahNumber = 2373
        )
    )

    val themes: List<IlluminationTheme> = listOf(
        IlluminationTheme(
            "royal_emerald", "الزمرد الملكي وورق الذهب", "Royal Emerald & 24K Gold", "Qirollik zumradi va oltin",
            Color(0xFF04221B), Color(0xFF093A2E), Color(0xFF02110D),
            Color(0xFFF5D782), Color(0xFF2FE3AA), Color(0xFFFFF8E6)
        ),
        IlluminationTheme(
            "kiswah_gold", "كسوة الكعبة المشرفة", "Kaaba Midnight Kiswah", "Ka'ba kisvasi va oltin",
            Color(0xFF0B0B0D), Color(0xFF181512), Color(0xFF050507),
            Color(0xFFE8C060), Color(0xFFD99B26), Color(0xFFFFF6DC)
        ),
        IlluminationTheme(
            "andalus_sapphire", "لازورد الأندلس والحمراء", "Andalusian Lapis Lazuli", "Andalusiya lazuriti",
            Color(0xFF071633), Color(0xFF0E2954), Color(0xFF040D20),
            Color(0xFFF7D57E), Color(0xFF38BDF8), Color(0xFFF0F8FF)
        ),
        IlluminationTheme(
            "damascus_burgundy", "ياقوت المخطوطات العثمانية", "Ottoman Ruby Manuscript", "Usmoniy yoqut qo'lyozma",
            Color(0xFF280814), Color(0xFF451023), Color(0xFF16040A),
            Color(0xFFF8D683), Color(0xFFF472B6), Color(0xFFFFF5F0)
        ),
        IlluminationTheme(
            "amber_mashrabiya", "عنبر المشربية الذهبي", "Golden Amber Mashrabiya", "Oltin kahrabo naqsh",
            Color(0xFF241606), Color(0xFF3E260C), Color(0xFF140B03),
            Color(0xFFFFDF8E), Color(0xFFF59E0B), Color(0xFFFFFBE8)
        )
    )
}
