package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginLangs
import com.lagradost.quicknovel.plugins.PluginManifestClient
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginRepo
import com.lagradost.quicknovel.plugins.PluginSelectAll
import com.lagradost.quicknovel.plugins.PluginStore
import kotlinx.coroutines.Dispatchers
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
        val builder = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_hub_title)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_add_repo) { _, _ ->
                showAddRepo(activity, onChanged)
            }
        if (installed.isEmpty()) {
            builder.setMessage(R.string.plugin_none)
        } else {
            val labels = installed.map { p ->
                buildString {
                    append(p.localName)
                    append("  ·  v")
                    append(p.version)
                    if (p.hasUpdate) append("  ↑").append(p.latestVersion)
                    append("  ·  ")
                    append(p.lang.ifBlank { p.langCode })
                }
            }.toTypedArray()
            builder.setItems(labels) { _, which ->
                showOptions(activity, installed[which], onChanged)
            }
            if (updates > 0) {
                builder.setNeutralButton(activity.getString(R.string.plugin_update_all, updates)) { _, _ ->
                    updateAll(activity, onChanged)
                }
            }
        }
        builder.show()
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
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_langs_hint)
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
        val pad = (16 * density).toInt()
        val installedIds = PluginStore.loadInstalled().map { it.id }.toSet()
        val selected = linkedSetOf<String>()
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        column.addView(TextView(activity).apply {
            text = activity.getString(R.string.plugin_cards_hint, items.size)
            setPadding(0, 0, 0, (8 * density).toInt())
        })
        val boxes = ArrayList<CheckBox>(items.size)
        items.forEach { item ->
            val already = item.id in installedIds
            val box = CheckBox(activity).apply {
                tag = item.id
                text = buildString {
                    append(item.name)
                    append("\n")
                    append(item.displayLang)
                    append(" · v")
                    append(item.version.ifBlank { "—" })
                    if (already) append("  ✓")
                }
                isChecked = false
                setOnCheckedChangeListener { button, checked ->
                    val id = button.tag as? String ?: return@setOnCheckedChangeListener
                    if (checked) selected.add(id) else selected.remove(id)
                }
            }
            boxes.add(box)
            column.addView(box)
        }
        fun applySelectAll() {
            selected.clear()
            selected.addAll(PluginSelectAll.allIds(items))
            boxes.forEach { box ->
                if (!box.isChecked) box.isChecked = true
            }
        }
        val scroll = ScrollView(activity).apply {
            addView(column)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (420 * density).toInt(),
            )
        }
        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_cards_title)
            .setView(scroll)
            .setNeutralButton(R.string.plugin_select_all, null)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_install, null)
            .create()
        dialog.setOnShowListener {
            // Keep the dialog open on Select all / empty Install (no dismiss + reset).
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                applySelectAll()
                showToast(activity, activity.getString(R.string.plugin_select_all_applied))
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (selected.isEmpty()) {
                    showToast(activity, activity.getString(R.string.plugin_pick_one))
                    return@setOnClickListener
                }
                val chosen = items.filter { it.id in selected }
                dialog.dismiss()
                installSelected(activity, repoUrl, chosen, onChanged)
            }
        }
        dialog.show()
    }

    private fun installSelected(
        activity: Activity,
        repoUrl: String,
        items: List<PluginManifestItem>,
        onChanged: () -> Unit,
    ) {
        val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return
        val progress = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_installing)
            .setMessage(activity.getString(R.string.plugin_installing_msg, items.size))
            .setCancelable(false)
            .create()
        progress.show()
        scope.launch {
            var ok = 0
            var fail = 0
            withContext(Dispatchers.IO) {
                for (item in items) {
                    runCatching {
                        PluginInstaller.install(activity, item, repoUrl)
                        ok++
                    }.onFailure { fail++ }
                }
            }
            progress.dismiss()
            showToast(activity, activity.getString(R.string.plugin_install_done, ok, fail))
            onChanged()
            showHub(activity, onChanged)
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
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.plugin_action_uninstall)
            .setMessage(activity.getString(R.string.plugin_uninstall_confirm, plugin.localName))
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.plugin_action_uninstall) { _, _ ->
                PluginInstaller.uninstall(activity, plugin)
                onChanged()
                showToast(activity, activity.getString(R.string.plugin_uninstalled, plugin.localName))
                showHub(activity, onChanged)
            }
            .show()
    }

    private fun updateOne(activity: Activity, plugin: InstalledPlugin, onChanged: () -> Unit) {
        val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return
        scope.launch {
            val catalog = withContext(Dispatchers.IO) {
                val cached = PluginStore.loadRepos()
                    .firstOrNull { it.url == plugin.repoUrl }
                    ?.let { PluginStore.loadCachedManifest(it.id) }
                    .orEmpty()
                if (cached.isNotEmpty()) cached
                else if (plugin.repoUrl.startsWith("http")) {
                    runCatching { PluginManifestClient.fetch(plugin.repoUrl) }.getOrDefault(emptyList())
                } else emptyList()
            }
            val remote = catalog.firstOrNull { it.id == plugin.id }
            withContext(Dispatchers.IO) {
                PluginInstaller.update(activity, plugin, remote)
            }
            onChanged()
            showToast(activity, activity.getString(R.string.plugin_updated, plugin.localName))
            showHub(activity, onChanged)
        }
    }

    private fun updateAll(activity: Activity, onChanged: () -> Unit) {
        val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: return
        val pending = PluginStore.loadInstalled().filter { it.hasUpdate }
        if (pending.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) {
                for (plugin in pending) {
                    runCatching { PluginInstaller.update(activity, plugin) }
                }
            }
            onChanged()
            showToast(activity, activity.getString(R.string.plugin_update_all_done, pending.size))
            showHub(activity, onChanged)
        }
    }
}
