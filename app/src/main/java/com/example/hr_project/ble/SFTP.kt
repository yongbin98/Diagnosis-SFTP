package com.example.hr_project.ble

import android.annotation.SuppressLint
import android.util.Log
import com.jcraft.jsch.*
import kotlinx.coroutines.delay
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException
import java.util.*

@SuppressLint("MissingPermission")
class SFTP {
    //SFTP 설정
    private val TAG = "SFTP"
    private var session : Session? = null
    private var channel : ChannelSftp? = null
    private val host = "aiwm.i234.me"
    private val userName = "bami01"
    private val password = "69706970"
    private val port = 7140
    private val privateKey = null
    companion object{
        var isFinished:Boolean = false
    }

    // 생성자
    init{
        var jSch : JSch = JSch()
        try {
            session = jSch.getSession(userName,host,port)
            session?.setPassword(password)
            var config : Properties = Properties()
            config["StrictHostKeyChecking"] = "no"
            session?.setConfig(config)
        } catch (e: FileNotFoundException){
            // 파일 존재 x
            e.printStackTrace()
        } catch (e: UnknownHostException){
            // 원격 호스트 유효 X
            e.printStackTrace()
        } catch (e : JSchException){
            // username 또는 password 일치 X
            e.printStackTrace()
        } catch (e: SftpException) {
            // FIFO 권한 X일때 처리
            e.printStackTrace()
        } catch (e: Exception){
            e.printStackTrace()
        } finally {
            session?.disconnect()
        }
    }

    suspend fun bleFinished(){
        while(!isFinished){
            Log.i(TAG,"Wait data fifo")
            delay(1000)
        }
    }
    // SFTP 서버 연결
    fun connect() {
        Log.i(TAG,"connecting....")
        session?.connect()

        channel = session?.openChannel("sftp") as ChannelSftp
        channel?.connect()
        Log.i(TAG,"connected")
    }

    // 디렉토리 생성
    private fun mkdir(dir:String)
    {
        val directory = dir.substring(0,dir.lastIndexOf('/'))
        val mkdirName = dir.substring(dir.lastIndexOf('/')+1)
        Log.i(TAG,"$directory , $mkdirName")
        try {
            channel?.cd(directory)
            channel?.mkdir(mkdirName)
        } catch (e: SftpException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    //파일 업로드
    fun upload(files:MutableList<java.io.File>) {
        val dir = getName(files)
        mkdir(dir)
        Log.i(TAG,"uploading....")
        var filein: FileInputStream? = null
        try {
            channel?.cd(dir)
            for (file in files)
            {
                Log.i(TAG,file.name)
                filein = FileInputStream(file)
                channel?.put(filein, file.name)
            }
            Log.i(TAG,"upload successfully!")
        } catch (e: SftpException) {
            //directory error
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                filein?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    private fun getName(files : MutableList<java.io.File>): String {
        val dir = "/SFTP_folder"
        val head_name = files.first().name.substring(0,files.first().name.toString().lastIndexOf('-'))
        val tail_name = files.last().name.substring(files.last().name.toString().indexOf(' '),files.last().name.toString().lastIndexOf('-'))
        val full_name = "$head_name - $tail_name"
        return "$dir/$full_name"
    }

    fun disconnect(){
        channel?.quit()
        session?.disconnect()
    }
}