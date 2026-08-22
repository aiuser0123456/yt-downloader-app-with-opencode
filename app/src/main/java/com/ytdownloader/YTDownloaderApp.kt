package com.ytdownloader

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL

class YTDownloaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            Log.e("YTDownloader", "Failed to initialize youtubedl-android", e)
        }
    }
}
