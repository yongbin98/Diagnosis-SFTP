package com.example.hr_project.enums

import android.util.Log
import java.lang.RuntimeException

enum class FileType(val startChar: Char, val fileName: String) {
    ECG('E', "ECG"),
    PPG('P', "PPG"),
    IMU('I', "IMU");

    companion object {
        const val NEXT_CHAR = 'N'
        const val FINISH_CHAR = 'F'

        fun startCharOf(startChar: Char): FileType{
            return values().find { it.startChar == startChar }!! // ?: throw RuntimeException("Not Found File Type")
        }

    }
}