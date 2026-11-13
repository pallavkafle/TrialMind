package com.example.trailmind

import com.example.trailmind.data.local.TokenStore
import android.os.StrictMode
import android.content.pm.ApplicationInfo
import com.example.trailmind.ui.map.MapConfig
import com.example.trailmind.data.local.OfflineCache
import android.app.Application
import com.example.trailmind.data.remote.ApiClient

class TrailMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads().detectDiskWrites().detectNetwork()
                    .penaltyLog().build()
            )
        }
        ApiClient.init(this)
        OfflineCache.init(this)
        MapConfig.init(this)
        // heavy first-time setup runs off the main thread so the first screen is not delayed
        Thread {
            runCatching {
                TokenStore.get(this).warmUp()
                ApiClient.warmUp()
            }
        }.start()
    }
}