package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.TranslationHistory
import com.lagradost.quicknovel.TranslationHistoryEntry
import com.lagradost.quicknovel.util.UIHelper.colorFromAttribute
import com.lagradost.quicknovel.util.UIHelper.getResourceColor
import java.util.Date

/** Lists finished background translations (Sections → Historial de traducciones). Theme-aware cards. */
object TranslationHistoryDialogs {
    fun show(activity: Activity) {
        val density = activity.resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val gap = (10 * density).toInt()
        val entries = TranslationHistory.available(activity)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val grayText = activity.colorFromAttribute(R.attr.grayTextColor)
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad)
        }
        var dialog: AlertDialog? = null
        if (entries.isEmpty()) {
            column.addView(TextView(activity).apply {
                text = activity.getString(R.string.translation_history_empty)
                setTextColor(grayText)
            })
        } else {
            entries.forEach { entry ->
                column.addView(buildRow(activity, entry, gap, textColor, grayText, primary) {
                    TranslationHistory.forget(entry.uri)
                    showToast(activity, activity.getString(R.string.translation_history_removed))
                    dialog?.dismiss()
                    show(activity)
                })
            }
        }
        val scroll = ScrollView(activity).apply {
            addView(column)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (520 * density).toInt(),
            )
        }
        dialog = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(
                activity.getString(R.string.translation_history_title_count, entries.size),
            )
            .setView(scroll)
            .setNegativeButton(R.string.close, null)
            .create()
        dialog?.show()
    }

    private fun buildRow(
        activity: Activity,
        entry: TranslationHistoryEntry,
        gap: Int,
        textColor: Int,
        grayText: Int,
        primary: Int,
        onDelete: () -> Unit,
    ): LinearLayout {
        val density = activity.resources.displayMetrics.density
        val date = DateFormat.getMediumDateFormat(activity).format(Date(entry.time)) +
            " · " + DateFormat.getTimeFormat(activity).format(Date(entry.time))
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(activity.colorFromAttribute(R.attr.boxItemBackground))
                setStroke(
                    (1 * density).toInt().coerceAtLeast(1),
                    activity.getResourceColor(R.attr.white, 0.16f),
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).also { it.bottomMargin = gap }
        }
        card.addView(TextView(activity).apply {
            text = entry.novelName.ifBlank { entry.fileName }
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })
        card.addView(TextView(activity).apply {
            text = buildString {
                append(entry.fileName)
                if (entry.langCode.isNotBlank()) append(" · ").append(entry.langCode.uppercase())
                append('\n').append(date)
            }
            setTextColor(grayText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, (4 * density).toInt(), 0, (8 * density).toInt())
        })
        val actions = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        actions.addView(actionBtn(activity, activity.getString(R.string.translation_history_open), primary) {
            runCatching { TranslationHistory.open(activity, entry) }
                .onFailure {
                    showToast(activity, activity.getString(R.string.translation_history_open_fail))
                }
        })
        actions.addView(actionBtn(activity, activity.getString(R.string.translation_history_folder), primary) {
            runCatching { TranslationHistory.openFolder(activity, entry) }
                .onFailure {
                    showToast(activity, activity.getString(R.string.translation_history_open_fail))
                }
        })
        actions.addView(
            actionBtn(
                activity,
                activity.getString(R.string.translation_history_delete),
                activity.getColor(R.color.colorPrimaryRed),
                onDelete,
            ),
        )
        card.addView(actions)
        return card
    }

    private fun actionBtn(
        activity: Activity,
        label: String,
        color: Int,
        onClick: () -> Unit,
    ): Button = Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
        text = label
        setTextColor(color)
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        minHeight = 0
        minimumHeight = 0
        setOnClickListener { onClick() }
    }
}
