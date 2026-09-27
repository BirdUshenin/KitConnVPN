package com.kitconnvpn

import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

object VpnStateRepository {

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _isRouteAnimating = MutableStateFlow(false)
    val isRouteAnimating: StateFlow<Boolean> = _isRouteAnimating.asStateFlow()

    private val _routeProgress = MutableStateFlow(0f)
    val routeProgress: StateFlow<Float> = _routeProgress.asStateFlow()

    private val _isSuccessFlash = MutableStateFlow(false)
    val isSuccessFlash: StateFlow<Boolean> = _isSuccessFlash.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _downloadSpeedMb = MutableStateFlow("0.0")
    val downloadSpeedMb: StateFlow<String> = _downloadSpeedMb.asStateFlow()

    private val _uploadSpeedMb = MutableStateFlow("0.0")
    val uploadSpeedMb: StateFlow<String> = _uploadSpeedMb.asStateFlow()

    private val _pingMs = MutableStateFlow(0)
    val pingMs: StateFlow<Int> = _pingMs.asStateFlow()

    private val _totalTrafficMb = MutableStateFlow(0.0)
    val totalTrafficMb: StateFlow<Double> = _totalTrafficMb.asStateFlow()

    private val _configs = MutableStateFlow<List<VpnConfig>>(emptyList())
    val configs: StateFlow<List<VpnConfig>> = _configs.asStateFlow()

    private val _selectedConfig = MutableStateFlow<VpnConfig?>(null)
    val selectedConfig: StateFlow<VpnConfig?> = _selectedConfig.asStateFlow()

    private val _isLoadingConfigs = MutableStateFlow(false)
    val isLoadingConfigs: StateFlow<Boolean> = _isLoadingConfigs.asStateFlow()

    private val _serverPings = MutableStateFlow<Map<String, Int?>>(emptyMap())
    val serverPings: StateFlow<Map<String, Int?>> = _serverPings.asStateFlow()

    private val _isPingingAll = MutableStateFlow(false)
    val isPingingAll: StateFlow<Boolean> = _isPingingAll.asStateFlow()

    private var timerJob: Job? = null
    private var animJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun setLoadingConfigs(loading: Boolean) {
        _isLoadingConfigs.value = loading
    }

    fun setConfigs(list: List<VpnConfig>) {
        _configs.value = list
        if (_selectedConfig.value == null && list.isNotEmpty()) {
            _selectedConfig.value = list.first()
        }
    }

    fun selectConfig(config: VpnConfig) {
        _selectedConfig.value = config
    }

    fun pingAllServers() {
        val currentConfigs = _configs.value
        if (currentConfigs.isEmpty()) return

        _isPingingAll.value = true

        val initialMap = currentConfigs.associate { it.config to null as Int? }.toMutableMap()
        _serverPings.value = initialMap.toMap()

        scope.launch {
            val updatedMap = HashMap(initialMap)

            currentConfigs.map { cfg ->
                launch {
                    val host = try { VlessParser.parse(cfg.config).address } catch (_: Exception) { null }
                    val port = try { VlessParser.parse(cfg.config).port } catch (_: Exception) { 80 }

                    val ping = if (host != null) measureRealPing(host, port) else -1
                    synchronized(updatedMap) {
                        updatedMap[cfg.config] = ping
                        _serverPings.value = HashMap(updatedMap)
                    }
                }
            }

            _isPingingAll.value = false
        }
    }

    fun onVpnStarting() {
        if (_vpnState.value == VpnState.DISCONNECTED) {
            _vpnState.value = VpnState.CONNECTING
        }
    }

    fun onVpnStarted() {
        _vpnState.value = VpnState.CONNECTED
        if (!_isRouteAnimating.value) {
            startTimer()
        }
    }

    fun startConnectingAnimation(onStartService: () -> Unit) {
        _vpnState.value = VpnState.CONNECTING
        _isRouteAnimating.value = true
        _routeProgress.value = 0f
        _isSuccessFlash.value = false
        stopTimer()

        onStartService()

        animJob?.cancel()
        animJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val totalAnimDuration = 3000L

            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val p = (elapsed.toFloat() / totalAnimDuration).coerceIn(0f, 1f)
                _routeProgress.value = p

                if (elapsed >= totalAnimDuration) {
                    break
                }
                delay(16L)
            }

            _routeProgress.value = 1f
            _isSuccessFlash.value = true
            _vpnState.value = VpnState.CONNECTED
            startTimer()

            delay(1000L)
            _isSuccessFlash.value = false
            _isRouteAnimating.value = false
        }
    }

    fun onVpnStopped() {
        animJob?.cancel()
        animJob = null
        _isRouteAnimating.value = false
        _isSuccessFlash.value = false
        _vpnState.value = VpnState.DISCONNECTED
        stopTimer()
    }

    private fun startTimer() {
        stopTimer()
        _durationSeconds.value = 0L
        _totalTrafficMb.value = 0.0

        val initialRx = getSystemRxBytes()
        val initialTx = getSystemTxBytes()

        var lastRx = initialRx
        var lastTx = initialTx

        timerJob = scope.launch {
            var pingCounter = 0

            while (isActive) {
                delay(1000L)
                _durationSeconds.value += 1

                val currentRx = getSystemRxBytes()
                val currentTx = getSystemTxBytes()

                val rxDiff = if (currentRx >= lastRx && lastRx > 0) currentRx - lastRx else 0L
                val txDiff = if (currentTx >= lastTx && lastTx > 0) currentTx - lastTx else 0L

                lastRx = currentRx
                lastTx = currentTx

                val dlMb = (rxDiff * 8.0) / (1000.0 * 1000.0)
                val ulMb = (txDiff * 8.0) / (1000.0 * 1000.0)

                _downloadSpeedMb.value = String.format(Locale.US, "%.1f", dlMb)
                _uploadSpeedMb.value = String.format(Locale.US, "%.1f", ulMb)

                val sessionRx = if (currentRx >= initialRx) currentRx - initialRx else 0L
                val sessionTx = if (currentTx >= initialTx) currentTx - initialTx else 0L
                val totalMb = (sessionRx + sessionTx).toDouble() / (1024.0 * 1024.0)

                _totalTrafficMb.value = totalMb

                if (pingCounter % 3 == 0) {
                    val serverHost = getServerHost()
                    val ping = measureRealPing(serverHost ?: "1.1.1.1", getServerPort())
                    if (ping > 0) {
                        _pingMs.value = ping
                    }
                }
                pingCounter++
            }
        }
    }

    private fun getSystemRxBytes(): Long {
        val total = TrafficStats.getTotalRxBytes()
        return if (total != TrafficStats.UNSUPPORTED.toLong()) total else TrafficStats.getUidRxBytes(Process.myUid())
    }

    private fun getSystemTxBytes(): Long {
        val total = TrafficStats.getTotalTxBytes()
        return if (total != TrafficStats.UNSUPPORTED.toLong()) total else TrafficStats.getUidTxBytes(Process.myUid())
    }

    private fun getServerHost(): String? {
        val vlessUrl = _selectedConfig.value?.config ?: return null
        return try {
            VlessParser.parse(vlessUrl).address
        } catch (_: Exception) {
            null
        }
    }

    private fun getServerPort(): Int {
        val vlessUrl = _selectedConfig.value?.config ?: return 80
        return try {
            VlessParser.parse(vlessUrl).port
        } catch (_: Exception) {
            80
        }
    }

    private suspend fun measureRealPing(host: String, port: Int): Int {
        return withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 1500)
                }
                (System.currentTimeMillis() - start).toInt()
            } catch (_: Exception) {
                -1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _durationSeconds.value = 0L
        _downloadSpeedMb.value = "0.0"
        _uploadSpeedMb.value = "0.0"
        _pingMs.value = 0
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    }

    fun getCountryFlag(country: String): String {
        val code = when (country.trim().lowercase()) {
            "netherlands", "нидерланды", "nl" -> "NL"
            "italy", "италия", "it" -> "IT"
            "germany", "германия", "de" -> "DE"
            "usa", "united states", "сша", "us" -> "US"
            "finland", "финляндия", "fi" -> "FI"
            "sweden", "швеция", "se" -> "SE"
            "turkey", "турция", "tr" -> "TR"
            "russia", "россия", "ru" -> "RU"
            "france", "франция", "fr" -> "FR"
            "uk", "united kingdom", "великобритания", "gb" -> "GB"
            else -> if (country.length == 2) country.uppercase() else ""
        }
        if (code.length != 2) return "🌐"
        val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}
