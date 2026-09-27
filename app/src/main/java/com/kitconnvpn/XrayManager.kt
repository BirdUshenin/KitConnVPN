package com.kitconnvpn

import android.util.Log
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray.newCoreController

class XrayManager {

    private val callback = object : CoreCallbackHandler {

        override fun onEmitStatus(
            p0: Long,
            p1: String?
        ): Long {
            Log.d(TAG, "Xray status: $p0 $p1")
            return 0
        }

        override fun shutdown(): Long {
            Log.d(TAG, "Xray stopped")
            return 0
        }

        override fun startup(): Long {
            Log.d(TAG, "Xray started")
            return 0
        }
    }

    private val controller: CoreController =
        newCoreController(callback)

    fun start(
        config: String,
        tunFd: Int
    ) {
        controller.startLoop(
            config,
            tunFd
        )
    }

    fun stop() {
        controller.stopLoop()
    }

    companion object {
        private const val TAG = "KitConnXray"
    }
}