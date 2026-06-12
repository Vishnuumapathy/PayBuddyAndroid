package com.paybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paybuddy.data.model.LedgerEntry
import com.paybuddy.data.repository.MainRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryViewModel(private val repository: MainRepository) : ViewModel() {

    private val _paymentHistory = MutableStateFlow<List<LedgerEntry>>(emptyList())
    val paymentHistory: StateFlow<List<LedgerEntry>> = _paymentHistory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val filteredPayments: StateFlow<List<LedgerEntry>> = combine(
        _paymentHistory,
        _searchQuery
    ) { payments, query ->
        if (query.isBlank()) {
            payments
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            payments.filter { payment ->
                val dateText = formatDate(payment.createdAt).lowercase(Locale.getDefault())
                val amountText = "₹${String.format(Locale.getDefault(), "%.2f", payment.amount)}".lowercase(Locale.getDefault())
                
                payment.customerName.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                (payment.itemName.lowercase(Locale.getDefault()).contains(lowerQuery)) ||
                amountText.contains(lowerQuery) ||
                dateText.contains(lowerQuery)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadPaymentHistory(vendorId: String) {
        if (vendorId.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPaymentHistory(vendorId)
                .collect { payments ->
                    _paymentHistory.value = payments
                    _isLoading.value = false
                }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
}
