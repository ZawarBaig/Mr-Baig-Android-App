package com.mrbaigsdownloader

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MrBaigApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initYtDlp()
    }

    private fun initYtDlp() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                Log.d("MrBaigApp", "yt-dlp initialized successfully")
            } catch (e: YoutubeDLException) {
                Log.e("MrBaigApp", "yt-dlp init failed: ${e.message}")
            } catch (e: Exception) {
                Log.e("MrBaigApp", "yt-dlp init error: ${e.message}")
            }
        }
    }
}
