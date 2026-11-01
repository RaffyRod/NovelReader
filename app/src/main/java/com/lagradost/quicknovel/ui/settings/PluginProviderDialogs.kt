package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
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
import com.lagradost.quicknovel.plugins.PluginHubUi
import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginLangs
import com.lagradost.quicknovel.plugins.PluginManifestClient
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginRepo
import com.lagradost.quicknovel.plugins.PluginSelectAll
import com.lagradost.quicknovel.plugins.PluginStore
import com.lagradost.quicknovel.util.UIHelper.colorFromAttribute
import com.lagradost.quicknovel.util.UIHelper.getResourceColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LNReader-style plugin flow: manage installed → add repo/fetch → languages → cards → install.
 * JSON is the catalog; install downloads JS and maps Madara/Blogger templates only when tagged.
 */
object PluginProviderDialogs {

    fun showHub(activity: Activity, onChanged: () -> Unit) {
        PluginStore.ensureOfficialRepo()
        val installed = PluginStore.loadInstalled()
        val updates = installed.count { it.hasUpdate }
        val density = activity.resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val gap = (10 * density).toInt()

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad)
        }

        val scroll = ScrollView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (480 * density).toInt(),
            )
        }

        val builder = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_hub_title)
            .setView(scroll)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_browse_official) { _, _ ->
                openOfficialCatalog(activity, onChanged)
            }
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

        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)

        if (installed.isEmpty()) {
            column.addView(TextView(activity).apply {
                text = activity.getString(R.string.plugin_none_simple)
                setTextColor(grayText)
                setPadding(0, 0, 0, gap)
            })
        } else {
            installed.forEach { plugin ->
                column.addView(buildPluginCard(activity, plugin, gap, textColor, grayText) { action ->
                    when (action) {
                        CardAction.Update -> {
                            // Dismiss first, then run on next frame so the window token stays valid.
                            dialog.dismiss()
                            activity.postUi { updateOne(activity, plugin, onChanged) }
                        }
                        CardAction.Remove -> {
                            dialog.dismiss()
                            activity.postUi { confirmUninstall(activity, plugin, onChanged) }
                        }
                        CardAction.More -> showOptions(activity, plugin, onChanged)
                    }
                })
            }
        }
        scroll.addView(column)
        if (activity.canShowDialog()) dialog.show()
    }

    private enum class CardAction { Update, Remove, More }

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
            text = PluginHubUi.metaLine(plugin)
            setTextColor(grayText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, (6 * density).toInt(), 0, (8 * density).toInt())
        })

        val actions = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
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
        val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return
        val progress = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_browse_official)
            .setMessage(R.string.plugin_fetching)
            .setCancelable(false)
            .create()
        progress.show()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val url = PluginStore.OFFICIAL_MANIFEST_URL
                    val items = PluginManifestClient.fetch(url)
                    url to items
                }
            }
            progress.dismiss()
            result.fold(
                onSuccess = { (url, items) ->
                    if (items.isEmpty()) {
                        showToast(activity, activity.getString(R.string.plugin_fetch_empty))
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
                    onChanged()
                    showLanguages(activity, repoId, url, items, onChanged)
                },
                onFailure = {
                    showToast(
                        activity,
                        activity.getString(R.string.plugin_fetch_fail, it.message ?: ""),
                    )
                },
            )
        }
    }

    fun showAddRepo(activity: Activity, onChanged: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_repo_hint)
            setPadding(0, 0, 0, (8 * density).toInt())
        })
        val urlEdit = EditText(activity).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            hint = activity.getString(R.string.plugin_repo_url_hint)
            setText(PluginStore.OFFICIAL_MANIFEST_URL)
        }
        column.addView(urlEdit)
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

        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_add_repo)
            .setView(column)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_fetch, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return@setOnClickListener
                progress.visibility = View.VISIBLE
                status.text = activity.getString(R.string.plugin_fetching)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
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
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
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
                            status.text = activity.getString(
                                R.string.plugin_fetch_ok,
                                items.size,
                                PluginManifestClient.languageCounts(items).size,
                            )
                            dialog.dismiss()
                            showLanguages(activity, repoId, repoUrl, items, onChanged)
                            onChanged()
                        },
                        onFailure = {
                            status.text = activity.getString(R.string.plugin_fetch_fail, it.message ?: "")
                        },
                    )
                }
            }
        }
        dialog.show()
    }

    private fun showLanguages(
        activity: Activity,
        repoId: String,
        repoUrl: String,
        items: List<PluginManifestItem>,
        onChanged: () -> Unit,
    ) {
        val density = activity.resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val counts = PluginManifestClient.languageCounts(items)
        val ordered = PluginLangs.ordered(counts)
        val selected = linkedSetOf<String>().apply {
            PluginLangs.PRIORITY_LABELS.filter { counts.containsKey(it) }.forEach { add(it) }
            if (isEmpty() && ordered.isNotEmpty()) add(ordered.first().first)
        }

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_langs_hint)
            setTextColor(grayText)
            setPadding(0, 0, 0, (8 * density).toInt())
        })
        val boxes = ordered.map { (lang, count) ->
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
            }
            val box = CheckBox(activity).apply {
                text = if (lang in PluginLangs.PRIORITY_LABELS) {
                    activity.getString(R.string.plugin_lang_priority, lang, count)
                } else {
                    "$lang ($count)"
                }
                setTextColor(textColor)
                isChecked = lang in selected
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(lang) else selected.remove(lang)
                }
            }
            row.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            column.addView(row)
            box
        }

        val scroll = ScrollView(activity).apply { addView(column) }
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_langs_title)
            .setView(scroll)
            .setNeutralButton(R.string.plugin_langs_all) { d, _ ->
                boxes.forEach { it.isChecked = true }
                // keep dialog open: recreate
                d.dismiss()
                selected.clear()
                ordered.forEach { selected.add(it.first) }
                showLanguages(activity, repoId, repoUrl, items, onChanged)
            }
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_langs_continue) { _, _ ->
                if (selected.isEmpty()) {
                    showToast(activity, activity.getString(R.string.plugin_langs_pick_one))
                    showLanguages(activity, repoId, repoUrl, items, onChanged)
                    return@setPositiveButton
                }
                val filtered = items.filter { it.displayLang in selected }
                    .sortedBy { it.name.lowercase() }
                showCards(activity, repoUrl, filtered, onChanged)
            }
            .show()
    }

    private fun showCards(
        activity: Activity,
        repoUrl: String,
        items: List<PluginManifestItem>,
        onChanged: () -> Unit,
    ) {
        val density = activity.resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val gap = (10 * density).toInt()
        val installedById = PluginStore.loadInstalled().associateBy { it.id }
        val selected = linkedSetOf<String>()
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad)
        }
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val selectable = PluginSelectAll.selectableIds(items, installedById)
        val updateCount = items.count {
            val inst = installedById[it.id] ?: return@count false
            PluginSelectAll.hasCatalogUpdate(inst, it)
        }
        val newCount = items.count { it.id !in installedById }
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_cards_hint, items.size, newCount, updateCount)
            setTextColor(grayText)
            setPadding(0, 0, 0, gap)
        })

        val checkBoxes = LinkedHashMap<String, CheckBox>()
        items.forEach { item ->
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
            )
            checkBoxes[item.id] = built.checkBox
            column.addView(built.root)
        }

        fun applySelectAll() {
            val ids = PluginSelectAll.selectableIds(items, installedById)
            selected.clear()
            selected.addAll(ids)
            checkBoxes.forEach { (id, box) ->
                if (!box.isEnabled) return@forEach
                box.isChecked = id in ids
            }
        }

        val scroll = ScrollView(activity).apply {
            addView(column)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (480 * density).toInt(),
            )
        }
        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_cards_title)
            .setView(scroll)
            .setNeutralButton(R.string.plugin_select_all, null)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_install_or_update, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                applySelectAll()
                showToast(
                    activity,
                    activity.getString(R.string.plugin_select_all_applied, selectable.size),
                )
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val chosen = PluginSelectAll.filterSelectable(items, selected, installedById)
                if (chosen.isEmpty()) {
                    showToast(activity, activity.getString(R.string.plugin_pick_one))
                    return@setOnClickListener
                }
                dialog.dismiss()
                installSelected(activity, repoUrl, chosen, installedById, onChanged)
            }
        }
        if (activity.canShowDialog()) dialog.show()
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
    ) {
        val scope = activity.pluginScope() ?: return
        val status = showStatusDialog(
            activity,
            activity.getString(R.string.plugin_installing),
            activity.getString(R.string.plugin_installing_msg, items.size),
        )
        scope.launch {
            var ok = 0
            var fail = 0
            var skipped = 0
            withContext(Dispatchers.IO) {
                for (item in items) {
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
                }
            }
            status.dismissSafely()
            showToast(
                activity,
                if (skipped > 0) {
                    activity.getString(R.string.plugin_install_done_skip, ok, fail, skipped)
                } else {
                    activity.getString(R.string.plugin_install_done, ok, fail)
                },
            )
            onChanged()
            activity.postUi { showHub(activity, onChanged) }
        }
    }

    private fun showOptions(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val actions = listOf(
            activity.getString(R.string.plugin_action_update),
            activity.getString(R.string.plugin_action_edit),
            activity.getString(R.string.plugin_action_reinstall),
            activity.getString(R.string.plugin_action_uninstall),
        )
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
                when (which) {
                    0 -> updateOne(activity, plugin, onChanged)
                    1 -> showEdit(activity, plugin, onChanged)
                    2 -> updateOne(activity, plugin, onChanged)
                    3 -> confirmUninstall(activity, plugin, onChanged)
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showEdit(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
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
                val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return@setPositiveButton
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val item = PluginManifestItem(
                            id = plugin.id,
                            name = plugin.name,
                            site = plugin.site,
                            lang = plugin.lang,
                            version = plugin.version,
                            url = plugin.jsUrl,
                            iconUrl = plugin.iconUrl,
                        )
                        PluginInstaller.install(activity, item, plugin.repoUrl, localName = name)
                    }
                    onChanged()
                    showToast(activity, activity.getString(R.string.plugin_edited))
                    showHub(activity, onChanged)
                }
            }
            .show()
    }

    private fun confirmUninstall(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        if (!activity.canShowDialog()) return
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_action_uninstall)
            .setMessage(activity.getString(R.string.plugin_uninstall_confirm, plugin.localName))
            .setNegativeButton(R.string.close) { _, _ ->
                activity.postUi { showHub(activity, onChanged) }
            }
            .setPositiveButton(R.string.plugin_action_uninstall) { _, _ ->
                activity.postUi { uninstallOne(activity, plugin, onChanged) }
            }
            .setOnCancelListener {
                activity.postUi { showHub(activity, onChanged) }
            }
            .show()
    }

    private fun uninstallOne(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val scope = activity.pluginScope() ?: return
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
            activity.postUi { showHub(activity, onChanged) }
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
        val status = showStatusDialog(
            activity,
            activity.getString(R.string.plugin_updating),
            activity.getString(R.string.plugin_updating_all_msg, pending.size),
        )
        scope.launch {
            withContext(Dispatchers.IO) {
                for (plugin in pending) {
                    runCatching { PluginInstaller.update(activity, plugin) }
                }
            }
            status.dismissSafely()
            onChanged()
            showToast(activity, activity.getString(R.string.plugin_update_all_done, pending.size))
            activity.postUi { showHub(activity, onChanged) }
        }
    }

    private fun showStatusDialog(activity: Activity, title: String, message: String): AlertDialog {
        val density = activity.resources.displayMetrics.density
        val pad = (20 * density).toInt()
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad / 2)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        column.addView(ProgressBar(activity).apply {
            isIndeterminate = true
        })
        column.addView(TextView(activity).apply {
            text = message
            setTextColor(activity.colorFromAttribute(R.attr.textColor))
            setPadding(0, (14 * density).toInt(), 0, 0)
            gravity = Gravity.CENTER
        })
        return AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(title)
            .setView(column)
            .setCancelable(false)
            .create()
            .also { if (activity.canShowDialog()) it.show() }
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
