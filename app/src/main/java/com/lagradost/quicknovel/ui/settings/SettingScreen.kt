package com.lagradost.quicknovel.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.lagradost.quicknovel.GeminiProtocol
import com.lagradost.quicknovel.GeminiTranslator
import com.mihon.presentation.settings.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.lagradost.cloudstream4.compose.BlackButton
import com.lagradost.cloudstream4.compose.WhiteButton
import com.lagradost.cloudstream4.compose.circle
import com.lagradost.cloudstream4.compose.rounded
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.theme.CloudStreamPreviewTheme
import com.lagradost.cloudstream4.theme.perfToColor
import com.lagradost.cloudstream4.viewmodels.GithubAction
import com.lagradost.quicknovel.BuildConfig
import com.lagradost.quicknovel.CommonActivity.activity
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.ErrorLoadingException
import com.lagradost.quicknovel.FileHelper
import com.lagradost.quicknovel.FileStorage
import com.lagradost.quicknovel.NewChaptersWorker
import com.lagradost.quicknovel.update.MainActivityScreen
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.util.ProviderLanguages
import com.lagradost.quicknovel.mvvm.logError
import com.lagradost.quicknovel.ui.txt
import com.lagradost.quicknovel.util.Apis.Companion.apis
import com.lagradost.quicknovel.plugins.PluginStore
import com.lagradost.quicknovel.util.BackupUtils
import com.lagradost.quicknovel.util.SubtitleHelper
import com.lagradost.quicknovel.util.UIHelper.clipboardHelper
import com.mihon.common.preference.AndroidPreferenceStore
import com.mihon.presentation.settings.Preference
import com.mihon.presentation.settings.SearchableSettings
import com.mihon.presentation.settings.widget.TextPreferenceWidget
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SettingScreen : SearchableSettings {
    @Composable
    override fun getTitleRes(): String = stringResource(R.string.title_settings)

    @Composable
    fun getFilePreference(
        storage: FileStorage,
        store: AndroidPreferenceStore,
        name: String,
    ): Preference.PreferenceItem.ListPreference<String> {
        val context = LocalContext.current
        val epubPathStore = storage.toPreference(context, store)

        val pathPicker =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                // It lies, it can be null if file manager quits.
                if (uri == null) return@rememberLauncherForActivityResult
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (t: Throwable) {
                    // remote shit can throw
                    logError(t)
                }
                storage.setLocation(context, uri)
            }
        return Preference.PreferenceItem.ListPreference(
            title = name,
            icon = painterResource(R.drawable.folder_24px),
            preference = epubPathStore,
            subtitleProvider = { _, _ ->
                storage.getVisualLocation(context)
            },
            entries =
                persistentMapOf(
                    storage.defaultLocation to storage.defaultLocation,
                    "Custom" to stringResource(R.string.select_location)
                ).toPersistentMap(),
            onValueChanged = { value ->
                when (value) {
                    "Custom" -> {
                        try {
                            pathPicker.launch(Uri.EMPTY)
                        } catch (t: Throwable) {
                            logError(t)
                            showToast(t.toString())
                        }
                    }

                    else -> storage.setLocation(context, null)
                }
                return@ListPreference false
            }
        )
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val store = AndroidPreferenceStore(context)
        val scope = rememberCoroutineScope()
        val settings = rememberAppSettings()
        val geminiKey by settings.translation.geminiApiKey.collectAsState()
        var customProviderCount by remember {
            mutableStateOf(com.lagradost.quicknovel.util.ProviderManage.count())
        }
        var pluginInstalledCount by remember {
            mutableStateOf(PluginStore.loadInstalled().size)
        }
        var pluginUpdateCount by remember {
            mutableStateOf(PluginStore.loadInstalled().count { it.hasUpdate })
        }
        val githubViewModel = MainActivityScreen.githubViewModel()
        val logcat =
            getFilePreference(FileHelper.logcat, store, stringResource(R.string.log_cat_location))
        val backup =
            getFilePreference(FileHelper.backup, store, stringResource(R.string.backup_location))
        val downloads =
            getFilePreference(FileHelper.epub, store, stringResource(R.string.download_path_pref))

        val restoreFileSelector =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    BackupUtils.restoreFromUri(context, uri).onFailure { error ->
                        logError(error)
                        showToast(
                            txt(R.string.restore_failed_format, error.toString())
                        )
                    }.onSuccess {
                        showToast(
                            R.string.restore_success,
                            Toast.LENGTH_LONG
                        )
                        activity?.recreate()
                    }
                }
            }

        return persistentListOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.general_settings),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.language_korean_latin_24px),
                        title = stringResource(R.string.locale_settings),
                        preference = settings.ui.locale,
                        entries = arrayListOf(
                            /* begin language list */
                            ("en" to "English"),
                            ("tr" to "Türkçe"),
                            ("es" to "Español"),
                            ("ru" to "Русский"),
                            /* end language list */
                        ).sortedBy { it.second.lowercase() }.map { (code, name) ->
                            val flag = SubtitleHelper.getFlagFromIso(code) ?: "🌐"
                            code to "$flag $name"
                        }.associate { it }.toPersistentMap(),
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        icon = painterResource(R.drawable.plugin_lang),
                        title = stringResource(R.string.provider_lang_settings),
                        preference = settings.provider.searchLangList,
                        entries = apis.filter { it.lang != ProviderLanguages.ENGLISH }.map { api ->
                            val lang = api.lang
                            val langName = SubtitleHelper.fromTwoLettersToLanguage(lang)!!
                            lang to langName
                        }.sortedBy { it.second.lowercase() }.map { (code, name) ->
                            val flag = SubtitleHelper.getFlagFromIso(code) ?: "🌐"
                            code to "$flag $name"
                        }.associate { it }.toPersistentMap(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        icon = painterResource(R.drawable.plugin_lang),
                        title = stringResource(R.string.show_english_providers),
                        subtitle = stringResource(R.string.show_english_providers_desc),
                        preference = settings.provider.showEnglishProviders,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        icon = painterResource(R.drawable.search_icon),
                        title = stringResource(R.string.allow_special_search_chars),
                        subtitle = stringResource(R.string.allow_special_search_chars_desc),
                        preference = settings.provider.allowSpecialSearchChars,
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        icon = painterResource(R.drawable.extension_24px),
                        title = stringResource(R.string.search_providers),
                        preference = settings.provider.searchProvidersList,
                        // distinctBy avoids duplicated names (built-in + custom clash).
                        entries = apis.distinctBy { it.name }
                            .associate { it.name to "${SubtitleHelper.getFlagFromIso(it.lang) ?: "🌐"} ${it.name}" }
                            .toPersistentMap(),
                        subtitleProvider = { v, _ ->
                            stringResource(R.string.active_providers, v.size)
                        }),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.theme),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        icon = painterResource(R.drawable.palette_24px),
                        title = stringResource(R.string.dark_mode_settings),
                        subtitle = stringResource(R.string.dark_mode_settings_desc),
                        preference = settings.ui.darkMode,
                        onValueChanged = { dark ->
                            activity?.let { act ->
                                com.lagradost.quicknovel.ui.AppearanceMode.apply(act, dark)
                            }
                            true
                        },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.colors_24px),
                        title = stringResource(R.string.ui_icon_theme_settings),
                        preference = settings.ui.uiIconTheme,
                        entries = stringArrayResource(R.array.ui_icon_themes_values).zip(
                            stringArrayResource(R.array.ui_icon_themes_names)
                        ).associate { it }.toPersistentMap(),
                        iconProvider = { k, _ ->
                            RoundColor(perfToColor(k).color)
                        },
                        onValueChanged = { value ->
                            // Accent pack only; icon treatment stays on Tema de iconos.
                            activity?.let { act ->
                                com.lagradost.quicknovel.ui.FireThemes.apply(act, value)
                            }
                            true
                        },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.extension_24px),
                        title = stringResource(R.string.icon_style_settings),
                        preference = settings.ui.iconStyle,
                        entries = stringArrayResource(R.array.icon_styles_values).zip(
                            stringArrayResource(R.array.icon_styles_names)
                        ).associate { it }.toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.ic_baseline_collections_bookmark_24),
                        title = stringResource(R.string.nav_style_title),
                        preference = settings.ui.navStyle,
                        entries = persistentMapOf(
                            "A" to stringResource(R.string.nav_style_a),
                            "B" to stringResource(R.string.nav_style_b),
                            "C" to stringResource(R.string.nav_style_c),
                            "D" to stringResource(R.string.nav_style_d),
                        ),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.ic_baseline_checklist_24),
                        title = stringResource(R.string.settings_style_title),
                        preference = settings.ui.settingsStyle,
                        entries = persistentMapOf(
                            "1" to stringResource(R.string.settings_style_1),
                            "2" to stringResource(R.string.settings_style_2),
                            "3" to stringResource(R.string.settings_style_3),
                        ),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.star_24px),
                        title = stringResource(R.string.rating_format),
                        preference = settings.ui.ratingFormat,
                        entries = stringArrayResource(R.array.RatingFormatData).zip(
                            stringArrayResource(R.array.RatingFormat)
                        ).associate { it }.toPersistentMap()
                    ),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.app_settings),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.mobile_arrow_down_24px),
                        title = stringResource(R.string.check_for_update),
                        subtitle = BuildConfig.VERSION_NAME,
                        onClick = {
                            githubViewModel?.onAction(GithubAction.SearchForUpdate)
                        }
                    ),

                    Preference.PreferenceItem.ListPreference(
                        title = stringResource(R.string.apk_installer_settings),
                        subtitle = stringResource(R.string.apk_installer_settings_des),
                        icon = painterResource(R.drawable.mobile_wrench_24px),
                        entries = integerArrayResource(R.array.apk_installer_values).zip(
                            stringArrayResource(R.array.apk_installer_pref)
                        ).toMap(),
                        preference = settings.updates.apkInstaller
                    ),

                    Preference.PreferenceItem.SwitchPreference(
                        icon = painterResource(R.drawable.notifications_active_24px),
                        title = stringResource(R.string.show_app_updates),
                        subtitle = stringResource(R.string.show_app_updates_desc),
                        preference = settings.updates.showAppUpdates,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.save_as_24px),
                        title = stringResource(R.string.backup_settings),
                        subtitle = null,
                        onClick = {
                            scope.launch {
                                BackupUtils.createBackupFile(context, activity)
                                    .onFailure { error ->
                                        logError(error)
                                        if (error.message != null) {
                                            showToast(
                                                txt(
                                                    R.string.backup_failed_error_format,
                                                    error.message
                                                ),
                                                Toast.LENGTH_LONG
                                            )
                                        } else {
                                            showToast(
                                                R.string.backup_failed,
                                                Toast.LENGTH_LONG
                                            )
                                        }
                                    }
                                    .onSuccess { location ->
                                        showToast(
                                            txt(R.string.backup_success, location),
                                            Toast.LENGTH_LONG
                                        )
                                    }
                            }
                        }
                    ),
                    backup,
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.restore_page_24px),
                        title = stringResource(R.string.restore_settings),
                        onClick = {
                            try {
                                restoreFileSelector.launch(
                                    arrayOf(
                                        "text/plain",
                                        "text/str",
                                        "text/x-unknown",
                                        "application/json",
                                        "unknown/unknown",
                                        "content/unknown",
                                    )
                                )
                            } catch (t: Throwable) {
                                logError(t)
                                showToast(t.message)
                            }
                        }
                    ),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.new_chapters_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        icon = painterResource(R.drawable.notifications_active_24px),
                        title = stringResource(R.string.new_chapters_notify),
                        subtitle = stringResource(R.string.new_chapters_notify_desc),
                        preference = settings.updates.notifyNewChapters,
                        onValueChanged = { enabled ->
                            NewChaptersWorker.schedule(
                                context,
                                enabled,
                                settings.updates.chapterCheckHours.get()
                            )
                            true
                        }
                    ),
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.ic_baseline_autorenew_24),
                        title = stringResource(R.string.new_chapters_frequency),
                        preference = settings.updates.chapterCheckHours,
                        entries = listOf(3, 6, 12, 24).associateWith { hours ->
                            stringResource(R.string.new_chapters_every_hours, hours)
                        }.toPersistentMap(),
                        enabled = true,
                        onValueChanged = { hours ->
                            NewChaptersWorker.schedule(
                                context,
                                settings.updates.notifyNewChapters.get(),
                                hours
                            )
                            true
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.mobile_arrow_down_24px),
                        title = stringResource(R.string.new_chapters_check_now),
                        subtitle = stringResource(R.string.new_chapters_check_now_desc),
                        onClick = {
                            showToast(R.string.new_chapters_checking)
                            NewChaptersWorker.checkNow(context)
                        }
                    ),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.translation_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.language_korean_latin_24px),
                        title = stringResource(R.string.translation_get_key),
                        subtitle = stringResource(R.string.translation_get_key_desc),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, GeminiProtocol.KEYS_URL.toUri())
                            )
                        }
                    ),
                    Preference.PreferenceItem.EditTextPreference(
                        preference = settings.translation.geminiApiKey,
                        title = stringResource(R.string.translation_api_key),
                        subtitle = if (geminiKey.isBlank()) {
                            stringResource(R.string.translation_key_not_set)
                        } else {
                            "••••" + geminiKey.takeLast(4)
                        },
                        onValueChanged = { value -> value.length < 200 }
                    ),
                    Preference.PreferenceItem.ListPreference(
                        icon = painterResource(R.drawable.ic_baseline_autorenew_24),
                        title = stringResource(R.string.translation_model),
                        preference = settings.translation.geminiModel,
                        entries = GeminiProtocol.MODELS.associateWith { it }.toPersistentMap(),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.notifications_active_24px),
                        title = stringResource(R.string.translation_test),
                        subtitle = stringResource(R.string.translation_test_desc),
                        onClick = {
                            val key = settings.translation.geminiApiKey.get().trim()
                            if (key.isEmpty()) {
                                showToast(R.string.translation_key_missing)
                            } else {
                                showToast(R.string.translation_testing)
                                scope.launch(Dispatchers.IO) {
                                    val error = GeminiTranslator(
                                        key,
                                        settings.translation.geminiModel.get(),
                                        maxRetries = 1
                                    ).check()
                                    showToast(
                                        error ?: context.getString(R.string.translation_test_ok),
                                        Toast.LENGTH_LONG
                                    )
                                }
                            }
                        }
                    ),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.cp_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.extension_24px),
                        title = stringResource(R.string.cp_add),
                        subtitle = stringResource(R.string.cp_add_desc),
                        onClick = {
                            activity?.let { act ->
                                CustomProviderDialogs.showSimpleAdd(act) {
                                    customProviderCount =
                                        com.lagradost.quicknovel.util.ProviderManage.count()
                                }
                            }
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.plugin_lang),
                        title = stringResource(R.string.cp_manage),
                        subtitle = stringResource(R.string.cp_count_manageable, customProviderCount),
                        onClick = {
                            activity?.let { act ->
                                CustomProviderDialogs.showManager(act) {
                                    customProviderCount =
                                        com.lagradost.quicknovel.util.ProviderManage.count()
                                }
                            }
                        }
                    ),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.plugin_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.extension_24px),
                        title = stringResource(R.string.plugin_hub),
                        subtitle = stringResource(
                            R.string.plugin_hub_count,
                            pluginInstalledCount,
                            if (pluginUpdateCount > 0) {
                                stringResource(R.string.plugin_hub_updates, pluginUpdateCount)
                            } else {
                                ""
                            },
                        ),
                        onClick = {
                            activity?.let { act ->
                                PluginProviderDialogs.showHub(act) {
                                    val installed = PluginStore.loadInstalled()
                                    pluginInstalledCount = installed.size
                                    pluginUpdateCount = installed.count { it.hasUpdate }
                                    customProviderCount =
                                        com.lagradost.quicknovel.util.ProviderManage.count()
                                }
                            }
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.plugin_lang),
                        title = stringResource(R.string.plugin_add_repo),
                        subtitle = stringResource(R.string.plugin_hub_desc),
                        onClick = {
                            activity?.let { act ->
                                PluginProviderDialogs.showAddRepo(act) {
                                    val installed = PluginStore.loadInstalled()
                                    pluginInstalledCount = installed.size
                                    pluginUpdateCount = installed.count { it.hasUpdate }
                                    customProviderCount =
                                        com.lagradost.quicknovel.util.ProviderManage.count()
                                }
                            }
                        }
                    ),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.reader_settings),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        icon = painterResource(R.drawable.ic_baseline_menu_book_24),
                        title = stringResource(R.string.external_reader),
                        subtitle = stringResource(R.string.external_reader_desc),
                        preference = settings.reader.externalReader,
                    ),
                    // Removed as the actual bloat filtering is shit and not very useful,
                    // but can increase the time too much
                    /*Preference.PreferenceItem.SwitchPreference(
                        icon = painterResource(R.drawable.ic_baseline_edit_24),
                        title = stringResource(R.string.remove_bloat),
                        subtitle = stringResource(R.string.remove_bloat_desc),
                        pref = store.getBoolean(
                            stringResource(R.string.remove_external_key),
                            true,
                        ),
                    ),*/
                    downloads,
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.info),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(R.string.show_log_cat),
                        content = {
                            var showDialog by remember { mutableStateOf(false) }
                            TextPreferenceWidget(
                                title = stringResource(R.string.show_log_cat),
                                subtitle = null,
                                icon = painterResource(R.drawable.article_24px),
                                onPreferenceClick = {
                                    showDialog = true
                                },
                            )
                            if (showDialog) {
                                LogcatDialog {
                                    showDialog = false
                                }
                            }
                        }
                    ),
                    logcat,
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.tutorial_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.info_24px),
                        title = stringResource(R.string.tutorial_providers),
                        subtitle = stringResource(R.string.tutorial_providers_desc),
                        onClick = {
                            activity?.let { act ->
                                androidx.appcompat.app.AlertDialog.Builder(act, R.style.AlertDialogCustom)
                                    .setTitle(R.string.tutorial_providers)
                                    .setMessage(R.string.tutorial_providers_body)
                                    .setPositiveButton(R.string.close, null)
                                    .show()
                            }
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.ic_baseline_menu_book_24),
                        title = stringResource(R.string.tutorial_download),
                        subtitle = stringResource(R.string.tutorial_download_desc),
                        onClick = {
                            activity?.let { act ->
                                androidx.appcompat.app.AlertDialog.Builder(act, R.style.AlertDialogCustom)
                                    .setTitle(R.string.tutorial_download)
                                    .setMessage(R.string.tutorial_download_body)
                                    .setPositiveButton(R.string.close, null)
                                    .show()
                            }
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.ic_baseline_volume_up_24),
                        title = stringResource(R.string.tutorial_tts),
                        subtitle = stringResource(R.string.tutorial_tts_desc),
                        onClick = {
                            activity?.let { act ->
                                androidx.appcompat.app.AlertDialog.Builder(act, R.style.AlertDialogCustom)
                                    .setTitle(R.string.tutorial_tts)
                                    .setMessage(R.string.tutorial_tts_body)
                                    .setPositiveButton(R.string.close, null)
                                    .show()
                            }
                        }
                    ),
                )
            ),
            )
    }
}


@Composable
fun LogcatDialog(dismiss: () -> Unit) {

    val list = remember { mutableStateOf(persistentListOf<LogcatItem>()) }
    LaunchedEffect(dismiss) {
        try {
            // https://developer.android.com/studio/command-line/logcat
            val process = Runtime.getRuntime().exec("logcat --binary -d")
            val items = arrayListOf<LogcatItem>()
            LogcatBinaryParser(process.inputStream).use { parser ->
                while (true) {
                    val item = parser.parseItem() ?: break
                    items.add(item)
                }
            }

            list.value = items.toPersistentList()
        } catch (e: Exception) {
            logError(e) // kinda ironic
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = dismiss,
        title = {
            Text(text = stringResource(R.string.log_cat))
        },
        text = {
            LazyColumn {
                items(items = list.value) { item ->
                    LogcatItem(item)
                }
            }
        },
        confirmButton = {
            WhiteButton(
                text = stringResource(R.string.save),
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val date = SimpleDateFormat(
                                    "yyyy_MM_dd_HH_mm",
                                    Locale.getDefault()
                                ).format(
                                    Date(System.currentTimeMillis())
                                )

                                val file = FileHelper.logcat.createFile(context, "logcat_${date}")
                                    ?: throw ErrorLoadingException("Unable to create file")
                                val stream = file.openOutputStream(append = false)
                                    ?: throw ErrorLoadingException("Unable to create stream")

                                stream.bufferedWriter()
                                    .use { writer ->
                                        list.value.forEach {
                                            writer.write(it.toString())
                                            writer.write("\n\n")
                                        }
                                    }
                                dismiss()
                                showToast(
                                    txt(
                                        R.string.logcat_success,
                                        file.absolutePath ?: file.uri.toString()
                                    ),
                                    Toast.LENGTH_LONG
                                )
                            } catch (t: Throwable) {
                                logError(t)
                                showToast(t.message)
                            }
                        }
                    }
                }
            )
            WhiteButton(
                text = stringResource(R.string.copylog),
                onClick = {
                    clipboardHelper(
                        txt("Logcat"),
                        list.value.joinToString(separator = "\n\n") { it.toString() }
                    )
                }
            )
            WhiteButton(
                text = stringResource(R.string.clear),
                onClick = {
                    try {
                        Runtime.getRuntime().exec("logcat -c")
                    } catch (t: Throwable) {
                        logError(t)
                    }
                    dismiss()
                }
            )
        },
        dismissButton = {
            BlackButton(
                text = stringResource(R.string.close),
                onClick = dismiss,
            )
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}


@Composable
fun LogcatItem(item: LogcatItem) {
    val color = when (item.level) {
        LogcatLevel.Fatal -> Color.Magenta
        LogcatLevel.Error -> Color.Red
        LogcatLevel.Warning -> Color.Yellow
        LogcatLevel.Info -> Color.White
        LogcatLevel.Debug -> Color.Green
        LogcatLevel.Verbose -> Color.Gray
        null -> Color.Transparent
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        item.level?.identifier?.let { value ->
            Text(
                value,
                modifier = Modifier
                    .padding(2.dp)
                    .rounded()
                    .background(MaterialTheme.colorScheme.onBackground)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Text(
            item.date.toHumanReadable(),
            modifier = Modifier
                .padding(2.dp)
                .rounded()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            item.tag,
            modifier = Modifier
                .padding(2.dp)
                .rounded()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .clickable(
                onClick = {
                    clipboardHelper(txt("Logcat"), item.toString())
                })
            .rounded()
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .circle()
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            item.message,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
fun RoundColor(color: Color) {
    Box(
        modifier = Modifier
            .padding(start = 15.dp)
            .size(20.dp)
            .border(
                width = 1.5.dp,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onBackground
            )
            .background(color, CircleShape)
    )
}

@PreviewLightDark
@Composable
private fun SettingScreenPreview() {
    CloudStreamPreviewTheme {
        SettingScreen.Content()
    }
}