package com.wms.gridlocator.data

data class User(
    val id: String = "",
    val username: String = "",
    val passwordHash: String = "",
    val role: String = ""
)

data class Booking(
    val id: String = "",
    val locationCode: String = "",
    val itemNumber: String = "",
    val itemType: String = "",
    val batchNumber: String = "",
    val qty: Int = 0,
    val description: String = "",
    val bookedBy: String = "",
    val bookedAt: String = "",
    val status: String = "Active"
)

data class Movement(
    val id: String = "",
    val bookingId: String = "",
    val action: String = "",
    val qty: Int = 0,
    val user: String = "",
    val timestamp: String = ""
)
