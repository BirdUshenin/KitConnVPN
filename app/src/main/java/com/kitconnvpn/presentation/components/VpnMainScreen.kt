package com.kitconnvpn.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitconnvpn.BuildConfig
import com.kitconnvpn.R
import com.kitconnvpn.VpnState
import com.kitconnvpn.core.model.VpnConfig
import com.kitconnvpn.presentation.main.MainAction
import com.kitconnvpn.presentation.main.MainUiState
import com.kitconnvpn.presentation.main.MainViewModel
import kotlin.text.ifEmpty

@Composable
internal fun VpnMainScreen(
    uiState: MainUiState,
    onAction: (MainAction) -> Unit,
    viewModel: MainViewModel
) {
    val backgroundColor = Color(0xFF0C0D14)

    Scaffold(
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KitConn VPN",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Версия: v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A93A6)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF161824),
                    border = BorderStroke(1.dp, Color(0xFF262A3E))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when (uiState.vpnState) {
                                        VpnState.CONNECTED -> Color(0xFF84F938)
                                        VpnState.CONNECTING -> Color(0xFF00E5FF)
                                        VpnState.DISCONNECTED -> Color(0xFF546E7A)
                                    },
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = when (uiState.vpnState) {
                                VpnState.CONNECTED -> "Подключено"
                                VpnState.CONNECTING -> "Подключение"
                                VpnState.DISCONNECTED -> "Не подключено"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Connecting Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8A93A6)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (uiState.vpnState == VpnState.CONNECTED) {
                        viewModel.formatDuration(uiState.durationSeconds)
                    } else {
                        "00:00:00"
                    },
                    fontSize = 56.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(32.dp))

                val isOn = uiState.vpnState != VpnState.DISCONNECTED
                Row(
                    modifier = Modifier.width(260.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Off",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isOn) Color(0xFF546E7A) else Color.White
                    )
                    Text(
                        text = "On",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isOn) Color.White else Color(0xFF546E7A)
                    )
                }

                VpnKnob(
                    vpnState = uiState.vpnState,
                    onToggle = { onAction(MainAction.ToggleVpn) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeedColumn(
                    title = "Download",
                    arrowIcon = "↓",
                    value = if (uiState.vpnState == VpnState.CONNECTED) "${uiState.downloadSpeedMb} KB/s" else "0 KB/s",
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(Color(0xFF262A3E))
                )
                SpeedColumn(
                    title = "Upload",
                    arrowIcon = "↑",
                    value = if (uiState.vpnState == VpnState.CONNECTED) "${uiState.uploadSpeedMb} KB/s" else "0 KB/s",
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                onClick = {
                    if (uiState.configs.isNotEmpty()) {
                        onAction(MainAction.SetBottomSheetVisible(true))
                    } else {
                        onAction(MainAction.RefreshConfigs)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141724),
                border = BorderStroke(1.dp, Color(0xFF22283C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (uiState.isLoadingConfigs) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val flag = uiState.selectedConfig?.let {
                                viewModel.getCountryFlag(it.country)
                            } ?: "🌐"
                            CountryFlag(flagEmoji = flag)
                        }

                        Column {
                            Text(
                                text = uiState.selectedConfig?.name?.ifEmpty { uiState.selectedConfig?.country }
                                    ?: if (uiState.isLoadingConfigs) "Загрузка серверов..." else "Выберите локацию",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = uiState.selectedConfig?.subtitle ?: "Нажмите для выбора страны",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8A93A6)
                            )
                        }
                    }

                    Text(
                        text = "▼",
                        color = Color(0xFF8A93A6),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (uiState.showBottomSheet) {
        CountrySelectorSheet(
            configs = uiState.configs,
            selectedConfig = uiState.selectedConfig,
            serverPings = uiState.serverPings,
            isPingingAll = uiState.isPingingAll,
            onPingAll = { onAction(MainAction.PingAllServers) },
            onSelect = { config ->
                onAction(MainAction.SelectConfig(config))
            },
            onDismiss = { onAction(MainAction.SetBottomSheetVisible(false)) },
            viewModel = viewModel
        )
    }
}

@Composable
private fun SpeedColumn(
    title: String,
    arrowIcon: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = arrowIcon, color = Color(0xFF8A93A6), fontSize = 14.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A93A6)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectorSheet(
    configs: List<VpnConfig>,
    selectedConfig: VpnConfig?,
    serverPings: Map<String, Int?>,
    isPingingAll: Boolean,
    onPingAll: () -> Unit,
    onSelect: (VpnConfig) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    LaunchedEffect(Unit) {
        if (serverPings.isEmpty()) {
            onPingAll()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
        containerColor = Color(0xFF0C0D14),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Список серверов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    onClick = onPingAll,
                    shape = CircleShape,
                    color = Color(0xFF222638),
                    border = BorderStroke(1.dp, Color(0xFF323850))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isPingingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.timer),
                                contentDescription = "test ping",
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (isPingingAll) "Замер..." else "Тест пинга",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(configs) { item ->
                    val isSelected = item == selectedConfig
                    val flag = viewModel.getCountryFlag(item.country)
                    val pingValue = serverPings[item.config]

                    val cardShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF181B28), Color(0xFF10121B))
                                )
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF84F938) else Color(0xFF232739),
                                shape = cardShape
                            )
                            .clickable {
                                onSelect(item)
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CountryFlag(flagEmoji = flag)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (serverPings.containsKey(item.config) && pingValue == null && isPingingAll) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color(0xFF00E5FF),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        val pingText = when {
                                            pingValue == null -> ""
                                            pingValue > 0 -> "$pingValue мс"
                                            else -> "n/a"
                                        }
                                        val pingColor = when {
                                            pingValue == null -> Color(0xFF8A93A6)
                                            pingValue in 1..250 -> Color(0xFF00E676)
                                            pingValue in 251..500 -> Color(0xFFFFB300)
                                            pingValue > 500 -> Color(0xFFFF5252)
                                            else -> Color(0xFF8A93A6)
                                        }
                                        Text(
                                            text = pingText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = pingColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = if (isSelected) "✓" else "•••",
                                        color = if (isSelected) Color(0xFF84F938) else Color(0xFF8A93A6),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isSelected) 20.sp else 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = item.subtitle.ifEmpty { "Location" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB4BBCB)
                            )
                            Text(
                                text = item.name.ifEmpty { item.country },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}