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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wms.gridlocator.data.ErpDelivery
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInDialog(locationCode: String, viewModel: WmsViewModel, onDismiss: () -> Unit) {
    var itemNumber by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf("Raw") }
    var batchNumber by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDeliveryIdx by remember { mutableIntStateOf(-1) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var erpSearch by remember { mutableStateOf("") }
    var deliveries by remember { mutableStateOf<List<ErpDelivery>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        deliveries = viewModel.getErpDeliveries()
        loading = false
    }

    val itemTypes = listOf("Raw", "Finished-External", "Finished-Internal")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Header))

                Column(modifier = Modifier.padding(16.dp)) {
                    // Title row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(locationCode, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Book In — Location Booking", fontSize = 12.sp, color = TextSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            validationError?.let {
                                Text(it, color = CellRed, fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.height(40.dp)
                            ) { Text("Cancel", fontSize = 13.sp) }
                            Button(
                                onClick = {
                                    val q = qty.toIntOrNull() ?: 0
                                    when {
                                        itemNumber.isBlank() -> validationError = "Item number required"
                                        batchNumber.isBlank() -> validationError = "PO# required"
                                        q <= 0 -> validationError = "Quantity must be > 0"
                                        else -> {
                                            validationError = null
                                            viewModel.bookIn(locationCode, itemNumber, itemType, batchNumber, q, description)
                                            onDismiss()
                                        }
                                    }
                                },
                                modifier = Modifier.height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) { Text("BOOK LOCATION", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Horizontal layout: ERP table left, form fields right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left: ERP Delivery table
                        Column(modifier = Modifier.weight(1.2f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select ERP Delivery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                Spacer(Modifier.weight(1f))
                                OutlinedTextField(
                                    value = erpSearch,
                                    onValueChange = { erpSearch = it },
                                    placeholder = { Text("Search item / PO#...", fontSize = 12.sp) },
                                    modifier = Modifier.width(220.dp),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))

                            val query = erpSearch.trim().lowercase()
                            val filteredDeliveries = remember(deliveries, query) {
                                if (query.isEmpty()) deliveries
                                else deliveries.filter {
                                    it.itemNumber.lowercase().contains(query) ||
                                    it.purchaseNumber.lowercase().contains(query) ||
                                    it.supplierName.lowercase().contains(query)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Border),
                                color = Surface
                            ) {
                                if (loading) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                } else if (filteredDeliveries.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (query.isNotEmpty()) "No matches for \"$erpSearch\"" else "No recent deliveries",
                                            color = TextSecondary, fontSize = 13.sp
                                        )
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
                                                Text("Item", Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                Text("PO#", Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                Text("Qty", Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                Text("Supplier", Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                Text("Date", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                            }
                                        }
                                        itemsIndexed(filteredDeliveries) { idx, d ->
                                            val origIdx = deliveries.indexOf(d)
                                            val isSelected = origIdx == selectedDeliveryIdx
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedDeliveryIdx = origIdx
                                                        itemNumber = d.itemNumber
                                                        batchNumber = d.purchaseNumber
                                                        qty = d.ordered.toInt().toString()
                                                        description = "${d.supplierName} | PO: ${d.purchaseNumber} | ExtRef: ${d.externalDeliveryNote}"
                                                        itemType = "Raw"
                                                    }
                                                    .background(if (isSelected) Accent.copy(alpha = 0.12f) else SurfaceWhite)
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(d.itemNumber, Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(d.purchaseNumber, Modifier.weight(1.5f), fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(d.ordered.toInt().toString(), Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(d.supplierName, Modifier.weight(2f), fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    if (d.deliveryDate.length >= 10) d.deliveryDate.substring(0, 10) else d.deliveryDate,
                                                    Modifier.weight(1.2f), fontSize = 11.sp, color = TextSecondary
                                                )
                                            }
                                            if (idx < filteredDeliveries.lastIndex) {
                                                HorizontalDivider(color = Border.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right: Form fields — compact 2-column grid
                        Column(
                            modifier = Modifier.weight(0.8f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Form Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = itemNumber,
                                    onValueChange = { itemNumber = it },
                                    label = { Text("Item Number") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = itemType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Type") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        itemTypes.forEach {
                                            DropdownMenuItem(text = { Text(it) }, onClick = { itemType = it; expanded = false })
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = batchNumber,
                                    onValueChange = { batchNumber = it },
                                    label = { Text("PO#") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = qty,
                                    onValueChange = { qty = it.filter { c -> c.isDigit() } },
                                    label = { Text("Qty") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
