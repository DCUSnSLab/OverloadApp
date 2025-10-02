package com.example.overloadapp

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.overloadapp.ui.theme.OverloadAppTheme
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

class DebuggingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OverloadAppTheme {
                DebuggingScreen()
            }
        }
    }
}

// 상태와 로직을 관리하는 ViewModel
class DebuggingViewModel {
    val connectionStatus = mutableStateOf("연결 안됨")
    val connectionStatusColor = mutableStateOf(Color.Red)
    val state = mutableStateOf("--")
    val weight = mutableStateOf("-- kg")
    val isDataReceivingEnabled = mutableStateOf(false)
    val isDriving = mutableStateOf(false)
    val connectButtonText = mutableStateOf("연결")
    val loadingButtonText = mutableStateOf("적재 시작")
    val drivingButtonText = mutableStateOf("운행 시작")
    val resetButtonEnabled = mutableStateOf(false)
    val drivingButtonEnabled = mutableStateOf(false)
    val loadingButtonEnabled = mutableStateOf(false)
    // DebuggingViewModel 클래스에 추가할 상태 변수
    val showDeviceDialog = mutableStateOf(false)
    var pairedDevicesList by mutableStateOf<List<BluetoothDevice>>(emptyList())

    private val myUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var workerJob: Job? = null



    // 초기화
    fun initialize(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(context: Context, device: BluetoothDevice) {
        Toast.makeText(context, "연결 시도: ${device.name}", Toast.LENGTH_SHORT).show()

        workerJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothSocket?.close()
            } catch (_: Exception) {}

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUuid)
                bluetoothSocket?.connect()

                withContext(Dispatchers.Main) {
                    inputStream = bluetoothSocket?.inputStream
                    outputStream = bluetoothSocket?.outputStream
                    beginListenForData()
                    updateUiOnConnect(device.name)
                    Toast.makeText(context, "연결 완료.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectBluetooth(context: Context) {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(context, "페어링된 기기가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        // Compose에서 AlertDialog를 띄우도록 수정해야 함. (여기서는 토스트로 대체)
        val deviceToConnect = pairedDevices.first() // 첫 번째 페어링된 기기로 연결 시도

        Toast.makeText(context, "연결시도: ${deviceToConnect.name}", Toast.LENGTH_SHORT).show()

        workerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothSocket?.close()
                bluetoothSocket = deviceToConnect.createRfcommSocketToServiceRecord(myUuid)
                bluetoothSocket?.connect()

                withContext(Dispatchers.Main) {
                    inputStream = bluetoothSocket?.inputStream
                    outputStream = bluetoothSocket?.outputStream
                    beginListenForData()
                    updateUiOnConnect(deviceToConnect.name)
                    Toast.makeText(context, "연결완료.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun disconnectBluetooth() {
        workerJob?.cancel("Bluetooth disconnected")
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Failed to close streams/socket: ${e.message}")
        }
        updateUiOnDisconnect()
    }

    // 데이터 수신 시작
    fun startDataReceiving() {
        if (bluetoothSocket?.isConnected == true) {
            isDataReceivingEnabled.value = true
            loadingButtonText.value = "적재 중지"
            sendCommand("LOADING_START")
        } else {
            Toast.makeText(null, "블루투스가 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 데이터 수신 중지
    fun stopDataReceiving() {
        isDataReceivingEnabled.value = false
        loadingButtonText.value = "적재 시작"
        sendCommand("LOADING_STOP")
        weight.value = "-- kg"
        state.value = "--"
    }

    // 운행 시작
    fun startDriving() {
        isDriving.value = true
        drivingButtonText.value = "운행 중지"
        sendCommand("START")
    }

    // 운행 중지
    fun stopDriving() {
        isDriving.value = false
        drivingButtonText.value = "운행 시작"
        sendCommand("STOP")
    }

    // 초기값 재설정
    fun resetInitialValues() {
        sendCommand("RESET_INIT")
        resetButtonEnabled.value = false
        // resetButtonText.value = "재설정 중..." // Compose에서 Button에 직접 Text를 넣음
    }

    private fun sendCommand(command: String) {
        if (bluetoothSocket?.isConnected == true && outputStream != null) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    outputStream?.write(("$command\n").toByteArray())
                    outputStream?.flush()
                } catch (e: IOException) {
                    // UI 스레드에서 Toast 메시지 표시
                    withContext(Dispatchers.Main) {
                        // Toast.makeText(null, "명령 전송 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Toast.makeText(null, "블루투스가 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun beginListenForData() {
        workerJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val readBuffer = ByteArray(1024)
            var readBufferPosition = 0
            while (isActive) { // 코루틴이 활성화된 동안 루프 실행
                try {
                    val available = inputStream?.available() ?: 0
                    if (available > 0) {
                        val packetBytes = ByteArray(available)
                        inputStream?.read(packetBytes)

                        for (i in 0 until available) {
                            val b = packetBytes[i]
                            if (b.toInt() == 10) { // LF
                                if (readBufferPosition > 0 && readBuffer[readBufferPosition - 1].toInt() == 13) {
                                    if (readBufferPosition > 20) {
                                        val dataBytes = readBuffer.copyOfRange(0, readBufferPosition)
                                        if (isDataReceivingEnabled.value) {
                                            val pair = parseStateAndWeight(dataBytes)
                                            if (pair != null) {
                                                withContext(Dispatchers.Main) { updateStateAndWeight(pair.first, pair.second) }
                                            }
                                        }
                                    }
                                }
                                readBufferPosition = 0
                            } else {
                                if (readBufferPosition < 1024) {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(null, "데이터 수신 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        disconnectBluetooth()
                    }
                    this.cancel()
                }
            }
        }
    }

    private fun parseStateAndWeight(data: ByteArray): Pair<Int, Int>? {
        if (data.size < 22) return null
        val u8 = { b: Byte -> b.toInt() and 0xFF }
        val u16 = { lowIdx: Int, highIdx: Int -> (u8(data[highIdx]) shl 8) or u8(data[lowIdx]) }
        val state = u8(data[2])
        val weight = u16(18, 19)
        return Pair(state, weight)
    }

    fun updateStateAndWeight(state: Int, weightValue: Int) {
        this.state.value = getStateString(state) // ViewModel의 상태 변수를 업데이트
        this.weight.value = "$weightValue kg"
    }

    private fun getStateString(state: Int): String {
        return when (state) {
            0 -> "초기화"
            1 -> "초기값 설정 중"
            2 -> "적재 대기 중"
            3 -> "적재 중"
            4 -> "안정화중"
            5 -> "무게 추정 중"
            else -> "알 수 없음"
        }
    }

    private fun updateUiOnConnect(deviceName: String) {
        connectionStatus.value = "연결됨: $deviceName"
        connectionStatusColor.value = Color.Green
        connectButtonText.value = "연결 해제"
        loadingButtonEnabled.value = true
        drivingButtonEnabled.value = true
        resetButtonEnabled.value = true
    }

    private fun updateUiOnDisconnect() {
        connectionStatus.value = "연결 안됨"
        connectionStatusColor.value = Color.Red
        connectButtonText.value = "연결"
        loadingButtonEnabled.value = false
        drivingButtonEnabled.value = false
        resetButtonEnabled.value = false
        loadingButtonText.value = "적재 시작"
        drivingButtonText.value = "운행 시작"
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun toggleConnection(context: Context) {
        if (bluetoothSocket?.isConnected == true) {
            disconnectBluetooth()
        } else {
            // 연결 다이얼로그를 띄우도록 상태 변경
            showPairedDevicesDialog(context)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun showPairedDevicesDialog(context: Context) {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(context, "페어링된 기기가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        // 상태 변수에 페어링된 기기 목록 저장
        pairedDevicesList = pairedDevices.toList()
        // 다이얼로그 표시 상태를 true로 변경
        showDeviceDialog.value = true
    }

}

// 다이얼로그를 Composable로 따로 분리
@Composable
fun DeviceSelectionDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("블루투스 기기 선택") },
        text = {
            Column(
                modifier = Modifier.wrapContentHeight()
            ) {
                devices.forEach { device ->
                    Text(
                        text = "Unknown\n${device.address}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDeviceSelected(device)
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebuggingScreen() {
    val context = LocalContext.current
    val viewModel = remember { DebuggingViewModel() }
    var showPermissionInfoDialog by remember { mutableStateOf(false) }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.initialize(context)
        } else {
            Toast.makeText(context, "블루투스 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    // 초기 권한 요청
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        } else {
            viewModel.initialize(context)
        }
    }

    // 생명주기에 따라 연결 해제
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectBluetooth()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("블루투스 디버깅") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "연결 상태: ${viewModel.connectionStatus.value}",
                color = viewModel.connectionStatusColor.value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text("상태: ${viewModel.state.value}", fontSize = 24.sp)
            Text("무게: ${viewModel.weight.value}", fontSize = 32.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = { viewModel.toggleConnection(context) }) {
                    Text(viewModel.connectButtonText.value)
                }
                Button(
                    onClick = {
                        if (viewModel.isDataReceivingEnabled.value) {
                            viewModel.stopDataReceiving()
                        } else {
                            viewModel.startDataReceiving()
                        }
                    },
                    enabled = viewModel.loadingButtonEnabled.value
                ) {
                    Text(viewModel.loadingButtonText.value)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (viewModel.isDriving.value) {
                        viewModel.stopDriving()
                    } else {
                        viewModel.startDriving()
                    }
                },
                enabled = viewModel.drivingButtonEnabled.value
            ) {
                Text(viewModel.drivingButtonText.value)
            }
            Button(
                onClick = { viewModel.resetInitialValues() },
                enabled = viewModel.resetButtonEnabled.value
            ) {
                Text("초기값 재설정")
            }
        }
    }

    // 첫 번째 팝업 (블루투스 권한 안내)
    if (showPermissionInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionInfoDialog = false },
            title = { Text("권한 안내") },
            text = { Text("블루투스 연결을 위해 권한이 필요합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionInfoDialog = false
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        } else {
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        permissionLauncher.launch(permissions) // 권한 요청 시작
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionInfoDialog = false }
                ) {
                    Text("취소")
                }
            }
        )
    }
    // 다이얼로그 표시
    if (viewModel.showDeviceDialog.value) {
        DeviceSelectionDialog(
            devices = viewModel.pairedDevicesList,
            onDeviceSelected = { device ->
                viewModel.showDeviceDialog.value = false
                viewModel.connectToDevice(context, device) // 선택된 기기로 연결 시도
            },
            onDismiss = {
                viewModel.showDeviceDialog.value = false
            }
        )
    }
}