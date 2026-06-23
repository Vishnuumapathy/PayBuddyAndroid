package com.paybuddy.data.repository

import android.util.Log
import com.paybuddy.data.model.*
import com.paybuddy.data.util.InstallmentMapper
import com.paybuddy.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class MainRepository(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "MainRepository"

    // Vendor
    suspend fun getVendor(vendorId: String): Vendor? {
        if (vendorId.isEmpty()) return null
        return try {
            firestore.collection("vendors").document(vendorId).get().await().toObject(Vendor::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching vendor: ${e.message}")
            null
        }
    }

    suspend fun saveVendor(vendor: Vendor) {
        try {
            firestore.collection("vendors").document(vendor.vendorId).set(vendor).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving vendor: ${e.message}")
            throw e
        }
    }

    // Customers
    suspend fun addCustomer(customer: Customer) {
        if (customer.vendorId.isBlank()) {
            throw IllegalArgumentException("Vendor ID is missing. Please sign in again.")
        }
        try {
            // Ensure archiving fields are explicitly set for new customers
            val newCustomer = customer.copy(
                isArchived = false,
                archivedAt = null
            )
            firestore.collection("customers").document(newCustomer.customerId).set(newCustomer).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding customer: ${e.message}")
            throw e
        }
    }

    suspend fun performMigration(vendorId: String) {
        try {
            val batch = firestore.batch()
            var migrationNeeded = false

            // 1. Backfill Customers
            val customers = firestore.collection("customers")
                .whereEqualTo("vendorId", vendorId)
                .get().await()
            
            customers.documents.forEach { doc ->
                if (!doc.contains("isArchived")) {
                    batch.update(doc.reference, "isArchived", false)
                    migrationNeeded = true
                }
            }

            // 2. Backfill Sales
            val sales = firestore.collection("sales")
                .whereEqualTo("vendorId", vendorId)
                .get().await()
            
            sales.documents.forEach { doc ->
                if (!doc.contains("isArchived")) {
                    batch.update(doc.reference, "isArchived", false)
                    migrationNeeded = true
                }
            }

            if (migrationNeeded) {
                batch.commit().await()
                Log.d(TAG, "Migration completed for vendor: $vendorId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed: ${e.message}")
        }
    }


    suspend fun updateCustomer(customer: Customer) {
        try {
            firestore.collection("customers").document(customer.customerId).set(customer).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating customer: ${e.message}")
            throw e
        }
    }

    suspend fun restoreCustomer(customerId: String): Boolean {
        try {
            firestore.collection("customers").document(customerId)
                .update(
                    "isArchived", false,
                    "archivedAt", null
                ).await()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring customer: ${e.message}")
            throw e
        }
    }

    suspend fun deleteCustomer(vendorId: String, customerId: String): Boolean {
        try {
            return if (hasSales(vendorId, customerId)) {
                val batch = firestore.batch()
                val customerRef = firestore.collection("customers").document(customerId)
                
                batch.update(customerRef, 
                    "isArchived", true,
                    "archivedAt", System.currentTimeMillis()
                )

                // Also archive all sales of this customer so they don't appear in general sales lists
                val salesSnapshot = firestore.collection("sales")
                    .whereEqualTo("vendorId", vendorId)
                    .whereEqualTo("customerId", customerId)
                    .get()
                    .await()
                
                salesSnapshot.documents.forEach { doc ->
                    batch.update(doc.reference, 
                        "isArchived", true,
                        "archivedAt", System.currentTimeMillis()
                    )
                }
                
                batch.commit().await()
                
                // Recalculate totals to reflect zero balance for the archived customer
                recalculateCustomerTotals(vendorId, customerId)
                true // Archived
            } else {
                firestore.collection("customers").document(customerId).delete().await()
                false // Hard deleted
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting customer: ${e.message}")
            throw e
        }
    }

    suspend fun hasSales(vendorId: String, customerId: String): Boolean {
        return try {
            val snapshot = firestore.collection("sales")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("customerId", customerId)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sales for customer $customerId: ${e.message}")
            false
        }
    }

    fun getAllCustomers(vendorId: String): Flow<List<Customer>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        Log.d(TAG, "getAllCustomers: Querying for vendorId: '$trimmedVendorId'")
        
        val subscription = firestore.collection("customers")
            .whereEqualTo("vendorId", trimmedVendorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getAllCustomers error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "Error in getAllCustomers for vendor $trimmedVendorId: ${error.message}", error)
                    // Don't close(error) to avoid crashing the app
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val customers = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapDocumentToCustomer(doc)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping customer ${doc.id}: ${e.message}")
                        null
                    }
                }?.filter { !it.isArchived } ?: emptyList()
                Log.d(TAG, "getAllCustomers: Found ${customers.size} non-archived customers for vendor $trimmedVendorId")
                trySend(customers)
            }
        awaitClose { subscription.remove() }
    }

    fun getArchivedCustomers(vendorId: String): Flow<List<Customer>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        if (trimmedVendorId.isEmpty()) {
            Log.w("ArchivedDebug", "getArchivedCustomers: vendorId is empty")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        Log.d("ArchivedDebug", "getArchivedCustomers: Querying for vendorId: '$trimmedVendorId'")
        val subscription = firestore.collection("customers")
            .whereEqualTo("vendorId", trimmedVendorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getArchivedCustomers error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e("ArchivedDebug", "getArchivedCustomers: Firestore error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: emptyList()
                Log.d("ArchivedDebug", "getArchivedCustomers: Found ${docs.size} raw documents")
                
                val customers = docs.mapNotNull { doc ->
                    try {
                        Log.d("ArchivedDebug", "getArchivedCustomers: Mapping doc ID: ${doc.id}")
                        val customer = mapDocumentToCustomer(doc)
                        if (customer != null) {
                            Log.d("ArchivedDebug", "getArchivedCustomers: Mapping success for ${doc.id}")
                        } else {
                            Log.w("ArchivedDebug", "getArchivedCustomers: Mapping returned null for ${doc.id}")
                        }
                        customer
                    } catch (e: Exception) {
                        Log.e("ArchivedDebug", "getArchivedCustomers: Exception mapping customer ${doc.id}", e)
                        null
                    }
                }.filter { it.isArchived }
                
                Log.d("ArchivedDebug", "getArchivedCustomers: Successfully mapped ${customers.size} archived customers")
                trySend(customers)
            }
        awaitClose { subscription.remove() }
    }

    fun getCustomerByIdFlow(customerId: String): Flow<Customer?> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("customers").document(customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching customer $customerId flow: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.let { mapDocumentToCustomer(it) })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getCustomerById(customerId: String): Customer? {
        if (customerId.isEmpty()) return null
        return try {
            val doc = firestore.collection("customers").document(customerId).get().await()
            mapDocumentToCustomer(doc)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching customer $customerId: ${e.message}")
            null
        }
    }

    private fun mapDocumentToCustomer(doc: DocumentSnapshot): Customer? {
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        return try {
            val customer = doc.toObject(Customer::class.java)
            if (customer != null) {
                val finalId = customer.customerId.ifBlank { doc.id }
                customer.copy(
                    customerId = finalId,
                    isArchived = data["isArchived"] as? Boolean ?: false,
                    archivedAt = safeParseLong(data["archivedAt"]),
                    totalAmount = safeParseDouble(data["totalAmount"], customer.totalAmount),
                    paidAmount = safeParseDouble(data["paidAmount"], customer.paidAmount),
                    createdAt = safeParseLong(data["createdAt"]) ?: customer.createdAt
                )
            } else {
                Log.w("ArchivedDebug", "toObject returned null for customer ${doc.id}")
                null
            }
        } catch (e: Exception) {
            Log.e("ArchivedDebug", "Default customer mapping failed for ${doc.id}, using manual mapping", e)
            try {
                Customer(
                    customerId = doc.id,
                    vendorId = data["vendorId"] as? String ?: "",
                    name = data["name"] as? String ?: "Unknown",
                    phone = data["phone"] as? String ?: "",
                    totalAmount = safeParseDouble(data["totalAmount"], 0.0),
                    paidAmount = safeParseDouble(data["paidAmount"], 0.0),
                    isArchived = data["isArchived"] as? Boolean ?: false,
                    archivedAt = safeParseLong(data["archivedAt"]),
                    createdAt = safeParseLong(data["createdAt"]) ?: System.currentTimeMillis()
                )
            } catch (inner: Exception) {
                Log.e("ArchivedDebug", "Critical failure manual mapping customer ${doc.id}", inner)
                null
            }
        }
    }

    // Sales
    suspend fun createSale(sale: Sale, installments: List<Installment>) {
        try {
            val db = firestore
            
            db.runTransaction { transaction ->
                // 1. Get current customer state (Inside Transaction)
                val customerRef = db.collection("customers").document(sale.customerId)
                val customerSnapshot = transaction.get(customerRef)
                val customer = mapDocumentToCustomer(customerSnapshot)
                    ?: throw FirebaseFirestoreException("Customer not found", FirebaseFirestoreException.Code.NOT_FOUND)

                // 2. Prepare data
                // Ensure archiving fields are explicitly set for new sales
                val newSale = sale.copy(
                    isArchived = false,
                    archivedAt = null
                )
                
                val finalAmount = newSale.finalAmount
                val newTotalAmount = customer.totalAmount + finalAmount
                val newPaidAmount = customer.paidAmount + newSale.amountPaid
                val balanceAfter = (newTotalAmount - newPaidAmount).coerceAtLeast(0.0)

                val ledgerId = "LEDGER_${System.currentTimeMillis()}"
                val ledgerEntry = LedgerEntry(
                    entryId = ledgerId,
                    vendorId = newSale.vendorId,
                    customerId = newSale.customerId,
                    customerName = newSale.customerName,
                    itemName = newSale.itemName,
                    saleId = newSale.saleId,
                    type = "sale",
                    amount = finalAmount,
                    balanceAfter = balanceAfter,
                    createdAt = newSale.createdAt
                )

                // 3. Write Phase (Atomic)
                
                // A. Create sale document
                transaction.set(db.collection("sales").document(newSale.saleId), newSale)
                
                // B. Create installment documents
                for (installment in installments) {
                    transaction.set(db.collection("installments").document(installment.installmentId), installment)
                }

                // C. Create Payment record for downpayment if exists
                if (newSale.amountPaid > 0.001) {
                    val paymentId = "PAY_DP_${System.currentTimeMillis()}"
                    val payment = Payment(
                        paymentId = paymentId,
                        saleId = newSale.saleId,
                        customerId = newSale.customerId,
                        vendorId = newSale.vendorId,
                        amount = newSale.amountPaid,
                        paymentMode = "CASH",
                        createdAt = newSale.createdAt
                    )
                    transaction.set(db.collection("payments").document(paymentId), payment)
                }

                // D. Update customer totals
                transaction.update(customerRef,
                    "totalAmount", newTotalAmount,
                    "paidAmount", newPaidAmount
                )

                // E. Create ledger entry for Sale
                transaction.set(db.collection("ledger").document(ledgerId), ledgerEntry)

                // F. Create ledger entry for Downpayment if exists
                if (newSale.amountPaid > 0.001) {
                    val payLedgerId = "LEDGER_PAY_DP_${System.currentTimeMillis()}"
                    val payLedgerEntry = LedgerEntry(
                        entryId = payLedgerId,
                        vendorId = newSale.vendorId,
                        customerId = newSale.customerId,
                        customerName = newSale.customerName,
                        itemName = newSale.itemName,
                        saleId = newSale.saleId,
                        type = "payment",
                        amount = newSale.amountPaid,
                        balanceAfter = balanceAfter,
                        createdAt = newSale.createdAt + 1 // Ensure it's slightly after the sale
                    )
                    transaction.set(db.collection("ledger").document(payLedgerId), payLedgerEntry)
                }

                null // Return from transaction
            }.await()

            Log.d(TAG, "Sale created successfully with transaction: ${sale.saleId}")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating sale: ${e.message}")
            throw e
        }
    }

    suspend fun updateSale(oldSale: Sale, newSale: Sale) {
        try {
            val db = firestore
            val saleRef = db.collection("sales").document(newSale.saleId)

            // 1. Identification Phase (Queries outside transaction to find document references)
            // Fetch discovery context snapshot to detect if dependency queries become stale
            val discoverySale = saleRef.get().await().let { mapDocumentToSale(it) }

            val installmentRefs = db.collection("installments")
                .whereEqualTo("vendorId", newSale.vendorId)
                .whereEqualTo("saleId", newSale.saleId)
                .get().await().documents.map { it.reference }
            
            val ledgerRefs = db.collection("ledger")
                .whereEqualTo("vendorId", newSale.vendorId)
                .whereEqualTo("saleId", newSale.saleId)
                .get().await().documents.map { it.reference }
                
            val paymentRefs = db.collection("payments")
                .whereEqualTo("vendorId", newSale.vendorId)
                .whereEqualTo("saleId", newSale.saleId)
                .get().await().documents.map { it.reference }

            db.runTransaction { transaction ->
                // 2. Read Phase (Inside Transaction - All reads must happen before any writes)
                val saleSnapshot = transaction.get(saleRef)
                if (!saleSnapshot.exists()) return@runTransaction
                
                val currentSale = mapDocumentToSale(saleSnapshot) 
                    ?: throw FirebaseFirestoreException("Sale mapping failed", FirebaseFirestoreException.Code.INTERNAL)
                
                if (currentSale.vendorId != newSale.vendorId) {
                    throw FirebaseFirestoreException("Vendor mismatch", FirebaseFirestoreException.Code.PERMISSION_DENIED)
                }
                
                val currentCustomerRef = db.collection("customers").document(currentSale.customerId)
                val currentCustomer = mapDocumentToCustomer(transaction.get(currentCustomerRef))
                    ?: throw FirebaseFirestoreException("Current customer not found", FirebaseFirestoreException.Code.NOT_FOUND)

                var targetCustomer: Customer? = null
                if (currentSale.customerId != newSale.customerId) {
                    val targetCustomerRef = db.collection("customers").document(newSale.customerId)
                    targetCustomer = mapDocumentToCustomer(transaction.get(targetCustomerRef))
                        ?: throw FirebaseFirestoreException("Target customer not found", FirebaseFirestoreException.Code.NOT_FOUND)
                }

                val currentInstallments = installmentRefs.mapNotNull { ref -> 
                    val doc = transaction.get(ref)
                    if (doc.exists()) mapDocumentToInstallment(doc) else null
                }
                val currentLedgerEntries = ledgerRefs.mapNotNull { ref ->
                    val doc = transaction.get(ref)
                    if (doc.exists()) mapDocumentToLedgerEntry(doc) else null
                }
                val hasPaymentDocs = paymentRefs.any { ref -> transaction.get(ref).exists() }

                // 3. Decision Logic & Activity Verification
                // We calculate activity using ONLY transaction-consistent reads.
                val hasActivity = currentSale.amountPaid > 0.001 ||
                        hasPaymentDocs ||
                        currentInstallments.any { it.amountPaid > 0.001 } ||
                        currentLedgerEntries.any { it.type == "payment" } ||
                        currentSale.status == "COMPLETED"

                // Bias toward Protected Mode if the state changed since discovery.
                val isDiscoveryStale = discoverySale == null || 
                                       currentSale.amountPaid != discoverySale.amountPaid ||
                                       currentSale.status != discoverySale.status ||
                                       currentSale.customerId != discoverySale.customerId

                // Handle uncertainty safely: if the DB state changed since the user started editing or since discovery,
                // we treat it as protected to prevent overwriting new activity.
                val isUncertain = isDiscoveryStale ||
                                 currentSale.amountPaid != oldSale.amountPaid ||
                                 currentSale.status != oldSale.status ||
                                 currentSale.customerId != oldSale.customerId

                if (hasActivity || isUncertain) {
                    // PROTECTED MODE: Sale has activity or state is uncertain.
                    
                    // Block changes to financial fields
                    val isFinancialChanged = currentSale.customerId != newSale.customerId ||
                            kotlin.math.abs(currentSale.totalAmount - newSale.totalAmount) > 0.001 ||
                            kotlin.math.abs(currentSale.finalAmount - newSale.finalAmount) > 0.001 ||
                            currentSale.quantity != newSale.quantity ||
                            kotlin.math.abs(currentSale.unitPrice - newSale.unitPrice) > 0.001 ||
                            kotlin.math.abs(currentSale.interestRate - newSale.interestRate) > 0.001 ||
                            kotlin.math.abs(currentSale.amountPaid - newSale.amountPaid) > 0.001 ||
                            currentSale.paymentType != newSale.paymentType ||
                            currentSale.installmentCount != newSale.installmentCount ||
                            currentSale.status != newSale.status

                    if (isFinancialChanged) {
                        throw FirebaseFirestoreException("Cannot edit financial fields after payments or installment activity exist.", FirebaseFirestoreException.Code.FAILED_PRECONDITION)
                    }

                    // Only allow non-financial updates (itemName). Preserve existing financial state.
                    val finalSale = currentSale.copy(itemName = newSale.itemName)
                    transaction.set(saleRef, finalSale)

                    // Update ledger itemName for the sale entry
                    currentLedgerEntries.forEach { entry ->
                        if (entry.type == "sale") {
                            transaction.update(db.collection("ledger").document(entry.entryId), "itemName", newSale.itemName)
                        }
                    }
                } else {
                    // FULL EDIT MODE: Clean sale (no activity, no uncertainty).
                    val downPayment = if (newSale.amountPaid > 0.001) newSale.amountPaid else 0.0

                    // 1. Customer Totals & Transfer handling
                    if (currentSale.customerId != newSale.customerId) {
                        // Customer Transfer
                        val tc = targetCustomer!!
                        
                        // Subtract totalAmount from old customer.
                        transaction.update(currentCustomerRef, "totalAmount", (currentCustomer.totalAmount - currentSale.finalAmount).coerceAtLeast(0.0))
                        
                        // Add totalAmount and new downpayment to new customer.
                        val updatedNewTotal = tc.totalAmount + newSale.finalAmount
                        val updatedNewPaid = tc.paidAmount + downPayment
                        
                        transaction.update(db.collection("customers").document(newSale.customerId), 
                            "totalAmount", updatedNewTotal,
                            "paidAmount", updatedNewPaid
                        )

                        // Record Downpayment if exists
                        if (downPayment > 0.001) {
                            val paymentId = "PAY_DP_${System.currentTimeMillis()}"
                            val payment = Payment(
                                paymentId = paymentId,
                                saleId = newSale.saleId,
                                customerId = newSale.customerId,
                                vendorId = newSale.vendorId,
                                amount = downPayment,
                                paymentMode = "CASH",
                                createdAt = System.currentTimeMillis()
                            )
                            transaction.set(db.collection("payments").document(paymentId), payment)
                        }

                        // Update Ledger Sale Entry
                        val saleLedgerEntry = currentLedgerEntries.find { it.type == "sale" }
                        if (saleLedgerEntry != null) {
                            val balanceAfter = (updatedNewTotal - updatedNewPaid).coerceAtLeast(0.0)
                            transaction.update(db.collection("ledger").document(saleLedgerEntry.entryId),
                                "customerId", newSale.customerId,
                                "customerName", newSale.customerName,
                                "itemName", newSale.itemName,
                                "amount", newSale.finalAmount,
                                "balanceAfter", balanceAfter
                            )
                        }
                    } else {
                        // Same customer update
                        val updatedTotal = (currentCustomer.totalAmount - currentSale.finalAmount + newSale.finalAmount).coerceAtLeast(0.0)
                        val updatedPaid = currentCustomer.paidAmount + downPayment
                        
                        transaction.update(currentCustomerRef, 
                            "totalAmount", updatedTotal,
                            "paidAmount", updatedPaid
                        )

                        // Record Downpayment if exists
                        if (downPayment > 0.001) {
                            val paymentId = "PAY_DP_${System.currentTimeMillis()}"
                            val payment = Payment(
                                paymentId = paymentId,
                                saleId = newSale.saleId,
                                customerId = newSale.customerId,
                                vendorId = newSale.vendorId,
                                amount = downPayment,
                                paymentMode = "CASH",
                                createdAt = System.currentTimeMillis()
                            )
                            transaction.set(db.collection("payments").document(paymentId), payment)
                        }

                        // Update Ledger Entry
                        val saleLedgerEntry = currentLedgerEntries.find { it.type == "sale" }
                        if (saleLedgerEntry != null) {
                            val balanceAfter = (updatedTotal - updatedPaid).coerceAtLeast(0.0)
                            transaction.update(db.collection("ledger").document(saleLedgerEntry.entryId),
                                "itemName", newSale.itemName,
                                "amount", newSale.finalAmount,
                                "balanceAfter", balanceAfter
                            )
                        }
                    }

                    // 2. Update Sale document
                    val isCompleted = downPayment >= newSale.finalAmount - 0.001
                    val finalSale = newSale.copy(
                        amountPaid = downPayment,
                        status = if (isCompleted) "COMPLETED" else "PENDING"
                    )
                    transaction.set(saleRef, finalSale)

                    // 3. Regenerate Installments
                    installmentRefs.forEach { transaction.delete(it) }
                    if (finalSale.paymentType == "Partial Payment") {
                        val count = finalSale.installmentCount.coerceAtLeast(1)
                        val totalToPay = (finalSale.finalAmount - finalSale.amountPaid).coerceAtLeast(0.0)
                        val amountPerInstallment = if (count > 0) totalToPay / count else 0.0

                        // Infer firstDueDate and interval from existing installments if available
                        val sortedInstallments = currentInstallments.sortedBy { it.dueDate }
                        
                        val inferredFirstDueDate = if (sortedInstallments.isNotEmpty()) {
                            sortedInstallments[0].dueDate
                        } else {
                            // Default to 30 days from creation if no installments exist
                            finalSale.createdAt + 30L * 24 * 60 * 60 * 1000
                        }

                        val inferredInterval = if (sortedInstallments.size >= 2) {
                            val diff = sortedInstallments[1].dueDate - sortedInstallments[0].dueDate
                            // Snap to standard intervals to avoid minor drift
                            when {
                                diff in 6 * 24 * 3600 * 1000L..8 * 24 * 3600 * 1000L -> 7L * 24 * 60 * 60 * 1000 // Weekly
                                diff in 13 * 24 * 3600 * 1000L..15 * 24 * 3600 * 1000L -> 14L * 24 * 60 * 60 * 1000 // Bi-weekly
                                diff in 27 * 24 * 3600 * 1000L..32 * 24 * 3600 * 1000L -> 30L * 24 * 60 * 60 * 1000 // Monthly
                                else -> diff.coerceAtLeast(24 * 60 * 60 * 1000L) // Use actual or min 1 day
                            }
                        } else {
                            30L * 24 * 60 * 60 * 1000 // Default to 30 days
                        }
                        
                        for (i in 0 until count) {
                            val existingInst = sortedInstallments.getOrNull(i)
                            val instId = "INST_${System.currentTimeMillis()}_${i}"
                            val installment = Installment(
                                installmentId = instId,
                                saleId = finalSale.saleId,
                                customerId = finalSale.customerId,
                                vendorId = finalSale.vendorId,
                                dueDate = inferredFirstDueDate + (i * inferredInterval),
                                amount = amountPerInstallment,
                                amountPaid = 0.0,
                                status = "PENDING",
                                lastReminderSentAt = existingInst?.lastReminderSentAt ?: 0L,
                                reminderCount = existingInst?.reminderCount ?: 0,
                                reminderStatus = existingInst?.reminderStatus ?: "NOT_SENT",
                                createdAt = existingInst?.createdAt ?: System.currentTimeMillis()
                            )
                            transaction.set(db.collection("installments").document(instId), installment)
                        }
                    }
                }
                null
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sale: ${e.message}")
            throw e
        }
    }

    suspend fun deleteSale(sale: Sale): Boolean {
        try {
            val db = firestore
            val saleRef = db.collection("sales").document(sale.saleId)
            val customerRef = db.collection("customers").document(sale.customerId)

            // 1. Identification Phase (Queries outside transaction - snapshots can be stale)
            val installmentRefs = db.collection("installments")
                .whereEqualTo("vendorId", sale.vendorId)
                .whereEqualTo("saleId", sale.saleId)
                .get().await().documents.map { it.reference }
            
            val ledgerRefs = db.collection("ledger")
                .whereEqualTo("vendorId", sale.vendorId)
                .whereEqualTo("saleId", sale.saleId)
                .get().await().documents.map { it.reference }
                
            val paymentRefs = db.collection("payments")
                .whereEqualTo("vendorId", sale.vendorId)
                .whereEqualTo("saleId", sale.saleId)
                .limit(1)
                .get().await().documents.map { it.reference }

            val wasArchivedResult = db.runTransaction { transaction ->
                // 2. Read Phase (Inside Transaction - Provides isolation for these documents)
                val saleSnapshot = transaction.get(saleRef)
                if (!saleSnapshot.exists()) return@runTransaction false
                
                val currentSale = mapDocumentToSale(saleSnapshot) 
                    ?: throw FirebaseFirestoreException("Sale mapping failed", FirebaseFirestoreException.Code.INTERNAL)
                
                if (currentSale.vendorId != sale.vendorId) {
                    throw FirebaseFirestoreException("Vendor mismatch", FirebaseFirestoreException.Code.PERMISSION_DENIED)
                }
                
                val customerSnapshot = transaction.get(customerRef)
                val currentCustomer = mapDocumentToCustomer(customerSnapshot)
                    ?: throw FirebaseFirestoreException("Customer not found", FirebaseFirestoreException.Code.NOT_FOUND)

                // Verify the state of specific documents discovered outside
                val installments = installmentRefs.mapNotNull { ref -> 
                    val doc = transaction.get(ref)
                    if (doc.exists()) mapDocumentToInstallment(doc) else null
                }
                val ledgerEntries = ledgerRefs.mapNotNull { ref ->
                    val doc = transaction.get(ref)
                    if (doc.exists()) mapDocumentToLedgerEntry(doc) else null
                }
                val hasPaymentDocs = paymentRefs.any { ref -> transaction.get(ref).exists() }

                // 3. Safety Check & Decision Logic (Atomic)
                
                // Identify confirmed financial history
                val hasFinancialHistory = currentSale.amountPaid > 0.01 ||
                        hasPaymentDocs ||
                        installments.any { it.amountPaid > 0.0 } ||
                        ledgerEntries.any { it.type == "payment" } ||
                        currentSale.status == "COMPLETED"

                // Handle stale dependency-set risk: 
                // If the sale state changed since the pre-transaction query, we cannot be certain 
                // about the completeness of the dependency set discovered outside. 
                // Biasing toward ARCHIVE for safety under uncertainty.
                val isUncertain = currentSale.amountPaid != sale.amountPaid || 
                                 currentSale.finalAmount != sale.finalAmount ||
                                 currentSale.status != sale.status ||
                                 currentSale.isArchived

                if (hasFinancialHistory || isUncertain) {
                    // --- ARCHIVE BRANCH (Safety Bias) ---
                    // Only perform updates if the sale is not already archived
                    if (!currentSale.isArchived) {
                        transaction.update(saleRef, 
                            "isArchived", true,
                            "archivedAt", System.currentTimeMillis()
                        )
                        
                        // Update Customer Totals: Reduce totalAmount (debt), preserve paidAmount (Payments truth)
                        val newTotalAmount = (currentCustomer.totalAmount - currentSale.finalAmount).coerceAtLeast(0.0)
                        transaction.update(customerRef, "totalAmount", newTotalAmount)
                    }
                    true
                } else {
                    // --- HARD DELETE BRANCH (Truly Clean Sale Only) ---
                    transaction.delete(saleRef)
                    installmentRefs.forEach { transaction.delete(it) }
                    ledgerRefs.forEach { transaction.delete(it) }
                    
                    // Update Customer Totals: Reduce totalAmount, preserve paidAmount.
                    // We align with the rule that Payments (not Sale.amountPaid) are the source of truth.
                    val newTotalAmount = (currentCustomer.totalAmount - currentSale.finalAmount).coerceAtLeast(0.0)
                    transaction.update(customerRef, "totalAmount", newTotalAmount)
                    
                    false
                }
            }.await()

            return wasArchivedResult
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting sale safely: ${e.message}")
            throw e
        }
    }

    fun getSaleByIdFlow(saleId: String): Flow<Sale?> = callbackFlow {
        if (saleId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("sales").document(saleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching sale $saleId flow: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.let { mapDocumentToSale(it) })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getSaleById(saleId: String): Sale? {
        if (saleId.isEmpty()) return null
        return try {
            val doc = firestore.collection("sales").document(saleId).get().await()
            mapDocumentToSale(doc)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sale $saleId: ${e.message}")
            null
        }
    }

    fun getSalesByVendor(vendorId: String): Flow<List<Sale>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        Log.d(TAG, "getSalesByVendor: Querying for vendorId: '$trimmedVendorId'")
        
        if (trimmedVendorId.isEmpty()) {
            Log.w(TAG, "getSalesByVendor: vendorId is empty, returning empty list")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = firestore.collection("sales")
            .whereEqualTo("vendorId", trimmedVendorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getSalesByVendor error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "getSalesByVendor: Firestore error for vendor $trimmedVendorId: ${error.message}", error)
                    // Don't close(error) to avoid crashing the app
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val salesList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapDocumentToSale(doc)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping sale ${doc.id}: ${e.message}")
                        null
                    }
                }?.filter { !it.isArchived } ?: emptyList()
                Log.d(TAG, "getSalesByVendor: Found ${salesList.size} non-archived sales for vendor $trimmedVendorId")
                trySend(salesList.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }

    fun getArchivedSales(vendorId: String): Flow<List<Sale>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        if (trimmedVendorId.isEmpty()) {
            Log.w("ArchivedDebug", "getArchivedSales: vendorId is empty")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d("ArchivedDebug", "getArchivedSales: Querying for vendorId: '$trimmedVendorId'")
        val subscription = firestore.collection("sales")
            .whereEqualTo("vendorId", trimmedVendorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getArchivedSales error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e("ArchivedDebug", "getArchivedSales: Firestore error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: emptyList()
                Log.d("ArchivedDebug", "getArchivedSales: Found ${docs.size} raw documents")

                val sales = docs.mapNotNull { doc ->
                    try {
                        Log.d("ArchivedDebug", "getArchivedSales: Mapping doc ID: ${doc.id}")
                        val sale = mapDocumentToSale(doc)
                        if (sale != null) {
                            Log.d("ArchivedDebug", "getArchivedSales: Mapping success for ${doc.id}")
                        } else {
                            Log.w("ArchivedDebug", "getArchivedSales: Mapping returned null for ${doc.id}")
                        }
                        sale
                    } catch (e: Exception) {
                        Log.e("ArchivedDebug", "getArchivedSales: Exception mapping sale ${doc.id}", e)
                        null
                    }
                }.filter { it.isArchived }
                
                Log.d("ArchivedDebug", "getArchivedSales: Successfully mapped ${sales.size} archived sales")
                trySend(sales.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun restoreSale(saleId: String): Boolean {
        try {
            firestore.collection("sales").document(saleId)
                .update(
                    "isArchived", false,
                    "archivedAt", null
                ).await()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring sale: ${e.message}")
            throw e
        }
    }

    private fun mapDocumentToSale(doc: DocumentSnapshot): Sale? {
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        return try {
            val sale = doc.toObject(Sale::class.java)
            if (sale != null) {
                val finalId = sale.saleId.ifBlank { doc.id }
                sale.copy(
                    saleId = finalId, 
                    createdAt = safeParseLong(data["createdAt"]) ?: sale.createdAt,
                    isArchived = data["isArchived"] as? Boolean ?: false,
                    archivedAt = safeParseLong(data["archivedAt"]),
                    totalAmount = safeParseDouble(data["totalAmount"], sale.totalAmount),
                    unitPrice = safeParseDouble(data["unitPrice"], sale.unitPrice),
                    amountPaid = safeParseDouble(data["amountPaid"], sale.amountPaid),
                    interestRate = safeParseDouble(data["interestRate"], sale.interestRate),
                    quantity = (data["quantity"] as? Number)?.toInt() ?: sale.quantity,
                    installmentCount = (data["installmentCount"] as? Number)?.toInt() ?: sale.installmentCount
                )
            } else {
                Log.w("ArchivedDebug", "toObject returned null for sale ${doc.id}")
                null
            }
        } catch (e: Exception) {
            Log.e("ArchivedDebug", "Default mapping failed for sale ${doc.id}, manual fallback", e)
            try {
                Sale(
                    saleId = doc.id,
                    vendorId = data["vendorId"] as? String ?: "",
                    customerId = data["customerId"] as? String ?: "",
                    customerName = data["customerName"] as? String ?: "Unknown",
                    itemName = data["itemName"] as? String ?: "Unknown Item",
                    quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                    unitPrice = safeParseDouble(data["unitPrice"], 0.0),
                    totalAmount = safeParseDouble(data["totalAmount"], 0.0),
                    interestRate = safeParseDouble(data["interestRate"], 0.0),
                    installmentCount = (data["installmentCount"] as? Number)?.toInt() ?: 1,
                    paymentType = data["paymentType"] as? String ?: "Full Payment",
                    amountPaid = safeParseDouble(data["amountPaid"], 0.0),
                    status = data["status"] as? String ?: "PENDING",
                    isArchived = data["isArchived"] as? Boolean ?: false,
                    archivedAt = safeParseLong(data["archivedAt"]),
                    createdAt = safeParseLong(data["createdAt"]) ?: System.currentTimeMillis()
                )
            } catch (inner: Exception) {
                Log.e("ArchivedDebug", "Critical failure manual mapping sale ${doc.id}", inner)
                null
            }
        }
    }

    fun getSalesByCustomer(vendorId: String, customerId: String): Flow<List<Sale>> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("sales")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching sales for customer $customerId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sales = snapshot?.documents?.mapNotNull { mapDocumentToSale(it) }
                    ?.filter { !it.isArchived } ?: emptyList()
                trySend(sales.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }
    
    fun getPaymentsByCustomer(vendorId: String, customerId: String): Flow<List<Payment>> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("payments")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("customerId", customerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching payments for customer $customerId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val payments = snapshot?.documents?.mapNotNull { mapDocumentToPayment(it) } ?: emptyList()
                trySend(payments)
            }
        awaitClose { subscription.remove() }
    }

    private fun mapDocumentToPayment(doc: DocumentSnapshot): Payment? {
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        return try {
            val payment = doc.toObject(Payment::class.java)
            if (payment != null) {
                val finalId = payment.paymentId.ifBlank { doc.id }
                payment.copy(
                    paymentId = finalId,
                    amount = (data["amount"] as? Number)?.toDouble() ?: payment.amount,
                    createdAt = when (val c = data["createdAt"]) {
                        is com.google.firebase.Timestamp -> c.toDate().time
                        is Number -> c.toLong()
                        else -> payment.createdAt
                    }
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Default mapping failed for payment ${doc.id}, fallback used: ${e.message}")
            Payment(
                paymentId = doc.id,
                saleId = data["saleId"] as? String ?: "",
                installmentId = data["installmentId"] as? String,
                customerId = data["customerId"] as? String ?: "",
                vendorId = data["vendorId"] as? String ?: "",
                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                paymentMode = data["paymentMode"] as? String ?: "CASH",
                createdAt = when (val c = data["createdAt"]) {
                    is com.google.firebase.Timestamp -> c.toDate().time
                    is Number -> c.toLong()
                    else -> System.currentTimeMillis()
                }
            )
        }
    }

    fun getInstallmentsBySale(vendorId: String, saleId: String): Flow<List<Installment>> = callbackFlow {
        if (saleId.isBlank() || vendorId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("installments")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("saleId", saleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching installments for sale $saleId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { mapDocumentToInstallment(it) } ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    fun getInstallmentsByCustomer(vendorId: String, customerId: String): Flow<List<Installment>> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("installments")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching installments for customer $customerId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { mapDocumentToInstallment(it) } ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    fun getAllInstallments(vendorId: String): Flow<List<Installment>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        Log.d(TAG, "getAllInstallments: Querying for vendorId: '$trimmedVendorId'")
        
        val subscription = firestore.collection("installments")
            .whereEqualTo("vendorId", trimmedVendorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getAllInstallments error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "getAllInstallments: Firestore error for vendor $trimmedVendorId: ${error.message}", error)
                    // Don't close(error) to avoid crashing the app
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { mapDocumentToInstallment(it) } ?: emptyList()
                Log.d(TAG, "getAllInstallments: Found ${items.size} installments for vendor $trimmedVendorId")
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    fun getDueAndOverdueInstallments(vendorId: String): Flow<List<Installment>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        val subscription = firestore.collection("installments")
            .whereEqualTo("vendorId", trimmedVendorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getDueAndOverdueInstallments error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "getDueAndOverdueInstallments error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Fetch all and filter in memory to ensure consistency with InstallmentMapper's computed logic
                // and to avoid complex Firestore indexing for status != PAID
                val items = snapshot?.documents?.mapNotNull { mapDocumentToInstallment(it) } ?: emptyList()
                val filtered = items.filter { it.status != "PAID" }
                
                trySend(filtered)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateInstallmentReminderSent(installmentId: String) {
        try {
            val docRef = firestore.collection("installments").document(installmentId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentCount = (snapshot.getLong("reminderCount") ?: 0L).toInt()
                    transaction.update(docRef,
                        "lastReminderSentAt", System.currentTimeMillis(),
                        "reminderCount", currentCount + 1,
                        "reminderStatus", "SENT"
                    )
                }
                null
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating reminder status: ${e.message}")
            throw e
        }
    }

    private fun mapDocumentToInstallment(doc: DocumentSnapshot): Installment? {
        return InstallmentMapper.mapDocumentToInstallment(doc)
    }

    fun getLedgerByCustomer(vendorId: String, customerId: String): Flow<List<LedgerEntry>> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("ledger")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "LEDGER_QUERY_ERROR: Failed to fetch ledger for customer $customerId. " +
                            "Error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { mapDocumentToLedgerEntry(it) } ?: emptyList()
                Log.d(TAG, "Fetched ${items.size} ledger entries for customer $customerId")
                trySend(items.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }

    fun getPaymentLedgerByCustomer(vendorId: String, customerId: String): Flow<List<LedgerEntry>> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("ledger")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("type", "payment")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "PAYMENT_LEDGER_QUERY_ERROR: Failed to fetch payment ledger for customer $customerId. " +
                            "Error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { mapDocumentToLedgerEntry(it) } ?: emptyList()
                Log.d(TAG, "Fetched ${items.size} payment ledger entries for customer $customerId")
                trySend(items.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }

    fun getRecentSales(vendorId: String): Flow<List<LedgerEntry>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        if (trimmedVendorId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = firestore.collection("sales")
            .whereEqualTo("vendorId", trimmedVendorId)
            .whereEqualTo("isArchived", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getRecentSales error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "getRecentSales error: ${error.message}")
                    // Don't close(error) to avoid crashing the app
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val sales = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToSale(doc)?.let { sale ->
                        LedgerEntry(
                            entryId = sale.saleId,
                            vendorId = sale.vendorId,
                            customerId = sale.customerId,
                            customerName = sale.customerName,
                            itemName = sale.itemName,
                            saleId = sale.saleId,
                            type = "sale",
                            amount = sale.finalAmount,
                            balanceAfter = 0.0, // Not needed for simple list display
                            createdAt = sale.createdAt
                        )
                    }
                } ?: emptyList()
                trySend(sales.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }

    fun getRecentActivity(vendorId: String): Flow<List<LedgerEntry>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        Log.d(TAG, "getRecentActivity: Fetching for vendor: '$trimmedVendorId'")
        
        if (trimmedVendorId.isEmpty()) {
            Log.w(TAG, "getRecentActivity: vendorId is empty, returning empty list")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var currentSubscription: com.google.firebase.firestore.ListenerRegistration? = null

        fun startQuery(useOrdering: Boolean) {
            val baseQuery = firestore.collection("ledger").whereEqualTo("vendorId", trimmedVendorId)
            val query = if (useOrdering) {
                baseQuery.orderBy("createdAt", Query.Direction.DESCENDING).limit(20)
            } else {
                baseQuery.limit(50)
            }

            currentSubscription?.remove()
            currentSubscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getRecentActivity error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (useOrdering && (error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION || error.message?.contains("index") == true)) {
                        Log.w(TAG, "getRecentActivity: Ordered query failed (missing index). Falling back to unordered + manual sort.")
                        startQuery(false)
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "getRecentActivity: Firestore error for vendor $trimmedVendorId: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { mapDocumentToLedgerEntry(it) } ?: emptyList()
                val finalItems = if (!useOrdering) {
                    Log.d(TAG, "getRecentActivity: Using fallback manual sort for ${items.size} items")
                    items.sortedByDescending { it.createdAt }.take(20)
                } else {
                    items
                }
                Log.d(TAG, "getRecentActivity: Found ${finalItems.size} entries (ordered=$useOrdering)")
                trySend(finalItems)
            }
        }

        startQuery(true)
        awaitClose { currentSubscription?.remove() }
    }

    fun getPaymentHistory(vendorId: String): Flow<List<LedgerEntry>> = callbackFlow {
        val trimmedVendorId = vendorId.trim()
        if (trimmedVendorId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = firestore.collection("ledger")
            .whereEqualTo("vendorId", trimmedVendorId)
            .whereEqualTo("type", "payment")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (SessionManager.isAuthTransitioning) {
                        Log.d(TAG, "Ignoring getPaymentHistory error during auth transition: ${error.message}")
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "getPaymentHistory error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { mapDocumentToLedgerEntry(it) } ?: emptyList()
                trySend(items.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }

    private fun mapDocumentToLedgerEntry(doc: DocumentSnapshot): LedgerEntry? {
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        return try {
            val entry = doc.toObject(LedgerEntry::class.java)
            if (entry != null) {
                val finalId = entry.entryId.ifBlank { doc.id }
                entry.copy(
                    entryId = finalId,
                    amount = safeParseDouble(data["amount"], entry.amount),
                    balanceAfter = safeParseDouble(data["balanceAfter"], entry.balanceAfter),
                    createdAt = safeParseLong(data["createdAt"]) ?: entry.createdAt
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Default mapping failed for ledger entry ${doc.id}, fallback used: ${e.message}")
            LedgerEntry(
                entryId = doc.id,
                vendorId = data["vendorId"] as? String ?: "",
                customerId = data["customerId"] as? String ?: "",
                customerName = data["customerName"] as? String ?: "Unknown",
                saleId = data["saleId"] as? String,
                type = data["type"] as? String ?: "",
                amount = safeParseDouble(data["amount"], 0.0),
                balanceAfter = safeParseDouble(data["balanceAfter"], 0.0),
                createdAt = safeParseLong(data["createdAt"]) ?: System.currentTimeMillis()
            )
        }
    }
    
    suspend fun recordPayment(payment: Payment) {
        val db = firestore
        try {
            db.runTransaction { transaction ->
                // 1. ALL READS FIRST
                val customerRef = db.collection("customers").document(payment.customerId)
                val customerDoc = transaction.get(customerRef)
                val currentCustomer = mapDocumentToCustomer(customerDoc)
                    ?: throw FirebaseFirestoreException("Customer not found", FirebaseFirestoreException.Code.NOT_FOUND)

                // Get Installment if linked
                var currentInst: Installment? = null
                if (!payment.installmentId.isNullOrBlank()) {
                    val instRef = db.collection("installments").document(payment.installmentId)
                    val instDoc = transaction.get(instRef)
                    currentInst = mapDocumentToInstallment(instDoc)
                }

                // Determine Sale ID
                val effectiveSaleId = when {
                    !payment.saleId.isNullOrBlank() -> payment.saleId
                    currentInst != null -> currentInst.saleId
                    else -> null
                }

                // Get Sale if identified
                var currentSale: Sale? = null
                if (effectiveSaleId != null) {
                    val saleRef = db.collection("sales").document(effectiveSaleId)
                    val saleDoc = transaction.get(saleRef)
                    currentSale = mapDocumentToSale(saleDoc)
                }

                // 2. CALCULATIONS
                val resolvedItemName = currentSale?.itemName ?: "Account Payment"
                val finalCustomerPaid = currentCustomer.paidAmount + payment.amount
                val currentBalance = (currentCustomer.totalAmount - finalCustomerPaid).coerceAtLeast(0.0)

                // 3. ALL WRITES LAST
                
                // Record Payment
                val finalPayment = if (payment.saleId.isBlank() && effectiveSaleId != null) {
                    payment.copy(saleId = effectiveSaleId)
                } else payment
                transaction.set(db.collection("payments").document(payment.paymentId), finalPayment)

                // Update Customer
                transaction.update(customerRef, "paidAmount", finalCustomerPaid)

                // Update Installment
                if (currentInst != null) {
                    val instRef = db.collection("installments").document(payment.installmentId!!)
                    val newInstPaid = currentInst.amountPaid + payment.amount
                    val newStatus = if (newInstPaid >= currentInst.amount - 0.01) "PAID" else currentInst.status
                    transaction.update(instRef, "amountPaid", newInstPaid, "status", newStatus)
                }

                // Update Sale
                if (currentSale != null) {
                    val saleRef = db.collection("sales").document(effectiveSaleId!!)
                    val newSalePaid = currentSale.amountPaid + payment.amount
                    val newStatus = if (newSalePaid >= currentSale.finalAmount - 0.01) "COMPLETED" else "PENDING"
                    transaction.update(saleRef, "amountPaid", newSalePaid, "status", newStatus)
                }

                // Record Ledger
                val ledgerId = "LEDGER_PAY_${System.currentTimeMillis()}"
                val ledgerEntry = LedgerEntry(
                    entryId = ledgerId,
                    vendorId = payment.vendorId,
                    customerId = payment.customerId,
                    customerName = currentCustomer.name,
                    itemName = resolvedItemName,
                    saleId = effectiveSaleId ?: "",
                    type = "payment",
                    amount = payment.amount,
                    balanceAfter = currentBalance,
                    createdAt = payment.createdAt
                )
                transaction.set(db.collection("ledger").document(ledgerId), ledgerEntry)

                null
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording payment: ${e.message}", e)
            throw e
        }
    }

    private fun safeParseDouble(value: Any?, default: Double): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun safeParseLong(value: Any?): Long? {
        return when (value) {
            is com.google.firebase.Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private suspend fun recalculateCustomerTotals(vendorId: String, customerId: String) {
        if (customerId.isBlank() || vendorId.isBlank()) return
        try {
            // 1. Total Debt: Sum of finalAmount from all non-archived sales
            val salesSnapshot = firestore.collection("sales")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("customerId", customerId)
                .get()
                .await()
            
            val sales = salesSnapshot.documents.mapNotNull { mapDocumentToSale(it) }
                .filter { !it.isArchived }
            val totalAmount = sales.sumOf { it.finalAmount }

            // 2. Total Paid: Sum of amount from all valid Payment records (Source of Truth)
            val paymentsSnapshot = firestore.collection("payments")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("customerId", customerId)
                .get()
                .await()
            
            val paidAmount = paymentsSnapshot.documents.mapNotNull { mapDocumentToPayment(it) }
                .sumOf { it.amount }
            
            // 3. Update Customer Record
            firestore.collection("customers").document(customerId)
                .update(
                    "totalAmount", totalAmount,
                    "paidAmount", paidAmount
                ).await()
            
            Log.d(TAG, "Recalculated totals for customer $customerId: total=$totalAmount, paid=$paidAmount")
        } catch (e: Exception) {
            Log.e(TAG, "Error recalculating customer totals for $customerId: ${e.message}")
        }
    }
}
