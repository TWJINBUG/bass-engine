package com.bassengine.control

import android.app.Application
import com.bassengine.control.data.ConfigRepository
import com.bassengine.control.data.LogRepository

class BassEngineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        ConfigRepository.init(this)
        LogRepository.init(this)
    }

    companion object {
        lateinit var instance: BassEngineApp
            private set
    }
}
