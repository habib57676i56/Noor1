package com.example.domain.astronomy

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

data class LunarPhaseSnapshot(
    val ageDays: Double,
    val synodicPeriodDays: Double,
    val phaseFraction: Double, // 0.0 = New Moon, 0.5 = Full Moon, 1.0 = New Moon
    val illuminationPercent: Double,
    val isWaxing: Boolean,
    val phaseNameAr: String,
    val phaseNameEn: String,
    val phaseNameUz: String,
    val lunarMansionAr: String,
    val lunarMansionEn: String,
    val daysToNextNewMoon: Double,
    val daysToNextFullMoon: Double
)

/**
 * Pure Kotlin Astronomical Crescent & Lunar Phase Engine.
 * Calculates Moon Age, Illumination %, Waxing/Waning state, and the 28 Classical Arabic Lunar Mansions
 * (المنازل القمرية الـ٢٨) from any UTC epoch timestamp.
 */
object LunarAstronomyEngine {

    const val SYNODIC_MONTH_DAYS = 29.53058867

    // Reference New Moon: 2000-01-06 18:14 UTC (JD 2451550.259722 -> Unix epoch 947182440000L)
    private const val REFERENCE_NEW_MOON_MS = 947_182_440_000L
    private const val MS_PER_DAY = 86_400_000.0

    private val arabicMansions: List<Pair<String, String>> = listOf(
        "الشرطان" to "Al-Sharatan",
        "البطين" to "Al-Butain",
        "الثريا" to "Al-Thurayya",
        "الدبران" to "Al-Dabaran",
        "الهقعة" to "Al-Haq'ah",
        "الهنعة" to "Al-Han'ah",
        "الذراع" to "Al-Dhira'",
        "النثرة" to "Al-Nathrah",
        "الطرف" to "Al-Tarf",
        "الجبهة" to "Al-Jabhah",
        "الزبرة" to "Al-Zubrah",
        "الصرفة" to "Al-Sarfah",
        "العواء" to "Al-'Awwa",
        "السماك" to "Al-Simak",
        "الغفر" to "Al-Ghafr",
        "الزبانى" to "Al-Zubana",
        "الإكليل" to "Al-Iklil",
        "القلب" to "Al-Qalb",
        "الشولة" to "Al-Shawlah",
        "النعائم" to "Al-Na'a'im",
        "البلدة" to "Al-Baldah",
        "سعد الذابح" to "Sa'd al-Dhabih",
        "سعد بلع" to "Sa'd Bula'",
        "سعد السعود" to "Sa'd al-Su'ud",
        "سعد الأخبية" to "Sa'd al-Akhbiyah",
        "الفرغ المقدم" to "Al-Fargh al-Muqaddam",
        "الفرغ المؤخر" to "Al-Fargh al-Mu'akhkhar",
        "بطن الحوت" to "Batn al-Hut"
    )

    fun computeLunarPhase(epochMillis: Long): LunarPhaseSnapshot {
        val daysSinceRef = (epochMillis - REFERENCE_NEW_MOON_MS) / MS_PER_DAY
        val cycles = daysSinceRef / SYNODIC_MONTH_DAYS
        val frac = (cycles - floor(cycles)).coerceIn(0.0, 1.0)
        val age = frac * SYNODIC_MONTH_DAYS
        val illumination = ((1.0 - cos(2.0 * PI * frac)) / 2.0) * 100.0
        val waxing = frac < 0.5

        val (ar, en, uz) = when {
            age < 1.2 || age >= 28.3 -> Triple("محاق (ولادة الهلال)", "New Moon", "Yangi oy (Mahoq)")
            age < 6.4 -> Triple("هلال متزايد", "Waxing Crescent", "O'sayotgan hilol")
            age < 8.4 -> Triple("تربيع أول", "First Quarter", "Birinchi chorak")
            age < 13.8 -> Triple("أحدب متزايد", "Waxing Gibbous", "To'lishayotgan oy")
            age < 15.8 -> Triple("بدر مكتمل", "Full Moon", "To'lin oy (Badr)")
            age < 21.1 -> Triple("أحدب متناقص", "Waning Gibbous", "Kamayayotgan qavariq oy")
            age < 23.1 -> Triple("تربيع أخير", "Last Quarter", "Oxirgi chorak")
            else -> Triple("هلال متناقص", "Waning Crescent", "Kamayayotgan hilol")
        }

        val mansionIndex = ((frac * 28.0).toInt()).coerceIn(0, 27)
        val (mAr, mEn) = arabicMansions[mansionIndex]

        val toNewMoon = SYNODIC_MONTH_DAYS - age
        val fullAge = SYNODIC_MONTH_DAYS / 2.0
        val toFullMoon = if (age <= fullAge) fullAge - age else (SYNODIC_MONTH_DAYS - age) + fullAge

        return LunarPhaseSnapshot(
            ageDays = age,
            synodicPeriodDays = SYNODIC_MONTH_DAYS,
            phaseFraction = frac,
            illuminationPercent = (illumination * 10.0).roundToInt() / 10.0,
            isWaxing = waxing,
            phaseNameAr = ar,
            phaseNameEn = en,
            phaseNameUz = uz,
            lunarMansionAr = mAr,
            lunarMansionEn = mEn,
            daysToNextNewMoon = toNewMoon,
            daysToNextFullMoon = toFullMoon
        )
    }

    /**
     * Evaluates tonight's Islamic Hilal sighting conditions combining lunar age & live cloud cover %.
     */
    fun sightingVerdictAr(ageDays: Double, cloudCoverPercent: Int): String = when {
        cloudCoverPercent > 75 -> "سماء غائمة (${cloudCoverPercent}%) — قد تصعب الرؤية البصرية للهلال الليلة"
        ageDays in 0.8..3.5 && cloudCoverPercent <= 35 -> "ظروف مثالية لرؤية الهلال الجديد بالعين المجردة بعد المغرب (غيوم ${cloudCoverPercent}%)"
        ageDays < 0.8 -> "القمر في طور المحاق — لم يتكوّن الهلال القابل للرؤية بعد"
        ageDays in 13.5..16.0 -> "ليلة بدر مضيء (أيام البيض) — نسبة السطوع عالية والسماء صافية بنسبة ${100 - cloudCoverPercent}%"
        else -> "صفاء السماء الليلة ${100 - cloudCoverPercent}% — القمر واضح في طوره الحالي"
    }

    fun sightingVerdictEn(ageDays: Double, cloudCoverPercent: Int): String = when {
        cloudCoverPercent > 75 -> "Heavy cloud cover (${cloudCoverPercent}%) — visual crescent sighting is difficult tonight."
        ageDays in 0.8..3.5 && cloudCoverPercent <= 35 -> "Prime crescent sighting window after Maghrib! Clear skies (${100 - cloudCoverPercent}%)."
        ageDays < 0.8 -> "Moon is currently in New Moon phase — crescent not yet visible."
        ageDays in 13.5..16.0 -> "Full Moon night (Ayyam al-Bid) — bright skies with ${100 - cloudCoverPercent}% clarity."
        else -> "Sky clarity is ${100 - cloudCoverPercent}% tonight — Moon clearly observable."
    }

    fun sightingVerdictUz(ageDays: Double, cloudCoverPercent: Int): String = when {
        cloudCoverPercent > 75 -> "Osmon bulutli (${cloudCoverPercent}%) — hilolni ko'rish qiyin bo'lishi mumkin."
        ageDays in 0.8..3.5 && cloudCoverPercent <= 35 -> "Shomdan keyin yangi hilolni ko'rish uchun a'lo sharoit (osmon ${100 - cloudCoverPercent}% ochiq)!"
        ageDays < 0.8 -> "Hozir yangi oy (Mahoq) — hilol hali ko'rinmaydi."
        ageDays in 13.5..16.0 -> "To'lin oy (Ayyomul-biyz) kechasi — osmon ${100 - cloudCoverPercent}% tiniq."
        else -> "Bugun kechasi osmon tiniqligi ${100 - cloudCoverPercent}%."
    }
}
