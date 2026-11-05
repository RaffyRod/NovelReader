package com.lagradost.quicknovel.ui.result

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import androidx.core.view.doOnLayout
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.text.InputType
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.lagradost.quicknovel.ChapterData
import com.lagradost.quicknovel.ChapterPage
import com.lagradost.quicknovel.ChapterPaging
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.lagradost.cloudstream4.AppSettings
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.tabs.TabLayout
import com.lagradost.quicknovel.BookDownloader2
import com.lagradost.quicknovel.ChapterSpec
import com.lagradost.quicknovel.CommonActivity
import com.lagradost.quicknovel.DownloadState
import com.lagradost.quicknovel.DownloadTranslateEngine
import com.lagradost.quicknovel.DownloadTranslateLangs
import com.lagradost.quicknovel.DownloadTranslateRequest
import com.lagradost.quicknovel.FileHelper
import com.lagradost.quicknovel.FileOpener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lagradost.quicknovel.LoadResponse
import com.lagradost.quicknovel.MainActivity.Companion.navigate
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.StreamResponse
import com.lagradost.quicknovel.databinding.ChapterDialogBinding
import com.lagradost.quicknovel.databinding.ChapterDownloadDialogBinding
import com.lagradost.quicknovel.databinding.ChapterFilterPopupBinding
import com.lagradost.quicknovel.databinding.ChapterRangeDialogBinding
import com.lagradost.quicknovel.databinding.ChapterTaskDialogBinding
import com.lagradost.quicknovel.databinding.FragmentResultBinding
import com.lagradost.quicknovel.mvvm.Resource
import com.lagradost.quicknovel.mvvm.debugException
import com.lagradost.quicknovel.mvvm.observe
import com.lagradost.quicknovel.mvvm.observeNullable
import com.lagradost.quicknovel.ui.BaseFragment
import com.lagradost.quicknovel.ui.ReadType
import com.lagradost.quicknovel.ui.SortingMethodAdapter
import com.lagradost.quicknovel.ui.mainpage.MainAdapter
import com.lagradost.quicknovel.ui.mainpage.MainPageFragment
import com.lagradost.quicknovel.ui.setRecycledViewPool
import com.lagradost.quicknovel.util.SettingsHelper.getRating
import com.lagradost.quicknovel.util.SingleSelectionHelper.showBottomDialog
import com.lagradost.quicknovel.util.UIHelper
import com.lagradost.quicknovel.util.UIHelper.colorFromAttribute
import com.lagradost.quicknovel.util.UIHelper.fixPaddingStatusbar
import com.lagradost.quicknovel.util.UIHelper.fixSystemBarsPadding
import com.lagradost.quicknovel.util.UIHelper.getStatusBarHeight
import com.lagradost.quicknovel.util.UIHelper.hideKeyboard
import com.lagradost.quicknovel.util.UIHelper.html
import com.lagradost.quicknovel.util.UIHelper.humanReadableByteCountSI
import com.lagradost.quicknovel.util.UIHelper.popupMenu
import com.lagradost.quicknovel.util.UIHelper.setImage
import com.lagradost.quicknovel.util.toPx

const val MAX_SYNO_LENGH = 300

class ResultFragment : BaseFragment<FragmentResultBinding>(
    BindingCreator.Inflate(FragmentResultBinding::inflate)
) {
    private val viewModel: ResultViewModel by viewModels()

    companion object {
        fun newInstance(url: String, apiName: String, startAction: Int = 0): Bundle =
            Bundle().apply {
                //println(data)
                putString("url", url)
                putString("apiName", apiName)
                putInt("startAction", startAction)
            }

    }

    override fun fixLayout(view: View) {
        val compactView = false //activity?.getGridIsCompact() ?: return
        val spanCountLandscape = if (compactView) 2 else 6
        val spanCountPortrait = if (compactView) 1 else 3
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding?.relatedList?.spanCount = spanCountLandscape
        } else {
            binding?.relatedList?.spanCount = spanCountPortrait
        }
        binding?.resultHolder?.post { // BUG FIX
            updateScrollHeight()
        }
        // The bottom controls must stay above the system navigation bar on every phone.
        binding?.let {
            it.stickyBar.keepAboveNavigationBar()
            it.chapterSelectionBar.root.keepAboveNavigationBar()
            it.chaptersFab.keepAboveNavigationBar(asMargin = true)
            it.reviewsFab.keepAboveNavigationBar(asMargin = true)
            it.chaptersSelectFabHost.keepAboveNavigationBar(asMargin = true)
        }

        binding?.resultDownloadCard?.let {
            fixSystemBarsPadding(
                it,
                padTop = false,
                overlayCutout = false
            )
        }
    }

    /**
     * Adds the height of the system navigation bar to the bottom padding (or margin) this view
     * already has, so buttons are not hidden under it on phones with edge-to-edge screens.
     */
    private fun View.keepAboveNavigationBar(asMargin: Boolean = false) {
        val basePadding = paddingBottom
        val baseMargin = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val bottom = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            if (asMargin) {
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = baseMargin + bottom }
            } else {
                view.updatePadding(bottom = basePadding + bottom)
            }
            windowInsets
        }
        ViewCompat.requestApplyInsets(this)
    }

    //private lateinit var viewModel: ResultViewModel

    val repo get() = viewModel.repo

    private var taskDialog: AlertDialog? = null
    private var taskBinding: ChapterTaskDialogBinding? = null
    /** When true, progress updates stay in the notification; the modal dialog stays closed. */
    private var taskDialogMinimized = false
    private val chapterFabBreathAnimators = mutableListOf<Animator>()
    private val detailBreathAnimators = mutableListOf<Animator>()

    private val selectionBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.setSelectionMode(false)
        }
    }

    /**
     * Detail chrome: actions C · info 2 · tabs 5 chip soft · Leer libro L4 soft tint ·
     * sticky bar E gemelos (Descargar soft · Leer primary). Theme `colorPrimary`.
     *
     * Note: “Leer libro” opens the local EPUB; sticky “Leer” streams online — different actions.
     */
    private fun applyResultDetailStyles() {
        val binding = binding ?: return
        stopDetailBreath()
        val ctx = binding.root.context
        val primary = ctx.colorFromAttribute(R.attr.colorPrimary)
        val onPrimary = contrastingOnColor(primary)
        val surface = ctx.colorFromAttribute(R.attr.iconGrayBackground)
        val muted = ctx.colorFromAttribute(R.attr.grayTextColor)
        val density = resources.displayMetrics.density
        val softFill = ColorUtils.setAlphaComponent(primary, 46)

        val discs = listOf(
            binding.resultBookmark to binding.resultBookmarkGlow,
            binding.resultFavorite to binding.resultFavoriteGlow,
            binding.resultOpeninbrower to binding.resultOpeninbrowerGlow,
            binding.resultShare to binding.resultShareGlow,
        )
        discs.forEach { (btn, glow) ->
            applyActionGhostCircle(btn, glow, primary, surface, density)
        }

        // Tabs 5 — soft primary chip + primary label
        binding.resultTabs.setSelectedTabIndicatorColor(softFill)
        binding.resultTabs.setTabTextColors(muted, primary)

        // L4 Soft tint — local EPUB button
        binding.resultDownloadGenerateEpub.apply {
            backgroundTintList = ColorStateList.valueOf(softFill)
            strokeColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 120))
            strokeWidth = density.toInt().coerceAtLeast(1)
            setTextColor(primary)
            iconTint = ColorStateList.valueOf(primary)
            rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 60))
        }

        // Bar E — Gemelos: Descargar soft · Leer (stream) solid primary
        binding.stickyDownload.apply {
            backgroundTintList = ColorStateList.valueOf(softFill)
            strokeColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 140))
            strokeWidth = (1.5f * density).toInt()
            setTextColor(primary)
            iconTint = ColorStateList.valueOf(primary)
            layoutParams = (layoutParams as? android.widget.LinearLayout.LayoutParams)?.apply {
                weight = 1f
            } ?: layoutParams
        }
        binding.stickyRead.apply {
            backgroundTintList = ColorStateList.valueOf(primary)
            strokeWidth = 0
            setTextColor(onPrimary)
            iconTint = ColorStateList.valueOf(onPrimary)
            rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(onPrimary, 60))
            elevation = 10f * density
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                outlineAmbientShadowColor = primary
                outlineSpotShadowColor = ColorUtils.setAlphaComponent(primary, 180)
            }
            layoutParams = (layoutParams as? android.widget.LinearLayout.LayoutParams)?.apply {
                weight = 1f
            } ?: layoutParams
        }
    }

    /** Dark glyph on light accents (Soft/Black/Gold…); white on saturated Ultimate packs. */
    private fun contrastingOnColor(background: Int): Int {
        return if (ColorUtils.calculateLuminance(background) > 0.45) Color.BLACK else Color.WHITE
    }

    /** Style C — true circle tile, surface fill, primary glyph, thin rim (no mismatched bloom). */
    private fun applyActionGhostCircle(
        fab: ImageButton,
        glow: View,
        primary: Int,
        surface: Int,
        density: Float,
    ) {
        fab.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(surface)
            setStroke((1.5f * density).toInt(), ColorUtils.setAlphaComponent(primary, 90))
        }
        fab.imageTintList = ColorStateList.valueOf(primary)
        fab.elevation = 6f * density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fab.outlineAmbientShadowColor = ColorUtils.setAlphaComponent(primary, 80)
            fab.outlineSpotShadowColor = ColorUtils.setAlphaComponent(primary, 60)
        }
        fab.doOnLayout {
            fab.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            fab.clipToOutline = true
            fab.invalidateOutline()
        }
        // Glow matches the circle size (no square/oval clash).
        glow.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke((2 * density).toInt(), ColorUtils.setAlphaComponent(primary, 40))
        }
        glow.alpha = 0.7f
    }

    private fun stopDetailBreath() {
        detailBreathAnimators.forEach { it.cancel() }
        detailBreathAnimators.clear()
        binding?.stickyRead?.let {
            it.scaleX = 1f
            it.scaleY = 1f
            it.alpha = 1f
        }
    }

    /** Select FAB only: primary circle + soft matching halo. Filter stays stock ExtendedFAB. */
    private fun applySelectFabCircle(
        fab: ImageButton,
        glow: View,
        primary: Int,
        density: Float,
    ) {
        val onPrimary = contrastingOnColor(primary)
        fab.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(primary)
        }
        fab.imageTintList = ColorStateList.valueOf(onPrimary)
        fab.elevation = 12f * density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fab.outlineAmbientShadowColor = primary
            fab.outlineSpotShadowColor = ColorUtils.setAlphaComponent(primary, 200)
        }
        fab.doOnLayout {
            fab.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            fab.clipToOutline = true
            fab.invalidateOutline()
        }
        glow.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ColorUtils.setAlphaComponent(primary, 36))
            setStroke((2 * density).toInt(), ColorUtils.setAlphaComponent(primary, 70))
        }
        glow.alpha = 0.65f
    }

    private fun applyChapterFabsStyleE() {
        val binding = binding ?: return
        val primary = binding.root.context.colorFromAttribute(R.attr.colorPrimary)
        val density = resources.displayMetrics.density
        applySelectFabCircle(binding.chaptersSelectFab, binding.chaptersSelectFabGlow, primary, density)
        startChapterFabsBreath(
            listOf(binding.chaptersSelectFabHost to binding.chaptersSelectFabGlow)
        )
    }

    /** Soft scale + halo alpha pulse for the Select FAB only. */
    private fun startChapterFabsBreath(pairs: List<Pair<View, View>>) {
        if (chapterFabBreathAnimators.any { it.isRunning }) return
        stopChapterFabsBreath()
        val ease = AccelerateDecelerateInterpolator()
        pairs.forEach { (host, glow) ->
            chapterFabBreathAnimators += ObjectAnimator.ofPropertyValuesHolder(
                host,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.04f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.04f),
            ).apply {
                duration = 2600L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = ease
                start()
            }
            chapterFabBreathAnimators += ObjectAnimator.ofFloat(glow, View.ALPHA, 0.4f, 0.9f).apply {
                duration = 2600L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = ease
                start()
            }
        }
    }

    private fun stopChapterFabsBreath() {
        chapterFabBreathAnimators.forEach { it.cancel() }
        chapterFabBreathAnimators.clear()
        binding?.chaptersSelectFabHost?.let {
            it.scaleX = 1f
            it.scaleY = 1f
        }
        binding?.chaptersSelectFabGlow?.let {
            it.alpha = 0.65f
            it.scaleX = 1f
            it.scaleY = 1f
        }
    }

    override fun onDestroyView() {
        stopChapterFabsBreath()
        stopDetailBreath()
        super.onDestroyView()
    }

    /** Shows or hides the selection controls depending on the current tab and selection mode. */
    private fun updateSelectionUi() {
        val binding = binding ?: return
        val onChapters = viewModel.currentTabIndex.value == 3
        val selecting = viewModel.selectionMode.value == true

        val bar = binding.chapterSelectionBar.root

        binding.chaptersFab.isVisible = onChapters && !selecting
        binding.chaptersSelectFabHost.isVisible = onChapters && !selecting
        if (onChapters && !selecting) {
            applyChapterFabsStyleE()
        } else {
            stopChapterFabsBreath()
        }
        bar.isVisible = onChapters && selecting
        updateChapterListPadding(if (bar.isVisible) bar.height else 0)
        if (bar.isVisible) {
            bar.doOnNextLayout { updateChapterListPadding(it.height) }
        }
        selectionBackCallback.isEnabled = onChapters && selecting
    }

    private fun updateChapterListPadding(bottom: Int) {
        val list = binding?.chapterList ?: return
        if (list.paddingBottom == bottom) return
        list.setPadding(list.paddingLeft, list.paddingTop, list.paddingRight, bottom)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshChapterRows() {
        (binding?.chapterList?.adapter as? ChapterAdapter)?.notifyDataSetChanged()
    }

    private fun showSelectionMenu(anchor: View) {
        val items = listOf(
            R.string.select_all to R.string.select_all,
            R.string.select_range to R.string.select_range,
            R.string.clear_selection to R.string.clear_selection,
            R.string.filter_and_sort to R.string.filter_and_sort,
        )
        anchor.popupMenu(items, null) {
            when (itemId) {
                R.string.select_all -> viewModel.selectAllChapters()
                R.string.select_range -> showRangeDialog()
                R.string.clear_selection -> viewModel.clearChapterSelection()
                R.string.filter_and_sort -> binding?.chaptersFab?.performClick()
            }
        }
    }

    private fun showRangeDialog() {
        val act = activity ?: return
        val total = viewModel.totalChapters
        val dialogBinding = ChapterRangeDialogBinding.inflate(layoutInflater, null, false)

        AlertDialog.Builder(act, R.style.AlertDialogCustom)
            .setView(dialogBinding.root)
            .setTitle(getString(R.string.select_range) + " (1-$total)")
            .setPositiveButton(R.string.ok) { _, _ ->
                val from = dialogBinding.rangeFrom.text?.toString()?.toIntOrNull()
                val to = dialogBinding.rangeTo.text?.toString()?.toIntOrNull()
                if (from == null || to == null || from !in 1..total || to !in 1..total) {
                    CommonActivity.showToast(getString(R.string.select_range_invalid, total))
                } else {
                    viewModel.selectChapterRange(from, to)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Asks for the output format, which chapters to include (any ranges or single numbers)
     * and how many chapters go in each file, then starts the download.
     */
    private fun showDownloadDialog() {
        val act = activity ?: return
        val total = viewModel.totalChapters
        if (total <= 0) return
        val dialogBinding = ChapterDownloadDialogBinding.inflate(layoutInflater, null, false)
        dialogBinding.downloadChaptersLabel.text = getString(R.string.download_chapters_label, total)
        // Prefill selection, or every chapter when the user opened Descargar without selecting.
        dialogBinding.downloadChapters.setText(
            viewModel.selectionAsText().ifBlank { "1-$total" }
        )

        fun selectedKind() = when (dialogBinding.downloadFormat.checkedRadioButtonId) {
            R.id.format_pdf -> SelectionTaskKind.Pdf
            R.id.format_chapters -> SelectionTaskKind.Download
            else -> SelectionTaskKind.Epub
        }

        fun perFile() = dialogBinding.downloadPerFile.text?.toString()?.toIntOrNull() ?: 0

        fun parsedChapters() =
            ChapterSpec.parse(dialogBinding.downloadChapters.text?.toString().orEmpty(), total)

        val batchPresets = listOf(0, 10, 20, 50, 100, 200)
        val chipForBatch = batchPresets.associateWith { size ->
            Chip(act).apply {
                id = View.generateViewId()
                isCheckable = true
                text = if (size == 0) getString(R.string.download_one_file) else size.toString()
                dialogBinding.downloadPerFileChips.addView(this)
            }
        }
        var syncingChips = false

        fun syncChips() {
            syncingChips = true
            val match = chipForBatch[perFile()]
            if (match != null) {
                match.isChecked = true
            } else {
                dialogBinding.downloadPerFileChips.clearCheck()
            }
            syncingChips = false
        }

        chipForBatch.forEach { (size, chip) ->
            chip.setOnCheckedChangeListener { _, checked ->
                if (syncingChips) return@setOnCheckedChangeListener
                if (checked) {
                    dialogBinding.downloadPerFile.setText(if (size == 0) "" else size.toString())
                } else {
                    dialogBinding.root.post { syncChips() }
                }
            }
        }

        fun updateSummary() {
            val kind = selectedKind()
            val numbers = parsedChapters()
            syncChips()
            dialogBinding.downloadPerFileHolder.isVisible = kind != SelectionTaskKind.Download
            // Language picker stays available for EPUB, PDF and chapters-only.
            dialogBinding.downloadTranslateHolder.isVisible = true
            dialogBinding.downloadRanges.isVisible = false
            if (numbers != null && kind != SelectionTaskKind.Download) {
                val groups = ChapterSpec.group(numbers.toList(), perFile())
                dialogBinding.downloadRanges.text = groups.take(4)
                    .joinToString(" · ") { ChapterSpec.format(it) } +
                    if (groups.size > 4) " · …" else ""
                dialogBinding.downloadRanges.isVisible = groups.size > 1
            }
            dialogBinding.downloadSummary.text = when {
                numbers == null -> getString(R.string.download_chapters_invalid, total)
                kind == SelectionTaskKind.Download ->
                    getString(R.string.download_summary_chapters, numbers.size)

                else -> getString(
                    R.string.download_summary_files,
                    numbers.size,
                    ChapterSpec.group(numbers.toList(), perFile()).size
                )
            }
        }

        val langLabels = listOf(getString(R.string.download_translate_none)) +
            DownloadTranslateLangs.ALL.map { it.displayName }
        dialogBinding.downloadTranslateLang.adapter = ArrayAdapter(
            act,
            android.R.layout.simple_spinner_dropdown_item,
            langLabels,
        )
        // Default to Original — never auto-translate (source may already be Spanish).
        dialogBinding.downloadTranslateLang.setSelection(0)

        val engineLabels = listOf(
            getString(R.string.download_translate_engine_gemini),
            getString(R.string.download_translate_engine_google),
        )
        dialogBinding.downloadTranslateEngine.adapter = ArrayAdapter(
            act,
            android.R.layout.simple_spinner_dropdown_item,
            engineLabels,
        )
        // Prefer Gemini when a key exists; otherwise Google Translate online.
        val hasGeminiKey = AppSettings(act).translation.geminiApiKey.get().trim().isNotBlank()
        dialogBinding.downloadTranslateEngine.setSelection(if (hasGeminiKey) 0 else 1)

        fun syncTranslateUi() {
            val translating = dialogBinding.downloadTranslateLang.selectedItemPosition > 0
            dialogBinding.downloadTranslateEngineLabel.isVisible = translating
            dialogBinding.downloadTranslateEngine.isVisible = translating
            dialogBinding.downloadTranslateHint.text = when {
                !translating -> getString(R.string.download_translate_hint)
                dialogBinding.downloadTranslateEngine.selectedItemPosition == 0 ->
                    getString(R.string.download_translate_hint_gemini)
                else -> getString(R.string.download_translate_hint_google)
            }
        }

        dialogBinding.downloadTranslateLang.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) = syncTranslateUi()

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        dialogBinding.downloadTranslateEngine.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) = syncTranslateUi()

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

        dialogBinding.downloadChapters.doOnTextChanged { _, _, _, _ -> updateSummary() }
        dialogBinding.downloadPerFile.doOnTextChanged { _, _, _, _ -> updateSummary() }
        dialogBinding.downloadFormat.setOnCheckedChangeListener { _, _ ->
            updateSummary()
            syncTranslateUi()
        }
        updateSummary()
        syncTranslateUi()

        val dialog = AlertDialog.Builder(act, R.style.AlertDialogCustom)
            .setView(dialogBinding.root)
            .setTitle(R.string.download_selected)
            .setPositiveButton(R.string.download_start, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val numbers = parsedChapters()
            if (numbers == null) {
                CommonActivity.showToast(getString(R.string.download_chapters_invalid, total))
                return@setOnClickListener
            }
            val langIndex = dialogBinding.downloadTranslateLang.selectedItemPosition
            val translateRequest = if (langIndex > 0) {
                val lang = DownloadTranslateLangs.ALL.getOrNull(langIndex - 1)
                    ?: return@setOnClickListener
                val engine = if (dialogBinding.downloadTranslateEngine.selectedItemPosition == 0) {
                    DownloadTranslateEngine.Gemini
                } else {
                    DownloadTranslateEngine.GoogleOnline
                }
                if (engine == DownloadTranslateEngine.Gemini &&
                    AppSettings(act).translation.geminiApiKey.get().trim().isBlank()
                ) {
                    CommonActivity.showToast(getString(R.string.translation_key_missing))
                    return@setOnClickListener
                }
                DownloadTranslateRequest(lang, engine)
            } else {
                null
            }
            dialog.dismiss()
            viewModel.startDownload(selectedKind(), numbers, perFile(), translateRequest)
        }
    }

    private fun closeTaskDialog() {
        taskDialog?.setOnDismissListener(null)
        taskDialog?.dismiss()
        taskDialog = null
        taskBinding = null
    }

    /** Closes the blocking dialog without cancelling the job; progress continues in the notification. */
    private fun minimizeTaskDialog() {
        if (taskDialog == null && taskDialogMinimized) return
        taskDialogMinimized = true
        closeTaskDialog()
        CommonActivity.showToast(activity, getString(R.string.task_running_background))
    }

    private fun renderSelectionTask(state: SelectionTaskState?) {
        val act = activity ?: return
        if (state == null) {
            taskDialogMinimized = false
            closeTaskDialog()
            return
        }
        if (state.finished) {
            val wasMinimized = taskDialogMinimized
            taskDialogMinimized = false
            closeTaskDialog()
            showTaskResult(state)
            if (wasMinimized) {
                CommonActivity.showToast(
                    act,
                    getString(
                        if (state.error) R.string.task_background_failed
                        else R.string.task_background_done
                    ),
                )
            }
            return
        }

        if (taskDialogMinimized) {
            // Job keeps running via SelectionTaskService; do not re-block the UI.
            return
        }

        val dialogBinding = taskBinding ?: ChapterTaskDialogBinding.inflate(
            layoutInflater,
            null,
            false
        ).also { created ->
            taskBinding = created
            taskDialog = AlertDialog.Builder(act, R.style.AlertDialogCustom)
                .setView(created.root)
                .setCancelable(true)
                .setNeutralButton(R.string.task_continue_background, null)
                .setNegativeButton(R.string.cancel) { _, _ -> viewModel.cancelSelectionTask() }
                .setOnCancelListener { minimizeTaskDialog() }
                .show()
                .also { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        minimizeTaskDialog()
                    }
                }
        }

        dialogBinding.taskMessage.setText(
            when {
                state.writing -> R.string.task_writing_file
                state.translating -> R.string.task_translating
                state.kind == SelectionTaskKind.Pdf -> R.string.task_creating_pdf
                state.kind == SelectionTaskKind.Epub -> R.string.task_creating_epub
                else -> R.string.task_downloading_chapters
            }
        )
        dialogBinding.taskProgress.isIndeterminate = state.writing
        dialogBinding.taskProgress.max = maxOf(state.total, 1)
        dialogBinding.taskProgress.progress = state.done
        dialogBinding.taskCounter.text =
            getString(R.string.task_progress_format, state.done, state.total)
    }

    private fun showTaskResult(state: SelectionTaskState) {
        val act = activity ?: return
        val builder = AlertDialog.Builder(act, R.style.AlertDialogCustom)
        val uris = state.resultUris

        when {
            state.error && state.kind == SelectionTaskKind.Download ->
                builder.setMessage(R.string.download_failed)

            state.error && state.errorMessage != null ->
                builder.setMessage(getString(R.string.task_translate_failed, state.errorMessage))

            state.error -> builder.setMessage(R.string.task_export_failed)

            state.kind == SelectionTaskKind.Download -> builder.setMessage(
                getString(R.string.task_download_done, state.done - state.failed, state.failed)
            )

            else -> {
                builder.setMessage(
                    getString(
                        R.string.task_export_done,
                        state.resultNames.size,
                        state.resultNames.joinToString("\n")
                    )
                )
                if (uris.size == 1) {
                    builder.setPositiveButton(R.string.open) { _, _ ->
                        val mime = if (state.kind == SelectionTaskKind.Pdf) {
                            FileHelper.PDF_MIME
                        } else {
                            FileHelper.EPUB_MIME
                        }
                        FileOpener.askAndOpen(act, uris.first(), mime)
                    }
                }
            }
        }

        builder
            .setNegativeButton(R.string.close, null)
            .setOnDismissListener {
                viewModel.dismissSelectionTask()
                refreshSavedFiles()
            }
            .show()
    }

    /** Page and size that are on screen, so the list only jumps to the top when they change. */
    private var shownChapterPage: Pair<Int, Int>? = null
    private var jumpedToLastChapter = false
    private var playedEnter = false

    /** The cover rises and settles, the background fades in and the action bar slides up. */
    private fun playEnterAnimation() {
        val binding = binding ?: return
        val spring = android.view.animation.OvershootInterpolator(0.8f)
        binding.resultInfoHeader.apply {
            alpha = 0f
            translationY = 60f
            scaleX = 0.92f
            scaleY = 0.92f
            animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(450).setInterpolator(spring).start()
        }
        binding.resultPosterBlur.apply {
            alpha = 0f
            animate().alpha(0.6f).setDuration(600).start()
        }
        binding.stickyBar.apply {
            translationY = 200f
            animate().translationY(0f).setStartDelay(150).setDuration(400)
                .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
        }
    }

    /** Opens the chapter list on the page of the chapter the user stopped at, only the first time. */
    private fun jumpToLastChapterOnce() {
        if (jumpedToLastChapter) return
        if (viewModel.lastChapter() == null || viewModel.chapterPage.value == null) return
        jumpedToLastChapter = true
        viewModel.goToLastChapterPage()
    }

    private fun showChapterPageBar(page: ChapterPage<ChapterData>?) {
        val bar = binding?.chapterPager ?: return
        bar.root.isVisible = page != null && page.total > 0
        if (page == null) return
        bar.chapterPageLabel.text = if (page.pageCount == 1) {
            getString(R.string.chapter_page_single, page.total)
        } else {
            getString(
                R.string.chapter_page_label,
                page.page + 1,
                page.pageCount,
                page.firstPosition,
                page.lastPosition
            )
        }
        bar.chapterPagePrevious.apply {
            isEnabled = page.hasPrevious
            alpha = if (page.hasPrevious) 1f else 0.3f
        }
        bar.chapterPageNext.apply {
            isEnabled = page.hasNext
            alpha = if (page.hasNext) 1f else 0.3f
        }
        bar.chapterPageSize.text = if (page.size == ChapterPaging.ALL) {
            getString(R.string.chapters_per_page_all)
        } else {
            getString(R.string.chapters_per_page_value, page.size)
        }
    }

    /** Asks for a number and gives it back once it is inside [valid]. */
    private fun showNumberDialog(
        @androidx.annotation.StringRes title: Int,
        @androidx.annotation.StringRes error: Int,
        initial: Int,
        valid: (String) -> Int?,
        onNumber: (Int) -> Unit
    ) {
        val act = activity ?: return
        val input = EditText(act).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(initial.toString())
            setSelectAllOnFocus(true)
        }
        val padding = (20 * resources.displayMetrics.density).toInt()
        val holder = FrameLayout(act).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(act, R.style.AlertDialogCustom)
            .setTitle(title)
            .setView(holder)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val number = valid(input.text.toString())
                if (number == null) {
                    CommonActivity.showToast(getString(error))
                } else {
                    onNumber(number)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showGoToPageDialog() {
        val page = viewModel.chapterPage.value ?: return
        if (page.pageCount <= 1) return
        val act = activity ?: return
        val input = EditText(act).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.go_to_hint, page.pageCount, page.total)
            setText((page.page + 1).toString())
            setSelectAllOnFocus(true)
        }
        val padding = (20 * resources.displayMetrics.density).toInt()
        val holder = FrameLayout(act).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(act, R.style.AlertDialogCustom)
            .setTitle(R.string.go_to_page)
            .setMessage(getString(R.string.go_to_hint, page.pageCount, page.total))
            .setView(holder)
            .setPositiveButton(R.string.go_to_page, null)
            .setNeutralButton(R.string.go_to_chapter, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            val number = { input.text.toString().trim().toIntOrNull() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val target = number()?.takeIf { it in 1..page.pageCount }
                if (target == null) {
                    CommonActivity.showToast(getString(R.string.go_to_page_invalid))
                } else {
                    viewModel.goToChapterPage(target - 1)
                    dialog.dismiss()
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val target = number()?.let { ChapterPaging.pageOfPosition(it, page.total, page.size) }
                if (target == null) {
                    CommonActivity.showToast(getString(R.string.go_to_chapter_invalid))
                } else {
                    viewModel.goToChapterPage(target)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showPageSizeDialog() {
        val act = activity ?: return
        val current = viewModel.chapterPage.value?.size ?: ChapterPaging.DEFAULT_SIZE
        val sizes = ChapterPaging.PRESETS + ChapterPaging.ALL
        val labels = sizes.map { size ->
            if (size == ChapterPaging.ALL) getString(R.string.chapters_per_page_all)
            else getString(R.string.chapters_per_page_value, size)
        } + getString(R.string.chapters_per_page_custom)
        val checked = sizes.indexOf(current).takeIf { it >= 0 } ?: sizes.size

        AlertDialog.Builder(act, R.style.AlertDialogCustom)
            .setTitle(R.string.chapters_per_page_title)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                dialog.dismiss()
                if (which < sizes.size) {
                    viewModel.setChaptersPerPage(sizes[which])
                } else {
                    showNumberDialog(
                        title = R.string.chapters_per_page_custom,
                        error = R.string.chapters_per_page_invalid,
                        initial = current.takeIf { it > 0 } ?: ChapterPaging.DEFAULT_SIZE,
                        valid = ChapterPaging::parseSize,
                        onNumber = viewModel::setChaptersPerPage
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Shows the "Saved files" button only when the novel has files that can still be opened. */
    private fun refreshSavedFiles() {
        viewLifecycleOwner.lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) { viewModel.savedFiles() }
            binding?.resultSavedFiles?.apply {
                isVisible = files.isNotEmpty()
                text = getString(R.string.saved_files_count, files.size)
            }
        }
    }

    private fun showSavedFiles() {
        val act = activity ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) { viewModel.savedFiles() }
            if (files.isEmpty()) {
                refreshSavedFiles()
                return@launch
            }
            AlertDialog.Builder(act, R.style.AlertDialogCustom)
                .setTitle(R.string.saved_files)
                .setItems(files.map { it.name }.toTypedArray()) { _, which ->
                    val file = files[which]
                    FileOpener.askAndOpen(act, file.toUri(), file.mime)
                }
                .setNegativeButton(R.string.close, null)
                .show()
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        //only if really is onResume
        if (viewModel.isResume) {
            viewModel.refreshLastChapter()
            (binding?.chapterList?.adapter as? ChapterAdapter)?.notifyDataSetChanged()
            viewModel.isResume = false
        }
        refreshSavedFiles()

        activity?.apply {
            window?.navigationBarColor =
                colorFromAttribute(R.attr.primaryBlackBackground)
        }
    }

    private fun updateScrollHeight() {
        val binding = binding ?: return
        val displayMetrics = context?.resources?.displayMetrics ?: return
        val total = displayMetrics.heightPixels - binding.resultDownloadCard.height

        binding.resultNovelHolder.apply {
            setPadding(
                paddingLeft,
                paddingTop,
                paddingRight,
                maxOf(0, total)
            )
        }
        val parameter = binding.chapterList.layoutParams
        parameter.height =
            displayMetrics.heightPixels - binding.viewsAndRating.height - binding.resultTabs.height -
                    binding.resultScrollPadding.paddingTop -
                    resources.getDimensionPixelSize(R.dimen.chapter_pager_height)

        binding.chapterList.layoutParams = parameter
        //ViewGroup.LayoutParams(binding.chapterList.layoutParams.width,displayMetrics.heightPixels)
        /*binding.hiddenView.apply {
            setPadding(
                paddingLeft,
                paddingTop,
                paddingRight,
                maxOf(0, total)
            )
        }*/
    }


    private fun newState(loadResponse: Resource<LoadResponse>?) {
        if (loadResponse == null) return
        val binding = binding ?: return
        //activity?.window?.navigationBarColor =
        //    requireContext().colorFromAttribute(R.attr.bitDarkerGrayBackground)

        when (loadResponse) {
            is Resource.Failure -> {
                binding.apply {
                    resultLoading.isVisible = false
                    resultLoadingError.isVisible = true
                    resultHolder.isVisible = false
                    resultErrorText.text = loadResponse.errorString
                    stickyBar.isVisible = false
                    resultHeroFade.isVisible = false
                    resultPosterBlur.isVisible = false
                }
            }

            is Resource.Loading -> {
                binding.apply {
                    resultLoading.isVisible = true
                    resultLoadingError.isVisible = false
                    resultHolder.isVisible = false
                    stickyBar.isVisible = false
                    resultHeroFade.isVisible = false
                    resultPosterBlur.isVisible = false
                }
            }

            is Resource.Success -> {
                val res = loadResponse.value
                refreshSavedFiles()

                binding.apply {
                    downloadWarning.isVisible = (repo?.rateLimitTime ?: 0) > 2000

                    res.image?.let { img ->
                        resultEmptyView.setOnClickListener {
                            UIHelper.showImage(it.context, img)
                        }
                    }

                    resultPoster.setImage(res.image)
                    resultPosterBlur.setImage(res.image, radius = 100, sample = 3)

                    resultTitle.text = res.name
                    resultAuthor.text = res.author ?: getString(R.string.no_author)

                    resultRatingVotedCount.text = getString(R.string.no_data)
                    res.rating?.let { rating ->
                        resultRating.text = context?.getRating(rating)
                        val votes = res.peopleVoted
                        if (votes != null) {
                            resultRatingVotedCount.text =
                                getString(R.string.votes_format).format(votes)
                        }
                    }
                    resultViews.text =
                        res.views?.let { views -> humanReadableByteCountSI(views) }
                            ?: getString(R.string.no_data)

                    resultBack.setColorFilter(Color.WHITE)
                    resultTabs.removeAllTabs()
                    resultTabs.isVisible = false
                    val hasRelated = !res.related.isNullOrEmpty()
                    val hasChapters =
                        res is StreamResponse && res.data.isNotEmpty() // this was removed because of lag, because of shitty android
                    val api = repo

                    if (api != null && (api.hasReviews || hasRelated || hasChapters)) {
                        resultTabs.isVisible = true
                        resultTabs.addTab(resultTabs.newTab().setText(R.string.novel).setId(0))
                        if (api.hasReviews) {
                            resultTabs.addTab(
                                resultTabs.newTab().setText(R.string.reviews).setId(1)
                            )
                        }
                        if (hasRelated) {
                            resultTabs.addTab(
                                resultTabs.newTab().setText(R.string.related).setId(2)
                            )
                            relatedList.apply {
                                setRecycledViewPool(MainAdapter.sharedPool)
                                val mainPageAdapter = MainAdapter(this, 0)
                                adapter = mainPageAdapter
                                mainPageAdapter.submitList(res.related)
                            }
                            fixLayout(binding.root)
                        }
                        if (hasChapters) {
                            resultTabs.addTab(
                                resultTabs.newTab().setText(R.string.read_action_chapters).setId(3)
                            )
                        }
                    }
                    val target = viewModel.currentTabIndex.value
                    if (target != null) {
                        resultTabs.getTabAt(target)?.let { new ->
                            resultTabs.selectTab(new)
                        }
                    }


                    //viewsAndRating.isVisible = res.views != null || res.peopleVoted != null

                    val readStatusText = res.status?.resource?.let { getString(it) } ?: ""
                    resultStatus.text = readStatusText
                    resultStatus.isVisible = readStatusText.isNotBlank()

                    resultTag.removeAllViews()
                    if (res.tags == null && res.status == null) {
                        // resultTagHolder.isVisible = false
                    } else {
                        // resultTagHolder.isGone = res.tags.isNullOrEmpty()
                        resultTag.apply {

                            val map =
                                api?.tags?.mapIndexed { i, (value, _) -> value to i }
                                    ?.associate { it } ?: emptyMap()

                            res.tags?.forEach { tag ->
                                val chip = Chip(context)
                                val chipDrawable = ChipDrawable.createFromAttributes(
                                    context,
                                    null,
                                    0,
                                    R.style.ChipFilled
                                )
                                chip.setChipDrawable(chipDrawable)
                                chip.text = tag
                                chip.isChecked = false
                                chip.isCheckable = false
                                chip.isFocusable = false
                                chip.isClickable = false

                                map[tag]?.let { index ->
                                    chip.isClickable = true
                                    chip.setOnClickListener {
                                        val api = repo
                                        if (api != null)
                                            activity?.navigate(
                                                R.id.global_to_navigation_mainpage,
                                                MainPageFragment.newInstance(
                                                    api.name,
                                                    tag = index
                                                )
                                            )
                                    }
                                }

                                chip.setTextColor(context.colorFromAttribute(R.attr.textColor))
                                addView(chip)
                            }
                        }
                    }
                    res.synopsis?.let { synopsis ->
                        val syno = if (synopsis.length > MAX_SYNO_LENGH) {
                            synopsis.substring(0, MAX_SYNO_LENGH) + "..."
                        } else {
                            synopsis
                        }

                        var isExpanded = false
                        resultSynopsisText.setOnClickListener {
                            isExpanded = !isExpanded
                            resultSynopsisText.text =
                                if (isExpanded) res.synopsis.html() else syno.html()
                        }
                        resultSynopsisText.text = syno.html()
                    } ?: run {
                        resultSynopsisText.text = "..."
                    }

                    if (res is StreamResponse) {
                        resultChaptersInfoHolder.isVisible = true
                        resultChapters.text = res.data.size.toString()
                        resultChaptersInfo.text =
                            if (res.data.size == 1) getString(R.string.chapter) else getString(R.string.chapters)
                        stickyRead.isVisible = true
                        resultTotalChapters.isVisible = true
                        if (res.data.isNotEmpty()) {
                            resultTotalChapters.text =
                                getString(R.string.latest_format).format(res.data.last().name)
                        } else {
                            resultTotalChapters.text = getString(R.string.no_chapters)
                        }
                    } else {
                        resultChaptersInfoHolder.isVisible = false
                        resultTotalChapters.isVisible = false
                        stickyRead.isVisible = false
                    }

                    resultLoading.isVisible = false
                    resultLoadingError.isVisible = false
                    if (!playedEnter) {
                        playedEnter = true
                        playEnterAnimation()
                    }
                    resultHolder.isVisible = true
                    stickyBar.isVisible = true
                    resultHeroFade.isVisible = true
                    resultPosterBlur.isVisible = true
                    resultHolder.doOnNextLayout {
                        updateScrollHeight()
                    }
                    resultHolder.post {
                        updateScrollHeight()
                        resultHolder.post {
                            updateScrollHeight()
                            resultHolder.post {
                                updateScrollHeight()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun doAction(action: Int) {
        when (action) {
            R.string.resume -> {
                viewModel.download()
            }

            R.string.download -> {
                viewModel.downloadFrom(null)
            }

            R.string.re_downloaded -> {
                viewModel.download()
            }

            R.string.download_from_chapter -> {
                val chapters =
                    ((viewModel.loadResponse.value as? Resource.Success)?.value as? StreamResponse)?.data
                        ?: return

                val act = CommonActivity.activity ?: return

                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(act, R.style.AlertDialogCustom)

                val binding = ChapterDialogBinding.inflate(layoutInflater, null, false)
                val dialogClickListener =
                    DialogInterface.OnClickListener { _, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                viewModel.downloadFrom(
                                    binding.chapterEdit.text?.toString()?.toIntOrNull()
                                )
                            }

                            DialogInterface.BUTTON_NEGATIVE -> {
                            }
                        }
                    }

                builder.setView(binding.root)
                    .setTitle(R.string.download_from_chapter)
                    .setPositiveButton(R.string.download, dialogClickListener)
                    .setNegativeButton(R.string.cancel, dialogClickListener)
                    .show()

                binding.chapterEdit.doOnTextChanged { text, _, _, _ ->
                    val parsedInt = text?.toString()?.toIntOrNull()
                    if (parsedInt == null || parsedInt < 0 || parsedInt >= chapters.size) {
                        binding.chapterEdit.error = act.getString(R.string.error_outside_chapter)
                    } else {
                        binding.chapterEdit.error = null
                    }
                }
            }

            R.string.delete -> {
                viewModel.deleteAlert()
            }

            R.string.pause -> {
                viewModel.pause()
            }

            R.string.stop -> {
                viewModel.stop()
            }
        }
    }

    private fun getActions(): List<Int>? {
        val items = mutableListOf<Int>()
        val progressState =
            viewModel.downloadState.value ?: return null
        val canDownload =
            progressState.progress < progressState.total
        val canPartialDownload =
            progressState.downloaded < progressState.total && progressState.total > 1

        when (progressState.state) {
            DownloadState.IsPaused -> {
                items.add(R.string.resume)
                items.add(R.string.stop)
            }

            DownloadState.IsDownloading -> {
                items.add(R.string.pause)
                items.add(R.string.stop)
            }

            DownloadState.IsDone -> {
                if (canPartialDownload) {
                    items.add(R.string.download_from_chapter)
                }
            }

            DownloadState.IsPending -> {
            }

            DownloadState.IsFailed, DownloadState.IsStopped -> {
                if (canDownload) {
                    items.add(R.string.re_downloaded)
                }

                if (canPartialDownload) {
                    items.add(R.string.download_from_chapter)
                }
            }

            DownloadState.Nothing -> {
                if (canDownload) {
                    items.add(R.string.download)
                }

                if (canPartialDownload) {
                    items.add(R.string.download_from_chapter)
                }
            }
        }

        if (progressState.progress > 0) {
            items.add(R.string.delete)
        }
        return items
    }

    override fun onBindingCreated(binding: FragmentResultBinding, savedInstanceState: Bundle?) {
        val url = savedInstanceState?.getString("url") ?: arguments?.getString("url")
        ?: throw NotImplementedError()
        val apiName = savedInstanceState?.getString("apiName") ?: arguments?.getString("apiName")
        ?: throw NotImplementedError()

        activity?.window?.decorView?.clearFocus()
        hideKeyboard()

        if (viewModel.loadResponse.value == null)
            viewModel.initState(apiName, url)

        binding.apply {
            activity?.fixPaddingStatusbar(resultInfoHeader)
            resultHeroFade.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.TRANSPARENT, root.context.colorFromAttribute(R.attr.primaryBlackBackground))
            )

            //resultOpeninbrowerText.text = apiName //""// resultUrl

            resultReloadConnectionerror.setOnClickListener {
                viewModel.initState(apiName, url)
            }

            resultOpeninbrower.setOnClickListener {
                viewModel.openInBrowser()
            }

            resultReloadConnectionOpenInBrowser.setOnClickListener {
                viewModel.openInBrowser()
            }

            reviewsFab.setOnClickListener {
                resultReviews.smoothScrollToPosition(0) // NEEDS THIS TO RESET VELOCITY
                resultMainscroll.smoothScrollTo(0, 0)
            }

            val backParameter = resultBack.layoutParams as CoordinatorLayout.LayoutParams
            backParameter.setMargins(
                backParameter.leftMargin,
                backParameter.topMargin + (activity?.getStatusBarHeight() ?: 0),
                backParameter.rightMargin,
                backParameter.bottomMargin
            )
            resultBack.layoutParams = backParameter

            resultBack.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
                //activity?.onBackPressed()
                //    (activity as? AppCompatActivity)?.backPressed()
            }

            val parameter = resultEmptyView.layoutParams as LinearLayout.LayoutParams
            parameter.setMargins(
                parameter.leftMargin,
                parameter.topMargin + (activity?.getStatusBarHeight() ?: 0),
                parameter.rightMargin,
                parameter.bottomMargin
            )
            resultEmptyView.layoutParams = parameter

            resultShare.setOnClickListener {
                viewModel.share()
            }

            val reviewAdapter = ReviewAdapter()
            resultReviews.setRecycledViewPool(ReviewAdapter.sharedPool)
            resultReviews.adapter = reviewAdapter
            resultReviews.layoutManager = GridLayoutManager(context, 1)

            observe(viewModel.reviews) { reviews ->
                when (reviews) {
                    is Resource.Success -> {
                        resultviewReviewsLoading.isVisible = false
                        resultviewReviewsLoadingShimmer.startShimmer()
                        resultReviews.isVisible = true
                        // fuck jvm, we have to do a copy because otherwise it wont fucking register
                        reviewAdapter.submitList(reviews.value.map { it.copy() })
                    }

                    is Resource.Loading -> {
                        resultviewReviewsLoadingShimmer.stopShimmer()
                        resultviewReviewsLoading.isVisible = true
                        resultReviews.isVisible = false
                    }

                    is Resource.Failure -> {
                        debugException { "This should never happened" }
                    }
                }
            }

            resultTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    viewModel.switchTab(tab?.id, resultTabs.selectedTabPosition)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            resultBookmark.setOnClickListener { view ->
                val context = view.context ?: return@setOnClickListener
                context.showBottomDialog(
                    ReadType.entries.map { context.getString(it.stringRes) },
                    selectedIndex = ReadType.entries.map { it.prefValue }
                        .indexOf(viewModel.readState.value?.prefValue),
                    context.getString(R.string.bookmark), false, {}
                ) { selected ->
                    viewModel.bookmark(ReadType.entries[selected].prefValue)
                }
            }

            resultDownloadGenerateEpub.setOnClickListener {
                viewModel.readEpub()
            }

            val downloadLongClick = View.OnLongClickListener { v ->
                val items = getActions() ?: return@OnLongClickListener true
                v.popupMenu(items.map { it to it }, null) {
                    doAction(itemId)
                }
                return@OnLongClickListener true
            }
            resultDownloadBtt.setOnLongClickListener(downloadLongClick)
            stickyDownload.setOnLongClickListener(downloadLongClick)

            val downloadClick = View.OnClickListener { v ->
                // Primary path: EPUB/PDF/chapters dialog (batches + language translation).
                if (viewModel.totalChapters > 0) {
                    showDownloadDialog()
                    return@OnClickListener
                }
                val actions = getActions()
                if (actions == null) {
                    viewModel.downloadOrPause()
                    return@OnClickListener
                }
                if (actions.size == 1) {
                    doAction(actions[0])
                } else if (actions.contains(R.string.download) || actions.contains(R.string.pause)) {
                    viewModel.downloadOrPause()
                } else {
                    v.popupMenu(actions.map { it to it }, null) {
                        doAction(itemId)
                    }
                }
            }
            resultDownloadBtt.setOnClickListener(downloadClick)
            stickyDownload.setOnClickListener(downloadClick)

            resultReadOffline.setOnClickListener {
                viewModel.readOffline()
            }

            resultSavedFiles.setOnClickListener {
                showSavedFiles()
            }

            resultQuickstream.setOnClickListener {
                viewModel.streamRead()
            }
            stickyRead.setOnClickListener { resultQuickstream.performClick() }

            applyChapterFabsStyleE()
            applyResultDetailStyles()
            chaptersSelectFab.setOnClickListener {
                viewModel.setSelectionMode(true)
            }
            chapterSelectionBar.chapterSelectionClose.setOnClickListener {
                viewModel.setSelectionMode(false)
            }
            chapterSelectionBar.chapterSelectionMore.setOnClickListener { view ->
                showSelectionMenu(view)
            }
            chapterSelectionBar.chapterSelectionRead.setOnClickListener {
                viewModel.streamReadSelected()
            }
            chapterSelectionBar.chapterSelectionDownload.setOnClickListener {
                showDownloadDialog()
            }

            /*downloadDeleteTrashFromResult.setOnClickListener {
                viewModel.deleteAlert()
            }*/
        }

        observe(viewModel.currentTabIndex) { pos ->
            binding.apply {
                resultNovelHolder.isVisible = 0 == pos
                // hiddenView.isGone = 0 == pos
                resultReviewsholder.isVisible = 1 == pos
                reviewsFab.isVisible = 1 == pos
                resultRelatedholder.isVisible = 2 == pos
                resultChapterholder.isVisible = 3 == pos
            }
            updateSelectionUi()
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, selectionBackCallback)

        observe(viewModel.selectionMode) {
            updateSelectionUi()
            refreshChapterRows()
        }

        observe(viewModel.selectedChapters) { selected ->
            binding.chapterSelectionBar.chapterSelectionCount.text =
                getString(R.string.selected_chapters_count, selected.size)
            refreshChapterRows()
        }

        observeNullable(viewModel.selectionTask, ::renderSelectionTask)

        observe(viewModel.currentTabPosition) { pos ->
            if (binding.resultTabs.selectedTabPosition != pos) {
                binding.resultTabs.selectTab(binding.resultTabs.getTabAt(pos))
            }
        }

        observe(viewModel.readState) { state ->
            binding.resultBookmarkLabel.setText(
                if (state == ReadType.NONE) R.string.bookmark else state.stringRes
            )
            binding.resultBookmark.setImageResource(
                if (state == ReadType.NONE) R.drawable.ic_baseline_bookmark_border_24
                else R.drawable.ic_baseline_bookmark_24
            )
        }
        observe(viewModel.loadResponse, ::newState)

        binding.resultFavorite.setOnClickListener { viewModel.toggleFavorite() }
        observe(viewModel.isFavorite) { favorite ->
            binding.resultFavorite.setImageResource(
                if (favorite) R.drawable.ic_baseline_favorite_24
                else R.drawable.ic_baseline_favorite_border_24
            )
        }

        binding.chapterResume.resumeContinue.setOnClickListener {
            viewModel.lastChapter()?.let { chapter ->
                viewModel.streamRead(chapter)
                viewModel.isResume = true
            }
        }
        binding.chapterResume.resumeInfo.setOnClickListener { viewModel.goToLastChapterPage() }
        observeNullable(viewModel.lastChapterIndex) {
            val chapter = viewModel.lastChapter()
            binding.chapterResume.root.isVisible = chapter != null
            binding.chapterResume.resumeChapterName.text = chapter?.name
            refreshChapterRows()
            jumpToLastChapterOnce()
        }

        binding.chapterList.apply {
            setRecycledViewPool(ChapterAdapter.sharedPool)
            val mainPageAdapter = ChapterAdapter(viewModel)
            adapter = mainPageAdapter
            setHasFixedSize(true)
        }

        binding.chapterPager.apply {
            chapterPagePrevious.setOnClickListener { viewModel.previousChapterPage() }
            chapterPageNext.setOnClickListener { viewModel.nextChapterPage() }
            chapterPageLabel.setOnClickListener { showGoToPageDialog() }
            chapterPageSize.setOnClickListener { showPageSizeDialog() }
        }

        observeNullable(viewModel.chapterPage) { page ->
            showChapterPageBar(page)
            if (page != null) jumpToLastChapterOnce()
            val chapters = page?.items
            val position = page?.let { it.page to it.size }
            val pageChanged = position != shownChapterPage
            shownChapterPage = position
            val toTop = Runnable { if (pageChanged) binding.chapterList.scrollToPosition(0) }
            (binding.chapterList.adapter as? ChapterAdapter)?.let { adapter ->
                if (chapters == null || chapters.size > 300) {
                    // if we have too many it takes a long time to diff
                    adapter.submitIncomparableList(chapters, toTop)
                } else {
                    adapter.submitList(chapters, toTop)
                }
            }
        }

        binding.chaptersFab.setOnClickListener { _ ->
            val act = activity ?: return@setOnClickListener
            val bottomSheetDialog = BottomSheetDialog(act)
            val binding = ChapterFilterPopupBinding.inflate(act.layoutInflater, null, false)
            bottomSheetDialog.setContentView(binding.root)

            val filterTab = binding.filterTabs.newTab().setText(getString(R.string.mainpage_filter))
            val sortTab = binding.filterTabs.newTab()
                .setText(getString(R.string.mainpage_sort_by_button_text))
            binding.filterTabs.addTab(filterTab)
            binding.filterTabs.addTab(sortTab)
            binding.filterTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab?.position
                    binding.filterContent.isVisible = position == 0
                    binding.sortContent.isVisible = position == 1
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            })
            binding.filterTabs.selectTab(filterTab)

            binding.filterBookmarked.isChecked = ResultViewModel.filterChapterByBookmarked
            binding.filterRead.isChecked = ResultViewModel.filterChapterByRead
            binding.filterUnread.isChecked = ResultViewModel.filterChapterByUnread
            binding.filterDownloaded.isChecked = ResultViewModel.filterChapterByDownloads

            binding.filterBookmarked.setOnCheckedChangeListener { _, isChecked ->
                ResultViewModel.filterChapterByBookmarked = isChecked
                viewModel.reorderChapters()
            }

            binding.filterRead.setOnCheckedChangeListener { _, isChecked ->
                ResultViewModel.filterChapterByRead = isChecked
                viewModel.reorderChapters()
            }

            binding.filterUnread.setOnCheckedChangeListener { _, isChecked ->
                ResultViewModel.filterChapterByUnread = isChecked
                viewModel.reorderChapters()
            }

            binding.filterDownloaded.setOnCheckedChangeListener { _, isChecked ->
                ResultViewModel.filterChapterByDownloads = isChecked
                viewModel.reorderChapters()
            }

            val adapter =
                SortingMethodAdapter(ResultViewModel.sortChapterBy) { item, position, newId ->
                    binding.sortContent.adapter?.notifyItemRangeChanged(
                        0,
                        ResultViewModel.chapterSortingMethods.size
                    )
                    ResultViewModel.sortChapterBy = newId
                    viewModel.reorderChapters()
                }
            adapter.submitList(ResultViewModel.chapterSortingMethods.toList())
            binding.sortContent.adapter = adapter
            bottomSheetDialog.show()
        }

        observe(viewModel.downloadState) { progressState ->
            if (progressState == null) {
                //binding.downloadDeleteTrashFromResult.isVisible = false
                return@observe
            }
            //val hasDownload = progressState.progress > 0

            /*binding.downloadDeleteTrashFromResult.apply {
                isVisible = hasDownload
                isClickable = hasDownload
            }*/
            binding.resultDownloadProgressText.text =
                "${progressState.progress}/${progressState.total}"

            binding.resultDownloadProgressBarNotDownloaded.apply {
                println("progressState: ${progressState}")
                max = progressState.total.toInt() * 100
                val animation: ObjectAnimator = ObjectAnimator.ofInt(
                    this,
                    "progress",
                    this.progress,
                    (progressState.progress - progressState.downloaded).toInt() * 100
                )
                animation.duration = 500
                animation.setAutoCancel(true)
                animation.interpolator = DecelerateInterpolator()
                animation.start()
            }

            binding.resultDownloadProgressBar.apply {
                max = progressState.total.toInt() * 100

                val animation: ObjectAnimator = ObjectAnimator.ofInt(
                    this,
                    "progress",
                    this.progress,
                    progressState.progress.toInt() * 100
                )
                animation.duration = 500
                animation.setAutoCancel(true)
                animation.interpolator = DecelerateInterpolator()
                animation.start()
            }

            binding.resultReadOffline.apply {
                isVisible = progressState.progress > 0
                text = getString(R.string.read_offline, progressState.progress.toInt())
            }

            val ePubGeneration = progressState.progress > 0
            binding.resultDownloadGenerateEpub.apply {
                isClickable = ePubGeneration
                alpha = if (ePubGeneration) 1f else 0.5f
            }

            val canDownload =
                progressState.progress < progressState.total

            val canClick = progressState.total > 0
            binding.resultDownloadBtt.apply {
                isClickable = canClick
                alpha = if (canClick) 1f else 0.5f

                //iconSize = 30.toPx
                setText(
                    when (progressState.state) {
                        DownloadState.IsDone -> R.string.manage
                        DownloadState.IsDownloading -> R.string.pause
                        DownloadState.IsPaused -> R.string.resume
                        DownloadState.IsFailed -> R.string.re_downloaded
                        DownloadState.IsStopped -> R.string.downloaded
                        DownloadState.Nothing -> if (canDownload) {
                            R.string.download
                        } else {
                            R.string.manage
                        }

                        DownloadState.IsPending -> R.string.loading
                    }
                )
                setIconResource(
                    when (progressState.state) {
                        DownloadState.IsDownloading -> R.drawable.ic_baseline_pause_24
                        DownloadState.IsPaused -> R.drawable.netflix_play
                        DownloadState.IsFailed -> R.drawable.ic_baseline_autorenew_24
                        DownloadState.IsDone -> R.drawable.ic_outline_settings_24
                        DownloadState.Nothing -> if (canDownload) {
                            R.drawable.netflix_download
                        } else {
                            R.drawable.ic_outline_settings_24
                        }

                        else -> R.drawable.netflix_download
                    }
                )
                binding.stickyDownload.also { sticky ->
                    sticky.text = text
                    sticky.icon = icon
                    sticky.isClickable = isClickable
                    sticky.alpha = alpha
                }
            }
        }


        //result_container.setBackgroundColor(requireContext().colorFromAttribute(R.attr.bitDarkerGrayBackground))

        binding.resultMainscroll.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            val arr = IntArray(2)
            binding.resultScrollPadding.getLocationOnScreen(arr)
            /*context?.resources?.displayMetrics?.let { displayMetrics->
                binding.scrollHolder.setPadding(0, (displayMetrics.heightPixels - arr[1]),0,0)
                println("scrolled: ${binding.scrollHolder.paddingTop}")

            }*/
            if (viewModel.isInReviews()) {
                binding.reviewsFab.alpha = scrollY / 50.toPx.toFloat()
            }

            val scrollFade = maxOf(0f, 1 - scrollY / 170.toPx.toFloat())
            binding.resultInfoHeader.apply {
                alpha = scrollFade
                scaleX = 0.95f + scrollFade * 0.05f
                scaleY = 0.95f + scrollFade * 0.05f
            }

            val crossFade = maxOf(0f, 1 - scrollY / 140.toPx.toFloat())
            binding.resultBack.apply {
                alpha = crossFade
                isEnabled = crossFade > 0
            }

            //REVIEWS
            val dy = scrollY - oldScrollY
            if (dy > 0) { //check for scroll down
                val max = (v.getChildAt(0).measuredHeight - v.measuredHeight)
                if (scrollY >= max) {
                    viewModel.loadMoreReviews()
                }
            }
        }
    }
}