package com.kitconnvpn.presentation.main

import com.kitconnvpn.core.model.VpnConfig
import com.kitconnvpn.VpnState

data class MainUiState(
    val isLoading: Boolean = false,
    val vpnState: VpnState = VpnState.DISCONNECTED,
    val durationSeconds: Long = 0L,
    val configs: List<VpnConfig> = emptyList(),
    val selectedConfig: VpnConfig? = null,
    val isLoadingConfigs: Boolean = false,
    val isUpdateRequired: Boolean = false,
    val downloadSpeedMb: String = "0.0",
    val uploadSpeedMb: String = "0.0",
    val pingMs: Int = 0,
    val totalTrafficMb: Double = 0.0,
    val serverPings: Map<String, Int?> = emptyMap(),
    val isPingingAll: Boolean = false,
    val isRouteAnimating: Boolean = false,
    val routeProgress: Float = 0f,
    val isSuccessFlash: Boolean = false,
    val showBottomSheet: Boolean = false
)
