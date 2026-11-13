package com.example.trailmind.ui.map

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

// OpenStreetMap's tile servers need an app-specific user agent and a writable cache, or they refuse the tiles
object MapConfig {
    fun init(context: Context) {
        val cfg = Configuration.getInstance()
        val base = File(context.cacheDir, "osmdroid")
        cfg.userAgentValue = "TrailMind/1.0 (${context.packageName})"
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = File(base, "tiles")
        cfg.tileFileSystemCacheMaxBytes = 150L * 1024 * 1024
        cfg.tileFileSystemCacheTrimBytes = 120L * 1024 * 1024
    }
}