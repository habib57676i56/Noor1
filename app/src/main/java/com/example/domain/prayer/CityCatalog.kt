package com.example.domain.prayer

import com.example.domain.model.AsrMadhhab
import com.example.domain.model.CalculationMethod
import com.example.domain.model.CityLocation
import java.util.TimeZone

object CityCatalog {
    val defaultCity = CityLocation(
        id = "makkah",
        nameAr = "مكة المكرمة",
        nameEn = "Makkah",
        countryAr = "المملكة العربية السعودية",
        countryEn = "Saudi Arabia",
        latitude = 21.4225,
        longitude = 39.8262,
        timezoneId = "Asia/Riyadh"
    )

    val cities: List<CityLocation> = listOf(
        defaultCity,
        CityLocation("madinah", "المدينة المنورة", "Madinah", "المملكة العربية السعودية", "Saudi Arabia", 24.4672, 39.6111, "Asia/Riyadh"),
        CityLocation("riyadh", "الرياض", "Riyadh", "المملكة العربية السعودية", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh"),
        CityLocation("jeddah", "جدة", "Jeddah", "المملكة العربية السعودية", "Saudi Arabia", 21.4858, 39.1925, "Asia/Riyadh"),
        CityLocation("dammam", "الدمام", "Dammam", "المملكة العربية السعودية", "Saudi Arabia", 26.4207, 50.0888, "Asia/Riyadh"),
        // Syria & Levant
        CityLocation("damascus", "دمشق", "Damascus", "سوريا", "Syria", 33.5138, 36.2765, "Asia/Damascus"),
        CityLocation("aleppo", "حلب", "Aleppo", "سوريا", "Syria", 36.2021, 37.1343, "Asia/Damascus"),
        CityLocation("idlib", "إدلب", "Idlib", "سوريا", "Syria", 35.9306, 36.6339, "Asia/Damascus"),
        CityLocation("homs", "حمص", "Homs", "سوريا", "Syria", 34.7324, 36.7137, "Asia/Damascus"),
        CityLocation("hama", "حماة", "Hama", "سوريا", "Syria", 35.1318, 36.7578, "Asia/Damascus"),
        CityLocation("latakia", "اللاذقية", "Latakia", "سوريا", "Syria", 35.5317, 35.7901, "Asia/Damascus"),
        CityLocation("jerusalem", "القدس", "Jerusalem", "فلسطين", "Palestine", 31.7683, 35.2137, "Asia/Jerusalem"),
        CityLocation("amman", "عمّان", "Amman", "الأردن", "Jordan", 31.9454, 35.9284, "Asia/Amman"),
        CityLocation("beirut", "بيروت", "Beirut", "لبنان", "Lebanon", 33.8938, 35.5018, "Asia/Beirut"),
        CityLocation("baghdad", "بغداد", "Baghdad", "العراق", "Iraq", 33.3152, 44.3661, "Asia/Baghdad"),
        // Egypt & Gulf
        CityLocation("cairo", "القاهرة", "Cairo", "مصر", "Egypt", 30.0444, 31.2357, "Africa/Cairo"),
        CityLocation("alexandria", "الإسكندرية", "Alexandria", "مصر", "Egypt", 31.2001, 29.9187, "Africa/Cairo"),
        CityLocation("dubai", "دبي", "Dubai", "الإمارات", "UAE", 25.2048, 55.2708, "Asia/Dubai"),
        CityLocation("abudhabi", "أبوظبي", "Abu Dhabi", "الإمارات", "UAE", 24.4539, 54.3773, "Asia/Dubai"),
        CityLocation("doha", "الدوحة", "Doha", "قطر", "Qatar", 25.2854, 51.5310, "Asia/Qatar"),
        CityLocation("kuwait", "مدينة الكويت", "Kuwait City", "الكويت", "Kuwait", 29.3759, 47.9774, "Asia/Kuwait"),
        CityLocation("manama", "المنامة", "Manama", "البحرين", "Bahrain", 26.2285, 50.5860, "Asia/Bahrain"),
        CityLocation("muscat", "مسقط", "Muscat", "عُمان", "Oman", 23.5880, 58.3829, "Asia/Muscat"),
        CityLocation("sanaa", "صنعاء", "Sana'a", "اليمن", "Yemen", 15.3694, 44.1910, "Asia/Aden"),
        // Turkey
        CityLocation("istanbul", "إسطنبول", "Istanbul", "تركيا", "Turkey", 41.0082, 28.9784, "Europe/Istanbul"),
        CityLocation("ankara", "أنقرة", "Ankara", "تركيا", "Turkey", 39.9334, 32.8597, "Europe/Istanbul"),
        CityLocation("gaziantep", "غازي عنتاب", "Gaziantep", "تركيا", "Turkey", 37.0662, 37.3833, "Europe/Istanbul"),
        CityLocation("bursa", "بورصة", "Bursa", "تركيا", "Turkey", 40.1885, 29.0610, "Europe/Istanbul"),
        // Uzbekistan & Central Asia
        CityLocation("tashkent", "طشقند", "Tashkent", "أوزبكستان", "Uzbekistan", 41.2995, 69.2401, "Asia/Tashkent"),
        CityLocation("samarkand", "سمرقند", "Samarkand", "أوزبكستان", "Uzbekistan", 39.6542, 66.9597, "Asia/Samarkand"),
        CityLocation("bukhara", "بخارى", "Bukhara", "أوزبكستان", "Uzbekistan", 39.7747, 64.4286, "Asia/Samarkand"),
        CityLocation("andijan", "أنديجان", "Andijan", "أوزبكستان", "Uzbekistan", 40.7821, 72.3442, "Asia/Tashkent"),
        // Russia
        CityLocation("moscow", "موسكو", "Moscow", "روسيا", "Russia", 55.7558, 37.6173, "Europe/Moscow"),
        CityLocation("kazan", "قازان", "Kazan", "روسيا", "Russia", 55.7961, 49.1064, "Europe/Moscow"),
        CityLocation("grozny", "غروزني", "Grozny", "روسيا", "Russia", 43.3180, 45.6981, "Europe/Moscow"),
        CityLocation("makhachkala", "محج قلعة", "Makhachkala", "روسيا", "Russia", 42.9849, 47.5047, "Europe/Moscow"),
        // Africa, South/SE Asia, Europe, Americas
        CityLocation("khartoum", "الخرطوم", "Khartoum", "السودان", "Sudan", 15.5007, 32.5599, "Africa/Khartoum"),
        CityLocation("tripoli", "طرابلس", "Tripoli", "ليبيا", "Libya", 32.8872, 13.1913, "Africa/Tripoli"),
        CityLocation("tunis", "تونس", "Tunis", "تونس", "Tunisia", 36.8065, 10.1815, "Africa/Tunis"),
        CityLocation("algiers", "الجزائر", "Algiers", "الجزائر", "Algeria", 36.7538, 3.0588, "Africa/Algiers"),
        CityLocation("rabat", "الرباط", "Rabat", "المغرب", "Morocco", 34.0209, -6.8416, "Africa/Casablanca"),
        CityLocation("casablanca", "الدار البيضاء", "Casablanca", "المغرب", "Morocco", 33.5731, -7.5898, "Africa/Casablanca"),
        CityLocation("karachi", "كراتشي", "Karachi", "باكستان", "Pakistan", 24.8607, 67.0011, "Asia/Karachi"),
        CityLocation("islamabad", "إسلام آباد", "Islamabad", "باكستان", "Pakistan", 33.6844, 73.0479, "Asia/Karachi"),
        CityLocation("dhaka", "دكا", "Dhaka", "بنغلاديش", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka"),
        CityLocation("jakarta", "جاكرتا", "Jakarta", "إندونيسيا", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta"),
        CityLocation("kualalumpur", "كوالالمبور", "Kuala Lumpur", "ماليزيا", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        CityLocation("london", "لندن", "London", "المملكة المتحدة", "United Kingdom", 51.5074, -0.1278, "Europe/London"),
        CityLocation("paris", "باريس", "Paris", "فرنسا", "France", 48.8566, 2.3522, "Europe/Paris"),
        CityLocation("berlin", "برلين", "Berlin", "ألمانيا", "Germany", 52.5200, 13.4050, "Europe/Berlin"),
        CityLocation("newyork", "نيويورك", "New York", "الولايات المتحدة", "United States", 40.7128, -74.0060, "America/New_York"),
        CityLocation("dearborn", "ديربورن", "Dearborn", "الولايات المتحدة", "United States", 42.3223, -83.1763, "America/Detroit"),
        CityLocation("toronto", "تورونتو", "Toronto", "كندا", "Canada", 43.6532, -79.3832, "America/Toronto"),
        CityLocation("sydney", "سيدني", "Sydney", "أستراليا", "Australia", -33.8688, 151.2093, "Australia/Sydney")
    )

    fun findById(id: String): CityLocation =
        cities.firstOrNull { it.id == id } ?: defaultCity

    /**
     * Picks the closest catalog city for the device's system timezone on first install
     * (falling back to Makkah when no catalog city shares that timezone).
     */
    fun defaultCityForTimeZone(tzId: String = TimeZone.getDefault().id): CityLocation {
        val exact = cities.firstOrNull { it.timezoneId.equals(tzId, ignoreCase = true) }
        return exact ?: defaultCity
    }

    /**
     * Recommends the official regional CalculationMethod and juristic AsrMadhhab
     * matching local mosque/ministry standards for a given city or GPS location.
     */
    fun recommendedSettingsFor(city: CityLocation): Pair<CalculationMethod, AsrMadhhab> {
        val tz = city.timezoneId.lowercase()
        val country = "${city.countryEn} ${city.countryAr}".lowercase()
        return when {
            tz.contains("tashkent") || tz.contains("samarkand") || tz.contains("almaty") ||
                tz.contains("bishkek") || tz.contains("dushanbe") || tz.contains("ashgabat") ||
                country.contains("uzbek") || country.contains("أوزبك") ->
                CalculationMethod.UZBEKISTAN to AsrMadhhab.HANAFI

            tz.contains("karachi") || tz.contains("dhaka") || tz.contains("kolkata") ||
                tz.contains("calcutta") || tz.contains("kabul") ||
                country.contains("pakistan") || country.contains("bangladesh") || country.contains("india") ->
                CalculationMethod.KARACHI to AsrMadhhab.HANAFI

            tz.contains("istanbul") || country.contains("turkey") || country.contains("تركيا") ->
                CalculationMethod.TURKEY to AsrMadhhab.SHAFI

            tz.contains("moscow") || tz.contains("samara") || tz.contains("yekaterinburg") ||
                tz.contains("novosibirsk") || tz.contains("kazan") || country.contains("russia") || country.contains("روسيا") -> {
                // North Caucasus (Chechnya / Dagestan, lat < 44.5) predominantly follows Shafi'i; Central Russia/Tatarstan follows Hanafi
                val madhhab = if (city.latitude in 41.0..44.5 && city.longitude in 43.0..49.0) {
                    AsrMadhhab.SHAFI
                } else {
                    AsrMadhhab.HANAFI
                }
                CalculationMethod.RUSSIA to madhhab
            }

            tz.contains("riyadh") || country.contains("saudi") || country.contains("السعودية") ->
                CalculationMethod.UMM_AL_QURA to AsrMadhhab.SHAFI

            tz.contains("dubai") || country.contains("uae") || country.contains("الإمارات") ->
                CalculationMethod.DUBAI to AsrMadhhab.SHAFI

            tz.contains("qatar") || country.contains("qatar") || country.contains("قطر") ->
                CalculationMethod.QATAR to AsrMadhhab.SHAFI

            tz.contains("kuwait") || country.contains("kuwait") || country.contains("الكويت") ->
                CalculationMethod.KUWAIT to AsrMadhhab.SHAFI

            tz.contains("cairo") || tz.contains("tripoli") || tz.contains("khartoum") ||
                tz.contains("damascus") || tz.contains("beirut") || tz.contains("amman") ||
                tz.contains("jerusalem") || tz.contains("baghdad") ||
                country.contains("egypt") || country.contains("syria") || country.contains("سوريا") ||
                country.contains("jordan") || country.contains("palestine") || country.contains("lebanon") ||
                country.contains("iraq") ->
                CalculationMethod.EGYPT to AsrMadhhab.SHAFI

            tz.startsWith("america/") ->
                CalculationMethod.ISNA to AsrMadhhab.SHAFI

            else ->
                CalculationMethod.MWL to AsrMadhhab.SHAFI
        }
    }
}
