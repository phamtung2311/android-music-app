package com.example.zingmp3.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.zingmp3.network.model.RegisterRequest
import com.example.zingmp3.ui.viewmodel.AuthState
import com.example.zingmp3.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val registerState by viewModel.registerState.collectAsState()

    LaunchedEffect(registerState) {
        if (registerState is AuthState.Success) {
            Toast.makeText(context, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show()
            navController.navigate("login_flow") {
                popUpTo("register") { inclusive = true }
            }
            viewModel.resetState()
        } else if (registerState is AuthState.Error) {
            Toast.makeText(context, (registerState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        }
    }

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.LibraryMusic, 
                contentDescription = null, 
                tint = Color(0xFF1DB954), 
                modifier = Modifier.size(60.dp)
            )
            Text(
                text = "Tạo tài khoản", 
                color = Color.White,
                fontSize = 28.sp, 
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Tên người dùng") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF1DB954),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF1DB954)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF1DB954),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF1DB954)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF1DB954),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF1DB954)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF1DB954),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF1DB954)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (registerState is AuthState.Loading) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            } else {
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                        } else if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                            viewModel.register(RegisterRequest(username, email, password))
                        } else {
                            Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Đăng ký", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Đã có tài khoản? Đăng nhập", color = Color(0xFF1DB954))
            }
        }
    }
}
