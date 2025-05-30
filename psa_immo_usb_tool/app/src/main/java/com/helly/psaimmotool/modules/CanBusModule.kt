package com.helly.psaimmotool.modules

import android.content.Context
import android.app.PendingIntent
import android.hardware.usb.*
import android.widget.Toast
import com.helly.psaimmotool.R
import com.helly.psaimmotool.utils.UiUpdater

object CanBusModule {

    private var usbConnection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
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
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val usbInterface = device.getInterface(0)
        endpointOut = usbInterface.getEndpoint(1)
        usbConnection = usbManager?.openDevice(device)
        usbConnection?.claimInterface(usbInterface, true)
        isConnected = true
        UiUpdater.setConnectedStatus(true.toString(), "CANBUS")
    }

    fun disconnect() {
        usbConnection?.close()
        usbConnection = null
        endpointOut = null
        isConnected = false
        UiUpdater.setConnectedStatus(false.toString(), "")
    }

    // --- Commandes ---

    fun sendPinRequest() {
        sendCommand("22 F1 90")
    }

    fun sendVinRequest() {
        sendCommand("09 02")
    }

    fun listenAll() {
        UiUpdater.appendLog("Écoute des trames CAN...")
        // TODO: Implémentation réelle de la lecture CAN en temps réel
    }

    fun sendCustomFrame(frame: String) {
        sendCommand(frame)
    }

    private fun sendCommand(command: String) {
        if (!isConnected || usbConnection == null || endpointOut == null) {
            UiUpdater.appendLog("Non connecté au module CAN.")
            return
        }
        val fullCommand = "$command\r"
        try {
            usbConnection!!.bulkTransfer(endpointOut, fullCommand.toByteArray(), fullCommand.length, 1000)
            UiUpdater.appendLog("> $command")
        } catch (e: Exception) {
            UiUpdater.appendLog("Erreur d'envoi : ${e.message}")
        }
    }

    // --- Fonctions avancées PSARemote ---

    fun sendButtonCode(code: Int) {
        val message = byteArrayOf(0xFD.toByte(), 0x04, 0x02, code.toByte())
        sendCanMessageWithChecksum(message)
    }

    fun sendTemperature(temp: Int) {
        val message = byteArrayOf(0xFD.toByte(), 0x04, 0x03, temp.toByte())
        sendCanMessageWithChecksum(message)
    }

    fun sendTripDataCar(distance: Int, consumption: Float, speed: Int) {
        val message = byteArrayOf(0xFD.toByte(), 0x07, 0x04) +
                intToTwoBytes(distance) +
                floatToTwoBytes(consumption) +
                intToTwoBytes(speed)
        sendCanMessageWithChecksum(message)
    }

    fun sendCarInfo(speed: Int, rpm: Int, fuelLevel: Int, doorStatus: Byte) {
        val message = byteArrayOf(0xFD.toByte(), 0x06, 0x05) +
                intToTwoBytes(speed) +
                intToTwoBytes(rpm) +
                byteArrayOf(fuelLevel.toByte(), doorStatus)
        sendCanMessageWithChecksum(message)
    }

    // --- Outils ---

    private fun sendCanMessageWithChecksum(message: ByteArray) {
        val checksum = calculateChecksum(message)
        val fullMessage = message + checksum
        sendRawFrame(fullMessage)
    }

    private fun sendRawFrame(frame: ByteArray) {
        if (!isConnected || usbConnection == null || endpointOut == null) {
            UiUpdater.appendLog("Module CAN non connecté.")
            return
        }
        try {
            usbConnection!!.bulkTransfer(endpointOut, frame, frame.size, 1000)
            UiUpdater.appendLog("> ${frame.joinToString(" ") { String.format("%02X", it) }}")
        } catch (e: Exception) {
            UiUpdater.appendLog("Erreur CAN : ${e.message}")
        }
    }

    private fun calculateChecksum(data: ByteArray): Byte {
        var sum = 0
        for (i in 1 until data.size) {
            sum += data[i].toInt() and 0xFF
        }
        return (sum and 0xFF).toByte()
    }

    private fun intToTwoBytes(value: Int): ByteArray {
        return byteArrayOf((value shr 8).toByte(), (value and 0xFF).toByte())
    }

    private fun floatToTwoBytes(value: Float): ByteArray {
        val intVal = (value * 10).toInt()
        return intToTwoBytes(intVal)
    }
}
