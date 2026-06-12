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

    fun logout() = auth.signOut()
}
