package com.wms.gridlocator.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Repository {

    private val db = FirebaseFirestore.getInstance()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private fun now(): String = LocalDateTime.now().format(fmt)

    // ── Auth ──

    suspend fun getUser(username: String): User? {
        val doc = db.collection("users").document(username).get().await()
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        return User(
            id = doc.id,
            username = data["username"] as? String ?: doc.id,
            passwordHash = data["password_hash"] as? String ?: "",
            role = data["role"] as? String ?: ""
        )
    }

    suspend fun createUser(username: String, passwordHash: String, role: String) {
        db.collection("users").document(username).set(
            mapOf("username" to username, "password_hash" to passwordHash, "role" to role)
        ).await()
    }

    // ── Cell operations ──

    suspend fun getCellContents(locationCode: String): List<Booking> {
        val snap = db.collection("bookings")
            .whereEqualTo("location_code", locationCode)
            .whereEqualTo("status", "Active")
            .get().await()
        return snap.documents.map { doc ->
            val d = doc.data ?: emptyMap()
            Booking(
                id = doc.id,
                locationCode = d["location_code"] as? String ?: "",
                itemNumber = d["item_number"] as? String ?: "",
                itemType = d["item_type"] as? String ?: "",
                batchNumber = d["batch_number"] as? String ?: "",
                qty = (d["qty"] as? Long)?.toInt() ?: 0,
                description = d["description"] as? String ?: "",
                bookedBy = d["booked_by"] as? String ?: "",
                bookedAt = d["booked_at"] as? String ?: "",
                status = d["status"] as? String ?: "Active"
            )
        }.sortedBy { it.bookedAt }
    }

    suspend fun getActiveSlotCount(locationCode: String): Int {
        val snap = db.collection("bookings")
            .whereEqualTo("location_code", locationCode)
            .whereEqualTo("status", "Active")
            .get().await()
        return snap.size()
    }

    suspend fun bookIn(
        locationCode: String, itemNumber: String, itemType: String,
        batchNumber: String, qty: Int, description: String, user: String
    ): String {
        val ts = now()
        val bookingRef = db.collection("bookings").document()
        bookingRef.set(
            mapOf(
                "location_code" to locationCode,
                "item_number" to itemNumber,
                "item_type" to itemType,
                "batch_number" to batchNumber,
                "qty" to qty,
                "description" to description,
                "booked_by" to user,
                "booked_at" to ts,
                "status" to "Active"
            )
        ).await()
        db.collection("movements").add(
            mapOf(
                "booking_id" to bookingRef.id,
                "action" to "BookIn",
                "qty" to qty,
                "user" to user,
                "timestamp" to ts
            )
        ).await()
        return bookingRef.id
    }

    suspend fun consume(bookingId: String, qty: Int, user: String): Boolean {
        val ref = db.collection("bookings").document(bookingId)
        val doc = ref.get().await()
        if (!doc.exists()) return false
        val data = doc.data ?: return false
        if (data["status"] != "Active") return false

        val currentQty = (data["qty"] as? Long)?.toInt() ?: 0
        val ts = now()
        val newQty = currentQty - qty
        val consumedQty: Int

        if (newQty <= 0) {
            ref.update(mapOf("qty" to 0, "status" to "Consumed")).await()
            consumedQty = currentQty
        } else {
            ref.update("qty", newQty).await()
            consumedQty = qty
        }

        db.collection("movements").add(
            mapOf(
                "booking_id" to bookingId,
                "action" to "Consume",
                "qty" to consumedQty,
                "user" to user,
                "timestamp" to ts
            )
        ).await()
        return true
    }

    // ── ERP deliveries (synced from desktop via Firestore) ──

    suspend fun getErpDeliveries(): List<ErpDelivery> {
        val snap = db.collection("erp_deliveries").get().await()
        return snap.documents.map { doc ->
            val d = doc.data ?: emptyMap()
            ErpDelivery(
                itemNumber = d["ITEMNUMBER"] as? String ?: "",
                purchaseNumber = d["PURCHASENUMBER"] as? String ?: "",
                ordered = (d["ORDERED"] as? Number)?.toDouble() ?: 0.0,
                deliveryNote = d["DELIVERYNOTE"] as? String ?: "",
                externalDeliveryNote = d["EXTERNALDELIVERYNOTE"] as? String ?: "",
                supplierName = d["NAME"] as? String ?: "",
                deliveryDate = d["DELIVERYDATE"] as? String ?: ""
            )
        }.sortedByDescending { it.deliveryDate }
    }

    // ── Zone queries — single-field query + client-side prefix filter ──

    suspend fun getZoneOccupancy(shelfCodes: List<String>): Int {
        val snap = db.collection("bookings")
            .whereEqualTo("status", "Active")
            .get().await()
        val prefixes = shelfCodes.map { "$it-" }
        val occupied = mutableSetOf<String>()
        for (doc in snap.documents) {
            val loc = doc.getString("location_code") ?: continue
            if (prefixes.any { loc.startsWith(it) }) {
                occupied.add(loc)
            }
        }
        return occupied.size
    }

    suspend fun getOccupiedCells(shelfCode: String): Set<String> {
        val snap = db.collection("bookings")
            .whereEqualTo("status", "Active")
            .get().await()
        val prefix = "$shelfCode-"
        return snap.documents
            .mapNotNull { it.getString("location_code") }
            .filter { it.startsWith(prefix) }
            .toSet()
    }
}
