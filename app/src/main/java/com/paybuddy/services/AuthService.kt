package com.paybuddy.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun checkAuthState(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }

    fun loginWithEmail() {
        // Handled via signInWithEmailAndPassword in LoginActivity
    }
}
