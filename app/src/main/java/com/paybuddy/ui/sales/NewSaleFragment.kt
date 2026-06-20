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
import com.paybuddy.ui.screens.NewSaleScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.viewmodel.CustomerViewModel
import com.paybuddy.viewmodel.SalesViewModel
import com.paybuddy.viewmodel.ViewModelFactory

class NewSaleFragment : Fragment() {

    private lateinit var salesViewModel: SalesViewModel
    private lateinit var customerViewModel: CustomerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        val factory = ViewModelFactory(repository)
        
        salesViewModel = ViewModelProvider(this, factory)[SalesViewModel::class.java]
        customerViewModel = ViewModelProvider(this, factory)[CustomerViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val vendorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val customers by customerViewModel.getCustomers(vendorId).collectAsState(initial = emptyList())
                    
                    NewSaleScreen(
                        viewModel = salesViewModel,
                        vendorId = vendorId,
                        customers = customers,
                        onSaleCreated = {
                            findNavController().popBackStack()
                        },
                        onAddCustomerClick = {
                            findNavController().navigate(R.id.action_newSaleFragment_to_addCustomerFragment)
                        },
                        onBack = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }
}
