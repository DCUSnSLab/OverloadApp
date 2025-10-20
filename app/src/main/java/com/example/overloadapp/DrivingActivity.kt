package com.example.overloadapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overloadapp.BluetoothManager as MyBluetoothManager
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyOrange40
import com.example.overloadapp.ui.theme.OverloadAppTheme

class DrivingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OverloadAppTheme {
                DrivingScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingScreen() {
    val context = LocalContext.current
    // BluetoothManager 싱글톤 객체의 상태를 직접 참조
    val connectedDeviceName by remember { MyBluetoothManager.connectedDeviceName }
    val sensorData by MyBluetoothManager.sensorData.collectAsState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "운행 중", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MyBlue80,
                    scrolledContainerColor = MyBlue80
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        // 뒤로가기
                        (context as? Activity)?.finish() // 현재 액티비티를 종료
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* 더보기 버튼 동작 */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* FAB 클릭 동작 */ },
                containerColor = MyOrange40,
            ) {
                Text(text = "화물 수동 추가", color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "추가",
                    tint = Color.White
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                // 상단 확장 가능한 헤더
                // connectedDeviceName을 매개변수로 전달
                MyTopHeader(
                    connectedDeviceName = connectedDeviceName ?: "연결 정보 없음",
                    data = sensorData?.weight ?: 0
                )
            }
            items(1) { index ->
                Text(
                    text = "추정 무게: ${sensorData?.weight ?: "---"} kg",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MyTopHeader(connectedDeviceName: String, data: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MyBlue80),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$connectedDeviceName : $data kg", color = Color.White, fontSize = 24.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun DrivingScreenPreview() {
    OverloadAppTheme {
        DrivingScreen()
    }
}