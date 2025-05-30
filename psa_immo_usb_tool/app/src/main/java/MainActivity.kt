package com.helly.psaimmotool

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.helly.psaimmotool.modules.CanBusModule
import com.helly.psaimmotool.modules.Obd2BluetoothModule
import com.helly.psaimmotool.modules.Obd2UsbModule
import com.helly.psaimmotool.utils.LocaleUtils
import com.helly.psaimmotool.utils.UiUpdater

class MainActivity : AppCompatActivity() {

    private lateinit var moduleSelector: Spinner
    private lateinit var connectButton: Button
    private lateinit var requestPinButton: Button
    private lateinit var requestVinButton: Button
    private lateinit var startCanListenButton: Button
    private lateinit var sendFrameLayout: LinearLayout
    private lateinit var inputFrameText: EditText
    private lateinit var sendFrameButton: Button
    private lateinit var sendButtonCode: Button
    private lateinit var sendTemperatureButton: Button
    private lateinit var sendTripDataButton: Button
    private lateinit var sendCarInfoButton: Button
    private lateinit var outputText: TextView
    private lateinit var statusText: TextView

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_USB_PERMISSION) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(ACTION_USB_PERMISSION, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(ACTION_USB_PERMISSION)
                }

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.let { CanBusModule.setupUsbDevice(this@MainActivity, it) }
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.error_usb_permission), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        moduleSelector = findViewById(R.id.moduleSelector)
        connectButton = findViewById(R.id.connectButton)
        requestPinButton = findViewById(R.id.requestPinButton)
        requestVinButton = findViewById(R.id.requestVinButton)
        startCanListenButton = findViewById(R.id.startCanListenButton)
        sendFrameLayout = findViewById(R.id.sendFrameLayout)
        inputFrameText = findViewById(R.id.inputFrameText)
        sendFrameButton = findViewById(R.id.sendFrameButton)
        sendButtonCode = findViewById(R.id.sendButtonCode)
        sendTemperatureButton = findViewById(R.id.sendTemperatureButton)
        sendTripDataButton = findViewById(R.id.sendTripDataButton)
        sendCarInfoButton = findViewById(R.id.sendCarInfoButton)
        outputText = findViewById(R.id.outputText)
        statusText = findViewById(R.id.statusText)

        UiUpdater.outputText = outputText
        UiUpdater.statusText = statusText

        moduleSelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("CANBUS", "OBD2 USB", "OBD2 Bluetooth")
        )

        moduleSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isCan = position == 0
                requestPinButton.visibility = if (isCan) View.VISIBLE else View.GONE
                startCanListenButton.visibility = if (isCan) View.VISIBLE else View.GONE
                sendFrameLayout.visibility = if (isCan) View.VISIBLE else View.GONE
                sendButtonCode.visibility = if (isCan) View.VISIBLE else View.GONE
                sendTemperatureButton.visibility = if (isCan) View.VISIBLE else View.GONE
                sendTripDataButton.visibility = if (isCan) View.VISIBLE else View.GONE
                sendCarInfoButton.visibility = if (isCan) View.VISIBLE else View.GONE
                requestVinButton.visibility = View.VISIBLE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        connectButton.setOnClickListener {
            when (moduleSelector.selectedItemPosition) {
                0 -> CanBusModule.connectUsb(this)
                1 -> Obd2UsbModule.connectUsb(this)
                2 -> Obd2BluetoothModule.connect(this)
            }
        }

        requestVinButton.setOnClickListener {
            when (moduleSelector.selectedItemPosition) {
                0 -> CanBusModule.sendVinRequest()
                1 -> Obd2UsbModule.sendVinRequest()
                2 -> Obd2BluetoothModule.sendVinRequest()
            }
        }

        requestPinButton.setOnClickListener {
            if (moduleSelector.selectedItemPosition == 0)
                CanBusModule.sendPinRequest()
        }

        startCanListenButton.setOnClickListener {
            if (moduleSelector.selectedItemPosition == 0)
                CanBusModule.listenAll()
        }

        sendFrameButton.setOnClickListener {
            if (moduleSelector.selectedItemPosition == 0) {
                val frame = inputFrameText.text.toString().trim()
                if (frame.isNotEmpty()) {
                    CanBusModule.sendCustomFrame(frame)
                }
            }
        }

        sendButtonCode.setOnClickListener {
            if (moduleSelector.selectedItemPosition == 0) {
                CanBusModule.sendButtonCode(0x22) // exemple : bouton "source radio"
            }
        }

        sendTemperatureButton.setOnClickListener {
            if (moduleSelector.selectedItemPosition == 0) {
                CanBusModule.sendTemperature(22) // exemple : 22°C
            }
        }

        sendTripDataButton.setOnClickListener {
            if (moduleSelector.selectedItemPosition == 0) {
                CanBusModule.sendTripDataCar(250, 6.4f, 75)
            }
        }

        sendCarInfoButton.setOnClickListener {
            if (moduleSelector.selectedItemPosition == 0) {
                CanBusModule.sendCarInfo(85, 2800, 50, 0x00)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, IntentFilter(ACTION_USB_PERMISSION), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbReceiver, IntentFilter(ACTION_USB_PERMISSION))
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_theme_light -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                true
            }
            R.id.menu_theme_dark -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                true
            }
            R.id.menu_lang_fr -> {
                LocaleUtils.setLocaleAndRestart(this, "fr")
                true
            }
            R.id.menu_lang_en -> {
                LocaleUtils.setLocaleAndRestart(this, "en")
                true
            }
            R.id.menu_quit -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.helly.psaimmotool.USB_PERMISSION"
    }
}
