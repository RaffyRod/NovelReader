package com.lagradost.quicknovel.ui.download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.ui.AppearanceMode
import com.lagradost.quicknovel.ui.UiIconTheme
import com.lagradost.quicknovel.ui.coreFor
import com.lagradost.quicknovel.ui.glowFor
import com.mihon.presentation.settings.collectAsState

/** Whether the section rail is open; the bottom menu toggles it and the library screen reads it. */
object LibraryRailState {
    var open by mutableStateOf(false)

    fun toggle() {
        open = !open
    }
}

/** A library section shown in the side rail: its name, icon and how many novels it holds. */
data class LibraryRailItem(val nameRes: Int, val iconRes: Int, val count: Int)

/** Icon of every library section, in the same order as the pager pages. */
val libraryRailIcons = listOf(
    R.drawable.ic_baseline_download_24,
    R.drawable.ic_baseline_menu_book_24,
    R.drawable.ic_baseline_pause_24,
    R.drawable.ic_baseline_bookmark_border_24,
    R.drawable.ic_baseline_check_24,
    R.drawable.ic_baseline_delete_outline_24,
    R.drawable.ic_baseline_favorite_24,
)

/** Soft fire halo for the selected section chip (surface-aware accents). */
private fun sectionFireBrush(theme: UiIconTheme, darkSurface: Boolean): Brush {
    val glow = theme.glowFor(darkSurface)
    val core = theme.coreFor(darkSurface)
    return object : ShaderBrush() {
        override fun createShader(size: androidx.compose.ui.geometry.Size) =
            RadialGradientShader(
                colors = listOf(
                    glow.copy(alpha = 0.85f),
                    core.copy(alpha = 0.55f),
                    core.copy(alpha = 0.92f),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.7f),
                radius = size.minDimension * 0.95f,
                tileMode = TileMode.Clamp,
            )
    }
}

/** A thin column of icons with counters that jumps between the library sections. */
@Composable
fun LibraryRail(
    items: List<LibraryRailItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onTranslationHistory: (() -> Unit)? = null,
) {
    val fire = UiIconTheme.fromKey(
        rememberAppSettings().ui.uiIconTheme.collectAsState().value
    )
    val darkSurface = AppearanceMode.isDark(LocalContext.current)
    val core = fire.coreFor(darkSurface)
    val coreLum = 0.2126f * core.red + 0.7152f * core.green + 0.0722f * core.blue
    val selectedFg = if (coreLum > 0.55f) Color(0xFF0A0C10) else Color.White
    val idleFg = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(52.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selected
            val label = stringResource(item.nameRes)
            val chipBg = Modifier
                .size(width = 42.dp, height = 48.dp)
                .clip(MaterialTheme.shapes.medium)
                .then(
                    if (isSelected) Modifier.background(sectionFireBrush(fire, darkSurface))
                    else Modifier.background(Color.Transparent)
                )
                .clickable { onSelect(index) }
            Box(
                modifier = chipBg,
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    tint = if (isSelected) selectedFg else idleFg,
                )
                Text(
                    text = item.count.toString(),
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp),
                    color = if (isSelected) selectedFg else idleFg,
                )
            }
        }
        if (onTranslationHistory != null) {
            val historyLabel = stringResource(R.string.translation_history_short)
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onTranslationHistory),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_language_24),
                    contentDescription = historyLabel,
                    modifier = Modifier.size(22.dp),
                    tint = idleFg,
                )
            }
        }
    }
}
