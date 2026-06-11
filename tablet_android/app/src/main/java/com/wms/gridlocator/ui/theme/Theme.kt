package com.wms.gridlocator.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Header = Color(0xFF1E293B)
val Accent = Color(0xFFF97316)
val CellGreen = Color(0xFF22C55E)
val CellRed = Color(0xFFEF4444)
val Surface = Color(0xFFF8F9FF)
val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF0B1C30)
val TextSecondary = Color(0xFF45474C)
val Border = Color(0xFFC5C6CD)
val SurfaceContainer = Color(0xFFE5EEFF)

private val WmsColorScheme = lightColorScheme(
    primary = Header,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    background = Surface,
    surface = SurfaceWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border,
    error = CellRed,
    onError = Color.White,
)

@Composable
fun WmsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WmsColorScheme,
        content = content
    )
}
