package com.example.gamepad

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

data class PairedHost(val address: String, val name: String)

/** Owns a HID registration only while the activity is in the foreground. */
@SuppressLint("MissingPermission")
class BluetoothGamepad(private val context: Context) {
    private val diagnostics = HidDiagnostics(context)
    private var failedPackets = 0
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val handler = Handler(Looper.getMainLooper())
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private var active = false
    private var openingProfile = false
    private var registrationPending = false
    private var generation = 0
    private var framePending = false
    private var startupTimeout: Runnable? = null
    private var connectionTimeout: Runnable? = null

    var status by mutableStateOf("请开启蓝牙并授予权限")
        private set
    var registered by mutableStateOf(false)
        private set
    var connectedName by mutableStateOf<String?>(null)
        private set
    var connecting by mutableStateOf(false)
        private set
    var pairedHosts by mutableStateOf<List<PairedHost>>(emptyList())
        private set
    var report by mutableStateOf(GamepadReport())
        private set

    val connected: Boolean get() = host != null && connectedName != null
    val hasBluetooth: Boolean get() = adapter != null
    val bluetoothEnabled: Boolean get() = hasConnectPermission() && adapter?.isEnabled == true

    private fun hasConnectPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    fun refreshPairedHosts() {
        pairedHosts = if (bluetoothEnabled) {
            adapter?.bondedDevices.orEmpty().map {
                PairedHost(it.address, it.name ?: "蓝牙设备")
            }.sortedBy { it.name }
        } else emptyList()
    }

    fun start() {
        if (active && registered) return
        diagnostics.record("Start; permission=${hasConnectPermission()} bluetoothEnabled=$bluetoothEnabled")
        active = true
        when {
            !hasBluetooth -> { status = "这台设备没有蓝牙"; return }
            !hasConnectPermission() -> { status = "需要蓝牙权限才能连接电脑"; return }
            !bluetoothEnabled -> { status = "请先开启手机蓝牙"; return }
        }
        refreshPairedHosts()
        if (hid != null) {
            register()
            return
        }
        if (openingProfile) return
        openingProfile = true
        status = "正在初始化蓝牙手柄…"
        val requestGeneration = ++generation
        watchStartup()
        val serviceListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (!active || generation != requestGeneration) {
                    adapter?.closeProfileProxy(profile, proxy)
                    return
                }
                openingProfile = false
                diagnostics.record("HID profile opened profile=$profile")
                hid = proxy as BluetoothHidDevice
                register()
            }

            override fun onServiceDisconnected(profile: Int) {
                if (generation != requestGeneration) return
                diagnostics.record("HID profile disconnected")
                handler.removeCallbacks(sendFrame)
                framePending = false
                hid = null
                host = null
                openingProfile = false
                registered = false
                registrationPending = false
                connectedName = null
                connecting = false
                report = GamepadReport()
                status = "蓝牙服务已断开，请重新启动手柄"
                cancelTimeouts()
            }
        }
        val accepted = adapter?.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE) == true
        diagnostics.record("getProfileProxy accepted=$accepted")
        if (!accepted) {
            openingProfile = false
            cancelTimeouts()
            status = "此手机系统不支持蓝牙 HID 手柄"
        }
    }

    private fun register() {
        if (registered || registrationPending || !active) return
        val profile = hid ?: return
        watchStartup()
        val callbackGeneration = generation
        val callback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, isRegistered: Boolean) {
                if (!active || generation != callbackGeneration) return
                diagnostics.record("HID registration callback registered=$isRegistered plugged=${pluggedDevice?.let(::peer)}")
                registrationPending = false
                startupTimeout?.let(handler::removeCallbacks)
                startupTimeout = null
                registered = isRegistered
                if (isRegistered) {
                    status = "手柄已就绪，等待电脑连接"
                    refreshPairedHosts()
                    if (pluggedDevice != null &&
                        profile.getConnectionState(pluggedDevice) == BluetoothProfile.STATE_CONNECTED) {
                        onConnectionStateChanged(pluggedDevice, BluetoothProfile.STATE_CONNECTED)
                    }
                } else {
                    host = null
                    connectedName = null
                    connecting = false
                    report = GamepadReport()
                    status = "手柄注册已取消，请重新启动手柄"
                }
            }

            override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                if (!active || generation != callbackGeneration) return
                diagnostics.record("Connection ${peer(device)} state=$state bond=${device.bondState}")
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectionTimeout?.let(handler::removeCallbacks)
                        connectionTimeout = null
                        host = device
                        connecting = false
                        connectedName = device.name ?: "Windows 电脑"
                        failedPackets = 0
                        status = "已连接 · $connectedName"
                        releaseAll()
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        connecting = true
                        status = "正在连接 ${device.name ?: "电脑"}…"
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (host != null && host?.address != device.address) return
                        handler.removeCallbacks(sendFrame)
                        framePending = false
                        host = null
                        connectionTimeout?.let(handler::removeCallbacks)
                        connectionTimeout = null
                        connectedName = null
                        connecting = false
                        report = GamepadReport()
                        status = "连接已断开，可以重新连接"
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> status = "正在断开连接…"
                }
            }

            override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
                if (!active || generation != callbackGeneration) return
                diagnostics.record("GetReport ${peer(device)} type=$type id=$id bufferSize=$bufferSize")
                if (type == BluetoothHidDevice.REPORT_TYPE_INPUT && id.toInt() == GamepadReport.ID) {
                    val bytes = report.toBytes()
                    val accepted = profile.replyReport(device, type, id,
                        if (bufferSize > 0) bytes.copyOf(minOf(bufferSize, bytes.size)) else bytes)
                    diagnostics.record("Reply input accepted=$accepted payload=${hex(bytes)}")
                } else {
                    profile.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                }
            }

            override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
                if (!active || generation != callbackGeneration) return
                diagnostics.record("SetProtocol ${peer(device)} protocol=$protocol")
            }

            override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
                if (!active || generation != callbackGeneration) return
                diagnostics.record("SetReport ${peer(device)} type=$type id=$id payload=${hex(data)}; no output/feature report declared")
                profile.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            }

            override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
                if (!active || generation != callbackGeneration) return
                diagnostics.record("Interrupt output ${peer(device)} id=$reportId payload=${hex(data)}; ignored")
            }

            override fun onVirtualCableUnplug(device: BluetoothDevice) {
                if (!active || generation != callbackGeneration) return
                diagnostics.record("Virtual cable unplug ${peer(device)}")
                onConnectionStateChanged(device, BluetoothProfile.STATE_DISCONNECTED)
            }
        }
        val settings = BluetoothHidDeviceAppSdpSettings(
            "Pocket Gamepad", "Touchscreen Bluetooth gamepad", "GamePad",
            BluetoothHidDevice.SUBCLASS2_GAMEPAD, GamepadReport.DESCRIPTOR,
        )
        registrationPending = true
        val accepted = profile.registerApp(settings, null, null, context.mainExecutor, callback)
        diagnostics.record("registerApp accepted=$accepted reportId=${GamepadReport.ID} payloadSize=${GamepadReport.SIZE} descriptor=${hex(GamepadReport.DESCRIPTOR)}")
        if (!accepted) {
            registrationPending = false
            startupTimeout?.let(handler::removeCallbacks)
            startupTimeout = null
            status = "注册手柄失败，请关闭其他蓝牙键鼠应用后重试"
        }
    }

    fun connect(address: String) {
        val profile = hid ?: return
        if (!registered || !bluetoothEnabled || connected || connecting) return
        val device = adapter?.bondedDevices?.firstOrNull { it.address == address } ?: run {
            status = "设备尚未配对，请先在 Windows 添加蓝牙设备"
            return
        }
        connecting = true
        status = "正在连接 ${device.name ?: "电脑"}…"
        val accepted = profile.connect(device)
        diagnostics.record("Connect requested ${peer(device)} accepted=$accepted")
        if (!accepted) {
            connecting = false
            status = "连接请求失败，请在电脑端重新连接"
        } else {
            val requestGeneration = generation
            connectionTimeout = Runnable {
                if (generation == requestGeneration && connecting && !connected) {
                    diagnostics.record("Connection timeout ${peer(device)}")
                    profile.disconnect(device)
                    connecting = false
                    status = "连接超时，请确认电脑蓝牙已开启，或删除配对后重试"
                }
            }.also { handler.postDelayed(it, 15_000L) }
        }
    }

    fun disconnect() {
        diagnostics.record("Disconnect requested")
        releaseAll()
        host?.let { if (hasConnectPermission()) hid?.disconnect(it) }
    }

    fun button(button: PadButton, pressed: Boolean) = update(report.withButton(button, pressed), immediate = true)
    fun direction(direction: PadDirection, pressed: Boolean) = update(report.withDirection(direction, pressed), immediate = true)
    fun stick(left: Boolean, x: Float, y: Float) = update(
        if (left) report.copy(leftX = x, leftY = y) else report.copy(rightX = x, rightY = y),
    )
    fun trigger(left: Boolean, pressed: Boolean) = update(
        if (left) report.copy(leftTrigger = if (pressed) 255 else 0)
        else report.copy(rightTrigger = if (pressed) 255 else 0), immediate = true,
    )

    private fun update(value: GamepadReport, immediate: Boolean = false) {
        if (report == value) return
        report = value
        if (!connected) return
        if (immediate) {
            handler.removeCallbacks(sendFrame)
            framePending = false
            sendCurrentReport()
            return
        }
        if (framePending) return
        framePending = true
        handler.postDelayed(sendFrame, 16L)
    }

    private val sendFrame = Runnable {
        framePending = false
        sendCurrentReport()
    }

    private fun sendCurrentReport() {
        val device = host ?: return
        if (!hasConnectPermission()) return
        val profile = hid ?: return
        val bytes = report.toBytes()
        val accepted = profile.sendReport(device, GamepadReport.ID, bytes)
        if (!accepted) {
            if (failedPackets < Int.MAX_VALUE) failedPackets++
            if (status != "输入发送失败，请检查电脑连接") {
                diagnostics.record("Input send failed; failures=$failedPackets")
            }
            status = "输入发送失败，请检查电脑连接"
        } else if (status == "输入发送失败，请检查电脑连接") {
            diagnostics.record("Input send recovered")
            status = "已连接 · $connectedName"
        }
    }

    fun releaseAll() {
        handler.removeCallbacks(sendFrame)
        framePending = false
        report = GamepadReport()
        sendCurrentReport()
    }

    fun stop() {
        if (active || hid != null) diagnostics.record("Stop; failures=$failedPackets connected=$connected")
        releaseAll()
        cancelTimeouts()
        active = false
        ++generation
        if (hasConnectPermission()) {
            host?.let { hid?.disconnect(it) }
            hid?.unregisterApp()
        }
        hid?.let { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        hid = null
        host = null
        registered = false
        registrationPending = false
        openingProfile = false
        connectedName = null
        connecting = false
        status = "手柄已暂停，回到应用后可重新连接"
        diagnostics.persist()
    }

    private fun watchStartup() {
        startupTimeout?.let(handler::removeCallbacks)
        startupTimeout = Runnable {
            if (active && !registered) {
                diagnostics.record("HID startup timeout")
                stop()
                status = "初始化超时，手机系统可能未开放蓝牙 HID；可重试或换一台手机"
            }
        }.also { handler.postDelayed(it, 10_000L) }
    }

    private fun cancelTimeouts() {
        startupTimeout?.let(handler::removeCallbacks)
        connectionTimeout?.let(handler::removeCallbacks)
        startupTimeout = null
        connectionTimeout = null
    }

    fun recordBondChange(device: BluetoothDevice, previous: Int, current: Int) {
        diagnostics.record("Bond changed ${peer(device)} $previous -> $current")
    }

    private fun peer(device: BluetoothDevice): String = "peer-…${device.address.takeLast(5)}"
    private fun hex(bytes: ByteArray): String = bytes.take(160).joinToString("-") { "%02X".format(it.toInt() and 0xFF) } +
        if (bytes.size > 160) "…(${bytes.size} bytes)" else ""
}
