package com.example.ui.i18n

import androidx.compose.runtime.compositionLocalOf
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }

object L10n {
    fun pick(lang: AppLanguage, ar: String, en: String, uz: String): String = when (lang) {
        AppLanguage.ARABIC -> ar
        AppLanguage.ENGLISH -> en
        AppLanguage.UZBEK -> uz
    }

    fun pick(lang: AppLanguage, ar: String, en: String): String = when (lang) {
        AppLanguage.ARABIC -> ar
        AppLanguage.ENGLISH -> en
        AppLanguage.UZBEK -> translateToUzbek(en)
    }

    fun prayerName(prayer: PrayerName, lang: AppLanguage): String = when (prayer) {
        PrayerName.FAJR -> pick(lang, "الفجر", "Fajr", "Bomdod")
        PrayerName.SUNRISE -> pick(lang, "الشروق", "Sunrise", "Quyosh chiqishi")
        PrayerName.DHUHR -> pick(lang, "الظهر", "Dhuhr", "Peshin")
        PrayerName.ASR -> pick(lang, "العصر", "Asr", "Asr")
        PrayerName.MAGHRIB -> pick(lang, "المغرب", "Maghrib", "Shom")
        PrayerName.ISHA -> pick(lang, "العشاء", "Isha", "Xufton")
    }

    fun formatCountdown(remainingMillis: Long, lang: AppLanguage): String {
        val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatTime12h(millis: Long, timeZoneId: String, lang: AppLanguage): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(timeZoneId)).apply {
            timeInMillis = millis
        }
        val hour24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val period = when (lang) {
            AppLanguage.ARABIC -> if (isPm) "م" else "ص"
            AppLanguage.UZBEK -> if (isPm) "PM" else "AM"
            AppLanguage.ENGLISH -> if (isPm) "PM" else "AM"
        }
        return String.format(java.util.Locale.US, "%d:%02d %s", hour12, minute, period)
    }

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
        "Watch" to "Ko'rish"
    )
}
