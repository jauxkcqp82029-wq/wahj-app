package com.example

import android.app.Application
import com.example.data.local.WahjDatabase
import com.example.data.local.WahjPreferences
import com.example.data.repository.WahjRepository
import com.example.data.sync.LocalLanSyncEngine
import com.example.system.NotificationHelper

class WahjApplication : Application() {
    lateinit var database: WahjDatabase
        private set
    lateinit var preferences: WahjPreferences
        private set
    lateinit var lanSyncEngine: LocalLanSyncEngine
        private set
    lateinit var repository: WahjRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        database = WahjDatabase.getInstance(this)
        preferences = WahjPreferences(this)
        lanSyncEngine = LocalLanSyncEngine()
        repository = WahjRepository(
            context = this,
            db = database,
            prefs = preferences,
            lanSync = lanSyncEngine
        )
    }
}
