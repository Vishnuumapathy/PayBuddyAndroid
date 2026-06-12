package com.paybuddy.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import android.util.Log

class DeveloperResetRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "DeveloperResetRepo"

    suspend fun resetVendorData(vendorId: String): Result<Unit> {
        if (vendorId.isBlank()) return Result.failure(Exception("Invalid Vendor ID"))

        val collections = listOf("customers", "sales", "installments", "payments", "ledger", "reminders")
        
        return try {
            for (collectionName in collections) {
                deleteCollectionWithFilter(collectionName, vendorId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during reset: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun deleteCollectionWithFilter(collectionName: String, vendorId: String) {
        while (true) {
            val snapshot = firestore.collection(collectionName)
                .whereEqualTo("vendorId", vendorId)
                .limit(500)
                .get()
                .await()

            if (snapshot.isEmpty) break

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            if (snapshot.size() < 500) break
        }
    }
}
