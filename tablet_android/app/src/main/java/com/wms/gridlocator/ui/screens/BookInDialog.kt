package com.wms.gridlocator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    val itemTypes = listOf("Raw", "Finished-External", "Finished-Internal")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            // Top accent bar
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Header))

            Column(modifier = Modifier.padding(24.dp)) {
                Text(locationCode, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Book In — Location Booking", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = itemNumber,
                        onValueChange = { itemNumber = it },
                        label = { Text("Item Number") },
                        modifier = Modifier.weight(1f).height(64.dp),
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
                            label = { Text("Item Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().height(64.dp)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            itemTypes.forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { itemType = it; expanded = false })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Number") },
                        modifier = Modifier.weight(1f).height(64.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f).height(64.dp),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 3
                )
                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            val q = qty.toIntOrNull() ?: 0
                            if (itemNumber.isNotBlank() && batchNumber.isNotBlank() && q > 0) {
                                viewModel.bookIn(locationCode, itemNumber, itemType, batchNumber, q, description)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(2f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text("BOOK LOCATION", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
