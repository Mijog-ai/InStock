package com.wms.gridlocator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.wms.gridlocator.data.Booking
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel

@Composable
fun CellDetailsDialog(
    locationCode: String,
    bookings: List<Booking>,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    readOnly: Boolean = false
) {
    val s = LocalAppStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(600.dp).heightIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Header))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().background(SurfaceContainer).padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "$locationCode — ${s.occupied}",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                        Text(
                            "${bookings.size} ${s.activeSlotsFormat}",
                            fontSize = 14.sp, color = TextSecondary
                        )
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = CellRed.copy(alpha = 0.15f)) {
                        Text(
                            s.occupiedStatus,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CellRed
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    bookings.forEach { booking ->
                        BatchCard(booking, viewModel, readOnly)
                    }

                    if (bookings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(s.cellNowEmpty, fontSize = 18.sp, color = TextSecondary)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text(s.close) }

                    if (!readOnly && bookings.size < viewModel.config.maxSlotsPerCell) {
                        Button(
                            onClick = {
                                viewModel.dismissCellDetails()
                            },
                            modifier = Modifier.weight(2f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Text(s.addBatch, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchCard(booking: Booking, viewModel: WmsViewModel, readOnly: Boolean = false) {
    val s = LocalAppStrings.current
    var showConsume by remember { mutableStateOf(false) }
    var consumeQty by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(s.itemNo, fontSize = 11.sp, color = TextSecondary)
                    Text(booking.itemNumber, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(s.batch, fontSize = 11.sp, color = TextSecondary)
                    Text(booking.batchNumber, fontSize = 13.sp, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainer, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(s.quantity, fontSize = 11.sp, color = TextSecondary)
                    Text("${booking.qty}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text(s.type, fontSize = 11.sp, color = TextSecondary)
                    Text(booking.itemType, fontSize = 14.sp, color = TextPrimary)
                }
                Column {
                    Text(s.bookedBy, fontSize = 11.sp, color = TextSecondary)
                    Text(booking.bookedBy, fontSize = 14.sp, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (!readOnly) {
                if (showConsume) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = consumeQty,
                            onValueChange = { consumeQty = it.filter { c -> c.isDigit() } },
                            label = { Text(s.qty) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Button(
                            onClick = {
                                val q = consumeQty.toIntOrNull() ?: 0
                                if (q in 1..booking.qty) {
                                    viewModel.consume(booking.id, q)
                                    showConsume = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Header),
                            modifier = Modifier.height(48.dp)
                        ) { Text(s.confirm) }
                        OutlinedButton(
                            onClick = { showConsume = false },
                            modifier = Modifier.height(48.dp)
                        ) { Text(s.cancel) }
                    }
                } else {
                    Button(
                        onClick = { showConsume = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Header)
                    ) { Text(s.consume) }
                }
            }
        }
    }
}
