package com.kitconnvpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.kitconnvpn.ui.theme.KitConnVPNTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingVlessUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadConfigs()

        setContent {
            KitConnVPNTheme {
                VpnMainScreen(
                    onToggleVpn = { handleToggleVpn() },
                    onSelectConfig = { config ->
                        val isConnected = VpnStateRepository.vpnState.value == VpnState.CONNECTED
                        VpnStateRepository.selectConfig(config)
                        if (isConnected) {
                            stopVpnService()
                            startVpn(config.config)
                        }
                    },
                    onRefreshConfigs = { loadConfigs() }
                )
            }
        }
    }

    private fun loadConfigs() {
        lifecycleScope.launch {
            VpnStateRepository.setLoadingConfigs(true)
            try {
                val response = kitConnApi.getConfigs()
                VpnStateRepository.setConfigs(response.configs)
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
                startVpn(config.config)
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
            pendingVlessUrl?.let(::startVpnService)
            pendingVlessUrl = null
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 100
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

    var showBottomSheet by remember { mutableStateOf(false) }

    val backgroundColor = Color(0xFF0D0F14)

    Scaffold(
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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
                        color = Color(0xFF8C9BAE)
                    )
                }

                // Security Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF181C28),
                    border = BorderStroke(1.dp, Color(0xFF2A3042))
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
                                        VpnState.CONNECTED -> Color(0xFF00E676)
                                        VpnState.CONNECTING -> Color(0xFFFF9100)
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                PowerButton(
                    vpnState = vpnState,
                    onClick = onToggleVpn
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Status Label
                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "ПОДКЛЮЧЕНО"
                        VpnState.CONNECTING -> "ПОДКЛЮЧЕНИЕ..."
                        VpnState.DISCONNECTED -> "НАЖМИТЕ ДЛЯ ВКЛЮЧЕНИЯ"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = when (vpnState) {
                        VpnState.CONNECTED -> Color(0xFF00E676)
                        VpnState.CONNECTING -> Color(0xFFFF9100)
                        VpnState.DISCONNECTED -> Color(0xFF8C9BAE)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Timer Display
                Text(
                    text = if (vpnState == VpnState.CONNECTED) {
                        VpnStateRepository.formatDuration(durationSeconds)
                    } else {
                        "00:00:00"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = if (vpnState == VpnState.CONNECTED) Color.White else Color(0xFF4A5568)
                )
            }

            // Bottom Section: Server Selection Card & Protocol Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Server Selector Card
                Surface(
                    onClick = {
                        if (configs.isNotEmpty()) {
                            showBottomSheet = true
                        } else {
                            onRefreshConfigs()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF161B26),
                    border = BorderStroke(1.dp, Color(0xFF283044)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isLoadingConfigs) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color(0xFF00E676),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val flag = selectedConfig?.let {
                                    VpnStateRepository.getCountryFlag(it.country)
                                } ?: "🌐"
                                Text(
                                    text = flag,
                                    fontSize = 28.sp
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
                                    color = Color(0xFF8C9BAE)
                                )
                            }
                        }

                        // Arrow down icon
                        Text(
                            text = "▼",
                            color = Color(0xFF8C9BAE),
                            fontSize = 12.sp
                        )
                    }
                }

                // Protocol & encryption footer info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Brid",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8C9BAE)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "WYY, RY, TC",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8C9BAE)
                        )
                    }
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
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val targetColor = when (vpnState) {
        VpnState.CONNECTED -> Color(0xFF00E676)
        VpnState.CONNECTING -> Color(0xFFFF9100)
        VpnState.DISCONNECTED -> Color(0xFF455A64)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(500),
        label = "color"
    )

    Box(
        modifier = modifier
            .size(210.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing aura when connected
        if (vpnState == VpnState.CONNECTED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = 0.25f
                    }
                    .background(
                        color = Color(0xFF00E676),
                        shape = CircleShape
                    )
            )
        }

        // Rotating arc when connecting or static border
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            if (vpnState == VpnState.CONNECTING) {
                rotate(rotationAngle) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF9100).copy(alpha = 0.1f),
                                Color(0xFFFF9100),
                                Color(0xFFFF9100).copy(alpha = 0.1f)
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            } else {
                drawCircle(
                    color = animatedColor.copy(alpha = if (vpnState == VpnState.CONNECTED) 0.8f else 0.3f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // Inner circular button container
        Surface(
            modifier = Modifier.size(155.dp),
            shape = CircleShape,
            color = Color(0xFF161B26),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.5.dp, animatedColor.copy(alpha = 0.5f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Inner radial glow
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedColor.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Canvas Power Icon
                Canvas(modifier = Modifier.size(52.dp)) {
                    val iconColor = if (vpnState == VpnState.DISCONNECTED) Color(0xFFB0BEC5) else animatedColor
                    val stroke = 5.dp.toPx()

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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B26),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Выберите локацию",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(configs) { item ->
                    val isSelected = item == selectedConfig
                    val flag = VpnStateRepository.getCountryFlag(item.country)

                    Surface(
                        onClick = {
                            onSelect(item)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF222B3D) else Color(0xFF10141D),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00E676) else Color(0xFF2A3042)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                                    if (item.subtitle.isNotEmpty()) {
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF8C9BAE)
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
