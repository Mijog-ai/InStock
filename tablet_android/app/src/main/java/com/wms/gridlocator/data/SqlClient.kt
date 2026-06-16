package com.wms.gridlocator.data

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager

object SqlClient {

    private const val WMS_URL = "jdbc:jtds:sqlserver://192.168.80.25:1433/Rohteillager"
    private const val WMS_USER = "guehringCS01"
    private const val WMS_PASS = "M+5WGWa..sD?gfC~RGHz"

    private const val ERP_URL = "jdbc:jtds:sqlserver://192.168.80.22:1433/XALinl"
    private const val ERP_USER = "XAL_ODBC"
    private const val ERP_PASS = "XAL_ODBC"

    init {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            Log.d("SqlClient", "jTDS driver loaded")
        } catch (e: Exception) {
            Log.e("SqlClient", "Failed to load jTDS driver", e)
        }
    }

    fun getWmsConnection(): Connection {
        Log.d("SqlClient", "Connecting to WMS at $WMS_URL")
        return DriverManager.getConnection(WMS_URL, WMS_USER, WMS_PASS)
    }

    fun getErpConnection(): Connection {
        Log.d("SqlClient", "Connecting to ERP at $ERP_URL")
        return DriverManager.getConnection(ERP_URL, ERP_USER, ERP_PASS)
    }

    fun <T> withWms(block: (Connection) -> T): T {
        val conn = getWmsConnection()
        return try {
            conn.autoCommit = false
            val result = block(conn)
            conn.commit()
            result
        } catch (e: Throwable) {
            try { conn.rollback() } catch (_: Exception) {}
            throw e
        } finally {
            try { conn.close() } catch (_: Exception) {}
        }
    }

    fun <T> withErp(block: (Connection) -> T): T {
        val conn = getErpConnection()
        return try {
            block(conn)
        } finally {
            try { conn.close() } catch (_: Exception) {}
        }
    }
}
