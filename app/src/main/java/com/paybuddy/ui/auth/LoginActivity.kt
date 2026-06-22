package com.paybuddy.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.databinding.ActivityLoginBinding
import com.paybuddy.ui.dashboard.DashboardActivity
import com.paybuddy.utils.SessionManager
import com.paybuddy.data.model.Vendor
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            sessionManager = SessionManager(this)

            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    private fun setupClickListeners() {
        binding.btnEmailLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateEmailLogin(email, password)) {
                loginWithEmail(email, password)
            }
        }
        
        binding.tvCreateAccount.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateEmailLogin(email, password)) {
                registerWithEmail(email, password)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.tilEmail.error = "Enter your email to reset password"
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter a valid email"
            } else {
                binding.tilEmail.error = null
                sendPasswordResetEmail(email)
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to send reset email"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateEmailLogin(email: String, password: String): Boolean {
        var isValid = true
        
        // Reset errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        return isValid
    }

    private fun loginWithEmail(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkUserRecord("email")
                } else {
                    binding.progressBar.visibility = View.GONE
                    val exception = task.exception
                    Log.e(TAG, "Email login failed", exception)
                    val errorMessage = mapFirebaseAuthException(exception)
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun mapFirebaseAuthException(exception: Exception?): String {
        return when (exception) {
            is com.google.firebase.FirebaseNetworkException -> "Check your internet connection"
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found with this email"
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
            is com.google.firebase.auth.FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Enter a valid email address"
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later"
                    else -> "Login failed. Please try again."
                }
            }
            else -> "Login failed. Please try again."
        }
    }

    private fun registerWithEmail(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkUserRecord("email")
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserRecord(method: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid
        db.collection("vendors").document(userId).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                lifecycleScope.launch {
                    try {
                        if (document.exists()) {
                            val vendor = document.toObject(Vendor::class.java)
                            if (vendor != null) {
                                sessionManager.saveSession(
                                    vendorId = userId,
                                    name = vendor.name,
                                    shopName = vendor.shopName,
                                    upiId = vendor.upiId,
                                    loginMethod = method
                                )
                            }
                            navigateToDashboard()
                        } else {
                            // New user, save session and go to setup
                            sessionManager.saveSession(
                                vendorId = userId,
                                name = "",
                                shopName = "",
                                loginMethod = method
                            )
                            navigateToVendorSetup()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving session", e)
                        Toast.makeText(this@LoginActivity, "Session error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error checking profile", e)
                Toast.makeText(this, "Error checking profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToDashboard() {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToVendorSetup() {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, VendorSetupActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
