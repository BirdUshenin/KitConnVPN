package com.kitconnvpn.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kitconnvpn.KitConnVpnService
import com.kitconnvpn.VpnStateRepository
import com.kitconnvpn.presentation.components.SplashScreen
import com.kitconnvpn.presentation.components.UpdateRequiredScreen
import com.kitconnvpn.presentation.components.VpnMainScreen
import com.kitconnvpn.presentation.main.MainAction
import com.kitconnvpn.presentation.main.MainViewModel
import com.kitconnvpn.ui.theme.KitConnVPNTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private var pendingVlessUrl: String? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            pendingVlessUrl?.let {
                startVpnService(it)
            }
        }

        pendingVlessUrl = null
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()

        setContent {
            KitConnVPNTheme {
                val viewModel: MainViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                when (uiState.isLoading) {
                    true -> {
                        SplashScreen(modifier = Modifier)
                    }
                    false -> {
                        Crossfade(
                            targetState = uiState.isUpdateRequired,
                            label = "screen_transition"
                        ) { updateReq ->
                            if (updateReq) {
                                UpdateRequiredScreen(
                                    onRefresh = {
                                        viewModel.onAction(MainAction.RefreshConfigs)
                                    }
                                )
                            } else {
                                VpnMainScreen(
                                    uiState = uiState,
                                    onAction = { action ->
                                        viewModel.onAction(
                                            action = action,
                                            onStartService = { url -> startVpn(url) },
                                            onStopService = { stopVpnService() }
                                        )
                                    }
//                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startVpn(vlessUrl: String) {
        val intent = VpnService.prepare(this)

        if (intent != null) {
            pendingVlessUrl = vlessUrl
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService(vlessUrl)
        }
    }

    private fun startVpnService(vlessUrl: String) {
        val intent = Intent(this, KitConnVpnService::class.java).apply {
            putExtra(KitConnVpnService.EXTRA_VLESS_URL, vlessUrl)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVpnService() {
        VpnStateRepository.onVpnStopped()
        val intent = Intent(this, KitConnVpnService::class.java).apply {
            action = KitConnVpnService.ACTION_STOP
        }
        startService(intent)
        stopService(Intent(this, KitConnVpnService::class.java))
    }
}