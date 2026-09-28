package com.kitconnvpn.domain.usecase

import com.kitconnvpn.domain.repository.VpnRepository

class PingServersUseCase(
    private val repository: VpnRepository
) {
    operator fun invoke() {
        repository.pingAllServers()
    }
}
