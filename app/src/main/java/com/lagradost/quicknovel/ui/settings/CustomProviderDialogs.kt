package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.content.DialogInterface
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.providers.BloggerValidation
import com.lagradost.quicknovel.providers.ConfigProblem
import com.lagradost.quicknovel.providers.CustomProvider
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.providers.CustomProviderParser
import com.lagradost.quicknovel.providers.CustomProviderPresets
import com.lagradost.quicknovel.providers.ProviderTestReport
import com.lagradost.quicknovel.providers.TestStep
import com.lagradost.quicknovel.util.CustomProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Dialogs to add, edit, test and remove providers made of CSS selectors. */
object CustomProviderDialogs {
    private const val DEFAULT_TEST_QUERY = "the"
    private const val DEFAULT_BLOGGER_TEST_QUERY = "a"

    private class Field(
        @StringRes val hint: Int,
        val get: (CustomProviderConfig) -> String,
        val inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
    )

    private val fields = linkedMapOf(
        "name" to Field(R.string.cp_name, { it.name }),
        "url" to Field(R.string.cp_url, { it.baseUrl }, InputType.TYPE_TEXT_VARIATION_URI),
        "lang" to Field(R.string.cp_lang, { it.lang }),
        "searchUrl" to Field(R.string.cp_search_url, { it.searchUrl }),
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

    /** Lists the custom providers, with a button to add another one. */
    fun showManager(activity: Activity, onChanged: () -> Unit) {
        val providers = CustomProviders.load()
        val builder = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.cp_manage)
            .setPositiveButton(R.string.cp_add) { _, _ ->
                showSimpleAdd(activity) {
                    onChanged()
                    showManager(activity, onChanged)
                }
            }
            .setNegativeButton(R.string.close, null)
        if (providers.isEmpty()) {
            builder.setMessage(R.string.cp_none)
        } else {
            builder.setItems(providers.map { it.name }.toTypedArray()) { _, which ->
                showOptions(activity, providers[which], onChanged)
            }
        }
        builder.show()
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
            hint.text = activity.getString(R.string.cp_validating_blog)
            scope.launch {
                val config = CustomProviderConfig.bloggerTemplate(name, url).copy(lang = lang)
                val check = withContext(Dispatchers.IO) { CustomProvider(config).validateBlogger() }
                if (check.ok) {
                    hint.text = formatBlogOk(activity, check)
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
            hint.text = activity.getString(R.string.cp_detecting)
            scope.launch {
                val detection = withContext(Dispatchers.IO) {
                    detect(CustomProviderPresets.candidates(name, url, lang, CustomProviderConfig.KIND_CSS))
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
                    hint.text = activity.getString(R.string.cp_not_detected) + reason
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

        fun read() = CustomProviderConfig(
            name = text("name"),
            baseUrl = text("url"),
            lang = text("lang").lowercase().ifBlank { "en" },
            searchUrl = text("searchUrl"),
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
            } else {
                CustomProviders.save(activity, config, replacing = existing?.name)
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
