package com.kitconnvpn

import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {

    fun build(config: VlessConfig): String {

        val realitySettings = JSONObject().apply {
            put("serverName", config.serverName)
            put("fingerprint", config.fingerprint ?: "chrome")
            put("publicKey", config.publicKey)
            put("shortId", config.shortId)
            put("spiderX", config.spiderX)
        }

        val outbound = JSONObject().apply {
            put("protocol", "vless")

            put(
                "settings",
                JSONObject().apply {
                    put(
                        "vnext",
                        JSONArray().put(
                            JSONObject().apply {
                                put("address", config.address)
                                put("port", config.port)

                                put(
                                    "users",
                                    JSONArray().put(
                                        JSONObject().apply {
                                            put("id", config.uuid)
                                            put("encryption", config.encryption)
                                        }
                                    )
                                )
                            }
                        )
                    )
                }
            )

            put(
                "streamSettings",
                JSONObject().apply {
                    put("network", config.type)
                    put("security", config.security)

                    if (config.security == "reality") {
                        put(
                            "realitySettings",
                            realitySettings
                        )
                    }

                    if (config.security == "tls") {
                        put(
                            "tlsSettings",
                            JSONObject().apply {
                                config.serverName?.let {
                                    put("serverName", it)
                                }

                                config.fingerprint?.let {
                                    put("fingerprint", it)
                                }
                            }
                        )
                    }

                    if (config.type == "xhttp") {
                        put(
                            "xhttpSettings",
                            buildXhttpSettings(config)
                        )
                    }
                }
            )
        }

        return JSONObject().apply {
            put(
                "inbounds",
                JSONArray().put(
                    JSONObject().apply {
                        put("tag", "tun-in")
                        put("protocol", "tun")
                        put("settings", JSONObject().apply {
                            put("name", "tun0")
                            put("mtu", 1500)
                        })
                    }
                )
            )

            put(
                "outbounds",
                JSONArray().put(outbound).put(
                    JSONObject().apply {
                        put("tag", "direct")
                        put("protocol", "freedom")
                    }
                )
            )

            put(
                "dns",
                JSONObject().apply {
                    put("servers", JSONArray().put("1.1.1.1").put("8.8.8.8"))
                }
            )
        }.toString()
    }
}

private fun buildXhttpSettings(
    config: VlessConfig
): JSONObject {
    val parameters = config.parameters

    return JSONObject().apply {
        put("host", config.serverName ?: config.address)

        parameters["path"]?.let {
            put("path", it)
        }

        parameters["mode"]?.let {
            put("mode", it)
        }

        parameters["extra"]?.let {
            put("extra", JSONObject(it))
        }
    }
}