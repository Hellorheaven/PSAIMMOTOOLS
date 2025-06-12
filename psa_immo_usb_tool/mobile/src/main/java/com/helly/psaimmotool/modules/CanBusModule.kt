package com.helly.psaimmotool.modules

import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.*
import com.helly.psaimmotool.R
import com.helly.psaimmotool.utils.PermissionUtils
import com.helly.psaimmotool.utils.UiUpdater
import com.helly.psaimmotool.mobile.FrameInterpreter

object CanBusModule {

    private var usbConnection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private var endpointIn: UsbEndpoint? = null
    private var isConnected = false

    fun connectUsb(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val device = usbManager?.deviceList?.values?.firstOrNull()
        if (device != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0,
                android.content.Intent(context.packageName + ".USB_PERMISSION"),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        } else {
            UiUpdater.appendLog(context.getString(R.string.no_usb_found))
        }
    }

    fun setupUsbDevice(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        if (!PermissionUtils.hasUsbPermission(context, device)) {
            UiUpdater.appendLog(context.getString(R.string.error_usb_permission))
            return
        }

        val usbInterface = device.getInterface(0)
        endpointOut = usbInterface.getEndpoint(1)
        endpointIn = usbInterface.getEndpoint(0)
        usbConnection = usbManager.openDevice(device)
        usbConnection?.claimInterface(usbInterface, true)
        isConnected = true

        UiUpdater.setConnectedStatus(
            context.getString(R.string.connected_to, "CANBUS"),
            "CANBUS"
        )
    }

    fun disconnect(context: Context) {
        try {
            usbConnection?.close()
        } catch (_: Exception) { }
        usbConnection = null
        endpointOut = null
        endpointIn = null
        isConnected = false

        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }

    fun sendPinRequest() {
        sendCommand("22 F1 90")
    }

    fun sendVinRequest() {
        sendCommand("09 02")
    }

    fun listenAll() {
        val context = UiUpdater.outputText?.context
        UiUpdater.appendLog(context?.getString(R.string.can_listening) ?: "🔍 Listening to CAN frames...")

        Thread {
            try {
                val buffer = ByteArray(64)
                while (isConnected && usbConnection != null && endpointIn != null) {
                    val len = usbConnection!!.bulkTransfer(endpointIn, buffer, buffer.size, 500)
                    if (len > 0) {
                        val raw = buffer.take(len).joinToString(" ") { "%02X".format(it) }
                        UiUpdater.appendLog("> $raw")
                        context?.let {
                            val decoded = FrameInterpreter.interpretAndFormat(it, raw)
                            UiUpdater.appendLog(it.getString(R.string.can_frame_decoded, decoded))
                        }
                    }
                }
            } catch (e: Exception) {
                UiUpdater.appendLog(context?.getString(R.string.can_listen_error, e.message ?: "error") ?: "CAN error")
            }
        }.start()
    }

    fun sendCustomFrame(frame: String) {
        sendCommand(frame)
    }

    private fun sendCommand(command: String) {
        val context = UiUpdater.outputText?.context
        if (!isConnected || usbConnection == null || endpointOut == null) {
            UiUpdater.appendLog(context?.getString(R.string.no_module_connected) ?: "Not connected to CAN module.")
            return
        }

        val fullCommand = "$command\r"
        try {
            usbConnection!!.bulkTransfer(endpointOut, fullCommand.toByteArray(), fullCommand.length, 1000)
            UiUpdater.appendLog("> $command")
        } catch (e: Exception) {
            UiUpdater.appendLog(context?.getString(R.string.error_export_logs, e.message ?: "unknown error") ?: "Send error")
        }
    }
}
