package com.wms.gridlocator.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel

private val MIN_CELL_SIZE = 72.dp
private val ROW_LABEL_WIDTH = 40.dp
private val GRID_PADDING = 12.dp

@Composable
fun ShelfGridScreen(viewModel: WmsViewModel) {
    val state by viewModel.state.collectAsState()
    val zoneCode = state.selectedZone ?: return
    val shelfCode = state.selectedShelf ?: return
    val config = viewModel.config
    val zone = config.zones[zoneCode] ?: return
    val shelf = zone.shelves[shelfCode] ?: return
    val occupiedCells = state.occupiedCells

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val availableWidth = screenWidth - ROW_LABEL_WIDTH - GRID_PADDING * 2 - 32.dp
    val cellSize = maxOf(MIN_CELL_SIZE, availableWidth / shelf.sections)

    Column(modifier = Modifier.fillMaxSize().background(Surface)) {
        // Shelf tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(zone.shelves.keys.sorted()) { sc ->
                val isActive = sc == shelfCode
                Surface(
                    modifier = Modifier.clickable { viewModel.selectShelf(sc) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) Accent else SurfaceContainer,
                    border = if (isActive) BorderStroke(2.dp, Accent) else BorderStroke(1.dp, Border)
                ) {
                    Text(
                        sc,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) SurfaceWhite else TextSecondary
                    )
                }
            }
        }

        // Legend
        val s = LocalAppStrings.current
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(CellGreen, s.empty)
            LegendItem(CellRed, s.occupied)
        }

        // Scrollable grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GRID_PADDING, vertical = 8.dp)
                .background(SurfaceWhite, RoundedCornerShape(16.dp))
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(GRID_PADDING)
        ) {
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()

            Column(modifier = Modifier.verticalScroll(verticalScroll)) {
                // Header row (section numbers)
                Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                    Spacer(Modifier.width(ROW_LABEL_WIDTH))
                    for (sec in 1..shelf.sections) {
                        Box(
                            modifier = Modifier.width(cellSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "%02d".format(sec),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Grid rows (top = highest row number)
                for (row in shelf.rows downTo 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(horizontalScroll),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Row label
                        Box(
                            modifier = Modifier
                                .width(ROW_LABEL_WIDTH)
                                .height(cellSize),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                "%02d".format(row),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        for (sec in 1..shelf.sections) {
                            val locCode = "$shelfCode-${"%02d".format(sec)}-${"%02d".format(row)}"
                            val isOccupied = locCode in occupiedCells
                            val isSelected = locCode == state.selectedCell

                            GridCell(
                                size = cellSize,
                                label = "${"%02d".format(sec)}-${"%02d".format(row)}",
                                isOccupied = isOccupied,
                                isSelected = isSelected,
                                onClick = { viewModel.selectCell(locCode) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    size: Dp,
    label: String,
    isOccupied: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(2.dp)) {
        Surface(
            modifier = Modifier
                .size(size - 4.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(10.dp),
            color = when {
                isSelected -> Accent.copy(alpha = 0.2f)
                isOccupied -> CellRed.copy(alpha = 0.15f)
                else -> CellGreen.copy(alpha = 0.12f)
            },
            border = BorderStroke(
                2.dp,
                when {
                    isSelected -> Accent
                    isOccupied -> CellRed
                    else -> CellGreen
                }
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isSelected -> Accent
                        isOccupied -> CellRed
                        else -> CellGreen.copy(alpha = 0.8f)
                    }
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(2.dp, color, RoundedCornerShape(4.dp))
        )
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
    }
}
