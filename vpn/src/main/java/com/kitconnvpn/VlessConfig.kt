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
    val flow: String?
        get() = parameters["flow"]

    val allowInsecure: Boolean
        get() = parameters["allowInsecure"].let { it == "1" || it == "true" }

    val alpn: List<String>
        get() = parameters["alpn"]?.split(',')?.filter { it.isNotBlank() }.orEmpty()

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
