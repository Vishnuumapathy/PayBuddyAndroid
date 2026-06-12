package com.paybuddy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paybuddy.data.model.*
import com.paybuddy.data.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerViewModel(private val repository: MainRepository) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _success.value = null
    }

    fun getCustomers(vendorId: String): Flow<List<Customer>> {
        viewModelScope.launch {
            repository.performMigration(vendorId)
        }
        return repository.getAllCustomers(vendorId)
    }

    fun getArchivedCustomers(vendorId: String): Flow<List<Customer>> = repository.getArchivedCustomers(vendorId)

    fun getInstallmentsByCustomer(vendorId: String, customerId: String): Flow<List<Installment>> = repository.getInstallmentsByCustomer(vendorId, customerId)

    fun restoreCustomer(customerId: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try {
            repository.restoreCustomer(customerId)
            _success.value = "Customer restored successfully"
            onSuccess()
        } catch (e: Exception) {
            Log.e("CustomerViewModel", "Error restoring customer", e)
            _error.value = "Failed to restore customer: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun getCustomerByIdFlow(customerId: String): Flow<Customer?> = repository.getCustomerByIdFlow(customerId)

    suspend fun getCustomerById(customerId: String): Customer? {
        return try {
            repository.getCustomerById(customerId)
        } catch (e: Exception) {
            Log.e("CustomerViewModel", "Error getting customer by id", e)
            _error.value = "Failed to load customer details."
            null
        }
    }

    fun getSalesByCustomer(vendorId: String, customerId: String): Flow<List<Sale>> = repository.getSalesByCustomer(vendorId, customerId)

    fun getPaymentsByCustomer(vendorId: String, customerId: String): Flow<List<Payment>> = repository.getPaymentsByCustomer(vendorId, customerId)

    fun getLedgerByCustomer(vendorId: String, customerId: String): Flow<List<LedgerEntry>> = repository.getLedgerByCustomer(vendorId, customerId)

    fun getPaymentLedgerByCustomer(vendorId: String, customerId: String): Flow<List<LedgerEntry>> = repository.getPaymentLedgerByCustomer(vendorId, customerId)

    fun addCustomer(customer: Customer, onSuccess: () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try {
            repository.addCustomer(customer)
            onSuccess()
            _success.value = "Customer added successfully"
        } catch (e: Exception) {
            Log.e("CustomerViewModel", "Error adding customer", e)
            _error.value = "Failed to add customer: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun updateCustomer(customer: Customer, onSuccess: () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try {
            repository.updateCustomer(customer)
            onSuccess()
            _success.value = "Customer updated successfully"
        } catch (e: Exception) {
            Log.e("CustomerViewModel", "Error updating customer", e)
            _error.value = "Failed to update customer: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteCustomer(vendorId: String, customerId: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val archived = repository.deleteCustomer(vendorId, customerId)
            onSuccess()
            _success.value = if (archived) {
                "Customer archived successfully"
            } else {
                "Customer deleted successfully"
            }
        } catch (e: Exception) {
            Log.e("CustomerViewModel", "Error deleting customer", e)
            _error.value = "Failed to delete customer: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun calculateRiskScore(sales: List<Sale>, installments: List<Installment>): String {
        try {
            if (sales.isEmpty()) return "Low Risk"

            val purchaseCount = sales.size
            val totalFinalAmount = sales.sumOf { it.finalAmount }
            val totalPaidAmount = sales.sumOf { it.amountPaid }
            
            val paymentCompletionRatio = if (totalFinalAmount > 0) totalPaidAmount / totalFinalAmount else 1.0
            
            // Check for any overdue installments
            val hasOverdue = installments.any { it.status == "OVERDUE" }
            val latePaymentCount = installments.count { it.status == "OVERDUE" }

            return when {
                latePaymentCount > 2 || (paymentCompletionRatio < 0.4 && purchaseCount > 0) -> "High Risk"
                latePaymentCount > 0 || paymentCompletionRatio < 0.8 -> "Medium Risk"
                else -> "Low Risk"
            }
        } catch (e: Exception) {
            Log.e("CustomerViewModel", "Error calculating risk score", e)
            return "Unknown"
        }
    }
}
