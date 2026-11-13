package com.example.trailmind

import com.example.trailmind.data.local.OfflineCache
import android.app.Application
import com.example.trailmind.data.remote.ApiClient

class TrailMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        OfflineCache.init(this)
    }
}