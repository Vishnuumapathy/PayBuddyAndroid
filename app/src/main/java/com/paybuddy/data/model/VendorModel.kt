package com.paybuddy.data.model

import java.util.Date

data class VendorModel(
    val vendorId: String = "",
    val name: String = "",
    val shopName: String = "",
    val phone: String = "",
    val email: String = "",
    val upiId: String = "",
    val createdAt: Date = Date()
)
