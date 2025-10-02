package com.example.overloadapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.core.content.PermissionChecker

import com.example.overloadapp.ui.theme.MyBlue40
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyGray40
import com.example.overloadapp.ui.theme.MyGray80
import com.example.overloadapp.ui.theme.OverloadAppTheme

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


var bluetoothSocket: BluetoothSocket? = null
var inputStream: InputStream? = null
var outputStream: OutputStream? = null
val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

class RegisteringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OverloadAppTheme {
                RegisteringScreen()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteringScreen() {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "차량 등록", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MyBlue80,
                    scrolledContainerColor = MyBlue80
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        // 뒤로가기
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            BluetoothBottomBar()
        }
    ) { innerPadding ->
        // 중앙 본문을 BluetoothStatusBox로 대체
        BluetoothStatusBox(innerPadding = innerPadding)
    }
}

@Composable
fun BluetoothDeviceElement(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth() // Row가 가로로 꽉 차도록 설정
            .height(72.dp)
            .background(Color.White, shape = RoundedCornerShape(22.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp) // 좌우 패딩 추가
    ) {
        Text(
            text = name,
            modifier = Modifier
                .padding(top = 6.dp)
                .weight(1f),
            color = Color.Black,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
        Box(
            modifier = Modifier
                .background(MyBlue40, shape = RoundedCornerShape(14.dp))
                .clickable { }
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clickable { onClick() }
        ) {
            Text(text = "선택", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BluetoothStatusBox(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter = remember { bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() }

    // 블루투스 켜짐 여부 상태
    val isEnabled = remember { mutableStateOf(adapter?.isEnabled == true) }

    // 기기 목록 로딩 상태 및 결과(간단히 이름만 저장)
    var loading by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    // 블루투스 상태 변경 수신기 등록/해제
    DisposableEffect(context, adapter) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                isEnabled.value = (state == BluetoothAdapter.STATE_ON || adapter?.isEnabled == true)
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) { }
        }
    }

    // 블루투스가 켜질 때 페어링된 기기 목록을 비동기로 불러옴
    LaunchedEffect(isEnabled.value) {
        if (isEnabled.value) {
            // 권한 체크 (Android S 이상에서는 BLUETOOTH_CONNECT 필요)
            val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                        PermissionChecker.PERMISSION_GRANTED
            } else true

            if (!hasConnectPermission) {
                // 권한이 없으면 목록을 못 불러오니 빈 상태로 유지
                pairedDevices = emptyList()
                loading = false
            } else {
                loading = true
                // 실제 IO는 IO 디스패처에서 처리 (bondedDevices 접근 등)
                val list = withContext(Dispatchers.IO) {
                    try {
                        adapter?.bondedDevices?.toList() ?: emptyList()
                    } catch (e: SecurityException) {
                        emptyList()
                    }
                }
                pairedDevices = list
                loading = false
            }
        } else {
            // 블루투스가 꺼져 있을 때 초기화
            pairedDevices = emptyList()
            loading = false
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        when {
            !isEnabled.value -> {
                Text(
                    text = "블루투스 꺼짐",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            loading -> {
                Text(
                    text = "기기 목록을 불러오는 중...",
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
            pairedDevices.isEmpty() -> {
                Text(
                    text = "페어링된 기기가 없습니다.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MyGray80) // 전체 바 배경 (필요없으면 제거)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    pairedDevices.forEach { device -> // deviceNames 배열을 순회
                        BluetoothDeviceElement(
                            name = device.name, // 각 기기 이름을 전달
                            modifier = Modifier
                                .padding(vertical = 5.dp, horizontal = 10.dp),
                            onClick = { connectToDevice(context, device) }
                        )
                    }
                }
            }
        }
    }
}



@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun connectToDevice(context: Context, device: BluetoothDevice) {
    Toast.makeText(context, "연결시도", Toast.LENGTH_SHORT).show()

    // 권한 재확인 (Android S 이상)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val has = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PermissionChecker.PERMISSION_GRANTED
        if (!has) {
            // 권한이 없으면 UI에 알려주고 종료 (또는 권한 요청 트리거)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "BLUETOOTH_CONNECT 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
            return
        }
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 기존 소켓 정리
            try {
                bluetoothSocket?.close()
            } catch (_: Exception) {}

            // 기본 RFCOMM 소켓 생성 시도
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
            } catch (e1: Exception) {
                // 1차 실패 시 대안 시도 (reflection)
                try {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    bluetoothSocket = m.invoke(device, 1) as BluetoothSocket
                    bluetoothSocket?.connect()
                } catch (e2: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "연결 실패: ${e2.message ?: "오류"}", Toast.LENGTH_LONG).show()
                    }
                    // 정리
                    try { bluetoothSocket?.close() } catch (_: Exception) {}
                    bluetoothSocket = null
                    return@launch
                }
            }

            // 연결 성공 시 스트림 설정
            try {
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "스트림 획득 실패: ${e.message ?: "오류"}", Toast.LENGTH_LONG).show()
                }
                try { bluetoothSocket?.close() } catch (_: Exception) {}
                bluetoothSocket = null
                return@launch
            }

            // beginListenForData 호출은 반드시 예외 처리로 감싸서 앱 크래시 방지
            try {
                beginListenForData(context) // 변경: context 전달
            } catch (e: Exception) {
                // 읽기 루프 관련 예외 처리
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "데이터 수신 루프 시작 실패: ${e.message ?: "오류"}", Toast.LENGTH_LONG).show()
                }
            }

            // UI 업데이트
            withContext(Dispatchers.Main) {
                updateUiOnConnect(device.name ?: "알 수 없음")
                Toast.makeText(context, "연결완료. 적재 시작 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show()
            }

        } catch (secEx: SecurityException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "권한 오류: ${secEx.message}", Toast.LENGTH_LONG).show()
            }
        } catch (ioe: IOException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "IO 오류: ${ioe.message}", Toast.LENGTH_LONG).show()
            }
        } catch (t: Throwable) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "알 수 없는 오류: ${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun CoroutineScope.updateUiOnConnect(name: String) {}

private fun beginListenForData(context: Context) {
    // 안전하게 초기 조건 검사
    val inStream = inputStream ?: throw IllegalStateException("inputStream is null")
    CoroutineScope(Dispatchers.IO).launch {
        val buffer = ByteArray(1024)
        while (isActive) {
            try {
                val bytesRead = inStream.read(buffer)
                if (bytesRead > 0) {
                    val data = String(buffer, 0, bytesRead) // 인코딩에 맞춰 수정 가능
                    // 메인 스레드에서 UI 처리
                    withContext(Dispatchers.Main) {
                        // 예: 토스트(디버깅용). 실제 앱에서는 로그나 화면에 작게 표시하세요.
                        Toast.makeText(context, "수신: $data", Toast.LENGTH_SHORT).show()
                    }
                } else if (bytesRead == -1) {
                    // 스트림 종료
                    break
                }
            } catch (e: IOException) {
                // 연결 끊김 등
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "데이터 수신 중 오류: ${e.message ?: "IO 오류"}", Toast.LENGTH_LONG).show()
                }
                break
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "수신 루프 예외: ${t.message ?: "오류"}", Toast.LENGTH_LONG).show()
                }
                break
            }
        }

        // 루프 빠져나오면 소켓/스트림 정리
        try { inStream.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { bluetoothSocket?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        bluetoothSocket = null

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "수신 루프 종료", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun BluetoothBottomBar() {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter

    // 블루투스 활성화 요청 런처
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 블루투스 활성화 성공
        }
    }

    // 블루투스 권한 요청 런처
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 권한 요청 결과 처리
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // 모든 권한이 허가되었을 경우, 블루투스 활성화 요청 시작
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            // 권한이 거부되었을 경우, 사용자에게 안내
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MyGray80) // 전체 바 배경 (필요없으면 제거)
            .navigationBarsPadding()
    ) {
        // "환경설정" 버튼
        BottomBarButton(
            iconResId = R.drawable.ic_settings,
            text = "블루투스 사용 설정",
            buttonColor = MyGray40,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12 이상에서 런타임 권한 요청
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        )
                    )
                } else {
                    // Android 11 이하에서는 Manifest 권한만으로 충분
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                }
            }
        )
    }
}

// Preview용 (간단)
@Preview(showBackground = true)
@Composable
fun RegisteringScreenPreview() {
    OverloadAppTheme {
        RegisteringScreen()
    }
}