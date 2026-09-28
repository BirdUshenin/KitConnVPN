package com.kitconnvpn.domain.repository

import com.kitconnvpn.core.model.VpnConfig
import com.kitconnvpn.VpnState
import kotlinx.coroutines.flow.StateFlow

interface VpnRepository {
    val vpnState: StateFlow<VpnState>
    val isRouteAnimating: StateFlow<Boolean>
    val routeProgress: StateFlow<Float>
    val isSuccessFlash: StateFlow<Boolean>
    val durationSeconds: StateFlow<Long>
    val downloadSpeedMb: StateFlow<String>
    val uploadSpeedMb: StateFlow<String>
    val pingMs: StateFlow<Int>
    val totalTrafficMb: StateFlow<Double>
    val configs: StateFlow<List<VpnConfig>>
    val selectedConfig: StateFlow<VpnConfig?>
    val isLoadingConfigs: StateFlow<Boolean>
    val serverPings: StateFlow<Map<String, Int?>>
    val isPingingAll: StateFlow<Boolean>
    val isUpdateRequired: StateFlow<Boolean>

    suspend fun fetchConfigs(appVersion: Int): Result<List<VpnConfig>>
    fun selectConfig(config: VpnConfig)
    fun pingAllServers()
    fun startConnectingAnimation(onStartService: () -> Unit)
    fun onVpnStopped()
    fun getServerHost(): String?
    fun getCountryFlag(country: String): String
    fun formatDuration(seconds: Long): String
}
