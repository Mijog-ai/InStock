package com.wms.gridlocator.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.favre.lib.crypto.bcrypt.BCrypt
import com.wms.gridlocator.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val PREFS_NAME = "wms_session"
private const val KEY_USERNAME = "username"
private const val KEY_ROLE = "role"
private const val KEY_LOGIN_TIME = "login_time"
private const val SESSION_DURATION_MS = 24 * 60 * 60 * 1000L

data class UiState(
    val configLoaded: Boolean = false,
    val loggedIn: Boolean = false,
    val username: String = "",
    val role: String = "",
    val loginError: String? = null,
    val selectedZone: String? = null,
    val selectedShelf: String? = null,
    val selectedCell: String? = null,
    val cellContents: List<Booking> = emptyList(),
    val occupiedCells: Set<String> = emptySet(),
    val zoneOccupancy: Map<String, Int> = emptyMap(),
    val actionMessage: String? = null
)

class WmsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = Repository()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var config: GridConfig = GridConfig()
        private set

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                config = ConfigLoader.load()
                seedDatabase(repo)
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "Init failed", e)
            }
            restoreSession()
            _state.value = _state.value.copy(configLoaded = true)
        }
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
        loadZoneOccupancies()
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
                val user = repo.getUser(trimmedUsername)
                if (user != null) {
                    val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
                    if (result.verified) {
                        saveSession(user.username, user.role)
                        _state.value = _state.value.copy(
                            loggedIn = true, username = user.username, role = user.role, loginError = null
                        )
                        loadZoneOccupancies()
                        return@launch
                    }
                }
                _state.value = _state.value.copy(loginError = "Invalid username or password")
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "Login failed", e)
                _state.value = _state.value.copy(loginError = "Connection error: ${e.message}")
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
                _state.value = _state.value.copy(selectedCell = locationCode, cellContents = contents, actionMessage = null)
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
                val maxSlots = config.maxSlotsPerCell
                val current = repo.getActiveSlotCount(locationCode)
                if (current >= maxSlots) {
                    _state.value = _state.value.copy(actionMessage = "Cell at max capacity ($maxSlots)")
                    return@launch
                }
                repo.bookIn(locationCode, itemNumber, itemType, batchNumber, qty, description, _state.value.username)
                _state.value = _state.value.copy(actionMessage = "Booked $qty x $itemNumber", selectedCell = null)
                refreshCurrentShelf()
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "bookIn failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
            }
        }
    }

    fun consume(bookingId: String, qty: Int) {
        viewModelScope.launch {
            try {
                repo.consume(bookingId, qty, _state.value.username)
                _state.value = _state.value.copy(actionMessage = "Consumed $qty units")
                val cell = _state.value.selectedCell
                if (cell != null) selectCell(cell)
                refreshCurrentShelf()
            } catch (e: Exception) {
                android.util.Log.e("WmsViewModel", "consume failed", e)
                _state.value = _state.value.copy(actionMessage = "Error: ${e.message}")
            }
        }
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
                val occ = mutableMapOf<String, Int>()
                for ((zone, zoneCfg) in config.zones) {
                    val shelfCodes = zoneCfg.shelves.keys.toList()
                    occ[zone] = repo.getZoneOccupancy(shelfCodes)
                }
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
                val cells = repo.getOccupiedCells(shelfCode)
                _state.value = _state.value.copy(occupiedCells = cells)
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
