package com.wms.gridlocator.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wms.gridlocator.data.Booking
import com.wms.gridlocator.data.RecentRelocation
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelocateScreen(viewModel: WmsViewModel) {
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val config = viewModel.config
    val maxSlots = config.maxSlotsPerCell

    // Source state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf<Booking?>(null) }

    // Destination state
    var availableCells by remember { mutableStateOf<List<String>>(emptyList()) }
    var cellSlotCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var loadingCells by remember { mutableStateOf(false) }
    var selectedDestCell by remember { mutableStateOf("") }

    // Location suggestions based on same part number
    var locationSuggestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    var cellExpanded by remember { mutableStateOf(false) }

    // Qty + validation
    var relocateQty by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var relocating by remember { mutableStateOf(false) }

    // Recent relocations
    var recentRelocations by remember { mutableStateOf<List<RecentRelocation>>(emptyList()) }
    var loadingRecent by remember { mutableStateOf(true) }
    var revertTarget by remember { mutableStateOf<RecentRelocation?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        loadingRecent = true
        recentRelocations = viewModel.getRecentRelocations()
        loadingRecent = false
    }

    LaunchedEffect(selectedSource) {
        locationSuggestions = if (selectedSource != null) {
            val srcDate = selectedSource!!.bookedAt.take(10)
            viewModel.getLocationSuggestions(selectedSource!!.itemNumber, srcDate)
                .filter { it.first != selectedSource!!.locationCode }
        } else emptyList()
    }

    // Load all available cells across all zones/shelves
    LaunchedEffect(Unit) {
        loadingCells = true
        val slotCounts = viewModel.getAllCellSlotCounts()
        cellSlotCounts = slotCounts
        val cells = mutableListOf<String>()
        for ((_, zone) in config.zones.entries.sortedBy { it.key }) {
            for ((shelfKey, shelfCfg) in zone.shelves.entries.sortedBy { it.key }) {
                for (sec in 1..shelfCfg.sections) {
                    for (row in 1..shelfCfg.rows) {
                        val code = "$shelfKey-${"%02d".format(sec)}-${"%02d".format(row)}"
                        val used = slotCounts[code] ?: 0
                        if (used == 0) cells.add(code)
                    }
                }
            }
        }
        availableCells = cells
        loadingCells = false
    }

    // Revert confirmation dialog
    revertTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { revertTarget = null },
            title = { Text(s.revertConfirmTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.revertConfirmMessage)
                    Spacer(Modifier.height(8.dp))
                    Text("${s.item}: ${target.itemNumber}", fontSize = 13.sp, color = TextSecondary)
                    Text("${target.toLocation} → ${target.fromLocation}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                    Text("${s.qty}: ${target.qty}", fontSize = 13.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revertRelocation(target.destBookingId, target.fromLocation, target.qty)
                        revertTarget = null
                        refreshTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CellRed)
                ) { Text(s.revert, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { revertTarget = null }) { Text(s.cancel) }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(s.relocateScreenTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(s.relocateScreenSubtitle, fontSize = 13.sp, color = TextSecondary)
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Left: Source stock ──
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(s.step1Source, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))

                    // Search
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(s.itemNumber) },
                            placeholder = { Text(s.searchPlaceholder, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    searching = true
                                    selectedSource = null
                                    relocateQty = ""
                                    validationError = null
                                    scope.launch {
                                        searchResults = viewModel.searchStockForRelocation(searchQuery.trim())
                                        searching = false
                                        searched = true
                                    }
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Text(s.searchStock, fontWeight = FontWeight.Bold) }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Results table
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border),
                        color = Surface
                    ) {
                        if (searching) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (!searched) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(s.relocateSubtitle, color = TextSecondary, fontSize = 13.sp)
                            }
                        } else if (searchResults.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(s.noStockFound, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = CellRed)
                                    Text("${s.noActiveBookingsFor} ${searchQuery.trim()}", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        } else {
                            LazyColumn {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SurfaceContainer)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(s.location, Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Text(s.item, Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Text(s.batch, Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Text(s.qty, Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Text(s.date, Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    }
                                }
                                itemsIndexed(searchResults) { idx, booking ->
                                    val isSelected = selectedSource?.id == booking.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedSource = booking
                                                relocateQty = booking.qty.toString()
                                                validationError = null
                                            }
                                            .background(if (isSelected) Accent.copy(alpha = 0.12f) else SurfaceWhite)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(booking.locationCode, Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(booking.itemNumber, Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(booking.batchNumber, Modifier.weight(1.5f), fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${booking.qty}", Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(
                                            if (booking.bookedAt.length >= 10) booking.bookedAt.substring(0, 10) else booking.bookedAt,
                                            Modifier.weight(1.2f), fontSize = 11.sp, color = TextSecondary
                                        )
                                    }
                                    if (idx < searchResults.lastIndex) {
                                        HorizontalDivider(color = Border.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }

                    // Selected source summary
                    if (selectedSource != null) {
                        Spacer(Modifier.height(8.dp))
                        val src = selectedSource!!
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Accent.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.fromLocation, fontSize = 11.sp, color = TextSecondary)
                                    Text(src.locationCode, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Accent)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.itemNo, fontSize = 11.sp, color = TextSecondary)
                                    Text(src.itemNumber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.quantity, fontSize = 11.sp, color = TextSecondary)
                                    Text("${src.qty}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // ── Right: Destination + Confirm + Recent Relocations ──
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Destination card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(s.step2Destination, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(12.dp))

                        // Destination cell + Qty in a row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = cellExpanded,
                                onExpandedChange = { cellExpanded = !cellExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedDestCell.ifBlank { "" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(s.selectDestCell) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cellExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    supportingText = {
                                        if (loadingCells) Text("...", fontSize = 12.sp)
                                        else Text("${availableCells.size} ${s.available}", fontSize = 12.sp, color = TextSecondary)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = cellExpanded,
                                    onDismissRequest = { cellExpanded = false },
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    val suggestedCells = locationSuggestions
                                        .filter { (cellSlotCounts[it.first] ?: 0) < maxSlots }
                                    if (availableCells.isEmpty() && suggestedCells.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text(s.allCellsFull, color = CellRed) },
                                            onClick = { cellExpanded = false }
                                        )
                                    } else {
                                        suggestedCells.forEach { (loc, dateStr) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "★ $loc (${dateStr.take(10)})",
                                                        color = CellGreen,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                onClick = {
                                                    selectedDestCell = loc
                                                    cellExpanded = false
                                                    validationError = null
                                                }
                                            )
                                        }
                                        if (suggestedCells.isNotEmpty()) {
                                            HorizontalDivider(color = Border, thickness = 1.dp)
                                        }
                                        val remaining = availableCells.filter { cell -> suggestedCells.none { it.first == cell } }
                                        remaining.forEach { cell ->
                                            DropdownMenuItem(
                                                text = { Text(cell) },
                                                onClick = {
                                                    selectedDestCell = cell
                                                    cellExpanded = false
                                                    validationError = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = relocateQty,
                                onValueChange = {
                                    relocateQty = it.filter { c -> c.isDigit() }
                                    validationError = null
                                },
                                label = { Text(s.qtyToRelocate) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        validationError?.let {
                            Text(it, color = CellRed, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val src = selectedSource
                                val q = relocateQty.toIntOrNull() ?: 0
                                when {
                                    src == null -> validationError = s.selectSourceStock
                                    selectedDestCell.isBlank() -> validationError = s.selectDestCell
                                    q <= 0 -> validationError = s.qtyMustBePositive
                                    q > src.qty -> validationError = "${s.maxAvailable}: ${src.qty}"
                                    selectedDestCell == src.locationCode -> validationError = s.selectDestCell
                                    else -> {
                                        validationError = null
                                        relocating = true
                                        scope.launch {
                                            viewModel.relocate(src.id, selectedDestCell, q)
                                            relocating = false
                                            selectedSource = null
                                            searchResults = emptyList()
                                            searched = false
                                            searchQuery = ""
                                            relocateQty = ""
                                            selectedDestCell = ""
                                            refreshTrigger++
                                        }
                                    }
                                }
                            },
                            enabled = !relocating,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Text(s.relocateConfirm, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Recent relocations with revert ──
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(s.recentRelocations, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))

                        if (loadingRecent) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            }
                        } else if (recentRelocations.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(s.noRecentRelocations, color = TextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(recentRelocations) { _, reloc ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Surface,
                                        border = BorderStroke(1.dp, Border.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    reloc.itemNumber,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${reloc.fromLocation} → ${reloc.toLocation}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Accent
                                                )
                                                Text(
                                                    "${s.qty}: ${reloc.qty} · ${reloc.movedBy} · ${
                                                        if (reloc.movedAt.length >= 10) reloc.movedAt.substring(0, 10) else reloc.movedAt
                                                    }",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { revertTarget = reloc },
                                                modifier = Modifier.height(36.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CellRed),
                                                border = BorderStroke(1.dp, CellRed.copy(alpha = 0.5f))
                                            ) {
                                                Text(s.revert, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
