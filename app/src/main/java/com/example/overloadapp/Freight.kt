package com.example.overloadapp

data class t_Freight(
    val owner: String,              // 화물주인 이름(또는 회사명)
    val requestTime: String,        // 요청 시간 (YYYYMMDDHHmm)
    val departure: String,          // 출발지 주소
    val destination: String,        // 목적지 주소
    val weight: Int,                // 화물 중량 (kg 단위)
    val photoUri: String? = null    // 화물 사진 (선택)
)
