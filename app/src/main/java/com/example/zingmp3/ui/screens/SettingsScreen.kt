package com.example.zingmp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingSectionTitle("Tài khoản") }
            item { SettingItem("Thông tin cá nhân") }
            item { SettingItem("Đổi mật khẩu") }
            item { SettingItem("Quản lý Premium") }

            item { SettingSectionTitle("Chất lượng âm thanh") }
            item { SettingItem("Chất lượng Streaming", "Cực cao") }
            item { SettingItem("Chất lượng Tải xuống", "Cao") }

            item { SettingSectionTitle("Tải xuống") }
            item { SettingItem("Quản lý nhạc offline") }
            item { SettingItem("Vị trí lưu trữ") }

            item { SettingSectionTitle("Thông báo") }
            item { SettingItem("Thông báo đẩy") }

            item { SettingSectionTitle("Giao diện") }
            item { SettingItem("Chế độ tối", "Bật") }

            item { SettingSectionTitle("Giới thiệu") }
            item { SettingItem("Phiên bản", "1.0.0") }
            item { SettingItem("Điều khoản sử dụng") }
            item { SettingItem("Chính sách bảo mật") }
        }
    }
}

@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF1DB954),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(title: String, value: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(text = value, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
