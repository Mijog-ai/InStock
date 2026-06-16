package com.wms.gridlocator.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel

@Composable
fun ConsumeStockScreen(viewModel: WmsViewModel) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current

    var itemNumber by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier.fillMaxSize().background(Surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(s.tabConsumeStock, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(s.fifoSubtitle, fontSize = 13.sp, color = TextSecondary)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = itemNumber,
                onValueChange = { itemNumber = it },
                label = { Text(s.partNumber) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it.filter { c -> c.isDigit() } },
                label = { Text(s.quantityToConsume) },
                modifier = Modifier.width(160.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                onClick = {
                    val q = qty.toIntOrNull() ?: 0
                    if (itemNumber.isNotBlank() && q > 0) {
                        viewModel.searchFifo(itemNumber.trim(), q)
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text(s.findStock, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            if (!state.fifoSearched) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.fifoEmptyTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(s.fifoEmptySubtitle, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else if (state.fifoResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.noStockFound, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CellRed)
                        Spacer(Modifier.height(4.dp))
                        Text("${s.noActiveBookingsFor} $itemNumber", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else {
                val totalAvailable = state.fifoResults.sumOf { it.qty }
                val requestedQty = qty.toIntOrNull() ?: 0
                val canFulfill = totalAvailable >= requestedQty

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${s.fifoSuggestion} — $itemNumber",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (canFulfill) CellGreen.copy(alpha = 0.15f) else CellRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (canFulfill) "$requestedQty / $totalAvailable ${s.available}"
                                else "${s.insufficientStock}: $totalAvailable ${s.onlyXAvailable} $requestedQty",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canFulfill) CellGreen else CellRed
                            )
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f))

                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("#", Modifier.width(32.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(s.location, Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(s.poNumber, Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(s.date, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(s.available, Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End)
                        Text(s.take, Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End)
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(state.fifoResults) { idx, booking ->
                            val planned = state.fifoPlan.find { it.bookingId == booking.id }
                            val isTake = planned != null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isTake) CellGreen.copy(alpha = 0.08f) else SurfaceWhite)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${idx + 1}",
                                    Modifier.width(32.dp),
                                    fontSize = 12.sp, color = TextSecondary
                                )
                                Text(
                                    booking.locationCode,
                                    Modifier.weight(1.5f),
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Accent
                                )
                                Text(
                                    booking.batchNumber,
                                    Modifier.weight(1.5f),
                                    fontSize = 12.sp, color = TextSecondary
                                )
                                Text(
                                    if (booking.bookedAt.length >= 10) booking.bookedAt.substring(0, 10) else booking.bookedAt,
                                    Modifier.weight(1f),
                                    fontSize = 12.sp, color = TextSecondary
                                )
                                Text(
                                    "${booking.qty}",
                                    Modifier.weight(0.8f),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                                    textAlign = TextAlign.End
                                )
                                if (isTake) {
                                    Surface(
                                        modifier = Modifier.weight(0.8f),
                                        shape = RoundedCornerShape(999.dp),
                                        color = CellGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "${planned!!.take}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CellGreen,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Text(
                                        "—",
                                        Modifier.weight(0.8f),
                                        fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.4f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                            if (idx < state.fifoResults.lastIndex) {
                                HorizontalDivider(color = Border.copy(alpha = 0.2f))
                            }
                        }
                    }

                    HorizontalDivider(color = Border)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canFulfill) {
                            Text(
                                "$requestedQty ${s.consumingFromLocations} ${state.fifoPlan.size}",
                                fontSize = 13.sp, color = TextSecondary
                            )
                        } else {
                            Text(
                                "${s.insufficientStock}: $totalAvailable ${s.onlyXAvailable} $requestedQty",
                                fontSize = 13.sp, color = CellRed
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                viewModel.clearFifo()
                                itemNumber = ""
                                qty = "1"
                            }) { Text(s.cancel) }
                            if (canFulfill) {
                                Button(
                                    onClick = { viewModel.consumeFifo() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                ) { Text(s.confirmConsumption, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}
