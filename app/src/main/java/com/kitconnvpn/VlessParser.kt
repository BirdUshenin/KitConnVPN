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

        val parameters = uri.queryParameterNames.associateWith { key ->
            uri.getQueryParameter(key).orEmpty()
        }

        return VlessConfig(
            uuid = uuid,
            address = address,
            port = port,
            encryption = parameters["encryption"] ?: "none",
            security = parameters["security"] ?: "none",
            type = parameters["type"] ?: "tcp",
            parameters = parameters,
            name = uri.fragment
        )
    }
}