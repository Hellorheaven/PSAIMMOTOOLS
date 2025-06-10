@file:Suppress("DEPRECATION")

package com.helly.psaimmotool

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.helly.psaimmotool.mobile.FrameInterpreter
import com.helly.psaimmotool.modules.*
import com.helly.psaimmotool.utils.LocaleUtils
import com.helly.psaimmotool.utils.LogExporter
import com.helly.psaimmotool.utils.PermissionUtils
import com.helly.psaimmotool.utils.UiUpdater

class MainActivity : AppCompatActivity() {

    private lateinit var moduleSelector: Spinner
    private lateinit var bluetoothDeviceSpinner: Spinner
    private lateinit var connectButton: Button
    private lateinit var requestPinButton: Button
    private lateinit var requestVinButton: Button
    private lateinit var startCanListenButton: Button
    private lateinit var sendFrameLayout: LinearLayout
    private lateinit var inputFrameText: EditText
    private lateinit var sendFrameButton: Button
    private lateinit var outputText: TextView
    private lateinit var statusText: TextView
    private lateinit var exportLogsButton: Button
    private lateinit var clearLogsButton: Button

    private var isConnected = false
    private var selectedBluetoothDevice: BluetoothDevice? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_USB_PERMISSION) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.let {
                        when (moduleSelector.selectedItemPosition) {
                            0 -> CanBusModule.setupUsbDevice(this@MainActivity, it)
                            1 -> Obd2UsbModule.setupUsbDevice(this@MainActivity, it)
                            3 -> KLineUsbModule.setupUsbDevice(this@MainActivity, it)
                        }
                        enableModuleButtons()
                        connectButton.text = getString(R.string.disconnect)
                        isConnected = true
                    }
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.error_usb_permission), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.topAppBar))

        moduleSelector = findViewById(R.id.moduleSelector)
        bluetoothDeviceSpinner = findViewById(R.id.bluetoothDeviceSpinner)
        connectButton = findViewById(R.id.connectButton)
        requestPinButton = findViewById(R.id.requestPinButton)
        requestVinButton = findViewById(R.id.requestVinButton)
        startCanListenButton = findViewById(R.id.startCanListenButton)
        sendFrameLayout = findViewById(R.id.sendFrameLayout)
        inputFrameText = findViewById(R.id.inputFrameText)
        sendFrameButton = findViewById(R.id.sendFrameButton)
        outputText = findViewById(R.id.outputText)
        statusText = findViewById(R.id.statusText)
        exportLogsButton = findViewById(R.id.exportLogsButton)
        clearLogsButton = findViewById(R.id.clearLogsButton)

        UiUpdater.outputText = outputText
        UiUpdater.statusText = statusText

        moduleSelector.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.module_list,
            android.R.layout.simple_spinner_dropdown_item
        )

        moduleSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val isCan = pos == 0 || pos == 4
                val isKLine = pos == 3
                val showAdvanced = isCan || isKLine

                requestPinButton.visibility = if (isCan) View.VISIBLE else View.GONE
                startCanListenButton.visibility = if (isCan) View.VISIBLE else View.GONE
                sendFrameLayout.visibility = if (showAdvanced) View.VISIBLE else View.GONE
                requestVinButton.visibility = View.VISIBLE
                bluetoothDeviceSpinner.visibility = if (pos == 2) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        connectButton.setOnClickListener {
            if (isConnected) disconnectModule() else connectToModule()
        }

        requestPinButton.setOnClickListener {
            when (moduleSelector.selectedItemPosition) {
                0 -> CanBusModule.sendPinRequest()
                4 -> CanBusUartModule.sendPinRequest()
            }
        }

        requestVinButton.setOnClickListener {
            when (moduleSelector.selectedItemPosition) {
                0 -> CanBusModule.sendVinRequest()
                1 -> Obd2UsbModule.sendVinRequest(this)
                2 -> Obd2BluetoothModule.sendVinRequest(this)
                3 -> KLineUsbModule.sendVinRequest(this)
                4 -> CanBusUartModule.sendVinRequest()
            }
        }

        startCanListenButton.setOnClickListener {
            when (moduleSelector.selectedItemPosition) {
                0 -> CanBusModule.listenAll()
                4 -> CanBusUartModule.listenAll()
            }
        }

        sendFrameButton.setOnClickListener {
            val frame = inputFrameText.text.toString()
            if (frame.isNotBlank()) {
                when (moduleSelector.selectedItemPosition) {
                    0 -> CanBusModule.sendCustomFrame(frame)
                    3 -> KLineUsbModule.sendCommand(this, frame)
                    4 -> CanBusUartModule.sendCustomFrame(frame)
                }
                val interpreted = FrameInterpreter.interpretAndFormat(this, frame)
                UiUpdater.appendLog(getString(R.string.can_frame_decoded, interpreted))
            }
        }

        exportLogsButton.setOnClickListener {
            LogExporter.exportLogs(this, outputText.text.toString())
        }

        clearLogsButton.setOnClickListener {
            outputText.text = ""
            UiUpdater.appendLog(getString(R.string.logs_cleared))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, IntentFilter(ACTION_USB_PERMISSION), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbReceiver, IntentFilter(ACTION_USB_PERMISSION))
        }

        initBluetoothDeviceList()
        PermissionUtils.requestNecessaryPermissions(this)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectToModule() {
        when (moduleSelector.selectedItemPosition) {
            0 -> CanBusModule.connectUsb(this)
            1 -> Obd2UsbModule.connectUsb(this)
            2 -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    selectedBluetoothDevice?.let {
                        Obd2BluetoothModule.connectToDevice(this, it)
                    } ?: UiUpdater.appendLog(getString(R.string.module_not_found))
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1001)
                    return
                }
            }
            3 -> KLineUsbModule.connectUsb(this)
            4 -> CanBusUartModule.connectUsb(this)
        }
        connectButton.text = getString(R.string.disconnect)
        isConnected = true
        enableModuleButtons()
    }

    private fun disconnectModule() {
        when (moduleSelector.selectedItemPosition) {
            0 -> CanBusModule.disconnect(this)
            1 -> Obd2UsbModule.disconnect(this)
            2 -> Obd2BluetoothModule.disconnect(this)
            3 -> KLineUsbModule.disconnect(this)
            4 -> CanBusUartModule.disconnect(this)
        }
        UiUpdater.setDisconnectedStatus(getString(R.string.disconnected))
        connectButton.text = getString(R.string.connect)
        isConnected = false
        disableModuleButtons()
    }

    private fun enableModuleButtons() {
        requestPinButton.isEnabled = true
        requestVinButton.isEnabled = true
        startCanListenButton.isEnabled = true
        sendFrameButton.isEnabled = true
    }

    private fun disableModuleButtons() {
        requestPinButton.isEnabled = false
        requestVinButton.isEnabled = false
        startCanListenButton.isEnabled = false
        sendFrameButton.isEnabled = false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun initBluetoothDeviceList() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && adapter.isEnabled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1002)
                return
            }

            val paired = adapter.bondedDevices
            val devices = paired.map { it.name + " (${it.address})" }
            val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, devices)
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            bluetoothDeviceSpinner.adapter = adapterSpinner

            bluetoothDeviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedBluetoothDevice = paired.elementAtOrNull(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
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


    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.helly.psaimmotool.USB_PERMISSION"
    }
}
