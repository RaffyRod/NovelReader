package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginStore
import com.lagradost.quicknovel.providers.BloggerValidation
import com.lagradost.quicknovel.providers.ConfigProblem
import com.lagradost.quicknovel.providers.CustomProvider
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.providers.CustomProviderParser
import com.lagradost.quicknovel.providers.CustomProviderPresets
import com.lagradost.quicknovel.providers.ProviderHostHealth
import com.lagradost.quicknovel.providers.ProviderTestReport
import com.lagradost.quicknovel.providers.TestStep
import com.lagradost.quicknovel.util.Apis
import com.lagradost.quicknovel.util.CustomProviders
import com.lagradost.quicknovel.util.ProviderManage
import com.lagradost.quicknovel.util.UIHelper.colorFromAttribute
import com.lagradost.quicknovel.util.UIHelper.getResourceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Dialogs to add, edit, test and remove providers made of CSS selectors. */
object CustomProviderDialogs {
    private const val DEFAULT_TEST_QUERY = "the"
    private const val DEFAULT_BLOGGER_TEST_QUERY = "a"

    /** Accessibility / e2e ids for the host-health long-press bottom sheet. */
    const val HOST_HEALTH_SHEET_DESC = "host_health_sheet"
    const val HOST_HEALTH_PROGRESS_DESC = "host_health_progress"
    const val HOST_HEALTH_RESULT_DESC = "host_health_result"
    const val HOST_HEALTH_CONFIGURED_DESC = "host_health_configured"
    const val HOST_HEALTH_FINAL_DESC = "host_health_final"
    const val HOST_HEALTH_STATUS_DESC = "host_health_status"
    const val HOST_HEALTH_LATENCY_DESC = "host_health_latency"
    const val HOST_HEALTH_USE_URL_DESC = "host_health_use_url"
    const val HOST_HEALTH_RECHECK_DESC = "host_health_recheck"
    const val HOST_HEALTH_CLOSE_DESC = "host_health_close"

    private class Field(
        @StringRes val hint: Int,
        val get: (CustomProviderConfig) -> String,
        val inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
    )

    private val fields = linkedMapOf(
        "name" to Field(R.string.cp_name, { it.name }),
        "url" to Field(R.string.cp_url, { it.baseUrl }, InputType.TYPE_TEXT_VARIATION_URI),
        "lang" to Field(R.string.cp_lang, { it.lang }),
        // searchUrl is not shown — derived from site URL (+ Madara ajax at browse time).
        "resultItem" to Field(R.string.cp_result_item, { it.resultItem }),
        "resultLink" to Field(R.string.cp_result_link, { it.resultLink }),
        "resultImage" to Field(R.string.cp_result_image, { it.resultImage }),
        "novelTitle" to Field(R.string.cp_novel_title, { it.novelTitle }),
        "novelAuthor" to Field(R.string.cp_novel_author, { it.novelAuthor }),
        "novelCover" to Field(R.string.cp_novel_cover, { it.novelCover }),
        "novelSynopsis" to Field(R.string.cp_novel_synopsis, { it.novelSynopsis }),
        "chapterLink" to Field(R.string.cp_chapter_link, { it.chapterLink }),
        "content" to Field(R.string.cp_content, { it.content }),
    )

    /**
     * Fuentes de Buscar: tap a row → Editar / Eliminar. Footer **Seleccionar** →
     * checkboxes + bulk delete. Theme attrs for Ultimate / Light / Amoled.
     */
    fun showManager(activity: Activity, onChanged: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val gap = (8 * density).toInt()
        val cardGap = (6 * density).toInt()
        val switchSlot = (56 * density).toInt()
        val railW = (3 * density).toInt().coerceAtLeast(2)
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val gray = activity.colorFromAttribute(R.attr.grayTextColor)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val boxBg = activity.colorFromAttribute(R.attr.boxItemBackground)
        val mutedStroke = activity.getResourceColor(R.attr.white, 0.14f)
        val accent = run {
            val a = activity.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorAccent))
            val c = a.getColor(0, 0)
            a.recycle()
            if (c != 0) c else primary
        }
        val danger = themedDangerColor(activity, primary)
        val checkTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_enabled),
            ),
            intArrayOf(primary, gray, ColorUtils.setAlphaComponent(gray, 90)),
        )
        val switchThumbTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked),
            ),
            intArrayOf(primary, gray),
        )
        val switchTrackTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked),
            ),
            intArrayOf(
                ColorUtils.setAlphaComponent(primary, 140),
                ColorUtils.setAlphaComponent(gray, 110),
            ),
        )

        fun pillBg(fill: Int, stroke: Int = 0): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(fill)
                if (stroke != 0) {
                    setStroke((1 * density).toInt().coerceAtLeast(1), stroke)
                }
            }

        fun rowBg(active: Boolean): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(boxBg)
                setStroke(
                    (1 * density).toInt().coerceAtLeast(1),
                    if (active) ColorUtils.setAlphaComponent(primary, 110) else mutedStroke,
                )
            }

        var currentTab = ProviderManage.Tab.ALL
        var currentQuery = ""
        var currentPage = 0
        var editing = false
        val selected = linkedSetOf<String>()
        var dialog: AlertDialog? = null

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad / 2)
        }

        val tabs = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, gap)
        }
        lateinit var renderPage: (Boolean) -> Unit
        lateinit var applyChrome: () -> Unit
        fun tabBtn(label: String, tab: ProviderManage.Tab): Button =
            Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = label
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = (3 * density).toInt()
                    it.marginEnd = (3 * density).toInt()
                }
                setOnClickListener {
                    currentTab = tab
                    currentPage = 0
                    renderPage(true)
                }
            }
        val tabActive = tabBtn(activity.getString(R.string.search_sources_tab_active), ProviderManage.Tab.ACTIVE)
        val tabInstalled = tabBtn(activity.getString(R.string.search_sources_tab_installed), ProviderManage.Tab.INSTALLED)
        val tabAll = tabBtn(activity.getString(R.string.search_sources_tab_all), ProviderManage.Tab.ALL)
        tabs.addView(tabActive)
        tabs.addView(tabInstalled)
        tabs.addView(tabAll)
        root.addView(tabs)

        val metaLabel = TextView(activity).apply {
            setTextColor(gray)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, 0, 0, gap)
        }
        root.addView(metaLabel)
        // Kept for goto tap target when multi-page (hidden when single page).
        val pageLabel = TextView(activity).apply {
            visibility = View.GONE
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        root.addView(pageLabel)

        val filterEdit = EditText(activity).apply {
            hint = activity.getString(R.string.plugin_cards_filter_hint)
            setTextColor(textColor)
            setHintTextColor(gray)
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(pad, (10 * density).toInt(), pad, (10 * density).toInt())
            background = pillBg(boxBg, mutedStroke)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).also { it.bottomMargin = gap }
        }
        root.addView(filterEdit)

        val selectRow = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, gap / 2)
            visibility = View.GONE
        }
        val selectPickRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val selectAllBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_select_all)
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(primary)
            background = pillBg(ColorUtils.setAlphaComponent(primary, 28))
        }
        val selectNoneBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_select_none)
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(gray)
        }
        // In-content delete: AlertDialog Neutral was clipped on CI under Agregar/Hecho.
        val deleteSelectedBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.cp_delete_selected)
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(danger)
            background = pillBg(ColorUtils.setAlphaComponent(danger, 36))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).also { it.topMargin = gap / 2 }
        }
        selectPickRow.addView(selectAllBtn)
        selectPickRow.addView(selectNoneBtn)
        selectRow.addView(selectPickRow)
        selectRow.addView(deleteSelectedBtn)
        root.addView(selectRow)

        val cardsHost = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(activity).apply {
            addView(cardsHost)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (420 * density).toInt(),
            )
            isVerticalScrollBarEnabled = false
        }
        root.addView(scroll)

        val nav = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, gap, 0, 0)
            visibility = View.GONE
        }
        val prevBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_cards_prev)
            isAllCaps = false
            setTextColor(textColor)
        }
        val gotoBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_cards_goto_page)
            isAllCaps = false
            setTextColor(primary)
        }
        val nextBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_cards_next)
            isAllCaps = false
            setTextColor(textColor)
        }
        nav.addView(prevBtn)
        nav.addView(gotoBtn)
        nav.addView(nextBtn)
        root.addView(nav)

        fun styleTabs() {
            fun paint(btn: Button, on: Boolean) {
                btn.setTextColor(if (on) primary else gray)
                btn.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
                btn.background = if (on) {
                    pillBg(ColorUtils.setAlphaComponent(primary, 36))
                } else {
                    pillBg(ColorUtils.setAlphaComponent(boxBg, 180), mutedStroke)
                }
            }
            paint(tabActive, currentTab == ProviderManage.Tab.ACTIVE)
            paint(tabInstalled, currentTab == ProviderManage.Tab.INSTALLED)
            paint(tabAll, currentTab == ProviderManage.Tab.ALL)
        }

        fun filtered(): List<ProviderManage.Row> {
            val active = ProviderManage.readActiveNames(activity)
            val all = ProviderManage.loadRows(activity, active)
            return ProviderManage.filter(ProviderManage.filterTab(all, currentTab), currentQuery)
        }

        fun reopen() {
            dialog?.dismiss()
            activity.runOnUiThread {
                onChanged()
                showManager(activity, onChanged)
            }
        }

        fun openRow(row: ProviderManage.Row) {
            if (editing) return
            dialog?.dismiss()
            when (row.kind) {
                ProviderManage.Kind.BUILTIN -> {
                    showToast(activity, activity.getString(R.string.search_sources_builtin_hint))
                    showManager(activity, onChanged)
                }
                ProviderManage.Kind.CUSTOM -> {
                    val cfg = row.config ?: return
                    // Tap opens edit directly (Eliminar stays in Seleccionar / options).
                    showEditor(activity, cfg) { reopen() }
                }
                ProviderManage.Kind.PLUGIN -> {
                    val plugin = row.plugin ?: return
                    PluginProviderDialogs.showOptionsForManage(activity, plugin) { reopen() }
                }
            }
        }

        /** Removes custom CSS and/or the LNReader plugin that owns that Search name. */
        fun deleteRows(doomed: List<ProviderManage.Row>) {
            for (row in doomed) {
                when (row.kind) {
                    ProviderManage.Kind.CUSTOM -> {
                        val name = row.config?.name?.trim().orEmpty()
                        if (name.isBlank()) continue
                        val linked = PluginStore.loadInstalled()
                            .firstOrNull { it.providerName.trim() == name }
                        if (linked != null) {
                            PluginInstaller.uninstall(activity, linked)
                        } else {
                            CustomProviders.remove(activity, name)
                        }
                    }
                    ProviderManage.Kind.PLUGIN ->
                        row.plugin?.let { PluginInstaller.uninstall(activity, it) }
                    ProviderManage.Kind.BUILTIN -> Unit
                }
            }
        }

        fun confirmDeleteSelected() {
            val doomed = filtered().filter {
                it.key in selected && it.kind != ProviderManage.Kind.BUILTIN
            }
            if (doomed.isEmpty()) {
                showToast(activity, activity.getString(R.string.plugin_pick_one))
                return
            }
            AlertDialog.Builder(activity, R.style.AlertDialogCustom)
                .setTitle(R.string.cp_delete_selected)
                .setMessage(activity.getString(R.string.cp_delete_selected_msg, doomed.size))
                .setPositiveButton(R.string.ok) { _, _ ->
                    deleteRows(doomed)
                    selected.clear()
                    showToast(
                        activity,
                        activity.getString(R.string.cp_delete_selected_done, doomed.size),
                    )
                    reopen()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        applyChrome = chrome@{
            selectRow.visibility = if (editing) View.VISIBLE else View.GONE
            val shown = dialog ?: return@chrome
            val neutral = shown.getButton(AlertDialog.BUTTON_NEUTRAL) ?: return@chrome
            val negative = shown.getButton(AlertDialog.BUTTON_NEGATIVE) ?: return@chrome
            val positive = shown.getButton(AlertDialog.BUTTON_POSITIVE) ?: return@chrome
            if (editing) {
                // Footer: only Hecho — delete lives in selectRow (avoids Neutral clip on narrow CI).
                positive.visibility = View.GONE
                neutral.visibility = View.GONE
                negative.visibility = View.VISIBLE
                negative.text = activity.getString(R.string.done)
                negative.setTextColor(accent)
            } else {
                positive.visibility = View.VISIBLE
                neutral.visibility = View.VISIBLE
                negative.visibility = View.VISIBLE
                positive.setTextColor(primary)
                neutral.text = activity.getString(R.string.cp_select)
                neutral.setTextColor(primary)
                negative.text = activity.getString(R.string.close)
                negative.setTextColor(gray)
            }
        }

        renderPage = { scrollToTop ->
            styleTabs()
            val list = filtered()
            val pageCount = ProviderManage.pageCount(list.size)
            currentPage = currentPage.coerceIn(0, pageCount - 1)
            val slice = ProviderManage.pageSlice(list, currentPage)
            val from = currentPage * ProviderManage.PAGE_SIZE
            val rangeStart = if (slice.isEmpty()) 0 else from + 1
            val rangeEnd = from + slice.size

            val countText = activity.getString(R.string.cp_manage_count, list.size)
            if (pageCount <= 1) {
                metaLabel.text = countText
                pageLabel.visibility = View.GONE
                nav.visibility = View.GONE
            } else {
                val pageText = activity.getString(
                    R.string.plugin_cards_page,
                    currentPage + 1,
                    pageCount,
                    rangeStart,
                    rangeEnd,
                    list.size,
                )
                metaLabel.text = "$countText · ${pageText.substringBefore(" · ")}"
                pageLabel.text = pageText
                pageLabel.visibility = View.GONE
                nav.visibility = View.VISIBLE
            }

            // Detach before removeAllViews — destroying a checked Switch can fire
            // onCheckedChange(false) and would disable every other row on the page
            // (same-URL mirrors filtered into one list are hit together).
            detachCompoundButtonListeners(cardsHost)
            cardsHost.removeAllViews()
            if (list.isEmpty()) {
                cardsHost.addView(
                    TextView(activity).apply {
                        text = activity.getString(R.string.cp_manage_empty_hint)
                        setTextColor(gray)
                        setPadding(0, gap, 0, gap)
                    },
                )
            } else {
                slice.forEach { row ->
                    val active = row.activeInSearch
                    val body = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        isClickable = !editing
                        isFocusable = !editing
                        if (!editing) setOnClickListener { openRow(row) }
                    }
                    body.addView(
                        TextView(activity).apply {
                            text = row.title
                            setTextColor(textColor)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            typeface = Typeface.DEFAULT_BOLD
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                        },
                    )
                    body.addView(
                        TextView(activity).apply {
                            text = row.subtitle
                            setTextColor(gray)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            setPadding(0, (2 * density).toInt(), 0, 0)
                        },
                    )

                    val cardPad = (12 * density).toInt()
                    val line = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(cardPad, cardPad, cardPad / 2, cardPad)
                        minimumHeight = (56 * density).toInt()
                        background = rowBg(active && !editing)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).also { it.bottomMargin = cardGap }
                    }

                    if (!editing) {
                        line.addView(
                            View(activity).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    railW,
                                    (36 * density).toInt(),
                                ).also { it.marginEnd = (10 * density).toInt() }
                                background = GradientDrawable().apply {
                                    cornerRadius = 2 * density
                                    setColor(if (active) primary else ColorUtils.setAlphaComponent(gray, 40))
                                }
                            },
                        )
                    }

                    if (editing) {
                        line.addView(
                            CheckBox(activity).apply {
                                isChecked = row.key in selected
                                isEnabled = row.kind != ProviderManage.Kind.BUILTIN
                                buttonTintList = checkTint
                                setOnCheckedChangeListener { _, checked ->
                                    if (checked) selected.add(row.key) else selected.remove(row.key)
                                }
                            },
                        )
                        line.addView(body)
                    } else {
                        val openHost = View.OnLongClickListener {
                            showHostHealthSheet(activity, row) { reopen() }
                            true
                        }
                        body.setOnLongClickListener(openHost)
                        line.setOnLongClickListener(openHost)
                        line.addView(body)
                        val toggle = Switch(activity).apply {
                            isChecked = row.activeInSearch
                            thumbTintList = switchThumbTint
                            trackTintList = switchTrackTint
                            setOnCheckedChangeListener { button, wantOn ->
                                button.setOnCheckedChangeListener(null)
                                if (!row.canToggleSearch) {
                                    button.isChecked = false
                                    dialog?.dismiss()
                                    row.plugin?.let { plugin ->
                                        PluginProviderDialogs.showOptionsForManage(activity, plugin) {
                                            reopen()
                                        }
                                    }
                                    return@setOnCheckedChangeListener
                                }
                                CustomProviders.setSearchEnabled(activity, row.searchName, wantOn)
                                onChanged()
                                renderPage(false)
                            }
                        }
                        line.addView(
                            FrameLayout(activity).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    switchSlot,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                )
                                addView(
                                    toggle,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        Gravity.CENTER_VERTICAL or Gravity.END,
                                    ),
                                )
                            },
                        )
                    }
                    cardsHost.addView(line)
                }
            }
            prevBtn.isEnabled = currentPage > 0
            nextBtn.isEnabled = currentPage < pageCount - 1
            gotoBtn.isEnabled = pageCount > 1
            applyChrome()
            if (scrollToTop) scroll.scrollTo(0, 0)
        }

        var applyingFilterText = false
        fun goTo(page: Int, query: String = filterEdit.text?.toString().orEmpty()) {
            currentQuery = query
            currentPage = page
            if (filterEdit.text?.toString().orEmpty() != currentQuery) {
                applyingFilterText = true
                filterEdit.setText(currentQuery)
                applyingFilterText = false
            }
            renderPage(true)
        }

        filterEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (applyingFilterText) return
                val q = s?.toString().orEmpty()
                if (q == currentQuery) return
                goTo(0, q)
            }
        })
        selectAllBtn.setOnClickListener {
            selected.clear()
            selected.addAll(
                filtered().filter { it.kind != ProviderManage.Kind.BUILTIN }.map { it.key },
            )
            renderPage(false)
        }
        selectNoneBtn.setOnClickListener {
            selected.clear()
            renderPage(false)
        }
        deleteSelectedBtn.setOnClickListener { confirmDeleteSelected() }
        prevBtn.setOnClickListener { goTo(currentPage - 1) }
        nextBtn.setOnClickListener { goTo(currentPage + 1) }

        dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.cp_manage)
            .setView(root)
            .setNeutralButton(R.string.cp_select, null)
            .setPositiveButton(R.string.cp_add) { _, _ ->
                showSimpleAdd(activity) {
                    onChanged()
                    showManager(activity, onChanged)
                }
            }
            .setNegativeButton(R.string.close, null)
            .create()

        dialog?.setOnShowListener {
            val shown = dialog ?: return@setOnShowListener
            gotoBtn.setOnClickListener {
                val pageCount = ProviderManage.pageCount(filtered().size)
                if (pageCount <= 1) return@setOnClickListener
                val edit = EditText(activity).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    hint = activity.getString(R.string.plugin_cards_goto_hint, pageCount)
                    setPadding(pad, pad, pad, pad)
                }
                AlertDialog.Builder(activity, R.style.AlertDialogCustom)
                    .setTitle(R.string.plugin_cards_goto_page)
                    .setView(edit)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val target = ProviderManage.parseGoToPage(edit.text?.toString(), pageCount)
                            ?: return@setPositiveButton
                        goTo(target)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            pageLabel.setOnClickListener { gotoBtn.performClick() }

            shown.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                // Select mode for bulk delete — per-source edit is via tapping a row.
                if (!editing) {
                    editing = true
                    selected.clear()
                    renderPage(false)
                }
            }
            shown.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                if (editing) {
                    editing = false
                    selected.clear()
                    renderPage(false)
                } else {
                    shown.dismiss()
                }
            }
            applyChrome()
        }
        renderPage(false)
        dialog?.show()
    }

    /**
     * Destructive accent blended from the active [primary] so delete stays readable on every
     * Ultimate / Light / Amoled overlay without a hard-coded red.
     */
    private fun themedDangerColor(activity: Activity, primary: Int): Int {
        val attrs = activity.obtainStyledAttributes(intArrayOf(android.R.attr.colorError))
        val fromTheme = attrs.getColor(0, 0)
        attrs.recycle()
        if (fromTheme != 0) return fromTheme
        return ColorUtils.blendARGB(primary, 0xFFFF5252.toInt(), 0.55f)
    }

    /** Clears listeners so tearing down the Fuentes list cannot cascade disable toggles. */
    private fun detachCompoundButtonListeners(root: ViewGroup) {
        for (i in 0 until root.childCount) {
            when (val child = root.getChildAt(i)) {
                is CompoundButton -> child.setOnCheckedChangeListener(null)
                is ViewGroup -> detachCompoundButtonListeners(child)
            }
        }
    }

    /** Site URL used for host probes (custom / plugin / built-in). */
    private fun probeUrlFor(row: ProviderManage.Row): String? {
        when (row.kind) {
            ProviderManage.Kind.CUSTOM -> {
                val url = row.config?.baseUrl?.trim().orEmpty()
                return url.ifBlank { null }
            }
            ProviderManage.Kind.PLUGIN -> {
                val mapped = row.plugin?.providerName?.trim().orEmpty()
                if (mapped.isNotBlank()) {
                    val cfg = CustomProviders.load().firstOrNull { it.name == mapped }
                    val fromCfg = cfg?.baseUrl?.trim().orEmpty()
                    if (fromCfg.isNotBlank()) return fromCfg
                }
                val site = row.plugin?.site?.trim().orEmpty()
                return site.ifBlank { null }
            }
            ProviderManage.Kind.BUILTIN ->
                return Apis.getApiFromNameNull(row.searchName)?.mainUrl?.trim()?.ifBlank { null }
        }
    }

    /**
     * Resolves a [ProviderManage.Row] for a Search tile [api], so the same host sheet
     * works from Buscar (long-press) and from Fuentes.
     */
    fun rowForApi(activity: Activity, api: MainAPI): ProviderManage.Row {
        val active = ProviderManage.readActiveNames(activity)
        val match = ProviderManage.loadRows(activity, active).firstOrNull { row ->
            row.searchName.equals(api.name, ignoreCase = true) ||
                row.title.equals(api.name, ignoreCase = true)
        }
        if (match != null) return match
        val url = api.mainUrl.trim().trimEnd('/')
        return ProviderManage.Row(
            key = "api:${api.name}",
            title = api.name,
            subtitle = url.ifBlank { api.lang },
            kind = if (api is CustomProvider) ProviderManage.Kind.CUSTOM else ProviderManage.Kind.BUILTIN,
            searchName = api.name,
            activeInSearch = true,
            canToggleSearch = true,
            config = (api as? CustomProvider)?.config,
        )
    }

    /**
     * Search tile entry: long-press only (tap still opens the provider).
     * Opens the same bottom sheet with progress loader (#1).
     */
    fun showHostHealthForApi(
        activity: Activity,
        api: MainAPI,
        onChanged: () -> Unit = {},
    ) {
        showHostHealthSheet(activity, rowForApi(activity, api), onChanged)
    }

    /**
     * Bottom sheet (mock #1): probes the host with a progress loader while the grid stays
     * dimmed behind, then shows configured URL, final URL, status, latency, and
     * “Use new URL” when the domain changed. Theme attrs for Ultimate / Light / Amoled.
     */
    fun showHostHealthSheet(activity: Activity, row: ProviderManage.Row, onChanged: () -> Unit) {
        val configured = probeUrlFor(row)
        if (configured.isNullOrBlank()) {
            showToast(activity, activity.getString(R.string.cp_host_no_url))
            return
        }
        val density = activity.resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val gap = (8 * density).toInt()
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val gray = activity.colorFromAttribute(R.attr.grayTextColor)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val boxBg = activity.colorFromAttribute(R.attr.boxItemBackground)
        val danger = themedDangerColor(activity, primary)
        val okColor = ColorUtils.blendARGB(textColor, 0xFF1B8A4A.toInt(), 0.55f).let {
            ColorUtils.blendARGB(it, primary, 0.2f)
        }
        val warnColor = ColorUtils.blendARGB(textColor, 0xFFE07010.toInt(), 0.5f).let {
            ColorUtils.blendARGB(it, primary, 0.25f)
        }
        val progressTint = ColorStateList.valueOf(primary)

        fun pillBtn(
            label: String,
            fill: Int,
            labelColor: Int,
            desc: String,
        ): MaterialButton =
            MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                isAllCaps = false
                setTextColor(labelColor)
                contentDescription = desc
                backgroundTintList = ColorStateList.valueOf(fill)
                strokeColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(gray, 80))
                strokeWidth = (1 * density).toInt().coerceAtLeast(1)
                cornerRadius = (12 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = (4 * density).toInt()
                    it.marginEnd = (4 * density).toInt()
                }
            }

        val sheet = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (10 * density).toInt(), pad, pad)
            contentDescription = HOST_HEALTH_SHEET_DESC
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    18 * density, 18 * density, 18 * density, 18 * density,
                    0f, 0f, 0f, 0f,
                )
                setColor(boxBg)
            }
        }

        // Drag handle — matches mock #1 bottom sheet.
        sheet.addView(
            View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (36 * density).toInt(),
                    (4 * density).toInt(),
                ).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                    it.bottomMargin = gap
                }
                background = GradientDrawable().apply {
                    cornerRadius = 2 * density
                    setColor(ColorUtils.setAlphaComponent(gray, 90))
                }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
        )

        val titleView = TextView(activity).apply {
            text = row.title
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
        }
        sheet.addView(titleView)
        sheet.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.cp_host_sheet_subtitle, row.subtitle)
                setTextColor(gray)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, (2 * density).toInt(), 0, gap)
            },
        )

        val checkingBlock = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, gap / 2, 0, gap)
            contentDescription = HOST_HEALTH_PROGRESS_DESC
        }
        checkingBlock.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.cp_host_checking)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            },
        )
        checkingBlock.addView(
            ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                isIndeterminate = true
                progressTintList = progressTint
                indeterminateTintList = progressTint
                secondaryProgressTintList = ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(primary, 70),
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.topMargin = gap }
            },
        )
        checkingBlock.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.cp_host_checking_hint)
                setTextColor(gray)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, gap / 2, 0, 0)
            },
        )
        sheet.addView(checkingBlock)

        val resultBlock = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            contentDescription = HOST_HEALTH_RESULT_DESC
        }
        fun metaLine(label: String, value: String, desc: String): TextView =
            TextView(activity).apply {
                text = "$label\n$value"
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, 0, 0, gap)
                setLineSpacing(0f, 1.15f)
                contentDescription = desc
            }
        val configuredView = metaLine(
            activity.getString(R.string.cp_host_configured),
            configured,
            HOST_HEALTH_CONFIGURED_DESC,
        )
        val finalView = metaLine(
            activity.getString(R.string.cp_host_final),
            "—",
            HOST_HEALTH_FINAL_DESC,
        )
        val statusView = TextView(activity).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, gap / 2)
            contentDescription = HOST_HEALTH_STATUS_DESC
        }
        val latencyView = TextView(activity).apply {
            setTextColor(gray)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, 0, 0, gap)
            contentDescription = HOST_HEALTH_LATENCY_DESC
        }
        resultBlock.addView(configuredView)
        resultBlock.addView(finalView)
        resultBlock.addView(statusView)
        resultBlock.addView(latencyView)
        sheet.addView(resultBlock)

        val actions = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, gap, 0, 0)
        }
        val useUrlBtn = pillBtn(
            activity.getString(R.string.cp_host_use_new_url),
            primary,
            android.graphics.Color.WHITE,
            HOST_HEALTH_USE_URL_DESC,
        ).also { it.visibility = View.GONE }
        val recheckBtn = pillBtn(
            activity.getString(R.string.cp_host_recheck),
            ColorUtils.setAlphaComponent(primary, 36),
            primary,
            HOST_HEALTH_RECHECK_DESC,
        )
        val closeBtn = pillBtn(
            activity.getString(R.string.close),
            ColorUtils.setAlphaComponent(gray, 36),
            gray,
            HOST_HEALTH_CLOSE_DESC,
        )
        actions.addView(useUrlBtn)
        actions.addView(recheckBtn)
        actions.addView(closeBtn)
        sheet.addView(actions)

        val dialog = BottomSheetDialog(activity, R.style.AppBottomSheetDialogTheme)
        dialog.setContentView(sheet)
        dialog.setCanceledOnTouchOutside(true)

        var lastReport: ProviderHostHealth.Report? = null
        var probeJob: Job? = null
        val scope = (activity as? AppCompatActivity)?.lifecycleScope

        fun applyNewUrl(report: ProviderHostHealth.Report) {
            val newBase = report.effectiveBaseUrl()
            when (row.kind) {
                ProviderManage.Kind.CUSTOM -> {
                    val cfg = row.config
                        ?: CustomProviders.load().firstOrNull { it.name == row.searchName }
                        ?: CustomProviders.load().firstOrNull { it.name == row.title }
                    if (cfg == null) {
                        showToast(activity, activity.getString(R.string.cp_host_no_url))
                        return
                    }
                    CustomProviders.save(activity, cfg.copy(baseUrl = newBase), replacing = cfg.name)
                }
                ProviderManage.Kind.PLUGIN -> {
                    val plugin = row.plugin ?: return
                    val mapped = plugin.providerName.trim()
                    if (mapped.isNotBlank()) {
                        val cfg = CustomProviders.load().firstOrNull { it.name == mapped }
                        if (cfg != null) {
                            CustomProviders.save(
                                activity,
                                cfg.copy(baseUrl = newBase),
                                replacing = cfg.name,
                            )
                        }
                    }
                    PluginStore.saveInstalled(plugin.copy(site = newBase))
                }
                ProviderManage.Kind.BUILTIN -> {
                    showToast(activity, activity.getString(R.string.cp_host_builtin_readonly))
                    return
                }
            }
            showToast(
                activity,
                activity.getString(
                    R.string.cp_host_url_updated,
                    ProviderHostHealth.hostOf(newBase).orEmpty(),
                ),
            )
            dialog.dismiss()
            onChanged()
        }

        fun paintResult(report: ProviderHostHealth.Report) {
            lastReport = report
            checkingBlock.visibility = View.GONE
            resultBlock.visibility = View.VISIBLE
            recheckBtn.isEnabled = true
            val finalShown = report.finalUrl ?: report.configuredUrl
            finalView.text = "${activity.getString(R.string.cp_host_final)}\n$finalShown"
            when {
                !report.reachable -> {
                    statusView.text = activity.getString(R.string.cp_host_status_down, report.userMessage())
                    statusView.setTextColor(danger)
                }
                report.domainChanged -> {
                    statusView.text = activity.getString(
                        R.string.cp_host_status_moved,
                        ProviderHostHealth.hostOf(report.finalUrl).orEmpty(),
                        ProviderHostHealth.hostOf(report.configuredUrl).orEmpty(),
                    )
                    statusView.setTextColor(warnColor)
                }
                else -> {
                    statusView.text = activity.getString(
                        R.string.cp_host_status_ok,
                        report.httpStatus?.toString() ?: "—",
                    )
                    statusView.setTextColor(okColor)
                }
            }
            val ms = report.latencyMs
            latencyView.text = if (ms != null) {
                activity.getString(R.string.cp_host_latency, ms)
            } else {
                activity.getString(R.string.cp_host_latency_unknown)
            }
            val canApply = report.reachable && report.domainChanged &&
                row.kind != ProviderManage.Kind.BUILTIN
            useUrlBtn.visibility = if (canApply) View.VISIBLE else View.GONE
            useUrlBtn.isEnabled = canApply
        }

        fun runProbe() {
            checkingBlock.visibility = View.VISIBLE
            resultBlock.visibility = View.GONE
            useUrlBtn.visibility = View.GONE
            recheckBtn.isEnabled = false
            if (scope == null) {
                paintResult(ProviderHostHealth.probe(configured))
                return
            }
            probeJob?.cancel()
            probeJob = scope.launch {
                val report = withContext(Dispatchers.IO) { ProviderHostHealth.probe(configured) }
                if (!dialog.isShowing) return@launch
                paintResult(report)
            }
        }

        useUrlBtn.setOnClickListener {
            val report = lastReport ?: return@setOnClickListener
            if (!report.domainChanged) return@setOnClickListener
            applyNewUrl(report)
        }
        recheckBtn.setOnClickListener { runProbe() }
        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.setOnShowListener {
            val bottom = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottom?.let { sheetView ->
                sheetView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheetView)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            runProbe()
        }
        dialog.setOnDismissListener { probeJob?.cancel() }
        dialog.show()
    }

    /**
     * Easy form: pick Blog or Website, then name and address.
     * Only the chosen layout is tested, so Blogger catalogs and Madara sites do not mix.
     */
    fun showSimpleAdd(activity: Activity, onSaved: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        val typeLabel = TextView(activity).apply {
            text = activity.getString(R.string.cp_type_label)
            setPadding(0, 0, 0, (4 * density).toInt())
        }
        column.addView(typeLabel)

        val typeGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.VERTICAL
            val blog = RadioButton(activity).apply {
                id = View.generateViewId()
                text = activity.getString(R.string.cp_type_blog)
            }
            val site = RadioButton(activity).apply {
                id = View.generateViewId()
                text = activity.getString(R.string.cp_type_site)
            }
            addView(blog)
            addView(site)
            check(blog.id)
            tag = blog.id to site.id
        }
        column.addView(typeGroup)

        val hint = TextView(activity).apply {
            text = activity.getString(R.string.cp_hint_blog)
            setPadding(0, (8 * density).toInt(), 0, (4 * density).toInt())
        }
        column.addView(hint)

        fun addInput(hintRes: Int, value: String = "", inputType: Int = InputType.TYPE_CLASS_TEXT): TextInputEditText {
            val layout = TextInputLayout(activity).apply { this.hint = activity.getString(hintRes) }
            val edit = TextInputEditText(layout.context).apply {
                this.inputType = inputType
                setText(value)
                setSingleLine()
            }
            layout.addView(edit)
            column.addView(layout, matchWidth(top = (8 * density).toInt()))
            return edit
        }
        val nameEdit = addInput(R.string.cp_name)
        val urlEdit = addInput(R.string.cp_site_address, inputType = InputType.TYPE_TEXT_VARIATION_URI)
        val langEdit = addInput(R.string.cp_lang, "es", InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)

        val validateBtn = MaterialButton(activity).apply {
            setText(R.string.cp_validate)
            visibility = View.VISIBLE
        }
        column.addView(validateBtn, matchWidth(top = (8 * density).toInt()))

        val (blogId, siteId) = typeGroup.tag as Pair<Int, Int>
        fun selectedKind() =
            if (typeGroup.checkedRadioButtonId == blogId) CustomProviderConfig.KIND_BLOGGER
            else CustomProviderConfig.KIND_CSS

        fun syncValidateVisibility() {
            validateBtn.visibility =
                if (selectedKind() == CustomProviderConfig.KIND_BLOGGER) View.VISIBLE else View.GONE
        }

        typeGroup.setOnCheckedChangeListener { _, checkedId ->
            hint.text = activity.getString(
                if (checkedId == blogId) R.string.cp_hint_blog else R.string.cp_hint_site
            )
            syncValidateVisibility()
        }
        syncValidateVisibility()

        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.cp_add)
            .setView(ScrollView(activity).apply { addView(column) })
            .setPositiveButton(R.string.cp_add, null)
            .setNeutralButton(R.string.cp_advanced, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()

        fun name() = nameEdit.text?.toString().orEmpty().trim()
        fun lang() = langEdit.text?.toString().orEmpty().trim().lowercase().ifBlank { "es" }
        fun baseUrl() = CustomProviderPresets.normalizeBaseUrl(urlEdit.text?.toString().orEmpty())

        val add = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            dialog.dismiss()
            val url = baseUrl().orEmpty()
            // Do not auto-fill Madara selectors — optional via the editor's "Madara preset" button.
            val prefill = if (selectedKind() == CustomProviderConfig.KIND_BLOGGER) {
                CustomProviderConfig.bloggerTemplate(name(), url).copy(lang = lang())
            } else {
                CustomProviderConfig(
                    name = name(),
                    baseUrl = url,
                    lang = lang(),
                    searchUrl = "",
                    resultItem = "",
                    novelTitle = "",
                    chapterLink = "",
                    content = "",
                )
            }
            showEditor(activity, existing = null, prefill = prefill, onSaved = onSaved)
        }

        fun setBusy(busy: Boolean) {
            add.isEnabled = !busy
            validateBtn.isEnabled = !busy
        }

        fun runBlogValidation(saveIfOk: Boolean) {
            val url = baseUrl()
            if (saveIfOk && (name().isBlank() || name() in CustomProviders.takenNames())) {
                showToast(R.string.cp_err_name)
                return
            }
            if (url == null) {
                showToast(R.string.cp_err_url)
                return
            }
            val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return
            val name = name().ifBlank { "Blog" }
            val lang = lang()
            setBusy(true)
            hint.text = activity.getString(R.string.cp_host_checking)
            scope.launch {
                val health = withContext(Dispatchers.IO) { ProviderHostHealth.probe(url) }
                if (!health.reachable) {
                    hint.text = activity.getString(R.string.cp_host_down, health.userMessage())
                    setBusy(false)
                    return@launch
                }
                val effectiveUrl = health.effectiveBaseUrl()
                if (health.domainChanged) {
                    urlEdit.setText(effectiveUrl)
                    hint.text = activity.getString(
                        R.string.cp_host_domain_changed,
                        ProviderHostHealth.hostOf(health.finalUrl).orEmpty(),
                        ProviderHostHealth.hostOf(url).orEmpty(),
                    )
                } else {
                    hint.text = activity.getString(R.string.cp_validating_blog)
                }
                val config = CustomProviderConfig.bloggerTemplate(name, effectiveUrl).copy(lang = lang)
                val check = withContext(Dispatchers.IO) { CustomProvider(config).validateBlogger() }
                if (check.ok) {
                    hint.text = buildString {
                        if (health.domainChanged) {
                            append(
                                activity.getString(
                                    R.string.cp_host_domain_changed,
                                    ProviderHostHealth.hostOf(health.finalUrl).orEmpty(),
                                    ProviderHostHealth.hostOf(url).orEmpty(),
                                )
                            )
                            append('\n')
                        }
                        append(formatBlogOk(activity, check))
                    }
                    if (saveIfOk) {
                        CustomProviders.save(activity, config)
                        showToast(R.string.cp_saved)
                        dialog.dismiss()
                        onSaved()
                        return@launch
                    }
                } else {
                    hint.text = activity.getString(
                        R.string.cp_blog_fail,
                        check.error ?: check.step.orEmpty()
                    )
                }
                setBusy(false)
            }
        }

        validateBtn.setOnClickListener { runBlogValidation(saveIfOk = false) }

        add.setOnClickListener {
            if (selectedKind() == CustomProviderConfig.KIND_BLOGGER) {
                runBlogValidation(saveIfOk = true)
                return@setOnClickListener
            }
            val url = baseUrl()
            if (name().isBlank() || name() in CustomProviders.takenNames()) {
                showToast(R.string.cp_err_name)
                return@setOnClickListener
            }
            if (url == null) {
                showToast(R.string.cp_err_url)
                return@setOnClickListener
            }
            val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return@setOnClickListener
            val name = name()
            val lang = lang()
            setBusy(true)
            hint.text = activity.getString(R.string.cp_host_checking)
            scope.launch {
                val health = withContext(Dispatchers.IO) { ProviderHostHealth.probe(url) }
                if (!health.reachable) {
                    hint.text = activity.getString(R.string.cp_host_down, health.userMessage())
                    setBusy(false)
                    return@launch
                }
                val effectiveUrl = health.effectiveBaseUrl()
                if (health.domainChanged) {
                    urlEdit.setText(effectiveUrl)
                    hint.text = activity.getString(
                        R.string.cp_host_domain_changed,
                        ProviderHostHealth.hostOf(health.finalUrl).orEmpty(),
                        ProviderHostHealth.hostOf(url).orEmpty(),
                    )
                } else {
                    hint.text = activity.getString(R.string.cp_detecting)
                }
                val detection = withContext(Dispatchers.IO) {
                    detect(
                        CustomProviderPresets.candidates(
                            name,
                            effectiveUrl,
                            lang,
                            CustomProviderConfig.KIND_CSS,
                        )
                    )
                }
                val found = detection.config
                if (found != null) {
                    CustomProviders.save(activity, found)
                    showToast(R.string.cp_saved)
                    dialog.dismiss()
                    onSaved()
                } else {
                    setBusy(false)
                    val reason = detection.error?.let { "\n\n($it)" }.orEmpty()
                    val domainNote = if (health.domainChanged) {
                        "\n\n" + activity.getString(
                            R.string.cp_host_domain_changed,
                            ProviderHostHealth.hostOf(health.finalUrl).orEmpty(),
                            ProviderHostHealth.hostOf(url).orEmpty(),
                        )
                    } else {
                        ""
                    }
                    hint.text = activity.getString(R.string.cp_not_detected) + domainNote + reason
                }
            }
        }
    }

    private fun formatBlogOk(activity: Activity, check: BloggerValidation): String {
        val modeLabel = when (check.mode) {
            "index" -> activity.getString(R.string.cp_blog_mode_index)
            "feed" -> activity.getString(R.string.cp_blog_mode_feed)
            else -> check.mode.orEmpty()
        }
        return activity.getString(
            R.string.cp_blog_ok,
            check.novels,
            modeLabel,
            check.sampleNovel.orEmpty(),
            check.chapters,
        )
    }

    /** First layout whose search, novel page and chapter text all work on the site. */
    private suspend fun detect(candidates: List<CustomProviderConfig>): Detection {
        var lastError: String? = null
        for (config in candidates) {
            val provider = CustomProvider(config)
            if (config.isBlogger) {
                val check = provider.validateBlogger()
                if (check.ok) return Detection(config, null)
                lastError = "blogger/${check.step}: ${check.error}"
                continue
            }
            for (query in CustomProviderPresets.testQueries) {
                val report = provider.runTest(query)
                if (report.success) return Detection(config, null)
                lastError = "${config.kind}/${report.failedStep}: ${report.error}"
                if (report.failedStep != TestStep.Search) break
            }
        }
        return Detection(null, lastError)
    }

    /** The layout that worked, or the last reason none did. */
    private data class Detection(val config: CustomProviderConfig?, val error: String?)

    private fun showOptions(activity: Activity, config: CustomProviderConfig, onChanged: () -> Unit) {
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(config.name)
            .setItems(arrayOf(activity.getString(R.string.cp_edit), activity.getString(R.string.cp_delete))) { _, which ->
                if (which == 0) {
                    showEditor(activity, config) {
                        onChanged()
                        showManager(activity, onChanged)
                    }
                } else {
                    confirmDelete(activity, config, onChanged)
                }
            }
            .show()
    }

    private fun confirmDelete(activity: Activity, config: CustomProviderConfig, onChanged: () -> Unit) {
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(config.name)
            .setMessage(R.string.cp_delete_confirm)
            .setPositiveButton(R.string.cp_delete) { _, _ ->
                CustomProviders.remove(activity, config.name)
                showToast(R.string.cp_deleted)
                onChanged()
                showManager(activity, onChanged)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> showManager(activity, onChanged) }
            .show()
    }

    /** Form for a provider. [existing] is null when a new one is being created. */
    fun showEditor(
        activity: Activity,
        existing: CustomProviderConfig?,
        prefill: CustomProviderConfig? = null,
        onSaved: () -> Unit,
    ) {
        val density = activity.resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        val inputs = HashMap<String, TextInputEditText>()
        val initial = existing ?: prefill ?: CustomProviderConfig(
            name = "", baseUrl = "", searchUrl = "", resultItem = "", novelTitle = "",
            chapterLink = "", content = ""
        )

        fields.forEach { (key, field) ->
            val layout = TextInputLayout(activity).apply { hint = activity.getString(field.hint) }
            val edit = TextInputEditText(layout.context).apply {
                inputType = field.inputType
                setText(field.get(initial))
                setSingleLine()
            }
            layout.addView(edit)
            column.addView(layout, matchWidth(top = (8 * density).toInt()))
            inputs[key] = edit
        }

        val newestFirst = CheckBox(activity).apply {
            setText(R.string.cp_newest_first)
            isChecked = initial.chaptersNewestFirst
        }
        column.addView(newestFirst, matchWidth(top = (8 * density).toInt()))
        val ajax = CheckBox(activity).apply {
            setText(R.string.cp_ajax)
            isChecked = initial.chaptersViaAjax
        }
        column.addView(ajax, matchWidth())

        val madara = MaterialButton(activity).apply { setText(R.string.cp_preset_madara) }
        column.addView(madara, matchWidth(top = (8 * density).toInt()))

        val testQuery = TextInputLayout(activity).apply { hint = activity.getString(R.string.cp_test_query) }
        val testQueryEdit = TextInputEditText(testQuery.context).apply {
            setText(if (initial.isBlogger) DEFAULT_BLOGGER_TEST_QUERY else DEFAULT_TEST_QUERY)
            setSingleLine()
        }
        testQuery.addView(testQueryEdit)
        column.addView(testQuery, matchWidth(top = (8 * density).toInt()))

        fun text(key: String) = inputs.getValue(key).text?.toString().orEmpty().trim()

        fun resolvedSearchUrl(): String = when {
            initial.isBlogger -> initial.searchUrl.ifBlank {
                CustomProviderConfig.bloggerTemplate().searchUrl
            }
            else -> CustomProviderConfig.DEFAULT_SITE_SEARCH_URL
        }

        fun read() = CustomProviderConfig(
            name = text("name"),
            baseUrl = text("url"),
            lang = text("lang").lowercase().ifBlank { "en" },
            searchUrl = resolvedSearchUrl(),
            resultItem = text("resultItem"),
            resultLink = text("resultLink"),
            resultImage = text("resultImage"),
            novelTitle = text("novelTitle"),
            novelAuthor = text("novelAuthor"),
            novelCover = text("novelCover"),
            novelSynopsis = text("novelSynopsis"),
            chapterLink = text("chapterLink"),
            chaptersNewestFirst = newestFirst.isChecked,
            chaptersViaAjax = ajax.isChecked,
            content = text("content"),
            kind = initial.kind,
        )

        madara.setOnClickListener {
            val template = CustomProviderConfig.madaraTemplate(text("name"), text("url"))
            fields.forEach { (key, field) ->
                if (key != "name" && key != "url" && key != "lang") {
                    inputs.getValue(key).setText(field.get(template))
                }
            }
            newestFirst.isChecked = template.chaptersNewestFirst
            ajax.isChecked = template.chaptersViaAjax
        }

        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(if (existing == null) R.string.cp_add else R.string.cp_edit)
            .setView(ScrollView(activity).apply { addView(column) })
            .setPositiveButton(R.string.cp_save, null)
            .setNeutralButton(R.string.cp_test, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()

        fun problemMessage(config: CustomProviderConfig): String? {
            val check = CustomProviderParser.validate(
                config,
                CustomProviders.takenNames(ignoring = existing?.name)
            ) ?: return null
            return when (check.problem) {
                ConfigProblem.EmptyName -> activity.getString(R.string.cp_err_name)
                ConfigProblem.InvalidUrl -> activity.getString(R.string.cp_err_url)
                ConfigProblem.SearchUrlWithoutQuery -> activity.getString(R.string.cp_err_search)
                ConfigProblem.MissingSelector -> activity.getString(R.string.cp_err_missing, check.detail)
                ConfigProblem.InvalidSelector -> activity.getString(R.string.cp_err_selector, check.detail)
            }
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val config = read()
            val problem = problemMessage(config)
            if (problem != null) {
                showToast(problem)
                return@setOnClickListener
            }
            val scope = (activity as? AppCompatActivity)?.lifecycleScope
            if (scope == null) {
                CustomProviders.save(activity, config, replacing = existing?.name)
                showToast(R.string.cp_saved)
                dialog.dismiss()
                onSaved()
                return@setOnClickListener
            }
            showToast(R.string.cp_host_checking)
            scope.launch {
                val health = withContext(Dispatchers.IO) { ProviderHostHealth.probe(config.baseUrl) }
                if (!health.reachable) {
                    showToast(activity.getString(R.string.cp_host_down, health.userMessage()))
                    return@launch
                }
                val toSave = if (health.domainChanged) {
                    showToast(
                        activity.getString(
                            R.string.cp_host_domain_changed,
                            ProviderHostHealth.hostOf(health.finalUrl).orEmpty(),
                            ProviderHostHealth.hostOf(config.baseUrl).orEmpty(),
                        )
                    )
                    config.copy(baseUrl = health.effectiveBaseUrl())
                } else {
                    config
                }
                CustomProviders.save(activity, toSave, replacing = existing?.name)
                showToast(R.string.cp_saved)
                dialog.dismiss()
                onSaved()
            }
        }

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            val config = read()
            val problem = problemMessage(config)
            val scope = (activity as? AppCompatActivity)?.lifecycleScope
            if (problem != null) {
                showToast(problem)
            } else if (scope != null) {
                showToast(R.string.cp_testing)
                val fallback = if (config.isBlogger) DEFAULT_BLOGGER_TEST_QUERY else DEFAULT_TEST_QUERY
                val query = testQueryEdit.text?.toString().orEmpty().ifBlank { fallback }
                scope.launch {
                    val health = withContext(Dispatchers.IO) { ProviderHostHealth.probe(config.baseUrl) }
                    if (!health.reachable) {
                        showToast(activity.getString(R.string.cp_host_down, health.userMessage()))
                        return@launch
                    }
                    if (health.domainChanged) {
                        showToast(
                            activity.getString(
                                R.string.cp_host_domain_changed,
                                ProviderHostHealth.hostOf(health.finalUrl).orEmpty(),
                                ProviderHostHealth.hostOf(config.baseUrl).orEmpty(),
                            )
                        )
                        return@launch
                    }
                    val report = withContext(Dispatchers.IO) { CustomProvider(config).runTest(query) }
                    showReport(activity, report)
                }
            }
        }
    }

    private fun showReport(activity: Activity, report: ProviderTestReport) {
        val lines = ArrayList<String>()
        if (report.results > 0) {
            lines.add(activity.getString(R.string.cp_test_search_ok, report.results, report.firstName))
        }
        if (report.chapters > 0) {
            lines.add(activity.getString(R.string.cp_test_chapters_ok, report.chapters))
        }
        if (report.textLength > 0) {
            lines.add(activity.getString(R.string.cp_test_text_ok, report.textLength))
        }
        val failedStep = report.failedStep
        if (failedStep != null) {
            val step = when (failedStep) {
                TestStep.Search -> R.string.cp_test_step_search
                TestStep.Novel -> R.string.cp_test_step_novel
                TestStep.Chapter -> R.string.cp_test_step_chapter
            }
            lines.add(
                activity.getString(R.string.cp_test_failed, activity.getString(step), report.error.orEmpty())
            )
        }
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(if (report.success) R.string.cp_test_ok else R.string.cp_test_problem)
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun matchWidth(top: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = top }
}
