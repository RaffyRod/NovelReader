package com.lagradost.quicknovel

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.databinding.ActivityPdfReaderBinding
import com.lagradost.quicknovel.util.UIHelper.fixSystemBarsPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Reads a PDF inside the app, page after page, and remembers the page where the reader stopped. */
class PdfReaderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfReaderBinding
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var currentPage = 0
    private val lastPageKey get() = intent.data.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfReaderBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        fixSystemBarsPadding(binding.pdfRoot)

        val uri = intent.data
        val opened = uri?.let { runCatching { contentResolver.openFileDescriptor(it, "r") }.getOrNull() }
        val pdf = opened?.let { runCatching { PdfRenderer(it) }.getOrNull() }
        if (opened == null || pdf == null) {
            opened?.close()
            showToast(R.string.pdf_open_failed)
            finish()
            return
        }
        descriptor = opened
        renderer = pdf

        val pageCount = renderer?.pageCount ?: 0
        val adapter = PdfPagesAdapter(
            renderer ?: return,
            resources.displayMetrics.widthPixels,
            lifecycleScope
        )
        val layoutManager = LinearLayoutManager(this)
        binding.pdfPages.layoutManager = layoutManager
        binding.pdfPages.adapter = adapter
        binding.pdfLoading.isVisible = false
        binding.pdfPageIndicator.isVisible = pageCount > 0

        val startPage = getKey<Int>(PDF_LAST_PAGE, lastPageKey)
            ?.takeIf { it in 0 until pageCount } ?: 0
        layoutManager.scrollToPosition(startPage)
        showPage(startPage, pageCount)

        binding.pdfPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val first = layoutManager.findFirstVisibleItemPosition()
                if (first >= 0) showPage(first, pageCount)
            }
        })
    }

    private fun showPage(index: Int, pageCount: Int) {
        currentPage = index
        binding.pdfPageIndicator.text = getString(R.string.pdf_page_of, index + 1, pageCount)
    }

    override fun onPause() {
        if (renderer != null) setKey(PDF_LAST_PAGE, lastPageKey, currentPage)
        super.onPause()
    }

    override fun onDestroy() {
        (binding.pdfPages.adapter as? PdfPagesAdapter)?.close()
        renderer?.close()
        descriptor?.close()
        super.onDestroy()
    }

    companion object {
        private const val PDF_LAST_PAGE = "pdf_last_page"
    }
}

private class PdfPagesAdapter(
    private val renderer: PdfRenderer,
    private val width: Int,
    private val scope: CoroutineScope,
) : RecyclerView.Adapter<PdfPagesAdapter.PageHolder>() {
    /** PdfRenderer can only have one page open at a time, so every render takes this lock. */
    private val renderLock = Mutex()
    private var closed = false

    class PageHolder(val image: ImageView) : RecyclerView.ViewHolder(image) {
        var job: Job? = null
    }

    override fun getItemCount() = renderer.pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val image = ImageView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (PAGE_GAP_DP * parent.resources.displayMetrics.density).toInt() }
            adjustViewBounds = true
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
        }
        return PageHolder(image)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.job?.cancel()
        holder.image.setImageDrawable(null)
        holder.image.minimumHeight = (width * A4_RATIO).toInt()
        holder.job = scope.launch {
            val bitmap = withContext(Dispatchers.Default) { render(position) } ?: return@launch
            holder.image.minimumHeight = 0
            holder.image.setImageBitmap(bitmap)
        }
    }

    override fun onViewRecycled(holder: PageHolder) {
        holder.job?.cancel()
        holder.image.setImageDrawable(null)
    }

    private suspend fun render(index: Int): Bitmap? = renderLock.withLock {
        if (closed) return null
        renderer.openPage(index).use { page ->
            val scale = width.toFloat() / page.width
            val bitmap = Bitmap.createBitmap(
                width,
                (page.height * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }
    }

    /** Waits for a page being drawn to finish, then refuses new renders. */
    fun close() = runBlocking { renderLock.withLock { closed = true } }

    private companion object {
        const val A4_RATIO = 1.414f
        const val PAGE_GAP_DP = 8
    }
}
