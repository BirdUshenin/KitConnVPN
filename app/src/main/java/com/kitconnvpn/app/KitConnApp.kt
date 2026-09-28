package com.kitconnvpn.app

import android.app.Application
import com.kitconnvpn.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KitConnApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@KitConnApp)
            modules(appModule)
        }
    }
}