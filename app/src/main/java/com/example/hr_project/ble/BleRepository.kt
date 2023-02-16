package com.example.hr_project.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.hr_project.*

@SuppressLint("MissingPermission")
class BleRepository(
    activity: Activity,
    private val deviceProcessor: (BluetoothDevice) -> Boolean
) {

    private val bleAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bleManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleManager.adapter
    }
    private val scanResults = mutableSetOf<BluetoothDevice>()
    private val bleScanCallback: ScanCallback = BLEScanCallback(this::addScanResult)
    private val TAG = "bleRepository"

    fun connectBLE() {
        scanResults.clear()

        //ble adapter check request ble enable
        if (!bleAdapter.isEnabled) {
            Log.e(TAG, "Scanning Failed : ble does not enabled")
            return
        }

        //check bluetooth name
        val filters: MutableList<ScanFilter> = ArrayList()
        Log.i(TAG,"scan device using MAC")
        Log.i(TAG,"find MAC : $SERVICE_MAC_ADDRESS")
        val scanFilter: ScanFilter = ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING))) // UUID로 검색
//            .setDeviceName("Rehab01")
            .setDeviceAddress(SERVICE_MAC_ADDRESS)
            .build()
        filters.add(scanFilter)
        val scanFilter2: ScanFilter = ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING))) // UUID로 검색
//            .setDeviceName("Rehab01")
            .setDeviceAddress(SERVICE_MAC_ADDRESS_2)
            .build()
        filters.add(scanFilter2)
        val scanFilter3: ScanFilter = ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING))) // UUID로 검색
//            .setDeviceName("Rehab01")
            .setDeviceAddress(SERVICE_MAC_ADDRESS_3)
            .build()
        filters.add(scanFilter3)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        bleAdapter.bluetoothLeScanner.startScan(filters, settings, bleScanCallback)
//        bleAdapter?.bluetoothLeScanner?.startScan(BLEScanCallback) //filtering X
    }

    private fun stopScan(){
        bleAdapter.bluetoothLeScanner.stopScan(bleScanCallback)
        Log.d(TAG, "BLE Stop!")
    }

    private fun addScanResult(result: ScanResult) {
        // get scanned device
        val device = result.device
        // get scanned device MAC address
        val deviceAddress = device.address

        // 중복 체크
        if (scanResults.contains(device))
            return

        scanResults.add(device)
        Log.i(TAG, "add scanned device: $deviceAddress")

        if (deviceProcessor(device))
            stopScan()
    }
}