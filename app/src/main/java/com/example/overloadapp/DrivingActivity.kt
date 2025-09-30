package com.example.overloadapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
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

import com.example.overloadapp.ui.theme.OverloadAppTheme
import com.example.overloadapp.ui.theme.MyOrange40
import com.example.overloadapp.ui.theme.MyBlue40
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyGray40
import com.example.overloadapp.ui.theme.MyGray80

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
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent) }) {
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
                Text(text = "화물 추가", color = Color.White)
                Spacer(modifier = Modifier.width(8.dp)) // 아이콘과 텍스트 사이 간격 추가
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
                MyTopHeader()
            }
            // 스크롤 가능한 콘텐츠
            items(20) { index ->
                Text(
                    text = "스크롤 가능한 아이템 $index",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White) // 회색 배경
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MyTopHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // 높이를 조절하여 3분의 1 크기처럼 보이게 합니다.
            .background(MyBlue80),
        contentAlignment = Alignment.Center
    ) {
        // 이미지나 다른 콘텐츠를 여기에 배치
        Text(text = "?", color = Color.White, fontSize = 24.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun DrivingScreenPreview() {
    OverloadAppTheme {
        DrivingScreen()
    }
}