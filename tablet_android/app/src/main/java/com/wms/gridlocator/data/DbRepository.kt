package com.wms.gridlocator.data

import android.util.Log
import java.sql.ResultSet
import java.sql.Statement

object DbRepository {

    fun initDb() {
        SqlClient.withWms { conn ->
            Log.d("DbRepository", "Initializing DB...")

            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    """IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'users' AND schema_id = SCHEMA_ID('dbo'))
                       CREATE TABLE dbo.users (
                           username NCHAR(20) NOT NULL PRIMARY KEY,
                           password_hash NVARCHAR(200) NOT NULL,
                           role NCHAR(20) NOT NULL
                       )"""
                )
                Log.d("DbRepository", "dbo.users table ensured.")
            }

            val users = listOf(
                Triple("admin", "admin123", "Admin"),
                Triple("logistics1", "log123", "Logistics"),
                Triple("logistics2", "log123", "Logistics"),
                Triple("consumer1", "con123", "Consumer")
            )

            users.forEach { (user, pass, role) ->
                conn.prepareStatement("SELECT COUNT(*) FROM dbo.users WHERE RTRIM(username) = ?").use { ps ->
                    ps.setString(1, user)
                    ps.executeQuery().use { rs ->
                        if (rs.next() && rs.getInt(1) == 0) {
                            Log.d("DbRepository", "Seeding user: $user")
                            val hash = org.mindrot.jbcrypt.BCrypt.hashpw(pass, org.mindrot.jbcrypt.BCrypt.gensalt())
                            conn.prepareStatement("INSERT INTO dbo.users (username, password_hash, role) VALUES (?, ?, ?)").use { insert ->
                                insert.setString(1, user)
                                insert.setString(2, hash)
                                insert.setString(3, role)
                                insert.executeUpdate()
                            }
                        }
                    }
                }
            }

            Log.d("DbRepository", "DB initialization complete.")
        }
    }

    fun getUser(username: String): Map<String, String>? {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                "SELECT username, password_hash, role FROM dbo.users WHERE RTRIM(username) = ?"
            ).use { ps ->
                ps.setString(1, username)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val foundUser = rs.getString("username").trim()
                        Log.d("DbRepository", "User found: $foundUser")
                        mapOf(
                            "id" to foundUser,
                            "username" to foundUser,
                            "password_hash" to rs.getString("password_hash").trim(),
                            "role" to rs.getString("role").trim()
                        )
                    } else {
                        Log.w("DbRepository", "User not found: $username")
                        null
                    }
                }
            }
        }
    }

    // ── Cell contents ──

    fun getCellContents(locationCode: String): List<Booking> {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                "SELECT * FROM dbo.rohteillager WHERE RTRIM(Lagerort) = ? AND CAST(RTRIM(Menge) AS INT) > 0"
            ).use { ps ->
                ps.setString(1, locationCode)
                ps.executeQuery().use { rs ->
                    val bookings = mutableListOf<Booking>()
                    while (rs.next()) bookings.add(rowToBooking(rs))
                    bookings.sortedBy { it.bookedAt }
                }
            }
        }
    }

    fun getActiveSlotCount(locationCode: String): Int {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                "SELECT COUNT(*) FROM dbo.rohteillager WHERE RTRIM(Lagerort) = ? AND CAST(RTRIM(Menge) AS INT) > 0"
            ).use { ps ->
                ps.setString(1, locationCode)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    // ── Book-in ──

    fun bookIn(
        locationCode: String, itemNumber: String, itemType: String,
        batchNumber: String, qty: Int, description: String, user: String
    ): String {
        return SqlClient.withWms { conn ->
            val bookingId: String

            conn.prepareStatement(
                """INSERT INTO dbo.rohteillager
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, itemNumber.take(20))
                ps.setString(2, description.take(40))
                ps.setString(3, locationCode.take(20))
                ps.setString(4, qty.toString().take(10))
                ps.setString(5, itemType.take(20))
                ps.setString(6, batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.executeUpdate()

                bookingId = ps.generatedKeys.use { keys ->
                    if (keys.next()) keys.getInt(1).toString() else "0"
                }
            }

            conn.prepareStatement(
                """INSERT INTO dbo.journal
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)"""
            ).use { ps ->
                ps.setString(1, itemNumber.take(20))
                ps.setString(2, description.take(40))
                ps.setString(3, locationCode.take(20))
                ps.setString(4, qty.toString().take(10))
                ps.setString(5, itemType.take(20))
                ps.setString(6, batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.setString(8, "Ein $qty Stk".take(20))
                ps.executeUpdate()
            }

            bookingId
        }
    }

    // ── Consume ──

    fun consume(bookingId: String, qty: Int, user: String): Boolean {
        return SqlClient.withWms { conn ->
            val booking = conn.prepareStatement(
                "SELECT * FROM dbo.rohteillager WHERE Rownumber = ?"
            ).use { ps ->
                ps.setInt(1, bookingId.toInt())
                ps.executeQuery().use { rs ->
                    if (rs.next()) rowToBooking(rs) else return@withWms false
                }
            }

            val currentQty = booking.qty
            val newQty = currentQty - qty
            val consumedQty = if (newQty > 0) qty else currentQty

            if (newQty <= 0) {
                conn.prepareStatement("DELETE FROM dbo.rohteillager WHERE Rownumber = ?").use { ps ->
                    ps.setInt(1, bookingId.toInt())
                    ps.executeUpdate()
                }
            } else {
                conn.prepareStatement("UPDATE dbo.rohteillager SET Menge = ? WHERE Rownumber = ?").use { ps ->
                    ps.setString(1, newQty.toString())
                    ps.setInt(2, bookingId.toInt())
                    ps.executeUpdate()
                }
            }

            conn.prepareStatement(
                """INSERT INTO dbo.journal
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)"""
            ).use { ps ->
                ps.setString(1, booking.itemNumber.take(20))
                ps.setString(2, booking.description.take(40))
                ps.setString(3, booking.locationCode.take(20))
                ps.setString(4, consumedQty.toString().take(10))
                ps.setString(5, booking.itemType.take(20))
                ps.setString(6, booking.batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.setString(8, "Aus -$consumedQty Stk".take(20))
                ps.executeUpdate()
            }

            true
        }
    }

    // ── Relocation ──

    fun searchStock(itemNumber: String): List<Booking> {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                """SELECT * FROM dbo.rohteillager
                   WHERE RTRIM(Artikelnummer) LIKE ? AND CAST(RTRIM(Menge) AS INT) > 0
                   ORDER BY Lagerort ASC, Datum ASC, Rownumber ASC"""
            ).use { ps ->
                ps.setString(1, "%$itemNumber%")
                ps.executeQuery().use { rs ->
                    val results = mutableListOf<Booking>()
                    while (rs.next()) results.add(rowToBooking(rs))
                    results
                }
            }
        }
    }

    fun relocate(sourceBookingId: Int, destLocationCode: String, qty: Int, user: String): String {
        return SqlClient.withWms { conn ->
            val source = conn.prepareStatement(
                "SELECT * FROM dbo.rohteillager WHERE Rownumber = ?"
            ).use { ps ->
                ps.setInt(1, sourceBookingId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rowToBooking(rs) else throw IllegalStateException("Source booking not found")
                }
            }

            val actualQty = minOf(qty, source.qty)
            val newSourceQty = source.qty - actualQty

            if (newSourceQty <= 0) {
                conn.prepareStatement("DELETE FROM dbo.rohteillager WHERE Rownumber = ?").use { ps ->
                    ps.setInt(1, sourceBookingId)
                    ps.executeUpdate()
                }
            } else {
                conn.prepareStatement("UPDATE dbo.rohteillager SET Menge = ? WHERE Rownumber = ?").use { ps ->
                    ps.setString(1, newSourceQty.toString())
                    ps.setInt(2, sourceBookingId)
                    ps.executeUpdate()
                }
            }

            val bookingId: String
            conn.prepareStatement(
                """INSERT INTO dbo.rohteillager
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, source.itemNumber.take(20))
                ps.setString(2, source.description.take(40))
                ps.setString(3, destLocationCode.take(20))
                ps.setString(4, actualQty.toString().take(10))
                ps.setString(5, source.itemType.take(20))
                ps.setString(6, source.batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.executeUpdate()
                bookingId = ps.generatedKeys.use { keys ->
                    if (keys.next()) keys.getInt(1).toString() else "0"
                }
            }

            conn.prepareStatement(
                """INSERT INTO dbo.journal
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)"""
            ).use { ps ->
                ps.setString(1, source.itemNumber.take(20))
                ps.setString(2, source.description.take(40))
                ps.setString(3, source.locationCode.take(20))
                ps.setString(4, actualQty.toString().take(10))
                ps.setString(5, source.itemType.take(20))
                ps.setString(6, source.batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.setString(8, "Uml. aus -$actualQty".take(20))
                ps.executeUpdate()
            }

            conn.prepareStatement(
                """INSERT INTO dbo.journal
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)"""
            ).use { ps ->
                ps.setString(1, source.itemNumber.take(20))
                ps.setString(2, source.description.take(40))
                ps.setString(3, destLocationCode.take(20))
                ps.setString(4, actualQty.toString().take(10))
                ps.setString(5, source.itemType.take(20))
                ps.setString(6, source.batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.setString(8, "Uml. ein +$actualQty".take(20))
                ps.executeUpdate()
            }

            bookingId
        }
    }

    // ── Relocation revert ──

    fun getRecentRelocations(): List<RecentRelocation> {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                """SELECT j_in.Artikelnummer, j_in.Bezeichnung, j_in.Lagerort AS dest,
                          j_out.Lagerort AS src, j_in.Menge, j_in.Zusatz1, j_in.Datum,
                          r.Rownumber AS destRowNum
                   FROM dbo.journal j_in
                   INNER JOIN dbo.journal j_out
                       ON j_out.Artikelnummer = j_in.Artikelnummer
                       AND j_out.Menge = j_in.Menge
                       AND j_out.Datum = j_in.Datum
                       AND j_out.Zusatz1 = j_in.Zusatz1
                       AND RTRIM(j_out.Bemerkung) LIKE 'Uml. aus%'
                       AND RTRIM(j_in.Bemerkung) LIKE 'Uml. ein%'
                   INNER JOIN dbo.rohteillager r
                       ON RTRIM(r.Artikelnummer) = RTRIM(j_in.Artikelnummer)
                       AND RTRIM(r.Lagerort) = RTRIM(j_in.Lagerort)
                       AND CAST(RTRIM(r.Menge) AS INT) > 0
                   WHERE RTRIM(j_in.Bemerkung) LIKE 'Uml. ein%'
                   ORDER BY j_in.Datum DESC"""
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    val results = mutableListOf<RecentRelocation>()
                    val seen = mutableSetOf<Int>()
                    while (rs.next() && results.size < 20) {
                        val destRow = rs.getInt("destRowNum")
                        if (seen.add(destRow)) {
                            results.add(
                                RecentRelocation(
                                    destBookingId = destRow.toString(),
                                    itemNumber = rs.getString("Artikelnummer")?.trim() ?: "",
                                    description = rs.getString("Bezeichnung")?.trim() ?: "",
                                    fromLocation = rs.getString("src")?.trim() ?: "",
                                    toLocation = rs.getString("dest")?.trim() ?: "",
                                    qty = (rs.getString("Menge")?.trim() ?: "0").toIntOrNull() ?: 0,
                                    movedBy = rs.getString("Zusatz1")?.trim() ?: "",
                                    movedAt = rs.getTimestamp("Datum")?.toString() ?: ""
                                )
                            )
                        }
                    }
                    results
                }
            }
        }
    }

    fun revertRelocation(destBookingId: Int, originalSourceLocation: String, qty: Int, user: String): String {
        return SqlClient.withWms { conn ->
            val dest = conn.prepareStatement(
                "SELECT * FROM dbo.rohteillager WHERE Rownumber = ?"
            ).use { ps ->
                ps.setInt(1, destBookingId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rowToBooking(rs) else throw IllegalStateException("Destination booking not found")
                }
            }

            val actualQty = minOf(qty, dest.qty)
            val newDestQty = dest.qty - actualQty

            if (newDestQty <= 0) {
                conn.prepareStatement("DELETE FROM dbo.rohteillager WHERE Rownumber = ?").use { ps ->
                    ps.setInt(1, destBookingId)
                    ps.executeUpdate()
                }
            } else {
                conn.prepareStatement("UPDATE dbo.rohteillager SET Menge = ? WHERE Rownumber = ?").use { ps ->
                    ps.setString(1, newDestQty.toString())
                    ps.setInt(2, destBookingId)
                    ps.executeUpdate()
                }
            }

            val bookingId: String
            conn.prepareStatement(
                """INSERT INTO dbo.rohteillager
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, dest.itemNumber.take(20))
                ps.setString(2, dest.description.take(40))
                ps.setString(3, originalSourceLocation.take(20))
                ps.setString(4, actualQty.toString().take(10))
                ps.setString(5, dest.itemType.take(20))
                ps.setString(6, dest.batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.executeUpdate()
                bookingId = ps.generatedKeys.use { keys ->
                    if (keys.next()) keys.getInt(1).toString() else "0"
                }
            }

            conn.prepareStatement(
                """INSERT INTO dbo.journal
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)"""
            ).use { ps ->
                ps.setString(1, dest.itemNumber.take(20))
                ps.setString(2, dest.description.take(40))
                ps.setString(3, dest.locationCode.take(20))
                ps.setString(4, actualQty.toString().take(10))
                ps.setString(5, dest.itemType.take(20))
                ps.setString(6, dest.batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.setString(8, "Storno Uml. -$actualQty".take(20))
                ps.executeUpdate()
            }

            conn.prepareStatement(
                """INSERT INTO dbo.journal
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
                   VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)"""
            ).use { ps ->
                ps.setString(1, dest.itemNumber.take(20))
                ps.setString(2, dest.description.take(40))
                ps.setString(3, originalSourceLocation.take(20))
                ps.setString(4, actualQty.toString().take(10))
                ps.setString(5, dest.itemType.take(20))
                ps.setString(6, dest.batchNumber.take(20))
                ps.setString(7, user.take(20))
                ps.setString(8, "Storno Uml. +$actualQty".take(20))
                ps.executeUpdate()
            }

            bookingId
        }
    }

    // ── FIFO lookup ──

    fun searchFifo(itemNumber: String): List<Booking> {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                """SELECT * FROM dbo.rohteillager
                   WHERE RTRIM(Artikelnummer) = ? AND CAST(RTRIM(Menge) AS INT) > 0
                   ORDER BY Datum ASC, Rownumber ASC"""
            ).use { ps ->
                ps.setString(1, itemNumber)
                ps.executeQuery().use { rs ->
                    val results = mutableListOf<Booking>()
                    while (rs.next()) results.add(rowToBooking(rs))
                    results
                }
            }
        }
    }

    // ── Zone / shelf queries ──

    fun getZoneOccupancy(shelfCodes: List<String>): Int {
        if (shelfCodes.isEmpty()) return 0
        val placeholders = shelfCodes.joinToString(" OR ") { "RTRIM(Lagerort) LIKE ?" }
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                """SELECT COUNT(DISTINCT RTRIM(Lagerort))
                   FROM dbo.rohteillager
                   WHERE CAST(RTRIM(Menge) AS INT) > 0 AND ($placeholders)"""
            ).use { ps ->
                shelfCodes.forEachIndexed { i, sc -> ps.setString(i + 1, "$sc-%") }
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    fun getOccupiedCells(shelfCode: String): List<String> {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                """SELECT DISTINCT RTRIM(Lagerort)
                   FROM dbo.rohteillager
                   WHERE RTRIM(Lagerort) LIKE ? AND CAST(RTRIM(Menge) AS INT) > 0"""
            ).use { ps ->
                ps.setString(1, "$shelfCode-%")
                ps.executeQuery().use { rs ->
                    val cells = mutableListOf<String>()
                    while (rs.next()) cells.add(rs.getString(1).trim())
                    cells
                }
            }
        }
    }

    fun getCellSlotCounts(shelfCode: String): Map<String, Int> {
        return SqlClient.withWms { conn ->
            conn.prepareStatement(
                """SELECT RTRIM(Lagerort) AS loc, COUNT(*) AS cnt
                   FROM dbo.rohteillager
                   WHERE RTRIM(Lagerort) LIKE ? AND CAST(RTRIM(Menge) AS INT) > 0
                   GROUP BY RTRIM(Lagerort)"""
            ).use { ps ->
                ps.setString(1, "$shelfCode-%")
                ps.executeQuery().use { rs ->
                    val map = mutableMapOf<String, Int>()
                    while (rs.next()) map[rs.getString("loc").trim()] = rs.getInt("cnt")
                    map
                }
            }
        }
    }

    // ── Helpers ──

    private fun rowToBooking(rs: ResultSet): Booking {
        return Booking(
            id = rs.getInt("Rownumber").toString(),
            locationCode = rs.getString("Lagerort")?.trim() ?: "",
            itemNumber = rs.getString("Artikelnummer")?.trim() ?: "",
            itemType = rs.getString("Art")?.trim() ?: "",
            batchNumber = rs.getString("Lieferschein")?.trim() ?: "",
            qty = (rs.getString("Menge")?.trim() ?: "0").toIntOrNull() ?: 0,
            description = rs.getString("Bezeichnung")?.trim() ?: "",
            bookedBy = rs.getString("Zusatz1")?.trim() ?: "",
            bookedAt = rs.getTimestamp("Datum")?.toString() ?: "",
            status = "Active"
        )
    }
}
