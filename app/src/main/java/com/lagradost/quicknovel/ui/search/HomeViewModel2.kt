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
import com.lagradost.quicknovel.util.Apis
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Immutable
data class HomeViewModelState(
    val allApis: ImmutableList<MainAPI> = Apis.apis.toImmutableList(),
    val mainPageApis: ImmutableList<MainAPI> = allApis.filter { api -> api.hasMainPage }
        .toImmutableList(),
    val shownMainPageApis: ImmutableList<MainAPI> = mainPageApis,
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
    object ConfigureApis : HomeAction()
    object DismissConfigureApis : HomeAction()
    data class Open(val item: MainAPI) : HomeAction()
    data class ConfigureApisNames(val names: ImmutableSet<String>) : HomeAction()
    data class ConfigureApisLanguages(val languages: ImmutableSet<String>) : HomeAction()
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

    fun filterApis(
        apis: ImmutableList<MainAPI>,
        names: ImmutableSet<String>,
        languages: ImmutableSet<String>
    ): ImmutableList<MainAPI> {
        if (names.isEmpty() || languages.isEmpty()) return persistentListOf()

        return apis.filter { api ->
            languages.contains(api.lang) && names.contains(
                api.name
            )
        }.toPersistentList()
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

            HomeAction.DismissConfigureApis -> {
                updateState {
                    copy(isConfigureShow = false)
                }
            }

            is HomeAction.ConfigureApisNames -> {
                updateState {
                    copy(
                        filterNames = action.names,
                        shownMainPageApis = filterApis(
                            mainPageApis,
                            action.names,
                            filterLanguages
                        )
                    )
                }
                loadPopular()
            }

            is HomeAction.ConfigureApisLanguages -> {
                updateState {
                    copy(
                        filterLanguages = action.languages,
                        shownMainPageApis = filterApis(
                            mainPageApis,
                            filterNames,
                            action.languages
                        )
                    )
                }
                loadPopular()
            }

            HomeAction.CloseQuery -> updateState {
                copy(isQueryOpen = false)
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
            val state = state.value
            val searchApis = filterApis(state.allApis, state.filterNames, state.filterLanguages)
            searchQuery(query, searchApis)
        }
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