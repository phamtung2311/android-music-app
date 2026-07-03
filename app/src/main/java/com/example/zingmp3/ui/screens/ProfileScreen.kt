package com.example.zingmp3.ui.screens

import android.content.Context.MODE_PRIVATE
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.zingmp3.network.model.UpdateProfileRequest
import com.example.zingmp3.ui.viewmodel.AuthState
import com.example.zingmp3.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    rootNavController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("USER_DATA", MODE_PRIVATE) }
    
    var userId by remember { mutableIntStateOf(sharedPref.getInt("userId", -1)) }
    var username by remember { mutableStateOf(sharedPref.getString("username", "") ?: "") }
    var email by remember { mutableStateOf(sharedPref.getString("email", "") ?: "") }
    var bio by remember { mutableStateOf(sharedPref.getString("bio", "") ?: "") }
    var avatarUrl by remember { mutableStateOf(sharedPref.getString("avatar_url", "") ?: "") }

    var isEditing by remember { mutableStateOf(false) }
    
    val updateState by authViewModel.updateProfileState.collectAsState()

    LaunchedEffect(updateState) {
        if (updateState is AuthState.Success) {
            Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
            sharedPref.edit()
                .putString("username", username)
                .putString("bio", bio)
                .apply()
            isEditing = false
            authViewModel.resetState()
        } else if (updateState is AuthState.Error) {
            Toast.makeText(context, (updateState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            authViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang cá nhân") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            AsyncImage(
                model = if (avatarUrl.isEmpty()) "https://picsum.photos/200" else avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Tên người dùng") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF1DB954),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Tiểu sử") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF1DB954),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (updateState is AuthState.Loading) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { 
                                isEditing = false
                                username = sharedPref.getString("username", "") ?: ""
                                bio = sharedPref.getString("bio", "") ?: ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Hủy")
                        }
                        Button(
                            onClick = { 
                                authViewModel.updateProfile(userId, UpdateProfileRequest(username, bio, avatarUrl))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                        ) {
                            Text("Lưu")
                        }
                    }
                }
            } else {
                Text(
                    text = username,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (bio.isNotEmpty()) {
                    Text(
                        text = bio,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        sharedPref.edit().clear().apply()
                        com.example.zingmp3.network.RetrofitClient.setToken(null)
                        rootNavController.navigate("login_flow") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Đăng xuất", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
