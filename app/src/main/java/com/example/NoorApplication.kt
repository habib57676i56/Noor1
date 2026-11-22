package com.example

import android.app.Application
import com.example.audio.AdhanAudioPlayer
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranAudioService
import com.example.data.cloud.NoorCloudRepository
import com.example.data.local.db.NoorDatabase
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.remote.OverpassApiService
import com.example.data.remote.QuranApiService
import com.example.data.repository.EcosystemRepository
import com.example.data.repository.NoorRepository
import com.example.notifications.NoorNotificationHelper
import com.example.shortcuts.NoorShortcutManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoorApplication : Application() {

    lateinit var repository: NoorRepository
        private set

    lateinit var cloudRepository: NoorCloudRepository
        private set

    lateinit var quranAudioPlayer: QuranAudioPlayer
        private set

    lateinit var adhanAudioPlayer: AdhanAudioPlayer
        private set

    lateinit var ecosystemRepository: EcosystemRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {
        }
        NoorNotificationHelper.createChannels(this)
        val db = NoorDatabase.getInstance(this)
        val prefs = UserPreferencesRepository(this)
        val api = QuranApiService.create()
        val overpass = OverpassApiService()
        repository = NoorRepository(
            dao = db.noorDao(),
            prefs = prefs,
            quranApi = api,
            overpassApi = overpass,
            appContext = applicationContext
        )
        cloudRepository = NoorCloudRepository(applicationContext)
        quranAudioPlayer = QuranAudioPlayer(applicationContext).apply {
            configureCallbacks(
                resolveLocalFile = { trackId -> repository.getCachedAudioPath(trackId) },
                onTrackStarted = { track ->
                    appScope.launch {
                        repository.recordRecentAudio(track)
                        cloudRepository.incrementDailyAnalytics(audioPlaysDelta = 1L, section = "audio")
                    }
                }
            )
            setStateListener { st ->
                if (st.hasActiveSession) {
                    val isAr = try {
                        resources.configuration.locales[0].language == "ar"
                    } catch (_: Exception) {
                        true
                    }
                    val title = (if (isAr) st.titleAr else st.titleEn).ifBlank { "NOOR Audio" }
                    val sub = if (isAr) st.subtitleAr else st.subtitleEn
                    QuranAudioService.updateNotification(applicationContext, title, sub, st.isPlaying)
                }
            }
        }
        adhanAudioPlayer = AdhanAudioPlayer(applicationContext)
        ecosystemRepository = EcosystemRepository(
            dao = db.ecosystemDao(),
            noorDao = db.noorDao(),
            appContext = applicationContext
        )

        appScope.launch {
            runCatching { ecosystemRepository.ensureSeeded() }
            runCatching {
                val p = prefs.getPreferencesOnce()
                NoorShortcutManager.publishShortcuts(applicationContext, p)
            }
        }
    }
}
