package com.lagradost.quicknovel.ui.settings

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.lagradost.quicknovel.BuildConfig
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.util.UIHelper.colorFromAttribute

object AboutPrivacyDialogs {
    const val ABOUT_SHEET_DESC = "about_sheet"
    const val PRIVACY_SHEET_DESC = "privacy_sheet"
    const val ABOUT_OPEN_PRIVACY_DESC = "about_open_privacy"
    const val ABOUT_CLOSE_DESC = "about_close"
    const val PRIVACY_CLOSE_DESC = "privacy_close"

    fun showAbout(activity: Activity) {
        val density = activity.resources.displayMetrics.density
        val pad = (20 * density).toInt()
        val primary = activity.colorFromAttribute(R.attr.colorPrimary)
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val gray = activity.colorFromAttribute(R.attr.grayTextColor)
        val boxBg = activity.colorFromAttribute(R.attr.boxItemBackground)

        val sheet = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (10 * density).toInt(), pad, pad)
            contentDescription = ABOUT_SHEET_DESC
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    18 * density, 18 * density, 18 * density, 18 * density,
                    0f, 0f, 0f, 0f,
                )
                setColor(boxBg)
            }
        }

        sheet.addView(
            View(activity).apply {
                layoutParams = LinearLayout.LayoutParams((42 * density).toInt(), (4 * density).toInt()).also {
                    it.gravity = android.view.Gravity.CENTER_HORIZONTAL
                    it.bottomMargin = (12 * density).toInt()
                }
                background = GradientDrawable().apply {
                    cornerRadius = 999f
                    setColor(ColorUtils.setAlphaComponent(gray, 90))
                }
            },
        )

        sheet.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.app_name)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = Typeface.DEFAULT_BOLD
            },
        )
        sheet.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.about_tagline)
                setTextColor(gray)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, (4 * density).toInt(), 0, (14 * density).toInt())
            },
        )

        fun metaRow(label: String, value: String) {
            sheet.addView(
                LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
                    addView(
                        TextView(activity).apply {
                            text = label
                            setTextColor(gray)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        },
                    )
                    addView(
                        TextView(activity).apply {
                            text = value
                            setTextColor(textColor)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            typeface = Typeface.DEFAULT_BOLD
                            textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                        },
                    )
                },
            )
        }

        metaRow(
            activity.getString(R.string.about_version_label),
            activity.getString(
                R.string.about_version_value,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
        )
        metaRow(activity.getString(R.string.about_package_label), BuildConfig.APPLICATION_ID)
        metaRow(activity.getString(R.string.about_build_label), BuildConfig.BUILD_TYPE)

        val dialog = BottomSheetDialog(activity, R.style.AppBottomSheetDialogTheme)
        dialog.setContentView(sheet)
        dialog.setCanceledOnTouchOutside(true)

        sheet.addView(
            MaterialButton(activity).apply {
                text = activity.getString(R.string.about_open_privacy)
                isAllCaps = false
                contentDescription = ABOUT_OPEN_PRIVACY_DESC
                backgroundTintList = ColorStateList.valueOf(primary)
                setTextColor(activity.colorFromAttribute(R.attr.colorOnPrimary))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.topMargin = (16 * density).toInt() }
                setOnClickListener {
                    dialog.dismiss()
                    showPrivacy(activity)
                }
            },
        )
        sheet.addView(
            MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = activity.getString(R.string.close)
                isAllCaps = false
                contentDescription = ABOUT_CLOSE_DESC
                setTextColor(textColor)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.topMargin = (8 * density).toInt() }
                setOnClickListener { dialog.dismiss() }
            },
        )

        dialog.show()
    }

    fun showPrivacy(activity: Activity) {
        val density = activity.resources.displayMetrics.density
        val pad = (20 * density).toInt()
        val textColor = activity.colorFromAttribute(R.attr.textColor)
        val gray = activity.colorFromAttribute(R.attr.grayTextColor)
        val boxBg = activity.colorFromAttribute(R.attr.boxItemBackground)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (10 * density).toInt(), pad, pad)
            contentDescription = PRIVACY_SHEET_DESC
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    18 * density, 18 * density, 18 * density, 18 * density,
                    0f, 0f, 0f, 0f,
                )
                setColor(boxBg)
            }
        }

        root.addView(
            View(activity).apply {
                layoutParams = LinearLayout.LayoutParams((42 * density).toInt(), (4 * density).toInt()).also {
                    it.gravity = android.view.Gravity.CENTER_HORIZONTAL
                    it.bottomMargin = (12 * density).toInt()
                }
                background = GradientDrawable().apply {
                    cornerRadius = 999f
                    setColor(ColorUtils.setAlphaComponent(gray, 90))
                }
            },
        )
        root.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.privacy_title)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = Typeface.DEFAULT_BOLD
            },
        )

        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
        }
        body.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.privacy_intro)
                setTextColor(gray)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, 0, 0, (10 * density).toInt())
            },
        )
        val sections = listOf(
            R.string.privacy_s1 to R.string.privacy_p1,
            R.string.privacy_s2 to R.string.privacy_p2,
            R.string.privacy_s3 to R.string.privacy_p3,
            R.string.privacy_s4 to R.string.privacy_p4,
            R.string.privacy_s5 to R.string.privacy_p5,
            R.string.privacy_s6 to R.string.privacy_p6,
        )
        sections.forEach { (titleRes, bodyRes) ->
            body.addView(
                TextView(activity).apply {
                    text = activity.getString(titleRes)
                    setTextColor(textColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, (10 * density).toInt(), 0, (4 * density).toInt())
                },
            )
            body.addView(
                TextView(activity).apply {
                    text = activity.getString(bodyRes)
                    setTextColor(gray)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                },
            )
        }
        body.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.privacy_updated)
                setTextColor(gray)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, (14 * density).toInt(), 0, 0)
            },
        )

        root.addView(
            ScrollView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (420 * density).toInt(),
                )
                addView(body)
            },
        )

        val dialog = BottomSheetDialog(activity, R.style.AppBottomSheetDialogTheme)
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(true)

        root.addView(
            MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = activity.getString(R.string.close)
                isAllCaps = false
                contentDescription = PRIVACY_CLOSE_DESC
                setTextColor(textColor)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.topMargin = (8 * density).toInt() }
                setOnClickListener { dialog.dismiss() }
            },
        )

        dialog.show()
    }
}
