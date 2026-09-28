package com.kitconnvpn.di

import com.kitconnvpn.data.repository.VpnRepositoryImpl
import com.kitconnvpn.domain.repository.VpnRepository
import com.kitconnvpn.domain.usecase.GetVpnConfigsUseCase
import com.kitconnvpn.domain.usecase.PingServersUseCase
import com.kitconnvpn.core.network.kitConnApi
import com.kitconnvpn.presentation.main.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { kitConnApi }
    single<VpnRepository> { VpnRepositoryImpl(get()) }

    factory { GetVpnConfigsUseCase(get()) }
    factory { PingServersUseCase(get()) }

    viewModel { MainViewModel(get(), get(), get()) }
}
