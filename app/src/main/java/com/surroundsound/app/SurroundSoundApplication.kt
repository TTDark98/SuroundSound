package com.surroundsound.app

import android.app.Application
import com.surroundsound.app.di.AppContainer

class SurroundSoundApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
