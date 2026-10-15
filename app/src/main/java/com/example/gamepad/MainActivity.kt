package com.example.gamepad

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import com.example.gamepad.ui.theme.GamePadTheme

class MainActivity : ComponentActivity() {
    private lateinit var gamepad: BluetoothGamepad
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants -> if (grants.values.all { it }) enableBluetooth() }
    private val enableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { gamepad.start() }
    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { gamepad.start(); gamepad.refreshPairedHosts() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        gamepad.start()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> gamepad.stop()
                }
            } else if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val device = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device != null) gamepad.recordBondChange(device,
                    intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1),
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1))
                gamepad.refreshPairedHosts()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        gamepad = BluetoothGamepad(this)
        ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }, ContextCompat.RECEIVER_EXPORTED)
        setContent {
            GamePadTheme(darkTheme = false, dynamicColor = false) {
                GamepadScreen(gamepad, onStart = ::requestBluetooth, onPair = ::makeDiscoverable)
            }
        }
    }

    private fun requestBluetooth() {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else emptyArray()
        if (required.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(required)
        } else enableBluetooth()
    }

    private fun enableBluetooth() {
        if (!gamepad.hasBluetooth) { gamepad.start(); return }
        if (gamepad.bluetoothEnabled) gamepad.start()
        else enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun makeDiscoverable() {
        if (!gamepad.registered) { requestBluetooth(); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_ADVERTISE,
            ) != PackageManager.PERMISSION_GRANTED) {
            requestBluetooth()
            return
        }
        discoverableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        })
    }

    override fun onResume() {
        super.onResume()
        if (::gamepad.isInitialized) gamepad.start()
    }

    override fun onPause() {
        if (::gamepad.isInitialized) gamepad.releaseAll()
        super.onPause()
    }

    override fun onStop() {
        gamepad.stop()
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        gamepad.stop()
        super.onDestroy()
    }
}
