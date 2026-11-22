package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.NoorApplication
import com.example.R
import com.example.audio.QuranAudioService
import com.example.data.local.catalog.SurahCatalog
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.domain.model.AppLanguage
import com.example.shortcuts.NoorShortcutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Interactive Home Screen Quran Player Widget connected directly to the live QuranAudioService
 * and application-scoped QuranAudioPlayer.
 * Controls real background playback (Previous, Play/Pause, Next) and displays current Surah,
 * Reciter, live status, and reading/playback progress.
 */
class QuranPlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            runCatching {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, QuranPlayerWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, QuranPlayerWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            }
        }

        private suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val prefs = UserPreferencesRepository(context).getPreferencesOnce()
            val isAr = prefs.language == AppLanguage.ARABIC
            val app = context.applicationContext as? NoorApplication
            val audioState = app?.quranAudioPlayer?.state?.value

            val lastSurah = SurahCatalog.findSurah(audioState?.playingSurah ?: prefs.lastReadSurah)
            val title = if (audioState != null && audioState.hasActiveSession) {
                (if (isAr) audioState.titleAr else audioState.titleEn)
                    .ifBlank { if (isAr) "سورة ${lastSurah.nameAr}" else "Surah ${lastSurah.nameEn}" }
            } else {
                if (isAr) "سورة ${lastSurah.nameAr}" else "Surah ${lastSurah.nameEn}"
            }

            val reciter = if (audioState != null && audioState.hasActiveSession) {
                (if (isAr) audioState.subtitleAr else audioState.subtitleEn)
                    .ifBlank { if (isAr) prefs.quranReciter.nameAr else prefs.quranReciter.nameEn }
            } else {
                if (isAr) prefs.quranReciter.nameAr else prefs.quranReciter.nameEn
            }

            val statusLabel = when {
                audioState?.isBuffering == true -> if (isAr) "جارٍ التحميل..." else "Buffering..."
                audioState?.isPlaying == true -> if (isAr) "جارٍ التلاوة الآن" else "Playing Now"
                audioState?.hasActiveSession == true -> if (isAr) "متوقف مؤقتاً" else "Paused"
                else -> if (isAr) {
                    "آخر قراءة: آية ${prefs.lastReadAyah}"
                } else {
                    "Last Read: Ayah ${prefs.lastReadAyah}"
                }
            }

            val progressPct = if (audioState != null && audioState.durationMs > 0L) {
                ((audioState.currentPositionMs * 100L) / audioState.durationMs).toInt().coerceIn(0, 100)
            } else {
                ((prefs.lastReadAyah * 100) / lastSurah.verseCount.coerceAtLeast(1)).coerceIn(0, 100)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NoorShortcutManager.EXTRA_SHORTCUT_ROUTE, NoorShortcutManager.TARGET_AUDIO_LIBRARY)
            }
            val openPending = PendingIntent.getActivity(
                context,
                6200,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val prevPending = servicePendingIntent(context, QuranAudioService.ACTION_PREVIOUS, 6201)
            val playPausePending = servicePendingIntent(context, QuranAudioService.ACTION_PLAY_PAUSE, 6202)
            val nextPending = servicePendingIntent(context, QuranAudioService.ACTION_NEXT, 6203)

            val playPauseIcon = if (audioState?.isPlaying == true) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }

            for (appWidgetId in appWidgetIds) {
                val rv = RemoteViews(context.packageName, R.layout.widget_quran_player)
                rv.setTextViewText(R.id.widget_quran_title, title)
                rv.setTextViewText(R.id.widget_quran_reciter, reciter)
                rv.setTextViewText(R.id.widget_quran_status, statusLabel)
                rv.setProgressBar(R.id.widget_quran_progress, 100, progressPct, false)
                rv.setImageViewResource(R.id.widget_quran_btn_play, playPauseIcon)

                rv.setOnClickPendingIntent(R.id.widget_quran_info_area, openPending)
                rv.setOnClickPendingIntent(R.id.widget_quran_btn_prev, prevPending)
                rv.setOnClickPendingIntent(R.id.widget_quran_btn_play, playPausePending)
                rv.setOnClickPendingIntent(R.id.widget_quran_btn_next, nextPending)

                appWidgetManager.updateAppWidget(appWidgetId, rv)
            }
        }

        private fun servicePendingIntent(context: Context, actionName: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, QuranAudioService::class.java).apply {
                action = actionName
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }
    }
}
