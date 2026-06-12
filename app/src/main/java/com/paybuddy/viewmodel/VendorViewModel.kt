package com.paybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paybuddy.data.model.Vendor
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VendorViewModel(
    private val repository: MainRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _onboardingState = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val onboardingState: StateFlow<OnboardingState> = _onboardingState

    private val _currentVendor = MutableStateFlow<Vendor?>(null)
    val currentVendor: StateFlow<Vendor?> = _currentVendor

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _editName = MutableStateFlow("")
    val editName: StateFlow<String> = _editName

    private val _editShopName = MutableStateFlow("")
    val editShopName: StateFlow<String> = _editShopName

    private val _editPhone = MutableStateFlow("")
    val editPhone: StateFlow<String> = _editPhone

    private val _editUpiId = MutableStateFlow("")
    val editUpiId: StateFlow<String> = _editUpiId

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    fun loadVendorProfile() = viewModelScope.launch {
        val vendorId = sessionManager.getVendorId() ?: return@launch
        try {
            val vendor = repository.getVendor(vendorId)
            _currentVendor.value = vendor
        } catch (e: Exception) {
            // Log error
        }
    }

    fun saveVendorProfile(
        vendorId: String,
        name: String,
        shopName: String,
        phone: String,
        email: String,
        upiId: String
    ) = viewModelScope.launch {
        _onboardingState.value = OnboardingState.Loading
        try {
            val vendor = Vendor(
                vendorId = vendorId,
                name = name,
                shopName = shopName,
                phone = phone,
                email = email,
                upiId = upiId,
                createdAt = System.currentTimeMillis()
            )
            repository.saveVendor(vendor)
            sessionManager.saveSession(
                vendorId = vendorId,
                name = name,
                shopName = shopName,
                upiId = upiId,
                loginMethod = sessionManager.getLoginMethod() ?: "email"
            )
            _onboardingState.value = OnboardingState.Success
        } catch (e: Exception) {
            _onboardingState.value = OnboardingState.Error(e.message ?: "Unknown error")
        }
    }

    fun startEditing() {
        val vendor = _currentVendor.value ?: return
        _editName.value = vendor.name
        _editShopName.value = vendor.shopName
        _editPhone.value = vendor.phone
        _editUpiId.value = vendor.upiId
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
    }

    fun onNameChange(newName: String) { _editName.value = newName }
    fun onShopNameChange(newShopName: String) { _editShopName.value = newShopName }
    fun onPhoneChange(newPhone: String) { _editPhone.value = newPhone }
    fun onUpiIdChange(newUpiId: String) { _editUpiId.value = newUpiId }

    fun updateVendorProfile() = viewModelScope.launch {
        val currentVendor = _currentVendor.value ?: return@launch
        val name = _editName.value.trim()
        val shopName = _editShopName.value.trim()
        val phone = _editPhone.value.trim()
        val upiId = _editUpiId.value.trim()

        if (name.isEmpty()) {
            _updateState.value = UpdateState.Error("Vendor name cannot be empty")
            return@launch
        }
        if (phone.isEmpty()) {
            _updateState.value = UpdateState.Error("Phone number cannot be empty")
            return@launch
        }
        if (phone.length < 10) {
            _updateState.value = UpdateState.Error("Invalid phone number")
            return@launch
        }

        _updateState.value = UpdateState.Loading
        try {
            val updatedVendor = currentVendor.copy(
                name = name,
                shopName = shopName,
                phone = phone,
                upiId = upiId
            )
            repository.saveVendor(updatedVendor)
            sessionManager.saveSession(
                vendorId = updatedVendor.vendorId,
                name = updatedVendor.name,
                shopName = updatedVendor.shopName,
                upiId = updatedVendor.upiId,
                loginMethod = sessionManager.getLoginMethod() ?: "email"
            )
            _currentVendor.value = updatedVendor
            _isEditing.value = false
            _updateState.value = UpdateState.Success
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    fun clearUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    sealed class OnboardingState {
        object Idle : OnboardingState()
        object Loading : OnboardingState()
        object Success : OnboardingState()
        data class Error(val message: String) : OnboardingState()
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }
}
