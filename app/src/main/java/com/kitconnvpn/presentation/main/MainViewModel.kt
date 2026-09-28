package com.kitconnvpn.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitconnvpn.BuildConfig
import com.kitconnvpn.core.model.VpnConfig
import com.kitconnvpn.VpnState
import com.kitconnvpn.domain.repository.VpnRepository
import com.kitconnvpn.domain.usecase.GetVpnConfigsUseCase
import com.kitconnvpn.domain.usecase.PingServersUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

import kotlinx.coroutines.launch

class MainViewModel(
    private val getVpnConfigsUseCase: GetVpnConfigsUseCase,
    private val pingServersUseCase: PingServersUseCase,
    private val repository: VpnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    init {
        observeRepositoryState()
        loadConfigs()
    }

    private fun observeRepositoryState() {
        combine(
            repository.vpnState,
            repository.durationSeconds,
            repository.configs,
            repository.selectedConfig,
            repository.isLoadingConfigs
        ) { vpnState, duration, configs, selected, isLoading ->
            _uiState.value = _uiState.value.copy(
                vpnState = vpnState,
                durationSeconds = duration,
                configs = configs,
                selectedConfig = selected,
                isLoadingConfigs = isLoading
            )
        }.launchIn(viewModelScope)

        combine(
            repository.isUpdateRequired,
            repository.downloadSpeedMb,
            repository.uploadSpeedMb,
            repository.pingMs,
            repository.totalTrafficMb
        ) { updateReq, dl, ul, ping, traffic ->
            _uiState.value = _uiState.value.copy(
                isUpdateRequired = updateReq,
                downloadSpeedMb = dl,
                uploadSpeedMb = ul,
                pingMs = ping,
                totalTrafficMb = traffic
            )
        }.launchIn(viewModelScope)

        combine(
            repository.serverPings,
            repository.isPingingAll,
            repository.isRouteAnimating,
            repository.routeProgress,
            repository.isSuccessFlash
        ) { pings, isPinging, animating, progress, successFlash ->
            _uiState.value = _uiState.value.copy(
                serverPings = pings,
                isPingingAll = isPinging,
                isRouteAnimating = animating,
                routeProgress = progress,
                isSuccessFlash = successFlash
            )
        }.launchIn(viewModelScope)
    }

    fun onAction(action: MainAction, onStartService: ((String) -> Unit)? = null, onStopService: (() -> Unit)? = null) {
        when (action) {
            is MainAction.ToggleVpn -> handleToggleVpn(onStartService, onStopService)
            is MainAction.SelectConfig -> handleSelectConfig(action.config, onStartService, onStopService)
            is MainAction.RefreshConfigs -> loadConfigs()
            is MainAction.PingAllServers -> pingServersUseCase()
            is MainAction.SetBottomSheetVisible -> {
                _uiState.value = _uiState.value.copy(showBottomSheet = action.visible)
            }
        }
    }

    fun loadConfigs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true
            )
            val result = getVpnConfigsUseCase(BuildConfig.VERSION_CODE)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
                _eventFlow.emit("Ошибка загрузки серверов: ${e.localizedMessage}")
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }
        }
    }

    private fun handleToggleVpn(onStartService: ((String) -> Unit)?, onStopService: (() -> Unit)?) {
        when (_uiState.value.vpnState) {
            VpnState.CONNECTED, VpnState.CONNECTING -> {
                onStopService?.invoke()
            }
            VpnState.DISCONNECTED -> {
                val config = _uiState.value.selectedConfig
                if (config == null) {
                    viewModelScope.launch {
                        _eventFlow.emit("Серверы не загружены")
                    }
                    loadConfigs()
                    return
                }
                repository.startConnectingAnimation {
                    onStartService?.invoke(config.config)
                }
            }
        }
    }

    private fun handleSelectConfig(config: VpnConfig, onStartService: ((String) -> Unit)?, onStopService: (() -> Unit)?) {
        val isConnected = _uiState.value.vpnState == VpnState.CONNECTED
        repository.selectConfig(config)
        if (isConnected) {
            onStopService?.invoke()
            repository.startConnectingAnimation {
                onStartService?.invoke(config.config)
            }
        }
    }

    fun getCountryFlag(country: String): String = repository.getCountryFlag(country)
    fun formatDuration(seconds: Long): String = repository.formatDuration(seconds)
    fun getServerHost(): String? = repository.getServerHost()
}
