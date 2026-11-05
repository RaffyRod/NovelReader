package com.lagradost.quicknovel.ui.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream4.state.ActionHandler
import com.lagradost.cloudstream4.state.DefaultEffectContainer
import com.lagradost.cloudstream4.state.DefaultStateContainer
import com.lagradost.cloudstream4.state.EffectContainer
import com.lagradost.cloudstream4.state.SingleActiveQuery
import com.lagradost.cloudstream4.state.StateContainer
import com.lagradost.quicknovel.APIRepository
import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.ui.common.ImmutableSearchResponse
import com.lagradost.quicknovel.ui.common.SearchResponseAction
import com.lagradost.quicknovel.ui.mainpage.FilterQuery
import com.lagradost.quicknovel.ui.search.HomeEffect.NavigateToMainPage
import com.lagradost.quicknovel.CommonActivity
import com.lagradost.quicknovel.util.Apis
import com.lagradost.quicknovel.util.CustomProviders
import com.lagradost.quicknovel.util.ProviderLanguages
import com.lagradost.quicknovel.util.RecentSearches
import com.lagradost.quicknovel.util.cmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Immutable
data class HomeViewModelState(
    val allApis: ImmutableList<MainAPI> = Apis.apis.toImmutableList(),
    val mainPageApis: ImmutableList<MainAPI> = allApis.filter { api -> api.hasMainPage }
        .toImmutableList(),
    /** Enabled Fuentes ∩ hasMainPage (before the live grid text filter). */
    val enabledMainPageApis: ImmutableList<MainAPI> = mainPageApis,
    val shownMainPageApis: ImmutableList<MainAPI> = mainPageApis,
    /** Live text in the Buscar field used to shrink the provider tile grid. */
    val gridFilterQuery: String = "",
    val filterNames: ImmutableSet<String> = persistentSetOf(),
    val filterLanguages: ImmutableSet<String> = persistentSetOf(),
    val isConfigureShow: Boolean = false,
    val searchRows: PersistentList<SearchRow> = persistentListOf(),
    val isLoading: Boolean = false,
    val isQueryOpen: Boolean = false,
    val openRow: SearchRow? = null,
    val recentSearches: ImmutableList<String> = persistentListOf(),
    val popular: ImmutableList<ImmutableSearchResponse> = persistentListOf(),
    val popularLoading: Boolean = false,
)

@Immutable
data class SearchRow(
    val name: String,
    val items: ImmutableList<ImmutableSearchResponse> = persistentListOf(),
    val error: Throwable? = null,
)

@Immutable
sealed class HomeAction {
    data class Search(val query: String) : HomeAction()
    /** Live-filter provider tiles by name / site while typing (does not run novel search). */
    data class FilterGrid(val query: String) : HomeAction()
    object ConfigureApis : HomeAction()
    object DismissConfigureApis : HomeAction()
    data class Open(val item: MainAPI) : HomeAction()
    data class ConfigureApisNames(val names: ImmutableSet<String>) : HomeAction()
    data class ConfigureApisLanguages(val languages: ImmutableSet<String>) : HomeAction()
    /** Re-read Apis + prefs so newly installed Fuentes show on the Buscar grid. */
    object RefreshProviders : HomeAction()
    object CloseQuery : HomeAction()
    data class ResultAction(val action: SearchResponseAction) : HomeAction()
    data class OpenRow(val row: SearchRow) : HomeAction()
    object CloseRow : HomeAction()
    object ClearRecent : HomeAction()
}
@Immutable
sealed class HomeEffect {
    data class NavigateToMainPage(val api: String, val filter: FilterQuery) : HomeEffect()
}

private const val POPULAR_ATTEMPTS = 3
private const val POPULAR_COUNT = 12

class HomeViewModel2 : ViewModel(),
    StateContainer<HomeViewModelState> by DefaultStateContainer(HomeViewModelState()),
    ActionHandler<HomeAction>,
    EffectContainer<HomeEffect> by DefaultEffectContainer() {

    /*companion object {
        // Todo do this with injection of perf instead?
        fun provideFactory(selection: PreferenceData<Set<String>>) = viewModelFactory {
            initializer {
                val selection = selection.get()
                val state = HomeViewModelState()
                HomeViewModel2(
                    state.copy(shownMainPageApis = state.shownMainPageApis.filter { api ->
                        selection.contains(
                            api.name
                        )
                    }.toPersistentList())
                )
            }
        }
    }*/

    /**
     * Providers shown on the Buscar grid (and used when names are selected).
     * **Selected names win** — same rule as [apisForSearch] — so a provider toggled on in
     * Fuentes always appears even if its language is off in Settings (e.g. English plugins
     * with "show English" disabled). Language filter only applies when no names are selected.
     */
    fun filterApis(
        apis: ImmutableList<MainAPI>,
        names: ImmutableSet<String>,
        languages: ImmutableSet<String>
    ): ImmutableList<MainAPI> {
        if (names.isEmpty() && languages.isEmpty()) return persistentListOf()
        if (names.isNotEmpty()) {
            return apis.filter { names.contains(it.name) }.toPersistentList()
        }
        return apis.filter { languages.contains(it.lang) }.toPersistentList()
    }

    /**
     * Providers to hit for a novel search. Selected names win so English sources stay
     * searchable when they are enabled in prefs even if "show English" is off.
     */
    fun apisForSearch(
        apis: ImmutableList<MainAPI>,
        names: ImmutableSet<String>,
        languages: ImmutableSet<String>,
    ): ImmutableList<MainAPI> = when {
        names.isNotEmpty() -> apis.filter { names.contains(it.name) }.toPersistentList()
        languages.isNotEmpty() -> filterApis(
            apis,
            persistentSetOf(),
            languages,
        )
        else -> persistentListOf()
    }

    /**
     * Applies [SearchProviderScope] on top of [apisForSearch] so typing "Wattpad" /
     * "wattpad.com" (or a close prefix) only queries that site.
     */
    fun resolveNovelSearch(
        query: String,
        apis: ImmutableList<MainAPI>,
        names: ImmutableSet<String>,
        languages: ImmutableSet<String>,
    ): SearchProviderScope.Resolution {
        val base = apisForSearch(apis, names, languages)
        return SearchProviderScope.resolve(query, base)
    }

    override fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.ConfigureApis -> {
                updateState {
                    copy(isConfigureShow = true)
                }
            }

            is HomeAction.Open -> {
                viewModelScope.launch {
                    postEffect {
                        NavigateToMainPage(action.item.name, FilterQuery())
                    }
                }
            }

            is HomeAction.Search -> {
                search(action.query)
            }

            is HomeAction.FilterGrid -> {
                val q = action.query
                updateState {
                    val enabled = enabledMainPageApis.ifEmpty {
                        filterApis(mainPageApis, filterNames, filterLanguages)
                    }
                    copy(
                        gridFilterQuery = q,
                        shownMainPageApis = SearchProviderScope.filterGrid(enabled, q).toPersistentList(),
                    )
                }
            }

            HomeAction.DismissConfigureApis -> {
                updateState {
                    copy(isConfigureShow = false)
                }
            }

            is HomeAction.ConfigureApisNames -> {
                applyProviderFilters(names = action.names, languages = state.value.filterLanguages)
            }

            is HomeAction.ConfigureApisLanguages -> {
                applyProviderFilters(names = state.value.filterNames, languages = action.languages)
            }

            HomeAction.RefreshProviders -> {
                refreshProvidersFromPrefs()
            }

            HomeAction.CloseQuery -> updateState {
                copy(
                    isQueryOpen = false,
                    shownMainPageApis = SearchProviderScope
                        .filterGrid(enabledMainPageApis, gridFilterQuery)
                        .toPersistentList(),
                )
            }

            is HomeAction.ResultAction -> {
                resultAction(action.action)
            }

            HomeAction.ClearRecent -> {
                RecentSearches.save(emptyList())
                updateState { copy(recentSearches = persistentListOf()) }
            }

            HomeAction.CloseRow -> {
                updateState {
                    copy(openRow = null)
                }
            }

            is HomeAction.OpenRow -> {
                updateState {
                    copy(openRow = action.row)
                }
            }
        }
    }

    private fun resultAction(action: SearchResponseAction) {
        action.doAction()
    }

    init {
        updateState { copy(recentSearches = RecentSearches.load().toImmutableList()) }
    }

    private var popularRequested = false

    /** First page of the first provider that has one, shown on the search start page. */
    private fun loadPopular() {
        val apis = state.value.shownMainPageApis.sortedBy { it.lang != ProviderLanguages.DEFAULT }
        if (popularRequested || apis.isEmpty()) return
        popularRequested = true
        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(popularLoading = true) }
            for (api in apis.take(POPULAR_ATTEMPTS)) {
                val repo = APIRepository(api)
                val page = repo.loadMainPageResult(
                    page = 1,
                    mainCategory = api.mainCategories.getOrNull(0)?.second,
                    orderBy = api.orderBys.getOrNull(0)?.second,
                    tag = api.tags.getOrNull(0)?.second,
                ).getOrNull()
                if (page != null && page.list.isNotEmpty()) {
                    updateState { copy(popular = page.list.take(POPULAR_COUNT).toImmutableList()) }
                    break
                }
            }
            updateState { copy(popularLoading = false) }
        }
    }

    val searchQuery = SingleActiveQuery(Dispatchers.IO)

    private fun search(query: String) = viewModelScope.launch {
        val recent = RecentSearches.add(state.value.recentSearches, query)
        RecentSearches.save(recent)
        updateState { copy(recentSearches = recent.toImmutableList()) }
        searchQuery.launch {
            val snapshot = refreshProvidersFromPrefs()
            val scoped = resolveNovelSearch(
                query,
                snapshot.all,
                snapshot.names,
                snapshot.languages,
            )
            // Typing only the page/provider name → open that provider's main page.
            if (scoped.scopedToProvider && scoped.novelQuery.isBlank() && scoped.apis.size == 1) {
                val api = scoped.apis.first()
                updateState {
                    copy(
                        isQueryOpen = false,
                        isLoading = false,
                        gridFilterQuery = "",
                        shownMainPageApis = enabledMainPageApis,
                    )
                }
                if (api.hasMainPage) {
                    postEffect { NavigateToMainPage(api.name, FilterQuery()) }
                } else {
                    searchQuery("", listOf(api).toPersistentList())
                }
                return@launch
            }
            // Novel title (optionally "Wattpad romance") → search scoped providers.
            searchQuery(scoped.novelQuery.ifBlank { query }, scoped.apis.toPersistentList())
        }
    }

    private data class RefreshedApis(
        val all: ImmutableList<MainAPI>,
        val mainPage: ImmutableList<MainAPI>,
    )

    private data class ProviderFilterSnapshot(
        val all: ImmutableList<MainAPI>,
        val mainPage: ImmutableList<MainAPI>,
        val names: ImmutableSet<String>,
        val languages: ImmutableSet<String>,
    )

    private fun refreshedApis(): RefreshedApis {
        Apis.refreshApis()
        val all = Apis.apis.toImmutableList()
        return RefreshedApis(
            all = all,
            mainPage = all.filter { api -> api.hasMainPage }.toImmutableList(),
        )
    }

    private fun applyProviderFilters(
        names: ImmutableSet<String>,
        languages: ImmutableSet<String>,
    ) {
        val refreshed = refreshedApis()
        val customLangs = CustomProviders.load().map { it.lang }.filter { it.isNotBlank() }.toSet()
        val langs = (languages + customLangs).toPersistentSet()
        val enabled = filterApis(refreshed.mainPage, names, langs)
        val gridQ = state.value.gridFilterQuery
        updateState {
            copy(
                allApis = refreshed.all,
                mainPageApis = refreshed.mainPage,
                filterNames = names,
                filterLanguages = langs,
                enabledMainPageApis = enabled,
                shownMainPageApis = SearchProviderScope.filterGrid(enabled, gridQ).toPersistentList(),
            )
        }
        loadPopular()
    }

    /**
     * Syncs custom/plugin names into Search prefs, refreshes Apis, and updates the grid.
     * Call when Buscar resumes so installs done in Settings appear without a novel search.
     */
    private fun refreshProvidersFromPrefs(): ProviderFilterSnapshot {
        // Prefer live activity; fall back to app context so Buscar still syncs after recreate.
        val ctx = CommonActivity.activity
            ?: com.lagradost.quicknovel.BaseApplication.context
        val syncedNames = ctx?.let { CustomProviders.syncEnabledInSearch(it) }.orEmpty()
        val current = state.value
        val names = if (syncedNames.isNotEmpty()) {
            syncedNames.toPersistentSet()
        } else {
            current.filterNames
        }
        val customLangs = CustomProviders.load().map { it.lang }.filter { it.isNotBlank() }.toSet()
        val langs = (current.filterLanguages + customLangs).toPersistentSet()
        val refreshed = refreshedApis()
        val enabled = filterApis(refreshed.mainPage, names, langs)
        val gridQ = current.gridFilterQuery
        updateState {
            copy(
                allApis = refreshed.all,
                mainPageApis = refreshed.mainPage,
                filterNames = names,
                filterLanguages = langs,
                enabledMainPageApis = enabled,
                shownMainPageApis = SearchProviderScope.filterGrid(enabled, gridQ).toPersistentList(),
            )
        }
        loadPopular()
        return ProviderFilterSnapshot(refreshed.all, refreshed.mainPage, names, langs)
    }

    private suspend fun searchQuery(query: String, apis: ImmutableList<MainAPI>) {
        updateState {
            copy(
                isQueryOpen = true,
                isLoading = true,
                searchRows = persistentListOf()
            )
        }
        apis.cmap { api ->
            APIRepository(api).searchResult(query).onSuccess { value ->
                val row = SearchRow(name = api.name, items = value.toImmutableList())
                updateState {
                    if (value.isEmpty()) {
                        copy(searchRows = searchRows.adding(row))
                    } else {
                        val hasResults = searchRows.count { it.items.isNotEmpty() }
                        copy(searchRows = searchRows.addingAt(hasResults, row))
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                val row = SearchRow(name = api.name, error = error)
                updateState {
                    copy(searchRows = searchRows.adding(row))
                }
            }
        }
        updateState {
            copy(isLoading = false)
        }
    }
}