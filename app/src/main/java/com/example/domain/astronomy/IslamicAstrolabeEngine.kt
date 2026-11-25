package com.example.domain.astronomy

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class ArabicFixedStar(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val constellationAr: String,
    val constellationEn: String,
    val raHours: Double,   // Right Ascension in hours (0..24)
    val decDeg: Double,    // Declination in degrees (-90..+90)
    val magnitude: Double, // Apparent visual magnitude (lower = brighter)
    val heritageNoteAr: String,
    val heritageNoteEn: String,
    val heritageNoteUz: String
)

data class StarLivePosition(
    val star: ArabicFixedStar,
    val altitudeDeg: Double, // -90..+90 (> 0 = above horizon)
    val azimuthDeg: Double,  // 0..360 clockwise from North
    val isVisibleNow: Boolean
)

data class AstrolabeSkySnapshot(
    val localSiderealTimeHours: Double,
    val sunAltitudeDeg: Double,
    val sunAzimuthDeg: Double,
    val skyPhaseAr: String,
    val skyPhaseEn: String,
    val skyPhaseUz: String,
    val stars: List<StarLivePosition>
)

/**
 * Golden-Age Islamic Astrolabe & Fixed Stars Astronomy Engine.
 * Computes real-time Greenwich & Local Sidereal Time (LST), solar altitude/azimuth,
 * and live horizon coordinates (Altitude & Azimuth) for 15 historic Arabic-named stars
 * from Abd al-Rahman al-Sufi's Kitab Suwar al-Kawakib (Book of Fixed Stars, 964 CE).
 */
object IslamicAstrolabeEngine {

    val arabicFixedStars: List<ArabicFixedStar> = listOf(
        ArabicFixedStar(
            "sirius", "الشِّعْرَى اليمانية", "Sirius (Ash-Shi'ra)", "Shi'ro yulduzi (Sirius)",
            "الكلب الأكبر", "Canis Major", 6.7525, -16.7161, -1.46,
            "ألمع نجم في سماء الليل قاطبة، ذُكر بالاسم في القرآن الكريم: ﴿وَأَنَّهُ هُوَ رَبُّ الشِّعْرَىٰ﴾ (النجم: ٤٩).",
            "Brightest star in the night sky, explicitly named in the Holy Quran: 'And He is the Lord of Sirius' (An-Najm 53:49).",
            "Tungi osmondagi eng yorug' yulduz; Qur'oni Karimda (Najm surasi, 49-oyat) nomi zikr qilingan."
        ),
        ArabicFixedStar(
            "canopus", "سُهَيْل اليماني", "Canopus (Suhayl)", "Suhayl yulduzi",
            "القاعدة", "Carina", 6.3992, -52.6957, -0.74,
            "ثاني ألمع نجم في السماء؛ استدلّ به العرب في الترحال ومعرفة انكسار حرّ الصيف وبداية الاعتدال.",
            "Second brightest star; Arab navigators used Suhayl for desert navigation and to mark the easing of summer heat.",
            "Osmondagi ikkinchi eng yorug' yulduz; qadimgi arablar safarda va fasl almashishida undan foydalangan."
        ),
        ArabicFixedStar(
            "arcturus", "السِّمَاك الرامح", "Arcturus (As-Simak)", "Simok ar-Romih",
            "العوّاء", "Boötes", 14.2610, 19.1824, -0.05,
            "ألمع نجم في نصف الكرة الشمالي، وُصف في جداول الصوفي والبتاني بـ«حارس الشمال».",
            "Brightest star in the northern celestial hemisphere, charted in Al-Sufi's and Al-Battani's star tables.",
            "Shimoliy yarim shardagi eng yorug' yulduz; As-So'fiy va Al-Battoniy jadvallarida qayd etilgan."
        ),
        ArabicFixedStar(
            "vega", "النَّسْر الواقع", "Vega (An-Nasr al-Waqi')", "Nasr al-Voqi' (Vega)",
            "القيثارة", "Lyra", 18.6156, 38.7837, 0.03,
            "اسم Vega مشتق مباشرة من الكلمة العربية «الواقع» أي النسر الذي ضمّ جناحيه للهبوط.",
            "The international name 'Vega' derives directly from Arabic 'al-Waqi'' (the swooping eagle).",
            "Vega nomi bevosita arabcha «al-Voqi'» (qanotini yig'ib qo'nayotgan burgut) so'zidan olingan."
        ),
        ArabicFixedStar(
            "capella", "العَيُّوق", "Capella (Al-Ayyuq)", "Ayyuq yulduzi",
            "ممسك الأعنة", "Auriga", 5.2782, 45.9980, 0.08,
            "نجم ذهبي ساطع يُرى عالياً في سماء الشتاء، استعان به البحارة المسلمون لتقدير خط العرض.",
            "Radiant golden star prominent in winter skies, used by Muslim mariners to verify northern latitude.",
            "Qish osmonida yorqin porlaydigan oltin rangli yulduz; dengizchilar kenglikni aniqlashda ishlatgan."
        ),
        ArabicFixedStar(
            "rigel", "رِجْل الجبّار", "Rigel (Rijl al-Jabbar)", "Rijl al-Jabbor",
            "الجوزاء (الجبار)", "Orion", 5.2423, -8.2016, 0.13,
            "عملاق أزرق فائق، اسمه العالمي Rigel مشتق من «رِجل الجبار» في التراث الفلكي العربي.",
            "Blue supergiant whose global name Rigel comes from Arabic 'Rijl al-Jabbar' (foot of the giant).",
            "Moviy gigant yulduz; Rigel nomi arabcha «Rijl al-Jabbor» atamasidan kelib chiqqan."
        ),
        ArabicFixedStar(
            "betelgeuse", "مَنْكِب الجوزاء", "Betelgeuse (Yad al-Jawza')", "Mankib al-Javzo",
            "الجوزاء (الجبار)", "Orion", 5.9195, 7.4071, 0.42,
            "عملاق أحمر مهيب؛ اشتُق اسمه العالمي من «يد الجوزاء» في المخطوطات الأندلسية.",
            "Red supergiant whose international name evolved from Arabic 'Yad al-Jawza'' in Andalusian manuscripts.",
            "Ulkan qizil yulduz; nomi Andalusiya qo'lyozmalaridagi «Yad al-Javzo» so'zidan olingan."
        ),
        ArabicFixedStar(
            "altair", "النَّسْر الطائر", "Altair (An-Nasr at-Ta'ir)", "Nasr at-Toir (Altair)",
            "العقاب", "Aquila", 19.8464, 8.8683, 0.76,
            "اسمه العالمي Altair مأخوذ حرفياً من «الطائر»، وهو أحد أركان مثلث الصيف الفلكي.",
            "Its name 'Altair' comes verbatim from Arabic 'at-Ta'ir' (the flying eagle), crowning the Summer Triangle.",
            "Altair nomi aynan arabcha «at-Toir» (uchuvchi burgut) so'zidan olingan."
        ),
        ArabicFixedStar(
            "aldebaran", "الدَّبَرَان", "Aldebaran (Ad-Dabaran)", "Dabaron yulduzi",
            "الثور", "Taurus", 4.5987, 16.5093, 0.86,
            "أحد أشهر منازل القمر الـ٢٨؛ سُمّي «الدبران» لأنه يدبر أي يتبع عنقود الثريا في السماء.",
            "One of the famous 28 Lunar Mansions; named 'Ad-Dabaran' (the Follower) because it trails the Pleiades.",
            "28 oy manzilidan biri; Hulkar (Surayyo) yulduzlariga ergashib yurgani uchun «Dabaron» deyilgan."
        ),
        ArabicFixedStar(
            "spica", "السِّمَاك الأعزل", "Spica (As-Simak al-A'zal)", "Simok al-A'zal",
            "العذراء", "Virgo", 13.4199, -11.1613, 0.97,
            "منزل قمري ربيعي شهير؛ استعمله الفلكيون المسلمون لقياس حركة الاعتدال الربيعي بدقة فائقة.",
            "Famous spring Lunar Mansion used by Islamic astronomers to measure equinox precession accurately.",
            "Bahorgi mashhur oy manzili; musulmon astronomlari tengkunlikni hisoblashda qo'llagan."
        ),
        ArabicFixedStar(
            "antares", "قَلْب العقرب", "Antares (Qalb al-Aqrab)", "Qalb al-Aqrab",
            "العقرب", "Scorpius", 16.4901, -26.4320, 0.96,
            "نجم أحمر متوهّج يشكّل منزلة «القلب» في منازل القمر الصيفية.",
            "Glowing crimson supergiant marking the 'Al-Qalb' (Heart) Lunar Mansion in summer skies.",
            "Yoz osmonida qizil rangda porlovchi, «Qalb» oy manzilini bildiruvchi ulkan yulduz."
        ),
        ArabicFixedStar(
            "fomalhaut", "فَم الحوت", "Fomalhaut (Fam al-Hut)", "Fam al-Hut",
            "الحوت الجنوبي", "Piscis Austrinus", 22.9608, -29.6222, 1.16,
            "اسمه العالمي مأخوذ مباشرة من العربية «فم الحوت الجنوبي»، وهو من أبرز نجوم الخريف.",
            "Directly named from Arabic 'Fam al-Hut' (mouth of the Southern Fish), beacon of autumn nights.",
            "Nomi bevosita arabcha «Fam al-Hut» so'zidan olingan kuzgi yorug' yulduz."
        ),
        ArabicFixedStar(
            "deneb", "ذَنَب الدجاجة", "Deneb (Dhanab ad-Dajajah)", "Zanab ad-Dajoja (Deneb)",
            "الدجاجة", "Cygnus", 20.6905, 45.2803, 1.25,
            "اسمه مشتق من «الذنب»؛ وهو نجم عملاق شديد الضياء يقع على مجرة درب التبانة.",
            "Derived from Arabic 'Dhanab' (tail); an extraordinarily luminous supergiant along the Milky Way.",
            "Arabcha «Zanab» so'zidan olingan; Somon yo'li bo'ylab joylashgan juda yorug' gigant yulduz."
        ),
        ArabicFixedStar(
            "polaris", "الجُدَيّ (نجم القطب)", "Polaris (Al-Judayy)", "Qutb yulduzi (Judayy)",
            "الدب الأصغر", "Ursa Minor", 2.5303, 89.2641, 1.98,
            "الدليل الثابت للشمال الحقيقي ليلاً؛ به عرف المسلمون جهة الشمال ثم حددوا اتجاه القبلة.",
            "Steadfast guide to True North at night, helping travelers orient themselves toward the Qibla.",
            "Tunda haqiqiy shimolni ko'rsatuvchi yulduz; sayohatchilarga Qibla yo'nalishini topishda yordam bergan."
        ),
        ArabicFixedStar(
            "shaula", "الشَّوْلة", "Shaula (Ash-Shawlah)", "Shavla yulduzi",
            "العقرب", "Scorpius", 17.5601, -37.1038, 1.62,
            "منزلة «الشولة» أي إبرة ذيل العقرب؛ نجم مزدوج لامع في الأفق الجنوبي صيفاً.",
            "Marks the 'Ash-Shawlah' Lunar Mansion at the raised stinger of Scorpius in summer skies.",
            "Chayon yulduz turkumining uchidagi «Shavla» oy manzilini belgilovchi yorqin yulduz."
        )
    )

    /**
     * Computes Julian Date from UTC epoch milliseconds.
     */
    fun julianDate(epochMillis: Long): Double {
        return epochMillis / 86400000.0 + 2440587.5
    }

    /**
     * Computes Local Sidereal Time in hours [0, 24) for a given longitude and time.
     */
    fun localSiderealTimeHours(epochMillis: Long, longitudeDeg: Double): Double {
        val jd = julianDate(epochMillis)
        val d = jd - 2451545.0
        // Greenwich Mean Sidereal Time in hours
        val gmst = 18.697374558 + 24.06570982441908 * d
        var lst = (gmst + longitudeDeg / 15.0) % 24.0
        if (lst < 0.0) lst += 24.0
        return lst
    }

    /**
     * Converts Equatorial coordinates (RA in hours, Dec in degrees) to Observer Horizon coordinates (Alt, Az).
     */
    fun equatorialToHorizontal(
        raHours: Double,
        decDeg: Double,
        observerLatDeg: Double,
        lstHours: Double
    ): Pair<Double, Double> {
        var haDeg = (lstHours - raHours) * 15.0
        while (haDeg > 180.0) haDeg -= 360.0
        while (haDeg < -180.0) haDeg += 360.0

        val ha = Math.toRadians(haDeg)
        val dec = Math.toRadians(decDeg)
        val lat = Math.toRadians(observerLatDeg)

        val sinAlt = (sin(dec) * sin(lat) + cos(dec) * cos(lat) * cos(ha)).coerceIn(-1.0, 1.0)
        val alt = asin(sinAlt)

        val y = -sin(ha) * cos(dec)
        val x = sin(dec) * cos(lat) - cos(dec) * sin(lat) * cos(ha)
        var azDeg = Math.toDegrees(atan2(y, x))
        if (azDeg < 0.0) azDeg += 360.0

        return Pair(Math.toDegrees(alt), azDeg)
    }

    /**
     * Computes a complete real-time Astrolabe celestial snapshot for the user's city and timestamp.
     */
    fun computeSkySnapshot(
        epochMillis: Long,
        latitudeDeg: Double,
        longitudeDeg: Double
    ): AstrolabeSkySnapshot {
        val lst = localSiderealTimeHours(epochMillis, longitudeDeg)

        // Approximate Solar Equatorial coordinates (RA, Dec)
        val d = julianDate(epochMillis) - 2451545.0
        val g = Math.toRadians((357.529 + 0.98560028 * d) % 360.0)
        val q = (280.459 + 0.98564736 * d) % 360.0
        val eclLon = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2.0 * g)) % 360.0)
        val obliquity = Math.toRadians(23.439 - 0.00000036 * d)
        val sunRaRad = atan2(cos(obliquity) * sin(eclLon), cos(eclLon))
        var sunRaHours = Math.toDegrees(sunRaRad) / 15.0
        if (sunRaHours < 0.0) sunRaHours += 24.0
        val sunDecDeg = Math.toDegrees(asin((sin(obliquity) * sin(eclLon)).coerceIn(-1.0, 1.0)))

        val (sunAlt, sunAz) = equatorialToHorizontal(sunRaHours, sunDecDeg, latitudeDeg, lst)

        val (phaseAr, phaseEn, phaseUz) = when {
            sunAlt >= 12.0 -> Triple("نهار مشرق", "Bright Daylight", "Yorug' kunduz")
            sunAlt >= 0.0 -> Triple("الشروق / الغروب", "Golden Horizon", "Quyosh ufqda")
            sunAlt >= -6.0 -> Triple("الشفق المدني", "Civil Twilight", "Shafaq payti")
            sunAlt >= -12.0 -> Triple("الشفق البحري", "Nautical Twilight", "Kechki shafaq")
            sunAlt >= -18.0 -> Triple("الفجر الصادق / الشفق الفلكي", "Astronomical Twilight (Fajr)", "Bomdod / Falakiy shafaq")
            else -> Triple("سماء ليل صافية للرصد", "Deep Night — Ideal Stargazing", "Chuqur tun — yulduz kuzatish vaqti")
        }

        val starPositions = arabicFixedStars.map { star ->
            val (alt, az) = equatorialToHorizontal(star.raHours, star.decDeg, latitudeDeg, lst)
            StarLivePosition(
                star = star,
                altitudeDeg = alt,
                azimuthDeg = az,
                isVisibleNow = alt > 0.0
            )
        }.sortedByDescending { it.altitudeDeg }

        return AstrolabeSkySnapshot(
            localSiderealTimeHours = lst,
            sunAltitudeDeg = sunAlt,
            sunAzimuthDeg = sunAz,
            skyPhaseAr = phaseAr,
            skyPhaseEn = phaseEn,
            skyPhaseUz = phaseUz,
            stars = starPositions
        )
    }

    /**
     * Formats Local Sidereal Time as HH:MM:SS.
     */
    fun formatSiderealTime(lstHours: Double): String {
        val totalSec = ((lstHours * 3600.0).toInt()).coerceIn(0, 86399)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}
