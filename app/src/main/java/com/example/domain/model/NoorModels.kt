package com.example.domain.model

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en");

    val isRtl: Boolean get() = this == ARABIC

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ARABIC
    }
}

enum class ThemeMode(val key: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: DARK
    }
}

enum class PrayerName(val key: String, val isFard: Boolean) {
    FAJR("fajr", true),
    SUNRISE("sunrise", false),
    DHUHR("dhuhr", true),
    ASR("asr", true),
    MAGHRIB("maghrib", true),
    ISHA("isha", true);

    companion object {
        val fardPrayers: List<PrayerName> = entries.filter { it.isFard }
        fun fromKey(key: String): PrayerName =
            entries.firstOrNull { it.key == key } ?: FAJR
    }
}

enum class CalculationMethod(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int = 0
) {
    UMM_AL_QURA("umm_al_qura", "أم القرى (مكة المكرمة)", "Umm al-Qura (Makkah)", 18.5, 0.0, 90),
    MWL("mwl", "رابطة العالم الإسلامي", "Muslim World League", 18.0, 17.0, 0),
    EGYPT("egypt", "الهيئة المصرية العامة للمساحة", "Egyptian General Authority", 19.5, 17.5, 0),
    KARACHI("karachi", "جامعة العلوم الإسلامية بكراتشي", "Univ. of Islamic Sciences, Karachi", 18.0, 18.0, 0),
    ISNA("isna", "الجمعية الإسلامية لأمريكا الشمالية", "ISNA (North America)", 15.0, 15.0, 0),
    DUBAI("dubai", "دائرة الشؤون الإسلامية بدبي", "Dubai / UAE", 18.2, 18.2, 0),
    QATAR("qatar", "قطر", "Qatar", 18.0, 0.0, 90),
    KUWAIT("kuwait", "الكويت", "Kuwait", 18.0, 17.5, 0),
    TURKEY("turkey", "رئاسة الشؤون الدينية التركية", "Diyanet (Turkey)", 18.0, 17.0, 0);

    companion object {
        fun fromKey(key: String?): CalculationMethod =
            entries.firstOrNull { it.key == key } ?: UMM_AL_QURA
    }
}

enum class AsrMadhhab(val key: String, val shadowFactor: Double, val nameAr: String, val nameEn: String) {
    SHAFI("shafi", 1.0, "الجمهور (الشافعي والمالكي والحنبلي)", "Standard (Shafi'i, Maliki, Hanbali)"),
    HANAFI("hanafi", 2.0, "المذهب الحنفي", "Hanafi");

    companion object {
        fun fromKey(key: String?): AsrMadhhab =
            entries.firstOrNull { it.key == key } ?: SHAFI
    }
}

data class CityLocation(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val countryAr: String,
    val countryEn: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String
)

data class PrayerMinuteOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
)

data class IqamahDelays(
    val enabled: Boolean = false,
    val mosqueName: String = "",
    val fajr: Int = 20,
    val dhuhr: Int = 15,
    val asr: Int = 15,
    val maghrib: Int = 10,
    val isha: Int = 15
) {
    fun delayFor(prayer: PrayerName): Int = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
        PrayerName.SUNRISE -> 0
    }
}

data class DailyPrayerSchedule(
    val dateKey: String,
    val epochDay: Long,
    val fajrMillis: Long,
    val sunriseMillis: Long,
    val dhuhrMillis: Long,
    val asrMillis: Long,
    val maghribMillis: Long,
    val ishaMillis: Long
) {
    fun timeFor(prayer: PrayerName): Long = when (prayer) {
        PrayerName.FAJR -> fajrMillis
        PrayerName.SUNRISE -> sunriseMillis
        PrayerName.DHUHR -> dhuhrMillis
        PrayerName.ASR -> asrMillis
        PrayerName.MAGHRIB -> maghribMillis
        PrayerName.ISHA -> ishaMillis
    }

    fun allOrdered(): List<Pair<PrayerName, Long>> = listOf(
        PrayerName.FAJR to fajrMillis,
        PrayerName.SUNRISE to sunriseMillis,
        PrayerName.DHUHR to dhuhrMillis,
        PrayerName.ASR to asrMillis,
        PrayerName.MAGHRIB to maghribMillis,
        PrayerName.ISHA to ishaMillis
    )
}

data class NextPrayerInfo(
    val nextPrayer: PrayerName,
    val nextPrayerMillis: Long,
    val previousPrayer: PrayerName,
    val previousPrayerMillis: Long,
    val remainingMillis: Long,
    val progress: Float
)

/**
 * All 24 Authentic Islamic Adhkar & Supplication Categories (NOOR 2.0)
 */
enum class AdhkarCategory(val id: String, val nameAr: String, val nameEn: String) {
    MORNING("morning", "أذكار الصباح", "Morning Adhkar"),
    EVENING("evening", "أذكار المساء", "Evening Adhkar"),
    AFTER_PRAYER("after_prayer", "أذكار بعد الصلاة", "After-Prayer Adhkar"),
    SLEEP("sleep", "أذكار النوم", "Sleeping Adhkar"),
    WAKING("waking", "أذكار الاستيقاظ", "Waking-Up Adhkar"),
    WUDU("wudu", "أذكار الوضوء", "Wudu Adhkar"),
    MOSQUE("mosque", "دخول وخروج المسجد", "Entering & Leaving the Mosque"),
    HOME("home", "دخول وخروج المنزل", "Entering & Leaving the Home"),
    FOOD_DRINK("food_drink", "أذكار الطعام والشراب", "Food & Drink Adhkar"),
    TRAVEL("travel", "أذكار السفر", "Travel Adhkar"),
    RIDING("riding", "أذكار الركوب والمواصلات", "Riding & Transportation"),
    ANXIETY("anxiety", "أدعية الكرب والهم والحزن", "Anxiety & Distress Duas"),
    ISTIGHFAR("istighfar", "الاستغفار والتوبة", "Istighfar & Repentance"),
    TASBEEH_DHIKR("tasbeeh_dhikr", "التسبيح والتحميد والتكبير والتهليل", "Tasbeeh, Tahmeed, Takbeer & Tahlil"),
    QURANIC_DUA("quranic_dua", "الأدعية القرآنية", "Quranic Duas"),
    PROPHETIC_DUA("prophetic_dua", "الأدعية النبوية الصحيحة", "Prophetic Duas"),
    FRIDAY("friday", "أذكار وسنن يوم الجمعة", "Friday Adhkar"),
    HAJJ_UMRAH("hajj_umrah", "أدعية الحج والعمرة", "Hajj & Umrah Duas"),
    RAIN_WEATHER("rain_weather", "أذكار المطر والريح والرعد", "Rain & Weather Adhkar"),
    VISITING_SICK("visiting_sick", "أذكار عيادة المريض", "Visiting the Sick"),
    CONDOLENCE("condolence", "أدعية التعزية والجنازة", "Condolence Duas"),
    BATHROOM("bathroom", "دخول وخروج الخلاء", "Entering & Leaving the Bathroom"),
    ADHAN("adhan", "أذكار سماع الأذان", "Hearing the Adhan"),
    GENERAL_SUPPLICATIONS("general_supplications", "أدعية إسلامية جامعة", "General Authentic Supplications");

    companion object {
        fun fromId(id: String): AdhkarCategory = when (id) {
            // Backward compatibility with v1.0 route id
            "home_mosque" -> HOME
            else -> entries.firstOrNull { it.id == id } ?: MORNING
        }
    }
}

data class DhikrItem(
    val id: String,
    val category: AdhkarCategory,
    val textAr: String,
    val targetCount: Int,
    val sourceAr: String,
    val sourceEn: String,
    val gradingAr: String = "صحيح",
    val gradingEn: String = "Sahih",
    val meaningAr: String,
    val meaningEn: String,
    val transliteration: String = ""
)

data class AuthenticHadith(
    val id: String,
    val textAr: String,
    val textEn: String,
    val narratorAr: String,
    val narratorEn: String,
    val sourceAr: String,
    val sourceEn: String,
    val gradingAr: String,
    val gradingEn: String
)

data class TasbihPreset(
    val id: String,
    val textAr: String,
    val textEn: String,
    val defaultTarget: Int,
    val isCustom: Boolean = false
)

enum class QuranReadingTheme(val key: String) {
    AUTO("auto"),
    SEPIA("sepia"),
    NIGHT("night"),
    LIGHT("light");

    companion object {
        fun fromKey(key: String?): QuranReadingTheme =
            entries.firstOrNull { it.key == key } ?: AUTO
    }
}

enum class QuranDisplayMode(val key: String) {
    STUDY("study"),
    MUSHAF("mushaf"),
    MEMORIZE("memorize");

    companion object {
        fun fromKey(key: String?): QuranDisplayMode =
            entries.firstOrNull { it.key == key } ?: STUDY
    }
}

enum class QuranReciter(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val styleAr: String,
    val styleEn: String,
    val cdnSlug: String,
    val everyAyahFolder: String,
    val mp3QuranBaseUrl: String,
    val isFeatured: Boolean = false
) {
    YASSER_AL_DOSARI(
        id = "yasser_dosari",
        nameAr = "الشيخ ياسر الدوسري",
        nameEn = "Sheikh Yasser Al-Dosari",
        styleAr = "إمام المسجد الحرام · مرتل",
        styleEn = "Imam of Masjid al-Haram · Murattal",
        cdnSlug = "ar.alafasy",
        everyAyahFolder = "Yasser_Ad-Dussary_128kbps",
        mp3QuranBaseUrl = "https://server11.mp3quran.net/yasser",
        isFeatured = true
    ),
    ALAFASY(
        id = "alafasy",
        nameAr = "مشاري راشد العفاسي",
        nameEn = "Mishary Rashid Alafasy",
        styleAr = "تلاوة مرتلة · الكويت",
        styleEn = "Murattal · Kuwait",
        cdnSlug = "ar.alafasy",
        everyAyahFolder = "Alafasy_128kbps",
        mp3QuranBaseUrl = "https://server8.mp3quran.net/afs"
    ),
    HUSARY(
        id = "husary",
        nameAr = "محمود خليل الحصري",
        nameEn = "Mahmoud Khalil Al-Husary",
        styleAr = "المصحف المرتل · مصر",
        styleEn = "Classic Murattal · Egypt",
        cdnSlug = "ar.husary",
        everyAyahFolder = "Husary_128kbps",
        mp3QuranBaseUrl = "https://server13.mp3quran.net/husr"
    ),
    MINSHAWI(
        id = "minshawi",
        nameAr = "محمد صديق المنشاوي",
        nameEn = "Muhammad Siddiq Al-Minshawi",
        styleAr = "المصحف المرتل · مصر",
        styleEn = "Murattal · Egypt",
        cdnSlug = "ar.minshawi",
        everyAyahFolder = "Minshawy_Murattal_128kbps",
        mp3QuranBaseUrl = "https://server10.mp3quran.net/minsh"
    ),
    MAHER_AL_MUAIQLY(
        id = "maher",
        nameAr = "ماهر المعيقلي",
        nameEn = "Maher Al-Muaiqly",
        styleAr = "إمام المسجد الحرام · مرتل",
        styleEn = "Imam of Masjid al-Haram · Murattal",
        cdnSlug = "ar.mahermuaiqly",
        everyAyahFolder = "MaherAlMuaiqly128kbps",
        mp3QuranBaseUrl = "https://server12.mp3quran.net/maher"
    ),
    ABDUL_BASIT(
        id = "abdul_basit",
        nameAr = "عبد الباسط عبد الصمد",
        nameEn = "Abdul Basit Abdus Samad",
        styleAr = "المصحف المرتل · مصر",
        styleEn = "Murattal · Egypt",
        cdnSlug = "ar.abdulbasitmurattal",
        everyAyahFolder = "Abdul_Basit_Murattal_192kbps",
        mp3QuranBaseUrl = "https://server7.mp3quran.net/basit"
    );

    fun fullSurahUrl(surahNumber: Int): String {
        val padded = surahNumber.toString().padStart(3, '0')
        return "$mp3QuranBaseUrl/$padded.mp3"
    }

    fun ayahAudioUrl(surahNumber: Int, ayahNumber: Int): String {
        val s = surahNumber.toString().padStart(3, '0')
        val a = ayahNumber.toString().padStart(3, '0')
        return "https://everyayah.com/data/$everyAyahFolder/${s}${a}.mp3"
    }

    companion object {
        fun fromId(id: String?): QuranReciter =
            entries.firstOrNull { it.id == id } ?: YASSER_AL_DOSARI
    }
}

enum class AudioCategory(val key: String, val nameAr: String, val nameEn: String) {
    YASSER_RECITATIONS("yasser", "تلاوات ياسر الدوسري", "Yasser Al-Dosari"),
    QURAN_RECITERS("reciters", "قراء القرآن", "Quran Reciters"),
    ADHAN_TAKBEERAT("adhan", "الأذان والتكبيرات", "Adhan & Takbeerat"),
    DUAS_DHIKR("duas", "الأدعية والأذكار الصوتية", "Supplications & Dhikr")
}

data class AudioTrackItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val audioUrl: String,
    val category: AudioCategory,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val reciterId: String? = null,
    val sourceAttribution: String = "MP3Quran / EveryAyah Open Archive",
    val allowOfflineCache: Boolean = true
)

enum class AudioRepeatMode {
    OFF,
    REPEAT_ONE,
    REPEAT_ALL
}

data class SurahMeta(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val meaningEn: String,
    val verseCount: Int,
    val isMeccan: Boolean,
    val startJuz: Int,
    val startHizb: Int,
    val startPage: Int = 1
)

data class QuranVerse(
    val surahNumber: Int,
    val numberInSurah: Int,
    val globalNumber: Int,
    val textUthmani: String,
    val tafsirAr: String,
    val audioUrl: String,
    val translationEn: String = "",
    val pageNumber: Int = 1,
    val juzNumber: Int = 1,
    val hizbQuarter: Int = 1,
    val wordMeaningsAr: String = "",
    val asbabAlNuzulAr: String = "",
    val asbabAlNuzulEn: String = ""
)

data class HijriDateModel(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..30
    val monthNameAr: String,
    val monthNameEn: String
)

data class IslamicOccasion(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val daysUntil: Int,
    val isFastingRecommended: Boolean = false
)

data class AchievementItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val unlocked: Boolean,
    val progress: Float
)

data class NearbyMosque(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val bearingDegrees: Double,
    val address: String = ""
)

data class HomeDashboardConfig(
    val showPrayerStrip: Boolean = true,
    val showAdhkarShortcut: Boolean = true,
    val showQuranProgress: Boolean = true,
    val showDailyAyah: Boolean = true,
    val showDailyHadith: Boolean = true,
    val showRecentAudio: Boolean = true,
    val showUpcomingEvent: Boolean = true
)
