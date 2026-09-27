package com.kitconnvpn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

object VpnStateRepository {

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _configs = MutableStateFlow<List<VpnConfig>>(emptyList())
    val configs: StateFlow<List<VpnConfig>> = _configs.asStateFlow()

    private val _selectedConfig = MutableStateFlow<VpnConfig?>(null)
    val selectedConfig: StateFlow<VpnConfig?> = _selectedConfig.asStateFlow()

    private val _isLoadingConfigs = MutableStateFlow(false)
    val isLoadingConfigs: StateFlow<Boolean> = _isLoadingConfigs.asStateFlow()

    private var timerJob: Job? = null
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

    fun onVpnStarting() {
        _vpnState.value = VpnState.CONNECTING
        stopTimer()
    }

    fun onVpnStarted() {
        _vpnState.value = VpnState.CONNECTED
        startTimer()
    }

    fun onVpnStopped() {
        _vpnState.value = VpnState.DISCONNECTED
        stopTimer()
    }

    private fun startTimer() {
        stopTimer()
        _durationSeconds.value = 0L
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _durationSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _durationSeconds.value = 0L
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
