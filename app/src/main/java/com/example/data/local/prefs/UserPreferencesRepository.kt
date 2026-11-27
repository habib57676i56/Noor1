package com.example.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.AdhanCatalog
import com.example.audio.CustomAdhanRecord
import com.example.audio.PerPrayerAdhanConfig
import com.example.domain.model.AppLanguage
import com.example.domain.model.AsrMadhhab
import com.example.domain.model.CalculationMethod
import com.example.domain.model.CityLocation
import com.example.domain.model.HomeDashboardConfig
import com.example.domain.model.IqamahDelays
import com.example.domain.model.PrayerMinuteOffsets
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranDisplayMode
import com.example.domain.model.QuranReadingTheme
import com.example.domain.model.QuranReciter
import com.example.domain.model.ThemeMode
import com.example.domain.prayer.CityCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_settings")

data class UserPreferences(
    val userName: String = "",
    val customAvatarUri: String = "",
    val language: AppLanguage = AppLanguage.fromSystemLocale(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val cityId: String = CityCatalog.defaultCity.id,
    val cityNameAr: String = CityCatalog.defaultCity.nameAr,
    val cityNameEn: String = CityCatalog.defaultCity.nameEn,
    val countryAr: String = CityCatalog.defaultCity.countryAr,
    val countryEn: String = CityCatalog.defaultCity.countryEn,
    val latitude: Double = CityCatalog.defaultCity.latitude,
    val longitude: Double = CityCatalog.defaultCity.longitude,
    val timezoneId: String = CityCatalog.defaultCity.timezoneId,
    val isAutoLocation: Boolean = false,
    val calculationMethod: CalculationMethod = CalculationMethod.UMM_AL_QURA,
    val asrMadhhab: AsrMadhhab = AsrMadhhab.SHAFI,
    val offsets: PrayerMinuteOffsets = PrayerMinuteOffsets(),
    val iqamahDelays: IqamahDelays = IqamahDelays(),
    val hijriAdjustmentDays: Int = 0,
    // Notifications
    val notifyFajr: Boolean = true,
    val notifyDhuhr: Boolean = true,
    val notifyAsr: Boolean = true,
    val notifyMaghrib: Boolean = true,
    val notifyIsha: Boolean = true,
    val prePrayerAlertMinutes: Int = 10,
    val morningAdhkarReminderEnabled: Boolean = true,
    val morningAdhkarHour: Int = 6,
    val morningAdhkarMinute: Int = 30,
    val eveningAdhkarReminderEnabled: Boolean = true,
    val eveningAdhkarHour: Int = 17,
    val eveningAdhkarMinute: Int = 30,
    val tahajjudReminderEnabled: Boolean = true,
    val tahajjudReminderHour: Int = 2,
    val tahajjudReminderMinute: Int = 30,
    val morningReminderSoundId: String = AdhanCatalog.DEFAULT_VOICE_ID,
    val eveningReminderSoundId: String = AdhanCatalog.DEFAULT_VOICE_ID,
    val tahajjudReminderSoundId: String = AdhanCatalog.DEFAULT_VOICE_ID,
    val quranReminderEnabled: Boolean = true,
    val quranReminderHour: Int = 21,
    val quranReminderMinute: Int = 0,
    val tasbihReminderEnabled: Boolean = false,
    val tasbihReminderHour: Int = 15,
    val tasbihReminderMinute: Int = 0,
    // Audio & Haptics
    val tasbihVibrationEnabled: Boolean = true,
    val tasbihSoundEnabled: Boolean = true,
    val activeTasbihPresetId: String = "subhanallah",
    val activeTasbihCount: Int = 0,
    val activeTasbihTarget: Int = 33,
    val customTasbihText: String = "سبحان الله وبحمده",
    // Quran Reader & Audio
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val lastReadPage: Int = 1,
    val quranFontSizeSp: Int = 24,
    val quranReadingTheme: QuranReadingTheme = QuranReadingTheme.AUTO,
    val quranDisplayMode: QuranDisplayMode = QuranDisplayMode.STUDY,
    val quranReciter: QuranReciter = QuranReciter.YASSER_AL_DOSARI,
    val audioPlaybackSpeed: Float = 1.0f,
    val audioQualityLabel: String = "128 kbps",
    // Personal Worship Goals & Dashboard Config
    val dailyQuranGoalVerses: Int = 15,
    val dailyTasbihGoal: Int = 100,
    val homeConfig: HomeDashboardConfig = HomeDashboardConfig(),
    // Advanced Adhan Customization, Persistent Notification & Quran Auto-Scroll
    val adhanSoundEnabled: Boolean = true,
    val adhanNotificationOnly: Boolean = false,
    val adhanVolume: Float = 1.0f,
    val adhanRespectSilentMode: Boolean = true,
    val adhanConfig: PerPrayerAdhanConfig = PerPrayerAdhanConfig(),
    val customAdhanRecords: List<CustomAdhanRecord> = emptyList(),
    val persistentPrayerNotificationEnabled: Boolean = true,
    val quranAutoScrollSpeed: Float = 1.0f,
    // User-side Background Visual Effects Controls
    val backgroundStarsEnabled: Boolean = true,
    val backgroundAuroraEnabled: Boolean = true
) {
    fun isPrayerNotificationEnabled(prayer: PrayerName): Boolean = when (prayer) {
        PrayerName.FAJR -> notifyFajr
        PrayerName.SUNRISE -> false
        PrayerName.DHUHR -> notifyDhuhr
        PrayerName.ASR -> notifyAsr
        PrayerName.MAGHRIB -> notifyMaghrib
        PrayerName.ISHA -> notifyIsha
    }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val CUSTOM_AVATAR_URI = stringPreferencesKey("custom_avatar_uri")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CITY_ID = stringPreferencesKey("city_id")
        val CITY_NAME_AR = stringPreferencesKey("city_name_ar")
        val CITY_NAME_EN = stringPreferencesKey("city_name_en")
        val COUNTRY_AR = stringPreferencesKey("country_ar")
        val COUNTRY_EN = stringPreferencesKey("country_en")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val TIMEZONE_ID = stringPreferencesKey("timezone_id")
        val IS_AUTO_LOCATION = booleanPreferencesKey("is_auto_location")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val ASR_MADHHAB = stringPreferencesKey("asr_madhhab")
        val OFFSET_FAJR = intPreferencesKey("offset_fajr")
        val OFFSET_SUNRISE = intPreferencesKey("offset_sunrise")
        val OFFSET_DHUHR = intPreferencesKey("offset_dhuhr")
        val OFFSET_ASR = intPreferencesKey("offset_asr")
        val OFFSET_MAGHRIB = intPreferencesKey("offset_maghrib")
        val OFFSET_ISHA = intPreferencesKey("offset_isha")
        val IQAMAH_ENABLED = booleanPreferencesKey("iqamah_enabled")
        val IQAMAH_MOSQUE_NAME = stringPreferencesKey("iqamah_mosque_name")
        val IQAMAH_FAJR = intPreferencesKey("iqamah_fajr")
        val IQAMAH_DHUHR = intPreferencesKey("iqamah_dhuhr")
        val IQAMAH_ASR = intPreferencesKey("iqamah_asr")
        val IQAMAH_MAGHRIB = intPreferencesKey("iqamah_maghrib")
        val IQAMAH_ISHA = intPreferencesKey("iqamah_isha")
        val HIJRI_ADJUST = intPreferencesKey("hijri_adjust")

        val NOTIFY_FAJR = booleanPreferencesKey("notify_fajr")
        val NOTIFY_DHUHR = booleanPreferencesKey("notify_dhuhr")
        val NOTIFY_ASR = booleanPreferencesKey("notify_asr")
        val NOTIFY_MAGHRIB = booleanPreferencesKey("notify_maghrib")
        val NOTIFY_ISHA = booleanPreferencesKey("notify_isha")
        val PRE_PRAYER_MIN = intPreferencesKey("pre_prayer_min")
        val REMIND_MORNING = booleanPreferencesKey("remind_morning")
        val REMIND_MORNING_H = intPreferencesKey("remind_morning_h")
        val REMIND_MORNING_M = intPreferencesKey("remind_morning_m")
        val REMIND_EVENING = booleanPreferencesKey("remind_evening")
        val REMIND_EVENING_H = intPreferencesKey("remind_evening_h")
        val REMIND_EVENING_M = intPreferencesKey("remind_evening_m")
        val REMIND_TAHAJJUD = booleanPreferencesKey("remind_tahajjud")
        val REMIND_TAHAJJUD_H = intPreferencesKey("remind_tahajjud_h")
        val REMIND_TAHAJJUD_M = intPreferencesKey("remind_tahajjud_m")
        val REMIND_MORNING_SOUND = stringPreferencesKey("remind_morning_sound")
        val REMIND_EVENING_SOUND = stringPreferencesKey("remind_evening_sound")
        val REMIND_TAHAJJUD_SOUND = stringPreferencesKey("remind_tahajjud_sound")
        val REMIND_QURAN = booleanPreferencesKey("remind_quran")
        val REMIND_QURAN_H = intPreferencesKey("remind_quran_h")
        val REMIND_QURAN_M = intPreferencesKey("remind_quran_m")
        val REMIND_TASBIH = booleanPreferencesKey("remind_tasbih")
        val REMIND_TASBIH_H = intPreferencesKey("remind_tasbih_h")
        val REMIND_TASBIH_M = intPreferencesKey("remind_tasbih_m")

        val TASBIH_VIBRATION = booleanPreferencesKey("tasbih_vibration")
        val TASBIH_SOUND = booleanPreferencesKey("tasbih_sound")
        val ACTIVE_TASBIH_PRESET = stringPreferencesKey("active_tasbih_preset")
        val ACTIVE_TASBIH_COUNT = intPreferencesKey("active_tasbih_count")
        val ACTIVE_TASBIH_TARGET = intPreferencesKey("active_tasbih_target")
        val CUSTOM_TASBIH_TEXT = stringPreferencesKey("custom_tasbih_text")

        val LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        val LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val LAST_READ_PAGE = intPreferencesKey("last_read_page")
        val QURAN_FONT_SIZE = intPreferencesKey("quran_font_size")
        val QURAN_READING_THEME = stringPreferencesKey("quran_reading_theme")
        val QURAN_DISPLAY_MODE = stringPreferencesKey("quran_display_mode")
        val QURAN_RECITER = stringPreferencesKey("quran_reciter")
        val AUDIO_SPEED = floatPreferencesKey("audio_speed")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")

        val DAILY_QURAN_GOAL = intPreferencesKey("daily_quran_goal")
        val DAILY_TASBIH_GOAL = intPreferencesKey("daily_tasbih_goal")
        val HOME_SHOW_PRAYER_STRIP = booleanPreferencesKey("home_show_prayer_strip")
        val HOME_SHOW_AYAH = booleanPreferencesKey("home_show_ayah")
        val HOME_SHOW_HADITH = booleanPreferencesKey("home_show_hadith")
        val HOME_SHOW_ADHKAR = booleanPreferencesKey("home_show_adhkar")
        val HOME_SHOW_QURAN = booleanPreferencesKey("home_show_quran")
        val HOME_SHOW_AUDIO = booleanPreferencesKey("home_show_audio")
        val HOME_SHOW_EVENTS = booleanPreferencesKey("home_show_events")

        val ADHAN_SOUND_ENABLED = booleanPreferencesKey("adhan_sound_enabled")
        val ADHAN_NOTIFICATION_ONLY = booleanPreferencesKey("adhan_notification_only")
        val ADHAN_VOLUME = floatPreferencesKey("adhan_volume")
        val ADHAN_RESPECT_SILENT = booleanPreferencesKey("adhan_respect_silent")
        val ADHAN_DEFAULT_VOICE = stringPreferencesKey("adhan_default_voice")
        val ADHAN_FAJR_VOICE = stringPreferencesKey("adhan_fajr_voice")
        val ADHAN_DHUHR_VOICE = stringPreferencesKey("adhan_dhuhr_voice")
        val ADHAN_ASR_VOICE = stringPreferencesKey("adhan_asr_voice")
        val ADHAN_MAGHRIB_VOICE = stringPreferencesKey("adhan_maghrib_voice")
        val ADHAN_ISHA_VOICE = stringPreferencesKey("adhan_isha_voice")
        val ADHAN_CUSTOM_RECORDS = stringPreferencesKey("adhan_custom_records")
        val PERSISTENT_PRAYER_NOTIF = booleanPreferencesKey("persistent_prayer_notif")
        val QURAN_AUTO_SCROLL_SPEED = floatPreferencesKey("quran_auto_scroll_speed")
        val BACKGROUND_STARS_ENABLED = booleanPreferencesKey("background_stars_enabled")
        val BACKGROUND_AURORA_ENABLED = booleanPreferencesKey("background_aurora_enabled")
    }

    private fun resolveSystemLocale(): java.util.Locale {
        val cfgLocales = context.resources.configuration.locales
        return if (!cfgLocales.isEmpty) cfgLocales[0] else java.util.Locale.getDefault()
    }

    val preferencesFlow: Flow<UserPreferences> = context.noorDataStore.data.map { p ->
        val storedLang = p[Keys.LANGUAGE]
        UserPreferences(
            userName = p[Keys.USER_NAME] ?: "",
            customAvatarUri = p[Keys.CUSTOM_AVATAR_URI] ?: "",
            language = if (storedLang.isNullOrBlank()) {
                AppLanguage.fromSystemLocale(resolveSystemLocale())
            } else {
                AppLanguage.fromCode(storedLang)
            },
            themeMode = ThemeMode.fromKey(p[Keys.THEME_MODE]),
            cityId = p[Keys.CITY_ID] ?: CityCatalog.defaultCity.id,
            cityNameAr = p[Keys.CITY_NAME_AR] ?: CityCatalog.defaultCity.nameAr,
            cityNameEn = p[Keys.CITY_NAME_EN] ?: CityCatalog.defaultCity.nameEn,
            countryAr = p[Keys.COUNTRY_AR] ?: CityCatalog.defaultCity.countryAr,
            countryEn = p[Keys.COUNTRY_EN] ?: CityCatalog.defaultCity.countryEn,
            latitude = p[Keys.LATITUDE] ?: CityCatalog.defaultCity.latitude,
            longitude = p[Keys.LONGITUDE] ?: CityCatalog.defaultCity.longitude,
            timezoneId = p[Keys.TIMEZONE_ID] ?: CityCatalog.defaultCity.timezoneId,
            isAutoLocation = p[Keys.IS_AUTO_LOCATION] ?: false,
            calculationMethod = CalculationMethod.fromKey(p[Keys.CALC_METHOD]),
            asrMadhhab = AsrMadhhab.fromKey(p[Keys.ASR_MADHHAB]),
            offsets = PrayerMinuteOffsets(
                fajr = p[Keys.OFFSET_FAJR] ?: 0,
                sunrise = p[Keys.OFFSET_SUNRISE] ?: 0,
                dhuhr = p[Keys.OFFSET_DHUHR] ?: 0,
                asr = p[Keys.OFFSET_ASR] ?: 0,
                maghrib = p[Keys.OFFSET_MAGHRIB] ?: 0,
                isha = p[Keys.OFFSET_ISHA] ?: 0
            ),
            iqamahDelays = IqamahDelays(
                enabled = p[Keys.IQAMAH_ENABLED] ?: false,
                mosqueName = p[Keys.IQAMAH_MOSQUE_NAME] ?: "",
                fajr = p[Keys.IQAMAH_FAJR] ?: 20,
                dhuhr = p[Keys.IQAMAH_DHUHR] ?: 15,
                asr = p[Keys.IQAMAH_ASR] ?: 15,
                maghrib = p[Keys.IQAMAH_MAGHRIB] ?: 10,
                isha = p[Keys.IQAMAH_ISHA] ?: 15
            ),
            hijriAdjustmentDays = p[Keys.HIJRI_ADJUST] ?: 0,
            notifyFajr = p[Keys.NOTIFY_FAJR] ?: true,
            notifyDhuhr = p[Keys.NOTIFY_DHUHR] ?: true,
            notifyAsr = p[Keys.NOTIFY_ASR] ?: true,
            notifyMaghrib = p[Keys.NOTIFY_MAGHRIB] ?: true,
            notifyIsha = p[Keys.NOTIFY_ISHA] ?: true,
            prePrayerAlertMinutes = p[Keys.PRE_PRAYER_MIN] ?: 10,
            morningAdhkarReminderEnabled = p[Keys.REMIND_MORNING] ?: true,
            morningAdhkarHour = p[Keys.REMIND_MORNING_H] ?: 6,
            morningAdhkarMinute = p[Keys.REMIND_MORNING_M] ?: 30,
            eveningAdhkarReminderEnabled = p[Keys.REMIND_EVENING] ?: true,
            eveningAdhkarHour = p[Keys.REMIND_EVENING_H] ?: 17,
            eveningAdhkarMinute = p[Keys.REMIND_EVENING_M] ?: 30,
            tahajjudReminderEnabled = p[Keys.REMIND_TAHAJJUD] ?: true,
            tahajjudReminderHour = p[Keys.REMIND_TAHAJJUD_H] ?: 2,
            tahajjudReminderMinute = p[Keys.REMIND_TAHAJJUD_M] ?: 30,
            morningReminderSoundId = p[Keys.REMIND_MORNING_SOUND] ?: AdhanCatalog.DEFAULT_VOICE_ID,
            eveningReminderSoundId = p[Keys.REMIND_EVENING_SOUND] ?: AdhanCatalog.DEFAULT_VOICE_ID,
            tahajjudReminderSoundId = p[Keys.REMIND_TAHAJJUD_SOUND] ?: AdhanCatalog.DEFAULT_VOICE_ID,
            quranReminderEnabled = p[Keys.REMIND_QURAN] ?: true,
            quranReminderHour = p[Keys.REMIND_QURAN_H] ?: 21,
            quranReminderMinute = p[Keys.REMIND_QURAN_M] ?: 0,
            tasbihReminderEnabled = p[Keys.REMIND_TASBIH] ?: false,
            tasbihReminderHour = p[Keys.REMIND_TASBIH_H] ?: 15,
            tasbihReminderMinute = p[Keys.REMIND_TASBIH_M] ?: 0,
            tasbihVibrationEnabled = p[Keys.TASBIH_VIBRATION] ?: true,
            tasbihSoundEnabled = p[Keys.TASBIH_SOUND] ?: true,
            activeTasbihPresetId = p[Keys.ACTIVE_TASBIH_PRESET] ?: "subhanallah",
            activeTasbihCount = p[Keys.ACTIVE_TASBIH_COUNT] ?: 0,
            activeTasbihTarget = p[Keys.ACTIVE_TASBIH_TARGET] ?: 33,
            customTasbihText = p[Keys.CUSTOM_TASBIH_TEXT] ?: "سبحان الله وبحمده",
            lastReadSurah = p[Keys.LAST_READ_SURAH] ?: 1,
            lastReadAyah = p[Keys.LAST_READ_AYAH] ?: 1,
            lastReadPage = p[Keys.LAST_READ_PAGE]
                ?: com.example.data.local.catalog.SurahCatalog.findSurah(p[Keys.LAST_READ_SURAH] ?: 1).startPage,
            quranFontSizeSp = p[Keys.QURAN_FONT_SIZE] ?: 24,
            quranReadingTheme = QuranReadingTheme.fromKey(p[Keys.QURAN_READING_THEME]),
            quranDisplayMode = QuranDisplayMode.fromKey(p[Keys.QURAN_DISPLAY_MODE]),
            quranReciter = QuranReciter.fromId(p[Keys.QURAN_RECITER]),
            audioPlaybackSpeed = p[Keys.AUDIO_SPEED] ?: 1.0f,
            audioQualityLabel = p[Keys.AUDIO_QUALITY] ?: "128 kbps",
            dailyQuranGoalVerses = p[Keys.DAILY_QURAN_GOAL] ?: 15,
            dailyTasbihGoal = p[Keys.DAILY_TASBIH_GOAL] ?: 100,
            homeConfig = HomeDashboardConfig(
                showPrayerStrip = p[Keys.HOME_SHOW_PRAYER_STRIP] ?: true,
                showDailyAyah = p[Keys.HOME_SHOW_AYAH] ?: true,
                showDailyHadith = p[Keys.HOME_SHOW_HADITH] ?: true,
                showAdhkarShortcut = p[Keys.HOME_SHOW_ADHKAR] ?: true,
                showQuranProgress = p[Keys.HOME_SHOW_QURAN] ?: true,
                showRecentAudio = p[Keys.HOME_SHOW_AUDIO] ?: true,
                showUpcomingEvent = p[Keys.HOME_SHOW_EVENTS] ?: true
            ),
            adhanSoundEnabled = p[Keys.ADHAN_SOUND_ENABLED] ?: true,
            adhanNotificationOnly = p[Keys.ADHAN_NOTIFICATION_ONLY] ?: false,
            adhanVolume = (p[Keys.ADHAN_VOLUME] ?: 1.0f).coerceIn(0.1f, 1.0f),
            adhanRespectSilentMode = p[Keys.ADHAN_RESPECT_SILENT] ?: true,
            adhanConfig = PerPrayerAdhanConfig(
                defaultVoiceId = p[Keys.ADHAN_DEFAULT_VOICE] ?: AdhanCatalog.DEFAULT_VOICE_ID,
                fajrVoiceId = p[Keys.ADHAN_FAJR_VOICE] ?: "",
                dhuhrVoiceId = p[Keys.ADHAN_DHUHR_VOICE] ?: "",
                asrVoiceId = p[Keys.ADHAN_ASR_VOICE] ?: "",
                maghribVoiceId = p[Keys.ADHAN_MAGHRIB_VOICE] ?: "",
                ishaVoiceId = p[Keys.ADHAN_ISHA_VOICE] ?: ""
            ),
            customAdhanRecords = AdhanCatalog.decodeCustomRecords(p[Keys.ADHAN_CUSTOM_RECORDS] ?: ""),
            persistentPrayerNotificationEnabled = p[Keys.PERSISTENT_PRAYER_NOTIF] ?: true,
            quranAutoScrollSpeed = p[Keys.QURAN_AUTO_SCROLL_SPEED] ?: 1.0f,
            backgroundStarsEnabled = p[Keys.BACKGROUND_STARS_ENABLED] ?: true,
            backgroundAuroraEnabled = p[Keys.BACKGROUND_AURORA_ENABLED] ?: true
        )
    }

    suspend fun getPreferencesOnce(): UserPreferences = preferencesFlow.first()

    suspend fun setUserName(name: String) {
        context.noorDataStore.edit { it[Keys.USER_NAME] = name.trim().take(48) }
    }

    suspend fun setCustomAvatarUri(uri: String) {
        context.noorDataStore.edit { it[Keys.CUSTOM_AVATAR_URI] = uri.trim() }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.noorDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.noorDataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setCity(city: CityLocation, isAuto: Boolean = false) {
        context.noorDataStore.edit {
            it[Keys.CITY_ID] = city.id
            it[Keys.CITY_NAME_AR] = city.nameAr
            it[Keys.CITY_NAME_EN] = city.nameEn
            it[Keys.COUNTRY_AR] = city.countryAr
            it[Keys.COUNTRY_EN] = city.countryEn
            it[Keys.LATITUDE] = city.latitude
            it[Keys.LONGITUDE] = city.longitude
            it[Keys.TIMEZONE_ID] = city.timezoneId
            it[Keys.IS_AUTO_LOCATION] = isAuto
        }
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        context.noorDataStore.edit { it[Keys.CALC_METHOD] = method.key }
    }

    suspend fun setAsrMadhhab(madhhab: AsrMadhhab) {
        context.noorDataStore.edit { it[Keys.ASR_MADHHAB] = madhhab.key }
    }

    suspend fun setPrayerOffset(prayer: PrayerName, offsetMinutes: Int) {
        val clamped = offsetMinutes.coerceIn(-30, 30)
        context.noorDataStore.edit {
            when (prayer) {
                PrayerName.FAJR -> it[Keys.OFFSET_FAJR] = clamped
                PrayerName.SUNRISE -> it[Keys.OFFSET_SUNRISE] = clamped
                PrayerName.DHUHR -> it[Keys.OFFSET_DHUHR] = clamped
                PrayerName.ASR -> it[Keys.OFFSET_ASR] = clamped
                PrayerName.MAGHRIB -> it[Keys.OFFSET_MAGHRIB] = clamped
                PrayerName.ISHA -> it[Keys.OFFSET_ISHA] = clamped
            }
        }
    }

    suspend fun setIqamahDelays(delays: IqamahDelays) {
        context.noorDataStore.edit {
            it[Keys.IQAMAH_ENABLED] = delays.enabled
            it[Keys.IQAMAH_MOSQUE_NAME] = delays.mosqueName.trim().take(60)
            it[Keys.IQAMAH_FAJR] = delays.fajr.coerceIn(0, 60)
            it[Keys.IQAMAH_DHUHR] = delays.dhuhr.coerceIn(0, 60)
            it[Keys.IQAMAH_ASR] = delays.asr.coerceIn(0, 60)
            it[Keys.IQAMAH_MAGHRIB] = delays.maghrib.coerceIn(0, 60)
            it[Keys.IQAMAH_ISHA] = delays.isha.coerceIn(0, 60)
        }
    }

    suspend fun setHijriAdjustment(days: Int) {
        context.noorDataStore.edit { it[Keys.HIJRI_ADJUST] = days.coerceIn(-2, 2) }
    }

    suspend fun setPrayerNotification(prayer: PrayerName, enabled: Boolean) {
        context.noorDataStore.edit {
            when (prayer) {
                PrayerName.FAJR -> it[Keys.NOTIFY_FAJR] = enabled
                PrayerName.DHUHR -> it[Keys.NOTIFY_DHUHR] = enabled
                PrayerName.ASR -> it[Keys.NOTIFY_ASR] = enabled
                PrayerName.MAGHRIB -> it[Keys.NOTIFY_MAGHRIB] = enabled
                PrayerName.ISHA -> it[Keys.NOTIFY_ISHA] = enabled
                PrayerName.SUNRISE -> Unit
            }
        }
    }

    suspend fun setPrePrayerAlertMinutes(minutes: Int) {
        context.noorDataStore.edit { it[Keys.PRE_PRAYER_MIN] = minutes.coerceIn(0, 60) }
    }

    suspend fun setReminder(
        type: String,
        enabled: Boolean,
        hour: Int? = null,
        minute: Int? = null
    ) {
        val safeHour = hour?.coerceIn(0, 23)
        val safeMinute = minute?.coerceIn(0, 59)
        context.noorDataStore.edit {
            when (type) {
                "morning" -> {
                    it[Keys.REMIND_MORNING] = enabled
                    if (safeHour != null) it[Keys.REMIND_MORNING_H] = safeHour
                    if (safeMinute != null) it[Keys.REMIND_MORNING_M] = safeMinute
                }
                "evening" -> {
                    it[Keys.REMIND_EVENING] = enabled
                    if (safeHour != null) it[Keys.REMIND_EVENING_H] = safeHour
                    if (safeMinute != null) it[Keys.REMIND_EVENING_M] = safeMinute
                }
                "tahajjud" -> {
                    it[Keys.REMIND_TAHAJJUD] = enabled
                    if (safeHour != null) it[Keys.REMIND_TAHAJJUD_H] = safeHour
                    if (safeMinute != null) it[Keys.REMIND_TAHAJJUD_M] = safeMinute
                }
                "quran" -> {
                    it[Keys.REMIND_QURAN] = enabled
                    if (safeHour != null) it[Keys.REMIND_QURAN_H] = safeHour
                    if (safeMinute != null) it[Keys.REMIND_QURAN_M] = safeMinute
                }
                "tasbih" -> {
                    it[Keys.REMIND_TASBIH] = enabled
                    if (safeHour != null) it[Keys.REMIND_TASBIH_H] = safeHour
                    if (safeMinute != null) it[Keys.REMIND_TASBIH_M] = safeMinute
                }
            }
        }
    }

    suspend fun setReminderSound(type: String, voiceId: String) {
        val cleanId = voiceId.ifBlank { AdhanCatalog.DEFAULT_VOICE_ID }
        context.noorDataStore.edit {
            when (type) {
                "morning" -> it[Keys.REMIND_MORNING_SOUND] = cleanId
                "evening" -> it[Keys.REMIND_EVENING_SOUND] = cleanId
                "tahajjud" -> it[Keys.REMIND_TAHAJJUD_SOUND] = cleanId
            }
        }
    }

    suspend fun setTasbihVibration(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.TASBIH_VIBRATION] = enabled }
    }

    suspend fun setTasbihSound(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.TASBIH_SOUND] = enabled }
    }

    suspend fun saveActiveTasbih(presetId: String, count: Int, target: Int, customText: String? = null) {
        context.noorDataStore.edit {
            it[Keys.ACTIVE_TASBIH_PRESET] = presetId
            it[Keys.ACTIVE_TASBIH_COUNT] = count
            it[Keys.ACTIVE_TASBIH_TARGET] = target
            if (customText != null) it[Keys.CUSTOM_TASBIH_TEXT] = customText
        }
    }

    suspend fun saveLastRead(surahNumber: Int, ayahNumber: Int, pageNumber: Int? = null) {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val safeAyah = ayahNumber.coerceAtLeast(1)
        val resolvedPage = pageNumber?.coerceIn(1, 604)
            ?: com.example.data.local.catalog.SurahCatalog.findSurah(safeSurah).startPage
        context.noorDataStore.edit {
            it[Keys.LAST_READ_SURAH] = safeSurah
            it[Keys.LAST_READ_AYAH] = safeAyah
            it[Keys.LAST_READ_PAGE] = resolvedPage
        }
    }

    suspend fun setQuranFontSize(sizeSp: Int) {
        context.noorDataStore.edit { it[Keys.QURAN_FONT_SIZE] = sizeSp.coerceIn(18, 38) }
    }

    suspend fun setQuranReadingTheme(theme: QuranReadingTheme) {
        context.noorDataStore.edit { it[Keys.QURAN_READING_THEME] = theme.key }
    }

    suspend fun setQuranDisplayMode(mode: QuranDisplayMode) {
        context.noorDataStore.edit { it[Keys.QURAN_DISPLAY_MODE] = mode.key }
    }

    suspend fun setQuranReciter(reciter: QuranReciter) {
        context.noorDataStore.edit { it[Keys.QURAN_RECITER] = reciter.id }
    }

    suspend fun setAudioPlaybackSpeed(speed: Float) {
        context.noorDataStore.edit { it[Keys.AUDIO_SPEED] = speed.coerceIn(0.75f, 1.5f) }
    }

    suspend fun setAudioQualityLabel(quality: String) {
        context.noorDataStore.edit { it[Keys.AUDIO_QUALITY] = quality.trim().ifBlank { "128 kbps" } }
    }

    suspend fun setDailyGoals(quranVerses: Int, tasbihCount: Int) {
        context.noorDataStore.edit {
            it[Keys.DAILY_QURAN_GOAL] = quranVerses.coerceIn(5, 500)
            it[Keys.DAILY_TASBIH_GOAL] = tasbihCount.coerceIn(33, 5000)
        }
    }

    suspend fun setHomeDashboardConfig(config: HomeDashboardConfig) {
        context.noorDataStore.edit {
            it[Keys.HOME_SHOW_PRAYER_STRIP] = config.showPrayerStrip
            it[Keys.HOME_SHOW_AYAH] = config.showDailyAyah
            it[Keys.HOME_SHOW_HADITH] = config.showDailyHadith
            it[Keys.HOME_SHOW_ADHKAR] = config.showAdhkarShortcut
            it[Keys.HOME_SHOW_QURAN] = config.showQuranProgress
            it[Keys.HOME_SHOW_AUDIO] = config.showRecentAudio
            it[Keys.HOME_SHOW_EVENTS] = config.showUpcomingEvent
        }
    }

    suspend fun setAdhanSoundEnabled(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.ADHAN_SOUND_ENABLED] = enabled }
    }

    suspend fun setAdhanRespectSilentMode(respect: Boolean) {
        context.noorDataStore.edit { it[Keys.ADHAN_RESPECT_SILENT] = respect }
    }

    suspend fun setDefaultAdhanVoice(voiceId: String) {
        context.noorDataStore.edit { it[Keys.ADHAN_DEFAULT_VOICE] = voiceId }
    }

    suspend fun setPrayerSpecificAdhanVoice(prayer: PrayerName, voiceId: String) {
        context.noorDataStore.edit {
            when (prayer) {
                PrayerName.FAJR -> it[Keys.ADHAN_FAJR_VOICE] = voiceId
                PrayerName.DHUHR -> it[Keys.ADHAN_DHUHR_VOICE] = voiceId
                PrayerName.ASR -> it[Keys.ADHAN_ASR_VOICE] = voiceId
                PrayerName.MAGHRIB -> it[Keys.ADHAN_MAGHRIB_VOICE] = voiceId
                PrayerName.ISHA -> it[Keys.ADHAN_ISHA_VOICE] = voiceId
                PrayerName.SUNRISE -> Unit
            }
        }
    }

    suspend fun addCustomAdhanRecord(record: CustomAdhanRecord) {
        context.noorDataStore.edit { prefs ->
            val existing = AdhanCatalog.decodeCustomRecords(prefs[Keys.ADHAN_CUSTOM_RECORDS] ?: "")
            val updated = existing.filterNot { it.id == record.id } + record
            prefs[Keys.ADHAN_CUSTOM_RECORDS] = AdhanCatalog.encodeCustomRecords(updated)
        }
    }

    suspend fun removeCustomAdhanRecord(recordId: String) {
        context.noorDataStore.edit { prefs ->
            val existing = AdhanCatalog.decodeCustomRecords(prefs[Keys.ADHAN_CUSTOM_RECORDS] ?: "")
            val updated = existing.filterNot { it.id == recordId }
            prefs[Keys.ADHAN_CUSTOM_RECORDS] = AdhanCatalog.encodeCustomRecords(updated)
            if (prefs[Keys.ADHAN_DEFAULT_VOICE] == recordId) {
                prefs[Keys.ADHAN_DEFAULT_VOICE] = AdhanCatalog.DEFAULT_VOICE_ID
            }
            if (prefs[Keys.ADHAN_FAJR_VOICE] == recordId) prefs[Keys.ADHAN_FAJR_VOICE] = ""
            if (prefs[Keys.ADHAN_DHUHR_VOICE] == recordId) prefs[Keys.ADHAN_DHUHR_VOICE] = ""
            if (prefs[Keys.ADHAN_ASR_VOICE] == recordId) prefs[Keys.ADHAN_ASR_VOICE] = ""
            if (prefs[Keys.ADHAN_MAGHRIB_VOICE] == recordId) prefs[Keys.ADHAN_MAGHRIB_VOICE] = ""
            if (prefs[Keys.ADHAN_ISHA_VOICE] == recordId) prefs[Keys.ADHAN_ISHA_VOICE] = ""
            if (prefs[Keys.REMIND_MORNING_SOUND] == recordId) prefs[Keys.REMIND_MORNING_SOUND] = AdhanCatalog.DEFAULT_VOICE_ID
            if (prefs[Keys.REMIND_EVENING_SOUND] == recordId) prefs[Keys.REMIND_EVENING_SOUND] = AdhanCatalog.DEFAULT_VOICE_ID
            if (prefs[Keys.REMIND_TAHAJJUD_SOUND] == recordId) prefs[Keys.REMIND_TAHAJJUD_SOUND] = AdhanCatalog.DEFAULT_VOICE_ID
        }
    }

    suspend fun setAdhanNotificationOnly(notificationOnly: Boolean) {
        context.noorDataStore.edit { it[Keys.ADHAN_NOTIFICATION_ONLY] = notificationOnly }
    }

    suspend fun setAdhanVolume(volume: Float) {
        context.noorDataStore.edit { it[Keys.ADHAN_VOLUME] = volume.coerceIn(0.1f, 1.0f) }
    }

    suspend fun restoreAdhanDefaults() {
        context.noorDataStore.edit {
            it[Keys.ADHAN_SOUND_ENABLED] = true
            it[Keys.ADHAN_NOTIFICATION_ONLY] = false
            it[Keys.ADHAN_VOLUME] = 1.0f
            it[Keys.ADHAN_RESPECT_SILENT] = true
            it[Keys.ADHAN_DEFAULT_VOICE] = AdhanCatalog.DEFAULT_VOICE_ID
            it[Keys.ADHAN_FAJR_VOICE] = ""
            it[Keys.ADHAN_DHUHR_VOICE] = ""
            it[Keys.ADHAN_ASR_VOICE] = ""
            it[Keys.ADHAN_MAGHRIB_VOICE] = ""
            it[Keys.ADHAN_ISHA_VOICE] = ""
            it[Keys.NOTIFY_FAJR] = true
            it[Keys.NOTIFY_DHUHR] = true
            it[Keys.NOTIFY_ASR] = true
            it[Keys.NOTIFY_MAGHRIB] = true
            it[Keys.NOTIFY_ISHA] = true
        }
    }

    suspend fun setPersistentPrayerNotificationEnabled(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.PERSISTENT_PRAYER_NOTIF] = enabled }
    }

    suspend fun setQuranAutoScrollSpeed(speed: Float) {
        context.noorDataStore.edit { it[Keys.QURAN_AUTO_SCROLL_SPEED] = speed.coerceIn(0.4f, 3.0f) }
    }

    suspend fun setBackgroundStarsEnabled(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.BACKGROUND_STARS_ENABLED] = enabled }
    }

    suspend fun setBackgroundAuroraEnabled(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.BACKGROUND_AURORA_ENABLED] = enabled }
    }

    suspend fun restoreFromPreferences(prefs: UserPreferences) {
        context.noorDataStore.edit {
            it[Keys.USER_NAME] = prefs.userName
            it[Keys.CUSTOM_AVATAR_URI] = prefs.customAvatarUri
            it[Keys.LANGUAGE] = prefs.language.code
            it[Keys.THEME_MODE] = prefs.themeMode.key
            it[Keys.CITY_ID] = prefs.cityId
            it[Keys.CITY_NAME_AR] = prefs.cityNameAr
            it[Keys.CITY_NAME_EN] = prefs.cityNameEn
            it[Keys.COUNTRY_AR] = prefs.countryAr
            it[Keys.COUNTRY_EN] = prefs.countryEn
            it[Keys.LATITUDE] = prefs.latitude
            it[Keys.LONGITUDE] = prefs.longitude
            it[Keys.TIMEZONE_ID] = prefs.timezoneId
            it[Keys.IS_AUTO_LOCATION] = prefs.isAutoLocation
            it[Keys.CALC_METHOD] = prefs.calculationMethod.key
            it[Keys.ASR_MADHHAB] = prefs.asrMadhhab.key
            it[Keys.OFFSET_FAJR] = prefs.offsets.fajr
            it[Keys.OFFSET_SUNRISE] = prefs.offsets.sunrise
            it[Keys.OFFSET_DHUHR] = prefs.offsets.dhuhr
            it[Keys.OFFSET_ASR] = prefs.offsets.asr
            it[Keys.OFFSET_MAGHRIB] = prefs.offsets.maghrib
            it[Keys.OFFSET_ISHA] = prefs.offsets.isha
            it[Keys.IQAMAH_ENABLED] = prefs.iqamahDelays.enabled
            it[Keys.IQAMAH_MOSQUE_NAME] = prefs.iqamahDelays.mosqueName
            it[Keys.IQAMAH_FAJR] = prefs.iqamahDelays.fajr
            it[Keys.IQAMAH_DHUHR] = prefs.iqamahDelays.dhuhr
            it[Keys.IQAMAH_ASR] = prefs.iqamahDelays.asr
            it[Keys.IQAMAH_MAGHRIB] = prefs.iqamahDelays.maghrib
            it[Keys.IQAMAH_ISHA] = prefs.iqamahDelays.isha
            it[Keys.HIJRI_ADJUST] = prefs.hijriAdjustmentDays
            it[Keys.NOTIFY_FAJR] = prefs.notifyFajr
            it[Keys.NOTIFY_DHUHR] = prefs.notifyDhuhr
            it[Keys.NOTIFY_ASR] = prefs.notifyAsr
            it[Keys.NOTIFY_MAGHRIB] = prefs.notifyMaghrib
            it[Keys.NOTIFY_ISHA] = prefs.notifyIsha
            it[Keys.PRE_PRAYER_MIN] = prefs.prePrayerAlertMinutes
            it[Keys.REMIND_MORNING] = prefs.morningAdhkarReminderEnabled
            it[Keys.REMIND_MORNING_H] = prefs.morningAdhkarHour
            it[Keys.REMIND_MORNING_M] = prefs.morningAdhkarMinute
            it[Keys.REMIND_EVENING] = prefs.eveningAdhkarReminderEnabled
            it[Keys.REMIND_EVENING_H] = prefs.eveningAdhkarHour
            it[Keys.REMIND_EVENING_M] = prefs.eveningAdhkarMinute
            it[Keys.REMIND_TAHAJJUD] = prefs.tahajjudReminderEnabled
            it[Keys.REMIND_TAHAJJUD_H] = prefs.tahajjudReminderHour
            it[Keys.REMIND_TAHAJJUD_M] = prefs.tahajjudReminderMinute
            it[Keys.REMIND_MORNING_SOUND] = prefs.morningReminderSoundId
            it[Keys.REMIND_EVENING_SOUND] = prefs.eveningReminderSoundId
            it[Keys.REMIND_TAHAJJUD_SOUND] = prefs.tahajjudReminderSoundId
            it[Keys.REMIND_QURAN] = prefs.quranReminderEnabled
            it[Keys.REMIND_TASBIH] = prefs.tasbihReminderEnabled
            it[Keys.TASBIH_VIBRATION] = prefs.tasbihVibrationEnabled
            it[Keys.TASBIH_SOUND] = prefs.tasbihSoundEnabled
            it[Keys.LAST_READ_SURAH] = prefs.lastReadSurah
            it[Keys.LAST_READ_AYAH] = prefs.lastReadAyah
            it[Keys.LAST_READ_PAGE] = prefs.lastReadPage
            it[Keys.QURAN_FONT_SIZE] = prefs.quranFontSizeSp
            it[Keys.QURAN_READING_THEME] = prefs.quranReadingTheme.key
            it[Keys.QURAN_DISPLAY_MODE] = prefs.quranDisplayMode.key
            it[Keys.QURAN_RECITER] = prefs.quranReciter.id
            it[Keys.AUDIO_SPEED] = prefs.audioPlaybackSpeed
            it[Keys.DAILY_QURAN_GOAL] = prefs.dailyQuranGoalVerses
            it[Keys.DAILY_TASBIH_GOAL] = prefs.dailyTasbihGoal
            it[Keys.HOME_SHOW_PRAYER_STRIP] = prefs.homeConfig.showPrayerStrip
            it[Keys.HOME_SHOW_AYAH] = prefs.homeConfig.showDailyAyah
            it[Keys.HOME_SHOW_HADITH] = prefs.homeConfig.showDailyHadith
            it[Keys.HOME_SHOW_ADHKAR] = prefs.homeConfig.showAdhkarShortcut
            it[Keys.HOME_SHOW_QURAN] = prefs.homeConfig.showQuranProgress
            it[Keys.HOME_SHOW_AUDIO] = prefs.homeConfig.showRecentAudio
            it[Keys.HOME_SHOW_EVENTS] = prefs.homeConfig.showUpcomingEvent
            it[Keys.BACKGROUND_STARS_ENABLED] = prefs.backgroundStarsEnabled
            it[Keys.BACKGROUND_AURORA_ENABLED] = prefs.backgroundAuroraEnabled
        }
    }

    suspend fun clearAll() {
        context.noorDataStore.edit { it.clear() }
    }
}
