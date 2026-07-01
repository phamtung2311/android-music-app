package com.example.zingmp3.ui.screens

import android.content.Context.MODE_PRIVATE
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.zingmp3.network.model.LoginRequest
import com.example.zingmp3.ui.viewmodel.AuthState
import com.example.zingmp3.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthState.Success -> {
                val response = (loginState as AuthState.Success).data
                val user = response.user
                
                if (user != null && !user.isActive) {
                    Toast.makeText(context, "Tài khoản của bạn đã bị khóa bởi Admin", Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                } else {
                    val sharedPref = context.getSharedPreferences("USER_DATA", MODE_PRIVATE)
                    val role = user?.role ?: "user"
                    val token = response.token
                    
                    sharedPref.edit()
                        .putBoolean("isLoggedIn", true)
                        .putInt("userId", user?.id ?: -1)
                        .putString("username", user?.username)
                        .putString("email", user?.email)
                        .putString("role", role)
                        .putString("bio", user?.bio)
                        .putString("avatar_url", user?.avatar_url)
                        .putString("token", token)
                        .apply()

                    com.example.zingmp3.network.RetrofitClient.setToken(token)

                    Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    
                    val destination = if (role == "admin") "admin" else "main_flow"
                    navController.navigate(destination) {
                        popUpTo("login_flow") { inclusive = true }
                    }
                    viewModel.resetState()
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, (loginState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
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
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = "Muzic", 
                color = Color.White,
                fontSize = 42.sp, 
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Đăng nhập", 
                color = Color.White,
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Email hoặc Tên đăng nhập") },
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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF1DB954),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF1DB954)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = { /* Forgot password */ },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Quên mật khẩu?", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (loginState is AuthState.Loading) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            } else {
                Button(
                    onClick = {
                        if (login.isNotEmpty() && password.isNotEmpty()) {
                            viewModel.login(LoginRequest(login = login, password = password))
                        } else {
                            Toast.makeText(context, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Đăng nhập", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(modifier = Modifier.weight(1f), color = Color.DarkGray)
                Text(" HOẶC ", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Divider(modifier = Modifier.weight(1f), color = Color.DarkGray)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // Quick login placeholders
                SocialLoginButton("Google", Color.White, Color.Black)
                SocialLoginButton("Facebook", Color(0xFF1877F2), Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = { navController.navigate("register") }) {
                Text("Chưa có tài khoản? Đăng ký ngay", color = Color(0xFF1DB954))
            }
        }
    }
}

@Composable
fun SocialLoginButton(text: String, containerColor: Color, contentColor: Color) {
    Button(
        onClick = { },
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier.width(140.dp).height(48.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Text(text, color = contentColor, fontWeight = FontWeight.Bold)
    }
}
