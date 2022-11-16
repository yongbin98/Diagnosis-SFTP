package com.example.hr_project.enums

import java.lang.RuntimeException

enum class FileType(val startChar: Char, val fileName: String) {
    ECG('E', "ECG"),
    PPG('P', "PPG"),
    IMU('I', "IMU");

    companion object {
        const val FINISH_CHAR = 'F'

        fun startCharOf(startChar: Char): FileType =
            values().find { it.startChar == startChar } ?: throw RuntimeException("Not Found File Type")
    }
}