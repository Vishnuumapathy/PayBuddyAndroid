package com.paybuddy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paybuddy.data.model.Installment
import com.paybuddy.data.model.LedgerEntry
import com.paybuddy.data.model.Sale
import com.paybuddy.data.repository.MainRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(private val repository: MainRepository) : ViewModel() {

    private val _totalOutstanding = MutableStateFlow(0.0)
    val totalOutstanding: StateFlow<Double> = _totalOutstanding

    private val _todayDue = MutableStateFlow(0.0)
    val todayDue: StateFlow<Double> = _todayDue

    private val _overdue = MutableStateFlow(0.0)
    val overdue: StateFlow<Double> = _overdue

    private val _upcoming = MutableStateFlow(0.0)
    val upcoming: StateFlow<Double> = _upcoming

    private val _recentActivity = MutableStateFlow<List<LedgerEntry>>(emptyList())
    val recentActivity: StateFlow<List<LedgerEntry>> = _recentActivity

    private val _allInstallments = MutableStateFlow<List<Installment>>(emptyList())
    val allInstallments: StateFlow<List<Installment>> = _allInstallments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var dashboardJob: Job? = null
    private val TAG = "HomeViewModel"

    fun loadDashboardData(vendorId: String) {
        val trimmedVendorId = vendorId.trim()
        if (trimmedVendorId.isEmpty()) {
            Log.e(TAG, "loadDashboardData: Critical failure - vendorId is empty")
            _errorMessage.value = "Critical Error: Vendor ID is missing"
            return
        }
        
        Log.d(TAG, "loadDashboardData: Starting for vendor '$trimmedVendorId'")
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            _isLoading.value = true
            // Clear previous error when starting a fresh load
            _errorMessage.value = null
            
            val summaryLoaded = MutableStateFlow(false)
            val activityLoaded = MutableStateFlow(false)

            // Monitor loading state
            launch {
                combine(summaryLoaded, activityLoaded) { s, a ->
                    s && a
                }.collect { allLoaded ->
                    if (allLoaded) {
                        Log.d(TAG, "loadDashboardData: All flows initialized (Summary=$summaryLoaded, Activity=$activityLoaded)")
                        _isLoading.value = false
                    }
                }
            }

            // 1. Collect Installments and Sales for Summary
            launch {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val today = calendar.timeInMillis
                val endOfToday = today + 86400000L

                Log.d(TAG, "SummaryFlow: Starting for vendor $trimmedVendorId")
                combine(
                    repository.getAllInstallments(trimmedVendorId),
                    repository.getSalesByVendor(trimmedVendorId)
                ) { installments, sales ->
                    Log.d(TAG, "SummaryFlow: Received ${installments.size} installments and ${sales.size} sales")
                    
                    val activeSaleIds = sales.map { it.saleId }.toSet()
                    val pendingSales = sales.filter { it.status != "COMPLETED" && it.remainingAmount > 0.01 }
                    val totalOut = pendingSales.sumOf { it.remainingAmount }
                    
                    val completedSaleIds = sales.filter { it.status == "COMPLETED" || it.remainingAmount <= 0.01 }
                        .map { it.saleId }.toSet()
                    
                    val filteredInstallments = installments.filter { 
                        it.saleId in activeSaleIds &&
                        it.saleId !in completedSaleIds && 
                        it.status != "PAID" && 
                        it.remainingAmount > 0.01 
                    }
                    
                    Triple(totalOut, filteredInstallments, sales)
                }
                .catch { e ->
                    Log.e(TAG, "SummaryFlow: Error in installments/sales flow for vendor $trimmedVendorId", e)
                    _errorMessage.value = "Error loading summaries: ${e.message}"
                    summaryLoaded.value = true
                }
                .collect { (totalOut, filteredInstallments, _) ->
                    _totalOutstanding.value = totalOut
                    _allInstallments.value = filteredInstallments
                    
                    _todayDue.value = filteredInstallments.filter { it.dueDate in today until endOfToday }.sumOf { it.remainingAmount }
                    _overdue.value = filteredInstallments.filter { it.status == "OVERDUE" }.sumOf { it.remainingAmount }
                    _upcoming.value = filteredInstallments.filter { it.dueDate >= endOfToday && it.status != "OVERDUE" }.sumOf { it.remainingAmount }
                    
                    // If summary succeeds, clear any previous summary-specific error
                    if (_errorMessage.value?.startsWith("Error loading summaries") == true) {
                        _errorMessage.value = null
                    }
                    
                    Log.d(TAG, "SummaryFlow: Updated - TotalOut=$totalOut, Today=${_todayDue.value}, Overdue=${_overdue.value}, Upcoming=${_upcoming.value}")
                    summaryLoaded.value = true
                }
            }

            // 2. Collect Recent Sales
            launch {
                Log.d(TAG, "SalesFlow: Starting query for vendor $trimmedVendorId")
                repository.getRecentSales(trimmedVendorId)
                    .onStart { Log.d(TAG, "SalesFlow: Recent sales query started") }
                    .catch { e ->
                        Log.e(TAG, "SalesFlow: Query failure for vendor $trimmedVendorId", e)
                    }
                    .collect { sales ->
                        Log.d(TAG, "SalesFlow: Updated. Received ${sales.size} entries")
                        _recentActivity.value = sales
                        activityLoaded.value = true
                    }
            }
        }
    }
    
    fun clearError() {
        Log.d(TAG, "clearError: Error banner manually dismissed")
        _errorMessage.value = null
    }
}
