package com.example.hr_project.Activity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.hr_project.PERMISSIONS
import com.example.hr_project.R
import com.example.hr_project.REQUEST_ALL_PERMISSION
import com.example.hr_project.ble.BleService
import com.example.hr_project.ble.File
import com.example.hr_project.ble.SFTP
import com.example.hr_project.databinding.BleconnectBinding
import kotlinx.coroutines.*
import java.util.jar.Manifest
import kotlin.math.floor

class MainActivity : AppCompatActivity() {
    private val TAG = "Central"
    private val myCoroutinescope = CoroutineScope(Dispatchers.IO)
    private lateinit var job : Job
    private lateinit var binding: BleconnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BleconnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        while(true) {
            if (hasPermissions(this, PERMISSIONS)) {
                requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
                Toast.makeText(this, "request Permissions", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "get Permissions", Toast.LENGTH_SHORT).show()
                break
            }
        }

        binding.StartScan.setOnClickListener {
            showProgress()
            job = myCoroutinescope.launch {
                binding.textView.text = "Start scanning the device"
                BleService.connect(this@MainActivity)
                binding.textView.text = "Wait for connection"
                BleService.isbleUpdated()
                binding.textView.text = "Connected!!"
                BleService.write("SCS\n")

                val sftp = SFTP()
                binding.textView.text = "data read from the device\n"
                bleFinished()
                binding.textView.text = "All data has been read\nstart connecting to server"
                sftp.connect()
                binding.textView.text = "start uploading to server"
                sftp.upload(File.files)
                binding.textView.text = "files are uploaded"
                sftp.disconnect()
                BleService.disconnect()

            }
        }
        binding.StopScan.setOnClickListener {
            // stop button click
            binding.textView.text = "Disconnect button pressed"
            BleService.disconnect()
            if(this::job.isInitialized)
                job.cancel()
        }
    }

    private suspend fun bleFinished(){
        while(!SFTP.isFinished){
            binding.textView.text = "(now : ${BleService.nowFolder}/ total : ${BleService.totalFolder}) step in progress"
            showProgress()
            delay(1000)
        }
        binding.progressBar.progress = floor(((BleService.nowFile.toFloat()) / (BleService.totalFile.toFloat()))*100).toInt()
    }
    private fun showProgress(){
        if(BleService.nowFile != 0 || BleService.totalFile != 0) {
            binding.progressBar.progress = floor(((BleService.nowFile.toFloat()) / (BleService.totalFile.toFloat()))*100).toInt()
        }
        else
            binding.progressBar.progress = 0
    }


    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null){
            for (permission in permissions) {
                if(ActivityCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED){
                    binding.textView.text = "Permission denied"
                    return true
                }
            }
        }
        else {
            binding.textView.text = "permission version error!!"
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        BleService.disconnect()
    }

}