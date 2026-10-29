package com.lagradost.quicknovel.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.lagradost.quicknovel.ui.common.SkeletonCover
import com.lagradost.quicknovel.ui.common.SkeletonGrid
import com.lagradost.quicknovel.ui.common.staggeredEnter
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream4.compose.MultiSelectDialog
import com.lagradost.cloudstream4.compose.Screen
import com.lagradost.cloudstream4.compose.rounded
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.state.ObserveEffect
import com.lagradost.cloudstream4.theme.CloudStreamPreviewTheme
import com.lagradost.quicknovel.CommonActivity
import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.MainActivity.Companion.navigate
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.compose.BaseSearchBar
import com.lagradost.quicknovel.compose.isLandscape
import com.lagradost.quicknovel.ui.common.SearchResponseAction
import com.lagradost.quicknovel.ui.common.SearchResponseItem
import com.lagradost.quicknovel.ui.mainpage.MainPageFragment
import com.lagradost.quicknovel.ui.mainpage.SearchResponseDialog
import com.lagradost.quicknovel.ui.settings.searchLangList
import com.lagradost.quicknovel.util.ProviderLanguages
import com.lagradost.quicknovel.ui.settings.searchProvidersList
import com.lagradost.quicknovel.util.Apis.Companion.apis
import com.lagradost.quicknovel.util.SubtitleHelper
import com.mihon.common.preference.AndroidPreferenceStore
import com.mihon.presentation.settings.collectAsState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet

object SearchScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = viewModel<HomeViewModel2>()
        //val viewModel: HomeViewModel2 =
        //     viewModel(factory = HomeViewModel2.provideFactory(store.searchProvidersList()))
        val state by viewModel.state.collectAsStateWithLifecycle()

        val store = rememberAppSettings()
        val selectionState by store.provider.searchProvidersList.collectAsState()
        LaunchedEffect(selectionState) {
            viewModel.onAction(HomeAction.ConfigureApisNames(selectionState.toPersistentSet()))
        }

        val selectionLangState by store.provider.searchLangList.collectAsState()
        val showEnglish by store.provider.showEnglishProviders.collectAsState()
        LaunchedEffect(selectionLangState, showEnglish) {
            val languages = ProviderLanguages.effective(selectionLangState, showEnglish)
            viewModel.onAction(HomeAction.ConfigureApisLanguages(languages.toPersistentSet()))
        }

        ObserveEffect(viewModel.effect) { effect ->
            when (effect) {
                is HomeEffect.NavigateToMainPage -> {
                    CommonActivity.activity?.navigate(
                        R.id.global_to_navigation_mainpage,
                        MainPageFragment.newInstance(effect.api, effect.filter)
                    )
                }
            }
        }

        SearchScreen(state, viewModel::onAction)
    }
}

@Composable
fun SearchScreen(state: HomeViewModelState, action: (HomeAction) -> Unit) {
    ShowDialog(state.filterLanguages, state.isConfigureShow, action)
    BackHandler(state.isQueryOpen) {
        action(HomeAction.CloseQuery)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyGridState()


    val searchAction = remember<(SearchResponseAction) -> Unit>(action) {
        { item ->
            action(HomeAction.ResultAction(item))
        }
    }
    val dismissAction = remember(action) {
        {
            action(HomeAction.CloseRow)
        }
    }

    if (state.openRow != null) {
        SearchResponseDialog(state.openRow, action = searchAction, dismiss = dismissAction)
    }

    Scaffold(
        topBar = {
            BaseSearchBar(
                content = {
                    Spacer(modifier = Modifier.height(5.dp))
                },
                onQueryChange = { _ ->
                },
                onSearch = { query ->
                    action(HomeAction.Search(query))
                },
                trailingIcon = {
                    if (state.isLoading && state.isQueryOpen) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        IconButton(onClick = { action(HomeAction.ConfigureApis) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_tune_24),
                                contentDescription = stringResource(R.string.search_providers),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                leadingIcon = {
                    if (state.isQueryOpen) {
                        IconButton(onClick = {
                            action(HomeAction.CloseQuery)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                                contentDescription = stringResource(R.string.back_to_search),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = stringResource(R.string.search),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
            )
        }, modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            // This fixes the double padding from the bottom nav bar
            .padding(
                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                top = innerPadding.calculateTopPadding()
            )
        if (state.isQueryOpen) {
            SearchResults(state, action, searchAction, contentModifier)
        } else {
            val columns = if (isLandscape) 6 else 3
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = listState,
                modifier = contentModifier,
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.recentSearches.isNotEmpty()) {
                    item(key = "recent", span = { GridItemSpan(maxLineSpan) }) {
                        RecentSearchesSection(state.recentSearches, action)
                    }
                }
                if (state.popularLoading || state.popular.isNotEmpty()) {
                    item(key = "popular", span = { GridItemSpan(maxLineSpan) }) {
                        PopularSection(state, searchAction)
                    }
                }
                item(key = "providers_title", span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(stringResource(R.string.search_providers_title))
                }
                itemsIndexed(state.shownMainPageApis, key = { _, item -> item.name }) { index, api ->
                    MainAPIItem(
                        api = api,
                        action = action,
                        modifier = Modifier.staggeredEnter(index)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = modifier.padding(start = 6.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun RecentSearchesSection(recent: ImmutableList<String>, action: (HomeAction) -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionTitle(stringResource(R.string.search_recent))
            TextButton(onClick = { action(HomeAction.ClearRecent) }) {
                Text(stringResource(R.string.search_recent_clear))
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(recent, key = { it }) { query ->
                AssistChip(onClick = { action(HomeAction.Search(query)) }, label = { Text(query) })
            }
        }
    }
}

@Composable
private fun PopularSection(state: HomeViewModelState, searchAction: (SearchResponseAction) -> Unit) {
    Column {
        SectionTitle(stringResource(R.string.search_popular))
        if (state.popular.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                repeat(3) { SkeletonCover(Modifier.width(104.dp)) }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                items(state.popular, key = { it.randomUuid }) { response ->
                    SearchResponseItem(
                        response = response,
                        action = searchAction,
                        modifier = Modifier.width(104.dp)
                    )
                }
            }
        }
    }
}

/** Every result in one grid, with chips to keep only the ones of a single provider. */
@Composable
private fun SearchResults(
    state: HomeViewModelState,
    action: (HomeAction) -> Unit,
    searchAction: (SearchResponseAction) -> Unit,
    modifier: Modifier,
) {
    val withResults = state.searchRows.filter { it.items.isNotEmpty() }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val shownRows = if (selected == null) withResults else withResults.filter { it.name == selected }
    val items = shownRows.flatMap { it.items }
    val columns = if (isLandscape) 6 else 3

    Column(modifier) {
        if (withResults.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = selected == null,
                        onClick = { selected = null },
                        label = { Text("${stringResource(R.string.search_filter_all)} (${withResults.sumOf { it.items.size }})") }
                    )
                }
                items(withResults, key = { it.name }) { row ->
                    FilterChip(
                        selected = selected == row.name,
                        onClick = { selected = row.name },
                        label = { Text("${row.name} (${row.items.size})") }
                    )
                }
            }
        }
        when {
            items.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(items, key = { _, item -> item.randomUuid }) { index, response ->
                    SearchResponseItem(
                        response = response,
                        action = searchAction,
                        modifier = Modifier
                            .animateItem()
                            .staggeredEnter(index)
                    )
                }
            }

            state.isLoading -> SkeletonGrid(Modifier.fillMaxSize())

            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun ShowDialog(
    filterLanguages: ImmutableSet<String>, isDialogShown: Boolean, action: (HomeAction) -> Unit
) {
    if (!isDialogShown) return

    val entries = remember(filterLanguages) {
        apis.filter { api -> filterLanguages.contains(api.lang) }.associate {
            it.name to
                    if (filterLanguages.size > 1) {
                        "${SubtitleHelper.getFlagFromIso(it.lang) ?: "🌐"} ${it.name}"
                    } else {
                        it.name
                    }
        }.toPersistentMap()
    }
    val context = LocalContext.current
    val store = AndroidPreferenceStore(context)
    val apisSettings = store.searchProvidersList()
    val keys = apisSettings.get().toPersistentSet()

    MultiSelectDialog(
        title = stringResource(R.string.search_providers),
        entries = entries,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
        confirmText = stringResource(R.string.ok),
        confirm = { selection: Set<String> ->
            apisSettings.set(selection)
            action(HomeAction.DismissConfigureApis)
        },
        dismissText = stringResource(R.string.cancel),
        dismiss = {
            action(HomeAction.DismissConfigureApis)
        },
        selectedKeys = keys,
    )
}

@Composable
fun MainAPIItem(
    api: MainAPI,
    modifier: Modifier = Modifier,
    action: (HomeAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .rounded()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = {
                action(HomeAction.Open(api))
            })
    ) {
        Image(
            painter = painterResource(api.iconId ?: R.drawable.fiber_new_24px),
            contentDescription = api.name,
            modifier = Modifier
                .padding(10.dp)
                .size(40.dp)
                .rounded()
                .background(colorResource(api.iconBackgroundId))
        )
        Text(
            text = api.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    CloudStreamPreviewTheme {
        SearchScreen(state = HomeViewModelState(), action = {})
    }
}

@Composable
private fun SearchRow(
    row: SearchRow,
    action: (HomeAction) -> Unit
) {
    val searchAction = remember<(SearchResponseAction) -> Unit>(action) {
        { item ->
            action(HomeAction.ResultAction(item))
        }
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable(onClick = {
                    action(HomeAction.OpenRow(row))
                })
        ) {
            Text(
                row.name,
                modifier = Modifier.padding(10.dp),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp
            )
            Icon(
                modifier = Modifier.padding(10.dp),
                painter = painterResource(R.drawable.ic_baseline_arrow_forward_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(items = row.items, key = { item -> item.randomUuid }) { response ->
                SearchResponseItem(
                    response = response,
                    action = searchAction,
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}
