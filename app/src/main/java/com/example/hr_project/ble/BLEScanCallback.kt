package com.example.hr_project.ble

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log

class BLEScanCallback(
    private val processor: (ScanResult) -> Unit
) : ScanCallback() {
    private val TAG = "ScanCallback"

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        super.onScanResult(callbackType, result)
        processor(result)
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        results.forEach(processor)
    }

    override fun onScanFailed(_error: Int) {
        Log.e(TAG, "BLE scan failed with code $_error")
    }
}