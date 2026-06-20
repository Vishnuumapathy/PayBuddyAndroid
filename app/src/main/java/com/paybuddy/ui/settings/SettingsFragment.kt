package com.paybuddy.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.paybuddy.R
import com.paybuddy.ui.auth.LoginActivity
import com.paybuddy.ui.screens.SettingsScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { findNavController().popBackStack() },
                        onBusinessProfileClick = {
                            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                                findNavController().navigate(R.id.action_settings_to_businessProfile)
                            }
                        },
                        onSecurityNotificationsClick = {
                            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                                findNavController().navigate(R.id.action_settings_to_securityNotifications)
                            }
                        },
                        onArchivedRecordsClick = {
                            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                                findNavController().navigate(R.id.action_settings_to_archivedRecords)
                            }
                        },
                        onLogoutSuccess = {
                            handleExit()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetSuccess.collect { success ->
                if (success) {
                    handleExit()
                }
            }
        }
    }

    private fun handleExit() {
        Log.d("SettingsFragment", "handleExit: Starting logout process")
        val sessionManager = SessionManager(requireContext())
        
        // 1. Immediate sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // 2. Clear session and navigate
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SettingsFragment", "handleExit: Clearing session...")
                sessionManager.clearSession()
                Log.d("SettingsFragment", "handleExit: Session cleared. Navigating to Login.")
                
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                
                requireActivity().finish()
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error during logout", e)
                // Fallback: still try to navigate to Login
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun navigateToLogin() {
        handleExit()
    }
}
