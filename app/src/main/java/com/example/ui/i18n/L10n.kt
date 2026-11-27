package com.example.ui.i18n

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.compositionLocalOf
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName

val LocalAppLanguage = compositionLocalOf { AppLanguage.fromSystemLocale() }
val LocalIs24HourFormat = compositionLocalOf { false }

object L10n {
    fun pick(lang: AppLanguage, ar: String, en: String, uz: String, ru: String): String = when (lang) {
        AppLanguage.ARABIC -> ar
        AppLanguage.ENGLISH -> en
        AppLanguage.UZBEK -> uz
        AppLanguage.RUSSIAN -> ru
    }

    fun pick(lang: AppLanguage, ar: String, en: String, uz: String): String = when (lang) {
        AppLanguage.ARABIC -> ar
        AppLanguage.ENGLISH -> en
        AppLanguage.UZBEK -> uz
        AppLanguage.RUSSIAN -> translateToRussian(en)
    }

    fun pick(lang: AppLanguage, ar: String, en: String): String = when (lang) {
        AppLanguage.ARABIC -> ar
        AppLanguage.ENGLISH -> en
        AppLanguage.UZBEK -> translateToUzbek(en)
        AppLanguage.RUSSIAN -> translateToRussian(en)
    }

    fun prayerName(prayer: PrayerName, lang: AppLanguage): String = when (prayer) {
        PrayerName.FAJR -> pick(lang, "الفجر", "Fajr", "Bomdod", "Фаджр")
        PrayerName.SUNRISE -> pick(lang, "الشروق", "Sunrise", "Quyosh chiqishi", "Восход")
        PrayerName.DHUHR -> pick(lang, "الظهر", "Dhuhr", "Peshin", "Зухр")
        PrayerName.ASR -> pick(lang, "العصر", "Asr", "Asr", "Аср")
        PrayerName.MAGHRIB -> pick(lang, "المغرب", "Maghrib", "Shom", "Магриб")
        PrayerName.ISHA -> pick(lang, "العشاء", "Isha", "Xufton", "Иша")
    }

    fun formatCountdown(remainingMillis: Long, lang: AppLanguage): String {
        val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Formats a prayer timestamp strictly honoring the phone's 12-hour or 24-hour clock format:
     * - 24-hour system (`is24Hour = true`): `04:55`, `13:05`, `15:02`, `22:40` (or with `:ss` when [includeSeconds] is true).
     * - 12-hour system (`is24Hour = false`): `4:55 ص`, `1:05 م`, `3:02 م`, `10:40 م` (or compact `1:05`, `5:20`, `6:41`).
     */
    fun formatPrayerTime(
        millis: Long,
        timeZoneId: String,
        lang: AppLanguage,
        is24Hour: Boolean,
        includeSeconds: Boolean = false,
        compact: Boolean = false
    ): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(timeZoneId)).apply {
            timeInMillis = millis
        }
        val hour24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val second = cal.get(java.util.Calendar.SECOND)

        if (is24Hour) {
            return if (includeSeconds) {
                String.format(java.util.Locale.US, "%02d:%02d:%02d", hour24, minute, second)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", hour24, minute)
            }
        }

        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val core = if (includeSeconds) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hour12, minute, second)
        } else {
            String.format(java.util.Locale.US, "%d:%02d", hour12, minute)
        }
        if (compact) return core

        val period = when (lang) {
            AppLanguage.ARABIC -> if (isPm) "م" else "ص"
            AppLanguage.UZBEK -> if (isPm) "PM" else "AM"
            AppLanguage.ENGLISH -> if (isPm) "PM" else "AM"
            AppLanguage.RUSSIAN -> if (isPm) "PM" else "AM"
        }
        return "$core $period"
    }

    fun formatSystemTime(
        context: Context,
        millis: Long,
        timeZoneId: String,
        lang: AppLanguage,
        includeSeconds: Boolean = false,
        compact: Boolean = false
    ): String = formatPrayerTime(
        millis = millis,
        timeZoneId = timeZoneId,
        lang = lang,
        is24Hour = DateFormat.is24HourFormat(context),
        includeSeconds = includeSeconds,
        compact = compact
    )

    fun formatTime12h(millis: Long, timeZoneId: String, lang: AppLanguage): String =
        formatPrayerTime(millis, timeZoneId, lang, is24Hour = false, includeSeconds = false, compact = false)

    private fun translateToUzbek(en: String): String {
        UZBEK_EXACT[en]?.let { return it }

        // Dynamic pattern translations for common UI strings
        if (en.startsWith("Page ") && en.endsWith(" / 604")) {
            val page = en.removePrefix("Page ").removeSuffix(" / 604")
            return "$page-sahifa / 604"
        }
        if (en.startsWith("Loading Madinah Mushaf page ")) {
            val rest = en.removePrefix("Loading Madinah Mushaf page ").removeSuffix("...")
            return "Madina Mushafi $rest-sahifasi yuklanmoqda..."
        }
        if (en.startsWith("Recording: ")) {
            return "Yozilmoqda: ${en.removePrefix("Recording: ")}"
        }
        if (en.startsWith("Last recording (")) {
            return "Oxirgi yozuv (${en.removePrefix("Last recording (")}"
        }
        if (en.startsWith("SpeechRecognizer error code: ")) {
            return "Nutqni aniqlash xato kodi: ${en.removePrefix("SpeechRecognizer error code: ")}"
        }
        if (en.startsWith("Test Mode")) {
            return en.replaceFirst("Test Mode", "Sinov rejimi")
        }
        if (en.startsWith("Download Surah ")) {
            return en.replaceFirst("Download Surah ", "Surani yuklab olish: ").replace(" Pages", " sahifalari")
        }
        if (en.startsWith("Download Full Juz ")) {
            return "${en.removePrefix("Download Full Juz ")}-juzni to'liq yuklab olish"
        }
        if (en.startsWith("Synced Recitation: ")) {
            return "Sinxron tilovat: ${en.removePrefix("Synced Recitation: ").replace("Ayah", "Oyat").replace("Word", "So'z")}"
        }
        if (en.endsWith(" Prayer Alert")) {
            val prayer = en.removeSuffix(" Prayer Alert")
            return "$prayer namozi eslatmasi"
        }
        return en
    }

    private val UZBEK_EXACT: Map<String, String> = mapOf(
        // Bottom Navigation & Main Sections
        "Home" to "Bosh sahifa",
        "Quran" to "Qur'on",
        "Holy Quran" to "Qur'oni Karim",
        "Prayers" to "Namoz",
        "Prayer Times" to "Namoz vaqtlari",
        "Qibla" to "Qibla",
        "Adhkar" to "Zikrlar",
        "Tasbih" to "Tasbeh",
        "Settings" to "Sozlamalar",
        "More" to "Ko'proq",
        "Calendar" to "Taqvim",
        "Hijri Calendar" to "Hijriy taqvim",
        "Open Quran" to "Qur'onni ochish",
        "Resume Reading" to "O'qishni davom ettirish",
        "Listen Quran" to "Qur'on tinglash",
        "Adhan" to "Azon",

        // Settings Screen
        "Account, language, reciter, alerts, support & privacy" to "Hisob, til, qori, bildirishnomalar, yordam va maxfiylik",
        "Verified NOOR Account" to "Tasdiqlangan NOOR hisobi",
        "Sign Out" to "Chiqish",
        "Open Super Admin Control Center" to "Bosh administrator markazini ochish",
        "NOOR — Founder, Support & Announcements" to "NOOR — Asoschi, yordam va e'lonlar",
        "About the Founder — Osama" to "Asoschi haqida — Usoma",
        "Official Contact & Support Center" to "Rasmiy aloqa va yordam markazi",
        "Notification & Announcements Center" to "Bildirishnomalar va e'lonlar markazi",
        "Personal Greeting Name (Local Only)" to "Shaxsiy salomlashish ismi (faqat qurilmada)",
        "Enter your name..." to "Ismingizni kiriting...",
        "Save" to "Saqlash",
        "Preferred Quran Reciter" to "Afzal Qur'on qorisi",
        "Audio Streaming Quality" to "Audio oqim sifati",
        "Daily Quran Goal (Ayahs)" to "Kunlik Qur'on maqsadi (oyatlar)",
        "Language (اللغة)" to "Til (Language / اللغة)",
        "Language / Til" to "Til (O'zbekcha / العربية / English)",
        "Appearance" to "Ko'rinish",
        "Dark" to "Qorong'i",
        "Light" to "Yorug'",
        "System" to "Tizim",
        "Prayer Times & Location" to "Namoz vaqtlari va joylashuv",
        "Adhan Voice & Sound Settings" to "Azon ovozi va tovush sozlamalari",
        "Customize" to "Sozlash",
        "Offline Local Super Admin" to "Oflayn mahalliy boshqaruv",
        "Hub" to "Markaz",
        "Notifications & Reminders" to "Bildirishnomalar va eslatmalar",
        "Persistent Prayer Times Bar" to "Doimiy namoz vaqtlari paneli",
        "Pre-Prayer Alert" to "Namozdan oldin ogohlantirish",
        "Off" to "O'chiq",
        "m" to "daq",
        "Morning Adhkar (6:30 AM)" to "Tonggi zikrlar (06:30)",
        "Evening Adhkar (5:30 PM)" to "Kechki zikrlar (17:30)",
        "Daily Quran Reminder (9:00 PM)" to "Kunlik Qur'on eslatmasi (21:00)",
        "Tasbih Reminder (3:00 PM)" to "Tasbeh eslatmasi (15:00)",
        "Send Test Notification" to "Sinov bildirishnomasini yuborish",
        "Sound & Haptics" to "Tovush va tebranish",
        "Tasbih & Adhkar Vibration" to "Tasbeh va zikr tebranishi",
        "Tasbih Click Sound" to "Tasbeh bosish tovushi",
        "Home Screen Shortcuts & Widgets" to "Bosh ekran yorliqlari va vidjetlari",
        "Privacy & Local Backup" to "Maxfiylik va mahalliy zaxira",
        "Backup" to "Zaxiralash",
        "Restore" to "Tiklash",
        "Reset Application" to "Ilovani asl holiga qaytarish",
        "Confirm Full Reset" to "To'liq qaytarishni tasdiqlash",
        "Reset" to "Qaytarish",
        "Cancel" to "Bekor qilish",
        "Close" to "Yopish",

        // Quran Reader & Mushaf Interactive Menu
        "Surah" to "Sura",
        "Choose Surah" to "Surani tanlash",
        "Choose Reciter" to "Qorini tanlash",
        "Reciter" to "Qori",
        "Prev" to "Oldingi",
        "Next" to "Keyingi",
        "← Previous" to "← Oldingi oyat",
        "Next →" to "Keyingi oyat →",
        "Full-Screen Mode" to "To'liq ekran rejimi",
        "Exit Full-Screen" to "To'liq ekrandan chiqish",
        "Highlight Settings" to "Ajratib ko'rsatish sozlamalari",
        "Tap an Ayah for actions · Tap empty space for full screen" to "Oyatni bosing · To'liq ekran uchun bo'sh joyni bosing",
        "Read Here" to "Shu yerdan o'qish",
        "Listen" to "Tinglash",
        "Memorize" to "Yodlash",
        "Tafsir" to "Tafsir",
        "I'rab" to "E'rob",
        "Sarf" to "Sarf",
        "Repeat Range" to "Takrorlash",
        "Saved" to "Saqlangan",
        "Favorite" to "Sevimli",
        "Add Hifz" to "Hifz rejasi",
        "Copy" to "Nusxalash",
        "Share" to "Ulashish",
        "Close menu" to "Menyuni yopish",
        "Tap outside the Ayah to dismiss" to "Yopish uchun oyat tashqarisiga bosing",
        "Positioned below Ayah to keep text clear" to "Oyat matni yopilmasligi uchun pastda joylashgan",
        "Ayah copied to clipboard" to "Oyat buferga nusxalandi",
        "Share Ayah" to "Oyatni ulashish",
        "Jump in Madinah Mushaf" to "Madina Mushafida o'tish",
        "Page Number (1 - 604)" to "Sahifa raqami (1 - 604)",
        "Or Juz Number (1 - 30)" to "Yoki Juz raqami (1 - 30)",
        "Go to Juz" to "Juzga o'tish",
        "Go to Page" to "Sahifaga o'tish",
        "Retry" to "Qayta urinish",
        "Uthmani Text" to "Usmoniy matn",
        "Download Coordinates" to "Koordinatalarni yuklab olish",
        "Download All 604 Pages" to "Barcha 604 sahifani yuklab olish",
        "Import Local ZIP Pack (Offline)" to "Mahalliy ZIP to'plamini yuklash (oflayn)",
        "Clear Cached Pages" to "Kesh sahifalarni tozalash",
        "Offline Madinah Mushaf Manager (604 Pages)" to "Oflayn Madina Mushafi boshqaruvi (604 sahifa)",
        "Exact glyph coordinates DB activated" to "Aniq oyat koordinatalari bazasi faollashtirildi",
        "Invalid glyph coordinates file" to "Koordinata fayli yaroqsiz",

        // Memorization, Recitation, STT & Recorder
        "Quran Memorization" to "Qur'on yodlash (Hifz)",
        "Calm space for memorization, recitation & review" to "Yodlash, tilovat va takrorlash uchun qulay muhit",
        "Recite" to "Tilovat",
        "Test" to "Sinov",
        "Progress" to "Natijalar",
        "More Tools" to "Qo'shimcha vositalar",
        "Stop" to "To'xtatish",
        "Record" to "Yozib olish",
        "Pause" to "Pauza",
        "Resume" to "Davom ettirish",
        "Check" to "Tekshirish",
        "Evaluate" to "Baholash",
        "Clear" to "Tozalash",
        "Recognized" to "Aniqlangan matn",
        "Play Recording" to "Yozuvni tinglash",
        "Play My Recording" to "Yozuvimni tinglash",
        "Pause Recording" to "Yozuvni to'xtatish",
        "Delete Recording" to "Yozuvni o'chirish",
        "Record Voice" to "Ovoz yozish",
        "Stop & Save" to "To'xtatish va saqlash",
        "Start Reciting" to "Tilovatni boshlash",
        "Stop & Evaluate" to "To'xtatish va baholash",
        "Voice Input" to "Tizim ovozli kiritishi",
        "Range:" to "Oraliq:",
        "Reveal" to "Ko'rsatish",
        "Hide to Test" to "Sinov uchun yashirish",
        "Word-by-Word Check" to "So'zma-so'z tekshirish",
        "Type or edit recited text" to "Tilovat matnini kiriting yoki tahrirlang",
        "1. Voice Recorder (Real M4A)" to "1. Ovoz yozgich (M4A)",
        "2. Arabic Speech-to-Text & Memorization Check" to "2. Arabcha nutqni matnga aylantirish va tekshirish",
        "Your recognized Arabic recitation text will appear here live..." to "Tilovatingizdan aniqlangan arabcha matn shu yerda ko'rinadi...",
        "Excellent" to "A'lo",
        "Good — Minor Review" to "Yaxshi — biroz takrorlash kerak",
        "Needs Revision" to "Qayta takrorlash kerak",
        "Recognition Unclear — Retry or Teacher Review" to "Nutq aniq emas — qayta urinib ko'ring yoki ustozga tekshirtiring",
        "Microphone permission is required for speech recognition." to "Nutqni aniqlash uchun mikrofon ruxsati kerak.",
        "Microphone permission denied." to "Mikrofon ruxsati rad etildi.",
        "Microphone permission is required before recording." to "Ovoz yozish uchun mikrofon ruxsati kerak.",

        // Progressive Page Dictation & Islamic Videos
        "By Page (1–604)" to "Sahifa bo'yicha (1–604)",
        "By Surah (1–114)" to "Sura bo'yicha (1–114)",
        "Write-As-Recite" to "Aytganda yozish",
        "Ayah by Ayah" to "Oyatma-oyat",
        "Mistake Alert Sound" to "Xato ogohlantirish tovushi",
        "Reset Page" to "Sahifani qaytarish",
        "Videos" to "Videolar",
        "NOOR Videos" to "NOOR Videolar",
        "NOOR Islamic Videos" to "NOOR islomiy videolari",
        "Quran Lessons, Tajweed & Islamic Cartoons" to "Qur'on darslari, Tajvid va islomiy multfilmlar",
        "Quran & Tajweed Lessons" to "Qur'on va Tajvid darslari",
        "Islamic Cartoons" to "Islomiy multfilmlar",
        "Prophets & Quran Stories" to "Payg'ambarlar va Qur'on qissalari",
        "My Local Videos" to "Mening mahalliy videolarim",
        "Pick Local Video" to "Qurilmadan video tanlash",
        "Watch" to "Ko'rish",

        // User Profile, YouTube Video Publishing, Super Admin Videos & Prayer Time-Gate
        "Account Profile Photo" to "Profil rasmi",
        "Change Profile Photo" to "Profil rasmini o'zgartirish",
        "Change Name" to "Ismni o'zgartirish",
        "Change Display Name" to "Hisob ismini o'zgartirish",
        "Account & Display Name" to "Hisob va ko'rinadigan ism",
        "New Name" to "Yangi ism",
        "Restore Original Google Photo" to "Asl Google profil rasmini tiklash",
        "Publish" to "Nashr",
        "Publish Video" to "Video nashr qilish",
        "Publish New Video" to "Yangi video nashr qilish",
        "Publish Video with Cover" to "Muqovali video nashr qilish",
        "Edit Video Details" to "Video ma'lumotlarini tahrirlash",
        "Cover Photo" to "Muqova rasmi",
        "Pick Video" to "Video tanlash",
        "Video Title" to "Video nomi",
        "Description" to "Video tavsifi",
        "Channel / Speaker" to "Kanal / Ustoz",
        "Duration" to "Davomiyligi",
        "Save Changes" to "O'zgarishlarni saqlash",
        "Edit" to "Tahrirlash",
        "Delete" to "O'chirish",
        "Delete Video" to "Videoni o'chirish",
        "Delete Published Video" to "Nashr qilingan videoni o'chirish",
        "Prayed ✓" to "O'qildi ✓",
        "Entered — tap to mark prayed" to "Vaqti kirdi — belgilash uchun bosing",
        "Not entered yet" to "Vaqti hali kirmadi",
        "Today's Prayer Times" to "Bugungi namoz vaqtlari",
        "See All" to "Barchasini ko'rish",
        "Assalamu Alaikum" to "Assalomu alaykum",
        "Daily" to "Kunlik",
        "Weekly" to "Haftalik",
        "Monthly" to "Oylik",
        "Today's Worship Checklist" to "Bugungi ibodatlar ro'yxati",
        "Personal Worship Dashboard" to "Shaxsiy ibodat paneli",

        // Morning, Evening & Tahajjud/Qiyam Al-Layl Reminders + Custom Sounds
        "Morning, Evening & Tahajjud Reminders" to "Tonggi, Kechki va Tahajjud eslatmalari",
        "Morning Prayer & Adhkar" to "Bomdod va tonggi zikrlar",
        "Evening Adhkar Reminder" to "Kechki zikrlar eslatmasi",
        "Tahajjud & Qiyam Al-Layl" to "Tahajjud va tungi namoz (Qiyomul-layl)",
        "Wake up in the blessed last third of the night" to "Kechaning muborak so'nggi uchdan birida uyg'oning",
        "Daily morning prayer & fortress adhkar alert" to "Har kungi Bomdod va tonggi zikr eslatmasi",
        "Daily evening remembrance & tranquility alert" to "Har kungi kechki zikr va sokinlik eslatmasi",
        "Reminder Time" to "Eslatma vaqti",
        "Custom Sound" to "Maxsus ovoz",
        "Choose Reminder Sound" to "Eslatma ovozini tanlash",
        "Import Audio from Device" to "Qurilmadan audio fayl tanlash",
        "Preview" to "Tinglash",
        "Test Alert" to "Sinov",
        "Stop Sound" to "Ovozni to'xtatish",
        "Set Reminder Time" to "Eslatma vaqtini belgilash",
        "Hour" to "Soat",
        "Minute" to "Daqiqa",
        "Quick Presets" to "Tezkor vaqtlar",
        "Tahajjud Alerts" to "Tahajjud",
        "Next Prayer" to "Keyingi namoz",

        // Zakat Calculator, 99 Names of Allah & Wudu/Umrah/Ruqyah Guides
        "Essential Daily Islamic Tools" to "Asosiy kundalik islomiy vositalar",
        "Zakat Calculator" to "Zakot kalkulyatori",
        "Wealth, Gold, Silver, Business & Zakat al-Fitr" to "Mol, oltin, kumush, savdo va Fitr zakoti",
        "99 Names of Allah" to "Allohning 99 go'zal ismlari",
        "All 99 Beautiful Names with Meanings & Search" to "Barcha 99 ism, ma'nolari va qidiruv",
        "Wudu, Umrah & Ruqyah" to "Tahorat, Umra va Ruqya",
        "Step-by-Step Wudu, Umrah Rituals & Ruqyah" to "Bosqichma-bosqich tahorat, Umra va Ruqya",
        "Zakat" to "Zakot",
        "99 Names" to "99 Ism",
        "Guides" to "Qo'llanma",

        // Qada Tracker, Mirath Calculator & Islamic Card Designer
        "Missed Prayers (Qada)" to "Qazo namozlar kuzatuvchisi",
        "Track & Complete Missed Prayers Daily" to "Qazo namozlarni har kuni kuzatib o'qing",
        "Inheritance (Mirath)" to "Meros taqsimoti (Meros)",
        "Shariah Estate Shares, Awl, Radd & Blocking" to "Shariat ulushlari, Avl, Radd va Hijob",
        "Islamic Card Designer" to "Islomiy kartochka dizayneri",
        "Create & Share Luxury Ayah & Dua Cards" to "Oyat va duo kartochkalarini yarating va ulashing",
        "Qada" to "Qazo",
        "Mirath" to "Meros",
        "Cards" to "Kartalar",

        // Live Online Super-Features
        "Live Online Super-Features" to "Jonli internet super-imkoniyatlari",
        "Live Makkah & Madinah" to "Makka va Madina jonli efir",
        "24/7 Live Kaaba & Nabawi Streams + Weather" to "Ka'ba va Nabaviy 24/7 jonli efiri va ob-havo",
        "Global Quran & Tafsir" to "Qur'on qidiruvi va Tafsirlar",
        "Search All 6,236 Ayahs + Multi-Tafsir & Audio" to "6236 oyat qidiruvi, tafsirlar va jonli tilovat",
        "Live Hadith Encyclopedia" to "Jonli Hadislar ensiklopediyasi",
        "Verified Hadiths with Grading, Notes (AR/EN/UZ)" to "Sahih hadislar, daraja va sharhlar (O'zbekcha)",
        "Haramain" to "Haramayn",
        "Quran Search" to "Qur'on izlash",
        "Live Hadith" to "Hadislar",

        // Realistic 3D Misbaha, Quran I'rab & Sarf, Crescent Observatory & Global Ummah Pulse
        "Realistic 3D Misbaha" to "Haqiqiy 3D Tasbeh",
        "20 Luxury Bead Colors" to "20 ta hashamatli munchoq rangi",
        "Step Back" to "Orqaga qaytarish",
        "Quran I'rab & Sarf" to "Qur'on E'robi va Sarfi",
        "Word-by-Word Grammatical I'rab & Morphology" to "114 sura uchun so'zma-so'z e'rob va sarf tahlili",
        "I'rab & Sarf" to "E'rob va Sarf",
        "Crescent Observatory" to "Hilol va Oy rasadxonasi",
        "Live 3D Moon, 28 Lunar Mansions & Sighting" to "Jonli 3D Oy, 28 oy manzili va hilol ko'rinishi",
        "Crescent Lab" to "Hilol rasadxonasi",
        "Global Adhan & Qira'at" to "Ummat Azoni va Noyob Qiroatlar",
        "12 Islamic Cities Live Adhan + Rare Qira'at Vault" to "12 islom shahri jonli azoni + Varsh, Qolun va Mujavvad",
        "Ummah Pulse" to "Ummat nafas",

        // 3D Splash, Collapsible Ecosystem, Mutashabihat & Seerah Atlas
        "Remember us in your sincere prayers" to "Bizni ham solih duolaringizda unutmang",
        "Show More" to "Ko'proq ko'rsatish",
        "Show Less" to "Yig'ish",
        "Quran Mutashabihat Lab" to "Qur'on Mutashobihotlari",
        "Word-Level Verse Diffs, Hifz Rules & Live Scanner" to "So'zma-so'z farqlar, oltin qoidalar va jonli skaner",
        "Mutashabihat" to "Mutashobihot",
        "Seerah Atlas & Shama'il" to "Siyrat atlasi va Shamoil",
        "Interactive Hijrah GPS Map, Seerah & Sunnah Revival" to "Hijrat GPS xaritasi, siyrat va sunnatlarni tiriltirish",
        "Seerah Atlas" to "Siyrat atlasi",
        "Quran Root & Qira'at Universe" to "Qur'on ildizlari va Qiroatlar",
        "Living Root Galaxy (6,236 Ayahs) + 10 Qira'at + Sanad" to "Jonli ildiz galaktikasi (6236 oyat) + 10 Qiroat + Oltin Sanad",
        "Root Galaxy" to "Ildiz galaktikasi",
        "3D Earth & Qibla Globe" to "3D Yer shari va Qibla yoyi",
        "Interactive 3D Earth, Live Day/Night & Geodesic Qibla Arc" to "Interaktiv 3D Yer, jonli kun-tun va oltin Qibla yoyi",
        "3D Globe" to "3D Globus",
        "Quranic Soul Remedy" to "Qur'ondan qalb shifosi",
        "Emotional Quranic Prescriptions, Live Audio & Dhikr Halo" to "Holatingizga mos oyatlar, jonli tilovat va zikr nafas halqasi",
        "Soul Remedy" to "Qalb shifosi",
        "Cosmic Signs & 25 Prophets" to "Koinot mo'jizalari va 25 Payg'ambar",
        "Cosmic Signs, Parables & Complete 25 Prophets Atlas" to "Ilmiy oyatlar, Qur'on masallari va 25 payg'ambar atlasi",
        "25 Prophets" to "25 Payg'ambar",
        "Golden Calligraphy Studio" to "Zarhal Xattotlik Studiyasi",
        "Touch Noor Sparks, 5 Gold-Leaf Themes, Live Audio & HD Art" to "Oltin nur uchqunlari, 5 zarhal palitra, jonli tilovat va HD saqlash",
        "Calligraphy" to "Xattotlik",
        "Islamic Acoustic Sanctuary" to "Sokinlik va Audio Voha",
        "4-Layer Live Rain/Breeze/Stream Mixer + Sakinah Quran & Timer" to "4 qatlamli yomg'ir/shabada/suv mikseri + Qur'on tilovati va taymer",
        "Sanctuary" to "Sokinlik",
        "Golden Islamic Astrolabe" to "Oltin Islomiy Asturlob",
        "Interactive Brass Astrolabe & 15 Historic Arabic Stars Live" to "Interaktiv oltin asturlob va 15 tarixiy arab yulduzlari jonli xaritasi",
        "Astrolabe" to "Asturlob"
    )

    private fun translateToRussian(en: String): String {
        RUSSIAN_EXACT[en]?.let { return it }

        if (en.startsWith("Settings (")) {
            return en.replaceFirst("Settings", "Настройки")
        }
        if (en.startsWith("Page ") && en.endsWith(" / 604")) {
            val page = en.removePrefix("Page ").removeSuffix(" / 604")
            return "Страница $page / 604"
        }
        if (en.startsWith("Loading Madinah Mushaf page ")) {
            val rest = en.removePrefix("Loading Madinah Mushaf page ").removeSuffix("...")
            return "Загрузка страницы $rest Мединского мусхафа..."
        }
        if (en.startsWith("Recording: ")) {
            return "Запись: ${en.removePrefix("Recording: ")}"
        }
        if (en.startsWith("Last recording (")) {
            return "Последняя запись (${en.removePrefix("Last recording (")}"
        }
        if (en.startsWith("SpeechRecognizer error code: ")) {
            return "Код ошибки распознавания речи: ${en.removePrefix("SpeechRecognizer error code: ")}"
        }
        if (en.startsWith("Test Mode")) {
            return en.replaceFirst("Test Mode", "Режим проверки")
        }
        if (en.startsWith("Download Surah ")) {
            return en.replaceFirst("Download Surah ", "Скачать суру ").replace(" Pages", " (страницы)")
        }
        if (en.startsWith("Download Full Juz ")) {
            return "Скачать полный Джуз ${en.removePrefix("Download Full Juz ")}"
        }
        if (en.startsWith("Synced Recitation: ")) {
            return "Синхронное чтение: ${en.removePrefix("Synced Recitation: ").replace("Ayah", "Аят").replace("Word", "Слово")}"
        }
        if (en.endsWith(" Prayer Alert")) {
            val prayer = en.removeSuffix(" Prayer Alert")
            return "Напоминание о намазе $prayer"
        }
        if (en.startsWith("Round ")) {
            return "Круг ${en.removePrefix("Round ")}"
        }
        return en
    }

    private val RUSSIAN_EXACT: Map<String, String> = mapOf(
        // Bottom Navigation & Main Sections
        "Home" to "Главная",
        "Quran" to "Коран",
        "Holy Quran" to "Священный Коран",
        "Prayers" to "Намаз",
        "Prayer Times" to "Время намаза",
        "Qibla" to "Кибла",
        "Adhkar" to "Азкары",
        "Tasbih" to "Тасбих",
        "Settings" to "Настройки",
        "More" to "Ещё",
        "Calendar" to "Календарь",
        "Hijri Calendar" to "Календарь Хиджры",
        "Open Quran" to "Открыть Коран",
        "Resume Reading" to "Продолжить чтение",
        "Listen Quran" to "Слушать Коран",
        "Adhan" to "Азан",
        "Nearby Mosques" to "Ближайшие мечети",
        "Live GPS distance & directions" to "Точный GPS, расстояние и маршрут",

        // Settings Screen
        "Account, language, reciter, alerts, support & privacy" to "Аккаунт, язык, чтец, уведомления, поддержка и конфиденциальность",
        "Verified NOOR Account" to "Подтверждённый аккаунт NOOR",
        "Sign Out" to "Выйти",
        "Open Super Admin Control Center" to "Открыть панель Супер-Админа",
        "NOOR — Founder, Support & Announcements" to "NOOR — Основатель, поддержка и объявления",
        "About the Founder — Osama" to "Об основателе — Усама",
        "Official Contact & Support Center" to "Официальный центр поддержки",
        "Notification & Announcements Center" to "Центр уведомлений и объявлений",
        "Personal Greeting Name (Local Only)" to "Имя для приветствия (локально)",
        "Enter your name..." to "Введите ваше имя...",
        "Save" to "Сохранить",
        "Preferred Quran Reciter" to "Предпочитаемый чтец Корана",
        "Audio Streaming Quality" to "Качество аудиопотока",
        "Daily Quran Goal (Ayahs)" to "Дневная цель Корана (аяты)",
        "Language (اللغة)" to "Язык (Language)",
        "Language / Til" to "Язык (Русский / العربية / English / O'zbekcha)",
        "Appearance" to "Оформление",
        "Dark" to "Тёмная",
        "Light" to "Светлая",
        "System" to "Системная",
        "Prayer Times & Location" to "Время намаза и геолокация",
        "Adhan Voice & Sound Settings" to "Голос азана и настройки звука",
        "Customize" to "Настроить",
        "Offline Local Super Admin" to "Локальная панель администратора",
        "Hub" to "Центр",
        "Notifications & Reminders" to "Уведомления и напоминания",
        "Persistent Prayer Times Bar" to "Постоянная панель времени намаза",
        "Pre-Prayer Alert" to "Напоминание перед намазом",
        "Off" to "Выкл",
        "m" to "мин",
        "Morning Adhkar (6:30 AM)" to "Утренние азкары (06:30)",
        "Evening Adhkar (5:30 PM)" to "Вечерние азкары (17:30)",
        "Daily Quran Reminder (9:00 PM)" to "Напоминание о Коране (21:00)",
        "Tasbih Reminder (3:00 PM)" to "Напоминание о тасбихе (15:00)",
        "Send Test Notification" to "Отправить тестовое уведомление",
        "Sound & Haptics" to "Звук и вибрация",
        "Tasbih & Adhkar Vibration" to "Вибрация тасбиха и азкаров",
        "Tasbih Click Sound" to "Звук бусин тасбиха",
        "Home Screen Shortcuts & Widgets" to "Ярлыки и виджеты главного экрана",
        "Privacy & Local Backup" to "Конфиденциальность и резервная копия",
        "Backup" to "Резервная копия",
        "Restore" to "Восстановить",
        "Reset Application" to "Сбросить приложение",
        "Confirm Full Reset" to "Подтвердить полный сброс",
        "Reset" to "Сброс",
        "Cancel" to "Отмена",
        "Close" to "Закрыть",
        "Apply" to "Применить",

        // Quran Reader & Mushaf Interactive Menu
        "Surah" to "Сура",
        "Choose Surah" to "Выбрать суру",
        "Choose Reciter" to "Выбрать чтеца",
        "Reciter" to "Чтец",
        "Prev" to "Назад",
        "Next" to "Далее",
        "← Previous" to "← Предыдущий",
        "Next →" to "Следующий →",
        "Full-Screen Mode" to "Полноэкранный режим",
        "Exit Full-Screen" to "Выйти из полного экрана",
        "Highlight Settings" to "Настройки выделения",
        "Tap an Ayah for actions · Tap empty space for full screen" to "Нажмите на аят для действий · Нажмите пустое место для полного экрана",
        "Read Here" to "Читать отсюда",
        "Listen" to "Слушать",
        "Memorize" to "Заучивание",
        "Tafsir" to "Тафсир",
        "I'rab" to "И'раб",
        "Sarf" to "Сарф",
        "Repeat Range" to "Повтор диапазона",
        "Saved" to "Сохранено",
        "Favorite" to "Избранное",
        "Add Hifz" to "В план Хифза",
        "Copy" to "Копировать",
        "Share" to "Поделиться",
        "Close menu" to "Закрыть меню",
        "Tap outside the Ayah to dismiss" to "Нажмите вне аята, чтобы закрыть",
        "Positioned below Ayah to keep text clear" to "Расположено под аятом, чтобы не закрывать текст",
        "Ayah copied to clipboard" to "Аят скопирован в буфер обмена",
        "Share Ayah" to "Поделиться аятом",
        "Jump in Madinah Mushaf" to "Перейти в Мединском мусхафе",
        "Page Number (1 - 604)" to "Номер страницы (1 - 604)",
        "Or Juz Number (1 - 30)" to "Или номер Джуза (1 - 30)",
        "Go to Juz" to "Перейти к Джузу",
        "Go to Page" to "Перейти к странице",
        "Retry" to "Повторить",
        "Uthmani Text" to "Усманский текст",
        "Download Coordinates" to "Скачать координаты",
        "Download All 604 Pages" to "Скачать все 604 страницы",
        "Import Local ZIP Pack (Offline)" to "Импортировать локальный ZIP (офлайн)",
        "Clear Cached Pages" to "Очистить кэш страниц",
        "Offline Madinah Mushaf Manager (604 Pages)" to "Менеджер офлайн-мусхафа (604 стр.)",
        "Exact glyph coordinates DB activated" to "База точных координат аятов активирована",
        "Invalid glyph coordinates file" to "Неверный файл координат аятов",

        // Tasbih
        "Realistic 3D Misbaha" to "Реалистичные 3D чётки",
        "20 Luxury Bead Colors" to "20 материалов и цветов бусин",
        "Step Back" to "Назад",
        "Step Back (-1)" to "Назад (-1)",
        "Back (-1)" to "Назад (-1)",
        "Count (+1)" to "Счёт (+1)",
        "Today" to "Сегодня",
        "This Week" to "Эта неделя",
        "Total" to "Всего",
        "Custom" to "Свой",
        "Add Dhikr" to "Добавить зикр",
        "Add Custom Dhikr" to "Добавить свой зикр",
        "Dhikr Text" to "Текст зикра",
        "Label (Optional)" to "Перевод или название",
        "Default Target" to "Цель по умолчанию",
        "Set Custom Target" to "Установить свою цель",
        "Quick & Custom Dhikr" to "Быстрые и свои зикры",
        "Recent Sessions" to "Недавние сессии",
        "Start Dhikr" to "Выбрать зикр",
        "Show All" to "Показать все",
        "Tap anywhere to begin (swipe left-right to decrease)" to "Нажмите в любом месте (свайп влево-вправо уменьшит счёт)",

        // Memorization, Recitation, STT & Recorder
        "Quran Memorization" to "Заучивание Корана (Хифз)",
        "Calm space for memorization, recitation & review" to "Спокойное пространство для заучивания, чтения и повторения",
        "Recite" to "Чтение",
        "Test" to "Проверка",
        "Progress" to "Прогресс",
        "More Tools" to "Ещё инструменты",
        "Stop" to "Стоп",
        "Record" to "Запись",
        "Pause" to "Пауза",
        "Resume" to "Продолжить",
        "Check" to "Проверить",
        "Evaluate" to "Оценить",
        "Clear" to "Очистить",
        "Recognized" to "Распознано",
        "Play Recording" to "Прослушать запись",
        "Play My Recording" to "Прослушать мою запись",
        "Pause Recording" to "Пауза записи",
        "Delete Recording" to "Удалить запись",
        "Record Voice" to "Записать голос",
        "Stop & Save" to "Стоп и сохранить",
        "Start Reciting" to "Начать чтение",
        "Stop & Evaluate" to "Стоп и оценить",
        "Voice Input" to "Голосовой ввод",
        "Range:" to "Диапазон:",
        "Reveal" to "Показать",
        "Hide to Test" to "Скрыть для теста",
        "Word-by-Word Check" to "Проверка по словам",
        "Excellent" to "Отлично",
        "Good — Minor Review" to "Хорошо — нужно немного повторить",
        "Needs Revision" to "Требуется повторение",

        // Dashboard, Reminders & Ecosystem
        "Account & Display Name" to "Аккаунт и имя",
        "Change Profile Photo" to "Изменить фото профиля",
        "Change Name" to "Изменить имя",
        "Prayed ✓" to "Совершён ✓",
        "Entered — tap to mark prayed" to "Время наступило — нажмите для отметки",
        "Not entered yet" to "Время ещё не наступило",
        "Today's Prayer Times" to "Время намазов сегодня",
        "See All" to "Смотреть все",
        "Assalamu Alaikum" to "Ассаляму алейкум",
        "Daily" to "День",
        "Weekly" to "Неделя",
        "Monthly" to "Месяц",
        "Today's Worship Checklist" to "Список поклонений на сегодня",
        "Personal Worship Dashboard" to "Личная панель поклонений",
        "Morning, Evening & Tahajjud Reminders" to "Утренние, вечерние и Тахаджуд напоминания",
        "Morning Prayer & Adhkar" to "Фаджр и утренние азкары",
        "Evening Adhkar Reminder" to "Вечерние азкары",
        "Tahajjud & Qiyam Al-Layl" to "Тахаджуд и ночная молитва",
        "Next Prayer" to "Следующий намаз",
        "Essential Daily Islamic Tools" to "Основные исламские инструменты",
        "Zakat Calculator" to "Калькулятор Закята",
        "99 Names of Allah" to "99 имён Аллаха",
        "Wudu, Umrah & Ruqyah" to "Вуду, Умра и Рукья",
        "Zakat" to "Закят",
        "99 Names" to "99 имён",
        "Guides" to "Гиды",
        "Missed Prayers (Qada)" to "Пропущенные намазы (Каза)",
        "Inheritance (Mirath)" to "Наследство (Мирас)",
        "Islamic Card Designer" to "Конструктор открыток",
        "Qada" to "Каза",
        "Mirath" to "Мирас",
        "Cards" to "Открытки",
        "Live Online Super-Features" to "Онлайн-функции в реальном времени",
        "Live Makkah & Madinah" to "Мекка и Медина — прямой эфир",
        "Global Quran & Tafsir" to "Поиск по Корану и Тафсир",
        "Live Hadith Encyclopedia" to "Энциклопедия Хадисов",
        "Haramain" to "Харамейн",
        "Quran Search" to "Поиск Корана",
        "Live Hadith" to "Хадисы",
        "Quran I'rab & Sarf" to "И'раб и Сарф Корана",
        "I'rab & Sarf" to "И'раб и Сарф",
        "Crescent Observatory" to "Наблюдение Луны и затмений",
        "Crescent Lab" to "Луна и затмения",
        "Global Adhan & Qira'at" to "Азан мира и редкие Кирааты",
        "Ummah Pulse" to "Пульс Уммы",
        "Remember us in your sincere prayers" to "Не забывайте нас в ваших искренних дуа",
        "Show More" to "Показать больше",
        "Show Less" to "Свернуть",
        "Quran Mutashabihat Lab" to "Муташабихат Корана",
        "Mutashabihat" to "Муташабихат",
        "Seerah Atlas & Shama'il" to "Атлас Сиры и Шамаиль",
        "Seerah Atlas" to "Атлас Сиры",
        "Quran Root & Qira'at Universe" to "Корни Корана и 10 Кираатов",
        "Root Galaxy" to "Галактика корней",
        "3D Earth & Qibla Globe" to "3D Земля и Кибла",
        "3D Globe" to "3D Глобус",
        "Quranic Soul Remedy" to "Исцеление сердца Кораном",
        "Soul Remedy" to "Исцеление сердца",
        "Cosmic Signs & 25 Prophets" to "Знамения и 25 Пророков",
        "25 Prophets" to "25 Пророков",
        "Golden Calligraphy Studio" to "Студия каллиграфии",
        "Calligraphy" to "Каллиграфия",
        "Islamic Acoustic Sanctuary" to "Звуки спокойствия",
        "Sanctuary" to "Спокойствие",
        "Golden Islamic Astrolabe" to "Исламская астролябия",
        "Astrolabe" to "Астролябия",
        "Videos" to "Видео",
        "NOOR Videos" to "NOOR Видео"
    )
}
