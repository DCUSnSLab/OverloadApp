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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.overloadapp.BluetoothManager as MyBluetoothManager
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyOrange40
import com.example.overloadapp.ui.theme.OverloadAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 화물 데이터 클래스
data class Freight(
    val owner: String,              // 화물주인 이름(또는 회사명)
    val requestTime: String,        // 요청 시간 (YYYYMMDDHHmm)
    val departure: String,          // 출발지 주소
    val destination: String,        // 목적지 주소
    val weight: Int,                // 화물 중량 (kg 단위)
    val photoUri: String? = null    // 화물 사진 (선택)
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
    val connectedDeviceName by remember { MyBluetoothManager.connectedDeviceName }
    val sensorData by MyBluetoothManager.sensorData.collectAsState()

    // 화물 리스트 상태 관리
    val freightList = remember { mutableStateListOf<Freight>() }

    // 모달 표시 상태
    var showAddFreightDialog by remember { mutableStateOf(false) }

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
                        (context as? Activity)?.finish()
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
                onClick = { showAddFreightDialog = true },
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
                MyTopHeader(
                    connectedDeviceName = connectedDeviceName ?: "연결 정보 없음",
                    data = sensorData?.weight ?: 0
                )
            }

            // 화물 리스트
            items(freightList) { freight ->
                MyFreight(
                    freight = freight,
                    onClick = {
                        Toast.makeText(context, "${freight.owner} 클릭", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // 화물 추가 모달
    if (showAddFreightDialog) {
        AddFreightDialog(
            onDismiss = { showAddFreightDialog = false },
            onConfirm = { freight ->
                freightList.add(freight)
                showAddFreightDialog = false
                Toast.makeText(context, "화물이 추가되었습니다", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddFreightDialog(
    onDismiss: () -> Unit,
    onConfirm: (Freight) -> Unit
) {
    var owner by remember { mutableStateOf("") }
    var departure by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    var isDepartureExpanded by remember { mutableStateOf(false) }
    var isDestinationExpanded by remember { mutableStateOf(false) }

    val regions = listOf(
        "서울", "경기", "인천", "강원", "충북", "충남",
        "대전", "세종", "경북", "대구", "경남", "부산",
        "울산", "전북", "전남", "광주", "제주"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "화물 정보 입력",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "화주에게 청구되는 운임입니다.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 화주 입력
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("화주") },
                    placeholder = { Text("화주명 또는 회사명") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 출발지 드롭다운
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = departure,
                        onValueChange = { },
                        label = { Text("출발지") },
                        placeholder = { Text("지역 선택") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { isDepartureExpanded = !isDepartureExpanded }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "드롭다운"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = isDepartureExpanded,
                        onDismissRequest = { isDepartureExpanded = false }
                    ) {
                        regions.forEach { region ->
                            DropdownMenuItem(
                                text = { Text(region) },
                                onClick = {
                                    departure = region
                                    isDepartureExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 목적지 드롭다운
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { },
                        label = { Text("목적지") },
                        placeholder = { Text("지역 선택") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { isDestinationExpanded = !isDestinationExpanded }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "드롭다운"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = isDestinationExpanded,
                        onDismissRequest = { isDestinationExpanded = false }
                    ) {
                        regions.forEach { region ->
                            DropdownMenuItem(
                                text = { Text(region) },
                                onClick = {
                                    destination = region
                                    isDestinationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 중량 입력
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        // 정수만 입력 가능하도록
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            weight = it
                        }
                    },
                    label = { Text("중량 (kg)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { /* 키보드 닫기 */ }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 화물 추가 버튼
                Button(
                    onClick = {
                        if (owner.isNotBlank() && departure.isNotBlank() &&
                            destination.isNotBlank() && weight.isNotBlank()) {
                            val currentTime = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
                                .format(Date())
                            val freight = Freight(
                                owner = owner,
                                requestTime = currentTime,
                                departure = departure,
                                destination = destination,
                                weight = weight.toIntOrNull() ?: 0
                            )
                            onConfirm(freight)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B8DEE)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "화물 추가",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
    onClick: (() -> Unit)? = null
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