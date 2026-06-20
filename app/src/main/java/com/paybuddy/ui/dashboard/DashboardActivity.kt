package com.paybuddy.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.paybuddy.R
import com.paybuddy.databinding.ActivityDashboardBinding
import com.paybuddy.ui.auth.LoginActivity
import com.paybuddy.utils.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class DashboardActivity : AppCompatActivity() {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val TAG = "DashboardActivity"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.w(TAG, "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Dashboard onCreate")
        
        try {
            _binding = ActivityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            sessionManager = SessionManager(this)
            
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            val navController = navHostFragment?.navController
            
            if (navController != null) {
                binding.bottomNav.setupWithNavController(navController)
            } else {
                Log.e(TAG, "NavController not found")
            }

            lifecycleScope.launch {
                try {
                    // Authentication Guard
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val isLoggedIn = withTimeoutOrNull(3000) {
                        sessionManager.isLoggedIn()
                    } ?: false

                    Log.d(TAG, "Auth Guard: currentUser=${currentUser?.uid}, isLoggedIn=$isLoggedIn")
                    
                    if (currentUser == null || !isLoggedIn) {
                        Log.w(TAG, "Session invalid, redirecting to Login")
                        navigateToLogin()
                    } else {
                        checkNotificationPermission()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in session check", e)
                    navigateToLogin()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate", e)
            navigateToLogin()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                val alreadyRequested = sessionManager.isNotificationPermissionRequested()
                val isGranted = ContextCompat.checkSelfPermission(
                    this@DashboardActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (!isGranted && !alreadyRequested) {
                    showNotificationPermissionExplanation()
                }
            }
        }
    }

    private fun showNotificationPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Enable Reminders")
            .setMessage("PayBuddy uses notifications to remind you about due and overdue payments.")
            .setPositiveButton("Allow") { _, _ ->
                lifecycleScope.launch {
                    sessionManager.setNotificationPermissionRequested(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            .setNegativeButton("Not Now") { _, _ ->
                lifecycleScope.launch {
                    sessionManager.setNotificationPermissionRequested(true)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToLogin() {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
