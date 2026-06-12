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

    fun loginWithEmail(email: String, pass: String) = viewModelScope.launch {
        _loginState.value = Resource.Loading()
        _loginState.value = repository.loginWithEmail(email, pass)
    }

    fun isUserLoggedIn() = repository.getCurrentUser() != null
}
