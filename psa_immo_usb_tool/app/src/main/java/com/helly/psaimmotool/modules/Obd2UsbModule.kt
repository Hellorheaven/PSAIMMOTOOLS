package com.helly.psaimmotool.modules

import android.content.Context
import android.hardware.usb.*
import android.os.Handler
import android.os.Looper
import com.helly.psaimmotool.utils.UiUpdater

object Obd2UsbModule {
    private var usbManager: UsbManager? = null
    private var connection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private var endpointIn: UsbEndpoint? = null

    fun connectUsb(context: Context) {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = usbManager?.deviceList?.values?.firstOrNull()
        if (device != null) {
            setupUsbDevice(context, device)
        } else {
            UiUpdater.appendLog(context.getString(com.helly.psaimmotool.R.string.no_usb_found))
        }
    }

    fun setupUsbDevice(context: Context, device: UsbDevice) {
        val usbInterface = device.getInterface(0)
        endpointOut = usbInterface.getEndpoint(1)
        endpointIn = usbInterface.getEndpoint(0)
        connection = usbManager?.openDevice(device)
        connection?.claimInterface(usbInterface, true)
        UiUpdater.setConnectedStatus(true.toString(), "OBD2 USB")
        UiUpdater.appendLog(context.getString(com.helly.psaimmotool.R.string.connected_to, "OBD2 USB"))
    }

    fun sendVinRequest() {
        sendCommand("09 02")
    }

    private fun sendCommand(command: String) {
        val fullCmd = "$command\r"
        connection?.bulkTransfer(endpointOut, fullCmd.toByteArray(), fullCmd.length, 1000)
        UiUpdater.appendLog("> $command")
    }
}
