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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext

import com.example.overloadapp.ui.theme.OverloadAppTheme
import com.example.overloadapp.ui.theme.MyOrange40
import com.example.overloadapp.ui.theme.MyBlue40
import com.example.overloadapp.ui.theme.MyBlue80
import com.example.overloadapp.ui.theme.MyGray40
import com.example.overloadapp.ui.theme.MyGray80

class MainActivity : ComponentActivity() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppContent() {
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
            MyBottomBar()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MyGray80),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Hello World")
        }
    }
}

@Composable
fun MyBottomBar() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray) // 전체 바 배경 (필요없으면 제거)
            .navigationBarsPadding()
    ) {
        // "환경설정" 버튼
        BottomBarButton(
            iconResId = R.drawable.ic_settings,
            text = "환경설정",
            buttonColor = MyGray40,
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* 다른 동작 구현 */ }
        )
        // "운행이력" 버튼
        BottomBarButton(
            iconResId = R.drawable.ic_history,
            text = "운행이력",
            buttonColor = MyBlue80,
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* 다른 동작 구현 */ }
        )
        // "운행시작" 버튼
        BottomBarButton(
            iconResId = R.drawable.ic_flag,
            text = "운행시작",
            buttonColor = MyBlue40,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // 운행시작 버튼을 누르면 DrivingActivity를 시작
                val intent = Intent(context, DrivingActivity::class.java)
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun BottomBarButton(
    iconResId: Int,
    text: String,
    buttonColor: Color,
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
                .height(32.dp)  // 아이콘 높이를 동일하게 고정
                .aspectRatio(1f, matchHeightConstraintsFirst = false), // 정사각형 비율 유지
            contentScale = ContentScale.Fit
        )
        Text(
            text = text,
            modifier = Modifier.padding(top = 6.dp),
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    OverloadAppTheme {
        MyAppContent()
    }
}