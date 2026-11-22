package com.example.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage

/**
 * Manages NOOR Android Home Screen App Shortcuts (deduplicated stable IDs, localized Arabic/English):
 * 1. Open Quran
 * 2. Resume Last Reading (preserves exact last-read Surah & Ayah)
 * 3. Start Quran Listening
 * 4. Open Prayer Times
 * 5. Open Qibla
 * 6. Open Adhan Settings
 */
object NoorShortcutManager {

    const val EXTRA_SHORTCUT_ROUTE = "extra_shortcut_route"

    const val TARGET_QURAN = "quran"
    const val TARGET_RESUME_READING = "resume_reading"
    const val TARGET_START_LISTENING = "start_listening"
    const val TARGET_PRAYER_TIMES = "prayers"
    const val TARGET_QIBLA = "qibla"
    const val TARGET_ADHAN_SETTINGS = "adhan_settings"
    const val TARGET_AUDIO_LIBRARY = "audio_library"

    private data class ShortcutSpec(
        val id: String,
        val targetRoute: String,
        val shortAr: String,
        val shortEn: String,
        val longAr: String,
        val longEn: String,
        val iconRes: Int
    )

    private val specs = listOf(
        ShortcutSpec(
            id = "noor_sc_resume_reading",
            targetRoute = TARGET_RESUME_READING,
            shortAr = "متابعة القراءة",
            shortEn = "Resume Reading",
            longAr = "متابعة آخر موضع قراءة في القرآن",
            longEn = "Resume Last Quran Reading",
            iconRes = R.drawable.ic_shortcut_resume
        ),
        ShortcutSpec(
            id = "noor_sc_open_quran",
            targetRoute = TARGET_QURAN,
            shortAr = "القرآن الكريم",
            shortEn = "Open Quran",
            longAr = "فتح المصحف الشريف",
            longEn = "Open the Holy Quran",
            iconRes = R.drawable.ic_shortcut_quran
        ),
        ShortcutSpec(
            id = "noor_sc_start_listening",
            targetRoute = TARGET_START_LISTENING,
            shortAr = "الاستماع للقرآن",
            shortEn = "Listen Quran",
            longAr = "تشغيل الاستماع للقرآن الكريم",
            longEn = "Start Quran Listening",
            iconRes = R.drawable.ic_shortcut_audio
        ),
        ShortcutSpec(
            id = "noor_sc_prayer_times",
            targetRoute = TARGET_PRAYER_TIMES,
            shortAr = "مواقيت الصلاة",
            shortEn = "Prayer Times",
            longAr = "عرض مواقيت الصلاة اليوم",
            longEn = "Open Daily Prayer Times",
            iconRes = R.drawable.ic_shortcut_prayer
        ),
        ShortcutSpec(
            id = "noor_sc_qibla",
            targetRoute = TARGET_QIBLA,
            shortAr = "اتجاه القبلة",
            shortEn = "Open Qibla",
            longAr = "بوصلة اتجاه القبلة",
            longEn = "Open Qibla Compass",
            iconRes = R.drawable.ic_shortcut_qibla
        ),
        ShortcutSpec(
            id = "noor_sc_adhan_settings",
            targetRoute = TARGET_ADHAN_SETTINGS,
            shortAr = "إعدادات الأذان",
            shortEn = "Adhan Settings",
            longAr = "تخصيص أصوات الأذان والتنبيهات",
            longEn = "Open Adhan Sound Settings",
            iconRes = R.drawable.ic_shortcut_adhan
        )
    )

    fun publishShortcuts(context: Context, prefs: UserPreferences) {
        val isAr = prefs.language == AppLanguage.ARABIC
        val maxCount = runCatching {
            ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        }.getOrDefault(4).coerceAtLeast(4)

        val shortcuts = specs.take(maxCount.coerceAtMost(specs.size)).map { spec ->
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SHORTCUT_ROUTE, spec.targetRoute)
            }
            ShortcutInfoCompat.Builder(context, spec.id)
                .setShortLabel(if (isAr) spec.shortAr else spec.shortEn)
                .setLongLabel(if (isAr) spec.longAr else spec.longEn)
                .setIcon(IconCompat.createWithResource(context, spec.iconRes))
                .setIntent(intent)
                .build()
        }

        // setDynamicShortcuts replaces the full list by stable ID — never creates duplicates
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    fun requestPinShortcut(context: Context, targetRoute: String, isArabic: Boolean): Boolean {
        val spec = specs.firstOrNull { it.targetRoute == targetRoute } ?: return false
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHORTCUT_ROUTE, spec.targetRoute)
        }
        val info = ShortcutInfoCompat.Builder(context, spec.id)
            .setShortLabel(if (isArabic) spec.shortAr else spec.shortEn)
            .setLongLabel(if (isArabic) spec.longAr else spec.longEn)
            .setIcon(IconCompat.createWithResource(context, spec.iconRes))
            .setIntent(intent)
            .build()
        return runCatching {
            ShortcutManagerCompat.requestPinShortcut(context, info, null)
        }.getOrDefault(false)
    }

    fun allSupportedTargetRoutes(): List<String> = specs.map { it.targetRoute }
}
