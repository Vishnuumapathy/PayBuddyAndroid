package com.paybuddy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paybuddy.data.model.*
import com.paybuddy.data.repository.MainRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SalesUiState {
    object Loading : SalesUiState()
    data class Success(val sales: List<Sale>) : SalesUiState()
    object Empty : SalesUiState()
    data class Error(val message: String) : SalesUiState()
}

class SalesViewModel(private val repository: MainRepository) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _salesUiState = MutableStateFlow<SalesUiState>(SalesUiState.Loading)
    val salesUiState: StateFlow<SalesUiState> = _salesUiState.asStateFlow()

    val activeSales: StateFlow<List<Sale>> = _salesUiState
        .map { state ->
            if (state is SalesUiState.Success) {
                state.sales.filter { it.status == "PENDING" || it.remainingAmount > 0.01 }
            } else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historySales: StateFlow<List<Sale>> = _salesUiState
        .map { state ->
            if (state is SalesUiState.Success) {
                state.sales.filter { it.status == "COMPLETED" || it.remainingAmount <= 0.01 }
            } else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentVendorId: String? = null
    private var salesJob: kotlinx.coroutines.Job? = null

    fun getSaleById(saleId: String): Flow<Sale?> = repository.getSaleByIdFlow(saleId)
    
    fun getCustomerById(customerId: String): Flow<Customer?> = repository.getCustomerByIdFlow(customerId)

    fun clearError() {
        _error.value = null
    }

    fun loadSales(vendorId: String) {
        val trimmedId = vendorId.trim()
        if (currentVendorId == trimmedId && salesJob?.isActive == true) {
            Log.d("SalesViewModel", "loadSales: Already observing for vendorId: '$trimmedId'")
            return
        }
        
        currentVendorId = trimmedId
        salesJob?.cancel()

        if (trimmedId.isBlank()) {
            Log.w("SalesViewModel", "loadSales: vendorId is blank")
            _salesUiState.value = SalesUiState.Empty
            return
        }

        salesJob = viewModelScope.launch {
            Log.d("SalesViewModel", "loadSales: Starting fetch for vendorId: '$trimmedId'")
            
            // Only set loading if we don't have data or if it's a new vendor
            if (_salesUiState.value !is SalesUiState.Success) {
                _salesUiState.value = SalesUiState.Loading
            }

            repository.getSalesByVendor(trimmedId)
                .catch { e ->
                    Log.e("SalesViewModel", "loadSales: Error in flow", e)
                    _salesUiState.value = SalesUiState.Error(e.message ?: "Failed to load sales")
                }
                .collect { list ->
                    Log.d("SalesViewModel", "loadSales: Collected ${list.size} sales for '$trimmedId'")
                    if (list.isEmpty()) {
                        _salesUiState.value = SalesUiState.Empty
                    } else {
                        _salesUiState.value = SalesUiState.Success(list)
                    }
                }
        }
    }

    fun getSales(vendorId: String): Flow<List<Sale>> = repository.getSalesByVendor(vendorId)
    
    fun getArchivedSales(vendorId: String): Flow<List<Sale>> = repository.getArchivedSales(vendorId)

    fun restoreSale(saleId: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _isProcessing.value = true
        try {
            repository.restoreSale(saleId)
            _success.value = "Sale restored successfully"
            onSuccess()
        } catch (e: Exception) {
            Log.e("SalesViewModel", "Error restoring sale", e)
            _error.value = "Failed to restore sale: ${e.message}"
        } finally {
            _isProcessing.value = false
        }
    }

    fun getCustomers(vendorId: String): Flow<List<Customer>> = repository.getAllCustomers(vendorId)

    fun createSale(
        itemName: String,
        quantity: Int,
        unitPrice: Double,
        totalAmount: Double,
        interestRate: Double,
        installmentCount: Int,
        paymentType: String,
        amountPaid: Double,
        customerId: String,
        customerName: String,
        vendorId: String,
        installments: List<Installment>,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        if (vendorId.isBlank()) {
            _error.value = "Vendor session expired. Please login again."
            return@launch
        }
        
        try {
            _isProcessing.value = true
            val saleId = "SALE_${System.currentTimeMillis()}"
            
            // Implementation Rules computation:
            // IF FULL payment: finalAmount = totalAmount, amountPaid = finalAmount, remaining = 0, status = COMPLETED
            // IF PARTIAL payment: finalAmount = totalAmount + (totalAmount * interestRate / 100)
            // remaining = finalAmount - amountPaid
            // IF remaining <= 0 -> COMPLETED, ELSE -> PENDING
            
            val finalAmount = if (paymentType == "Full Payment") {
                totalAmount
            } else {
                totalAmount + (totalAmount * interestRate / 100)
            }
            
            // Safety Guard: Never persist overpayment, negative values, or invalid installment count
            val validatedAmountPaid = amountPaid.coerceIn(0.0, finalAmount)
            
            // Data Consistency: Ensure installmentCount matches the generated installments list
            val validatedInstallmentCount = if (paymentType == "Full Payment") 1 else installments.size.coerceAtLeast(1)
            
            // Ensure status is consistent with UI logic
            val status = if (finalAmount - validatedAmountPaid <= 0.01) "COMPLETED" else "PENDING"

            val sale = Sale(
                saleId = saleId,
                itemName = itemName,
                quantity = quantity,
                unitPrice = unitPrice,
                totalAmount = totalAmount,
                interestRate = interestRate,
                installmentCount = validatedInstallmentCount,
                paymentType = paymentType,
                amountPaid = validatedAmountPaid,
                status = status,
                customerId = customerId,
                customerName = customerName,
                vendorId = vendorId
            )
            
            val updatedInstallments = installments.map { it.copy(saleId = saleId) }
            repository.createSale(sale, updatedInstallments)
            onSuccess()
        } catch (e: Exception) {
            Log.e("SalesViewModel", "Error creating sale", e)
            _error.value = "Failed to create sale: ${e.message}"
        } finally {
            _isProcessing.value = false
        }
    }

    fun updateSale(oldSale: Sale, newSale: Sale, onSuccess: () -> Unit) = viewModelScope.launch {
        try {
            _isProcessing.value = true
            repository.updateSale(oldSale, newSale)
            onSuccess()
        } catch (e: Exception) {
            Log.e("SalesViewModel", "Error updating sale", e)
            _error.value = "Failed to update sale: ${e.message}"
        } finally {
            _isProcessing.value = false
        }
    }

    fun deleteSale(sale: Sale, onSuccess: () -> Unit) = viewModelScope.launch {
        try {
            _isProcessing.value = true
            val archived = repository.deleteSale(sale)
            onSuccess()
            _success.value = if (archived) {
                "Sale archived successfully"
            } else {
                "Sale deleted successfully"
            }
        } catch (e: Exception) {
            Log.e("SalesViewModel", "Error deleting sale", e)
            _error.value = "Failed to delete sale: ${e.message}"
        } finally {
            _isProcessing.value = false
        }
    }

    fun clearSuccess() {
        _success.value = null
    }

    fun recordPayment(payment: Payment, onSuccess: () -> Unit) = viewModelScope.launch {
        try {
            _isProcessing.value = true
            repository.recordPayment(payment)
            onSuccess()
        } catch (e: Exception) {
            Log.e("SalesViewModel", "Error recording payment", e)
            _error.value = "Failed to record payment: ${e.message}"
        } finally {
            _isProcessing.value = false
        }
    }

    fun getInstallmentsBySale(vendorId: String, saleId: String): Flow<List<Installment>> = repository.getInstallmentsBySale(vendorId, saleId)
    
    fun getInstallmentsByCustomer(vendorId: String, customerId: String): Flow<List<Installment>> = repository.getInstallmentsByCustomer(vendorId, customerId)
}
