package com.wms.gridlocator.data

import android.util.Log

object ErpRepository {

    fun getRecentDeliveries(): List<ErpDelivery> {
        return try {
            SqlClient.withErp { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(
                        """SELECT dbo.CREDDLVJOUR.LASTCHANGED,
                           dbo.CREDDLVJOUR.PURCHASENUMBER,
                           dbo.CREDDLVTRANS.ITEMNUMBER,
                           dbo.CREDDLVTRANS.ORDERED,
                           dbo.CREDDLVJOUR.DELIVERYNOTE,
                           dbo.CREDDLVJOUR.EXTERNALDELIVERYNOTE,
                           dbo.CREDTABLE.NAME,
                           dbo.CREDDLVTRANS.DELIVERYDATE
                        FROM dbo.CREDDLVJOUR
                        INNER JOIN dbo.CREDDLVTRANS
                            ON dbo.CREDDLVJOUR.DELIVERYNOTE = dbo.CREDDLVTRANS.DELIVERYNOTE
                        INNER JOIN dbo.CREDTABLE
                            ON LTRIM(RTRIM(dbo.CREDDLVJOUR.ORDERACCOUNT)) = dbo.CREDTABLE.ACCOUNTNUMBER
                        WHERE dbo.CREDDLVTRANS.DELIVERYDATE >= CURRENT_TIMESTAMP - 70
                          AND (dbo.CREDTABLE.NAME LIKE '%Hengli%' OR dbo.CREDTABLE.NAME LIKE '%Wenling%')
                        ORDER BY dbo.CREDDLVJOUR.LASTCHANGED"""
                    ).use { rs ->
                        val deliveries = mutableListOf<ErpDelivery>()
                        while (rs.next()) {
                            deliveries.add(
                                ErpDelivery(
                                    itemNumber = rs.getString("ITEMNUMBER") ?: "",
                                    purchaseNumber = rs.getString("PURCHASENUMBER") ?: "",
                                    ordered = rs.getDouble("ORDERED"),
                                    deliveryNote = rs.getString("DELIVERYNOTE") ?: "",
                                    externalDeliveryNote = rs.getString("EXTERNALDELIVERYNOTE") ?: "",
                                    supplierName = rs.getString("NAME") ?: "",
                                    deliveryDate = rs.getTimestamp("DELIVERYDATE")?.toString() ?: ""
                                )
                            )
                        }
                        deliveries
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ErpRepository", "ERP query error", e)
            emptyList()
        }
    }
}
