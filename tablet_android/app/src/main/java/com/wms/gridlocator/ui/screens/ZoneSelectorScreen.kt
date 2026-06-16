package com.wms.gridlocator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wms.gridlocator.data.ConfigLoader
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel

@Composable
fun ZoneSelectorScreen(viewModel: WmsViewModel) {
    val state by viewModel.state.collectAsState()
    val config = viewModel.config
    val s = LocalAppStrings.current

    Column(modifier = Modifier.fillMaxSize().background(Surface).padding(24.dp)) {
        Text(s.selectZone, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(s.tapZoneToView, fontSize = 14.sp, color = TextSecondary)
        Spacer(Modifier.height(24.dp))

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
                        .height(180.dp)
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
    }
}
