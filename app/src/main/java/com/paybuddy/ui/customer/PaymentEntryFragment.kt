package com.paybuddy.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.PaymentEntryScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.viewmodel.SalesViewModel
import com.paybuddy.viewmodel.ViewModelFactory

class PaymentEntryFragment : Fragment() {

    private lateinit var viewModel: SalesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val customerId = arguments?.getString("customerId") ?: ""
        val saleId = arguments?.getString("saleId")
        
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SalesViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val vendorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val installments by (if (!saleId.isNullOrEmpty()) {
                        viewModel.getInstallmentsBySale(vendorId, saleId)
                    } else {
                        viewModel.getInstallmentsByCustomer(vendorId, customerId)
                    }).collectAsState(initial = emptyList())
                    
                    PaymentEntryScreen(
                        viewModel = viewModel,
                        customerId = customerId,
                        saleId = saleId,
                        vendorId = vendorId,
                        installments = installments.filter { it.status != "PAID" },
                        onPaymentRecorded = { findNavController().popBackStack() },
                        onBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
