package com.paybuddy.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.paybuddy.R
import com.paybuddy.services.AuthService
import com.paybuddy.services.VendorService
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private lateinit var authService: AuthService
    private lateinit var vendorService: VendorService
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        authService = AuthService()
        vendorService = VendorService()
        sessionManager = SessionManager(requireContext())

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            
            val isLoggedIn = authService.isLoggedIn()
            var destinationId = R.id.action_splash_to_login

            if (isLoggedIn) {
                val vendorId = authService.getCurrentUser()?.uid
                if (vendorId != null) {
                    // Check if session exists
                    if (sessionManager.isLoggedIn()) {
                        destinationId = R.id.action_splash_to_dashboard
                    } else {
                        // Fetch from Firebase
                        val vendor = vendorService.getVendorProfile(vendorId)
                        if (vendor != null) {
                            sessionManager.saveSession(
                                vendorId = vendor.vendorId,
                                name = vendor.name,
                                shopName = vendor.shopName,
                                loginMethod = "firebase"
                            )
                            destinationId = R.id.action_splash_to_dashboard
                        } else {
                            destinationId = R.id.action_splash_to_onboarding
                        }
                    }
                }
            }

            // Ensure splash stays for at least 2 seconds
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 2000) {
                delay(2000 - elapsedTime)
            }
            
            try {
                findNavController().navigate(destinationId)
            } catch (e: Exception) {
                // Fallback in case of navigation error
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }
    }
}
