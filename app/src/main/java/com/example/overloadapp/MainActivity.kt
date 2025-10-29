package com.example.overloadapp

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

import com.example.overloadapp.ui.theme.OverloadAppTheme
import com.example.overloadapp.ui.theme.MyOrange40
import com.example.overloadapp.ui.theme.MyBlue40
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyGray40
import com.example.overloadapp.ui.theme.MyGray80
import com.example.overloadapp.BluetoothManager as MyBluetoothManager


class MainActivity : ComponentActivity() {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OverloadAppTheme {
                MyAppContent()
            }
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppContent() {
    val context = LocalContext.current
    // BluetoothManager 싱글톤 객체의 상태를 직접 참조
    val connectedDeviceName by remember { MyBluetoothManager.connectedDeviceName }

    // *****************************************************************
    // 1. [여기에 권한 요청 런처를 정의해야 합니다.]
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // 권한 허용 시 초기화 및 자동 연결 시도
            MyBluetoothManager.initialize(context)
            if (MyBluetoothManager.isBluetoothEnabled.value) {
                MyBluetoothManager.connectToRememberedDevice(context)
            }
        } else {
            //Toast.makeText(context, "블루투스 기능을 사용하려면 모든 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }
    // *****************************************************************

    LaunchedEffect(Unit) {
        // *****************************************************************
        // 2. [여기에 권한 확인 및 요청 로직을 추가해야 합니다.]
        val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allPermissionsGranted = permissionsToRequest.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            // 권한이 있다면 바로 초기화 및 자동 연결 시도
            MyBluetoothManager.initialize(context)
            if (MyBluetoothManager.isBluetoothEnabled.value) {
                MyBluetoothManager.connectToRememberedDevice(context)
            }
        } else {
            // 권한이 없다면 런처를 실행하여 요청 모달창을 띄웁니다.
            permissionLauncher.launch(permissionsToRequest)
        }
        // *****************************************************************
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "홈",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontWeight = FontWeight.Light
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MyOrange40
                )
            )
        },
        bottomBar = {
            MainBottomBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MyGray80),
        ) {
            // connectedDeviceName 상태를 ProfileSection으로 전달
            ProfileSection(connectedDeviceName)
            Spacer(modifier = Modifier.height(16.dp))
            RecentHistorySection()
        }
    }
}

@Composable
fun ProfileSection(connectedDeviceName: String?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            // 프로필 아이콘 (Vector Asset으로 추가)
            Image(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = "프로필 사진",
                modifier = Modifier
                    .height(32.dp)
                    .aspectRatio(1f, matchHeightConstraintsFirst = false),
                    //.background(MyGray40, shape = RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = "홍길동 님", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (connectedDeviceName != null) {
                    Text(text = connectedDeviceName, fontWeight = FontWeight.Bold, color = MyOrange40)
                } else {
                    Text(text = "차량 등록 필요", fontWeight = FontWeight.Bold, color = MyOrange40)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // 차량 등록 버튼
            Box(
                modifier = Modifier
                    .background(MyBlue40, shape = RoundedCornerShape(24.dp))
                    .clickable {
                        val intent = Intent(context, RegisteringActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(text = "차량등록 +", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecentHistorySection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MyGray80) // 운행 이력 섹션의 배경색
            .padding(16.dp)
    ) {
        // 최근 운행 이력 텍스트
        Text(text = "최근 운행 이력", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // 운행 이력 없음 텍스트
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 원하는 높이로 조절
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "운행 이력 없음", color = Color.Gray)
        }
    }
}

@Composable
fun BottomBarButton(
    iconResId: Int,
    text: String,
    buttonColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(96.dp) // 버튼 높이
            .background(buttonColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = text,
            modifier = Modifier
                .height(38.dp)  // 아이콘 높이를 동일하게 고정
                .aspectRatio(1f, matchHeightConstraintsFirst = false), // 정사각형 비율 유지
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(textColor)
        )
        Text(
            text = text,
            modifier = Modifier.padding(top = 6.dp),
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
    }
}

@Composable
fun MainBottomBar() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MyGray80) // 전체 바 배경 (필요없으면 제거)
            .navigationBarsPadding()
    ) {
        // "환경설정" 버튼
        BottomBarButton(
            iconResId = R.drawable.ic_settings,
            text = "환경설정",
            buttonColor = MyGray40,
            textColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* 다른 동작 구현 */ }
        )
        // "운행이력" 버튼
        BottomBarButton(
            iconResId = R.drawable.ic_history,
            text = "운행이력",
            buttonColor = MyBlue80,
            textColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* 다른 동작 구현 */ }
        )
        // "운행시작" 버튼
        BottomBarButton(
            iconResId = R.drawable.ic_flag,
            text = "운행시작",
            buttonColor = MyBlue40,
            textColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // 운행시작 버튼을 누르면 DrivingActivity를 시작
                val intent = Intent(context, DrivingActivity::class.java)
                context.startActivity(intent)
            }
        )
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Preview(showBackground = true)
@Composable
fun AppPreview() {
    OverloadAppTheme {
        MyAppContent()
    }
}