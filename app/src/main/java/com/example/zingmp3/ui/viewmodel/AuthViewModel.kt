package com.example.zingmp3.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zingmp3.data.repository.AuthRepository
import com.example.zingmp3.network.model.LoginRequest
import com.example.zingmp3.network.model.LoginResponse
import com.example.zingmp3.network.model.RegisterRequest
import com.example.zingmp3.network.model.RegisterResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import org.json.JSONObject
import retrofit2.Response

sealed class AuthState<out T> {
    object Idle : AuthState<Nothing>()
    object Loading : AuthState<Nothing>()
    data class Success<T>(val data: T) : AuthState<T>()
    data class Error(val message: String) : AuthState<Nothing>()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginState = MutableStateFlow<AuthState<LoginResponse>>(AuthState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthState<RegisterResponse>>(AuthState.Idle)
    val registerState = _registerState.asStateFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            try {
                val response = repository.login(request)
                if (response.isSuccessful) {
                    _loginState.value = AuthState.Success(response.body()!!)
                } else {
                    val errorMsg = try {
                        val errorObj = JSONObject(response.errorBody()?.string() ?: "{}")
                        errorObj.getString("message")
                    } catch (e: Exception) {
                        "Sai tài khoản hoặc mật khẩu"
                    }
                    _loginState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = AuthState.Error("Lỗi kết nối: ${e.message}")
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful) {
                    _registerState.value = AuthState.Success(response.body()!!)
                } else {
                    val errorMsg = try {
                        val errorObj = JSONObject(response.errorBody()?.string() ?: "{}")
                        errorObj.getString("message")
                    } catch (e: Exception) {
                        "Đăng ký thất bại"
                    }
                    _registerState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error("Lỗi kết nối: ${e.message}")
            }
        }
    }

    fun resetState() {
        _loginState.value = AuthState.Idle
        _registerState.value = AuthState.Idle
    }
}
