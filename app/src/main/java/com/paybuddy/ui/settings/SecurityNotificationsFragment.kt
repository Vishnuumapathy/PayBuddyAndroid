package com.paybuddy.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.paybuddy.ui.screens.SecurityNotificationsScreen
import com.paybuddy.ui.theme.PayBuddyTheme

class SecurityNotificationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    SecurityNotificationsScreen(
                        onBackClick = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
