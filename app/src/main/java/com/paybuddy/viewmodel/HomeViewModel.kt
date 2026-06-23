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
            _errorMessage.value = "Critical Error: Vendor ID is missing"
            return
        }
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val summaryLoaded = MutableStateFlow(false)
            val activityLoaded = MutableStateFlow(false)

            launch {
                combine(summaryLoaded, activityLoaded) { s, a -> s && a }.collect { allLoaded -> if (allLoaded) _isLoading.value = false }
            }

            launch {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val today = cal.timeInMillis
                val endOfToday = today + 86400000L

                combine(repository.getAllInstallments(trimmedVendorId), repository.getSalesByVendor(trimmedVendorId)) { inst, sales ->
                    val activeSaleIds = sales.map { it.saleId }.toSet()
                    val totalOut = sales.filter { it.status != "COMPLETED" && it.remainingAmount > 0.01 }.sumOf { it.remainingAmount }
                    val completedSaleIds = sales.filter { it.status == "COMPLETED" || it.remainingAmount <= 0.01 }.map { it.saleId }.toSet()
                    val filteredInst = inst.filter { it.saleId in activeSaleIds && it.saleId !in completedSaleIds && it.status != "PAID" && it.remainingAmount > 0.01 }
                    Triple(totalOut, filteredInst, sales)
                }.catch { e ->
                    _errorMessage.value = "Error loading summaries: ${e.message}"
                    summaryLoaded.value = true
                }.collect { (totalOut, filteredInst, _) ->
                    _totalOutstanding.value = totalOut
                    _allInstallments.value = filteredInst
                    _todayDue.value = filteredInst.filter { it.dueDate in today until endOfToday }.sumOf { it.remainingAmount }
                    _overdue.value = filteredInst.filter { it.status == "OVERDUE" }.sumOf { it.remainingAmount }
                    _upcoming.value = filteredInst.filter { it.dueDate >= endOfToday && it.status != "OVERDUE" }.sumOf { it.remainingAmount }
                    summaryLoaded.value = true
                }
            }

            launch {
                repository.getRecentSales(trimmedVendorId).catch { e -> }.collect { sales ->
                    _recentActivity.value = sales
                    activityLoaded.value = true
                }
            }
        }
    }
    
    fun clearError() { _errorMessage.value = null }
}
