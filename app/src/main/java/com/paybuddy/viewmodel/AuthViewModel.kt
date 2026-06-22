package com.paybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paybuddy.data.repository.AuthRepository
import com.paybuddy.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<String>?>(null)
    val loginState: StateFlow<Resource<String>?> = _loginState

    private val _resetPasswordState = MutableStateFlow<Resource<String>?>(null)
    val resetPasswordState: StateFlow<Resource<String>?> = _resetPasswordState

    fun loginWithEmail(email: String, pass: String) = viewModelScope.launch {
        _loginState.value = Resource.Loading()
        _loginState.value = repository.loginWithEmail(email, pass)
    }

    fun resetPassword(email: String) = viewModelScope.launch {
        _resetPasswordState.value = Resource.Loading()
        _resetPasswordState.value = repository.sendPasswordResetEmail(email)
    }

    fun isUserLoggedIn() = repository.getCurrentUser() != null
}
