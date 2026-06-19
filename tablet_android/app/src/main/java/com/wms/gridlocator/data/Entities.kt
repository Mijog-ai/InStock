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

data class FifoPlanItem(
    val bookingId: String,
    val locationCode: String,
    val itemNumber: String,
    val batchNumber: String,
    val available: Int,
    val take: Int,
    val bookedAt: String
)

data class RecentRelocation(
    val destBookingId: String,
    val itemNumber: String,
    val description: String,
    val fromLocation: String,
    val toLocation: String,
    val qty: Int,
    val movedBy: String,
    val movedAt: String
)

data class ErpDelivery(
    val itemNumber: String = "",
    val purchaseNumber: String = "",
    val ordered: Double = 0.0,
    val deliveryNote: String = "",
    val externalDeliveryNote: String = "",
    val supplierName: String = "",
    val deliveryDate: String = ""
)
