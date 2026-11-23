package io.klob.imperium.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors

enum class HidConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
}

data class BondedHost(
    val name: String,
    val address: String,
    val isComputer: Boolean,
)

data class HidUiState(
    val bluetoothSupported: Boolean = true,
    val bluetoothEnabled: Boolean = false,
    val hasPermissions: Boolean = false,
    val profileReady: Boolean = false,
    val appRegistered: Boolean = false,
    val connectionState: HidConnectionState = HidConnectionState.DISCONNECTED,
    val connectedHostName: String? = null,
    val connectedHostAddress: String? = null,
    val bondedHosts: List<BondedHost> = emptyList(),
    val statusNote: String? = null,
)

class BluetoothHidController(
    private val context: Context,
    private val onHostConnected: (address: String) -> Unit = {},
    private val onDisconnected: () -> Unit = {},
) {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val executor = Executors.newSingleThreadExecutor()

    private var hidDevice: BluetoothHidDevice? = null
    private var activeHost: BluetoothDevice? = null

    @Volatile
    private var buttonMask: Int = 0

    private val _state = MutableStateFlow(
        HidUiState(
            bluetoothSupported = bluetoothAdapter != null,
            bluetoothEnabled = bluetoothAdapter?.isEnabled == true,
            hasPermissions = hasRequiredPermissions(),
        )
    )
    val state: StateFlow<HidUiState> = _state.asStateFlow()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as? BluetoothHidDevice
            _state.update { it.copy(profileReady = true, statusNote = null) }
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            activeHost = null
            buttonMask = 0
            _state.update {
                it.copy(
                    profileReady = false,
                    appRegistered = false,
                    connectionState = HidConnectionState.DISCONNECTED,
                    connectedHostName = null,
                    connectedHostAddress = null,
                )
            }
            onDisconnected()
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _state.update {
                it.copy(
                    appRegistered = registered,
                    statusNote = if (!registered) "HID registration inactive" else null,
                )
            }
            if (registered && pluggedDevice != null) {
                val deviceState = runCatching {
                    hidDevice?.getConnectionState(pluggedDevice)
                }.getOrNull()
                if (deviceState == BluetoothProfile.STATE_CONNECTED) {
                    activeHost = pluggedDevice
                    _state.update {
                        it.copy(
                            connectionState = HidConnectionState.CONNECTED,
                            connectedHostName = pluggedDevice.safeName(),
                            connectedHostAddress = pluggedDevice.address,
                        )
                    }
                    onHostConnected(pluggedDevice.address)
                }
            }
            refreshBondedHosts()
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    activeHost = device
                    buttonMask = 0
                    _state.update {
                        it.copy(
                            connectionState = HidConnectionState.CONNECTED,
                            connectedHostName = device.safeName(),
                            connectedHostAddress = device.address,
                            statusNote = null,
                        )
                    }
                    onHostConnected(device.address)
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    _state.update {
                        it.copy(
                            connectionState = HidConnectionState.CONNECTING,
                            connectedHostName = device.safeName(),
                            connectedHostAddress = device.address,
                            statusNote = null,
                        )
                    }
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    _state.update { it.copy(connectionState = HidConnectionState.DISCONNECTING) }
                }

                else -> {
                    if (activeHost?.address == device.address || activeHost == null) {
                        activeHost = null
                        buttonMask = 0
                        _state.update {
                            it.copy(
                                connectionState = HidConnectionState.DISCONNECTED,
                                connectedHostName = null,
                                connectedHostAddress = null,
                            )
                        }
                        onDisconnected()
                    }
                }
            }
            refreshBondedHosts()
        }

        @SuppressLint("MissingPermission")
        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val hid = hidDevice ?: return
            if (!hasRequiredPermissions()) return
            val report = HidMouseDescriptor.buildReport(buttonMask, 0, 0, 0)
            runCatching {
                hid.replyReport(device, type, HidMouseDescriptor.REPORT_ID.toByte(), report)
            }
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun start() {
        val adapter = bluetoothAdapter
        val hasPerms = hasRequiredPermissions()
        val enabled = adapter?.isEnabled == true
        _state.update {
            it.copy(
                bluetoothSupported = adapter != null,
                bluetoothEnabled = enabled,
                hasPermissions = hasPerms,
            )
        }
        if (adapter == null || !enabled || !hasPerms) return
        refreshBondedHosts()
        if (hidDevice == null) {
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        } else if (!_state.value.appRegistered) {
            registerApp()
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val hid = hidDevice ?: return
        if (!hasRequiredPermissions()) return
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "imperium",
            "imperium bluetooth mouse",
            "klob",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            HidMouseDescriptor.DESCRIPTOR,
        )
        val outQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX,
        )
        val ok = runCatching {
            hid.registerApp(sdp, null, outQos, executor, hidCallback)
        }.getOrDefault(false)
        if (!ok) {
            _state.update { it.copy(statusNote = "Unable to register HID mouse service") }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedHosts() {
        val adapter = bluetoothAdapter
        val hasPerms = hasRequiredPermissions()
        val enabled = adapter?.isEnabled == true
        if (adapter == null || !enabled || !hasPerms) {
            _state.update {
                it.copy(
                    bluetoothEnabled = enabled,
                    hasPermissions = hasPerms,
                    bondedHosts = emptyList(),
                )
            }
            return
        }
        val hosts = runCatching {
            adapter.bondedDevices.orEmpty().map { dev ->
                val major = dev.bluetoothClass?.majorDeviceClass
                BondedHost(
                    name = dev.safeName(),
                    address = dev.address,
                    isComputer = major == BluetoothClass.Device.Major.COMPUTER,
                )
            }.sortedWith(
                compareByDescending<BondedHost> { it.isComputer }
                    .thenBy { it.name.lowercase() }
            )
        }.getOrDefault(emptyList())

        _state.update {
            it.copy(
                bluetoothEnabled = true,
                hasPermissions = true,
                bondedHosts = hosts,
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val adapter = bluetoothAdapter ?: return
        val hid = hidDevice ?: run {
            start()
            return
        }
        if (!hasRequiredPermissions()) return
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        _state.update {
            it.copy(
                connectionState = HidConnectionState.CONNECTING,
                connectedHostName = device.safeName(),
                connectedHostAddress = device.address,
                statusNote = null,
            )
        }
        val started = runCatching { hid.connect(device) }.getOrDefault(false)
        if (!started) {
            _state.update {
                it.copy(
                    connectionState = HidConnectionState.DISCONNECTED,
                    statusNote = "Connect request failed. Ensure PC Bluetooth is on.",
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        releaseAllButtons()
        val hid = hidDevice ?: return
        val host = activeHost ?: return
        if (!hasRequiredPermissions()) return
        runCatching { hid.disconnect(host) }
    }

    fun sendMove(dx: Int, dy: Int) {
        if (dx == 0 && dy == 0) return
        val packets = HidMouseDescriptor.splitDeltas(dx, dy, 0)
        for ((sx, sy, _) in packets) {
            sendRawReport(buttonMask, sx, sy, 0)
        }
    }

    fun sendScroll(wheelDelta: Int) {
        if (wheelDelta == 0) return
        val packets = HidMouseDescriptor.splitDeltas(0, 0, wheelDelta)
        for ((_, _, sw) in packets) {
            sendRawReport(buttonMask, 0, 0, sw)
        }
    }

    fun setLeftButton(down: Boolean) {
        val next = if (down) {
            buttonMask or HidMouseDescriptor.BUTTON_LEFT
        } else {
            buttonMask and HidMouseDescriptor.BUTTON_LEFT.inv()
        }
        if (next != buttonMask) {
            buttonMask = next
            sendRawReport(buttonMask, 0, 0, 0)
        }
    }

    fun sendLeftClick() {
        val pressed = buttonMask or HidMouseDescriptor.BUTTON_LEFT
        sendRawReport(pressed, 0, 0, 0)
        val released = buttonMask and HidMouseDescriptor.BUTTON_LEFT.inv()
        buttonMask = released
        sendRawReport(released, 0, 0, 0)
    }

    fun sendRightClick() {
        val pressed = buttonMask or HidMouseDescriptor.BUTTON_RIGHT
        sendRawReport(pressed, 0, 0, 0)
        val released = buttonMask and HidMouseDescriptor.BUTTON_RIGHT.inv()
        buttonMask = released
        sendRawReport(released, 0, 0, 0)
    }

    fun sendDoubleClick() {
        sendLeftClick()
        sendLeftClick()
    }

    fun releaseAllButtons() {
        if (buttonMask != 0) {
            buttonMask = 0
            sendRawReport(0, 0, 0, 0)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendRawReport(buttons: Int, dx: Int, dy: Int, wheel: Int) {
        val hid = hidDevice ?: return
        val host = activeHost ?: return
        if (!hasRequiredPermissions()) return
        val report = HidMouseDescriptor.buildReport(buttons, dx, dy, wheel)
        runCatching {
            hid.sendReport(host, HidMouseDescriptor.REPORT_ID, report)
        }
    }

    @SuppressLint("MissingPermission")
    fun shutdown() {
        releaseAllButtons()
        val hid = hidDevice
        if (hid != null && hasRequiredPermissions()) {
            runCatching { hid.unregisterApp() }
            runCatching { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid) }
        }
        hidDevice = null
        activeHost = null
        executor.shutdown()
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String =
        runCatching { name }.getOrNull()?.takeIf { it.isNotBlank() } ?: address
}
