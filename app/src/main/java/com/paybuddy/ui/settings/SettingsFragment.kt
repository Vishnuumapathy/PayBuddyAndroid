package com.paybuddy.ui.settings

import android.content.Intent
import android.os.Bundle
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
        val sessionManager = SessionManager(requireContext())
        
        // Start target activity first
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Then clean up
        lifecycleScope.launch {
            FirebaseAuth.getInstance().signOut()
            sessionManager.clearSession()
            requireActivity().finish()
        }
    }

    private fun navigateToLogin() {
        handleExit()
    }
}
