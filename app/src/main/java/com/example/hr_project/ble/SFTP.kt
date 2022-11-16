package com.example.hr_project.ble

import android.util.Log
import com.jcraft.jsch.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException
import java.util.*


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

    // 생성자
    init{
        var jSch : JSch = JSch()
        try {
            if(privateKey!=null){
                jSch.addIdentity(privateKey)
            }
            session = jSch.getSession(userName,host,port)
            if(privateKey==null){
                session?.setPassword(password)
            }
            var config : Properties = Properties()
            config.put("StrictHostKeyChecking","no")
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

    // SFTP 서버 연결
    fun connect() {
        Log.i(TAG,"connecting....")
        session?.connect()

        channel = session?.openChannel("sftp") as ChannelSftp
        channel?.connect()
        Log.i(TAG,"connected")
    }

    // 디렉토리 생성
    fun mkdir(dir:String,mkdirName:String)
    {
        Log.i(TAG,"$dir , $mkdirName")
        try {
            channel?.cd(dir)
            channel?.mkdir(mkdirName)
        } catch (e: SftpException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    //파일 업로드
    fun upload(dir:String, files:MutableList<File>) {
        Log.i(TAG,"uploading....")
        var filein: FileInputStream? = null
        try {
            channel?.cd(dir)
            for (file in files)
            {
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

    fun disconnect(){
        channel?.quit()
        session?.disconnect()
    }
}