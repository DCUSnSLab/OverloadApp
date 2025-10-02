package com.example.overloadapp

import android.content.Intent
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overloadapp.ui.theme.MyBlue40
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyGray40
import com.example.overloadapp.ui.theme.MyGray80
import com.example.overloadapp.ui.theme.OverloadAppTheme
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.PermissionChecker
import com.example.overloadapp.ui.theme.MyBlue40
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyGray40
import com.example.overloadapp.ui.theme.MyGray80
import com.example.overloadapp.ui.theme.OverloadAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
fun BluetoothStatusBox(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter = remember { bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() }

    // 블루투스 켜짐 여부 상태
    val isEnabled = remember { mutableStateOf(adapter?.isEnabled == true) }

    // 기기 목록 로딩 상태 및 결과(간단히 이름만 저장)
    var loading by remember { mutableStateOf(false) }
    var deviceNames by remember { mutableStateOf<List<String>>(emptyList()) }

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
                deviceNames = emptyList()
                loading = false
            } else {
                loading = true
                // 실제 IO는 IO 디스패처에서 처리 (bondedDevices 접근 등)
                val list = withContext(Dispatchers.IO) {
                    try {
                        adapter?.bondedDevices
                            ?.mapNotNull { d: BluetoothDevice ->
                                d.name ?: d.address
                            }
                            ?: emptyList()
                    } catch (e: SecurityException) {
                        emptyList()
                    }
                }
                deviceNames = list
                loading = false
            }
        } else {
            // 블루투스가 꺼져 있을 때 초기화
            deviceNames = emptyList()
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
            deviceNames.isEmpty() -> {
                Text(
                    text = "페어링된 기기가 없습니다.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                // 간단히 첫 번째 기기 이름만 표시 예시 — 필요하면 LazyColumn으로 목록을 보여줘
                Text(
                    text = "연결 가능한 기기 예시:\n" + deviceNames.joinToString("\n"),
                    fontSize = 14.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
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