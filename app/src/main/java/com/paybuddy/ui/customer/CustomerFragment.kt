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
import com.paybuddy.R
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.CustomerListScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.viewmodel.CustomerViewModel
import com.paybuddy.viewmodel.ViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore

class CustomerFragment : Fragment() {

    private lateinit var viewModel: CustomerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CustomerViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val vendorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    CustomerListScreen(
                        viewModel = viewModel,
                        vendorId = vendorId,
                        onCustomerClick = { customerId ->
                            val bundle = Bundle().apply { putString("customerId", customerId) }
                            findNavController().navigate(R.id.action_customerFragment_to_customerProfileFragment, bundle)
                        },
                        onAddCustomerClick = {
                            findNavController().navigate(R.id.action_customerFragment_to_addCustomerFragment)
                        }
                    )
                }
            }
        }
    }
}
