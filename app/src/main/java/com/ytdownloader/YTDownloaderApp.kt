package com.ytdownloader

import android.app.Application
import com.ytdownloader.python.BinaryManager

class YTDownloaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BinaryManager.initialize(this)
    }
}
