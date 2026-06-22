package com.wms.gridlocator.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wms.gridlocator.data.*
import com.wms.gridlocator.i18n.Language
import com.wms.gridlocator.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab { GRID_ZONES, INPUT_STOCK, CONSUME_STOCK }
enum class InputStockMode { NEW_BOOKING, RELOCATE }

private const val PREFS_NAME = "wms_session"
private const val KEY_USERNAME = "username"
private const val KEY_ROLE = "role"
private const val KEY_LOGIN_TIME = "login_time"
private const val KEY_LANGUAGE = "language"
private const val SESSION_DURATION_MS = 24 * 60 * 60 * 1000L

data class UiState(
    val configLoaded: Boolean = false,
    val loggedIn: Boolean = false,
    val username: String = "",
    val role: String = "",
    val loginError: String? = null,
    val currentTab: AppTab = AppTab.GRID_ZONES,
    val selectedZone: String? = null,
    val selectedShelf: String? = null,
    val selectedCell: String? = null,
    val cellContents: List<Booking> = emptyList(),
    val occupiedCells: Set<String> = emptySet(),
    val zoneOccupancy: Map<String, Int> = emptyMap(),
    val actionMessage: String? = null,
    val language: Language = Language.DE,
    val fifoResults: List<Booking> = emptyList(),
    val fifoPlan: List<FifoPlanItem> = emptyList(),
    val fifoSearched: Boolean = false,
    val inputStockMode: InputStockMode = InputStockMode.NEW_BOOKING
)

class WmsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = Repository()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var config: GridConfig = GridConfig()
        private set

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        val savedLang = prefs.getString(KEY_LANGUAGE, Language.DE.code)
        val lang = Language.entries.find { it.code == savedLang } ?: Language.DE
        _state.value = _state.value.copy(language = lang)

        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    DbRepository.initDb()
                }
                config = ConfigLoader.load(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "Initialization failed", e)
            }
            restoreSession()
            _state.value = _state.value.copy(configLoaded = true)
        }
    }

    fun setLanguage(language: Language) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _state.value = _state.value.copy(language = language)
    }

    private suspend fun restoreSession() {
        val savedUser = prefs.getString(KEY_USERNAME, null) ?: return
        val savedRole = prefs.getString(KEY_ROLE, null) ?: return
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (System.currentTimeMillis() - loginTime > SESSION_DURATION_MS) {
            clearSession()
            return
        }
        _state.value = _state.value.copy(
            loggedIn = true, username = savedUser, role = savedRole, loginError = null
        )
        if (config.zones.isNotEmpty()) loadZoneOccupancies()
    }

    private fun saveSession(username: String, role: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val trimmedUsername = username.trim()
                val response = repo.login(trimmedUsername, password)
                if (response.ok) {
                    saveSession(response.username ?: trimmedUsername, response.role ?: "")
                    _state.value = _state.value.copy(
                        loggedIn = true, username = response.username ?: trimmedUsername, role = response.role ?: "", loginError = null
                    )
                    loadZoneOccupancies()
                } else {
                    val s = Strings.get(_state.value.language)
                    _state.value = _state.value.copy(loginError = response.error ?: s.invalidCredentials)
                }
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "Login failed", e)
                val s = Strings.get(_state.value.language)
                _state.value = _state.value.copy(loginError = "${s.connectionError}: ${e.message}")
            }
        }
    }

    fun logout() {
        clearSession()
        _state.value = UiState(configLoaded = true)
    }

    fun clearLoginError() {
        _state.value = _state.value.copy(loginError = null)
    }

    fun selectZone(zoneCode: String) {
        val shelves = config.zones[zoneCode]?.shelves?.keys?.sorted() ?: return
        if (shelves.isEmpty()) return
        val firstShelf = shelves.first()
        _state.value = _state.value.copy(
            selectedZone = zoneCode, selectedShelf = firstShelf, selectedCell = null, cellContents = emptyList()
        )
        loadOccupiedCells(firstShelf)
    }

    fun selectShelf(shelfCode: String) {
        _state.value = _state.value.copy(selectedShelf = shelfCode, selectedCell = null, cellContents = emptyList())
        loadOccupiedCells(shelfCode)
    }

    fun selectCell(locationCode: String) {
        viewModelScope.launch {
            try {
                val contents = repo.getCellContents(locationCode)
                if (contents.isEmpty() && _state.value.currentTab == AppTab.GRID_ZONES) {
                    val s = Strings.get(_state.value.language)
                    _state.value = _state.value.copy(actionMessage = "$locationCode — ${s.empty}")
                } else {
                    _state.value = _state.value.copy(selectedCell = locationCode, cellContents = contents, actionMessage = null)
                }
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "selectCell failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
            }
        }
    }

    fun bookIn(
        locationCode: String, itemNumber: String, itemType: String,
        batchNumber: String, qty: Int, description: String
    ) {
        viewModelScope.launch {
            try {
                val response = repo.bookIn(
                    locationCode, itemNumber, itemType, batchNumber, qty, description, _state.value.username
                )
                if (response.ok) {
                    val s = Strings.get(_state.value.language)
                    _state.value = _state.value.copy(actionMessage = "${s.bookedMessage} $qty x $itemNumber", selectedCell = null)
                    refreshCurrentShelf()
                } else {
                    _state.value = _state.value.copy(actionMessage = response.error ?: "Book-in failed")
                }
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "bookIn failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
            }
        }
    }

    fun consume(bookingId: String, qty: Int) {
        viewModelScope.launch {
            try {
                val response = repo.consume(bookingId, qty, _state.value.username)
                if (response.ok) {
                    val s = Strings.get(_state.value.language)
                    _state.value = _state.value.copy(actionMessage = "$qty ${s.consumedMessage}")
                    val cell = _state.value.selectedCell
                    if (cell != null) selectCell(cell)
                    refreshCurrentShelf()
                } else {
                    _state.value = _state.value.copy(actionMessage = response.error ?: "Consume failed")
                }
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "consume failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
            }
        }
    }

    suspend fun getLocationSuggestions(itemNumber: String, sourceDate: String): List<Pair<String, String>> {
        return try {
            repo.getLocationSuggestions(itemNumber, sourceDate)
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "getLocationSuggestions failed", e)
            emptyList()
        }
    }

    suspend fun getAllCellSlotCounts(): Map<String, Int> {
        return try {
            repo.getAllCellSlotCounts()
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "getAllCellSlotCounts failed", e)
            emptyMap()
        }
    }

    suspend fun getCellSlotCounts(shelfCode: String): Map<String, Int> {
        return try {
            repo.getCellSlotCounts(shelfCode)
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "getCellSlotCounts failed", e)
            emptyMap()
        }
    }

    suspend fun getMergeCandidates(itemNumber: String): List<MergeCandidate> {
        return try {
            repo.getMergeCandidates(itemNumber)
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "getMergeCandidates failed", e)
            emptyList()
        }
    }

    suspend fun searchStockForRelocation(itemNumber: String): List<Booking> {
        return try {
            repo.searchStock(itemNumber)
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "searchStock failed", e)
            emptyList()
        }
    }

    suspend fun relocate(sourceBookingId: String, destLocationCode: String, qty: Int) {
        try {
            val response = repo.relocate(sourceBookingId, destLocationCode, qty, _state.value.username)
            if (response.ok) {
                val s = Strings.get(_state.value.language)
                _state.value = _state.value.copy(
                    actionMessage = "$qty x ${s.relocatedMessage}",
                    selectedCell = null
                )
                refreshCurrentShelf()
            } else {
                _state.value = _state.value.copy(actionMessage = response.error ?: "Relocation failed")
            }
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "relocate failed", e)
            _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
        }
    }

    suspend fun getRecentRelocations(): List<RecentRelocation> {
        return try {
            repo.getRecentRelocations()
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "getRecentRelocations failed", e)
            emptyList()
        }
    }

    fun revertRelocation(destBookingId: String, originalSourceLocation: String, qty: Int) {
        viewModelScope.launch {
            try {
                val response = repo.revertRelocation(destBookingId, originalSourceLocation, qty, _state.value.username)
                if (response.ok) {
                    val s = Strings.get(_state.value.language)
                    _state.value = _state.value.copy(
                        actionMessage = "$qty x ${s.revertedMessage}"
                    )
                    refreshCurrentShelf()
                } else {
                    _state.value = _state.value.copy(actionMessage = response.error ?: "Revert failed")
                }
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "revertRelocation failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
            }
        }
    }

    suspend fun getErpDeliveries(): List<ErpDelivery> {
        return try {
            repo.getErpDeliveries()
        } catch (e: Exception) {
            android.util.Log.e("WmsViewModel", "getErpDeliveries failed", e)
            emptyList()
        }
    }

    fun selectTab(tab: AppTab) {
        _state.value = _state.value.copy(
            currentTab = tab,
            selectedZone = null, selectedShelf = null, selectedCell = null,
            cellContents = emptyList(),
            fifoResults = emptyList(), fifoPlan = emptyList(), fifoSearched = false,
            inputStockMode = InputStockMode.NEW_BOOKING
        )
    }

    fun setInputStockMode(mode: InputStockMode) {
        _state.value = _state.value.copy(inputStockMode = mode)
    }

    fun searchFifo(itemNumber: String, requestedQty: Int) {
        viewModelScope.launch {
            try {
                val stock = repo.searchFifo(itemNumber)
                val plan = mutableListOf<FifoPlanItem>()
                var remaining = requestedQty
                for (b in stock) {
                    if (remaining <= 0) break
                    val take = minOf(remaining, b.qty)
                    plan.add(FifoPlanItem(b.id, b.locationCode, b.itemNumber, b.batchNumber, b.qty, take, b.bookedAt))
                    remaining -= take
                }
                _state.value = _state.value.copy(fifoResults = stock, fifoPlan = plan, fifoSearched = true)
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "searchFifo failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}", fifoSearched = true)
            }
        }
    }

    fun consumeFifo() {
        val plan = _state.value.fifoPlan
        if (plan.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = repo.consumeFifo(plan, _state.value.username)
                if (result.ok) {
                    val total = plan.sumOf { it.take }
                    val s = Strings.get(_state.value.language)
                    _state.value = _state.value.copy(
                        actionMessage = "$total ${s.consumedMessage}",
                        fifoResults = emptyList(), fifoPlan = emptyList(), fifoSearched = false
                    )
                    loadZoneOccupancies()
                } else {
                    _state.value = _state.value.copy(actionMessage = result.error ?: "Consume failed")
                }
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "consumeFifo failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
            }
        }
    }

    fun clearFifo() {
        _state.value = _state.value.copy(fifoResults = emptyList(), fifoPlan = emptyList(), fifoSearched = false)
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }

    fun dismissCellDetails() {
        _state.value = _state.value.copy(selectedCell = null, cellContents = emptyList())
    }

    fun goBackToZones() {
        _state.value = _state.value.copy(selectedZone = null, selectedShelf = null, selectedCell = null, cellContents = emptyList())
    }

    private fun loadZoneOccupancies() {
        viewModelScope.launch {
            try {
                if (config.zones.isEmpty()) return@launch
                val zones = repo.getZonesOverview(config.zones)
                val occ = zones.associate { it.code to it.occupied }
                _state.value = _state.value.copy(zoneOccupancy = occ)
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "loadZoneOccupancies failed", e)
                _state.value = _state.value.copy(actionMessage = "Error loading zones: ${e.message}")
            }
        }
    }

    private fun loadOccupiedCells(shelfCode: String) {
        viewModelScope.launch {
            try {
                val occupied = repo.getOccupiedCells(shelfCode)
                _state.value = _state.value.copy(occupiedCells = occupied)
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "loadOccupiedCells failed", e)
            }
        }
    }

    private fun refreshCurrentShelf() {
        val shelf = _state.value.selectedShelf ?: return
        loadOccupiedCells(shelf)
        loadZoneOccupancies()
    }
}
