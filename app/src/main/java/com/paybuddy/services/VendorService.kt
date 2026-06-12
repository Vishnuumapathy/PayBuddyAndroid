package com.paybuddy.services

import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.model.VendorModel
import kotlinx.coroutines.tasks.await

class VendorService {
    private val db = FirebaseFirestore.getInstance()
    private val vendorsCollection = db.collection("vendors")

    suspend fun getVendorProfile(vendorId: String): VendorModel? {
        return try {
            val document = vendorsCollection.document(vendorId).get().await()
            document.toObject(VendorModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveVendorProfile(vendor: VendorModel): Boolean {
        return try {
            vendorsCollection.document(vendor.vendorId).set(vendor).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
