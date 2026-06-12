package com.paybuddy.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.R
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.DashboardScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.utils.SessionManager
import com.paybuddy.viewmodel.HomeViewModel

class DashboardFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var sessionManager: SessionManager
    private val TAG = "DashboardFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            val context = requireContext()
            val db = FirebaseFirestore.getInstance()
            val repository = MainRepository(db)
            
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(repository) as T
                }
            }
            viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
            sessionManager = SessionManager(context)

            return ComposeView(context).apply {
                setContent {
                    PayBuddyTheme {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val vendorId = currentUser?.uid ?: ""
                        
                        var vendorName by remember { mutableStateOf("") }
                        var shopName by remember { mutableStateOf("") }
                        
                        LaunchedEffect(vendorId) {
                            if (vendorId.isNotEmpty()) {
                                try {
                                    vendorName = sessionManager.getVendorName() ?: ""
                                    shopName = sessionManager.getShopName() ?: ""
                                    viewModel.loadDashboardData(vendorId)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading session data", e)
                                }
                            } else {
                                Log.w(TAG, "Vendor ID is empty in DashboardFragment")
                            }
                        }

                        val todayDue by viewModel.todayDue.collectAsState()
                        val overdue by viewModel.overdue.collectAsState()
                        val upcoming by viewModel.upcoming.collectAsState()
                        val totalOutstanding by viewModel.totalOutstanding.collectAsState()
                        val recentActivity by viewModel.recentActivity.collectAsState()
                        val isLoading by viewModel.isLoading.collectAsState()
                        val errorMessage by viewModel.errorMessage.collectAsState()

                        DashboardScreen(
                            vendorName = vendorName,
                            shopName = shopName,
                            todayDue = todayDue,
                            overdue = overdue,
                            upcoming = upcoming,
                            totalOutstanding = totalOutstanding,
                            recentActivity = recentActivity,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onNewSaleClick = {
                                safeNavigate(R.id.action_dashboard_to_newSale)
                            },
                            onRecordPaymentClick = {
                                safeNavigate(R.id.customerFragment)
                            },
                            onRemindersClick = {
                                safeNavigate(R.id.reminderFragment)
                            },
                            onSettingsClick = {
                                safeNavigate(R.id.settingsFragment)
                            },
                            onErrorDismiss = {
                                viewModel.clearError()
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView", e)
            return View(requireContext()) // Fallback empty view
        }
    }

    private fun safeNavigate(destinationId: Int) {
        try {
            val navController = findNavController()
            val currentDestination = navController.currentDestination
            if (currentDestination?.id == R.id.dashboardFragment) {
                navController.navigate(destinationId)
            } else {
                Log.w(TAG, "Skipping navigation: current destination is ${currentDestination?.label}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error", e)
        }
    }
}
