package com.paybuddy.data.util

import com.google.firebase.firestore.DocumentSnapshot
import com.paybuddy.data.model.Installment

object InstallmentMapper {
    fun mapDocumentToInstallment(doc: DocumentSnapshot): Installment? {
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        
        return try {
            val inst = doc.toObject(Installment::class.java)
            if (inst != null) {
                val finalId = if (inst.installmentId.isBlank()) doc.id else inst.installmentId
                val dueDateRaw = data["dueDate"]
                val finalDueDate = when (dueDateRaw) {
                    is com.google.firebase.Timestamp -> dueDateRaw.toDate().time
                    is Number -> dueDateRaw.toLong()
                    else -> inst.dueDate
                }
                
                // Centralized dynamic overdue logic: compute effective status during mapping
                val rawStatus = data["status"] as? String ?: inst.status
                val computedStatus = if (rawStatus != "PAID" && finalDueDate > 0 && finalDueDate < System.currentTimeMillis()) {
                    "OVERDUE"
                } else {
                    rawStatus
                }

                inst.copy(
                    installmentId = finalId,
                    dueDate = finalDueDate,
                    amount = (data["amount"] as? Number)?.toDouble() ?: inst.amount,
                    amountPaid = (data["amountPaid"] as? Number)?.toDouble() ?: inst.amountPaid,
                    status = computedStatus,
                    lastReminderSentAt = (data["lastReminderSentAt"] as? Number)?.toLong() ?: inst.lastReminderSentAt,
                    reminderCount = (data["reminderCount"] as? Number)?.toInt() ?: inst.reminderCount,
                    reminderStatus = data["reminderStatus"] as? String ?: inst.reminderStatus,
                    createdAt = when (val c = data["createdAt"]) {
                        is com.google.firebase.Timestamp -> c.toDate().time
                        is Number -> c.toLong()
                        else -> inst.createdAt
                    }
                )
            } else null
        } catch (e: Exception) {
            val finalDueDate = when (val d = data["dueDate"]) {
                is com.google.firebase.Timestamp -> d.toDate().time
                is Number -> d.toLong()
                else -> 0L
            }
            Installment(
                installmentId = doc.id,
                saleId = data["saleId"] as? String ?: "",
                customerId = data["customerId"] as? String ?: "",
                vendorId = data["vendorId"] as? String ?: "",
                dueDate = finalDueDate,
                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                amountPaid = (data["amountPaid"] as? Number)?.toDouble() ?: 0.0,
                status = run {
                    val s = data["status"] as? String ?: "PENDING"
                    if (s != "PAID" && finalDueDate > 0 && finalDueDate < System.currentTimeMillis()) "OVERDUE" else s
                },
                createdAt = when (val c = data["createdAt"]) {
                    is com.google.firebase.Timestamp -> c.toDate().time
                    is Number -> c.toLong()
                    else -> System.currentTimeMillis()
                }
            )
        }
    }
}
