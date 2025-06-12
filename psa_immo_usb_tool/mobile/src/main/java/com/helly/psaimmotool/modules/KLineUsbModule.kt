package com.helly.psaimmotool.modules

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.*
import com.helly.psaimmotool.R
import com.helly.psaimmotool.utils.PermissionUtils
import com.helly.psaimmotool.utils.UiUpdater

object KLineUsbModule {

    private var usbConnection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private var isConnected = false

    fun connectUsb(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val device = usbManager?.deviceList?.values?.firstOrNull()

        if (device != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(context.packageName + ".USB_PERMISSION"),
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
        usbConnection = usbManager.openDevice(device)
        usbConnection?.claimInterface(usbInterface, true)

        isConnected = true
        UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, "K-Line USB"), "K-Line USB")
    }

    fun disconnect(context: Context) {
        try {
            usbConnection?.close()
        } catch (_: Exception) { }
        usbConnection = null
        endpointOut = null
        isConnected = false
        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }

    fun sendVinRequest(context: Context) {
        sendCommand(context, "09 02")
    }

    fun sendCommand(context: Context, frame: String) {
        if (!isConnected || usbConnection == null || endpointOut == null) {
            UiUpdater.appendLog(context.getString(R.string.no_module_connected))
            return
        }

        val fullCommand = "$frame\r"
        try {
            usbConnection!!.bulkTransfer(endpointOut, fullCommand.toByteArray(), fullCommand.length, 1000)
            UiUpdater.appendLog(context.getString(R.string.sending_kline_frame, frame))
        } catch (e: Exception) {
            UiUpdater.appendLog(context.getString(R.string.error_kline_send, e.message ?: ""))
        }
    }
}
