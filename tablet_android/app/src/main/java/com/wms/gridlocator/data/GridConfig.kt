package com.wms.gridlocator.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class GridConfig(
    val maxSlotsPerCell: Int = 2,
    val zones: Map<String, ZoneConfig> = emptyMap()
)

data class ZoneConfig(
    val displayName: String = "",
    val shelves: Map<String, ShelfConfig> = emptyMap()
)

data class ShelfConfig(
    val sections: Int = 4,
    val rows: Int = 6
)

object ConfigLoader {
    private var config: GridConfig? = null

    suspend fun load(): GridConfig {
        if (config != null) return config!!
        val doc = FirebaseFirestore.getInstance()
            .collection("app_config").document("grid_config")
            .get().await()
        if (!doc.exists()) {
            config = GridConfig()
            return config!!
        }
        val data = doc.data ?: run { config = GridConfig(); return config!! }
        val maxSlots = (data["max_slots_per_cell"] as? Long)?.toInt() ?: 2

        @Suppress("UNCHECKED_CAST")
        val zonesRaw = data["zones"] as? Map<String, Map<String, Any>> ?: emptyMap()
        val zones = zonesRaw.mapValues { (_, zoneData) ->
            val displayName = zoneData["display_name"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val shelvesRaw = zoneData["shelves"] as? Map<String, Map<String, Any>> ?: emptyMap()
            val shelves = shelvesRaw.mapValues { (_, shelfData) ->
                ShelfConfig(
                    sections = (shelfData["sections"] as? Long)?.toInt() ?: 4,
                    rows = (shelfData["rows"] as? Long)?.toInt() ?: 6
                )
            }
            ZoneConfig(displayName = displayName, shelves = shelves)
        }
        config = GridConfig(maxSlotsPerCell = maxSlots, zones = zones)
        return config!!
    }

    fun getTotalCells(zoneConfig: ZoneConfig): Int {
        return zoneConfig.shelves.values.sumOf { it.sections * it.rows }
    }
}
