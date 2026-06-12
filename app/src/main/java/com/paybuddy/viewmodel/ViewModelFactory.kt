package com.paybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.utils.SessionManager

class ViewModelFactory(
    private val repository: MainRepository,
    private val sessionManager: SessionManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.paybuddy.ui.reminders.ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.paybuddy.ui.reminders.ReminderViewModel(repository, sessionManager!!) as T
        }
        if (modelClass.isAssignableFrom(VendorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VendorViewModel(repository, sessionManager!!) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
