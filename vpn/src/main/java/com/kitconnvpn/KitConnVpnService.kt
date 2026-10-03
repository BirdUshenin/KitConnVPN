package com.kitconnvpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.kitconnvpn.vpn.R

class KitConnVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayManager: XrayManager? = null
    private var connectedAt = 0L

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

        // Сервис запущен через startForegroundService: уведомление нужно показать до любой работы
        showNotification(connected = false)

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
            vpnInterface?.let { vpnInterface ->
                xrayManager?.start(
                    config = xrayConfig,
                    tunFd = vpnInterface.fd
                )
            }
            Log.d(TAG, "Xray started successfully")
            VpnStateRepository.onVpnStarted()
            showNotification(connected = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray", e)
            stopVpn()
        }

        return START_STICKY
    }

    private fun showNotification(connected: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Статус VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }

        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, KitConnVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val config = VpnStateRepository.selectedConfig.value
        val server = config?.let {
            val flag = VpnStateRepository.getCountryFlag(it.country)
            val name = it.name.ifEmpty { it.country }
            listOfNotNull(flag, name, it.subtitle.takeIf { sub -> sub.isNotEmpty() })
                .joinToString(" ")
        }

        if (connected && connectedAt == 0L) connectedAt = System.currentTimeMillis()
        if (!connected) connectedAt = 0L

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setContentTitle(if (connected) "Подключено" else "Подключение…")
            .setContentText(server)
            .setContentIntent(openApp)
            .addAction(0, "Отключить", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Тёмная подложка в цвет приложения; текст система подбирает светлый сама
            .setColor(BRAND_BACKGROUND)
            .setColorized(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (connected) {
            // Системный секундомер времени подключения: тикает без обновлений уведомления
            builder.setUsesChronometer(true).setShowWhen(true).setWhen(connectedAt)
        } else {
            builder.setProgress(0, 0, true)
        }
        val notification = builder.build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
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
        private const val CHANNEL_ID = "vpn_status"
        private const val NOTIFICATION_ID = 1
        private const val BRAND_BACKGROUND = 0xFF0C0D14.toInt()
        const val EXTRA_VLESS_URL = "extra_vless_url"
        const val ACTION_STOP = "com.kitconnvpn.ACTION_STOP"
    }
}
