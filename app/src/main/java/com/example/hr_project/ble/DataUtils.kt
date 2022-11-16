package com.example.hr_project.ble

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object DataUtils {
    private const val FILE_PATH = "/data/data/com.example.hr_project/files"
    private const val TAG = "DataUtils"

    private var data_idx_head = 0
    private var data_idx_tail = 0
    private var dataList = mutableListOf<String>()
    private var file = mutableListOf<File>()

    fun get_data() = dataList
    fun get_data(i: Int) = dataList[i]
    fun get_file() = file

    fun get_datalength() = dataList.size
    fun get_dataidx(): Int {
        return data_idx_tail
    }

    @Synchronized
    fun set_data(data: String): Boolean {

        if(data.contains("FIN")) {
            dataList.add(data.substring(0, data.indexOf("FIN") - 1))
            return false
        }
        else {
            dataList.add(data)
            return true
        }
//        var split_idx = data.indexOf('@')
//        var split_idx2: Int
//        var this_data_length: Int
//
//        if(data_idx_tail != 0 && data_idx_head == 0)
//            data_idx_head = data_idx_tail
//
//        // data label
//        if (split_idx == 0) {
//            split_idx = data.indexOf('@', split_idx + 1)
//            data_idx_tail = data.substring(1, split_idx).toInt()
//
//            this_data_length = data.substring(split_idx + 1, data.indexOf('@', split_idx + 1)).toInt()
//        } else {
//            dataList.add("NaN")
//            return false
//        }
//        // data 정보
//        split_idx = data.indexOf('\n')
//        dataList.add(data.substring(split_idx+1, data.indexOf('@', split_idx) - 2))
//        if(dataList.last().count{ it =='\n' } != this_data_length-1)
//        {
//            return false
//            dataList.add("NaN")
//        }
//        split_idx = data.indexOf('@', split_idx)
//
//        if (data.lastIndexOf('@') != split_idx) {
//            set_data(data.substring(split_idx + 2))
//        }

        // Success!
        return true
    }

    @Synchronized
    fun create_file() {
        //파일 만들기
        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss")
        val file = File(FILE_PATH, "${dateFormat.format(Date(System.currentTimeMillis()))}_idx_${data_idx_head}_${data_idx_tail}.txt")
        data_idx_head = data_idx_tail
        val printWriter = PrintWriter(file)
        this.file.add(file)

        try {
            var count = 0
            val saveDataList = dataList
            dataList = mutableListOf()

            for (data in saveDataList) {
                printWriter.print(data)
                count++
                if (count % (saveDataList.size / 10) == 0) {
                    // 정확한 퍼센트 X
                    Log.i(TAG, "File Create : ${count / (saveDataList.size / 10)}0%")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Create file Error")
        } finally {
            printWriter.close()
        }
    }

}