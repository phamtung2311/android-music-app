package com.example.zingmp3.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AdminScreen(navController: NavController) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Admin Dashboard", fontSize = 24.sp)
            Button(onClick = {
                val sharedPref = context.getSharedPreferences("USER_DATA", Context.MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                com.example.zingmp3.network.RetrofitClient.setToken(null)
                navController.navigate("login_flow") {
                    popUpTo("admin") { inclusive = true }
                }
            }) {
                Text("Logout")
            }
        }
    }
}
