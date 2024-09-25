package com.valenpatel.btcbuzz

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.valenpatel.btcbuzz.databinding.ActivityMainBinding
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val discoveredDevices: MutableList<BluetoothDevice> = mutableListOf()
    lateinit var bluetoothReciever: BluetoothReciever
    val myUUID: UUID = UUID.randomUUID()
    private val MY_UUID: UUID = myUUID

    companion object {
        const val PERMISSION_REQUEST_CODE = 101
        const val DISCOVERABILITY_REQUEST_CODE = 102
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothReciever = BluetoothReciever()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        requestBluetoothPermissions()

        setupClickListeners()

        getPairedDevices()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupClickListeners() {
        binding.btnOnOFF.setOnClickListener {
            toggleBluetooth()
        }
        binding.btnDiscoverable.setOnClickListener {
            enableDiscoverability()
        }
        binding.btnPairedDevices.setOnClickListener {
            getPairedDevices()
        }
        binding.btnScannedDevices.setOnClickListener {
            checkAndRequestLocationPermission()
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )) {
                PackageManager.PERMISSION_DENIED -> {
                    showLocationPermissionDialog()
                }

                PackageManager.PERMISSION_GRANTED -> {
                    getScannedDevices()
                }
            }
        }
    }

    private fun showLocationPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Runtime Permission")
            .setMessage("Give permission to access location")
            .setNeutralButton("Okay") { dialog, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
                dialog.dismiss()
            }.show()
    }

    private fun getScannedDevices() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            discoveredDevices.clear()

            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            registerReceiver(discoveryReceiver, filter)

            bluetoothAdapter.startDiscovery()
        } else {
            println("Bluetooth scan permission not granted.")
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        discoveredDevices.add(it)
                        for (i in discoveredDevices) {
                            Toast.makeText(this@MainActivity, "" + i.name, Toast.LENGTH_SHORT)
                                .show()
                        }
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        Log.d(
                            "discoverDevices",
                            "Discovered device: ${it.name ?: "Unnamed"}, Address: ${it.address}"
                        )
                        Toast.makeText(
                            this@MainActivity,
                            "Discovered: ${it.name ?: "Unnamed"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("discoverDevices", "Discovery started")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("discoverDevices", "Discovery finished")
                }
            }
        }
    }

    private fun getPairedDevices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            if (!pairedDevices.isNullOrEmpty()) {
                val deviceList = pairedDevices.toList()

                if (deviceList.isNotEmpty()) {
                    binding.textDevice1.text = deviceList.getOrNull(0)?.name ?: "No device"
                }
                if (deviceList.size > 1) {
                    binding.textDevice2.text = deviceList.getOrNull(1)?.name ?: "No device"
                }
                if (deviceList.size > 2) {
                    binding.textDevice3.text = deviceList.getOrNull(2)?.name ?: "No device"
                }
            }
            else {
                Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Bluetooth permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        if (requiredPermissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
        } else {
            setupBluetoothToggle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupBluetoothToggle() {
        // Bluetooth toggle setup code
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun toggleBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startActivity(enableBtIntent)
                val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(bluetoothReciever, intentFilter)
            }
        } else {
            bluetoothAdapter.disable()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableDiscoverability() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                DISCOVERABILITY_REQUEST_CODE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    setupBluetoothToggle()
                } else {
                    // Handle permission denial
                }
            }

            DISCOVERABILITY_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    enableDiscoverability()
                } else {
                    // Handle permission denial
                }
            }
        }
    }

//    private fun connectToDevice(device: BluetoothDevice) {
//        val uuid: UUID = myUUID
//        val socket: BluetoothSocket? = device.createRfcommSocketToServiceRecord(uuid)
//
//        try {
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.BLUETOOTH_CONNECT
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return
//            }
//            socket?.connect()
//            // Start a communication thread
//            if (socket != null) {
//                startCommunication(socket)
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//            // Handle connection failure
//        }
//    }

//    private fun startCommunication(socket: BluetoothSocket) {
//        val inputStream = socket.inputStream
//        val outputStream = socket.outputStream
//        val buffer = ByteArray(1024) // Buffer for the stream
//        var bytes: Int
//
//        // Thread to read data
//        val readThread = Thread {
//            try {
//                while (true) {
//                    bytes = inputStream.read(buffer)
//                    val message = String(buffer, 0, bytes)
//                    runOnUiThread {
//                        binding.textReceivedMessages.append("Received: $message\n")
//                    }
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//
//
//        readThread.start()
//
//        // Sending data when button is clicked
//        binding.buttonSend.setOnClickListener {
//            val message = binding.editTextMessage.text.toString()
//            try {
//                outputStream.write(message.toByteArray())
//                binding.textReceivedMessages.append("Sent: $message\n")
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//
//    @SuppressLint("MissingPermission")
//    private fun startServer() {
//        val serverSocket: BluetoothServerSocket? =
//            bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyApp", MY_UUID)
//
//        var socket: BluetoothSocket?
//
//        while (true) {
//            socket = try {
//                serverSocket?.accept()
//            } catch (e: IOException) {
//                e.printStackTrace()
//                break
//            }
//
//            socket?.also {
//                startCommunication(it)
//                serverSocket?.close()
//
//            }
//        }
//    }
//
//    private fun closeConnection(socket: BluetoothSocket) {
//        try {
//            socket.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }
//
//    private fun startListeningForMessages(socket: BluetoothSocket) {
//        val inputStream = socket.inputStream
//        val buffer = ByteArray(1024)
//        var bytes: Int
//
//        // Thread to read data
//        val readThread = Thread {
//            try {
//                while (true) {
//                    bytes = inputStream.read(buffer)
//                    val receivedMessage = String(buffer, 0, bytes)
//
//                    // Update UI with the received message
//                    runOnUiThread {
//                        binding.textReceivedMessages.append("Received: $receivedMessage\n")
//                    }
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//        readThread.start()
//    }





//    runOnUiThread {
//        messageList.add(receivedMessage)
//        messageAdapter.notifyDataSetChanged()
//    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(discoveryReceiver)
            unregisterReceiver(bluetoothReciever)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}


//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothManager
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.DialogInterface
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.annotation.RequiresApi
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import com.valenpatel.btcbuzz.databinding.ActivityMainBinding
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var bluetoothManager: BluetoothManager
//    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private lateinit var bluetoothReceiver: BluetoothReciever
//    private lateinit var discoverabilityReceiver: Discoverability
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//
//        bluetoothReceiver = BluetoothReciever()
//        discoverabilityReceiver = Discoverability()
//
//        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothAdapter = bluetoothManager.adapter
//
//        // Request permissions at the start
//        requestBluetoothPermissions()
//
//        binding.btnOnOFF.setOnClickListener {
//            toggleBluetooth()
//        }
//        binding.btnDiscoverable.setOnClickListener {
//            enableDiscoverability()
//        }
//        binding.btnPairedDevices.setOnClickListener {
//            getPairedDevices()
//        }
//        binding.btnScannedDevices.setOnClickListener {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                when (ContextCompat.checkSelfPermission(
//                    baseContext,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                )) {
//                    PackageManager.PERMISSION_DENIED -> {
//                        androidx.appcompat.app.AlertDialog.Builder(this)
//                            .setTitle("Runtime Permission")
//                            .setMessage("Give permission to access location")
//                            .setNeutralButton("Okay") { dialog, _ ->
//                                ActivityCompat.requestPermissions(
//                                    this,
//                                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
//                                    101
//                                )
//                            }.show()
//                    }
//
//                    PackageManager.PERMISSION_GRANTED -> {
//                        Log.d("discoverDevices", "Permission Granted")
//                    }
//                }
//            }
//            getScannedDevices()
//        }
//    }
//
//    private var discoveredDevices: MutableList<BluetoothDevice> = mutableListOf()
//
//    private val discoveryReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val action = intent.action
//            if (BluetoothDevice.ACTION_FOUND == action) {
//                // Get the BluetoothDevice object from the Intent
//                val device: BluetoothDevice =
//                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
//                // Add the device to the discovered devices list
//                discoveredDevices.add(device)
//                // You can update your UI here to display the discovered device
//                if (ActivityCompat.checkSelfPermission(
//                        this@MainActivity,
//                        Manifest.permission.BLUETOOTH_CONNECT
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//                    return
//                }
//                println("Discovered device: ${device.name}, Address: ${device.address}")
//            }
//        }
//    }
//
//    private fun getScannedDevices() {
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_SCAN
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            discoveredDevices = mutableListOf()  // Initialize the list to hold discovered devices
//
//
//            // Register the receiver for ACTION_FOUND
//            val filter = IntentFilter()
//            filter.addAction(BluetoothDevice.ACTION_FOUND)
//            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
//            // You may also want to register for ACTION_DISCOVERY_FINISHED to know when scanning is complete
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//            registerReceiver(discoveryReceiver, filter)
//            // Start discovery
//            bluetoothAdapter.startDiscovery()
//        } else {
//            // Handle the case where Bluetooth scan permission is not granted
//            println("Bluetooth scan permission not granted.")
//        }
//    }
//
//    private val discoverable = object : BroadcastReceiver() {
//        override fun onReceive(p0: Context?, p1: Intent?) {
//            var action = ""
//            if (p1 != null) {
//                action = p1.action.toString()
//            }
//            when(action){
//                BluetoothAdapter.ACTION_STATE_CHANGED->{
//                    Log.d("discoverDevices", "STATE CHANGES")
//                }
//                BluetoothAdapter.ACTION_DISCOVERY_STARTED->{
//                    Log.d("discoverDevices", "DISCOVERY STARTED")
//                }
//                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{
//                    Log.d("discoverDevices", "DISCOVERY FINISHED")
//                }
//                BluetoothDevice.ACTION_FOUND->{
//                    val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//                    if(device!=null){
//                        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                            return
//                        Log.d("discoverDevices", device.name.toString())
//                        }
//                    }
//                }
//            }
//        }
//
//    }
//
//    private fun getPairedDevices() {
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            // Get paired devices
//            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
//
//            if (pairedDevices != null && pairedDevices.isNotEmpty()) {
//
//                for (device in pairedDevices) {
//                    Toast.makeText(
//                        this,
//                        device.name + device.address + device.uuids,
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } else {
//                // No paired devices found
//                Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            // Handle the case where Bluetooth permission is not granted
//            Toast.makeText(this, "Bluetooth permission not granted.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun requestBluetoothPermissions() {
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH
//            ) != PackageManager.PERMISSION_GRANTED ||
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_ADMIN
//            ) != PackageManager.PERMISSION_GRANTED ||
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_CONNECT
//            ) != PackageManager.PERMISSION_GRANTED ||
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_SCAN
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(
//                    Manifest.permission.BLUETOOTH,
//                    Manifest.permission.BLUETOOTH_ADMIN,
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN
//                ),
//                101
//            )
//        } else {
//            setupBluetoothToggle()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun setupBluetoothToggle() {
//        // Bluetooth toggle setup code
//    }
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun toggleBluetooth() {
//        if (!bluetoothAdapter.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.BLUETOOTH_CONNECT
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                return
//            }
//            startActivity(enableBtIntent)
//            val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
//            registerReceiver(bluetoothReceiver, intentFilter)
//        } else {
//            bluetoothAdapter.disable()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun enableDiscoverability() {
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH
//            ) != PackageManager.PERMISSION_GRANTED ||
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_ADMIN
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(
//                    Manifest.permission.BLUETOOTH,
//                    Manifest.permission.BLUETOOTH_ADMIN
//                ),
//                102
//            )
//        } else {
//            // Make device discoverable
//            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
//            discoverableIntent.putExtra(
//                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
//                300
//            ) // Duration in seconds
//            startActivity(discoverableIntent)
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            101 -> {
//                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                    setupBluetoothToggle()
//                } else {
//                    // Handle permission denial
//                }
//            }
//
//            102 -> {
//                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                    // Make device discoverable
//                    enableDiscoverability()
//                } else {
//                    // Handle permission denial
//                }
//            }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        try {
//            unregisterReceiver(bluetoothReceiver)
//            unregisterReceiver(discoverabilityReceiver)
//        } catch (e: IllegalArgumentException) {
//            e.printStackTrace()
//        }
//    }
//}
