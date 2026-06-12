package com.paybuddy.utils

object PhoneUtils {
    /**
     * Formats a raw phone number for WhatsApp deep links.
     * Rules:
     * 1. Remove all non-digit characters.
     * 2. Remove leading zero if present.
     * 3. If length is 10, prepend "91" (India country code).
     * 4. Return digits-only string.
     */
    fun formatPhoneForWhatsApp(rawPhone: String): String {
        // 1. Remove all non-digit characters
        var digits = rawPhone.filter { it.isDigit() }

        // 2. Handle leading zero
        if (digits.startsWith("0")) {
            digits = digits.substring(1)
        }

        // 3. Ensure country code
        return if (digits.length == 10) {
            "91$digits"
        } else {
            digits
        }
    }
}
