package com.helly.psaimmotool

import android.app.Service
import android.content.Intent
import android.hardware.usb.*
import android.os.Build
import android.os.IBinder
import java.io.File
import java.io.FileOutputStream

class UsbService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val connection = usbManager.openDevice(device)
        val intf = device?.getInterface(0)
        val endpointIn = intf?.getEndpoint(0)

        connection?.claimInterface(intf, true)

        Thread {
            val logFile = File(filesDir, "psa_can_log.txt")
            val fos = FileOutputStream(logFile, true)
            val buffer = ByteArray(64)

            while (true) {
                val read = connection?.bulkTransfer(endpointIn, buffer, buffer.size, 500)
                if (read != null && read > 0) {
                    val frame = buffer.copyOfRange(0, read)
                    val hex = frame.joinToString(" ") { String.format("%02X", it) }
                    fos.write((hex + "\n").toByteArray())
                    fos.flush()
                }
            }
        }.start()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
