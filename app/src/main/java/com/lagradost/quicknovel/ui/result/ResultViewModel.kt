package com.lagradost.quicknovel.ui.result

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.lagradost.quicknovel.ChapterPage
import com.lagradost.quicknovel.ChapterPaging
import com.lagradost.quicknovel.RESULT_CHAPTERS_PER_PAGE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.quicknovel.APIRepository
import com.lagradost.quicknovel.BaseApplication.Companion.context
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.removeKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey
import com.lagradost.quicknovel.BookDownloader2
import com.lagradost.quicknovel.BookDownloader2.downloadProgress
import com.lagradost.cloudstream4.AppSettings
import com.lagradost.quicknovel.GeminiTranslator
import com.lagradost.quicknovel.TranslationException
import com.lagradost.quicknovel.BookDownloader2Helper
import com.lagradost.quicknovel.BookDownloader2Helper.clearChapterCache
import com.lagradost.quicknovel.BookDownloader2Helper.createQuickStream
import com.lagradost.quicknovel.BookDownloader2Helper.generateId
import com.lagradost.quicknovel.BookDownloader2Helper.getFilePath
import com.lagradost.quicknovel.BookDownloader2Helper.getStripAuthorNodes
import com.lagradost.quicknovel.ChapterSpec
import com.lagradost.quicknovel.LoadedChapter
import com.lagradost.quicknovel.BookDownloader2Helper.getQuickChapter
import com.lagradost.quicknovel.ChapterData
import com.lagradost.quicknovel.ChapterPdfExporter
import com.lagradost.quicknovel.CommonActivity.activity
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.DOWNLOAD_EPUB_LAST_ACCESS
import com.lagradost.quicknovel.DownloadActionType
import com.lagradost.quicknovel.DownloadProgressState
import com.lagradost.quicknovel.DownloadState
import com.lagradost.quicknovel.EPUB_CURRENT_POSITION
import com.lagradost.quicknovel.EPUB_CURRENT_POSITION_CHAPTER
import com.lagradost.quicknovel.EPUB_CURRENT_POSITION_READ_AT
import com.lagradost.quicknovel.EPUB_CURRENT_POSITION_SCROLL_CHAR
import com.lagradost.quicknovel.ErrorLoadingException
import com.lagradost.quicknovel.ExportedFile
import com.lagradost.quicknovel.ExportedFiles
import com.lagradost.quicknovel.FileHelper
import com.lagradost.quicknovel.HISTORY_FOLDER
import com.lagradost.quicknovel.LoadResponse
import com.lagradost.quicknovel.PdfChapter
import com.lagradost.quicknovel.PreferenceDelegate
import com.lagradost.quicknovel.QuickStreamData
import com.lagradost.quicknovel.QuickStreamMetaData
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.RESULT_BOOKMARK
import com.lagradost.quicknovel.RESULT_BOOKMARK_STATE
import com.lagradost.quicknovel.RESULT_FAVORITE
import com.lagradost.quicknovel.RESULT_CHAPTER_FILTER_BOOKMARKED
import com.lagradost.quicknovel.RESULT_CHAPTER_FILTER_DOWNLOADED
import com.lagradost.quicknovel.RESULT_CHAPTER_FILTER_READ
import com.lagradost.quicknovel.RESULT_CHAPTER_FILTER_UNREAD
import com.lagradost.quicknovel.RESULT_CHAPTER_SORT
import com.lagradost.quicknovel.StreamResponse
import com.lagradost.quicknovel.UserReview
import com.lagradost.quicknovel.WaitOrCancel
import com.lagradost.quicknovel.mvvm.Resource
import com.lagradost.quicknovel.mvvm.launchSafe
import com.lagradost.quicknovel.mvvm.logError
import com.lagradost.quicknovel.ui.ReadType
import com.lagradost.quicknovel.ui.common.ImmutableSearchResponse
import com.lagradost.quicknovel.ui.download.CHAPTER_SORT
import com.lagradost.quicknovel.ui.download.DownloadFragment
import com.lagradost.quicknovel.ui.download.LAST_ACCES_SORT
import com.lagradost.quicknovel.ui.download.LAST_UPDATED_SORT
import com.lagradost.quicknovel.ui.download.REVERSE_CHAPTER_SORT
import com.lagradost.quicknovel.ui.download.REVERSE_LAST_ACCES_SORT
import com.lagradost.quicknovel.ui.download.SortingMethod
import com.lagradost.quicknovel.util.Apis
import com.lagradost.quicknovel.util.Coroutines.ioSafe
import com.lagradost.quicknovel.util.ResultCached
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class ResultViewModel : ViewModel() {
    companion object {
        val chapterSortingMethods = arrayOf(
            SortingMethod(R.string.chapter_sort, CHAPTER_SORT, REVERSE_CHAPTER_SORT),
            SortingMethod(R.string.recently_sort, LAST_ACCES_SORT, REVERSE_LAST_ACCES_SORT),
        )
        var sortChapterBy by PreferenceDelegate(RESULT_CHAPTER_SORT, CHAPTER_SORT, Int::class)

        var filterChapterByDownloads by PreferenceDelegate(
            RESULT_CHAPTER_FILTER_DOWNLOADED,
            false,
            Boolean::class
        )
        var filterChapterByBookmarked by PreferenceDelegate(
            RESULT_CHAPTER_FILTER_BOOKMARKED,
            false,
            Boolean::class
        )
        var filterChapterByRead by PreferenceDelegate(
            RESULT_CHAPTER_FILTER_READ,
            true,
            Boolean::class
        )
        var filterChapterByUnread by PreferenceDelegate(
            RESULT_CHAPTER_FILTER_UNREAD,
            true,
            Boolean::class
        )
        var chaptersPerPage by PreferenceDelegate(
            RESULT_CHAPTERS_PER_PAGE,
            ChapterPaging.DEFAULT_SIZE,
            Int::class
        )

    }

    fun reorderChapters() {
        when (val response = this.loadResponse.value) {
            is Resource.Success -> {
                reorderChapters(response.value)
            }

            else -> {}
        }
    }

    fun reorderChapters(response: LoadResponse) {
        when (response) {
            is StreamResponse -> {
                chapters.postValue(orderChapters(response.data))
            }

            else -> chapters.postValue(null)
        }
    }


    private fun orderChapters(list: List<ChapterData>): List<ChapterData> {
        val filterRead = filterChapterByRead
        val filterUnread = filterChapterByUnread
        // val filterBookmarked = filterChapterByBookmarked
        val filterDownloaded = filterChapterByDownloads
        val sort = sortChapterBy
        val state = downloadState.value

        return list.filter { chapter ->
            val read = hasReadChapter(chapter)

            (filterUnread && !read) || (filterRead && read) ||
                    (filterDownloaded && (state != null && state.progress > (chapterIndex(chapter)
                        ?: Int.MAX_VALUE)))
        }.sortedBy { chapter ->
            return@sortedBy when (sort) {
                CHAPTER_SORT -> {
                    chapterIndex(chapter)?.toLong()
                }

                REVERSE_CHAPTER_SORT -> {
                    chapterIndex(chapter)?.toLong()?.unaryMinus()
                }

                LAST_ACCES_SORT -> {
                    getChapterReadTime(chapter) ?: Long.MAX_VALUE
                }

                REVERSE_LAST_ACCES_SORT -> {
                    -(getChapterReadTime(chapter) ?: Long.MAX_VALUE)
                }

                else -> null
            }
        }
    }

    fun clear() {
        loadResponse.postValue(null)
        chapters.postValue(null)
        pageIndex.postValue(0)
    }

    fun hasReadChapter(chapter: ChapterData): Boolean {
        return getChapterReadTime(chapter) != null
    }

    fun getChapterReadTime(chapter: ChapterData): Long? {
        val streamResponse =
            (load as? StreamResponse) ?: return null
        val index = chapterIndex(chapter) ?: return null
        return getKey<Long>(
            EPUB_CURRENT_POSITION_READ_AT,
            "${streamResponse.name}/$index"
        )
    }

    fun setReadChapter(chapter: ChapterData, value: Boolean): Boolean {
        val streamResponse =
            (load as? StreamResponse) ?: return false
        val index = chapterIndex(chapter) ?: return false

        if (value) {
            setKey(
                EPUB_CURRENT_POSITION_READ_AT,
                "${streamResponse.name}/$index",
                System.currentTimeMillis()
            )
        } else {
            removeKey(
                EPUB_CURRENT_POSITION_READ_AT,
                "${streamResponse.name}/$index",
            )
        }

        return true
    }

    var repo: APIRepository? = null

    var isGetLoaded = false

    var id: MutableLiveData<Int> = MutableLiveData<Int>(-1)
    var readState: MutableLiveData<ReadType> = MutableLiveData<ReadType>(ReadType.NONE)

    var apiName : String = ""

    /*This is to detect whether it actually returned to
     the fragment after reading a chapter, and thus update the list of chapters.*/
    var isResume = false

    val currentTabIndex: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>(0)
    }

    val currentTabPosition: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>(0)
    }

    private val loadMutex = Mutex()
    private lateinit var load: LoadResponse
    private var loadId: Int = 0
    private var loadUrl: String = ""
    private var hasLoaded: Boolean = false

    val loadResponse: MutableLiveData<Resource<LoadResponse>?> =
        MutableLiveData<Resource<LoadResponse>?>()

    val chapters: MutableLiveData<List<ChapterData>?> =
        MutableLiveData<List<ChapterData>?>()

    private val pageIndex = MutableLiveData(0)
    private val pageSize = MutableLiveData(ChapterPaging.normalizeSize(chaptersPerPage))

    /** The part of [chapters] the user is looking at, according to the page and the page size. */
    val chapterPage = MediatorLiveData<ChapterPage<ChapterData>?>().apply {
        fun update() {
            val list = chapters.value
            value = if (list == null) {
                null
            } else {
                ChapterPaging.pageOf(
                    list,
                    pageIndex.value ?: 0,
                    pageSize.value ?: ChapterPaging.DEFAULT_SIZE
                )
            }
        }
        addSource(chapters) { update() }
        addSource(pageIndex) { update() }
        addSource(pageSize) { update() }
    }

    fun goToChapterPage(page: Int) {
        pageIndex.value = page.coerceIn(0, (chapterPage.value?.pageCount ?: 1) - 1)
    }

    fun nextChapterPage() = goToChapterPage((chapterPage.value?.page ?: 0) + 1)

    fun previousChapterPage() = goToChapterPage((chapterPage.value?.page ?: 0) - 1)

    /** Saves the page size and keeps the chapter being looked at on screen. */
    fun setChaptersPerPage(size: Int) {
        val normalized = ChapterPaging.normalizeSize(size)
        val current = chapterPage.value
        chaptersPerPage = normalized
        pageIndex.value = if (current == null) 0 else ChapterPaging.pageAfterResize(current, normalized)
        pageSize.value = normalized
    }

    val reviews: MutableLiveData<Resource<ArrayList<UserReview>>> by lazy {
        MutableLiveData<Resource<ArrayList<UserReview>>>()
    }
    private var currentReviews: ArrayList<UserReview> = arrayListOf()

    private val reviewPage: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>(0)
    }

    private val loadMoreReviewsMutex = Mutex()
    private fun loadMoreReviews(url: String) {
        viewModelScope.launch {
            if (loadMoreReviewsMutex.isLocked) return@launch
            val api = repo ?: return@launch
            loadMoreReviewsMutex.withLock {
                val loadPage = (reviewPage.value ?: 0) + 1
                if (loadPage == 1) {
                    reviews.postValue(Resource.Loading())
                }
                when (val data = api.loadReviews(url, loadPage, load.reviewData)) {
                    is Resource.Success -> {
                        val moreReviews = data.value
                        currentReviews.addAll(moreReviews)

                        reviews.postValue(Resource.Success(currentReviews))
                        reviewPage.postValue(loadPage)
                    }

                    else -> {}
                }
            }
        }
    }

    fun openInBrowser() = viewModelScope.launchSafe {
        loadMutex.withLock {
            if (loadUrl.isBlank()) return@launchSafe
            val i = Intent(Intent.ACTION_VIEW)
            i.data = loadUrl.toUri()
            activity?.startActivity(i)
        }
    }

    fun switchTab(index: Int?, position: Int?) {
        val newPos = index ?: return
        currentTabPosition.postValue(position ?: return)
        currentTabIndex.postValue(newPos)
        if (newPos == 1 && currentReviews.isEmpty()) {
            loadMoreReviews(verify = false)
        }
        if (newPos == 3) {
            reorderChapters()
        } else {
            // clears the chapters to avoid flicker
            chapters.postValue(null)
        }
    }

    fun readEpub() = viewModelScope.launchSafe {
        loadMutex.withLock {
            if (!hasLoaded) return@launchSafe
            addToHistory()
            BookDownloader2.readEpub(
                loadId,
                downloadState.value?.progress?.toInt() ?: return@launchSafe,
                load.author,
                load.name,
                apiName,
                load.synopsis
            )
        }
    }

    private var cachedChapters: HashMap<ChapterData, Int> = hashMapOf()

    private fun chapterIndex(chapter: ChapterData): Int? {
        return cachedChapters[chapter]
    }

    private fun reCacheChapters() {
        val streamResponse = (load as? StreamResponse)
        if (streamResponse == null) {
            cachedChapters = hashMapOf()
            return
        }
        val out = hashMapOf<ChapterData, Int>()
        streamResponse.data.mapIndexed { index, chapterData ->
            out[chapterData] = index
        }
        cachedChapters = out
    }

    fun streamRead(chapter: ChapterData? = null) = ioSafe {
        loadMutex.withLock {
            if (!hasLoaded) return@ioSafe
            addToHistory()

            chapter?.let {
                // TODO BETTER STORE
                val streamResponse =
                    ((loadResponse.value as? Resource.Success)?.value as? StreamResponse)
                        ?: return@let
                val index = chapterIndex(chapter)
                if (index != null && index >= 0) {
                    setReadChapter(chapter, true)
                    setKey(EPUB_CURRENT_POSITION, streamResponse.name, index)
                    setKey(
                        EPUB_CURRENT_POSITION_CHAPTER,
                        streamResponse.name,
                        streamResponse.data[index].name
                    )
                    setKey(
                        EPUB_CURRENT_POSITION_SCROLL_CHAR, streamResponse.name, 0,
                    )
                    refreshLastChapter()
                }
            }

            BookDownloader2.stream(load, apiName)
        }
    }

    /** paused => resume,
     *  downloading => pause,
     *  done / pending => nothing,
     *  else => download
     * */
    fun downloadOrPause() = viewModelScope.launchSafe {
        loadMutex.withLock {
            if (!hasLoaded) return@launchSafe

            BookDownloader2.downloadInfoMutex.withLock {
                downloadProgress[loadId]?.let { downloadState ->
                    when (downloadState.state) {
                        DownloadState.IsPaused -> BookDownloader2.addPendingAction(
                            loadId,
                            DownloadActionType.Resume
                        )

                        DownloadState.IsDownloading -> BookDownloader2.addPendingAction(
                            loadId,
                            DownloadActionType.Pause
                        )

                        DownloadState.IsDone, DownloadState.IsPending -> {

                        }

                        else -> BookDownloader2.download(load, context ?: return@launchSafe)
                    }
                } ?: run {
                    BookDownloader2.download(load, context ?: return@launchSafe)
                }
            }
        }
    }

    fun pause() = viewModelScope.launchSafe {
        loadMutex.withLock {
            if (!hasLoaded) return@launchSafe

            BookDownloader2.downloadInfoMutex.withLock {
                downloadProgress[loadId]?.let { downloadState ->
                    when (downloadState.state) {
                        DownloadState.IsDownloading -> BookDownloader2.addPendingAction(
                            loadId,
                            DownloadActionType.Pause
                        )

                        else -> {

                        }
                    }
                }
            }
        }
    }

    fun stop() = viewModelScope.launchSafe {
        loadMutex.withLock {
            if (!hasLoaded) return@launchSafe

            BookDownloader2.downloadInfoMutex.withLock {
                downloadProgress[loadId]?.let { downloadState ->
                    when (downloadState.state) {
                        DownloadState.Nothing, DownloadState.IsDone, DownloadState.IsStopped, DownloadState.IsFailed -> {

                        }

                        else -> {
                            BookDownloader2.addPendingAction(
                                loadId,
                                DownloadActionType.Stop
                            )
                        }
                    }
                }
            }
        }
    }

    fun downloadFrom(start: Int?) = viewModelScope.launchSafe {
        loadMutex.withLock {
            if (!hasLoaded) return@launchSafe
            val api = repo ?: return@launchSafe
            BookDownloader2.downloadInfoMutex.withLock {
                BookDownloader2.changeDownloadStart(load, api, start)
                downloadProgress[loadId]?.let { downloadState ->
                    when (downloadState.state) {
                        DownloadState.IsPaused -> BookDownloader2.addPendingAction(
                            loadId,
                            DownloadActionType.Resume
                        )

                        DownloadState.IsPending -> {

                        }

                        // DownloadState.IsDone
                        else -> BookDownloader2.download(load, context ?: return@launchSafe)
                    }
                } ?: run {
                    BookDownloader2.download(load, context ?: return@launchSafe)
                }
            }
        }
    }

    fun download() = viewModelScope.launchSafe {
        loadMutex.withLock {
            if (!hasLoaded) return@launchSafe
            BookDownloader2.downloadInfoMutex.withLock {
                downloadProgress[loadId]?.let { downloadState ->
                    when (downloadState.state) {
                        DownloadState.IsPaused -> BookDownloader2.addPendingAction(
                            loadId,
                            DownloadActionType.Resume
                        )

                        DownloadState.IsDone, DownloadState.IsPending -> {

                        }

                        else -> BookDownloader2.download(load, context ?: return@launchSafe)
                    }
                } ?: run {
                    BookDownloader2.download(load, context ?: return@launchSafe)
                }
            }
        }
    }


    private fun addToHistory() = viewModelScope.launchSafe {
        // we wont add it to history from cache
        if (!isGetLoaded) return@launchSafe
        loadMutex.withLock {
            if (!hasLoaded) return@launchSafe
            setKey(
                HISTORY_FOLDER, loadId.toString(), ResultCached(
                    loadUrl,
                    load.name,
                    apiName,
                    loadId,
                    load.author,
                    load.posterUrl,
                    load.tags,
                    load.rating,
                    (load as? StreamResponse)?.data?.size ?: 1,
                    System.currentTimeMillis(),
                    synopsis = load.synopsis,
                    posterHeaders = load.posterHeaders
                )
            )
        }
    }

// requireContext().setKey(DOWNLOAD_TOTAL, localId.toString(), res.data .size)
// loadReviews()

    fun isInReviews(): Boolean {
        return currentTabIndex.value == 1
    }

    fun deleteAlert() = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded) return@launch
            val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        delete()
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }
            val act = activity ?: return@launch
            val builder: AlertDialog.Builder = AlertDialog.Builder(act)
            builder.setMessage(act.getString(R.string.permanently_delete_format).format(load.name))
                .setTitle(R.string.delete)
                .setPositiveButton(R.string.delete, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show()
        }
    }

    fun delete() = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded) return@launch
            BookDownloader2.deleteNovel(load.author, load.name, apiName)
        }
    }

    private fun updateBookmarkData() {
        // dont update data if preview because that data is from cache
        if (!isGetLoaded && getKey<ResultCached>(RESULT_BOOKMARK, loadId.toString()) != null) {
            return
        }
        val totalChapters = (load as? StreamResponse)?.data?.size ?: 1
        setKey(
            RESULT_BOOKMARK, loadId.toString(), ResultCached(
                loadUrl,
                load.name,
                apiName,
                loadId,
                load.author,
                load.posterUrl,
                load.tags,
                load.rating,
                totalChapters,
                System.currentTimeMillis(),
                synopsis = load.synopsis,
                posterHeaders = load.posterHeaders
            )
        )
    }

    fun bookmark(state: Int) = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded) return@launch
            setKey(
                RESULT_BOOKMARK_STATE, loadId.toString(), state
            )
            updateBookmarkData()
            BookDownloader2.bookmarkChanged(loadId)
        }

        readState.postValue(ReadType.fromSpinner(state))
    }

    val isFavorite: MutableLiveData<Boolean> = MutableLiveData(false)

    /** Index (0 based, in the full chapter list) of the chapter the user stopped at, if any. */
    val lastChapterIndex: MutableLiveData<Int?> = MutableLiveData(null)

    private fun refreshFavorite() {
        isFavorite.postValue(getKey<Boolean>(RESULT_FAVORITE, loadId.toString()) == true)
    }

    fun refreshLastChapter() {
        if (!::load.isInitialized) return
        val name = (load as? StreamResponse)?.name
        lastChapterIndex.postValue(name?.let { getKey<Int>(EPUB_CURRENT_POSITION, it) })
    }

    fun toggleFavorite() = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded) return@launch
            val key = loadId.toString()
            if (getKey<Boolean>(RESULT_FAVORITE, key) == true) {
                removeKey(RESULT_FAVORITE, key)
            } else {
                setKey(RESULT_FAVORITE, key, true)
                updateBookmarkData()
            }
            refreshFavorite()
            BookDownloader2.bookmarkChanged(loadId)
        }
    }

    /** The chapter the user stopped at, or null when none was chosen or read yet. */
    fun lastChapter(): ChapterData? {
        val index = lastChapterIndex.value ?: return null
        return (load as? StreamResponse)?.data?.getOrNull(index)
    }

    fun isLastChapter(chapter: ChapterData): Boolean {
        val index = lastChapterIndex.value ?: return false
        return chapterIndex(chapter) == index
    }

    /** Marks [chapter] as the place where the user stopped, without opening it. */
    fun markLastChapter(chapter: ChapterData) {
        val streamResponse = (load as? StreamResponse) ?: return
        val index = chapterIndex(chapter) ?: return
        setKey(EPUB_CURRENT_POSITION, streamResponse.name, index)
        setKey(EPUB_CURRENT_POSITION_CHAPTER, streamResponse.name, streamResponse.data[index].name)
        setKey(EPUB_CURRENT_POSITION_SCROLL_CHAR, streamResponse.name, 0)
        refreshLastChapter()
    }

    fun clearLastChapter() {
        val streamResponse = (load as? StreamResponse) ?: return
        removeKey(EPUB_CURRENT_POSITION, streamResponse.name)
        removeKey(EPUB_CURRENT_POSITION_CHAPTER, streamResponse.name)
        removeKey(EPUB_CURRENT_POSITION_SCROLL_CHAR, streamResponse.name)
        refreshLastChapter()
    }

    /** Moves the chapter list to the page that holds the chapter the user stopped at. */
    fun goToLastChapterPage() {
        val chapter = lastChapter() ?: return
        val list = chapters.value ?: return
        val position = list.indexOf(chapter) + 1
        val size = pageSize.value ?: ChapterPaging.DEFAULT_SIZE
        ChapterPaging.pageOfPosition(position, list.size, size)?.let { pageIndex.value = it }
    }

    fun share() = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded) return@launch

            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, load.name)
            i.putExtra(Intent.EXTRA_TEXT, loadUrl)
            activity?.startActivity(Intent.createChooser(i, load.name))
        }
    }

    fun loadMoreReviews(verify: Boolean = true) = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded) return@launch
            if (verify && currentTabIndex.value == 0) return@launch
            loadMoreReviews(loadUrl)
        }
    }

    init {
        // BookDownloader2.downloadDataChanged += ::progressDataChanged
        BookDownloader2.downloadProgressChanged += ::progressChanged
        BookDownloader2.downloadRemoved += ::downloadRemoved
    }

    override fun onCleared() {
        BookDownloader2.downloadProgressChanged -= ::progressChanged
        //BookDownloader2.downloadDataChanged -= ::progressDataChanged
        BookDownloader2.downloadRemoved -= ::downloadRemoved
    }

    val downloadState: MutableLiveData<DownloadProgressState> by lazy {
        MutableLiveData<DownloadProgressState>(null)
    }
    private var downloadStateValue: DownloadProgressState? = null

    fun setDownloadState(state: DownloadProgressState) {
        downloadStateValue = state
        downloadState.postValue(state)
    }

    private fun progressChanged(data: Pair<Int, DownloadProgressState>) =
        viewModelScope.launch {
            val (id, state) = data
            loadMutex.withLock {
                if (!hasLoaded || id != loadId) return@launch
                setDownloadState(state)
            }
        }

    /*fun progressDataChanged(data: Pair<Int, DownloadFragment.DownloadData>) =
        viewModelScope.launch {
            val (id, downloadData) = data
            loadMutex.withLock {
                if (!hasLoaded || id != loadId) return@launch

            }
        }*/

    private fun downloadRemoved(id: Int) = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded || id != loadId) return@launch
            insertZeroData()
        }
    }

    private fun insertZeroData() = viewModelScope.launch {
        loadMutex.withLock {
            if (!hasLoaded) return@launch

            BookDownloader2.downloadInfoMutex.withLock {
                val current = downloadProgress[loadId]
                if (current != null) {
                    setDownloadState(current)
                } else {
                    BookDownloader2Helper.downloadInfo(
                        context,
                        load.author,
                        load.name,
                        load.apiName
                    )?.let { info ->
                        val new = DownloadProgressState(
                            state = DownloadState.Nothing,
                            progress = info.progress,
                            total = info.total,
                            downloaded = info.downloaded,
                            lastUpdatedMs = System.currentTimeMillis(),
                            etaMs = null
                        )
                        downloadProgress[loadId] = new
                        setDownloadState(new)
                    } ?: run {
                        val new = DownloadProgressState(
                            state = DownloadState.Nothing,
                            progress = 0,
                            total = (load as? StreamResponse)?.data?.size?.toLong() ?: 1,
                            downloaded = 0,
                            lastUpdatedMs = System.currentTimeMillis(),
                            etaMs = null
                        )
                        setDownloadState(new)
                    }
                }
            }
        }
    }

    fun initState(card: ResultCached) = viewModelScope.launch {
        isGetLoaded = false
        loadMutex.withLock {
            this@ResultViewModel.apiName = card.apiName
            repo = Apis.getApiFromNameOrNull(card.apiName)
            loadUrl = card.source

            val data = StreamResponse(
                url = card.source,
                name = card.name,
                data = listOf(),
                author = card.author,
                posterUrl = card.poster,
                rating = card.rating,
                synopsis = card.synopsis,
                tags = card.tags,
                apiName = card.apiName
            )

            load = data
            loadResponse.postValue(Resource.Success(data))
            setState(card.id)
        }
    }

    private fun setState(tid: Int) {
        loadId = tid

        readState.postValue(
            ReadType.fromSpinner(
                getKey(
                    RESULT_BOOKMARK_STATE, tid.toString()
                )
            )
        )

        setKey(
            DOWNLOAD_EPUB_LAST_ACCESS, tid.toString(), System.currentTimeMillis()
        )
        reCacheChapters()
        updateBookmarkData()
        refreshFavorite()
        refreshLastChapter()
        BookDownloader2.openChanged(tid)
        hasLoaded = true

        // insert a download progress if not found
        insertZeroData()
    }

    fun initState(card: DownloadFragment.DownloadDataLoaded) = viewModelScope.launch {
        isGetLoaded = false
        loadResponse.postValue(Resource.Loading(card.source))

        loadMutex.withLock {
            this@ResultViewModel.apiName = card.apiName
            repo = Apis.getApiFromName(card.apiName)
            loadUrl = card.source

            val data = StreamResponse(
                url = card.source,
                name = card.name,
                data = listOf(),
                author = card.author,
                posterUrl = card.posterUrl,
                rating = card.rating,
                synopsis = card.synopsis,
                tags = card.tags,
                apiName = card.apiName
            )
            load = data
            loadResponse.postValue(Resource.Success(data))
            setState(card.id)
        }
    }
    fun initState(card: ImmutableSearchResponse) = viewModelScope.launch {
        val id = card.id ?: throw ErrorLoadingException("Require Id")
        isGetLoaded = false
        loadResponse.postValue(Resource.Loading(card.url))

        loadMutex.withLock {
            this@ResultViewModel.apiName = card.apiName
            repo = Apis.getApiFromName(card.apiName)
            loadUrl = card.url

            val data = StreamResponse(
                url = card.url,
                name = card.name,
                data = listOf(),
                author = card.author,
                posterUrl = card.posterUrl,
                posterHeaders = card.posterHeaders,
                rating = card.rating,
                synopsis = card.synopsis,
                tags = card.tags,
                apiName = card.apiName
            )
            load = data
            loadResponse.postValue(Resource.Success(data))
            setState(id)
        }
    }

    fun initState(apiName: String, url: String) = viewModelScope.launch {
        isGetLoaded = true
        loadResponse.postValue(Resource.Loading(url))

        loadMutex.withLock {
            this@ResultViewModel.apiName = apiName
            repo = Apis.getApiFromNameOrNull(apiName)
            loadUrl = url
        }

        val data = repo?.load(url)
        loadMutex.withLock {
            when (data) {
                is Resource.Success -> {
                    val res = data.value

                    load = res
                    loadUrl = res.url

                    val tid = generateId(res, apiName)
                    setState(tid)
                }

                else -> {}
            }
            loadResponse.postValue(data)
        }
    }

    val selectionMode: MutableLiveData<Boolean> = MutableLiveData(false)
    val selectedChapters: MutableLiveData<Set<Int>> = MutableLiveData(emptySet())
    val selectionTask: MutableLiveData<SelectionTaskState?> = MutableLiveData(null)

    private var selectionJob: Job? = null
    private var selectionWorker = WaitOrCancel()

    val totalChapters: Int
        get() = if (hasLoaded) (load as? StreamResponse)?.data?.size ?: 0 else 0

    /** Enters or leaves the multi selection mode, leaving it also clears the selection. */
    fun setSelectionMode(enabled: Boolean) {
        selectionMode.value = enabled
        if (!enabled) {
            selectedChapters.value = emptySet()
        }
    }

    fun isChapterSelected(chapter: ChapterData): Boolean {
        val index = chapterIndex(chapter) ?: return false
        return selectedChapters.value?.contains(index) == true
    }

    fun toggleChapterSelected(chapter: ChapterData) {
        val index = chapterIndex(chapter) ?: return
        val current = selectedChapters.value ?: emptySet()
        selectedChapters.value = if (current.contains(index)) current - index else current + index
    }

    /** Selects every chapter that is currently visible after filtering. */
    fun selectAllChapters() {
        val visible = chapters.value ?: return
        selectedChapters.value = visible.mapNotNull { chapter -> chapterIndex(chapter) }.toSet()
    }

    fun clearChapterSelection() {
        selectedChapters.value = emptySet()
    }

    /** Adds the chapters between [from] and [to] (1 based, inclusive) to the selection. */
    fun selectChapterRange(from: Int, to: Int) {
        val total = totalChapters
        if (total <= 0) return
        val start = minOf(from, to).coerceIn(1, total) - 1
        val end = maxOf(from, to).coerceIn(1, total) - 1
        selectedChapters.value = (selectedChapters.value ?: emptySet()) + (start..end)
    }

    private fun selectedChapterList(): List<Pair<Int, ChapterData>> {
        val stream = (load as? StreamResponse)?.takeIf { hasLoaded } ?: return emptyList()
        return (selectedChapters.value ?: emptySet()).sorted().mapNotNull { index ->
            stream.data.getOrNull(index)?.let { chapter -> index to chapter }
        }
    }

    /** Opens the reader containing only the selected chapters, in book order. */
    fun streamReadSelected() = ioSafe {
        val stream = (load as? StreamResponse)?.takeIf { hasLoaded } ?: return@ioSafe
        val selected = selectedChapterList()
        if (selected.isEmpty()) {
            showToast(R.string.no_chapters_selected)
            return@ioSafe
        }
        val appContext = context ?: return@ioSafe
        val selectionName = "${stream.name} [${appContext.getString(R.string.select_chapters)}]"
        val meta = QuickStreamMetaData(stream.author, selectionName, apiName)

        appContext.clearChapterCache(meta)
        setKey(EPUB_CURRENT_POSITION, selectionName, 0)
        setKey(EPUB_CURRENT_POSITION_CHAPTER, selectionName, selected.first().second.name)
        setKey(EPUB_CURRENT_POSITION_SCROLL_CHAR, selectionName, 0)

        val uri = activity?.createQuickStream(
            QuickStreamData(
                meta,
                stream.posterUrl,
                selected.map { (_, chapter) -> chapter }.toMutableList()
            )
        )
        BookDownloader2.openQuickStream(uri)
    }

    /** Chapter indexes (book order) whose text is already stored on the device. */
    private fun downloadedChapterFiles(stream: StreamResponse, appContext: Context): List<Pair<Int, java.io.File>> {
        val meta = QuickStreamMetaData(stream.author, stream.name, apiName)
        val directory = java.io.File(appContext.getFilePath(meta, 0)).parentFile
        return directory?.listFiles()
            ?.mapNotNull { file ->
                val index = file.nameWithoutExtension.toIntOrNull() ?: return@mapNotNull null
                if (file.isFile && file.length() > 0 && index in stream.data.indices) index to file else null
            }
            ?.sortedBy { it.first }
            ?: emptyList()
    }

    /**
     * Opens the reader with only the chapters already downloaded. Their text is copied to a
     * separate offline book, so reading never needs the network.
     */
    fun readOffline() = ioSafe {
        val stream = (load as? StreamResponse)?.takeIf { hasLoaded } ?: return@ioSafe
        val appContext = context ?: return@ioSafe
        val downloaded = downloadedChapterFiles(stream, appContext)
        if (downloaded.isEmpty()) {
            showToast(R.string.read_offline_empty)
            return@ioSafe
        }

        val offlineName = "${stream.name} [offline]"
        val meta = QuickStreamMetaData(stream.author, offlineName, apiName)
        appContext.clearChapterCache(meta)
        downloaded.forEachIndexed { offlineIndex, (_, file) ->
            val target = java.io.File(appContext.getFilePath(meta, offlineIndex))
            target.parentFile?.mkdirs()
            file.copyTo(target, overwrite = true)
        }

        if (getKey<Int>(EPUB_CURRENT_POSITION, offlineName) == null) {
            setKey(EPUB_CURRENT_POSITION, offlineName, 0)
            setKey(EPUB_CURRENT_POSITION_CHAPTER, offlineName, stream.data[downloaded.first().first].name)
            setKey(EPUB_CURRENT_POSITION_SCROLL_CHAR, offlineName, 0)
        }
        addToHistory()

        val uri = activity?.createQuickStream(
            QuickStreamData(
                meta,
                stream.posterUrl,
                downloaded.map { (index, _) -> stream.data[index] }.toMutableList()
            )
        )
        BookDownloader2.openQuickStream(uri)
    }

    /** EPUB and PDF files saved for this novel that can still be opened. */
    fun savedFiles(): List<ExportedFile> {
        val appContext = context ?: return emptyList()
        if (!::load.isInitialized) return emptyList()
        return ExportedFiles.available(appContext, load.name)
    }

    /** Current selection as a compact text such as "1-10, 15", used to prefill the download dialog. */
    fun selectionAsText(): String =
        ChapterSpec.format((selectedChapters.value ?: emptySet()).map { it + 1 })

    /**
     * Starts the download of the chapter [numbers] (1 based) in the given [kind].
     * For [SelectionTaskKind.Epub] and [SelectionTaskKind.Pdf] the chapters are split into
     * files of [perFile] chapters, or a single file when [perFile] is not positive.
     */
    fun startDownload(
        kind: SelectionTaskKind,
        numbers: Set<Int>,
        perFile: Int,
        translate: Boolean = false
    ) {
        selectedChapters.value = numbers.map { it - 1 }.toSet()
        startSelectionTask(kind, perFile, translate)
    }

    /** Cancels the running selection task and closes its progress. */
    fun cancelSelectionTask() {
        selectionWorker.notifyEvent()
        selectionJob?.cancel()
        selectionTask.value = null
    }

    fun dismissSelectionTask() {
        selectionTask.value = null
    }

    private fun startSelectionTask(kind: SelectionTaskKind, perFile: Int, translate: Boolean = false) {
        if (selectionJob?.isActive == true) return
        val stream = (load as? StreamResponse)?.takeIf { hasLoaded } ?: return
        val selected = selectedChapterList()
        if (selected.isEmpty()) {
            showToast(R.string.no_chapters_selected)
            return
        }
        val api = repo ?: return
        val appContext = context ?: return
        val meta = QuickStreamMetaData(stream.author, stream.name, apiName)
        val worker = WaitOrCancel()
        selectionWorker = worker

        selectionTask.value = SelectionTaskState(kind = kind, total = selected.size)
        val errorHandler = CoroutineExceptionHandler { _, throwable ->
            logError(throwable)
            selectionTask.postValue(
                SelectionTaskState(kind = kind, total = selected.size, finished = true, error = true)
            )
        }
        selectionJob = viewModelScope.launch(Dispatchers.IO + errorHandler) {
            when (kind) {
                SelectionTaskKind.Download ->
                    runDownloadTask(appContext, api, meta, selected, worker)

                SelectionTaskKind.Pdf, SelectionTaskKind.Epub ->
                    runExportTask(kind, appContext, stream, meta, selected, perFile, translate)
            }
        }
    }

    private suspend fun runDownloadTask(
        appContext: Context,
        api: APIRepository,
        meta: QuickStreamMetaData,
        selected: List<Pair<Int, ChapterData>>,
        worker: WaitOrCancel
    ) {
        var failed = 0
        selected.forEachIndexed { position, (index, chapter) ->
            currentCoroutineContext().ensureActive()
            val result = BookDownloader2Helper.downloadIndividualChapter(
                appContext.getFilePath(meta, index),
                api,
                chapter,
                worker = worker
            )
            if (result != BookDownloader2Helper.DownloadResult.Success) {
                failed++
            }
            selectionTask.postValue(
                SelectionTaskState(
                    kind = SelectionTaskKind.Download,
                    done = position + 1,
                    total = selected.size,
                    failed = failed
                )
            )
        }
        selectionTask.postValue(
            SelectionTaskState(
                kind = SelectionTaskKind.Download,
                done = selected.size,
                total = selected.size,
                failed = failed,
                finished = true
            )
        )
    }

    /**
     * Translates a chapter, keeping the result next to the downloaded chapter so a retry after
     * a failure or a cancel does not translate the same chapters again.
     */
    private suspend fun translateWithCache(
        translator: GeminiTranslator,
        appContext: Context,
        meta: QuickStreamMetaData,
        index: Int,
        chapter: LoadedChapter
    ): LoadedChapter {
        val cache = java.io.File(appContext.getFilePath(meta, index) + ".es")
        if (cache.isFile && cache.length() > 0) {
            val text = cache.readText()
            val newline = text.indexOf('\n')
            if (newline > 0) {
                return LoadedChapter(text.substring(0, newline), text.substring(newline + 1))
            }
        }
        val translated = translator.translateChapter(chapter)
        cache.writeText("${translated.title.replace('\n', ' ')}\n${translated.html}")
        return translated
    }

    /**
     * Downloads the selected chapters and packs them into one or several PDF or EPUB files.
     * Each chapter keeps the number it has in the book, also in the file names.
     */
    private suspend fun runExportTask(
        kind: SelectionTaskKind,
        appContext: Context,
        stream: StreamResponse,
        meta: QuickStreamMetaData,
        selected: List<Pair<Int, ChapterData>>,
        perFile: Int,
        translate: Boolean
    ) {
        val stripAuthorNotes = appContext.getStripAuthorNodes()
        val loadedChapters = LinkedHashMap<Int, LoadedChapter>()
        var failed = 0
        selected.forEachIndexed { position, (index, chapter) ->
            currentCoroutineContext().ensureActive()
            val loaded = appContext.getQuickChapter(meta, chapter, index, false, stripAuthorNotes)
            if (loaded == null) {
                failed++
            } else {
                loadedChapters[index + 1] =
                    loaded.copy(title = loaded.title.ifBlank { chapter.name })
            }
            selectionTask.postValue(
                SelectionTaskState(
                    kind = kind,
                    done = position + 1,
                    total = selected.size,
                    failed = failed
                )
            )
        }

        val failedState = SelectionTaskState(
            kind = kind,
            done = selected.size,
            total = selected.size,
            failed = failed,
            finished = true,
            error = true
        )
        if (loadedChapters.isEmpty()) {
            selectionTask.postValue(failedState)
            return
        }

        if (translate) {
            val translation = AppSettings(appContext).translation
            val translator = GeminiTranslator(
                translation.geminiApiKey.get().trim(),
                translation.geminiModel.get()
            )
            val translatingState =
                failedState.copy(finished = false, error = false, translating = true, done = 0)
            selectionTask.postValue(translatingState)
            try {
                loadedChapters.entries.forEachIndexed { position, entry ->
                    currentCoroutineContext().ensureActive()
                    val index = entry.key - 1
                    entry.setValue(translateWithCache(translator, appContext, meta, index, entry.value))
                    selectionTask.postValue(translatingState.copy(done = position + 1))
                }
            } catch (e: TranslationException) {
                selectionTask.postValue(failedState.copy(errorMessage = e.message))
                return
            }
        }

        selectionTask.postValue(
            failedState.copy(finished = false, error = false, translating = false, writing = true)
        )

        val storage = if (kind == SelectionTaskKind.Pdf) FileHelper.pdf else FileHelper.epub
        val extension = if (kind == SelectionTaskKind.Pdf) "pdf" else "epub"
        val names = ArrayList<String>()
        val uris = ArrayList<Uri>()

        for (group in ChapterSpec.group(loadedChapters.keys.toList(), perFile)) {
            currentCoroutineContext().ensureActive()
            val fileName = "${meta.name} - ${ChapterSpec.format(group, ",")}" +
                if (translate) " [ES]" else ""
            val file = storage.createFile(appContext, fileName, novelFolder = meta.name)
            val output = file?.openOutputStream(append = false)
            if (file == null || output == null) {
                selectionTask.postValue(failedState)
                return
            }

            withContext(Dispatchers.Default) {
                if (kind == SelectionTaskKind.Pdf) {
                    val pdfChapters = group.map { number ->
                        val chapter = loadedChapters.getValue(number)
                        PdfChapter(
                            title = ChapterSpec.numberedTitle(number, chapter.title),
                            paragraphs = ChapterPdfExporter.htmlToParagraphs(chapter.html)
                        )
                    }
                    output.use { target -> ChapterPdfExporter.write(pdfChapters, target) }
                } else {
                    BookDownloader2Helper.writeEpubOfChapters(
                        appContext,
                        apiName,
                        stream.name,
                        stream.author,
                        stream.synopsis,
                        group.map { number -> number to loadedChapters.getValue(number) },
                        output
                    )
                }
            }
            names.add("$fileName.$extension")
            uris.add(file.uri)
            ExportedFiles.record(meta.name, "$fileName.$extension", file.uri, storage.mimeType)
        }

        selectionTask.postValue(
            failedState.copy(
                error = false,
                writing = false,
                resultNames = names,
                resultUris = uris
            )
        )
    }
}

enum class SelectionTaskKind {
    Download,
    Epub,
    Pdf
}

/** Progress and result of a download or PDF export running over the selected chapters. */
data class SelectionTaskState(
    val kind: SelectionTaskKind,
    val done: Int = 0,
    val total: Int = 0,
    val failed: Int = 0,
    val writing: Boolean = false,
    val translating: Boolean = false,
    val finished: Boolean = false,
    val error: Boolean = false,
    val errorMessage: String? = null,
    val resultNames: List<String> = emptyList(),
    val resultUris: List<Uri> = emptyList(),
)