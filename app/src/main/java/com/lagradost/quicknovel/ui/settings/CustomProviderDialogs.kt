package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.content.DialogInterface
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.R
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
     * Easy form: only a name and the web address. The layout of the site is detected by
     * trying the known layouts; the advanced form stays available for sites that are not detected.
     */
    fun showSimpleAdd(activity: Activity, onSaved: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }
        fun addInput(hint: Int, value: String = "", inputType: Int = InputType.TYPE_CLASS_TEXT): TextInputEditText {
            val layout = TextInputLayout(activity).apply { this.hint = activity.getString(hint) }
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

        val dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.cp_add)
            .setMessage(R.string.cp_simple_hint)
            .setView(ScrollView(activity).apply { addView(column) })
            .setPositiveButton(R.string.cp_add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()

        fun name() = nameEdit.text?.toString().orEmpty().trim()
        fun lang() = langEdit.text?.toString().orEmpty().trim().lowercase().ifBlank { "es" }
        fun baseUrl() = CustomProviderPresets.normalizeBaseUrl(urlEdit.text?.toString().orEmpty())

        val add = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        add.setOnClickListener {
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
            add.isEnabled = false
            dialog.setMessage(activity.getString(R.string.cp_detecting))
            scope.launch {
                val found = withContext(Dispatchers.IO) { detect(CustomProviderPresets.candidates(name, url, lang)) }
                if (found != null) {
                    CustomProviders.save(activity, found)
                    showToast(R.string.cp_saved)
                    dialog.dismiss()
                    onSaved()
                } else {
                    add.isEnabled = true
                    dialog.setMessage(activity.getString(R.string.cp_not_detected))
                }
            }
        }
    }

    /** First layout whose search, novel page and chapter text all work on the site. */
    private suspend fun detect(candidates: List<CustomProviderConfig>): CustomProviderConfig? {
        for (config in candidates) {
            val provider = CustomProvider(config)
            for (query in CustomProviderPresets.testQueries) {
                val report = provider.runTest(query)
                if (report.success) return config
                if (report.failedStep != TestStep.Search) break
            }
        }
        return null
    }

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
                CustomProviders.remove(config.name)
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

        val madara = MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            .apply { setText(R.string.cp_preset_madara) }
        column.addView(madara, matchWidth())

        fun addField(key: String, field: Field, value: String) {
            val layout = TextInputLayout(activity).apply { hint = activity.getString(field.hint) }
            val edit = TextInputEditText(layout.context).apply {
                inputType = field.inputType
                setText(value)
                setSingleLine()
            }
            layout.addView(edit)
            inputs[key] = edit
            column.addView(layout, matchWidth(top = (8 * density).toInt()))
        }
        fields.forEach { (key, field) -> addField(key, field, field.get(initial)) }

        val newestFirst = CheckBox(activity).apply {
            setText(R.string.cp_newest_first)
            isChecked = initial.chaptersNewestFirst
        }
        val ajax = CheckBox(activity).apply {
            setText(R.string.cp_ajax)
            isChecked = initial.chaptersViaAjax
        }
        column.addView(newestFirst, matchWidth(top = (8 * density).toInt()))
        column.addView(ajax, matchWidth())

        val testQuery = TextInputLayout(activity).apply { hint = activity.getString(R.string.cp_test_query) }
        val testQueryEdit = TextInputEditText(testQuery.context).apply {
            setText(DEFAULT_TEST_QUERY)
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
                val query = testQueryEdit.text?.toString().orEmpty().ifBlank { DEFAULT_TEST_QUERY }
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
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = top }
}
