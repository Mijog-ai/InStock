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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wms.gridlocator.data.Booking
import com.wms.gridlocator.data.MergeCandidate
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel
import kotlinx.coroutines.launch

private enum class DestinationMode { MERGE, NEW_LOCATION }

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
    var destinationMode by remember { mutableStateOf(DestinationMode.MERGE) }
    var availableCells by remember { mutableStateOf<List<String>>(emptyList()) }
    var cellSlotCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var loadingCells by remember { mutableStateOf(false) }
    var selectedDestCell by remember { mutableStateOf("") }
    var cellFilter by remember { mutableStateOf("") }

    // Merge candidates
    var mergeCandidates by remember { mutableStateOf<List<MergeCandidate>>(emptyList()) }
    var loadingMerge by remember { mutableStateOf(false) }

    // Location suggestions
    var locationSuggestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // Qty + validation
    var relocateQty by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var relocating by remember { mutableStateOf(false) }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedSource) {
        if (selectedSource != null) {
            val srcDate = selectedSource!!.bookedAt.take(10)
            locationSuggestions = viewModel.getLocationSuggestions(selectedSource!!.itemNumber, srcDate)
                .filter { it.first != selectedSource!!.locationCode }
            loadingMerge = true
            mergeCandidates = viewModel.getMergeCandidates(selectedSource!!.itemNumber)
                .filter { it.locationCode != selectedSource!!.locationCode }
            loadingMerge = false
        } else {
            locationSuggestions = emptyList()
            mergeCandidates = emptyList()
        }
        selectedDestCell = ""
    }

    // Load all available empty cells
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

    Column(
        modifier = Modifier.fillMaxSize().background(Surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

            // ── Right: Destination + Recent Relocations ──
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Destination: Mode toggle + Cell list (combined card) ──
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header: title + mode toggle
                        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)) {
                            Text(s.step2Destination, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DestModeButton(
                                    label = s.mergeStock,
                                    subtitle = s.mergeDescription,
                                    isActive = destinationMode == DestinationMode.MERGE,
                                    modifier = Modifier.weight(1f)
                                ) { destinationMode = DestinationMode.MERGE; selectedDestCell = "" }
                                DestModeButton(
                                    label = s.newStockLocation,
                                    subtitle = s.newLocationDescription,
                                    isActive = destinationMode == DestinationMode.NEW_LOCATION,
                                    modifier = Modifier.weight(1f)
                                ) { destinationMode = DestinationMode.NEW_LOCATION; selectedDestCell = "" }
                            }
                        }
                        HorizontalDivider(color = Border.copy(alpha = 0.3f))

                        // Cell list body
                        if (selectedSource == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(s.selectSourceFirst, color = TextSecondary, fontSize = 13.sp)
                            }
                        } else if (destinationMode == DestinationMode.MERGE) {
                            // ── Merge: cells with same item, sorted lowest-filled first ──
                            if (loadingMerge) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else if (mergeCandidates.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(s.noMergeCandidates, fontSize = 14.sp, color = TextSecondary)
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SurfaceContainer)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(s.location, Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Text(s.currentStock, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Text(s.slotsUsed, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    }
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        itemsIndexed(mergeCandidates) { idx, candidate ->
                                            val isFull = candidate.slotCount >= maxSlots
                                            val isSelected = candidate.locationCode == selectedDestCell
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (!isFull) Modifier.clickable {
                                                            selectedDestCell = candidate.locationCode
                                                            validationError = null
                                                        } else Modifier
                                                    )
                                                    .background(
                                                        when {
                                                            isSelected -> Accent.copy(alpha = 0.12f)
                                                            isFull -> CellRed.copy(alpha = 0.05f)
                                                            else -> SurfaceWhite
                                                        }
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    candidate.locationCode,
                                                    Modifier.weight(1.5f),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isFull) TextSecondary.copy(alpha = 0.5f) else Accent
                                                )
                                                Text(
                                                    "${candidate.itemQty}",
                                                    Modifier.weight(1f),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isFull) TextSecondary.copy(alpha = 0.5f) else TextPrimary
                                                )
                                                if (isFull) {
                                                    Surface(
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = CellRed.copy(alpha = 0.1f)
                                                    ) {
                                                        Text(
                                                            s.cellFull,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CellRed,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        "${candidate.slotCount}/$maxSlots",
                                                        Modifier.weight(1f),
                                                        fontSize = 12.sp,
                                                        color = if (isSelected) Accent else TextSecondary
                                                    )
                                                }
                                            }
                                            if (idx < mergeCandidates.lastIndex) {
                                                HorizontalDivider(color = Border.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // ── New Location: empty cells with filter ──
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                OutlinedTextField(
                                    value = cellFilter,
                                    onValueChange = { cellFilter = it },
                                    placeholder = { Text(s.filterCells, fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                                Spacer(Modifier.height(6.dp))

                                val filteredCells = remember(availableCells, cellFilter) {
                                    val q = cellFilter.trim().lowercase()
                                    if (q.isEmpty()) availableCells
                                    else availableCells.filter { it.lowercase().contains(q) }
                                }

                                if (loadingCells) {
                                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                } else if (filteredCells.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (cellFilter.isNotEmpty()) "${s.noMatches} \"$cellFilter\"" else s.allCellsFull,
                                            color = TextSecondary, fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    val suggestedSet = locationSuggestions.map { it.first }.toSet()
                                    val suggested = filteredCells.filter { it in suggestedSet }
                                    val rest = filteredCells.filter { it !in suggestedSet }

                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        if (suggested.isNotEmpty()) {
                                            items(suggested.size) { idx ->
                                                val cell = suggested[idx]
                                                val dateStr = locationSuggestions.firstOrNull { it.first == cell }?.second ?: ""
                                                val isSelected = cell == selectedDestCell
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedDestCell = cell
                                                            validationError = null
                                                        }
                                                        .background(if (isSelected) Accent.copy(alpha = 0.12f) else CellGreen.copy(alpha = 0.06f))
                                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("★ ", color = CellGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        Text(cell, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CellGreen)
                                                    }
                                                    Text(dateStr.take(10), fontSize = 11.sp, color = TextSecondary)
                                                }
                                                if (idx < suggested.lastIndex || rest.isNotEmpty()) {
                                                    HorizontalDivider(color = Border.copy(alpha = 0.3f))
                                                }
                                            }
                                            if (rest.isNotEmpty()) {
                                                item { HorizontalDivider(color = Border, thickness = 1.dp) }
                                            }
                                        }

                                        items(rest.size) { idx ->
                                            val cell = rest[idx]
                                            val isSelected = cell == selectedDestCell
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedDestCell = cell
                                                        validationError = null
                                                    }
                                                    .background(if (isSelected) Accent.copy(alpha = 0.12f) else SurfaceWhite)
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    cell,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) Accent else TextPrimary
                                                )
                                            }
                                            if (idx < rest.lastIndex) {
                                                HorizontalDivider(color = Border.copy(alpha = 0.2f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Qty + Confirm (compact) ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Border.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (selectedDestCell.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Accent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(s.destinationLocation, fontSize = 10.sp, color = TextSecondary)
                                        Text(selectedDestCell, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Accent)
                                    }
                                    if (destinationMode == DestinationMode.MERGE) {
                                        val candidate = mergeCandidates.find { it.locationCode == selectedDestCell }
                                        if (candidate != null) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(s.currentStock, fontSize = 10.sp, color = TextSecondary)
                                                Text("${candidate.itemQty}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                                cellFilter = ""
                                                mergeCandidates = emptyList()
                                                refreshTrigger++
                                            }
                                        }
                                    }
                                },
                                enabled = !relocating,
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text(s.relocateConfirm, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        validationError?.let {
                            Text(it, color = CellRed, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun DestModeButton(
    label: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Accent.copy(alpha = 0.12f) else SurfaceContainer,
        border = if (isActive) BorderStroke(2.dp, Accent) else BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Accent else TextSecondary
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = if (isActive) Accent.copy(alpha = 0.7f) else TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
