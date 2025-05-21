package com.helly.psaimmotool

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var usbManager: UsbManager
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var requestPinButton: Button
    private lateinit var requestVinButton: Button
    private var connection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private val ACTION_USB_PERMISSION = "com.helly.psaimmotool.USB_PERMISSION"

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.apply {
                                val intf = getInterface(0)
                                val epOut = intf.getEndpoint(1)
                                endpointOut = epOut
                                connection = usbManager.openDevice(this)
                                connection?.claimInterface(intf, true)
                                log("Périphérique connecté. Prêt à envoyer les commandes.")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputText = findViewById(R.id.outputText)
        scrollView = findViewById(R.id.scrollView)
        requestPinButton = findViewById(R.id.requestPinButton)
        requestVinButton = findViewById(R.id.requestVinButton)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

        detectDevice()

        requestPinButton.setOnClickListener {
            sendPinRequestCommand()
        }

        requestVinButton.setOnClickListener {
            sendVinRequestCommand()
        }
    }

    private fun detectDevice() {
        val deviceList = usbManager.deviceList
        for (device in deviceList.values) {
            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    private fun sendPinRequestCommand() {
        val command = byteArrayOf(0x22.toByte(), 0xF1.toByte(), 0x90.toByte())
        connection?.bulkTransfer(endpointOut, command, command.size, 200)
        log("Commande de demande PIN envoyée")
    }

    private fun sendVinRequestCommand() {
        val command = byteArrayOf(0x22.toByte(), 0xF1.toByte(), 0x87.toByte())
        connection?.bulkTransfer(endpointOut, command, command.size, 200)
        log("Commande de demande VIN envoyée")
    }

    fun displayDetectedPin(hex: String) {
        if (hex.startsWith("62 F1 90")) {
            val hexParts = hex.split(" ")
            val pin = hexParts.subList(3, 7).joinToString("") {
                it.toInt(16).toChar().toString()
            }
            runOnUiThread {
                log("Code PIN détecté : $pin")
            }
        } else if (hex.startsWith("62 F1 87")) {
            val hexParts = hex.split(" ")
            val vin = hexParts.drop(3).joinToString("") {
                it.toInt(16).toChar().toString()
            }
            runOnUiThread {
                log("VIN détecté : $vin")
            }
        }
    }

    private fun log(message: String) {
        outputText.append("$message\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
