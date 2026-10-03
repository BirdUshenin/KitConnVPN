package com.kitconnvpn.app

import android.app.Application
import com.kitconnvpn.VpnStateRepository
import com.kitconnvpn.app.widget.VpnWidgetProvider
import com.kitconnvpn.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KitConnApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        VpnStateRepository.init(this)

        startKoin {
            androidContext(this@KitConnApp)
            modules(appModule)
        }

        // Виджет живёт отдельно от активити, поэтому обновляем его от состояния репозитория
        combine(VpnStateRepository.vpnState, VpnStateRepository.selectedConfig) { _, _ -> }
            .onEach { VpnWidgetProvider.updateAll(this) }
            .launchIn(appScope)
    }
}
