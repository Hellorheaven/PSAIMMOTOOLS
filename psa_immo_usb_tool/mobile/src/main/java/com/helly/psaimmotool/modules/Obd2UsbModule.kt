package com.helly.psaimmotool.modules

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import com.helly.psaimmotool.R
import com.helly.psaimmotool.utils.PermissionUtils
import com.helly.psaimmotool.utils.UiUpdater

object Obd2UsbModule {

    private var connection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private var endpointIn: UsbEndpoint? = null
    private var isConnected = false

    fun connectUsb(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val device = usbManager?.deviceList?.values?.firstOrNull()

        if (device != null) {
            setupUsbDevice(context, device)
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
        connection = usbManager.openDevice(device)
        connection?.claimInterface(usbInterface, true)

        isConnected = true
        UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, "OBD2 USB"), "OBD2 USB")
    }

    fun sendVinRequest(context: Context) {
        sendCommand(context, "09 02")
    }

    private fun sendCommand(context: Context, command: String) {
        if (!isConnected || connection == null || endpointOut == null) {
            UiUpdater.appendLog(context.getString(R.string.no_module_connected))
            return
        }

        val fullCmd = "$command\r"
        try {
            connection!!.bulkTransfer(endpointOut, fullCmd.toByteArray(), fullCmd.length, 1000)
            UiUpdater.appendLog("> $command")
        } catch (e: Exception) {
            UiUpdater.appendLog(context.getString(R.string.error_export_logs, e.message ?: ""))
        }
    }

    fun disconnect(context: Context) {
        try {
            connection?.close()
        } catch (_: Exception) { }

        connection = null
        endpointOut = null
        endpointIn = null
        isConnected = false

        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }
}
