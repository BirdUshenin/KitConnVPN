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
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Stopping VPN service via ACTION_STOP")
            stopVpn()
            return START_NOT_STICKY
        }

        val vlessUrl = intent?.getStringExtra(EXTRA_VLESS_URL)

        if (vlessUrl == null) {
            Log.e(TAG, "VLESS URL is missing")
            stopVpn()
            return START_NOT_STICKY
        }

        VpnStateRepository.onVpnStarting()

        val builder = Builder()
            .setSession("KitConn")
            .addAddress("10.0.0.2", 24)
            .addAddress("fd00::1", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .setMtu(1500)

        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disallow application", e)
        }

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            Log.e(TAG, "Failed to establish VPN interface")
            stopVpn()
            return START_NOT_STICKY
        }

        xrayManager = XrayManager()

        try {
            val vlessConfig = VlessParser.parse(vlessUrl)
            val xrayConfig = XrayConfigBuilder.build(vlessConfig)

            xrayManager?.start(
                config = xrayConfig,
                tunFd = vpnInterface!!.fd
            )

            Log.d(TAG, "Xray started successfully")
            VpnStateRepository.onVpnStarted()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray", e)
            stopVpn()
        }

        return START_STICKY
    }

    override fun onRevoke() {
        Log.d(TAG, "VPN service revoked by system settings")
        stopVpn()
        super.onRevoke()
    }

    private fun stopVpn() {
        Log.d(TAG, "Executing stopVpn cleanup")

        try {
            xrayManager?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping xray manager", e)
        }
        xrayManager = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        VpnStateRepository.onVpnStopped()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "KitConnVpnService"
        const val EXTRA_VLESS_URL = "extra_vless_url"
        const val ACTION_STOP = "com.kitconnvpn.ACTION_STOP"
    }
}
