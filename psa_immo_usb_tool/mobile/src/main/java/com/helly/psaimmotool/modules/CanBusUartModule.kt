package com.helly.psaimmotool.modules

import android.content.Context
import com.helly.psaimmotool.R
import com.helly.psaimmotool.utils.UiUpdater
import com.helly.psaimmotool.mobile.FrameInterpreter
import com.hoho.android.usbserial.driver.*
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.util.concurrent.Executors

object CanBusUartModule {

    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private var isConnected = false

    fun connectUsb(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            UiUpdater.appendLog(context.getString(R.string.no_usb_found))
            return
        }

        val driver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device) ?: run {
            UiUpdater.appendLog(context.getString(R.string.error_usb_permission))
            return
        }

        port = driver.ports[0]
        port?.open(connection)
        port?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        ioManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray?) {
                data?.let {
                    val hex = it.joinToString(" ") { b -> "%02X".format(b) }
                    UiUpdater.appendLog("> $hex")

                    val contextLocal = UiUpdater.outputText?.context
                    contextLocal?.let {
                        val decoded = FrameInterpreter.interpretAndFormat(it, hex)
                        UiUpdater.appendLog(it.getString(R.string.can_frame_decoded, decoded))
                    }
                }
            }

            override fun onRunError(e: Exception?) {
                UiUpdater.appendLog("UART error: ${e?.message}")
            }
        })

        Executors.newSingleThreadExecutor().submit(ioManager)
        isConnected = true
        UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, "CAN UART"), "CAN UART")
    }

    fun disconnect(context: Context) {
        try {
            ioManager?.stop()
            port?.close()
        } catch (_: Exception) { }
        ioManager = null
        port = null
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
        UiUpdater.appendLog("🔍 " + (UiUpdater.outputText?.context?.getString(R.string.can_listening)
            ?: "Listening to CAN frames (UART)..."))
        // already handled via SerialInputOutputManager
    }

    fun sendCustomFrame(frame: String) {
        sendCommand(frame)
    }

    private fun sendCommand(command: String) {
        if (!isConnected || port == null) {
            UiUpdater.appendLog("Not connected to CAN UART module.")
            return
        }

        val fullCommand = "$command\r"
        try {
            port!!.write(fullCommand.toByteArray(), 1000)
            UiUpdater.appendLog("> $command")
        } catch (e: Exception) {
            UiUpdater.appendLog("UART send error: ${e.message}")
        }
    }

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

    private fun sendCanMessageWithChecksum(message: ByteArray) {
        val checksum = calculateChecksum(message)
        val fullMessage = message + checksum
        sendRawFrame(fullMessage)
    }

    private fun sendRawFrame(frame: ByteArray) {
        if (!isConnected || port == null) {
            UiUpdater.appendLog("CAN UART not connected.")
            return
        }

        try {
            port!!.write(frame, 1000)
            UiUpdater.appendLog("> ${frame.joinToString(" ") { "%02X".format(it) }}")
        } catch (e: Exception) {
            UiUpdater.appendLog("UART error: ${e.message}")
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
