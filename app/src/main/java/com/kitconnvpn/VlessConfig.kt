package com.kitconnvpn

data class VlessConfig(
    val uuid: String,
    val address: String,
    val port: Int,
    val encryption: String,
    val security: String,
    val type: String,
    val serverName: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val spiderX: String? = null,
)