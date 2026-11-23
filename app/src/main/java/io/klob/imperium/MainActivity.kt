package io.klob.imperium

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.klob.imperium.hid.BluetoothHidController
import io.klob.imperium.input.GestureConfig
import io.klob.imperium.input.GestureScheduler
import io.klob.imperium.input.MouseOutput
import io.klob.imperium.input.TrackpadGestureEngine
import io.klob.imperium.settings.SettingsStore
import io.klob.imperium.ui.ImperiumScreen
import io.klob.imperium.ui.theme.ImperiumTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsStore: SettingsStore
    private lateinit var hidController: BluetoothHidController
    private lateinit var gestureEngine: TrackpadGestureEngine

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isDragLocked by mutableStateOf(false)
    private var isHolding by mutableStateOf(false)
    private var isIgnoringBatteryOptimizations by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hidController.start()
    }

    private val bluetoothIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hidController.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        settingsStore = SettingsStore(applicationContext)
        hidController = BluetoothHidController(
            context = applicationContext,
            onHostConnected = { address ->
                settingsStore.update { it.copy(lastHostAddress = address) }
            },
            onDisconnected = {
                mainHandler.post {
                    gestureEngine.clearDragLock()
                    isDragLocked = false
                    isHolding = false
                }
            },
        )

        val touchSlopPx = 8f * resources.displayMetrics.density
        val scheduler = object : GestureScheduler {
            override fun schedule(delayMs: Long, action: () -> Unit): GestureScheduler.Cancellable {
                val runnable = Runnable { action() }
                mainHandler.postDelayed(runnable, delayMs)
                return GestureScheduler.Cancellable { mainHandler.removeCallbacks(runnable) }
            }
        }

        val mouseOutput = object : MouseOutput {
            override fun onMove(dx: Int, dy: Int) {
                hidController.sendMove(dx, dy)
            }

            override fun onLeftClick() {
                haptic(HapticFeedbackConstants.CONTEXT_CLICK)
                hidController.sendLeftClick()
            }

            override fun onLeftDown() {
                hidController.setLeftButton(true)
            }

            override fun onLeftUp() {
                hidController.setLeftButton(false)
            }

            override fun onRightClick() {
                haptic(HapticFeedbackConstants.LONG_PRESS)
                hidController.sendRightClick()
            }

            override fun onDoubleClick() {
                haptic(HapticFeedbackConstants.CONTEXT_CLICK)
                hidController.sendDoubleClick()
            }

            override fun onDragLockChanged(locked: Boolean) {
                isDragLocked = locked
                haptic(if (locked) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CONTEXT_CLICK)
            }

            override fun onHoldingChanged(holding: Boolean) {
                isHolding = holding
                if (holding) haptic(HapticFeedbackConstants.LONG_PRESS)
            }
        }

        val initial = settingsStore.settings.value
        gestureEngine = TrackpadGestureEngine(
            output = mouseOutput,
            scheduler = scheduler,
            initialConfig = GestureConfig(
                touchSlopPx = touchSlopPx,
                pointerSpeed = initial.pointerSpeed,
                pointerAcceleration = initial.pointerAcceleration,
            ),
        )

        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle()
            val hidState by hidController.state.collectAsStateWithLifecycle()

            LaunchedEffect(settings.pointerSpeed, settings.pointerAcceleration) {
                gestureEngine.config = gestureEngine.config.copy(
                    pointerSpeed = settings.pointerSpeed,
                    pointerAcceleration = settings.pointerAcceleration,
                )
            }

            ImperiumTheme(themeMode = settings.themeMode) {
                ImperiumScreen(
                    hidState = hidState,
                    settings = settings,
                    isDragLocked = isDragLocked,
                    isHolding = isHolding,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                    gestureEngine = gestureEngine,
                    onScrollWheel = { ticks -> hidController.sendScroll(ticks) },
                    onRequestPermissions = ::requestBluetoothPermissions,
                    onEnableBluetooth = ::requestEnableBluetooth,
                    onMakeDiscoverable = ::requestDiscoverable,
                    onRequestUnrestrictedBattery = ::requestUnrestrictedBattery,
                    onConnectHost = { address -> hidController.connect(address) },
                    onDisconnect = {
                        gestureEngine.clearDragLock()
                        hidController.disconnect()
                    },
                    onRefreshHosts = { hidController.refreshBondedHosts() },
                    onUpdateSettings = { transform -> settingsStore.update(transform) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshBatteryOptimizationState()
        hidController.start()
    }

    override fun onResume() {
        super.onResume()
        refreshBatteryOptimizationState()
    }

    override fun onPause() {
        super.onPause()
        gestureEngine.clearDragLock()
        hidController.releaseAllButtons()
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureEngine.clearDragLock()
        hidController.shutdown()
    }

    private fun haptic(constant: Int) {
        if (settingsStore.settings.value.hapticsEnabled) {
            window.decorView.performHapticFeedback(constant)
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                )
            )
        } else {
            hidController.start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestEnableBluetooth() {
        bluetoothIntentLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    @SuppressLint("MissingPermission")
    private fun requestDiscoverable() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        bluetoothIntentLauncher.launch(intent)
    }

    private fun refreshBatteryOptimizationState() {
        val pm = getSystemService(PowerManager::class.java)
        isIgnoringBatteryOptimizations = pm?.isIgnoringBatteryOptimizations(packageName) == true
    }

    @SuppressLint("BatteryLife")
    private fun requestUnrestrictedBattery() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { bluetoothIntentLauncher.launch(intent) }
            .onFailure {
                runCatching {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
    }
}
