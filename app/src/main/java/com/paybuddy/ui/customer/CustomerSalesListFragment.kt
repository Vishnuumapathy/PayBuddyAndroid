package com.paybuddy.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.CustomerSalesListScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.viewmodel.CustomerViewModel
import com.paybuddy.viewmodel.ViewModelFactory

class CustomerSalesListFragment : Fragment() {

    private lateinit var viewModel: CustomerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val customerId = arguments?.getString("customerId") ?: ""
        
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CustomerViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val vendorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    CustomerSalesListScreen(
                        customerId = customerId,
                        vendorId = vendorId,
                        viewModel = viewModel,
                        onBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
