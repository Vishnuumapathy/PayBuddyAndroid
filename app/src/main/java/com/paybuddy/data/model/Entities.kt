package com.paybuddy.data.model

import java.util.Date

data class Vendor(
    val vendorId: String = "",
    val name: String = "",
    val shopName: String = "",
    val phone: String = "",
    val email: String = "",
    val upiId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Customer(
    val customerId: String = "",
    val vendorId: String = "",
    val name: String = "",
    val phone: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingBalance: Double
        get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
}

data class Sale(
    val saleId: String = "",
    val vendorId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val itemName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0, // Base amount before interest
    val interestRate: Double = 0.0,
    val installmentCount: Int = 1,
    val paymentType: String = "Full Payment", // "Full Payment" or "Partial Payment"
    val amountPaid: Double = 0.0,
    val status: String = "PENDING", // "PENDING" or "COMPLETED"
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val finalAmount: Double
        get() = if (paymentType == "Full Payment") {
            totalAmount
        } else {
            totalAmount + (totalAmount * interestRate / 100)
        }
    
    val remainingAmount: Double
        get() = (finalAmount - amountPaid).coerceAtLeast(0.0)

    val isCompleted: Boolean
        get() = remainingAmount <= 0.01 // Use small epsilon for double comparison
}

data class Installment(
    val installmentId: String = "",
    val saleId: String = "",
    val customerId: String = "",
    val vendorId: String = "",
    val dueDate: Long = 0L,
    val amount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val status: String = "PENDING", // "PENDING", "PAID", "OVERDUE"
    val lastReminderSentAt: Long = 0L,
    val reminderCount: Int = 0,
    val reminderStatus: String = "NOT_SENT",
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double
        get() = (amount - amountPaid).coerceAtLeast(0.0)
}

data class Payment(
    val paymentId: String = "",
    val saleId: String = "",
    val installmentId: String? = null,
    val customerId: String = "",
    val vendorId: String = "",
    val amount: Double = 0.0,
    val paymentMode: String = "CASH",
    val createdAt: Long = System.currentTimeMillis()
)

data class LedgerEntry(
    val entryId: String = "",
    val vendorId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val itemName: String = "",
    val saleId: String? = null,
    val type: String = "", // "sale" or "payment"
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
