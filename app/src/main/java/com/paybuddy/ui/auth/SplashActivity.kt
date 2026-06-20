package com.paybuddy.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.paybuddy.databinding.ActivitySplashBinding
import com.paybuddy.ui.dashboard.DashboardActivity
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var _binding: ActivitySplashBinding? = null
    private val binding get() = _binding!!
    private val TAG = "SplashActivity"
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep the splash screen on-screen until we are ready to navigate
        splashScreen.setKeepOnScreenCondition { !isReady }

        Log.d(TAG, "Splash Activity Started")
        
        try {
            _binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Reveal the activity layout by dismissing the system splash overlay
            isReady = true

            // Start authentication check
            checkAuthAndNavigate()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            isReady = true
            navigateToLogin()
        }
    }

    private fun checkAuthAndNavigate() {
        lifecycleScope.launch {
            try {
                // Ensure a minimum splash visibility for brand identity
                delay(1500)
                
                Log.d(TAG, "Starting auth check...")
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                val sessionManager = SessionManager(this@SplashActivity)
                
                // Timeout added to prevent hanging on DataStore or internal logic
                val isLoggedIn = withTimeoutOrNull(3000) {
                    sessionManager.isLoggedIn()
                } ?: false
                
                Log.d(TAG, "Auth check: uid=${currentUser?.uid}, isLoggedIn=$isLoggedIn")

                if (currentUser != null && isLoggedIn) {
                    Log.d(TAG, "Proceeding to Dashboard")
                    navigateToDashboard()
                } else {
                    Log.d(TAG, "Proceeding to Login")
                    navigateToLogin()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auth check failed", e)
                navigateToLogin()
            } finally {
                isReady = true
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
