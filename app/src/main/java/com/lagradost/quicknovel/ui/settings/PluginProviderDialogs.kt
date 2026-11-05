package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginCatalogFilter
import com.lagradost.quicknovel.plugins.PluginHubUi
import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginLangs
import com.lagradost.quicknovel.plugins.PluginManifestClient
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginRepo
import com.lagradost.quicknovel.plugins.PluginRepoUrlHistory
import com.lagradost.quicknovel.plugins.PluginSelectAll
import com.lagradost.quicknovel.plugins.PluginStore
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.providers.CustomProviderPresets
import com.lagradost.quicknovel.util.CustomProviders
import com.lagradost.quicknovel.util.UIHelper.colorFromAttribute
import com.lagradost.quicknovel.util.UIHelper.getResourceColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LNReader-style plugin flow: manage installed → add repo/fetch → languages → cards → install.
 * JSON is the catalog; install downloads JS. Search needs a CSS map (site URL first; Madara/Blogger optional).
 */
object PluginProviderDialogs {
    /** Catalog cards are paged so a 200+ JSON never freezes / looks truncated (no hard 100 cap). */
    private const val CARDS_PAGE_SIZE = PluginCatalogFilter.DEFAULT_PAGE_SIZE

    fun showHub(activity: Activity, onChanged: () -> Unit) {
        PluginStore.ensureOfficialRepo()
        val installed = PluginStore.loadInstalled()
        val updates = installed.count { it.hasUpdate }
        val density = activity.resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val gap = (10 * density).toInt()
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad)
        }
        val listHost = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(activity).apply {
            addView(listHost)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (520 * density).toInt(),
            )
        }

        val builder = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(activity.getString(R.string.plugin_hub_title_count, installed.size))
            .setView(root)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_browse_official, null)
        if (updates > 0) {
            builder.setNeutralButton(activity.getString(R.string.plugin_update_all, updates)) { _, _ ->
                updateAll(activity, onChanged)
            }
        } else {
            builder.setNeutralButton(R.string.plugin_add_repo) { _, _ ->
                showAddRepo(activity, onChanged)
            }
        }
        val dialog = builder.create()

        var currentQuery = ""
        var applyingSearch = false

        val filterCount = TextView(activity).apply {
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, gap / 2)
            visibility = View.GONE
        }

        fun renderList(query: String = currentQuery) {
            currentQuery = query
            listHost.removeAllViews()
            if (installed.isEmpty()) {
                filterCount.visibility = View.GONE
                listHost.addView(TextView(activity).apply {
                    text = activity.getString(R.string.plugin_none_simple)
                    setTextColor(grayText)
                    setPadding(0, 0, 0, gap)
                })
                return
            }
            val filtered = PluginHubUi.filterInstalled(installed, currentQuery)
            if (currentQuery.isBlank()) {
                filterCount.visibility = View.GONE
            } else {
                filterCount.visibility = View.VISIBLE
                filterCount.text = activity.getString(
                    R.string.plugin_hub_filter_count,
                    filtered.size,
                    installed.size,
                )
            }
            if (filtered.isEmpty()) {
                listHost.addView(TextView(activity).apply {
                    text = activity.getString(R.string.plugin_hub_search_empty)
                    setTextColor(grayText)
                    setPadding(0, gap, 0, gap)
                })
                return
            }
            filtered.forEach { plugin ->
                listHost.addView(buildPluginCard(activity, plugin, gap, textColor, grayText) { action ->
                    when (action) {
                        CardAction.Update -> {
                            dialog.dismiss()
                            activity.postUi { updateOne(activity, plugin, onChanged) }
                        }
                        CardAction.Remove -> {
                            dialog.dismiss()
                            activity.postUi { confirmUninstall(activity, plugin, onChanged) }
                        }
                        CardAction.ConfigureSearch -> {
                            dialog.dismiss()
                            activity.postUi { showAddToSearchChooser(activity, plugin, onChanged) }
                        }
                        CardAction.More -> showOptions(activity, plugin, onChanged)
                    }
                })
            }
        }

        if (installed.isNotEmpty()) {
            val inSearch = PluginHubUi.inSearchCount(installed)
            val pending = PluginHubUi.pendingSearchCount(installed)
            root.addView(TextView(activity).apply {
                text = activity.getString(R.string.plugin_hub_smart_banner, inSearch, pending)
                setTextColor(grayText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, gap)
                setLineSpacing(0f, 1.15f)
            })
            val filterRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, gap / 2)
            }
            val filterEdit = EditText(activity).apply {
                hint = activity.getString(R.string.plugin_cards_filter_hint)
                setTextColor(textColor)
                setHintTextColor(grayText)
                inputType = InputType.TYPE_CLASS_TEXT
                setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                contentDescription = activity.getString(R.string.plugin_cards_filter_hint)
            }
            val clearBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = activity.getString(R.string.plugin_cards_filter_clear)
                setTextColor(activity.getColor(R.color.colorPrimaryRed))
                visibility = View.GONE
                isAllCaps = false
            }
            filterRow.addView(filterEdit)
            filterRow.addView(clearBtn)
            root.addView(filterRow)
            root.addView(filterCount)

            filterEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (applyingSearch) return
                    val q = s?.toString().orEmpty()
                    clearBtn.visibility = if (q.isBlank()) View.GONE else View.VISIBLE
                    renderList(q)
                    scroll.scrollTo(0, 0)
                }
            })
            clearBtn.setOnClickListener {
                applyingSearch = true
                filterEdit.setText("")
                applyingSearch = false
                clearBtn.visibility = View.GONE
                renderList("")
                scroll.scrollTo(0, 0)
            }
            filterEdit.setOnEditorActionListener { _, _, _ ->
                renderList(filterEdit.text?.toString().orEmpty())
                true
            }
        }

        root.addView(scroll)
        renderList("")

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                dialog.dismiss()
                activity.postUi { openOfficialCatalog(activity, onChanged) }
            }
        }
        if (activity.canShowDialog()) dialog.show()
    }

    private enum class CardAction { Update, Remove, More, ConfigureSearch }

    private fun themedCardBackground(activity: Activity): GradientDrawable {
        val density = activity.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14 * density
            setColor(activity.colorFromAttribute(R.attr.boxItemBackground))
            setStroke(
                (1 * density).toInt().coerceAtLeast(1),
                activity.getResourceColor(R.attr.white, 0.18f),
            )
        }
    }

    private fun themedKindChipBackground(activity: Activity): GradientDrawable {
        val density = activity.resources.displayMetrics.density
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8 * density
            setColor(ColorUtils.setAlphaComponent(primary, 46))
        }
    }

    private fun buildPluginCard(
        activity: Activity,
        plugin: InstalledPlugin,
        gap: Int,
        textColor: Int,
        grayText: Int,
        onAction: (CardAction) -> Unit,
    ): View {
        val density = activity.resources.displayMetrics.density
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val cardPad = (12 * density).toInt()
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(cardPad, cardPad, cardPad, cardPad)
            background = themedCardBackground(activity)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).also { it.bottomMargin = gap }
            isClickable = true
            isFocusable = true
            setOnClickListener { onAction(CardAction.More) }
        }

        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(activity).apply {
            text = plugin.localName
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        top.addView(TextView(activity).apply {
            text = PluginHubUi.kindLabel(plugin)
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
            background = themedKindChipBackground(activity)
        })
        card.addView(top)

        val host = PluginHubUi.siteHost(plugin)
        if (host.isNotBlank()) {
            card.addView(TextView(activity).apply {
                text = host
                setTextColor(grayText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, (4 * density).toInt(), 0, 0)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
        }

        card.addView(TextView(activity).apply {
            text = PluginHubUi.metaLine(
                plugin,
                notInSearchLabel = activity.getString(R.string.plugin_meta_not_in_search),
            )
            setTextColor(grayText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, (6 * density).toInt(), 0, (4 * density).toInt())
        })
        card.addView(TextView(activity).apply {
            if (plugin.inSearch) {
                text = activity.getString(R.string.plugin_meta_in_search)
                setTextColor(activity.getColor(R.color.colorPrimaryGreen))
            } else {
                text = activity.getString(R.string.plugin_js_not_in_search)
                setTextColor(primary)
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, (8 * density).toInt())
        })

        val actions = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        if (!plugin.inSearch) {
            actions.addView(Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = activity.getString(R.string.plugin_action_configure_url_short)
                setTextColor(primary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                isAllCaps = false
                minHeight = 0
                minimumHeight = 0
                setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
                setOnClickListener { onAction(CardAction.ConfigureSearch) }
            })
        }
        actions.addView(Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_action_update)
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            isAllCaps = false
            minHeight = 0
            minimumHeight = 0
            setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
            setOnClickListener { onAction(CardAction.Update) }
        })
        actions.addView(Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_action_uninstall)
            setTextColor(activity.getColor(R.color.colorPrimaryRed))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            isAllCaps = false
            minHeight = 0
            minimumHeight = 0
            setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
            setOnClickListener { onAction(CardAction.Remove) }
        })
        card.addView(actions)
        return card
    }

    /** Fetches [PluginStore.OFFICIAL_MANIFEST_URL] and opens language → provider pickers. */
    private fun openOfficialCatalog(activity: Activity, onChanged: () -> Unit) {
        val scope = activity.pluginScope() ?: return
        val progress = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_browse_official)
            .setMessage(R.string.plugin_fetching)
            .setCancelable(false)
            .create()
        if (activity.canShowDialog()) progress.show()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val url = PluginStore.OFFICIAL_MANIFEST_URL
                    val items = PluginManifestClient.fetch(url)
                    url to items
                }
            }
            progress.dismissSafely()
            result.fold(
                onSuccess = { (url, items) ->
                    if (items.isEmpty()) {
                        showToast(activity, activity.getString(R.string.plugin_fetch_empty))
                        activity.postUi { showHub(activity, onChanged) }
                        return@fold
                    }
                    val repoId = "lnreader_official"
                    PluginStore.saveRepo(
                        PluginRepo(
                            id = repoId,
                            url = url,
                            label = "LNReader official",
                            lastSyncedAt = System.currentTimeMillis(),
                            pluginCount = items.size,
                        )
                    )
                    PluginStore.cacheManifest(repoId, items)
                    PluginInstaller.markUpdatesFromCatalog(items, url)
                    PluginRepoUrlHistory.remember(url, "LNReader official")
                    activity.postUi { showLanguages(activity, repoId, url, items, onChanged) }
                },
                onFailure = {
                    showToast(
                        activity,
                        activity.getString(R.string.plugin_fetch_fail, it.message ?: ""),
                    )
                    activity.postUi { showHub(activity, onChanged) }
                },
            )
        }
    }

    fun showAddRepo(activity: Activity, onChanged: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_repo_hint)
            setPadding(0, 0, 0, (8 * density).toInt())
        })
        val history = PluginRepoUrlHistory.load()
        val urlEdit = EditText(activity).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            hint = activity.getString(R.string.plugin_repo_url_hint)
            // Prefer last used catalog URL; leave blank so the official URL is not forced every time.
            setText(history.firstOrNull()?.url.orEmpty())
        }
        column.addView(urlEdit)
        var dialog: AlertDialog? = null
        fun rebuildAddRepo() {
            dialog?.dismiss()
            activity.postUi { showAddRepo(activity, onChanged) }
        }
        if (history.isNotEmpty()) {
            column.addView(TextView(activity).apply {
                text = activity.getString(R.string.plugin_repo_history_title)
                setTextColor(grayText)
                setPadding(0, (10 * density).toInt(), 0, (4 * density).toInt())
            })
            history.take(6).forEach { entry ->
                column.addView(TextView(activity).apply {
                    text = entry.label.ifBlank { entry.url }
                    setTextColor(primary)
                    setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
                    setOnClickListener { urlEdit.setText(entry.url) }
                    setOnLongClickListener {
                        PluginRepoUrlHistory.remove(entry.url)
                        showToast(activity, activity.getString(R.string.plugin_repo_history_removed))
                        rebuildAddRepo()
                        true
                    }
                })
            }
            column.addView(Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = activity.getString(R.string.plugin_repo_history_clear)
                setTextColor(activity.getColor(R.color.colorPrimaryRed))
                setOnClickListener {
                    PluginRepoUrlHistory.clear()
                    urlEdit.setText("")
                    showToast(activity, activity.getString(R.string.plugin_repo_history_cleared))
                    rebuildAddRepo()
                }
            })
        }
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_paste_json_label)
            setPadding(0, (12 * density).toInt(), 0, (4 * density).toInt())
        })
        val pasteEdit = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            hint = activity.getString(R.string.plugin_paste_json_hint)
        }
        column.addView(pasteEdit)
        val status = TextView(activity).apply {
            setPadding(0, (10 * density).toInt(), 0, 0)
            text = activity.getString(R.string.plugin_fetch_idle)
        }
        column.addView(status)
        val progress = ProgressBar(activity).apply {
            visibility = View.GONE
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        column.addView(progress)

        dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_add_repo)
            .setView(
                ScrollView(activity).apply {
                    addView(column)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (520 * density).toInt(),
                    )
                },
            )
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_fetch, null)
            .create()

        val shown = dialog ?: return
        shown.setOnShowListener {
            shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val scope = activity.pluginScope() ?: return@setOnClickListener
                progress.visibility = View.VISIBLE
                status.text = activity.getString(R.string.plugin_fetching)
                shown.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                scope.launch {
                    val paste = pasteEdit.text?.toString().orEmpty().trim()
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            if (paste.isNotBlank()) {
                                val items = PluginManifestClient.parse(paste)
                                "paste" to items
                            } else {
                                val url = urlEdit.text?.toString()?.trim().orEmpty()
                                    .ifBlank { PluginStore.OFFICIAL_MANIFEST_URL }
                                val items = PluginManifestClient.fetch(url)
                                url to items
                            }
                        }
                    }
                    progress.visibility = View.GONE
                    shown.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    result.fold(
                        onSuccess = { (source, items) ->
                            if (items.isEmpty()) {
                                status.text = activity.getString(R.string.plugin_fetch_empty)
                                return@fold
                            }
                            val repoUrl = if (source == "paste") "paste://local" else source
                            val repoId = repoUrl.hashCode().toString()
                            PluginStore.saveRepo(
                                PluginRepo(
                                    id = repoId,
                                    url = repoUrl,
                                    label = if (source == "paste") "Pasted JSON" else "LNReader",
                                    lastSyncedAt = System.currentTimeMillis(),
                                    pluginCount = items.size,
                                )
                            )
                            PluginStore.cacheManifest(repoId, items)
                            PluginInstaller.markUpdatesFromCatalog(items, repoUrl)
                            if (source != "paste") {
                                PluginRepoUrlHistory.remember(repoUrl, "LNReader")
                            }
                            status.text = activity.getString(
                                R.string.plugin_fetch_ok,
                                items.size,
                                PluginManifestClient.languageCounts(items).size,
                            )
                            shown.dismiss()
                            activity.postUi {
                                showLanguages(activity, repoId, repoUrl, items, onChanged)
                            }
                        },
                        onFailure = {
                            status.text = activity.getString(R.string.plugin_fetch_fail, it.message ?: "")
                        },
                    )
                }
            }
        }
        if (activity.canShowDialog()) shown.show()
    }

    private fun showLanguages(
        activity: Activity,
        repoId: String,
        repoUrl: String,
        items: List<PluginManifestItem>,
        onChanged: () -> Unit,
    ) {
        val density = activity.resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val gap = (8 * density).toInt()
        val counts = PluginManifestClient.languageCounts(items)
        val ordered = PluginLangs.ordered(counts)
        val selected = linkedSetOf<String>().apply {
            PluginLangs.PRIORITY_LABELS.filter { counts.containsKey(it) }.forEach { add(it) }
            if (isEmpty() && ordered.isNotEmpty()) add(ordered.first().first)
        }

        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val onPrimary = activity.colorFromAttribute(R.attr.colorOnPrimary)
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val boxBg = activity.colorFromAttribute(R.attr.boxItemBackground)
        // Light themes remap attr.white → black; dark themes keep white. Same stroke helper as hub cards.
        val mutedStroke = activity.getResourceColor(R.attr.white, 0.18f)
        val checkTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked),
            ),
            intArrayOf(primary, grayText),
        )

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad / 2)
            // Keep every language row LTR so Arabic / Hebrew labels don't push right.
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_langs_hint)
            setTextColor(grayText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, gap)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        })

        data class LangCard(
            val lang: String,
            val root: LinearLayout,
            val check: CheckBox,
            val avatar: TextView,
        )

        fun styleAvatar(avatar: TextView, checked: Boolean) {
            val fill = if (checked) primary else ColorUtils.setAlphaComponent(primary, 38)
            avatar.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(fill)
            }
            // colorOnPrimary follows each Ultimate / Light / Amoled overlay.
            avatar.setTextColor(if (checked) onPrimary else primary)
        }

        fun styleCard(card: LinearLayout, checked: Boolean) {
            card.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(boxBg)
                setStroke(
                    (1 * density).toInt().coerceAtLeast(1),
                    if (checked) primary else mutedStroke,
                )
            }
            card.alpha = 1f
        }

        val cards = ordered.map { (langRaw, count) ->
            val lang = PluginLangs.cleanLabel(langRaw)
            val code = PluginLangs.codeFor(lang).ifBlank { lang.take(2) }.uppercase().take(2)
            val checked0 = lang in selected || langRaw in selected
            val check = CheckBox(activity).apply {
                isChecked = checked0
                minWidth = 0
                minimumWidth = 0
                // No label on the checkbox — name lives in the card body (avoids RTL text runs).
                text = null
                buttonTintList = checkTint
                // Keep a clear gap from the language name / subtitle.
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also {
                    it.marginStart = (14 * density).toInt()
                    it.gravity = Gravity.CENTER_VERTICAL
                }
            }
            val avatar = TextView(activity).apply {
                text = code
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    (32 * density).toInt(),
                    (32 * density).toInt(),
                ).also { it.marginEnd = (12 * density).toInt() }
            }
            styleAvatar(avatar, checked0)

            val body = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginEnd = (8 * density).toInt()
                }
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }
            val title = PluginLangs.listLabel(lang)
            body.addView(TextView(activity).apply {
                text = if (lang in PluginLangs.PRIORITY_LABELS || title in PluginLangs.PRIORITY_LABELS) {
                    "★ $title"
                } else {
                    title
                }
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                textDirection = View.TEXT_DIRECTION_LTR
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            })
            body.addView(TextView(activity).apply {
                text = activity.getString(R.string.plugin_langs_sources, count)
                setTextColor(grayText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                textDirection = View.TEXT_DIRECTION_LTR
            })

            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                val cardPadH = (12 * density).toInt()
                val cardPadV = (12 * density).toInt()
                // Extra end padding so the check isn't flush against the card edge.
                setPadding(cardPadH, cardPadV, (10 * density).toInt(), cardPadV)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.bottomMargin = gap }
                isClickable = true
                isFocusable = true
            }
            styleCard(card, checked0)
            card.addView(avatar)
            card.addView(body)
            card.addView(check)

            fun sync(checked: Boolean) {
                if (checked) {
                    selected.add(lang)
                    selected.add(langRaw)
                } else {
                    selected.remove(lang)
                    selected.remove(langRaw)
                }
                styleAvatar(avatar, checked)
                styleCard(card, checked)
            }
            check.setOnCheckedChangeListener { _, checked -> sync(checked) }
            card.setOnClickListener { check.isChecked = !check.isChecked }

            column.addView(card)
            LangCard(lang, card, check, avatar)
        }

        val scroll = ScrollView(activity).apply {
            addView(column)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (480 * density).toInt(),
            )
        }
        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_langs_title)
            .setView(scroll)
            .setNeutralButton(R.string.plugin_langs_all, null)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_langs_continue, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).apply {
                isAllCaps = false
                setTextColor(primary)
                setOnClickListener {
                    cards.forEach { it.check.isChecked = true }
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                isAllCaps = false
                setTextColor(grayText)
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isAllCaps = false
                isSingleLine = true
                maxLines = 1
                setTextColor(primary)
                setOnClickListener {
                    val chosen = selected.map { PluginLangs.cleanLabel(it) }.toSet()
                    if (chosen.isEmpty()) {
                        showToast(activity, activity.getString(R.string.plugin_langs_pick_one))
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    val filtered = items.filter { PluginLangs.cleanLabel(it.displayLang) in chosen }
                        .sortedBy { it.name.lowercase() }
                    activity.postUi {
                        showCards(activity, repoUrl, filtered, onChanged, filterQuery = "", page = 0)
                    }
                }
            }
        }
        if (activity.canShowDialog()) dialog.show()
    }

    private fun showCards(
        activity: Activity,
        repoUrl: String,
        items: List<PluginManifestItem>,
        onChanged: () -> Unit,
        filterQuery: String = "",
        page: Int = 0,
        selectedIds: LinkedHashSet<String> = linkedSetOf(),
        statusFilter: PluginCatalogFilter.Status = PluginCatalogFilter.Status.ALL,
        kindFilter: PluginCatalogFilter.Kind = PluginCatalogFilter.Kind.ALL,
    ) {
        val density = activity.resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val gap = (10 * density).toInt()
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)
        val textColor = activity.colorFromAttribute(R.attr.textColor)

        var installedById = PluginStore.loadInstalled().associateBy { it.id }
        val selected = selectedIds
        var currentPage = page
        var currentQuery = filterQuery
        var currentStatus = statusFilter
        var currentKind = kindFilter
        val checkBoxes = LinkedHashMap<String, CheckBox>()
        var dialog: AlertDialog? = null

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad / 2)
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        val hintView = TextView(activity).apply {
            setTextColor(grayText)
            setPadding(0, 0, 0, gap / 2)
        }
        header.addView(hintView)

        val pageLabel = TextView(activity).apply {
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, gap)
            tag = "page_label"
        }
        header.addView(pageLabel)

        val filterRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val filterEdit = EditText(activity).apply {
            hint = activity.getString(R.string.plugin_cards_filter_hint)
            setText(currentQuery)
            setTextColor(textColor)
            setHintTextColor(grayText)
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        filterRow.addView(filterEdit)
        filterRow.addView(Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(android.R.string.search_go)
            setTextColor(primary)
            tag = "filter_go"
        })
        val clearBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_cards_filter_clear)
            setTextColor(activity.getColor(R.color.colorPrimaryRed))
            tag = "filter_clear"
            visibility = View.GONE
        }
        filterRow.addView(clearBtn)
        header.addView(filterRow)

        fun chipButton(label: String, tag: String): Button =
            Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = label
                this.tag = tag
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                minHeight = 0
                minimumHeight = 0
                setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            }

        fun styleChip(chip: Button?, active: Boolean) {
            chip ?: return
            chip.setTextColor(if (active) primary else grayText)
            chip.typeface = Typeface.defaultFromStyle(if (active) Typeface.BOLD else Typeface.NORMAL)
        }

        val statusRow = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
        }
        val statusChips = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusChips.addView(chipButton(activity.getString(R.string.plugin_cards_filter_all), "status_all"))
        statusChips.addView(chipButton(activity.getString(R.string.plugin_cards_filter_new), "status_new"))
        statusChips.addView(chipButton(activity.getString(R.string.plugin_cards_filter_updates), "status_updates"))
        statusChips.addView(chipButton(activity.getString(R.string.plugin_cards_filter_installed), "status_installed"))
        statusRow.addView(statusChips)
        header.addView(statusRow)

        // Madara/Blogger/JS kind chips removed — Madara/Blog are added via URL/JSON
        // (Proveedores propios). Keep status chips + name filter only.
        val selectRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * density).toInt(), 0, gap / 2)
            tag = "select_row"
        }
        val selectAllBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_select_all)
            isAllCaps = false
            setTextColor(primary)
            tag = "select_all"
        }
        val selectNoneBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_select_none)
            isAllCaps = false
            setTextColor(grayText)
            tag = "select_none"
        }
        selectRow.addView(selectAllBtn)
        selectRow.addView(selectNoneBtn)
        header.addView(selectRow)

        val pageHint = TextView(activity).apply {
            text = activity.getString(R.string.plugin_cards_page_hint)
            setTextColor(grayText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, gap / 2)
            tag = "page_hint"
        }
        header.addView(pageHint)
        root.addView(header)

        val cardsHost = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            tag = "cards_host"
        }
        // Fixed height so filters + nav stay visible; only the card list scrolls.
        val scroll = ScrollView(activity).apply {
            addView(cardsHost)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (420 * density).toInt(),
            )
        }
        root.addView(scroll)

        val nav = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, gap, 0, 0)
        }
        val prevBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_cards_prev)
            isAllCaps = false
            tag = "prev"
        }
        val gotoBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_cards_goto_page)
            isAllCaps = false
            setTextColor(primary)
            tag = "goto"
        }
        val nextBtn = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.plugin_cards_next)
            isAllCaps = false
            tag = "next"
        }
        nav.addView(prevBtn)
        nav.addView(gotoBtn)
        nav.addView(nextBtn)
        root.addView(nav)

        fun filteredList(): List<PluginManifestItem> = PluginCatalogFilter.filter(
            items = items,
            query = currentQuery,
            status = currentStatus,
            kind = PluginCatalogFilter.Kind.ALL,
            installedById = installedById,
        )

        fun reopenAfterExternalChange() {
            dialog?.dismiss()
            activity.postUi {
                showCards(
                    activity,
                    repoUrl,
                    items,
                    onChanged,
                    currentQuery,
                    currentPage,
                    selected,
                    currentStatus,
                    currentKind,
                )
            }
        }

        fun renderPage(scrollToTop: Boolean = true) {
            installedById = PluginStore.loadInstalled().associateBy { it.id }
            val updateCount = items.count {
                val inst = installedById[it.id] ?: return@count false
                PluginSelectAll.hasCatalogUpdate(inst, it)
            }
            val newCount = items.count { it.id !in installedById }
            hintView.text = activity.getString(R.string.plugin_cards_hint, items.size, newCount, updateCount)

            val filtered = filteredList()
            val pageCount = PluginCatalogFilter.pageCount(filtered.size, CARDS_PAGE_SIZE)
            currentPage = currentPage.coerceIn(0, pageCount - 1)
            val from = currentPage * CARDS_PAGE_SIZE
            val slice = PluginCatalogFilter.pageSlice(filtered, currentPage, CARDS_PAGE_SIZE)
            val to = (from + slice.size).coerceAtLeast(from)

            pageLabel.text = activity.getString(
                R.string.plugin_cards_page,
                currentPage + 1,
                pageCount,
                if (slice.isEmpty()) 0 else from + 1,
                to,
                filtered.size,
            )
            pageLabel.isClickable = pageCount > 1
            pageLabel.isFocusable = pageCount > 1

            val filtersActive = currentQuery.isNotBlank() ||
                currentStatus != PluginCatalogFilter.Status.ALL
            clearBtn.visibility = if (filtersActive) View.VISIBLE else View.GONE

            styleChip(statusChips.findViewWithTag("status_all"), currentStatus == PluginCatalogFilter.Status.ALL)
            styleChip(statusChips.findViewWithTag("status_new"), currentStatus == PluginCatalogFilter.Status.NEW)
            styleChip(statusChips.findViewWithTag("status_updates"), currentStatus == PluginCatalogFilter.Status.UPDATES)
            styleChip(statusChips.findViewWithTag("status_installed"), currentStatus == PluginCatalogFilter.Status.INSTALLED)

            cardsHost.removeAllViews()
            checkBoxes.clear()
            slice.forEach { item ->
                val installed = installedById[item.id]
                val canSelect = PluginSelectAll.isSelectable(item, installedById)
                val hasUpdate = installed != null && PluginSelectAll.hasCatalogUpdate(installed, item)
                val built = buildCatalogSelectCard(
                    activity = activity,
                    item = item,
                    installed = installed,
                    canSelect = canSelect,
                    hasUpdate = hasUpdate,
                    gap = gap,
                    textColor = textColor,
                    grayText = grayText,
                    onToggle = { id, checked ->
                        if (checked) selected.add(id) else selected.remove(id)
                    },
                    onUninstall = { plugin ->
                        dialog?.dismiss()
                        confirmUninstall(activity, plugin, onChanged) {
                            selected.remove(plugin.id)
                            reopenAfterExternalChange()
                        }
                    },
                )
                built.checkBox.isChecked = item.id in selected && canSelect
                checkBoxes[item.id] = built.checkBox
                cardsHost.addView(built.root)
            }

            prevBtn.isEnabled = currentPage > 0
            nextBtn.isEnabled = currentPage < pageCount - 1
            gotoBtn.isEnabled = pageCount > 1
            if (scrollToTop) scroll.scrollTo(0, 0)
        }

        var applyingFilterText = false

        fun goTo(
            nextPage: Int,
            query: String = filterEdit.text?.toString().orEmpty(),
            status: PluginCatalogFilter.Status = currentStatus,
            kind: PluginCatalogFilter.Kind = currentKind,
        ) {
            currentQuery = query
            currentStatus = status
            currentKind = kind
            currentPage = nextPage
            if (filterEdit.text?.toString().orEmpty() != currentQuery) {
                applyingFilterText = true
                filterEdit.setText(currentQuery)
                applyingFilterText = false
            }
            renderPage(scrollToTop = true)
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

        fun applySelectAll() {
            installedById = PluginStore.loadInstalled().associateBy { it.id }
            val ids = PluginSelectAll.selectableIds(filteredList(), installedById)
            selected.clear()
            selected.addAll(ids)
            // Re-render so every visible checkbox reflects selection (listener-safe).
            renderPage(scrollToTop = false)
            showToast(
                activity,
                activity.getString(R.string.plugin_select_all_applied, ids.size),
            )
        }

        fun applySelectNone() {
            selected.clear()
            renderPage(scrollToTop = false)
        }

        selectAllBtn.setOnClickListener { applySelectAll() }
        selectNoneBtn.setOnClickListener { applySelectNone() }

        renderPage(scrollToTop = false)

        prevBtn.setOnClickListener { goTo(currentPage - 1) }
        nextBtn.setOnClickListener { goTo(currentPage + 1) }

        dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_cards_title)
            .setView(root)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_install_or_update, null)
            .create()
        dialog?.setOnShowListener {
            val shown = dialog ?: return@setOnShowListener
            shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val chosen = PluginSelectAll.filterSelectable(items, selected, installedById)
                if (chosen.isEmpty()) {
                    showToast(activity, activity.getString(R.string.plugin_pick_one))
                    return@setOnClickListener
                }
                shown.dismiss()
                installSelected(
                    activity = activity,
                    repoUrl = repoUrl,
                    items = chosen,
                    installedById = installedById,
                    onChanged = onChanged,
                    catalogItems = items,
                    filterQuery = currentQuery,
                    page = currentPage,
                    statusFilter = currentStatus,
                    kindFilter = currentKind,
                )
            }
            fun askGoToPage() {
                val pageCount = PluginCatalogFilter.pageCount(filteredList().size, CARDS_PAGE_SIZE)
                if (pageCount <= 1) return
                // Empty field (not prefilled) so typing "3" cannot become "13" and jump to the last page.
                val edit = EditText(activity).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    hint = activity.getString(R.string.plugin_cards_goto_hint, pageCount)
                    setText("")
                    setPadding(pad, pad, pad, pad)
                }
                val gotoDialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
                    .setTitle(R.string.plugin_cards_goto_page)
                    .setMessage(
                        activity.getString(
                            R.string.plugin_cards_goto_current,
                            currentPage + 1,
                            pageCount,
                        ),
                    )
                    .setView(edit)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val target = PluginCatalogFilter.parseGoToPage(
                            edit.text?.toString(),
                            pageCount,
                        ) ?: return@setPositiveButton
                        goTo(target)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                gotoDialog.setOnShowListener {
                    edit.requestFocus()
                    val imm = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as? android.view.inputmethod.InputMethodManager
                    imm?.showSoftInput(edit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
                gotoDialog.show()
            }
            gotoBtn.setOnClickListener { askGoToPage() }
            pageLabel.setOnClickListener { askGoToPage() }
            filterRow.findViewWithTag<Button>("filter_go")?.setOnClickListener {
                goTo(0, filterEdit.text?.toString().orEmpty())
            }
            clearBtn.setOnClickListener {
                goTo(0, "", PluginCatalogFilter.Status.ALL, PluginCatalogFilter.Kind.ALL)
            }
            filterEdit.setOnEditorActionListener { _, _, _ ->
                goTo(0, filterEdit.text?.toString().orEmpty())
                true
            }
            statusChips.findViewWithTag<Button>("status_all")?.setOnClickListener {
                goTo(0, status = PluginCatalogFilter.Status.ALL)
            }
            statusChips.findViewWithTag<Button>("status_new")?.setOnClickListener {
                goTo(0, status = PluginCatalogFilter.Status.NEW)
            }
            statusChips.findViewWithTag<Button>("status_updates")?.setOnClickListener {
                goTo(0, status = PluginCatalogFilter.Status.UPDATES)
            }
            statusChips.findViewWithTag<Button>("status_installed")?.setOnClickListener {
                goTo(0, status = PluginCatalogFilter.Status.INSTALLED)
            }
        }
        if (activity.canShowDialog()) dialog?.show()
    }

    private data class CatalogSelectCard(val root: View, val checkBox: CheckBox)

    private fun buildCatalogSelectCard(
        activity: Activity,
        item: PluginManifestItem,
        installed: InstalledPlugin?,
        canSelect: Boolean,
        hasUpdate: Boolean,
        gap: Int,
        textColor: Int,
        grayText: Int,
        onToggle: (String, Boolean) -> Unit,
        onUninstall: ((InstalledPlugin) -> Unit)? = null,
    ): CatalogSelectCard {
        val density = activity.resources.displayMetrics.density
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val cardPad = (12 * density).toInt()
        val checkBox = CheckBox(activity).apply {
            tag = item.id
            isEnabled = canSelect
            isChecked = false
            minWidth = 0
            minimumWidth = 0
            setOnCheckedChangeListener { button, checked ->
                if (!button.isEnabled) return@setOnCheckedChangeListener
                onToggle(item.id, checked)
            }
        }

        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(activity).apply {
            text = item.name
            setTextColor(if (canSelect) textColor else grayText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        top.addView(TextView(activity).apply {
            text = PluginHubUi.kindLabel(item)
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
            background = themedKindChipBackground(activity)
        })
        body.addView(top)

        val host = PluginHubUi.siteHost(item.site)
        if (host.isNotBlank()) {
            body.addView(TextView(activity).apply {
                text = host
                setTextColor(grayText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, (4 * density).toInt(), 0, 0)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
        }

        body.addView(TextView(activity).apply {
            text = PluginHubUi.catalogMetaLine(item)
            setTextColor(grayText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, (6 * density).toInt(), 0, 0)
        })

        val status = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        when {
            hasUpdate && installed != null -> {
                status.addView(statusChip(
                    activity,
                    activity.getString(R.string.plugin_update_available, installed.version, item.version),
                    primary,
                ))
            }
            installed != null -> {
                status.addView(statusChip(
                    activity,
                    activity.getString(R.string.plugin_already_installed),
                    grayText,
                ))
            }
        }
        if (installed != null && onUninstall != null) {
            status.addView(Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
                text = activity.getString(R.string.plugin_action_uninstall)
                setTextColor(activity.getColor(R.color.colorPrimaryRed))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                isAllCaps = false
                minHeight = 0
                minimumHeight = 0
                setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
                tag = "uninstall_${item.id}"
                setOnClickListener { onUninstall(installed) }
            })
        }
        if (status.childCount > 0) body.addView(status)

        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(cardPad, cardPad, cardPad, cardPad)
            background = themedCardBackground(activity)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).also { it.bottomMargin = gap }
            alpha = if (canSelect) 1f else 0.72f
            isClickable = canSelect
            isFocusable = canSelect
            if (canSelect) {
                setOnClickListener { checkBox.isChecked = !checkBox.isChecked }
            }
            addView(checkBox)
            addView(body)
        }
        return CatalogSelectCard(row, checkBox)
    }

    private fun statusChip(activity: Activity, label: String, color: Int): TextView {
        val density = activity.resources.displayMetrics.density
        return TextView(activity).apply {
            text = label
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
            background = themedKindChipBackground(activity)
        }
    }

    private fun installSelected(
        activity: Activity,
        repoUrl: String,
        items: List<PluginManifestItem>,
        installedById: Map<String, InstalledPlugin>,
        onChanged: () -> Unit,
        /** Full filtered catalog list so we can reopen the picker after install. */
        catalogItems: List<PluginManifestItem> = items,
        filterQuery: String = "",
        page: Int = 0,
        statusFilter: PluginCatalogFilter.Status = PluginCatalogFilter.Status.ALL,
        kindFilter: PluginCatalogFilter.Kind = PluginCatalogFilter.Kind.ALL,
    ) {
        val scope = activity.pluginScope() ?: return
        val total = items.size.coerceAtLeast(1)
        val status = showGlyphPulseProgress(
            activity = activity,
            title = activity.getString(R.string.plugin_installing),
            total = total,
        )
        scope.launch {
            var ok = 0
            var fail = 0
            var skipped = 0
            var processed = 0
            withContext(Dispatchers.IO) {
                for (item in items) {
                    status.setProgress(processed, total, item.name)
                    runCatching {
                        val before = installedById[item.id] ?: PluginStore.getInstalled(item.id)
                        val isUpdate = before != null && PluginSelectAll.hasCatalogUpdate(before, item)
                        if (before != null && !isUpdate) {
                            skipped++
                            return@runCatching
                        }
                        PluginInstaller.install(
                            activity,
                            item,
                            repoUrl,
                            skipIfInstalled = !isUpdate,
                        )
                        ok++
                    }.onFailure { fail++ }
                    processed++
                    status.setProgress(processed, total, item.name)
                }
            }
            status.dismiss()
            showToast(
                activity,
                if (skipped > 0) {
                    activity.getString(R.string.plugin_install_done_skip, ok, fail, skipped)
                } else {
                    activity.getString(R.string.plugin_install_done, ok, fail)
                },
            )
            onChanged()
            // Stay in the catalog so bulk installs remain visible; hub is one tap away via Close.
            activity.postUi {
                showCards(
                    activity,
                    repoUrl,
                    catalogItems,
                    onChanged,
                    filterQuery,
                    page,
                    linkedSetOf(),
                    statusFilter,
                    kindFilter,
                )
            }
        }
    }

    /** Public entry for Proveedores propios manager → same actions as the plugin hub. */
    fun showOptionsForManage(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        showOptions(activity, plugin, onChanged)
    }

    private fun showOptions(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val actions = mutableListOf<String>()
        val run = mutableListOf<() -> Unit>()

        fun add(label: String, block: () -> Unit) {
            actions += label
            run += block
        }

        add(activity.getString(R.string.plugin_action_update)) {
            updateOne(activity, plugin, onChanged)
        }
        if (plugin.inSearch) {
            add(activity.getString(R.string.plugin_action_edit_css)) {
                editCssMapped(activity, plugin, onChanged)
            }
        } else {
            add(activity.getString(R.string.plugin_action_configure_url)) {
                showAddToSearchChooser(activity, plugin, onChanged)
            }
        }
        add(activity.getString(R.string.plugin_action_edit)) {
            showRename(activity, plugin, onChanged)
        }
        add(activity.getString(R.string.plugin_action_reinstall)) {
            updateOne(activity, plugin, onChanged)
        }
        add(activity.getString(R.string.plugin_action_uninstall)) {
            confirmUninstall(activity, plugin, onChanged)
        }

        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(plugin.localName)
            .setMessage(
                activity.getString(
                    R.string.plugin_options_msg,
                    plugin.lang,
                    plugin.version,
                    if (plugin.hasUpdate) plugin.latestVersion else "—",
                    plugin.site,
                )
            )
            .setItems(actions.toTypedArray()) { _, which ->
                run.getOrNull(which)?.invoke()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    /** Opens the real CSS editor — never reinstalls / re-fills Madara defaults. */
    private fun editCssMapped(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val config = CustomProviders.load().firstOrNull { it.name == plugin.providerName }
        if (config == null) {
            // Metadata says CSS-mapped but storage entry is missing (legacy hash wipe).
            showAddToSearchChooser(activity, plugin, onChanged)
            return
        }
        CustomProviderDialogs.showEditor(activity, config) {
            linkInstalledAfterSave(activity, plugin, onChanged)
        }
    }

    /**
     * One chooser: Madara / Blogger / manual CSS with site URL.
     * Keeps Madara optional — never applied until the user picks it.
     */
    private fun showAddToSearchChooser(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val labels = arrayOf(
            activity.getString(R.string.plugin_action_map_madara),
            activity.getString(R.string.plugin_action_map_blogger),
            activity.getString(R.string.plugin_action_edit_selectors),
        )
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_add_to_search_title)
            .setMessage(
                activity.getString(R.string.plugin_configure_url_hint) +
                    "\n\n" + PluginHubUi.siteHost(plugin).ifBlank { plugin.site },
            )
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> mapToTemplate(activity, plugin, onChanged, madara = true)
                    1 -> mapToTemplate(activity, plugin, onChanged, madara = false)
                    else -> configureWithSiteUrl(activity, plugin, onChanged)
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    /**
     * Manual path: editor with site URL + empty selectors (user fills or taps Madara inside).
     */
    private fun configureWithSiteUrl(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val base = CustomProviderPresets.normalizeBaseUrl(plugin.site)
            ?: plugin.site.trim().trimEnd('/')
        if (base.isBlank()) {
            showToast(activity, activity.getString(R.string.plugin_js_not_in_search))
            return
        }
        val existing = findMappedConfig(plugin)
        if (existing != null) {
            CustomProviderDialogs.showEditor(activity, existing) {
                linkInstalledAfterSave(activity, plugin, onChanged)
            }
            return
        }
        val prefill = if (base.contains("blogspot", ignoreCase = true) ||
            base.contains("blogger", ignoreCase = true)
        ) {
            CustomProviderConfig.bloggerTemplate(plugin.localName, base).copy(
                lang = plugin.langCode,
                iconUrl = plugin.iconUrl,
            )
        } else {
            CustomProviderConfig(
                name = plugin.localName,
                baseUrl = base,
                searchUrl = "/?s={query}",
                resultItem = "",
                resultLink = "a",
                novelTitle = "",
                chapterLink = "",
                content = "",
                lang = plugin.langCode,
                iconUrl = plugin.iconUrl,
            )
        }
        CustomProviderDialogs.showEditor(activity, existing = null, prefill = prefill) {
            linkInstalledAfterSave(activity, plugin, onChanged)
        }
    }

    private fun findMappedConfig(plugin: InstalledPlugin): CustomProviderConfig? {
        val all = CustomProviders.load()
        return all.firstOrNull { it.name.equals(plugin.providerName, ignoreCase = true) }
            ?: all.firstOrNull { it.name.equals(plugin.localName, ignoreCase = true) }
    }

    private fun linkInstalledAfterSave(
        activity: Activity,
        plugin: InstalledPlugin,
        onChanged: () -> Unit,
    ) {
        // Match by explicit names only — never by host/URL. Same-site customs with
        // distinct names must stay independently toggleable in Fuentes.
        val all = CustomProviders.load()
        val saved = all.firstOrNull { it.name.equals(plugin.providerName, ignoreCase = true) }
            ?: all.firstOrNull { it.name.equals(plugin.localName, ignoreCase = true) }
        if (saved != null) {
            val kind = when {
                saved.isBlogger -> "blogger"
                "wp-manga" in saved.searchUrl || "ajax/chapters" in saved.searchUrl ||
                    saved.chaptersViaAjax -> "madara"
                else -> "css"
            }
            PluginStore.saveInstalled(
                plugin.copy(
                    localName = saved.name,
                    providerName = saved.name,
                    cssMapped = true,
                    templateKind = kind,
                ),
            )
            showToast(activity, activity.getString(R.string.plugin_edited))
        }
        onChanged()
        showHub(activity, onChanged)
    }

    /** One-tap Madara/Blogger map using the plugin site URL — no empty-selector trap. */
    private fun mapToTemplate(
        activity: Activity,
        plugin: InstalledPlugin,
        onChanged: () -> Unit,
        madara: Boolean,
    ) {
        val scope = activity.pluginScope() ?: return
        val base = CustomProviderPresets.normalizeBaseUrl(plugin.site)
            ?: plugin.site.trim().trimEnd('/')
        if (base.isBlank()) {
            showToast(activity, activity.getString(R.string.plugin_js_not_in_search))
            return
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                val existing = findMappedConfig(plugin)
                val name = existing?.name?.takeIf { it.isNotBlank() } ?: plugin.localName
                val config = if (madara) {
                    CustomProviderConfig.madaraTemplate(name, base).copy(
                        lang = plugin.langCode,
                        iconUrl = plugin.iconUrl,
                    )
                } else {
                    CustomProviderConfig.bloggerTemplate(name, base).copy(
                        lang = plugin.langCode,
                        iconUrl = plugin.iconUrl,
                    )
                }
                CustomProviders.save(
                    activity,
                    config,
                    replacing = existing?.name ?: plugin.providerName.takeIf { it.isNotBlank() },
                )
                PluginStore.saveInstalled(
                    plugin.copy(
                        localName = config.name,
                        providerName = config.name,
                        cssMapped = true,
                        templateKind = if (madara) "madara" else "blogger",
                    ),
                )
            }
            showToast(activity, activity.getString(R.string.plugin_edited))
            onChanged()
            activity.postUi { showHub(activity, onChanged) }
        }
    }

    private fun showRename(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val edit = EditText(activity).apply {
            setText(plugin.localName)
            setPadding(pad, pad, pad, pad)
        }
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_action_edit)
            .setView(edit)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = edit.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) return@setPositiveButton
                val scope = activity.pluginScope() ?: return@setPositiveButton
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val existingConfig = plugin.providerName.takeIf { it.isNotBlank() }
                            ?.let { prev -> CustomProviders.load().firstOrNull { it.name == prev } }
                        if (existingConfig != null) {
                            val renamed = existingConfig.copy(name = name)
                            CustomProviders.save(activity, renamed, replacing = existingConfig.name)
                            PluginStore.saveInstalled(
                                plugin.copy(localName = name, providerName = name),
                            )
                        } else {
                            // Rename metadata only — do not reinstall / re-template Madara.
                            PluginStore.saveInstalled(plugin.copy(localName = name))
                        }
                    }
                    onChanged()
                    showToast(activity, activity.getString(R.string.plugin_edited))
                    activity.postUi { showHub(activity, onChanged) }
                }
            }
            .show()
    }

    private fun confirmUninstall(
        activity: Activity,
        plugin: InstalledPlugin,
        onChanged: () -> Unit,
        onStay: (() -> Unit)? = null,
    ) {
        if (!activity.canShowDialog()) return
        val stay = onStay ?: { showHub(activity, onChanged) }
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_action_uninstall)
            .setMessage(activity.getString(R.string.plugin_uninstall_confirm, plugin.localName))
            .setNegativeButton(R.string.close) { _, _ ->
                activity.postUi { stay() }
            }
            .setPositiveButton(R.string.plugin_action_uninstall) { _, _ ->
                activity.postUi { uninstallOne(activity, plugin, onChanged, stay) }
            }
            .setOnCancelListener {
                activity.postUi { stay() }
            }
            .show()
    }

    private fun uninstallOne(
        activity: Activity,
        plugin: InstalledPlugin,
        onChanged: () -> Unit,
        onStay: (() -> Unit)? = null,
    ) {
        val scope = activity.pluginScope() ?: return
        val stay = onStay ?: { showHub(activity, onChanged) }
        val status = showStatusDialog(
            activity,
            activity.getString(R.string.plugin_uninstalling),
            activity.getString(R.string.plugin_uninstalling_msg, plugin.localName),
        )
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { PluginInstaller.uninstall(activity, plugin) }
            }
            status.dismissSafely()
            if (result.isSuccess) {
                onChanged()
                showToast(activity, activity.getString(R.string.plugin_uninstalled, plugin.localName))
            } else {
                showToast(
                    activity,
                    activity.getString(
                        R.string.plugin_action_failed,
                        plugin.localName,
                        result.exceptionOrNull()?.message ?: "",
                    ),
                )
            }
            activity.postUi { stay() }
        }
    }

    private fun updateOne(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val scope = activity.pluginScope() ?: return
        val status = showStatusDialog(
            activity,
            activity.getString(R.string.plugin_updating),
            activity.getString(R.string.plugin_updating_msg, plugin.localName),
        )
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val catalog = PluginStore.loadRepos()
                        .firstOrNull { it.url == plugin.repoUrl }
                        ?.let { PluginStore.loadCachedManifest(it.id) }
                        .orEmpty()
                        .ifEmpty {
                            if (plugin.repoUrl.startsWith("http")) {
                                runCatching { PluginManifestClient.fetch(plugin.repoUrl) }
                                    .getOrDefault(emptyList())
                            } else emptyList()
                        }
                    val remote = catalog.firstOrNull { it.id == plugin.id }
                    PluginInstaller.update(activity, plugin, remote)
                }
            }
            status.dismissSafely()
            if (result.isSuccess) {
                onChanged()
                showToast(activity, activity.getString(R.string.plugin_updated, plugin.localName))
            } else {
                showToast(
                    activity,
                    activity.getString(
                        R.string.plugin_action_failed,
                        plugin.localName,
                        result.exceptionOrNull()?.message ?: "",
                    ),
                )
            }
            activity.postUi { showHub(activity, onChanged) }
        }
    }

    private fun updateAll(activity: Activity, onChanged: () -> Unit) {
        val scope = activity.pluginScope() ?: return
        val pending = PluginStore.loadInstalled().filter { it.hasUpdate }
        if (pending.isEmpty()) return
        val total = pending.size.coerceAtLeast(1)
        val status = showGlyphPulseProgress(
            activity = activity,
            title = activity.getString(R.string.plugin_updating),
            total = total,
        )
        scope.launch {
            var processed = 0
            withContext(Dispatchers.IO) {
                for (plugin in pending) {
                    status.setProgress(processed, total, plugin.localName)
                    runCatching { PluginInstaller.update(activity, plugin) }
                    processed++
                    status.setProgress(processed, total, plugin.localName)
                }
            }
            status.dismiss()
            onChanged()
            showToast(activity, activity.getString(R.string.plugin_update_all_done, pending.size))
            activity.postUi { showHub(activity, onChanged) }
        }
    }

    /**
     * Bulk install/update progress: reader 本 glyph pulse + fraction + bar + current name (mock D).
     */
    private class GlyphPulseProgress(
        private val activity: Activity,
        private val dialog: AlertDialog,
        private val glyphWrap: View,
        private val fractionView: TextView,
        private val bar: ProgressBar,
        private val nameView: TextView,
    ) {
        fun setProgress(done: Int, total: Int, currentName: String) {
            activity.postUi {
                val safeTotal = total.coerceAtLeast(1)
                val clamped = done.coerceIn(0, safeTotal)
                fractionView.text =
                    activity.getString(R.string.plugin_installing_fraction, clamped, safeTotal)
                bar.max = safeTotal
                bar.isIndeterminate = false
                bar.progress = clamped
                nameView.text = currentName
                nameView.visibility =
                    if (currentName.isBlank()) View.GONE else View.VISIBLE
            }
        }

        fun dismiss() {
            activity.postUi {
                glyphWrap.clearAnimation()
                dialog.dismissSafely()
            }
        }
    }

    private fun showGlyphPulseProgress(
        activity: Activity,
        title: String,
        total: Int,
    ): GlyphPulseProgress {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val text = activity.colorFromAttribute(R.attr.textColor)
        val muted = activity.colorFromAttribute(R.attr.grayTextColor)

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val glyphSize = dp(96)
        val glyphWrap = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(glyphSize, glyphSize)
        }
        glyphWrap.addView(View(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            )
            alpha = 0.12f
            scaleX = 1.18f
            scaleY = 1.18f
            background = activity.getDrawable(R.drawable.bg_reader_loading_glyph_halo)
            backgroundTintList =
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 55))
        })
        glyphWrap.addView(View(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            background = activity.getDrawable(R.drawable.bg_reader_loading_glyph_ring)
            backgroundTintList = ColorStateList.valueOf(primary)
        })
        glyphWrap.addView(TextView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            gravity = Gravity.CENTER
            includeFontPadding = false
            setText(R.string.loading_glyph)
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            setTypeface(typeface, Typeface.BOLD)
        })
        column.addView(glyphWrap)

        column.addView(TextView(activity).apply {
            this.text = title
            setTextColor(text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        })

        val fractionView = TextView(activity).apply {
            this.text = activity.getString(R.string.plugin_installing_fraction, 0, total.coerceAtLeast(1))
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        column.addView(fractionView)

        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(6),
            ).apply {
                topMargin = dp(12)
            }
            max = total.coerceAtLeast(1)
            progress = 0
            isIndeterminate = false
            progressTintList = ColorStateList.valueOf(primary)
            progressBackgroundTintList =
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 48))
        }
        column.addView(bar)

        val nameView = TextView(activity).apply {
            setTextColor(muted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(10), 0, 0)
            visibility = View.GONE
        }
        column.addView(nameView)

        column.addView(TextView(activity).apply {
            this.text = activity.getString(R.string.plugin_installing_hint)
            setTextColor(ColorUtils.setAlphaComponent(muted, 180))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setView(column)
            .setCancelable(false)
            .create()
            .also {
                if (activity.canShowDialog()) {
                    it.show()
                    glyphWrap.startAnimation(
                        AnimationUtils.loadAnimation(activity, R.anim.reader_glyph_pulse),
                    )
                }
            }

        return GlyphPulseProgress(activity, dialog, glyphWrap, fractionView, bar, nameView)
    }

    private fun showStatusDialog(activity: Activity, title: String, message: String): AlertDialog {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val text = activity.colorFromAttribute(R.attr.textColor)

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val glyphSize = dp(72)
        val glyphWrap = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(glyphSize, glyphSize)
            tag = "glyph_wrap"
        }
        glyphWrap.addView(View(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            )
            alpha = 0.12f
            scaleX = 1.18f
            scaleY = 1.18f
            background = activity.getDrawable(R.drawable.bg_reader_loading_glyph_halo)
            backgroundTintList =
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 55))
        })
        glyphWrap.addView(View(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            background = activity.getDrawable(R.drawable.bg_reader_loading_glyph_ring)
            backgroundTintList = ColorStateList.valueOf(primary)
        })
        glyphWrap.addView(TextView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            gravity = Gravity.CENTER
            includeFontPadding = false
            setText(R.string.loading_glyph)
            setTextColor(primary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            setTypeface(typeface, Typeface.BOLD)
        })
        column.addView(glyphWrap)

        column.addView(TextView(activity).apply {
            this.text = title
            setTextColor(text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        })
        column.addView(TextView(activity).apply {
            this.text = message
            setTextColor(activity.colorFromAttribute(R.attr.grayTextColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        return AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setView(column)
            .setCancelable(false)
            .create()
            .also {
                if (activity.canShowDialog()) {
                    it.show()
                    glyphWrap.startAnimation(
                        AnimationUtils.loadAnimation(activity, R.anim.reader_glyph_pulse),
                    )
                    it.setOnDismissListener { glyphWrap.clearAnimation() }
                }
            }
    }

    private fun Activity.canShowDialog(): Boolean {
        if (isFinishing) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed) return false
        return true
    }

    private fun Activity.postUi(block: () -> Unit) {
        runOnUiThread {
            if (!canShowDialog()) return@runOnUiThread
            window?.decorView?.post {
                if (canShowDialog()) block()
            } ?: block()
        }
    }

    private fun Activity.pluginScope(): CoroutineScope? {
        (this as? AppCompatActivity)?.lifecycleScope?.let { return it }
        return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    private fun AlertDialog.dismissSafely() {
        runCatching {
            if (isShowing) dismiss()
        }
    }
}
