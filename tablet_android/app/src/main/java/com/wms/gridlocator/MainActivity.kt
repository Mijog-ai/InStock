package com.wms.gridlocator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wms.gridlocator.ui.screens.*
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WmsTheme {
                WmsApp()
            }
        }
    }
}

@Composable
fun WmsApp(viewModel: WmsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    if (!state.configLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Loading configuration...", fontSize = 16.sp, color = TextSecondary)
            }
        }
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    if (!state.loggedIn) {
        LoginScreen(viewModel)
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Header)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("WMS Grid Locator", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "${state.username} (${state.role})",
                        fontSize = 14.sp, color = SurfaceWhite.copy(alpha = 0.85f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SurfaceWhite)
                    ) { Text("Logout", fontSize = 12.sp) }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.selectedZone == null) {
                ZoneSelectorScreen(viewModel)
            } else {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { viewModel.goBackToZones() }) {
                        Text("< Back to Zones")
                    }
                    Text(
                        "Zone ${state.selectedZone} — ${viewModel.config.zones[state.selectedZone]?.displayName ?: ""}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                }
                ShelfGridScreen(viewModel)
            }
        }

        val selectedCell = state.selectedCell
        if (selectedCell != null) {
            val isOccupied = state.cellContents.isNotEmpty()
            if (isOccupied) {
                CellDetailsDialog(
                    locationCode = selectedCell,
                    bookings = state.cellContents,
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissCellDetails() }
                )
            } else {
                BookInDialog(
                    locationCode = selectedCell,
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissCellDetails() }
                )
            }
        }
    }
}
