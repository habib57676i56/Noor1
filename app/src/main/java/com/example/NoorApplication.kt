package com.example

import android.app.Application
import com.example.data.cloud.NoorCloudRepository
import com.example.data.local.db.NoorDatabase
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.remote.OverpassApiService
import com.example.data.remote.QuranApiService
import com.example.data.repository.NoorRepository
import com.example.notifications.NoorNotificationHelper
import com.google.firebase.FirebaseApp

class NoorApplication : Application() {

    lateinit var repository: NoorRepository
        private set

    lateinit var cloudRepository: NoorCloudRepository
        private set

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
    }
}
