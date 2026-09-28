package com.kitconnvpn.presentation.main

import com.kitconnvpn.core.model.VpnConfig

sealed interface MainAction {
    data object ToggleVpn : MainAction
    data class SelectConfig(val config: VpnConfig) : MainAction
    data object RefreshConfigs : MainAction
    data object PingAllServers : MainAction
    data class SetBottomSheetVisible(val visible: Boolean) : MainAction
}
