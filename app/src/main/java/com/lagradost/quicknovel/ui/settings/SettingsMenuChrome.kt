package com.lagradost.quicknovel.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.quicknovel.R

/** Mock 9 sticky segmented control for Settings (Configuración / Administración). */
@Composable
fun SettingsMenuChrome(
    tab: SettingsMenuTab,
    onTabChange: (SettingsMenuTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .padding(4.dp)
            .semantics { contentDescription = "settings_menu_tabs" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsMenuTabChip(
            label = stringResource(R.string.settings_tab_configuration),
            description = "settings_tab_configuration",
            selected = tab == SettingsMenuTab.CONFIGURATION,
            onClick = { onTabChange(SettingsMenuTab.CONFIGURATION) },
            modifier = Modifier.weight(1f),
        )
        SettingsMenuTabChip(
            label = stringResource(R.string.settings_tab_administration),
            description = "settings_tab_administration",
            selected = tab == SettingsMenuTab.ADMINISTRATION,
            onClick = { onTabChange(SettingsMenuTab.ADMINISTRATION) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SettingsMenuTabChip(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(11.dp)
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Tab
                this.selected = selected
            }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
