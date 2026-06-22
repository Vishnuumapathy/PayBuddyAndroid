package com.paybuddy.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.paybuddy.utils.Resource
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {

    fun getCurrentUser() = auth.currentUser

    suspend fun loginWithEmail(email: String, pass: String): Resource<String> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Resource.Success("Login Successful")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login Failed")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Resource<String> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success("Reset link sent to your email")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send reset email")
        }
    }

    fun logout() = auth.signOut()
}
