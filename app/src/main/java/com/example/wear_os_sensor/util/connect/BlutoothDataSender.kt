package com.example.wear_os_sensor.util.connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class BluetoothDataSender: DataSender {

    private var bluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDvices: Set<BluetoothDevice>
    private lateinit var mBluetoothDevice: BluetoothDevice
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mOutputStream: OutputStream
    private var context:Context

    constructor(context:Context){
        this.context = context;
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager!!.adapter
    }

    @SuppressLint("MissingPermission")
    override fun searchDevice(): String {
        pairedDvices = mBluetoothAdapter.bondedDevices
        val connected = getParingBluetoothDevice(pairedDvices)
        if (connected == null) return "error"
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(connected.toString())
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid)
        }catch (e: IOException) {
            Log.e("Search Device", "Error", e)
            return "error"
        }
        return connected.name
    }

    @SuppressLint("MissingPermission")
    override fun connect(context: Context): String {

//        pairedDvices = mBluetoothAdapter.bondedDevices
//        val connected = getParingBluetoothDevice(pairedDvices)
//        if (connected.equals("error")) return "error"
//        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(connected)
//        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        try {
            if (mBluetoothSocket.isConnected()) Log.d("", "qweasd")
            Thread.sleep(100)
            mBluetoothSocket.connect()
            mOutputStream = mBluetoothSocket.outputStream
            return "Success"
        } catch (e: IOException) {
            Log.e("Bluetooth Connection", "Error opening socket", e)
            return "error123"
        }
    }

    @Throws(Exception::class)
    override fun sendData(data: String){

        val stringArray = data.split("[|]".toRegex())
            .filter { it.isNotEmpty() && it != "[.-]" }
        println(stringArray.size)
//        25개에 400바이트
        // 200개 3200바이트
        // 50개 800바이트
        // 5개당 80바이트
        // 60개 960바이트
        val byteBuffer = ByteBuffer.allocate(960)

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        stringArray.forEachIndexed { index, element ->
            if(index % 3 == 0){
                byteBuffer.putInt(element.toInt())
            }
            else if(index % 3 == 1){
                byteBuffer.putLong(element.toLong())
            }
            else if(index % 3 == 2){
                byteBuffer.putFloat(element.toFloat())
            }
        }
        val byteArray = byteBuffer.array()
        Log.i("wqe", byteArray.size.toString())
        mOutputStream.write(byteArray)

        if(!mBluetoothSocket.isConnected){
            Log.i("Bluetooth Connection", mBluetoothSocket.isConnected.toString())
            throw Exception()
        }
    }

    override fun disconnect() {
        try {
            mBluetoothSocket.close()
            Log.i("Bluetooth Connection", "Error closing socket")
        } catch (e: IOException) {
            Log.e("Bluetooth Connection", "Error closing socket", e)
        }
    }

    //페어링된 기기를 찾는 함수
    @SuppressLint("MissingPermission")
    private fun getParingBluetoothDevice(pairedDevice: Set<BluetoothDevice>): BluetoothDevice? {
        try {
            for (bluetoothDevice: BluetoothDevice in pairedDevice) {
                if (isConnected(bluetoothDevice) as Boolean) {
                    Log.d(bluetoothDevice.toString(), bluetoothDevice.name)
                    return bluetoothDevice
                }
            }
        } catch (e: IOException) {
            //블루투스 서비스 사용불가인 경우
        }
        return null
    }

    // 현재 연결되어있는지 유무
    private fun isConnected(device: BluetoothDevice): Any? {
        val m: Method = device.javaClass.getMethod("isConnected")
        val connected = m.invoke(device)
        return connected
    }
}