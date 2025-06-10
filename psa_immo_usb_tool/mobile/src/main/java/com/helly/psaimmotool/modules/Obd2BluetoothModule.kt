package com.helly.psaimmotool.modules

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.annotation.RequiresPermission
import com.helly.psaimmotool.R
import com.helly.psaimmotool.utils.UiUpdater
import java.util.*
import kotlin.concurrent.thread

object Obd2BluetoothModule {
    private var socket: BluetoothSocket? = null
    private var isConnected = false

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(context: Context, device: BluetoothDevice) {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket?.connect()
                isConnected = true

                UiUpdater.setConnectedStatus(
                    context.getString(R.string.connected_to, device.name ?: "OBD2"),
                    "OBD2 Bluetooth"
                )
            } catch (e: Exception) {
                UiUpdater.appendLog(
                    context.getString(R.string.error_bt_connection) + ": ${e.message}"
                )
            }
        }
    }

    fun disconnect(context: Context) {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        isConnected = false
        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
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
        } catch (e: Exception) {
            UiUpdater.appendLog(
                context.getString(R.string.error_bt_connection) + ": ${e.message}"
            )
        }
    }
}
