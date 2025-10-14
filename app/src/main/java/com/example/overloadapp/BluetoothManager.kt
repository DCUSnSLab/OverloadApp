package com.example.overloadapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.example.overloadapp.DrivingActivity
import com.example.overloadapp.RegisteringActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BluetoothManager {

    // 하드코딩된 타겟 디바이스 이름(DB가 없어서 하드코딩으로 대체)
    private const val TARGET_DEVICE_NAME = "None" // "=BLTEST"

    // 블루투스 켜짐/꺼짐 상태를 나타내는 변수
    val isBluetoothEnabled = mutableStateOf(false)

    // 현재 블루투스 연결 상태를 나타내는 상태 변수 (앱 전체에서 공유)
    val isConnected = mutableStateOf(false)
    val connectedDeviceName = mutableStateOf<String?>(null)
    val pairedDevices = mutableStateOf<List<BluetoothDevice>>(emptyList())
    val isLoading = mutableStateOf(false)

    // 블루투스 소켓 및 스트림
    var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // 데이터 수신을 위한 코루틴 Job
    private var workerJob: Job? = null
    private val myUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // 초기화
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun initialize(context: Context) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true

        // **여기서 자동 연결 로직을 추가합니다.**
        if (isBluetoothEnabled.value) {
            connectToRememberedDevice(context)
        }
    }

    // 블루투스 연결
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(context: Context, device: BluetoothDevice) {
        if (bluetoothSocket?.isConnected == true) return // 이미 연결됨

        CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothSocket?.close()
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUuid)
                bluetoothSocket?.connect()

                withContext(Dispatchers.Main) {
                    inputStream = bluetoothSocket?.inputStream
                    outputStream = bluetoothSocket?.outputStream
                    beginListenForData()
                    isConnected.value = true
                    connectedDeviceName.value = device.name ?: device.address
                    Toast.makeText(context, "연결 성공: ${connectedDeviceName.value}", Toast.LENGTH_SHORT).show()

                    // 이미 MainActivity라면 이동하지 않음
                    if (context !is MainActivity) {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            putExtra("BLUETOOTH_DEVICE_NAME", connectedDeviceName.value)
                        }
                        context.startActivity(intent)
                        (context as? Activity)?.finish() // 현재 액티비티 종료 (예: RegisteringActivity)
                    } else {
                        // 이미 MainActivity라면 화면 이동 없이 상태만 갱신
                        Log.d("BluetoothManager", "이미 MainActivity에 있음 — 화면 이동 생략")
                    }
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    isConnected.value = false
                    connectedDeviceName.value = null
                    Toast.makeText(context, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 블루투스 연결 해제
    fun disconnectBluetooth() {
        // 코루틴 작업 취소
        workerJob?.cancel("Bluetooth disconnected")
        // 블루투스 소켓 및 스트림 정리
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // 로그로 오류 기록 (context 필요 없음)
            Log.e("Bluetooth", "Failed to close streams/socket: ${e.message}")
        }
        // 상태 변수 업데이트
        isConnected.value = false
        connectedDeviceName.value = null
    }

    // 데이터 수신 루프
    private fun beginListenForData() {
        workerJob = CoroutineScope(Dispatchers.IO).launch {
            val inStream = inputStream ?: throw IllegalStateException("inputStream is null")
            val buffer = ByteArray(1024)
            var leftover = ByteArray(0) // 이전에 읽은 데이터 중 아직 22바이트가 안 된 남은 부분

            while (isActive) {
                try {
                    val bytesRead = inStream.read(buffer)
                    if (bytesRead == -1) break

                    if (bytesRead > 0) {
                        val newData = leftover + buffer.copyOfRange(0, bytesRead)
                        var offset = 0
                        while (offset + 22 <= newData.size) {
                            val slice = newData.copyOfRange(offset, offset + 22)
                            val sensor = sliceData(slice)
                            withContext(Dispatchers.Main) {
                                // context가 필요하므로, 토스트 대신 로그로 대체
                                if (sensor != null) {
                                    Log.d("BluetoothManager", "Top Left: ${sensor.distTL}")
                                } else {
                                    Log.w("BluetoothManager", "잘못된 데이터 수신")
                                }
                            }
                            offset += 22
                        }
                        leftover = if (offset < newData.size) {
                            newData.copyOfRange(offset, newData.size)
                        } else {
                            ByteArray(0)
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        Log.e("BluetoothManager", "데이터 수신 중 오류: ${e.message ?: "IO 오류"}")
                        // 연결 해제 로직 호출
                        disconnectBluetooth()
                    }
                    break
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        Log.e("BluetoothManager", "수신 루프 예외: ${t.message ?: "오류"}")
                    }
                    break
                }
            }

            // 루프 종료 시 정리
            try { inStream.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            try { bluetoothSocket?.close() } catch (_: Exception) {}
            inputStream = null
            outputStream = null
            bluetoothSocket = null

            withContext(Dispatchers.Main) {
                Log.d("BluetoothManager", "수신 루프 종료")
            }
        }
    }

    private fun sliceData(data: ByteArray): SensorData? {
        if (data.size != 22) return null
        if (data[20] != 13.toByte() || data[21] != 10.toByte()) return null

        val sensorData = SensorData()

        sensorData.distTL = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
        sensorData.state = data[2].toInt() and 0xFF
        sensorData.distTR = ((data[4].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        sensorData.tempTR = data[5].toInt() and 0xFF
        sensorData.distBL = ((data[7].toInt() and 0xFF) shl 8) or (data[6].toInt() and 0xFF)
        sensorData.tempBL = data[8].toInt() and 0xFF
        sensorData.distBR = ((data[10].toInt() and 0xFF) shl 8) or (data[9].toInt() and 0xFF)
        sensorData.tempBR = data[11].toInt() and 0xFF
        sensorData.accelX = data[12]
        sensorData.accelY = data[13]
        sensorData.accelZ = data[14]
        sensorData.slope = ((data[16].toInt() and 0xFF) shl 8) or (data[15].toInt() and 0xFF)
        sensorData.tempIMU = data[17].toInt() and 0xFF
        sensorData.weight = ((data[19].toInt() and 0xFF) shl 8) or (data[18].toInt() and 0xFF)

        return sensorData
    }

    private class SensorData {
        var distTL: Int = 0
        var distTR: Int = 0
        var distBL: Int = 0
        var distBR: Int = 0
        var state: Int = 0
        var tempTR: Int = 0
        var tempBL: Int = 0
        var tempBR: Int = 0
        var accelX: Byte = 0
        var accelY: Byte = 0
        var accelZ: Byte = 0
        var slope: Int = 0
        var tempIMU: Int = 0
        var weight: Int = 0
    }

    // 새로운 함수 추가
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun loadPairedDevices(context: Context) {
        isLoading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                withContext(Dispatchers.Main) {
                    pairedDevices.value = list
                }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    Log.e("MyBluetoothManager", "블루투스 권한 부족으로 기기 목록을 불러올 수 없습니다.")
                    pairedDevices.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                }
            }
        }
    }

    // 새 메소드: 기억된 디바이스에 연결 시도
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToRememberedDevice(context: Context) {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.firstOrNull { it.name == TARGET_DEVICE_NAME }?.let { targetDevice ->
            connectToDevice(context, targetDevice)
        }
    }
}