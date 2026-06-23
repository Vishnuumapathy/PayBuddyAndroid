package com.paybuddy.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paybuddy.data.model.Installment
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReminderItem(
    val installment: Installment,
    val customerName: String = "",
    val customerPhone: String = "",
    val itemName: String = ""
)

data class ReminderUiState(
    val isLoading: Boolean = false,
    val reminders: List<ReminderItem> = emptyList(),
    val errorMessage: String? = null,
    val isSending: Boolean = false
)

class ReminderViewModel(
    private val repository: MainRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    private var remindersJob: Job? = null

    fun loadReminders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val vendorId = sessionManager.getVendorId()
            if (vendorId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Critical Error: Vendor session not found") }
                return@launch
            }
            startObservingReminders(vendorId)
        }
    }

    private fun startObservingReminders(vendorId: String) {
        remindersJob?.cancel()
        remindersJob = viewModelScope.launch {
            combine(
                repository.getDueAndOverdueInstallments(vendorId),
                repository.getSalesByVendor(vendorId),
                repository.getAllCustomers(vendorId)
            ) { installments, sales, customers ->
                val salesMap = sales.associateBy { it.saleId }
                val customersMap = customers.associateBy { it.customerId }
                installments.map { installment ->
                    val sale = salesMap[installment.saleId]
                    val customer = customersMap[installment.customerId]
                    ReminderItem(
                        installment = installment,
                        customerName = customer?.name ?: sale?.customerName ?: "Unknown",
                        customerPhone = customer?.phone ?: "",
                        itemName = sale?.itemName ?: "Purchase"
                    )
                }
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error loading reminders: ${e.message}") }
            }.collect { list ->
                _uiState.update { it.copy(isLoading = false, reminders = list, errorMessage = null) }
            }
        }
    }

    fun markReminderSent(installmentId: String) {
        if (installmentId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                repository.updateInstallmentReminderSent(installmentId)
                _uiState.update { it.copy(isSending = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, errorMessage = "Failed to update reminder status: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
