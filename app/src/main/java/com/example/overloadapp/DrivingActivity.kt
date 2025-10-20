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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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

data class Freight(
    val owner: String,              // 화물주인 이름(또는 회사명)
    val requestTime: String,        // 요청 시간 (YYYYMMDDHHmm)
    val departure: String,          // 출발지 주소
    val destination: String,        // 목적지 주소
    val weight: Double,             // 화물 중량 (톤 단위)
    val photoUri: String? = null,   // 화물 사진 (선택)
    val cost: Int? = 0              // 화물 운송비
)

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

    val freightList = remember {    // 테스트용 화물 데이터
        listOf(
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8),
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8),
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8),
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8),
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8),
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8),
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8),
            Freight("삼성물류", "202410201430", "대구", "서울", 3.5),
            Freight("현대운송", "202410201500", "경북", "부산", 2.8)
        )
    }
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

            items(freightList) { freight ->
                MyFreight(
                    freight = freight,
                    onClick = {
                        // 상세보기 화면으로 이동 등
                        Toast.makeText(context, "${freight.owner} 클릭", Toast.LENGTH_SHORT).show()
                    }
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

@Composable
fun MyFreight(
    freight: Freight,
    onClick: (() -> Unit)? = null  // 클릭 시 상세보기용
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp)
    ) {
        Column {
            // 화물주인
            Text(
                text = freight.owner,
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 출발지 -> 목적지
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = freight.departure,
                    color = MyBlue80,
                    fontSize = 16.sp
                )

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "화살표",
                    tint = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Text(
                    text = freight.destination,
                    color = MyBlue80,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrivingScreenPreview() {
    OverloadAppTheme {
        DrivingScreen()
    }
}