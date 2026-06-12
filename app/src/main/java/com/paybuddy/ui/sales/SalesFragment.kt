package com.paybuddy.ui.sales

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
import com.paybuddy.R
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.SalesHistoryScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.viewmodel.SalesViewModel
import com.paybuddy.viewmodel.ViewModelFactory

class SalesFragment : Fragment() {

    private lateinit var viewModel: SalesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SalesViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val vendorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val customers by viewModel.getCustomers(vendorId).collectAsState(initial = emptyList())
                    
                    SalesHistoryScreen(
                        viewModel = viewModel,
                        vendorId = vendorId,
                        customers = customers,
                        onAddSaleClick = {
                            findNavController().navigate(R.id.action_salesFragment_to_newSaleFragment)
                        },
                        onSaleClick = { saleId ->
                            val bundle = Bundle().apply { putString("saleId", saleId) }
                            findNavController().navigate(R.id.action_salesFragment_to_saleDetailFragment, bundle)
                        }
                    )
                }
            }
        }
    }
}
