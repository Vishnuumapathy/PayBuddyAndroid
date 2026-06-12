package com.paybuddy.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderMessageBuilder {

    /**
     * Builds a payment reminder message for WhatsApp.
     *
     * Format:
     * Hi {customerName}, this is a reminder regarding your purchase of {itemName}. 
     * Your pending amount is ₹{amount}. Kindly complete the payment by {formattedDate}. - PayBuddy
     */
    fun buildPaymentReminderMessage(
        customerName: String,
        itemName: String,
        pendingAmount: Double,
        dueDate: Long
    ): String {
        val name = if (customerName.isBlank()) "Customer" else customerName
        val item = if (itemName.isBlank()) "your purchase" else itemName
        
        // Format amount cleanly: remove .0 if it's a whole number
        val formattedAmount = if (pendingAmount % 1.0 == 0.0) {
            pendingAmount.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", pendingAmount)
        }

        val formattedDate = if (dueDate <= 0) {
            "the due date"
        } else {
            try {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date(dueDate))
            } catch (e: Exception) {
                "the due date"
            }
        }

        return "Hi $name, this is a reminder regarding your purchase of $item. Your pending amount is ₹$formattedAmount. Kindly complete the payment by $formattedDate. - PayBuddy"
    }
}
