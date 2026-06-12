package com.paybuddy.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.auth.FirebaseAuth
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.dashboard.DashboardActivity
import com.paybuddy.ui.screens.OnboardingScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.utils.SessionManager
import com.paybuddy.viewmodel.VendorViewModel
import com.google.firebase.firestore.FirebaseFirestore

class VendorSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        val sessionManager = SessionManager(this)
        
        val viewModel = VendorViewModel(repository, sessionManager)

        setContent {
            PayBuddyTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    vendorId = auth.currentUser?.uid ?: "",
                    initialEmail = auth.currentUser?.email ?: "",
                    initialPhone = "",
                    onBack = { finish() },
                    onSuccess = {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
