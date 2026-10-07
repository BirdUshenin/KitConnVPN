package com.kitconnvpn.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.kitconnvpn.KitConnVpnService
import com.kitconnvpn.R
import com.kitconnvpn.VpnState
import com.kitconnvpn.VpnStateRepository
import com.kitconnvpn.app.MainActivity
import androidx.core.graphics.toColorInt

class VpnWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) toggle(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggle(context: Context) {
        if (VpnStateRepository.vpnState.value != VpnState.DISCONNECTED) {
            context.startService(
                Intent(
                    context,
                    KitConnVpnService::class.java
                ).setAction(KitConnVpnService.ACTION_STOP)
            )
            return
        }

        val url = VpnStateRepository.selectedConfig.value?.config ?: VpnStateRepository.savedConfigUrl()

        if (url == null || VpnService.prepare(context) != null) {
            context.startActivity(
                Intent(
                    context,
                    MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        context.startForegroundService(
            Intent(
                context,
                KitConnVpnService::class.java
            ).putExtra(KitConnVpnService.EXTRA_VLESS_URL, url)
        )
    }

    companion object {
        private const val ACTION_TOGGLE = "com.kitconnvpn.widget.ACTION_TOGGLE"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VpnWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val state = VpnStateRepository.vpnState.value
            val server = VpnStateRepository.selectedConfig.value
                ?.let { it.name.ifEmpty { it.country } }
                .orEmpty()

            val (statusText, color) = when (state) {
                VpnState.CONNECTED -> "Подключено" to "#84F938".toColorInt()
                VpnState.CONNECTING -> "Подключение…" to "#00E5FF".toColorInt()
                VpnState.DISCONNECTED -> "Не подключено" to "#546E7A".toColorInt()
            }

            val toggle = PendingIntent.getBroadcast(
                context,
                0,
                Intent(
                    context,
                    VpnWidgetProvider::class.java
                ).setAction(ACTION_TOGGLE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            return RemoteViews(context.packageName, R.layout.widget_vpn).apply {
                setTextViewText(R.id.widget_status, statusText)
                setTextViewText(R.id.widget_server, server)
                setInt(R.id.widget_power, "setColorFilter", color)
                setOnClickPendingIntent(R.id.widget_power, toggle)
                setOnClickPendingIntent(R.id.widget_root, toggle)
            }
        }
    }
}