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
import com.example.hr_project.PERMISSIONS
import com.example.hr_project.REQUEST_ALL_PERMISSION
import com.example.hr_project.enums.FileType
import kotlinx.coroutines.delay


@SuppressLint("MissingPermission")
object BleService {
    private val TAG = "BleService"
    private lateinit var bleGatt: BluetoothGatt
    private lateinit var bleRepository: BleRepository
    private lateinit var bleCallback:BluetoothCallback
    private var file: File? = null
    var totalFolder : Int = 0
    var totalFile : Int = 0
    var nowFolder : Int = 0
    var nowFile : Int = 0

    private val isConnected: Boolean
        get() = ::bleGatt.isInitialized

    suspend fun isbleUpdated(){
        while(!this::bleCallback.isInitialized)
            delay(1000)
        while (!bleCallback.isConnectionUpdated) {
            Log.d(TAG, "cnt wait...")
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
            delay(1000)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Closing Gatt connection")
        SFTP.isFinished = false
        nowFile = 0
        nowFolder = 0
        totalFile = 0
        totalFolder = 0

        if(this::bleCallback.isInitialized)
            bleCallback.isConnectionUpdated = false

        if (isConnected) {
            bleGatt.disconnect()
            bleGatt.close()
            File.files.forEach {
                it?.delete()
            }
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

        var success: Boolean = bleGatt.writeCharacteristic(cmdCharacteristic)

        // check the result
        while(!success){
            success = bleGatt.writeCharacteristic(cmdCharacteristic)
            Log.e(TAG, "Failed to write command")
        }
    }

    @Synchronized
    fun readFromBluetooth(characteristic: BluetoothGattCharacteristic) {
        var msg = characteristic.getStringValue(0)

        var index = msg.indexOf(FileType.FOLDER_CHAR)
        if(index != -1) {
            totalFolder = msg.substring(index+1,index+5).toInt()
            Log.i(TAG,"totalFolder : $totalFolder")
            msg = msg.substring(index+5)
        }

        index = msg.indexOf(FileType.FILE_CHAR)
        if(index != -1) {
            totalFile = msg.substring(index+1,index+5).toInt()
            msg = msg.substring(0,index)+msg.substring(index+5)
            nowFolder++
            nowFile = 0
        }
        index = msg.indexOf(FileType.FINISH_CHAR)

        if(index != -1) {
            if(index != 0)
                msg = msg.substring(0, index-1)
            else
                msg = msg.substring(1)
            read(msg)
            close()
            nowFile++
            SFTP.isFinished = true
        }
        else {
            index = msg.indexOf(FileType.NEXT_CHAR)

            if (index != -1) {
                read(msg.substring(0, index))
                close()
                nowFile++

                read(msg.substring(index + 1))
            } else {
                read(msg)
            }
        }
    }

    private fun read(msg: String) {
        var writeMsg = msg
        if(msg.isNotEmpty()) {
            try {
                if (file == null) {
                    file = File(FileType.startCharOf(msg[0]))
                    writeMsg = msg.substring(1)
                    Log.i(TAG, "create file ${file!!.getFileName()}")
                }
                file!!.write(writeMsg)
            } catch (exception: RuntimeException) {
                exception.printStackTrace()
                Log.i(TAG, msg)
                file = null
            }
        }
    }

    private fun close() {
        file!!.close()
        Log.i(TAG, "close file ${file!!.getFileName()}")
        file = null
    }

}