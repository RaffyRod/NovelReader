package com.lagradost.cloudstream4.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.theme.CloudStreamTheme
import com.lagradost.cloudstream4.theme.CloudStreamThemeMode
import com.lagradost.cloudstream4.theme.perfToColor
import com.lagradost.quicknovel.ui.IconStyle
import com.lagradost.quicknovel.ui.LocalIconStyle
import com.lagradost.quicknovel.ui.LocalUiIconTheme
import com.lagradost.quicknovel.ui.UiIconTheme
import com.mihon.presentation.LocalBackPress
import com.mihon.presentation.settings.collectAsState

/** Backwards compatible fragment for compose, before we switch entirely to compose navigation */
fun Screen.createComposeView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
): View = ComposeView(inflater.context).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        CloudStreamThemeAppSettings(this@createComposeView::Content)
    }
}

@Composable
fun CloudStreamThemeAppSettings(content: @Composable () -> Unit) {
    val settings = rememberAppSettings()
    val darkMode by settings.ui.darkMode.collectAsState()
    val primaryColor by settings.ui.primaryColor.collectAsState()
    val uiIconKey by settings.ui.uiIconTheme.collectAsState()
    val appIconKey by settings.ui.appIconTheme.collectAsState()
    val iconStyleKey by settings.ui.iconStyle.collectAsState()
    val fireTheme = UiIconTheme.fromKey(uiIconKey.ifBlank { appIconKey })
    val iconStyle = IconStyle.fromKey(iconStyleKey)
    val mode = if (darkMode) CloudStreamThemeMode.UltimateBlue else CloudStreamThemeMode.Light

    CloudStreamTheme(
        mode = mode,
        primaryColor = perfToColor(primaryColor, darkSurface = darkMode),
    ) {
        val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
            "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
        }.onBackPressedDispatcher

        CompositionLocalProvider(
            LocalBackPress provides backDispatcher::onBackPressed,
            LocalUiIconTheme provides fireTheme,
            LocalIconStyle provides iconStyle,
        ) {
            content()
        }
    }
}