package com.kitconnvpn

import kotlinx.serialization.Serializable
@Serializable
data class VpnConfig(
    val version: Int,
    val country: String,
    val name: String,
    val subtitle: String,
    val config: String
)

@Serializable
data class VpnConfigsResponse(
    val configs: List<VpnConfig>
)