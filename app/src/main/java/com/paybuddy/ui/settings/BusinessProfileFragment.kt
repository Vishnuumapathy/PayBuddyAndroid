package com.paybuddy.ui.settings

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
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.BusinessProfileScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.utils.SessionManager
import com.paybuddy.viewmodel.VendorViewModel
import com.paybuddy.viewmodel.ViewModelFactory

class BusinessProfileFragment : Fragment() {

    private lateinit var viewModel: VendorViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val repository = MainRepository(FirebaseFirestore.getInstance())
        val sessionManager = SessionManager(requireContext())
        val factory = ViewModelFactory(repository, sessionManager)
        viewModel = ViewModelProvider(this, factory)[VendorViewModel::class.java]

        viewModel.loadVendorProfile()

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val vendor by viewModel.currentVendor.collectAsState()
                    BusinessProfileScreen(
                        vendor = vendor,
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
