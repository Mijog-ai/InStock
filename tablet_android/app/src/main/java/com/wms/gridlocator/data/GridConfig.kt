package com.wms.gridlocator.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GridConfig(
    @SerializedName("max_slots_per_cell") val maxSlotsPerCell: Int = 2,
    val zones: Map<String, ZoneConfig> = emptyMap()
)

data class ZoneConfig(
    @SerializedName("display_name") val displayName: String = "",
    val shelves: Map<String, ShelfConfig> = emptyMap()
)

data class ShelfConfig(
    val sections: Int = 4,
    val rows: Int = 6
)

object ConfigLoader {
    private var config: GridConfig? = null
    private val gson = Gson()

    suspend fun load(context: Context): GridConfig = withContext(Dispatchers.IO) {
        if (config != null) return@withContext config!!
        val json = context.assets.open("grid_config.json").bufferedReader().readText()
        config = gson.fromJson(json, GridConfig::class.java)
        config!!
    }

    fun getTotalCells(zoneConfig: ZoneConfig): Int {
        return zoneConfig.shelves.values.sumOf { it.sections * it.rows }
    }
}
