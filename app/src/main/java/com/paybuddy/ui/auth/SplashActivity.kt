package com.paybuddy.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.paybuddy.databinding.ActivitySplashBinding
import com.paybuddy.ui.dashboard.DashboardActivity
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var _binding: ActivitySplashBinding? = null
    private val binding get() = _binding!!
    private val SPLASH_DELAY = 2000L // 2 seconds minimum delay
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        Log.d(TAG, "Splash started")
        
        try {
            // Remove any window background manually set to avoid white flash
            window.setBackgroundDrawable(null)

            _binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Add simple fade-in animation for logo and text
            binding.ivLogo.alpha = 0f
            binding.tvAppName.alpha = 0f
            binding.tvSubtitle.alpha = 0f

            binding.ivLogo.animate().alpha(1f).setDuration(1000).start()
            binding.tvAppName.animate().alpha(1f).setDuration(1000).setStartDelay(300).start()
            binding.tvSubtitle.animate().alpha(1f).setDuration(1000).setStartDelay(600).start()

            // Start navigation check after the specified delay
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    checkAuthAndNavigate()
                }
            }, SPLASH_DELAY)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            navigateToLogin()
        }
    }

    private fun checkAuthAndNavigate() {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val sessionManager = SessionManager(this@SplashActivity)
                val isLoggedIn = sessionManager.isLoggedIn()
                
                Log.d(TAG, "Auth check: currentUser=${currentUser?.uid}, isLoggedIn=$isLoggedIn")

                if (currentUser != null && isLoggedIn) {
                    Log.d(TAG, "Navigating to Dashboard")
                    navigateToDashboard()
                } else {
                    Log.d(TAG, "Navigating to Login: currentUser is null or session not logged in")
                    navigateToLogin()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during auth check", e)
                navigateToLogin()
            }
        }
    }

    private fun navigateToDashboard() {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
