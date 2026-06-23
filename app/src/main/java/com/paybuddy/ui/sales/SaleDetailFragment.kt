package com.paybuddy.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.SaleDetailScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.viewmodel.SalesViewModel
import com.paybuddy.viewmodel.ViewModelFactory
import com.paybuddy.R

class SaleDetailFragment : Fragment() {

    private lateinit var viewModel: SalesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val saleId = arguments?.getString("saleId") ?: ""
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SalesViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    SaleDetailScreen(
                        saleId = saleId,
                        viewModel = viewModel,
                        onRecordPayment = { custId, sId ->
                            val bundle = Bundle().apply {
                                putString("customerId", custId)
                                putString("saleId", sId)
                            }
                            findNavController().navigate(R.id.action_saleDetailFragment_to_paymentEntryFragment, bundle)
                        },
                        onBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
