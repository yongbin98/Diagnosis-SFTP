package com.example.hr_project.Activity

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.hr_project.*
import com.example.hr_project.ble.BleService
import com.example.hr_project.ble.BluetoothUtils
import com.example.hr_project.ble.File
import com.example.hr_project.ble.SFTP
import com.example.hr_project.enums.FileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {
    private val bleManager: BluetoothManager? by lazy(LazyThreadSafetyMode.NONE){
        this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bleAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        bleManager?.adapter
    }
    private val TAG = "Central"
    private var textview:TextView? = null
    private var statusview:TextView? = null
    private val myCoroutinescope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bleconnect)
        //connect button
        var startbtn = findViewById<Button>(R.id.StartScan)
        var stopbtn = findViewById<Button>(R.id.StopScan)
        textview = findViewById(R.id.textView)

        startbtn.setOnClickListener {
            textview?.text = "wait for connect"

            myCoroutinescope.launch {
                BleService.connect(this@MainActivity)
                statusview?.text = "Find Characteristic"
                BleService.isbleUpdated()
                statusview?.text = "Connected!!"
                BleService.write("SCS\n")

                val sftp = SFTP()
                sftp.bleFinished()
                sftp.connect()
                sftp.upload(File.files)
                sftp.disconnect()
                BleService.disconnect()
            }
        }
        stopbtn.setOnClickListener {
            // stop button click
            textview?.text = "Disconnect"
            BleService.disconnect()
        }
    }
}