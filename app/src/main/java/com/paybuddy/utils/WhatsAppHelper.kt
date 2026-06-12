package com.paybuddy.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {

    fun sendReminder(
        context: Context,
        phone: String,
        message: String
    ) {
        if (phone.isBlank()) {
            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedPhone = PhoneUtils.formatPhoneForWhatsApp(phone)

        if (formattedPhone.length < 10) {
            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val encodedMessage = Uri.encode(message)
            val uri = "https://wa.me/$formattedPhone?text=$encodedMessage"
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uri)
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Normalizes phone number and opens WhatsApp with the given message.
     * Returns true if successful, false otherwise.
     */
    fun openWhatsAppReminder(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        if (phoneNumber.isBlank()) return false

        // Normalize phone number
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        
        val normalizedPhone = when {
            digitsOnly.length == 10 -> "91$digitsOnly"
            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> digitsOnly
            else -> {
                // If it doesn't fit the 10-digit or 12-digit (91) rule, 
                // we check if it's at least a reasonable length after cleaning.
                // But the requirement says "if 10 digits prefix 91" and "if 91 and 12 digits keep it".
                // We'll follow the normalization strictly.
                if (digitsOnly.length < 10) return false
                digitsOnly
            }
        }

        return try {
            val encodedMessage = Uri.encode(message)
            val url = "https://wa.me/$normalizedPhone?text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            
            // Try to set package to WhatsApp to ensure it opens in the app
            intent.setPackage("com.whatsapp")
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback: Try without setting package if WhatsApp specifically isn't found 
            // but a browser or other app can handle wa.me
            try {
                val encodedMessage = Uri.encode(message)
                val url = "https://wa.me/$normalizedPhone?text=$encodedMessage"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
}
