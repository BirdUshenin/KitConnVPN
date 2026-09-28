package com.kitconnvpn.domain.usecase

import com.kitconnvpn.core.model.VpnConfig
import com.kitconnvpn.domain.repository.VpnRepository

class GetVpnConfigsUseCase(
    private val repository: VpnRepository
) {
    suspend operator fun invoke(appVersion: Int): Result<List<VpnConfig>> {
        return repository.fetchConfigs(appVersion)
    }
}
