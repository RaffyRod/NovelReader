package com.lagradost.quicknovel.ui

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.PathParser
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.lagradost.quicknovel.R

/** Bottom menu looks the user can pick in settings. */
enum class NavStyle(val key: String) {
    /** Outline icons inside a translucent pill. */
    OUTLINE("A"),

    /** Icons that fill when selected. */
    SOLID("B"),

    /** Two tone icons with a glow. */
    DUOTONE("C"),

    /** Kanji instead of icons. */
    KANJI("D");

    companion object {
        const val DEFAULT_KEY = "A"
        fun fromKey(key: String?): NavStyle = entries.firstOrNull { it.key == key } ?: OUTLINE
    }
}

/** Shapes of the four bottom menu icons on a 24x24 grid, plus the kanji used by [NavStyle.KANJI]. */
enum class NavIcon(val itemId: Int, val kanji: String, val fillable: List<String>, val strokeOnly: List<String>) {
    LIBRARY(
        R.id.navigation_download, "本",
        listOf("M3,4h4v16H3z", "M9,4h4v16H9z", "M15.2,5.6l3.9,-1 3,14.6 -3.9,1z"),
        emptyList()
    ),
    SEARCH(
        R.id.navigation_search, "探",
        listOf("M4,10.5a6.5,6.5 0 1,0 13,0a6.5,6.5 0 1,0 -13,0"),
        listOf("M15.5,15.5L21,21")
    ),
    HISTORY(
        R.id.navigation_history, "歴",
        listOf("M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0"),
        listOf("M12,7v5l3,2")
    ),
    SECTIONS(
        R.id.navigation_sections, "棚",
        listOf("M4,4h16v4H4z", "M4,10h16v4H4z", "M4,16h16v4H4z"),
        emptyList()
    ),
    SETTINGS(
        R.id.navigation_settings, "設",
        listOf(
            "M12,2.5l1.6,2.4 2.8,-0.6 0.9,2.7 2.7,0.9 -0.6,2.8L21.5,12l-2.1,1.6 0.6,2.8 -2.7,0.9 -0.9,2.7 -2.8,-0.6L12,21.5l-1.6,-2.4 -2.8,0.6 -0.9,-2.7 -2.7,-0.9 0.6,-2.8L2.5,12l2.1,-1.6 -0.6,-2.8 2.7,-0.9 0.9,-2.7 2.8,0.6z",
            "M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0"
        ),
        emptyList()
    ),
}

/**
 * Bottom menu icon drawn from path data so one definition serves every [NavStyle].
 * It follows the tint list of the navigation bar and reacts to the selected state.
 * Optional [UiIconTheme] draws a soft fire halo behind the glyph.
 */
class NavIconDrawable(
    private val icon: NavIcon,
    private val style: NavStyle,
    private val fire: UiIconTheme = UiIconTheme.BLUE_FIRE,
) : Drawable() {
    private val fillPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
        icon.fillable.forEach { addPath(PathParser.createPathFromPathData(it)) }
    }
    private val strokePath = Path().apply {
        icon.strokeOnly.forEach { addPath(PathParser.createPathFromPathData(it)) }
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tint: ColorStateList? = null
    private var color = Color.WHITE
    private var selected = false

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val save = canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        val scale = minOf(b.width(), b.height()) / GRID
        canvas.scale(scale, scale)

        // Fire aura (stronger when selected)
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        val cx = GRID / 2f
        val cy = GRID * 0.72f
        val glowA = if (selected) 0xB8 else 0x55
        paint.shader = RadialGradient(
            cx, cy, GRID * 0.7f,
            intArrayOf(
                (glowA shl 24) or (fire.glowArgb and 0x00FFFFFF),
                (0x28 shl 24) or (fire.coreArgb and 0x00FFFFFF),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, GRID * 0.7f, paint)
        paint.shader = null

        if (style == NavStyle.KANJI) {
            paint.reset()
            paint.isAntiAlias = true
            paint.color = color
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            paint.textSize = 22f
            paint.textAlign = Paint.Align.CENTER
            val baseline = GRID / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(icon.kanji, GRID / 2f, baseline, paint)
            canvas.restoreToCount(save)
            return
        }

        val fillAlpha = when (style) {
            NavStyle.SOLID -> if (selected) 255 else 0
            NavStyle.DUOTONE -> if (selected) 115 else 46
            else -> 0
        }
        if (fillAlpha > 0) {
            paint.reset()
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = fillAlpha
            canvas.drawPath(fillPath, paint)
        }
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.9f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = color
        canvas.drawPath(fillPath, paint)
        canvas.drawPath(strokePath, paint)
        canvas.restoreToCount(save)
    }

    override fun isStateful() = true

    override fun onStateChange(state: IntArray): Boolean {
        selected = state.contains(android.R.attr.state_selected) || state.contains(android.R.attr.state_checked)
        color = tint?.getColorForState(state, tint?.defaultColor ?: Color.WHITE) ?: Color.WHITE
        invalidateSelf()
        return true
    }

    override fun setTintList(tint: ColorStateList?) {
        this.tint = tint
        onStateChange(state)
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    private companion object {
        const val GRID = 24f
    }
}

/** Applies a [NavStyle] to the bottom navigation bar. */
object NavStyler {
    fun apply(
        navView: BottomNavigationView,
        style: NavStyle,
        primary: Int,
        accent: Int,
        gray: Int,
        fire: UiIconTheme = UiIconTheme.BLUE_FIRE,
    ) {
        val active = when {
            style == NavStyle.DUOTONE -> fire.glowArgb
            else -> fire.coreArgb
        }
        val tint = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(active, gray)
        )
        navView.itemIconTintList = tint
        navView.itemTextColor = tint
        navView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        val pill = fire.coreArgb
        navView.itemActiveIndicatorColor = ColorStateList.valueOf(
            if (style == NavStyle.OUTLINE) Color.argb(0x33, Color.red(pill), Color.green(pill), Color.blue(pill))
            else Color.TRANSPARENT
        )
        NavIcon.entries.forEach { icon ->
            navView.menu.findItem(icon.itemId)?.icon = NavIconDrawable(icon, style, fire)
        }
    }

    /** Small bounce on the tab that was just selected. */
    fun pop(navView: BottomNavigationView, itemId: Int) {
        val view: View = navView.findViewById(itemId) ?: return
        view.scaleX = 0.82f
        view.scaleY = 0.82f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320)
            .setInterpolator(OvershootInterpolator(2.2f))
            .start()
    }
}
