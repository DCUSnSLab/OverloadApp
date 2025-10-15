package com.example.overloadapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BluetoothManager {

    // 하드코딩된 타겟 디바이스 이름(DB가 없어서 하드코딩으로 대체)
    private const val TARGET_DEVICE_NAME = "=BLTEST"
    const val STATE_INITIALIZING  = 0
    const val STATE_SETTING_INITIAL_VALUES = 1
    const val STATE_WAITING_FOR_LOADING = 2
    const val STATE_LOADING_IN_PROGRESS = 3
    const val STATE_STABILIZING = 4
    const val STATE_ESTIMATING_WEIGHT = 5


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

    // 센서 데이터를 UI에 전달하는 StateFlow
    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

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
            val inStream = inputStream ?: return@launch
            val buffer = ByteArray(1024)
            var bytes: Int
            val packet = ByteArray(22)
            var packetPos = 0

            while (isActive) {
                try {
                    bytes = inStream.read(buffer)
                    if (bytes == -1) break

                    if (bytes > 0) {
                        for (i in 0 until bytes) {
                            val b = buffer[i]
                            if (b.toInt() == 13) {
                                if (packetPos == 21) {
                                    packet[20] = 13.toByte()
                                    packet[21] = 10.toByte()
                                    val sensor = sliceData(packet)
                                    if (sensor != null) {
                                        _sensorData.value = sensor // <-- 이 부분 수정: 데이터 방출
                                        Log.d("BluetoothManager", "--- 센서 데이터 수신 ---")
                                        Log.d("BluetoothManager", "Top Left 거리: ${sensor.distTL} mm")
                                        Log.d("BluetoothManager", "적재 상태: ${sensor.loadState} C")
                                        Log.d("BluetoothManager", "Top Right 거리: ${sensor.distTR} mm")
                                        Log.d("BluetoothManager", "Top Right 온도: ${sensor.tempTR} C")
                                        Log.d("BluetoothManager", "Bottom Left 거리: ${sensor.distBL} mm")
                                        Log.d("BluetoothManager", "Bottom Left 온도: ${sensor.tempBL} C")
                                        Log.d("BluetoothManager", "Bottom Right 거리: ${sensor.distBR} mm")
                                        Log.d("BluetoothManager", "Bottom Right 온도: ${sensor.tempBR} C")
                                        Log.d("BluetoothManager", "가속도(X, Y, Z): ${sensor.accelX}, ${sensor.accelY}, ${sensor.accelZ}")
                                        Log.d("BluetoothManager", "경사각: ${sensor.slope} 도")
                                        Log.d("BluetoothManager", "IMU 온도: ${sensor.tempIMU} C")
                                        Log.d("BluetoothManager", "추정 무게: ${sensor.weight} kg")
                                        Log.d("BluetoothManager", "---------------------")
                                    } else {
                                        Log.w("BluetoothManager", "잘못된 데이터 수신: 패킷 길이 또는 형식 오류")
                                    }
                                }
                                packetPos = 0
                            } else if (packetPos < 21) {
                                packet[packetPos++] = b
                            }
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

        // 수신한 데이터의 유효성 검사(길이 및 끝마침표 유무)
        if (data.size != 22) return null
        if (data[20] != 13.toByte() || data[21] != 10.toByte()) return null

        val sensorData = SensorData()

        sensorData.distTL = sensorData.u16(data[0], data[1])
        sensorData.loadState = sensorData.u8(data[2])   // 당신의 tempTL state로 대체되었다
        sensorData.distTR = sensorData.u16(data[3], data[4])
        sensorData.tempTR = sensorData.u8(data[5])
        sensorData.distBL = sensorData.u16(data[6], data[7])
        sensorData.tempBL = sensorData.u8(data[8])
        sensorData.distBR = sensorData.u16(data[9], data[10])
        sensorData.tempBR = sensorData.u8(data[11])
        sensorData.accelX = sensorData.u8(data[12])
        sensorData.accelY = sensorData.u8(data[13])
        sensorData.accelZ = sensorData.u8(data[14])
        sensorData.slope = sensorData.u16(data[15], data[16])
        sensorData.tempIMU = sensorData.u8(data[17])
        sensorData.weight = sensorData.u16(data[18], data[19])

        return sensorData
    }

    class SensorData {
        var distTL: Int = 0
        var loadState: Int = 0
        var distTR: Int = 0
        var tempTR: Int = 0
        var distBL: Int = 0
        var tempBL: Int = 0
        var distBR: Int = 0
        var tempBR: Int = 0
        var accelX: Int = 0
        var accelY: Int = 0
        var accelZ: Int = 0
        var slope: Int = 0
        var tempIMU: Int = 0
        var weight: Int = 0

        val u8 = { b: Byte -> b.toInt() and 0xFF }  // 단일 byte를 부호 없는 값으로 변환하는 헬퍼 함수
        val u16 = { low: Byte, high: Byte -> (u8(high) shl 8) or u8(low) }  // 2바이트 데이터를 부호 없는 16비트 정수로 변환 및 조합하는 헬퍼 함수
    }

    // 연결된 디바이스 목록 불러오기
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

    // 기억된 디바이스에 연결 시도
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToRememberedDevice(context: Context) {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.firstOrNull { it.name == TARGET_DEVICE_NAME }?.let { targetDevice ->
            connectToDevice(context, targetDevice)
        }
    }
}