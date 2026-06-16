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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wms.gridlocator.i18n.Language
import com.wms.gridlocator.i18n.LocalAppStrings
import com.wms.gridlocator.ui.theme.*
import com.wms.gridlocator.viewmodel.WmsViewModel

@Composable
fun LoginScreen(viewModel: WmsViewModel) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp).background(Accent)
            )
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Language switcher at top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LoginLanguageSwitcher(
                        currentLanguage = state.language,
                        onLanguageChange = { viewModel.setLanguage(it) }
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Logo
                Box(
                    modifier = Modifier.size(64.dp).background(Header, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("W", color = SurfaceWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Text(s.appTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(s.warehousePortal, fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (state.loginError != null) viewModel.clearLoginError()
                    },
                    label = { Text(s.username) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (state.loginError != null) viewModel.clearLoginError()
                    },
                    label = { Text(s.password) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                state.loginError?.let {
                    Text(it, color = CellRed, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text(s.authenticateAccess, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LoginLanguageSwitcher(currentLanguage: Language, onLanguageChange: (Language) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Language.entries.forEach { lang ->
            val isActive = lang == currentLanguage
            Surface(
                modifier = Modifier.clickable { onLanguageChange(lang) },
                shape = RoundedCornerShape(6.dp),
                color = if (isActive) Accent.copy(alpha = 0.15f) else SurfaceContainer
            ) {
                Text(
                    lang.displayName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Accent else TextSecondary
                )
            }
        }
    }
}
