package com.kitconnvpn

import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.kitconnvpn.ui.theme.KitConnVPNTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var pendingVlessUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadConfigs()

        setContent {
            KitConnVPNTheme {
                val isUpdateRequired by VpnStateRepository.isUpdateRequired.collectAsState()
                val isRouteAnimating by VpnStateRepository.isRouteAnimating.collectAsState()
                val routeProgress by VpnStateRepository.routeProgress.collectAsState()
                val isSuccessFlash by VpnStateRepository.isSuccessFlash.collectAsState()
                val selectedConfig by VpnStateRepository.selectedConfig.collectAsState()

                Crossfade(targetState = isUpdateRequired to isRouteAnimating, label = "screen_transition") { (updateReq, animating) ->
                    if (updateReq) {
                        UpdateRequiredScreen(
                            onRefresh = { loadConfigs() }
                        )
                    } else if (animating) {
                        AnimatedMapRouteView(
                            originCity = "Россия (RU)",
                            destCountry = selectedConfig?.country ?: "Нидерланды",
                            destFlag = selectedConfig?.let { VpnStateRepository.getCountryFlag(it.country) } ?: "🇳🇱",
                            progress = routeProgress,
                            isSuccessFlash = isSuccessFlash
                        )
                    } else {
                        VpnMainScreen(
                            onToggleVpn = { handleToggleVpn() },
                            onSelectConfig = { config ->
                                val isConnected = VpnStateRepository.vpnState.value == VpnState.CONNECTED
                                VpnStateRepository.selectConfig(config)
                                if (isConnected) {
                                    stopVpnService()
                                    VpnStateRepository.startConnectingAnimation {
                                        startVpn(config.config)
                                    }
                                }
                            },
                            onRefreshConfigs = { loadConfigs() }
                        )
                    }
                }
            }
        }
    }

    private fun loadConfigs() {
        lifecycleScope.launch {
            VpnStateRepository.setLoadingConfigs(true)

            try {
                val response = kitConnApi.getConfigs(
                    appVersion = BuildConfig.VERSION_CODE
                )
                VpnStateRepository.setRequireUpdate(false)
                VpnStateRepository.setConfigs(response.configs)

            } catch (e: HttpException) {
                if (e.code() == 426) {
                    VpnStateRepository.setRequireUpdate(true)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка загрузки серверов: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Ошибка загрузки серверов: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                VpnStateRepository.setLoadingConfigs(false)
            }
        }
    }

    private fun handleToggleVpn() {
        when (VpnStateRepository.vpnState.value) {
            VpnState.CONNECTED, VpnState.CONNECTING -> {
                stopVpnService()
            }
            VpnState.DISCONNECTED -> {
                val config = VpnStateRepository.selectedConfig.value
                if (config == null) {
                    Toast.makeText(this, "Серверы не загружены", Toast.LENGTH_SHORT).show()
                    loadConfigs()
                    return
                }
                VpnStateRepository.startConnectingAnimation {
                    startVpn(config.config)
                }
            }
        }
    }

    private fun startVpn(vlessUrl: String) {
        val intent = VpnService.prepare(this)

        if (intent != null) {
            pendingVlessUrl = vlessUrl
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            startVpnService(vlessUrl)
        }
    }

    private fun startVpnService(vlessUrl: String) {
        val intent = Intent(this, KitConnVpnService::class.java).apply {
            putExtra(KitConnVpnService.EXTRA_VLESS_URL, vlessUrl)
        }
        startService(intent)
    }

    private fun stopVpnService() {
        VpnStateRepository.onVpnStopped()
        val intent = Intent(this, KitConnVpnService::class.java).apply {
            action = KitConnVpnService.ACTION_STOP
        }
        startService(intent)
        stopService(Intent(this, KitConnVpnService::class.java))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            pendingVlessUrl?.let { url ->
                VpnStateRepository.startConnectingAnimation {
                    startVpnService(url)
                }
            }
            pendingVlessUrl = null
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 100
    }
}

@Composable
fun UpdateRequiredScreen(
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "update_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val cyan = Color(0xFF00E5FF)
    val purple = Color(0xFF7C4DFF)

    Scaffold(
        containerColor = Color(0xFF0C0D14)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "KitConn VPN",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Системное уведомление",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A93A6)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = 0.25f
                            }
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(purple, Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )

                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = Color(0xFF161824),
                        border = BorderStroke(1.5.dp, cyan.copy(alpha = 0.6f)),
                        shadowElevation = 12.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🚀",
                                fontSize = 48.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Требуется обновление",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ваша версия приложения устарела и больше не поддерживается сервером.\nПожалуйста, обновите KitConn VPN до последней версии для продолжения работы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A93A6),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = {
                        try {
//                            val intent = Intent(
//                                Intent.ACTION_VIEW,
//                                Uri.parse("https://kitconn.ru/update")
//                            )
//                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Ссылка для обновления недоступна", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF00E5FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Обновить приложение",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0C0D14)
                        )
                    }
                }

                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF161824),
                    border = BorderStroke(1.dp, Color(0xFF262A3E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Проверить снова",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VpnMainScreen(
    onToggleVpn: () -> Unit,
    onSelectConfig: (VpnConfig) -> Unit,
    onRefreshConfigs: () -> Unit
) {
    val vpnState by VpnStateRepository.vpnState.collectAsState()
    val durationSeconds by VpnStateRepository.durationSeconds.collectAsState()
    val configs by VpnStateRepository.configs.collectAsState()
    val selectedConfig by VpnStateRepository.selectedConfig.collectAsState()
    val isLoadingConfigs by VpnStateRepository.isLoadingConfigs.collectAsState()

    val downloadSpeed by VpnStateRepository.downloadSpeedMb.collectAsState()
    val uploadSpeed by VpnStateRepository.uploadSpeedMb.collectAsState()
    val pingMs by VpnStateRepository.pingMs.collectAsState()
    val totalTrafficMb by VpnStateRepository.totalTrafficMb.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }

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
                        text = "Версия: v3.0.1",
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
                                    color = when (vpnState) {
                                        VpnState.CONNECTED -> Color(0xFF84F938)
                                        VpnState.CONNECTING -> Color(0xFF00E5FF)
                                        VpnState.DISCONNECTED -> Color(0xFF546E7A)
                                    },
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = when (vpnState) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PillMetricCard(
                        title = "Upload",
                        value = if (vpnState == VpnState.CONNECTED) "$uploadSpeed KB/S" else "0 KB/S",
                        arrowIcon = "↑",
                        modifier = Modifier.weight(1f)
                    )
                    PillMetricCard(
                        title = "Download",
                        value = if (vpnState == VpnState.CONNECTED) "$downloadSpeed KB/S" else "0 KB/S",
                        arrowIcon = "↓",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PillMetricCard(
                        title = "Ping",
                        value = if (vpnState == VpnState.CONNECTED && pingMs > 0) "$pingMs ms" else "--",
                        arrowIcon = "⚡",
                        modifier = Modifier.weight(1f)
                    )
                    PillMetricCard(
                        title = "Traffic",
                        value = if (vpnState == VpnState.CONNECTED) String.format(Locale.US, "%.1f MB", totalTrafficMb) else "0 MB",
                        arrowIcon = "⇄",
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF141724),
                        border = BorderStroke(1.dp, Color(0xFF22283C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🌐", fontSize = 12.sp)
                            val hostIp = VpnStateRepository.getServerHost() ?: "185.25.12.5"
                            Text(
                                text = "Your IP : $hostIp",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                PowerButton(
                    vpnState = vpnState,
                    onClick = onToggleVpn
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "Connected"
                        VpnState.CONNECTING -> "Connecting..."
                        VpnState.DISCONNECTED -> "Disconnected"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8A93A6)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (vpnState == VpnState.CONNECTED) {
                        VpnStateRepository.formatDuration(durationSeconds)
                    } else {
                        "00 : 00 : 00"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Surface(
                onClick = {
                    if (configs.isNotEmpty()) {
                        showBottomSheet = true
                    } else {
                        onRefreshConfigs()
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
                        if (isLoadingConfigs) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val flag = selectedConfig?.let {
                                VpnStateRepository.getCountryFlag(it.country)
                            } ?: "🌐"
                            Text(
                                text = flag,
                                fontSize = 26.sp
                            )
                        }

                        Column {
                            Text(
                                text = selectedConfig?.name?.ifEmpty { selectedConfig?.country }
                                    ?: if (isLoadingConfigs) "Загрузка серверов..." else "Выберите локацию",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = selectedConfig?.subtitle ?: "Нажмите для выбора страны",
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

    if (showBottomSheet) {
        CountrySelectorSheet(
            configs = configs,
            selectedConfig = selectedConfig,
            onSelect = { config ->
                onSelectConfig(config)
            },
            onDismiss = { showBottomSheet = false }
        )
    }
}

@Composable
fun PillMetricCard(
    title: String,
    value: String,
    arrowIcon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF141724),
        border = BorderStroke(1.dp, Color(0xFF22283C)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF202638),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = arrowIcon,
                        color = Color(0xFF8A93A6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFF8A93A6)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PowerButton(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "power_btn")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val cyan = Color(0xFF00E5FF)
    val purple = Color(0xFF7C4DFF)
    val pink = Color(0xFFFF007F)
    val green = Color(0xFF00E676)

    Box(
        modifier = modifier
            .size(200.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (vpnState == VpnState.CONNECTED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = 0.3f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(green, Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()

            if (vpnState == VpnState.CONNECTING) {
                rotate(rotationAngle) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                cyan.copy(alpha = 0.1f),
                                cyan,
                                purple,
                                pink,
                                cyan.copy(alpha = 0.1f)
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 290f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            } else if (vpnState == VpnState.CONNECTED) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(cyan, purple, pink, green, cyan)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    color = Color(0xFF23283B),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Surface(
            modifier = Modifier.size(145.dp),
            shape = CircleShape,
            color = Color(0xFF12141F),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                1.5.dp,
                if (vpnState == VpnState.CONNECTED) green else Color(0xFF2B3147)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (vpnState == VpnState.CONNECTED) green.copy(alpha = 0.25f) else purple.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Canvas(modifier = Modifier.size(48.dp)) {
                    val iconColor = when (vpnState) {
                        VpnState.CONNECTED -> green
                        VpnState.CONNECTING -> cyan
                        VpnState.DISCONNECTED -> Color(0xFF8A93A6)
                    }
                    val stroke = 4.5.dp.toPx()

                    drawArc(
                        color = iconColor,
                        startAngle = 120f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    drawLine(
                        color = iconColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height * 0.45f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectorSheet(
    configs: List<VpnConfig>,
    selectedConfig: VpnConfig?,
    onSelect: (VpnConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val serverPings by VpnStateRepository.serverPings.collectAsState()
    val isPingingAll by VpnStateRepository.isPingingAll.collectAsState()

    LaunchedEffect(Unit) {
        if (serverPings.isEmpty()) {
            VpnStateRepository.pingAllServers()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161824),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    onClick = { VpnStateRepository.pingAllServers() },
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
                            Text(
                                text = "⏱️",
                                fontSize = 14.sp
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(configs) { item ->
                    val isSelected = item == selectedConfig
                    val flag = VpnStateRepository.getCountryFlag(item.country)
                    val pingValue = serverPings[item.config]

                    Surface(
                        onClick = {
                            onSelect(item)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF222B3D) else Color(0xFF10141D),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00E676) else Color(0xFF262A3E)
                        ),
                        modifier = Modifier.fillMaxWidth()
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
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = flag,
                                    fontSize = 28.sp
                                )
                                Column {
                                    Text(
                                        text = item.name.ifEmpty { item.country },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = item.subtitle.ifEmpty { "VLESS" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8A93A6)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    text = if (isSelected) "✓" else "›",
                                    color = if (isSelected) Color(0xFF00E676) else Color(0xFF546E7A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isSelected) 18.sp else 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
