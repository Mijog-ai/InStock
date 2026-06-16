package com.wms.gridlocator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wms.gridlocator.i18n.Language
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.i18n.LocalLanguage
import com.wms.gridlocator.i18n.Strings
import com.wms.gridlocator.ui.screens.*
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.AppTab
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
    val strings = Strings.get(state.language)

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLanguage provides state.language
    ) {
        if (!state.configLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(strings.loadingConfiguration, fontSize = 16.sp, color = TextSecondary)
                }
            }
            return@CompositionLocalProvider
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
            return@CompositionLocalProvider
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Header)
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.appTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LanguageSwitcher(
                                currentLanguage = state.language,
                                onLanguageChange = { viewModel.setLanguage(it) }
                            )
                            Text(
                                "${state.username} (${state.role})",
                                fontSize = 14.sp, color = SurfaceWhite.copy(alpha = 0.85f)
                            )
                            OutlinedButton(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SurfaceWhite)
                            ) { Text(strings.logout, fontSize = 12.sp) }
                        }
                    }

                    // Tab bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceWhite)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabButton(strings.tabGridZones, state.currentTab == AppTab.GRID_ZONES) {
                            viewModel.selectTab(AppTab.GRID_ZONES)
                        }
                        TabButton(strings.tabInputStock, state.currentTab == AppTab.INPUT_STOCK) {
                            viewModel.selectTab(AppTab.INPUT_STOCK)
                        }
                        TabButton(strings.tabConsumeStock, state.currentTab == AppTab.CONSUME_STOCK) {
                            viewModel.selectTab(AppTab.CONSUME_STOCK)
                        }
                    }
                    HorizontalDivider(color = Border.copy(alpha = 0.5f))
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (state.currentTab) {
                    AppTab.CONSUME_STOCK -> {
                        ConsumeStockScreen(viewModel)
                    }
                    else -> {
                        if (state.selectedZone == null) {
                            ZoneSelectorScreen(viewModel)
                        } else {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = { viewModel.goBackToZones() }) {
                                    Text(strings.backToZones)
                                }
                                Text(
                                    "${strings.zone} ${state.selectedZone} — ${viewModel.config.zones[state.selectedZone]?.displayName ?: ""}",
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                                )
                            }
                            ShelfGridScreen(viewModel)
                        }
                    }
                }
            }

            // Cell dialog logic — depends on current tab
            val selectedCell = state.selectedCell
            if (selectedCell != null) {
                val isOccupied = state.cellContents.isNotEmpty()
                val isReadOnly = state.currentTab == AppTab.GRID_ZONES

                if (isOccupied) {
                    CellDetailsDialog(
                        locationCode = selectedCell,
                        bookings = state.cellContents,
                        viewModel = viewModel,
                        onDismiss = { viewModel.dismissCellDetails() },
                        readOnly = isReadOnly
                    )
                } else if (!isReadOnly) {
                    BookInDialog(
                        locationCode = selectedCell,
                        viewModel = viewModel,
                        onDismiss = { viewModel.dismissCellDetails() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) Accent else SurfaceContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) SurfaceWhite else TextSecondary
        )
    }
}

@Composable
private fun LanguageSwitcher(currentLanguage: Language, onLanguageChange: (Language) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Language.entries.forEach { lang ->
            val isActive = lang == currentLanguage
            Surface(
                modifier = Modifier.clickable { onLanguageChange(lang) },
                shape = RoundedCornerShape(6.dp),
                color = if (isActive) SurfaceWhite.copy(alpha = 0.25f) else SurfaceWhite.copy(alpha = 0.05f)
            ) {
                Text(
                    lang.code.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = SurfaceWhite
                )
            }
        }
    }
}
