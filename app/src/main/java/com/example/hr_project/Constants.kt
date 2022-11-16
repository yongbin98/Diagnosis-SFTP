package com.example.hr_project

import android.Manifest


// used to identify adding bluetooth names
const val REQUEST_ENABLE_BT = 1
// used to request fine location permission
const val REQUEST_ALL_PERMISSION = 11
val PERMISSIONS = arrayOf(
    Manifest.permission.BLUETOOTH,
    Manifest.permission.BLUETOOTH_ADMIN,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_ADVERTISE,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.INTERNET,
    Manifest.permission.ACCESS_NETWORK_STATE
)

//사용자 BLE UUID Service/Rx/Tx
const val SERVICE_STRING = "0000fff0-0000-1000-8000-00805f9b34fb"
const val CHARACTERISTIC_COMMAND_STRING = "0000fff2-0000-1000-8000-00805f9b34fb"
const val CHARACTERISTIC_RESPONSE_STRING = "0000fff1-0000-1000-8000-00805f9b34fb"

// MAC Address
const val SERVICE_MAC_ADDRESS = "EB:1F:68:DE:E7:B1"
//BluetoothGattDescriptor 고정
const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"