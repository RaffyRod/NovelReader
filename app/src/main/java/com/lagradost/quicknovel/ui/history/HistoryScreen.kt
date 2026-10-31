package com.lagradost.quicknovel.ui.history


import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream4.compose.ActionDialog
import com.lagradost.cloudstream4.compose.Screen
import com.lagradost.cloudstream4.compose.SinglePairSelectDialog
import com.lagradost.cloudstream4.compose.rounded
import com.lagradost.cloudstream4.theme.CloudStreamPreviewTheme
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.compose.BaseSearchBar
import com.lagradost.quicknovel.compose.IsScrolling
import com.lagradost.quicknovel.compose.isLandscape
import com.lagradost.quicknovel.ui.AppearanceMode
import com.lagradost.quicknovel.ui.LocalUiIconTheme
import com.lagradost.quicknovel.ui.UiIconTheme
import com.lagradost.quicknovel.ui.coreFor
import com.lagradost.quicknovel.ui.glowFor
import com.lagradost.quicknovel.ui.common.ImmutableSearchList
import com.lagradost.quicknovel.ui.common.ImmutableSearchResponse
import com.lagradost.quicknovel.ui.common.SearchResponseAction
import com.lagradost.quicknovel.ui.common.SearchResponseItem
import com.lagradost.quicknovel.ui.common.SearchResponseOperation
import com.lagradost.quicknovel.ui.common.SearchResponseRow
import com.lagradost.quicknovel.ui.common.SkeletonList
import com.lagradost.quicknovel.ui.common.normalSortingMethods
import com.lagradost.quicknovel.ui.common.staggeredEnter
import com.lagradost.quicknovel.ui.history.HistoryAction.AskDeleteAll
import com.lagradost.quicknovel.ui.history.HistoryAction.DeleteAll
import com.lagradost.quicknovel.ui.history.HistoryAction.DismissDialog
import com.lagradost.quicknovel.ui.history.HistoryAction.Refresh
import com.lagradost.quicknovel.ui.history.HistoryAction.ResultAction
import com.lagradost.quicknovel.ui.history.HistoryAction.Search
import com.lagradost.quicknovel.ui.history.HistoryAction.SelectSortingMethod
import com.lagradost.quicknovel.ui.history.HistoryAction.ShowSorting
import com.lagradost.quicknovel.util.DateGroup
import com.lagradost.quicknovel.util.DateGroups
import com.mihon.common.preference.AndroidPreferenceStore
import com.mihon.presentation.settings.collectAsState

object HistoryScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = viewModel<HistoryViewModel2>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        HistoryScreen(state, viewModel::onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun HistoryScreen(
    state: HistoryState,
    action: (HistoryAction) -> Unit
) {
    LaunchedEffect(Unit) {
        action(Refresh)
    }

    val searchAction = remember<(SearchResponseAction) -> Unit> {
        { data ->
            action(ResultAction(data))
        }
    }

    val context = LocalContext.current
    val store = AndroidPreferenceStore(context)
    val historyIsRow = store.getBoolean(stringResource(R.string.history_list_view_key), true)
    val historyIsRowState by historyIsRow.collectAsState()

    var fabExpanded by remember { mutableStateOf(false) }
    val fire = LocalUiIconTheme.current
    val fireTint = if (fire == UiIconTheme.BLACK_FIRE) Color(0xFFF5F7FA) else fire.core
    HistoryDialog(state.dialog, action)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(snapAnimationSpec = null)
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    action(ShowSorting)
                },
                containerColor = fire.core.copy(alpha = 0.18f),
                contentColor = fireTint,
                // Elevation actually changes the color, because who wanted a sane framework
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                text = {
                    Text(stringResource(R.string.filter_dialog_sort_by))
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_sort_24dp),
                        contentDescription = stringResource(R.string.filter_dialog_sort_by),
                        tint = fireTint,
                    )
                },
                expanded = fabExpanded
            )
        },
        topBar = {
            BaseSearchBar(
                content = {
                    Spacer(modifier = Modifier.height(5.dp))
                },
                onQueryChange = { query ->
                    action(Search(query))
                },
                onSearch = { query ->
                    action(Search(query))
                },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { action(AskDeleteAll) }) {
                            Icon(
                                painter = painterResource(R.drawable.clear_all_24px),
                                contentDescription = stringResource(R.string.history_more_options),
                                tint = fireTint,
                            )
                        }
                        IconButton(onClick = {
                            historyIsRow.set(!historyIsRowState)
                        }) {
                            Icon(
                                painter = painterResource(if (historyIsRowState) R.drawable.ic_baseline_grid_view_24 else R.drawable.ic_baseline_list_24),
                                contentDescription = stringResource(if (historyIsRowState) R.string.grid_view else R.string.list_view),
                                modifier = Modifier.size(24.dp),
                                tint = fireTint,
                            )
                        }
                    }

                },
                scrollBehavior = scrollBehavior,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search_icon),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(24.dp),
                        tint = fireTint,
                    )
                },
            )
        }, modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        val lazyGridState: LazyGridState = rememberLazyGridState()
        lazyGridState.IsScrolling(up = {
            fabExpanded = true
        }, down = {
            fabExpanded = false
        })

        val listModifier = Modifier
            // This fixes the double padding from the bottom nav bar
            .padding(
                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                top = innerPadding.calculateTopPadding()
            )
        if (state.loading && state.history.sorted.isEmpty()) {
            SkeletonList(listModifier)
        } else if (!state.loading && state.history.sorted.isEmpty()) {
            Box(
                modifier = listModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
        } else {
            GroupedHistory(
                isRow = historyIsRowState,
                lazyGridState = lazyGridState,
                modifier = listModifier,
                state = state.history,
                searchAction = searchAction
            )
        }
    }
}

private fun DateGroup.title(): Int = when (this) {
    DateGroup.TODAY -> R.string.history_today
    DateGroup.YESTERDAY -> R.string.history_yesterday
    DateGroup.THIS_WEEK -> R.string.history_this_week
    DateGroup.EARLIER -> R.string.history_earlier
}

/** Reading = at least one chapter opened; Visited = opened the novel page only. */
private enum class HistoryKind { READING, VISITED }

private fun ImmutableSearchResponse.historyKind(): HistoryKind =
    if (chaptersRead > 0) HistoryKind.READING else HistoryKind.VISITED

private fun historyStamp(item: ImmutableSearchResponse): Long =
    maxOf(item.timeOfPageOpened ?: 0L, item.timeOfCached)

private fun fireAuraBrush(glow: Color, core: Color): Brush = object : ShaderBrush() {
    override fun createShader(size: androidx.compose.ui.geometry.Size) =
        RadialGradientShader(
            colors = listOf(
                glow.copy(alpha = 0.75f),
                core.copy(alpha = 0.35f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.5f, size.height * 0.7f),
            radius = size.minDimension * 0.85f,
            tileMode = TileMode.Clamp,
        )
}

private fun onAccentGlyph(core: Color): Color {
    val lum = 0.2126f * core.red + 0.7152f * core.green + 0.0722f * core.blue
    return if (lum > 0.55f) Color(0xFF0A0C10) else Color.White
}

@Composable
private fun FireSectionHeader(
    title: String,
    count: Int,
    iconRes: Int,
) {
    val fire = LocalUiIconTheme.current
    val darkSurface = AppearanceMode.isDark(LocalContext.current)
    val core = fire.coreFor(darkSurface)
    val glow = fire.glowFor(darkSurface)
    val glyph = onAccentGlyph(core)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp),
    ) {
        val badgeFill = Brush.linearGradient(listOf(glow, core))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(badgeFill)
                .background(fireAuraBrush(glow, core)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = glyph,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            color = glyph,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(core)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun HistoryStatusPill(kind: HistoryKind) {
    val fire = LocalUiIconTheme.current
    val darkSurface = AppearanceMode.isDark(LocalContext.current)
    val core = fire.coreFor(darkSurface)
    val label = stringResource(
        if (kind == HistoryKind.READING) R.string.history_reading_pill
        else R.string.history_visited_pill
    )
    val shape = RoundedCornerShape(999.dp)
    // Reading = solid accent; Visited = soft fill + strong border so it stays visible on light.
    val bg = if (kind == HistoryKind.READING) {
        core
    } else if (darkSurface) {
        core.copy(alpha = 0.28f)
    } else {
        core.copy(alpha = 0.14f)
    }
    val fg = if (kind == HistoryKind.READING) onAccentGlyph(core) else core
    Text(
        text = label,
        color = fg,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .then(
                if (kind == HistoryKind.VISITED) Modifier.border(1.dp, core, shape)
                else Modifier
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** History split into Reading vs Visited; date groups inside each; Fire-themed headers/pills. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupedHistory(
    isRow: Boolean,
    lazyGridState: LazyGridState,
    modifier: Modifier,
    state: ImmutableSearchList,
    searchAction: (SearchResponseAction) -> Unit,
) {
    val now = remember(state) { System.currentTimeMillis() }
    val buckets = remember(state, now) {
        val all = state.sorted.mapNotNull { id -> state.data[id] }
        HistoryKind.entries.associateWith { kind ->
            all.filter { it.historyKind() == kind }
                .groupBy { DateGroups.of(historyStamp(it), now) }
                .toSortedMap()
        }
    }
    val columns = if (isRow) (if (isLandscape) 2 else 1) else (if (isLandscape) 6 else 3)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = lazyGridState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(if (isRow) 5.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        var position = 0
        HistoryKind.entries.forEach { kind ->
            val dateMap = buckets[kind].orEmpty()
            val count = dateMap.values.sumOf { it.size }
            if (count == 0) return@forEach
            item(key = "kind_${kind.name}", span = { GridItemSpan(maxLineSpan) }) {
                FireSectionHeader(
                    title = stringResource(
                        if (kind == HistoryKind.READING) R.string.history_reading
                        else R.string.history_visited
                    ),
                    count = count,
                    iconRes = if (kind == HistoryKind.READING) {
                        R.drawable.ic_baseline_menu_book_24
                    } else {
                        R.drawable.ic_baseline_history_24
                    },
                )
            }
            dateMap.forEach { (group, items) ->
                item(key = "kind_${kind.name}_date_${group.name}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(group.title()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp)
                    )
                }
                items.forEach { response ->
                    val index = position++
                    item(key = "${kind.name}_${response.id ?: response.randomUuid}") {
                        if (isRow) {
                            val dismiss = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        searchAction(
                                            SearchResponseAction(
                                                response,
                                                SearchResponseOperation.AskDelete
                                            )
                                        )
                                    }
                                    false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismiss,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .rounded()
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(end = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.delete_24px),
                                            contentDescription = stringResource(R.string.remove_history),
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredEnter(index)
                            ) {
                                Box {
                                    SearchResponseRow(response = response, action = searchAction)
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        HistoryStatusPill(kind)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredEnter(index)
                            ) {
                                SearchResponseItem(
                                    response = response,
                                    action = searchAction,
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    HistoryStatusPill(kind)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun HistoryDialog(
    dialog: HistoryDialog?,
    action: (HistoryAction) -> Unit
) {
    when (dialog) {
        null -> {}
        is HistoryDialog.DeleteAll -> {
            ActionDialog(
                title = stringResource(R.string.remove_history),
                text = stringResource(R.string.remove_all_history),
                confirmText = stringResource(R.string.remove),
                dismissText = stringResource(R.string.cancel),
                dismiss = {
                    action(DismissDialog)
                },
                confirm = {
                    action(DeleteAll)
                }
            )
        }

        is HistoryDialog.DeleteItem -> {
            ActionDialog(
                title = stringResource(R.string.remove),
                text = stringResource(
                    R.string.remove_from_history_format,
                    dialog.about.name
                ),
                confirmText = stringResource(R.string.remove),
                dismissText = stringResource(R.string.cancel),
                dismiss = {
                    action(DismissDialog)
                },
                confirm = {
                    action(
                        ResultAction(
                            SearchResponseAction(
                                dialog.about,
                                SearchResponseOperation.Delete
                            )
                        )
                    )
                }
            )
        }

        is HistoryDialog.Sort -> {
            val data = normalSortingMethods
            SinglePairSelectDialog(
                entries = data.associate { (it.id to it.inverse) to stringResource(it.name) },
                selectedKey = dialog.method,
                title = stringResource(R.string.filter_dialog_sort_by),
                confirmText = stringResource(R.string.sort_apply),
                dismissText = stringResource(R.string.sort_cancel),
                dismiss = {
                    action(DismissDialog)
                },
                confirm = { key ->
                    action(SelectSortingMethod(key))
                    action(DismissDialog)
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    CloudStreamPreviewTheme {
        HistoryScreen(state = HistoryState(), action = {})
    }
}
