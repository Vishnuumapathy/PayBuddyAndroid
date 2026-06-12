package com.paybuddy.utils

import kotlin.math.pow

object InstallmentCalculator {

    /**
     * Zero Interest EMI
     */
    fun calculateZeroInterest(balance: Double, installments: Int): Double {
        if (installments <= 0) return balance
        return balance / installments
    }

    /**
     * Flat Interest EMI
     * Total = balance + (balance * rate / 100)
     */
    fun calculateFlatInterest(balance: Double, installments: Int, annualRate: Double): Double {
        if (installments <= 0) return balance
        val total = balance + (balance * (annualRate / 100.0))
        return total / installments
    }

    /**
     * Reducing Balance EMI
     * P * r * (1 + r)^n / ((1 + r)^n - 1)
     */
    fun calculateReducingBalance(balance: Double, installments: Int, annualRate: Double): Double {
        if (installments <= 0) return balance
        val r = (annualRate / 12.0) / 100.0
        val n = installments.toDouble()
        val factor = (1 + r).pow(n)
        return balance * r * factor / (factor - 1)
    }

    /**
     * Flat Fee EMI
     */
    fun calculateFlatFee(balance: Double, installments: Int, fee: Double): Double {
        if (installments <= 0) return balance
        val total = balance + fee
        return total / installments
    }
}
