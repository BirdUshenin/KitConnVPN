package com.kitconnvpn

data class VlessConfig(
    val uuid: String,
    val address: String,
    val port: Int,
    val encryption: String,
    val security: String,
    val type: String,
    val parameters: Map<String, String>,
    val name: String?
) {
    val serverName: String?
        get() = parameters["sni"]

    val fingerprint: String?
        get() = parameters["fp"]

    val publicKey: String?
        get() = parameters["pbk"]

    val shortId: String?
        get() = parameters["sid"]

    val spiderX: String?
        get() = parameters["spx"]
}
