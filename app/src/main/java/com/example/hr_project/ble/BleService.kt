package com.example.hr_project.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import com.example.hr_project.Activity.MainActivity
import com.example.hr_project.enums.FileType
import kotlinx.coroutines.delay


@SuppressLint("MissingPermission")
object BleService {
    private val TAG = "BleService"
    private lateinit var bleGatt: BluetoothGatt
    private lateinit var bleRepository: BleRepository
    private lateinit var bleCallback:BluetoothCallback
    private var file: File? = null

    private val isConnected: Boolean
        get() = ::bleGatt.isInitialized

    suspend fun isbleUpdated(){
        while(!bleCallback.isConnectionUpdated) {
            Log.d(TAG,"cnt wait...")
            delay(1000)
        }
        delay(1000)
    }

    suspend fun connect(activity: Activity) {
        bleRepository = BleRepository(activity) { device ->
            Log.i(TAG, "connected")
            bleCallback = BluetoothCallback(this::readFromBluetooth)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                bleGatt = device.connectGatt(activity,false, bleCallback, BluetoothDevice.TRANSPORT_LE)
            }else{
                bleGatt = device.connectGatt(activity,false, bleCallback)
            }
            true
        }
        bleRepository.connectBLE()

        while (!isConnected) {
            Log.d(TAG,"not Connected")
            delay(1000)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Closing Gatt connection")
        SFTP.isFinished = false
        if(this::bleCallback.isInitialized)
            bleCallback.isConnectionUpdated = false

        if (isConnected) {
            bleGatt.disconnect()
            bleGatt.close()
            file?.delete()
        }
    }

    fun write(write_txt: String) {
        if (!isConnected) {
            Log.d(TAG, "not connected")
            return
        }

        val cmdCharacteristic = BluetoothUtils.findCommandCharacteristic(bleGatt)
        // disconnect if the characteristic is not found
        if (cmdCharacteristic == null) {
            Log.e(TAG, "Unable to find cmd characteristic")
            return
        }
        cmdCharacteristic.setValue(write_txt)

        val success: Boolean = bleGatt.writeCharacteristic(cmdCharacteristic)

        // check the result
        while(!success){
            Log.e(TAG, "Failed to write command")
        }
    }

    @Synchronized
    fun readFromBluetooth(characteristic: BluetoothGattCharacteristic) {
        var msg = characteristic.getStringValue(0)
        Log.i(TAG,msg)

        var index = msg.indexOf(FileType.FINISH_CHAR)

        if(index != -1) {
            msg = msg.substring(0, index-1)
            read(msg)
            close()
            SFTP.isFinished = true
        }
        else {
            index = msg.indexOf(FileType.NEXT_CHAR)

            if (index != -1) {
                read(msg.substring(0, index))
                close()

                read(msg.substring(index + 1))
            } else {
                read(msg)
            }
        }

    }

    private fun read(msg: String) {
        var writeMsg = msg
        try {
            if (file == null) {
                file = File(FileType.startCharOf(msg[0]))
                writeMsg = msg.substring(1)
                Log.i(TAG, "create file ${file!!.getFileName()}")
            }
            file!!.write(writeMsg)
        } catch(exception: RuntimeException){
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