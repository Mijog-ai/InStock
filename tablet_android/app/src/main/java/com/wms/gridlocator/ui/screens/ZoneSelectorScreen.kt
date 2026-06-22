package com.wms.gridlocator.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wms.gridlocator.data.ConfigLoader
import com.wms.gridlocator.data.RecentRelocation
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel
import kotlinx.coroutines.launch

@Composable
fun ZoneSelectorScreen(viewModel: WmsViewModel) {
    val state by viewModel.state.collectAsState()
    val config = viewModel.config
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var recentRelocations by remember { mutableStateOf<List<RecentRelocation>>(emptyList()) }
    var loadingRecent by remember { mutableStateOf(true) }
    var revertTarget by remember { mutableStateOf<RecentRelocation?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        loadingRecent = true
        recentRelocations = viewModel.getRecentRelocations()
        loadingRecent = false
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

    Column(modifier = Modifier.fillMaxSize().background(Surface).padding(24.dp)) {
        Text(s.selectZone, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(s.tapZoneToView, fontSize = 14.sp, color = TextSecondary)
        Spacer(Modifier.height(16.dp))

        // Zone cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            config.zones.forEach { (code, zone) ->
                val total = ConfigLoader.getTotalCells(zone)
                val occupied = state.zoneOccupancy[code] ?: 0
                val pct = if (total > 0) (occupied * 100 / total) else 0
                val isSelected = state.selectedZone == code

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                        .clickable { viewModel.selectZone(code) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Accent.copy(alpha = 0.15f) else SurfaceWhite
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                    ) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                zone.displayName,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SurfaceContainer
                            ) {
                                Text(
                                    "${s.zone} $code",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$occupied / $total ${s.locations}", fontSize = 14.sp, color = TextSecondary)
                                Text("$pct%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth().height(12.dp),
                                color = when {
                                    pct > 80 -> CellRed
                                    pct > 50 -> Accent
                                    else -> CellGreen
                                },
                                trackColor = Border.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Recent Relocations
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(s.recentRelocations, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))

                if (loadingRecent) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (recentRelocations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.noRecentRelocations, color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(recentRelocations) { _, reloc ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Surface,
                                border = BorderStroke(1.dp, Border.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            reloc.itemNumber,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${reloc.fromLocation} → ${reloc.toLocation}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Accent
                                        )
                                        Text(
                                            "${s.qty}: ${reloc.qty} · ${reloc.movedBy} · ${
                                                if (reloc.movedAt.length >= 10) reloc.movedAt.substring(0, 10) else reloc.movedAt
                                            }",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { revertTarget = reloc },
                                        modifier = Modifier.height(40.dp),
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
