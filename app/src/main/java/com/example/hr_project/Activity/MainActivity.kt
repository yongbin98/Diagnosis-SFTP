package com.example.hr_project.Activity

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.hr_project.*
import com.example.hr_project.ble.BluetoothUtils
import com.example.hr_project.ble.File
import com.example.hr_project.ble.SFTP
import com.example.hr_project.enums.FileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : Activity() {
    private val bleAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleManager.adapter
    }
    private val TAG = "Central"
    private var scanResults: java.util.ArrayList<BluetoothDevice>? = java.util.ArrayList()
    private var bleGatt: BluetoothGatt? = null
    private var statusTxt: String = ""
    private var textview:TextView? = null
    private var statusview:TextView? = null
    private var txtRead: String = "▷▷▷▷▷▷▷▷▷▷"
    private var Screen_Flag : Int = 0;
    private val myCoroutinescope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bleconnect)
        //connect button
        var serverbtn = findViewById<Button>(R.id.server)
        var startbtn = findViewById<Button>(R.id.StartScan)
        var stopbtn = findViewById<Button>(R.id.StopScan)
        var receivebtn = findViewById<Button>(R.id.receive)
        var editview = findViewById<EditText>(R.id.name)
        textview = findViewById<TextView>(R.id.textView)
        statusview = findViewById<TextView>(R.id.statusTxt)

        /**
         * 저장 용량 체크
         * */
//        val storageManager = applicationContext.getSystemService<StorageManager>()!!
//        val appSpecificInternalDirUuid: UUID = storageManager.getUuidForPath(filesDir)
//        val availableBytes: Long = storageManager.getAllocatableBytes(appSpecificInternalDirUuid)
//        Log.i(TAG, "availableBytes : $availableBytes")

        // ble permission check
        while(true) {
            if (!hasPermissions(this, PERMISSIONS)) {
                requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "get Permission", Toast.LENGTH_SHORT).show()
                break
            }
        }

        // ble adapter check , request ble enable
        if (bleAdapter == null || !bleAdapter?.isEnabled!!) {
            requestEnableBLE()
            Toast.makeText(this, "Scanning Failed : ble does not enabled", Toast.LENGTH_SHORT).show()
            return
        }

        startbtn.setOnClickListener(){
            // start button click
            //scan filter setting
            var filters: MutableList<ScanFilter> = ArrayList()
            if(editview.text.toString() == "Write bluetooth name" || editview.text.toString() == "")
            {
                textview?.setText("scan device using MAC")
                statusview?.setText("find MAC : $SERVICE_MAC_ADDRESS")
                val scanFilter: ScanFilter = ScanFilter.Builder()
//                    .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING))) // UUID로 검색
                    .setDeviceAddress(SERVICE_MAC_ADDRESS)
                    .build()
                filters.add(scanFilter)
            }
            else{
                textview?.setText("scan device using ${editview.text.toString()}")
                statusview?.setText("find name : ${editview.text.toString()}")
                val scanFilter: ScanFilter = ScanFilter.Builder()
//                  .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING))) // UUID로 검색
                    .setDeviceName(editview.text.toString())
                    .build()
                filters.add(scanFilter)
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                textview?.setText("Permission Denied..")
                requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
            }
            else {
                bleAdapter?.bluetoothLeScanner?.startScan(filters, settings, BLEScanCallback)
//                bleAdapter?.bluetoothLeScanner?.startScan(BLEScanCallback) //filtering X
            }
        }
        stopbtn.setOnClickListener(){
            // stop button click
            textview?.setText("stop scan & bluetooth")
            statusview?.setText("Disconnected")
            stopScan()
            disconnectGattServer()
        }

        receivebtn.setOnClickListener(){
            // file button click
            Screen_Flag = 0;
            write("SCS\n");
            textview?.setText("receive data & make files")
            statusview?.setText("Receiving data")
        }

        serverbtn.setOnClickListener(){
            // server button click
            stopScan()
            disconnectGattServer()

            val files = File.files
            File.files = mutableListOf()

            if(files.isEmpty()) {
                textview?.setText("file is empty.")
            }
            else{
                textview?.setText("file is detected.\nconnect to the server")
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    textview?.setText("FIFO Permission not granted")
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
                }
                else{
                    val sftp:SFTP = SFTP()
                    // SFTP 순서대로 연결하는 쓰레드
                    myCoroutinescope.launch {
                        val job1 = myCoroutinescope.async {
                            statusview?.setText("initialize setting")
                            sftp.connect()
                        }
                        job1.await()

                        val job2 = myCoroutinescope.async {
                            statusview?.setText("upload to server")

                            val dir = "/SFTP_folder"
                            val head_name = files.first().name.substring(0,files.first().name.toString().lastIndexOf('-'))
                            val tail_name = files.last().name.substring(0,files.last().name.toString().lastIndexOf('-'))
                            val full_name = "$head_name - $tail_name"
                            sftp.mkdir(dir,full_name)
                            sftp.upload("$dir"+"/"+"$full_name", files)

                            // TODO("delete local files")
                        }
                        job2.await()

                        val job3 = myCoroutinescope.async {
                            statusview?.setText("upload is over")
                            sftp.disconnect()
                        }
                    }
                }
            }
        }

    }

    /**
     * Request BLE enable
     */
    private fun requestEnableBLE() {
        val bleEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            textview?.setText("requestEnableBLE Denied..")
            return
        }
        startActivityForResult(bleEnableIntent, REQUEST_ENABLE_BT)
    }

    /**
     * Permission check
     */
    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null){
            for (permission in permissions) {
                if(ActivityCompat.checkSelfPermission(context,permission)
                != PackageManager.PERMISSION_GRANTED){
                    return false
                }
            }
        }
        return true
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(this, "Permissions must be granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * BLE Scan Callback
     */
    private val BLEScanCallback: ScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                textview?.setText("Connect denied")
                return
            }
            Log.i(TAG, "Remote device name: " + result.device.name)
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(_error: Int) {
            Log.e(TAG, "BLE scan failed with code $_error")
        }

        /**
         * Add scan result
         */
        private fun addScanResult(result: ScanResult) {
            // get scanned device
            val device = result.device
            // get scanned device MAC address
            val deviceAddress = device.address
            val deviceName = device.name

            // 중복 체크
            for (dev in scanResults!!) {
                if (dev.address == deviceAddress) return
            }
            // add arrayList
            scanResults?.add(result.device)
            // status text UI update
            statusTxt = "add scanned device: $deviceAddress"

            statusview?.setText("$statusTxt")

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                textview?.setText("Connect denied")
                return
            }
            stopScan()
            bleGatt = device?.connectGatt(this@MainActivity, true, gattClientCallback)
        }
    }

    /**
      * Scan stop
      */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScan(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            textview?.setText("stopscan Denied..")
            return
        }
        bleAdapter?.bluetoothLeScanner?.stopScan(BLEScanCallback)
        //isScanning.set(false) //스캔 중지(버튼 활성화)
        //btnScanTxt.set("Start Scan") //button text update
        scanResults = ArrayList() //list 초기화
        Log.d(TAG, "BLE Stop!")
    }
    /**
     * BLE gattClientCallback
     */
    private val gattClientCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if( status == BluetoothGatt.GATT_FAILURE ) {
                disconnectGattServer()
                return
            } else if( status != BluetoothGatt.GATT_SUCCESS ) {
                disconnectGattServer()
                return
            }
            if( newState == BluetoothProfile.STATE_CONNECTED ) {
                // update the connection status message

                statusview?.setText("Connected")
                Log.d(TAG, "Connected to the GATT server")
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    textview?.setText("connect permission denied")
                    return
                }
                gatt.discoverServices()
            } else if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                disconnectGattServer()
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            // check if the discovery failed
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery failed, status: $status")
                return
            }

            // log for successful discovery
            Log.d(TAG, "Services discovery is successful")

            // find command characteristics from the GATT server
            val respCharacteristic = gatt?.let { BluetoothUtils.findResponseCharacteristic(it) }

            /**
             * BLE UUID 직접 찾기
             */
//            val services = gatt?.services
//            if(services != null) {
//                for (service in services) {
//                    val characteristics = service?.characteristics
//
//                    if(characteristics != null){
//                        for (characteristic in characteristics) {
//                            if(characteristic.properties == BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE )
//                                Log.d(TAG, "WRITE Response : ${characteristic.uuid}")
//                            else if(characteristic.properties == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
//                                Log.d(TAG, "NOTIFY : ${characteristic.uuid}")
//                            else
//                                Log.e(TAG, "ERROR : ${characteristic.uuid}")
//                        }
//                    }
//                }
//            }

            // disconnect if the characteristic is not found
            if( respCharacteristic == null ) {
                Log.e(TAG, "Unable to find cmd characteristic")
                disconnectGattServer()
                return
            }
            else
                Log.d(TAG, "find cmd characteristic successfully")

            // sending Success

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                textview?.setText("connect permission denied")
                return
            }
            // UUID 알람 설정
            gatt.setCharacteristicNotification(respCharacteristic, true)
            Log.d(TAG,"UUID Alarm on")

            // UUID for notification
            val descriptor:BluetoothGattDescriptor = respCharacteristic.getDescriptor(
                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
            )
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            Log.d(TAG,"${descriptor.value}")
            gatt.writeDescriptor(descriptor)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
//            Log.d(TAG, "characteristic changed: " + characteristic.uuid.toString())
            readCharacteristic(characteristic)
        }

        @Synchronized
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            } else {
                Log.e(TAG, "Characteristic write unsuccessful, status: $status")
                disconnectGattServer()
            }
        }

        @Synchronized
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully")
                readCharacteristic(characteristic)
            } else {
                Log.e(TAG, "Characteristic read unsuccessful, status: $status")
                // Trying to read from the Time Characteristic? It doesnt have the property or permissions
                // set to allow this. Normally this would be an error and you would want to:
                // disconnectGattServer();
            }
        }

        /**
         * Log the value of the characteristic
         * @param characteristic
         */
        // Bluetooth 통신 읽기
        var file: File? = null

        @Synchronized
        private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
            var msg = characteristic.getStringValue(0)

            while (true) {
                val index = msg.indexOf(FileType.FINISH_CHAR)

                if (index != -1) {
                    write(msg.substring(0, index))
                    close()

                    msg = msg.substring(index + 1)
                } else {
                    write(msg)
                    break
                }
            }
        }

        private fun write(msg: String) {
            try {
                var writeMsg = msg

                if (file == null) {
                    file = File(FileType.startCharOf(msg[0]))
                    writeMsg = msg.substring(1)

                    Log.i(TAG, "create file ${file!!.getFileName()}")
                }

                file!!.write(writeMsg)
                textview?.setText("collect\n" + StringBuilder(txtRead).also { it.setCharAt((((file!!.getFileLength()%1000)/100).toInt()),'▶')}.toString())
            } catch (exception: RuntimeException) {
                exception.printStackTrace()
                file = null
            }
        }

        private fun close() {
            file!!.close()
            Log.i(TAG, "close file ${file!!.getFileName()}")
            file = null
        }
    }

    /**
     * Disconnect Gatt Server
     */
    fun disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection")
        // disconnect and close the gatt
        if (bleGatt != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                statusview?.setText("connect permission denied")
                return
            }
            bleGatt!!.disconnect()
            bleGatt!!.close()
        }
    }

    private fun write(write_txt : String){
        val cmdCharacteristic = BluetoothUtils.findCommandCharacteristic(bleGatt!!)
        // disconnect if the characteristic is not found
        if (cmdCharacteristic == null) {
            Log.e(TAG, "Unable to find cmd characteristic")
            disconnectGattServer()
            return
        }
        cmdCharacteristic.setValue(write_txt)

        val success: Boolean = bleGatt!!.writeCharacteristic(cmdCharacteristic)

        // check the result
        if( !success ) {
            Log.e(TAG, "Failed to write command")
        }
    }

    override fun onResume() {
        super.onResume()
        /**
         * finish app if the BLE is not supported
         * */
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }


}