package com.helly.psaimmotool.modules

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.helly.psaimmotool.R
import com.helly.psaimmotool.utils.UiUpdater
import java.util.*
import kotlin.concurrent.thread

object Obd2BluetoothModule {
    private var socket: BluetoothSocket? = null
    private var isConnected = false

    fun connect(context: Context) {
        val adapter = BluetoothAdapter.getDefaultAdapter()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1001
            )
            UiUpdater.appendLog(context.getString(R.string.error_bt_permission))
            return
        }

        try {
            val device: BluetoothDevice? = adapter?.bondedDevices?.firstOrNull {
                it.name.contains("OBD", ignoreCase = true) || it.name.contains("ELM", ignoreCase = true)
            }

            if (device == null) {
                UiUpdater.appendLog(context.getString(R.string.module_not_found))
                return
            }

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            thread {
                try {
                    socket = device.createRfcommSocketToServiceRecord(uuid)
                    socket?.connect()
                    isConnected = true
                    UiUpdater.setConnectedStatus(
                        context.getString(R.string.connected_to, device.name),
                        "OBD2 Bluetooth"
                    )
                } catch (e: Exception) {
                    UiUpdater.appendLog(context.getString(R.string.error_bt_connection) + ": ${e.message}")
                }
            }
        } catch (e: SecurityException) {
            UiUpdater.appendLog(context.getString(R.string.error_bt_permission))
        }
    }

    fun disconnect(context: Context) {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        isConnected = false
        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected))
    }

    fun sendVinRequest(context: Context) {
        sendCommand(context, "09 02")
    }

    fun sendCommand(context: Context, command: String) {
        if (!isConnected || socket == null) {
            UiUpdater.appendLog(context.getString(R.string.no_module_connected))
            return
        }

        try {
            val fullCommand = "$command\r"
            socket!!.outputStream.write(fullCommand.toByteArray())
            UiUpdater.appendLog("> $command")
            // Optionally, read response here
        } catch (e: Exception) {
            UiUpdater.appendLog("Erreur: ${e.message}")
        }
    }
}
