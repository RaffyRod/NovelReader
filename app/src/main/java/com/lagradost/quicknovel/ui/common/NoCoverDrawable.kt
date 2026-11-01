package com.lagradost.quicknovel.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.util.UIHelper.colorFromAttribute
import kotlin.math.min

/**
 * Title-as-cover placeholder (mock option J): novel title centered on a theme-tinted field,
 * with the localized “No cover / Sin portada” hint. Used by Coil and View loaders.
 */
class NoCoverDrawable(
    context: Context,
    private val title: String?,
) : Drawable() {
    private val hint = context.getString(R.string.no_cover)
    private val surface = context.colorFromAttribute(R.attr.primaryBlackBackground)
    private val primary = context.colorFromAttribute(R.attr.colorPrimary)
    private val onSurface = context.colorFromAttribute(R.attr.textColor)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(onSurface, 210)
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    private val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(onSurface, 140)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val w = b.width().toFloat()
        val h = b.height().toFloat()

        fillPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            ColorUtils.blendARGB(surface, primary, 0.22f),
            surface,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(b, fillPaint)
        fillPaint.shader = null

        val padX = w * 0.12f
        val maxTitleWidth = (w - padX * 2f).toInt().coerceAtLeast(1)
        titlePaint.textSize = min(w, h) * 0.13f
        hintPaint.textSize = min(w, h) * 0.055f

        val displayTitle = title?.trim()?.takeIf { it.isNotEmpty() && !it.equals(hint, true) }
        val titleLayout = if (displayTitle != null) {
            StaticLayout.Builder.obtain(displayTitle, 0, displayTitle.length, titlePaint, maxTitleWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(4)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            null
        }

        val hintWidth = maxTitleWidth
        val hintLayout = StaticLayout.Builder.obtain(hint, 0, hint.length, hintPaint, hintWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(1)
            .setIncludePad(false)
            .build()

        val blockHeight = (titleLayout?.height ?: 0) + hintLayout.height + (h * 0.04f).toInt()
        var top = b.top + (h - blockHeight) / 2f
        if (titleLayout != null) {
            canvas.save()
            canvas.translate(b.exactCenterX() - titleLayout.width / 2f, top)
            titleLayout.draw(canvas)
            canvas.restore()
            top += titleLayout.height + h * 0.04f
        }
        canvas.save()
        canvas.translate(b.exactCenterX() - hintLayout.width / 2f, top)
        hintLayout.draw(canvas)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        titlePaint.alpha = (alpha * 0.82f).toInt().coerceIn(0, 255)
        hintPaint.alpha = (alpha * 0.55f).toInt().coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        titlePaint.colorFilter = colorFilter
        hintPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = 136
    override fun getIntrinsicHeight(): Int = 200
}
