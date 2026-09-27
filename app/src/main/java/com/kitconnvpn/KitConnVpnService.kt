package com.kitconnvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class KitConnVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayManager: XrayManager? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        vpnInterface = Builder()
            .setSession("KitConn")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .establish()

        if (vpnInterface == null) {
            Log.e(TAG, "Failed to establish VPN interface")
            stopSelf()
            return START_NOT_STICKY
        }

        val config = ""

        xrayManager = XrayManager()

        xrayManager = XrayManager()

        try {
            xrayManager?.start(
                config = config,
                tunFd = vpnInterface!!.fd
            )

            Log.d(TAG, "Xray started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray", e)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        xrayManager?.stop()
        xrayManager = null

        vpnInterface?.close()
        vpnInterface = null

        super.onDestroy()
    }

    companion object {
        private const val TAG = "KitConnVpnService"
    }
}