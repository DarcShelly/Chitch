package com.chitch.app

import android.app.Application
import com.chitch.app.di.AppContainer
import com.chitch.app.notification.ChitchNotifier

class ChitchApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ChitchNotifier.ensureChannel(this)
    }
}
