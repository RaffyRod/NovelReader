package com.mihon.presentation.settings.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.lagradost.cloudstream4.theme.CloudStreamPreviewTheme
import com.lagradost.quicknovel.R

@Composable
fun SwitchPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: Painter? = null,
    checked: Boolean = false,
    onCheckedChanged: (Boolean) -> Unit,
) {
    TextPreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = icon,
        widget = {
            // Non-null onCheckedChange + explicit semantics so UiAutomator can read ON/OFF.
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChanged(it) },
                modifier = Modifier
                    .padding(start = TrailingWidgetBuffer)
                    .semantics {
                        contentDescription = if (checked) "switch_on" else "switch_off"
                        stateDescription = if (checked) "ON" else "OFF"
                        toggleableState =
                            if (checked) ToggleableState.On else ToggleableState.Off
                    },
            )
        },
        onPreferenceClick = { onCheckedChanged(!checked) },
    )
}

@PreviewLightDark
@Composable
private fun SwitchPreferenceWidgetPreview() {
    CloudStreamPreviewTheme {
        Surface {
            Column {
                SwitchPreferenceWidget(
                    title = "Text preference with icon",
                    subtitle = "Text preference summary",
                    icon = painterResource(R.drawable.preview_24px) ,
                    checked = true,
                    onCheckedChanged = {},
                )
                SwitchPreferenceWidget(
                    title = "Text preference",
                    subtitle = "Text preference summary",
                    checked = false,
                    onCheckedChanged = {},
                )
                SwitchPreferenceWidget(
                    title = "Text preference no summary",
                    checked = false,
                    onCheckedChanged = {},
                )
                SwitchPreferenceWidget(
                    title = "Another text preference no summary",
                    checked = false,
                    onCheckedChanged = {},
                )
            }
        }
    }
}
