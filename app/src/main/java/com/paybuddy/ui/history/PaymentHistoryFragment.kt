package com.paybuddy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.viewmodel.PaymentHistoryViewModel

class PaymentHistoryFragment : Fragment() {

    private lateinit var viewModel: PaymentHistoryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PaymentHistoryViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[PaymentHistoryViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val payments by viewModel.filteredPayments.collectAsState()
                    val searchQuery by viewModel.searchQuery.collectAsState()
                    val isLoading by viewModel.isLoading.collectAsState()

                    PaymentHistoryScreen(
                        payments = payments,
                        searchQuery = searchQuery,
                        isLoading = isLoading,
                        onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { vendorId ->
            viewModel.loadPaymentHistory(vendorId)
        }
    }
}
