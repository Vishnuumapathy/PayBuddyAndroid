package com.paybuddy.ui.auth

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
import com.paybuddy.R
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.OnboardingScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.utils.SessionManager
import com.paybuddy.viewmodel.VendorViewModel
import com.paybuddy.viewmodel.ViewModelFactory

class OnboardingFragment : Fragment() {

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

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val auth = FirebaseAuth.getInstance()
                    OnboardingScreen(
                        viewModel = viewModel,
                        vendorId = auth.currentUser?.uid ?: "",
                        initialEmail = auth.currentUser?.email ?: "",
                        initialPhone = "",
                        onBack = { findNavController().popBackStack() },
                        onSuccess = {
                            findNavController().navigate(R.id.action_onboarding_to_dashboard)
                        }
                    )
                }
            }
        }
    }
}
