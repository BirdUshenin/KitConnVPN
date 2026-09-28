package com.kitconnvpn.data.repository

import com.kitconnvpn.core.network.KitConnApi
import com.kitconnvpn.core.model.VpnConfig
import com.kitconnvpn.VpnState
import com.kitconnvpn.VpnStateRepository
import com.kitconnvpn.domain.repository.VpnRepository
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

class VpnRepositoryImpl(
    private val api: KitConnApi
) : VpnRepository {
    override val vpnState: StateFlow<VpnState> = VpnStateRepository.vpnState
    override val isRouteAnimating: StateFlow<Boolean> = VpnStateRepository.isRouteAnimating
    override val routeProgress: StateFlow<Float> = VpnStateRepository.routeProgress
    override val isSuccessFlash: StateFlow<Boolean> = VpnStateRepository.isSuccessFlash
    override val durationSeconds: StateFlow<Long> = VpnStateRepository.durationSeconds
    override val downloadSpeedMb: StateFlow<String> = VpnStateRepository.downloadSpeedMb
    override val uploadSpeedMb: StateFlow<String> = VpnStateRepository.uploadSpeedMb
    override val pingMs: StateFlow<Int> = VpnStateRepository.pingMs
    override val totalTrafficMb: StateFlow<Double> = VpnStateRepository.totalTrafficMb
    override val configs: StateFlow<List<VpnConfig>> = VpnStateRepository.configs
    override val selectedConfig: StateFlow<VpnConfig?> = VpnStateRepository.selectedConfig
    override val isLoadingConfigs: StateFlow<Boolean> = VpnStateRepository.isLoadingConfigs
    override val serverPings: StateFlow<Map<String, Int?>> = VpnStateRepository.serverPings
    override val isPingingAll: StateFlow<Boolean> = VpnStateRepository.isPingingAll
    override val isUpdateRequired: StateFlow<Boolean> = VpnStateRepository.isUpdateRequired

    override suspend fun fetchConfigs(appVersion: Int): Result<List<VpnConfig>> {
        VpnStateRepository.setLoadingConfigs(true)
        return try {
            val response = api.getConfigs(appVersion)
            VpnStateRepository.setRequireUpdate(false)
            VpnStateRepository.setConfigs(response.configs)
            Result.success(response.configs)
        } catch (e: HttpException) {
            if (e.code() == 426) {
                VpnStateRepository.setRequireUpdate(true)
            }
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            VpnStateRepository.setLoadingConfigs(false)
        }
    }

    override fun selectConfig(config: VpnConfig) {
        VpnStateRepository.selectConfig(config)
    }

    override fun pingAllServers() {
        VpnStateRepository.pingAllServers()
    }

    override fun startConnectingAnimation(onStartService: () -> Unit) {
        VpnStateRepository.startConnectingAnimation(onStartService)
    }

    override fun onVpnStopped() {
        VpnStateRepository.onVpnStopped()
    }

    override fun getServerHost(): String? {
        return VpnStateRepository.getServerHost()
    }

    override fun getCountryFlag(country: String): String {
        return VpnStateRepository.getCountryFlag(country)
    }

    override fun formatDuration(seconds: Long): String {
        return VpnStateRepository.formatDuration(seconds)
    }
}
