package com.example.hr_project.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.util.Log
import com.example.hr_project.CLIENT_CHARACTERISTIC_CONFIG
import java.util.*


@SuppressLint("MissingPermission")
class BluetoothCallback(
    private val reader: (BluetoothGattCharacteristic) -> Unit
) : BluetoothGattCallback() {
    private val TAG = "BluetoothCallback"
    var isConnectionUpdated : Boolean = false

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (status == BluetoothGatt.GATT_FAILURE) {
            disconnectGattServer(gatt)
            return
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            disconnectGattServer(gatt)
            return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // update the connection status message

            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            disconnectGattServer(gatt)
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
        if (respCharacteristic == null) {
            Log.e(TAG, "Unable to find cmd characteristic")
            disconnectGattServer(gatt)
            return
        } else
            Log.d(TAG, "find cmd characteristic successfully")

        // UUID 알람 설정
        gatt.setCharacteristicNotification(respCharacteristic, true)
        Log.d(TAG, "UUID Alarm on")

        // UUID for notification
        val descriptor: BluetoothGattDescriptor = respCharacteristic.getDescriptor(
            UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
        )
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        Log.d(TAG, "${descriptor.value}")
        isConnectionUpdated = gatt.writeDescriptor(descriptor)
        Log.i(TAG,"is connection update? : $isConnectionUpdated")
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
//            Log.d(TAG, "characteristic changed: " + characteristic.uuid.toString())
        reader(characteristic)
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
            disconnectGattServer(gatt)
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
            reader(characteristic)
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


    /**
     * Disconnect Gatt Server
     */
    private fun disconnectGattServer(bleGatt: BluetoothGatt?) {
        Log.d(TAG, "Closing Gatt connection")

        bleGatt?.let {
            isConnectionUpdated = false
            bleGatt.disconnect()
            bleGatt.close()
        }
    }
}