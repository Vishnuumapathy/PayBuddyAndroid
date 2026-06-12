package com.paybuddy.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.paybuddy.data.repository.DeveloperResetRepository
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val auth = FirebaseAuth.getInstance()
    private val resetRepository = DeveloperResetRepository()

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog = _showLogoutDialog.asStateFlow()

    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog = _showResetDialog.asStateFlow()

    private val _isResetting = MutableStateFlow(false)
    val isResetting = _isResetting.asStateFlow()

    private val _resetSuccess = MutableStateFlow(false)
    val resetSuccess = _resetSuccess.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut = _isLoggingOut.asStateFlow()

    private val _logoutSuccess = MutableStateFlow(false)
    val logoutSuccess = _logoutSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun onLogoutClick() {
        if (!_isLoggingOut.value) {
            _showLogoutDialog.value = true
        }
    }

    fun dismissLogoutDialog() {
        _showLogoutDialog.value = false
    }

    fun onResetClick() {
        if (!_isResetting.value) {
            _showResetDialog.value = true
        }
    }

    fun dismissResetDialog() {
        _showResetDialog.value = false
    }

    fun confirmReset() {
        _showResetDialog.value = false
        if (_isResetting.value) return

        _isResetting.value = true
        viewModelScope.launch {
            try {
                val vendorId = sessionManager.getVendorId()
                if (vendorId != null) {
                    val result = resetRepository.resetVendorData(vendorId)
                    if (result.isSuccess) {
                        // After success, set transition flag
                        SessionManager.isAuthTransitioning = true
                        _resetSuccess.value = true
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Reset failed"
                        _isResetting.value = false
                    }
                } else {
                    _errorMessage.value = "Vendor ID not found"
                    _isResetting.value = false
                }
            } catch (e: Exception) {
                _isResetting.value = false
                _errorMessage.value = e.message ?: "An unknown error occurred"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun confirmLogout() {
        _showLogoutDialog.value = false
        if (_isLoggingOut.value) return

        _isLoggingOut.value = true
        SessionManager.isAuthTransitioning = true
        _logoutSuccess.value = true
    }
}
