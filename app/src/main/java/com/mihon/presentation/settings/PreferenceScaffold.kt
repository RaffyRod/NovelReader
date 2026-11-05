package com.mihon.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.mihon.material.AppBar
import com.mihon.material.Scaffold

@Composable
fun PreferenceScaffold(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    /** Sticky chrome under the app bar (e.g. Config / Admin tabs). */
    stickyHeader: @Composable () -> Unit = {},
    itemsProvider: @Composable () -> List<Preference>,
) {
    val (top, bottom) = remember { FocusRequester.createRefs() }

    Scaffold(
        topBar = {
            AppBar(
                modifier = Modifier.focusRequester(top).focusProperties {
                    down = bottom
                },
                title = title,
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = it,
            )
        },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                stickyHeader()
                PreferenceScreen(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(bottom)
                        .focusProperties { up = top },
                    items = itemsProvider(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                )
            }
        },
    )
}
