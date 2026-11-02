package com.lagradost.quicknovel.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
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
import com.lagradost.quicknovel.ui.common.SkeletonCover
import com.lagradost.quicknovel.ui.common.SkeletonProviderRows
import com.lagradost.quicknovel.ui.common.staggeredEnter
import com.lagradost.quicknovel.ui.mainpage.MainPageFragment
import com.lagradost.quicknovel.ui.mainpage.SearchResponseDialog
import com.lagradost.quicknovel.ui.settings.searchLangList
import com.lagradost.quicknovel.ui.settings.searchProvidersList
import com.lagradost.quicknovel.util.Apis.Companion.apis
import com.lagradost.quicknovel.util.ProviderIcons
import com.lagradost.quicknovel.util.ProviderLanguages
import com.lagradost.quicknovel.util.SubtitleHelper
import com.mihon.common.preference.AndroidPreferenceStore
import com.mihon.presentation.settings.collectAsState
import kotlinx.collections.immutable.ImmutableList
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
            SearchResults(state, action, contentModifier)
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

/** Base 87.dp (+5% after adding card gaps). */
private val SearchCoverWidth = 91.dp

@Composable
private fun PopularSection(state: HomeViewModelState, searchAction: (SearchResponseAction) -> Unit) {
    Column {
        SectionTitle(stringResource(R.string.search_popular))
        if (state.popular.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                repeat(3) { SkeletonCover(Modifier.width(SearchCoverWidth)) }
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
                        modifier = Modifier.width(SearchCoverWidth)
                    )
                }
            }
        }
    }
}

/**
 * Results grouped by provider (horizontal rows). Tapping the provider name opens that
 * provider’s full result list ([HomeAction.OpenRow]).
 */
@Composable
private fun SearchResults(
    state: HomeViewModelState,
    action: (HomeAction) -> Unit,
    modifier: Modifier,
) {
    val withResults = state.searchRows.filter { it.items.isNotEmpty() }

    when {
        withResults.isNotEmpty() -> LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(withResults, key = { it.name }) { row ->
                SearchRow(row = row, action = action)
            }
            if (state.isLoading) {
                item(key = "loading_more") {
                    SkeletonProviderRows(
                        rows = 1,
                        coversPerRow = 4,
                        coverWidth = SearchCoverWidth,
                    )
                }
            }
        }

        state.isLoading -> SkeletonProviderRows(
            modifier = modifier,
            rows = 4,
            coversPerRow = 4,
            coverWidth = SearchCoverWidth,
        )

        else -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.search_no_results),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Box(
            modifier = Modifier
                .padding(10.dp)
                .size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProviderIconTile(api = api)
            if (api.showNewBadge) {
                Text(
                    text = stringResource(R.string.provider_new_badge),
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF1A6CFF), Color(0xFF4DA3FF)),
                            ),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = api.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Provider tile art: built-in [MainAPI.iconId], else remote URL / favicon, else initials placeholder.
 * Never uses the full-tile `fiber_new` glyph as a fake logo.
 */
@Composable
private fun ProviderIconTile(api: MainAPI) {
    val tileModifier = Modifier
        .fillMaxSize()
        .rounded()
        .background(colorResource(api.iconBackgroundId))
    val iconId = api.iconId
    if (iconId != null) {
        Image(
            painter = painterResource(iconId),
            contentDescription = api.name,
            modifier = tileModifier.padding(4.dp),
            contentScale = ContentScale.Fit,
        )
        return
    }
    val remote = remember(api.iconUrl, api.mainUrl) {
        ProviderIcons.resolvedIconUrl(api.iconUrl, api.mainUrl)
    }
    var showRemote by remember(remote) { mutableStateOf(!remote.isNullOrBlank()) }
    Box(modifier = tileModifier, contentAlignment = Alignment.Center) {
        ProviderIconPlaceholder(name = api.name)
        if (showRemote && !remote.isNullOrBlank()) {
            AsyncImage(
                model = remote,
                contentDescription = api.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .rounded(),
                onError = { showRemote = false },
                onSuccess = { showRemote = true },
            )
        }
    }
}

@Composable
private fun ProviderIconPlaceholder(name: String) {
    val initials = remember(name) { ProviderIcons.initials(name) }
    Text(
        text = initials,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(items = row.items, key = { item -> item.randomUuid }) { response ->
                SearchResponseItem(
                    response = response,
                    action = searchAction,
                    modifier = Modifier.width(SearchCoverWidth)
                )
            }
        }
    }
}
