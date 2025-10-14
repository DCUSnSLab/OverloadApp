package com.example.overloadapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.example.overloadapp.ui.theme.MyGray80
import com.example.overloadapp.ui.theme.MyOrange40
import com.example.overloadapp.ui.theme.OverloadAppTheme
import com.example.overloadapp.BluetoothManager as MyBluetoothManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

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

    // BluetoothManager의 초기화를 보장합니다.
    LaunchedEffect(Unit) {
        MyBluetoothManager.initialize(context)
    }

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

    // 싱글톤 객체에서 상태를 직접 가져옵니다.
    val isBluetoothEnabled by remember { MyBluetoothManager.isBluetoothEnabled }
    val pairedDevices by remember { MyBluetoothManager.pairedDevices } // 싱글톤의 pairedDevices 상태를 사용
    val loading by remember { MyBluetoothManager.isLoading } // 싱글톤의 loading 상태를 사용

    // 블루투스 상태 변경 수신기 등록/해제
    // 이 부분은 MyBluetoothManager 내부에 통합하는 것이 더 좋습니다.
    // 여기서는 일단 기존 로직을 유지하면서 MyBluetoothManager의 상태를 업데이트하도록 수정
    DisposableEffect(context) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                MyBluetoothManager.isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
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
    LaunchedEffect(isBluetoothEnabled) {
        if (isBluetoothEnabled) {
            val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                        PackageManager.PERMISSION_GRANTED
            } else true

            if (hasConnectPermission) {
                // 권한이 있을 때만 loadPairedDevices 함수를 호출
                MyBluetoothManager.loadPairedDevices(context)
            } else {
                // 권한이 없으면 사용자에게 권한 요청을 유도하는 UI를 표시하거나
                // 메시지를 띄울 수 있습니다.
                // 예를 들어, Toast 메시지를 띄우거나 다이얼로그를 표시할 수 있습니다.
            }
        } else {
            // 블루투스가 꺼져 있을 때
            MyBluetoothManager.pairedDevices.value = emptyList()
            MyBluetoothManager.isLoading.value = false
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
            !isBluetoothEnabled -> {
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
            // 이 else 블록을 추가하여 기기 목록을 출력합니다.
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MyGray80)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    pairedDevices.forEach { device ->
                        BluetoothDeviceElement(
                            name = device.name ?: "알 수 없는 기기",
                            modifier = Modifier
                                .padding(vertical = 5.dp, horizontal = 10.dp),
                            onClick = {
                                // BluetoothManager의 connectToDevice 호출
                                MyBluetoothManager.connectToDevice(context, device)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothBottomBar() {
    val context = LocalContext.current

    // isBluetoothOn 상태를 싱글톤에서 직접 가져옴
    val isBluetoothEnabled by remember { MyBluetoothManager.isBluetoothEnabled }
    val buttonColor = if (isBluetoothEnabled) MyGray80 else MyOrange40
    val textColor = if (isBluetoothEnabled) Color.Black else Color.White

    // ... (블루투스 권한/활성화 런처는 그대로 유지)
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            MyBluetoothManager.isBluetoothEnabled.value = true
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MyGray80)
            .navigationBarsPadding()
    ) {
        BottomBarButton(
            iconResId = R.drawable.ic_settings,
            text = "블루투스 사용 설정",
            buttonColor = buttonColor,
            textColor = textColor,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        )
                    )
                } else {
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