package com.kitconnvpn

import android.net.Uri

object VlessParser {

    private const val MAX_EXTRA_DECODES = 3

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

        // Пустые значения (sni=, host=, flow=) считаем отсутствующими, иначе в конфиг уйдут ""
        val parameters = uri.queryParameterNames
            .associateWith { key -> uri.getQueryParameter(key).orEmpty() }
            .filterValues { it.isNotBlank() }
            .toMutableMap()

        // extra бывает закодирован дважды (%257B...): getQueryParameter снял только один слой
        parameters["extra"]?.let { extra ->
            var decoded = extra
            repeat(MAX_EXTRA_DECODES) {
                if (!decoded.trimStart().startsWith("{")) decoded = Uri.decode(decoded)
            }
            parameters["extra"] = decoded
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
