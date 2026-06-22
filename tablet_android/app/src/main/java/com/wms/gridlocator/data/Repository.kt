package com.wms.gridlocator.data

import android.util.Log
import org.mindrot.jbcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LoginResult(val ok: Boolean, val username: String?, val role: String?, val error: String?)
data class ActionResult(val ok: Boolean, val error: String? = null, val bookingId: String? = null)
data class ZoneOverview(val code: String, val displayName: String, val occupied: Int, val total: Int, val percent: Int)

class Repository {

    // ── Auth ──

    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val user = DbRepository.getUser(username)
            if (user != null) {
                // jBCrypt (0.4) only supports $2a$ — Python's bcrypt writes $2b$ which is identical algorithmically
                val hash = (user["password_hash"] ?: "").replace("\$2b\$", "\$2a\$")
                val isValid = try {
                    BCrypt.checkpw(password, hash)
                } catch (e: IllegalArgumentException) {
                    Log.e("Repository", "BCrypt error for user=$username", e)
                    false
                }

                if (isValid) {
                    LoginResult(ok = true, username = user["username"], role = user["role"], error = null)
                } else {
                    LoginResult(ok = false, username = null, role = null, error = "Invalid credentials")
                }
            } else {
                LoginResult(ok = false, username = null, role = null, error = "Invalid credentials")
            }
        } catch (e: Throwable) {
            LoginResult(ok = false, username = null, role = null, error = "Connection error: ${e.message}")
        }
    }

    // ── Cell operations ──

    suspend fun getCellContents(locationCode: String): List<Booking> = withContext(Dispatchers.IO) {
        DbRepository.getCellContents(locationCode)
    }

    suspend fun bookIn(
        locationCode: String,
        itemNumber: String,
        itemType: String,
        batchNumber: String,
        qty: Int,
        description: String,
        user: String,
    ): ActionResult = withContext(Dispatchers.IO) {
        try {
            val maxSlots = 2
            val current = DbRepository.getActiveSlotCount(locationCode)
            if (current >= maxSlots) {
                return@withContext ActionResult(false, "Cell at max capacity ($maxSlots slots)")
            }
            val bookingId = DbRepository.bookIn(
                locationCode, itemNumber, itemType, batchNumber, qty, description, user
            )
            ActionResult(true, bookingId = bookingId)
        } catch (e: Throwable) {
            ActionResult(false, error = e.message ?: "Book-in failed")
        }
    }

    suspend fun consume(bookingId: String, qty: Int, user: String): ActionResult = withContext(Dispatchers.IO) {
        try {
            val ok = DbRepository.consume(bookingId, qty, user)
            if (ok) ActionResult(true) else ActionResult(false, error = "Consume failed")
        } catch (e: Throwable) {
            ActionResult(false, error = e.message ?: "Consume failed")
        }
    }

    // ── Relocation ──

    suspend fun searchStock(itemNumber: String): List<Booking> = withContext(Dispatchers.IO) {
        DbRepository.searchStock(itemNumber)
    }

    suspend fun relocate(
        sourceBookingId: String, destLocationCode: String, qty: Int, user: String
    ): ActionResult = withContext(Dispatchers.IO) {
        try {
            val maxSlots = 2
            val current = DbRepository.getActiveSlotCount(destLocationCode)
            if (current >= maxSlots) {
                return@withContext ActionResult(false, "Destination cell at max capacity ($maxSlots slots)")
            }
            val bookingId = DbRepository.relocate(sourceBookingId.toInt(), destLocationCode, qty, user)
            ActionResult(true, bookingId = bookingId)
        } catch (e: Throwable) {
            ActionResult(false, error = e.message ?: "Relocation failed")
        }
    }

    suspend fun getRecentRelocations(): List<RecentRelocation> = withContext(Dispatchers.IO) {
        DbRepository.getRecentRelocations()
    }

    suspend fun revertRelocation(
        destBookingId: String, originalSourceLocation: String, qty: Int, user: String
    ): ActionResult = withContext(Dispatchers.IO) {
        try {
            val bookingId = DbRepository.revertRelocation(destBookingId.toInt(), originalSourceLocation, qty, user)
            ActionResult(true, bookingId = bookingId)
        } catch (e: Throwable) {
            ActionResult(false, error = e.message ?: "Revert failed")
        }
    }

    suspend fun getLocationSuggestions(itemNumber: String, sourceDate: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        DbRepository.getLocationSuggestions(itemNumber, sourceDate)
    }

    suspend fun getAllCellSlotCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        DbRepository.getAllCellSlotCounts()
    }

    suspend fun getCellSlotCounts(shelfCode: String): Map<String, Int> = withContext(Dispatchers.IO) {
        DbRepository.getCellSlotCounts(shelfCode)
    }

    // ── Merge candidates ──

    suspend fun getMergeCandidates(itemNumber: String): List<MergeCandidate> = withContext(Dispatchers.IO) {
        DbRepository.getMergeCandidates(itemNumber)
    }

    // ── FIFO consume ──

    suspend fun searchFifo(itemNumber: String): List<Booking> = withContext(Dispatchers.IO) {
        DbRepository.searchFifo(itemNumber)
    }

    suspend fun consumeFifo(plan: List<FifoPlanItem>, user: String): ActionResult = withContext(Dispatchers.IO) {
        try {
            for (item in plan) {
                DbRepository.consume(item.bookingId, item.take, user)
            }
            ActionResult(true)
        } catch (e: Throwable) {
            ActionResult(false, error = e.message ?: "FIFO consume failed")
        }
    }

    // ── ERP deliveries ──

    suspend fun getErpDeliveries(): List<ErpDelivery> = withContext(Dispatchers.IO) {
        ErpRepository.getRecentDeliveries()
    }

    // ── Zone queries ──

    suspend fun getZonesOverview(
        zones: Map<String, ZoneConfig>
    ): List<ZoneOverview> = withContext(Dispatchers.IO) {
        zones.map { (code, z) ->
            val shelfCodes = z.shelves.keys.toList()
            val total = z.shelves.values.sumOf { it.sections * it.rows }
            val occupied = DbRepository.getZoneOccupancy(shelfCodes)
            val pct = if (total > 0) (occupied * 100) / total else 0
            ZoneOverview(code, z.displayName, occupied, total, pct)
        }
    }

    suspend fun getOccupiedCells(shelfCode: String): Set<String> = withContext(Dispatchers.IO) {
        DbRepository.getOccupiedCells(shelfCode).toSet()
    }
}
