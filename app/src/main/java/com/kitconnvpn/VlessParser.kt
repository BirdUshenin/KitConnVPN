package com.kitconnvpn

import android.net.Uri

object VlessParser {

    fun parse(url: String): VlessConfig {
        val uri = Uri.parse(url)

        require(uri.scheme == "vless") {
            "Invalid VLESS URL"
        }

        val uuid = requireNotNull(uri.userInfo) {
            "UUID is missing"
        }

        val address = requireNotNull(uri.host) {
            "Address is missing"
        }

        val port = uri.port.takeIf { it != -1 }
            ?: error("Port is missing")

        val query = uri.queryParameterNames.associateWith {
            uri.getQueryParameter(it)
        }

        return VlessConfig(
            uuid = uuid,
            address = address,
            port = port,
            encryption = query["encryption"] ?: "none",
            security = query["security"] ?: "none",
            type = query["type"] ?: "tcp",
            serverName = query["sni"],
            fingerprint = query["fp"],
            publicKey = query["pbk"],
            shortId = query["sid"],
            spiderX = query["spx"]
        )
    }
}