package com.mihon.presentation.settings.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import com.lagradost.cloudstream4.compose.focusOutline
import com.lagradost.quicknovel.ui.AppearanceMode
import com.lagradost.quicknovel.ui.IconStyle
import com.lagradost.quicknovel.ui.LocalIconStyle
import com.lagradost.quicknovel.ui.LocalUiIconTheme
import com.lagradost.quicknovel.ui.UiIconTheme
import com.lagradost.quicknovel.ui.coreFor
import com.lagradost.quicknovel.ui.glowFor
import com.lagradost.quicknovel.ui.glyphFor
import com.mihon.presentation.settings.LocalPreferenceHighlighted
import com.mihon.presentation.settings.LocalPreferenceMinHeight
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/** Look of the settings list, chosen in settings: 1 = colored tiles, 2 = mosaic cards, 3 = grouped cards. */
val LocalSettingsStyle = compositionLocalOf { 1 }

/** Index and size of the item inside its group, used to round the corners of grouped cards. */
val LocalItemPosition = compositionLocalOf { 0 to 1 }

/** Soft fire halo behind a settings icon (style A). */
private fun fireAuraBrush(glow: Color, core: Color): Brush {
    return object : ShaderBrush() {
        override fun createShader(size: androidx.compose.ui.geometry.Size) =
            RadialGradientShader(
                colors = listOf(
                    glow.copy(alpha = 0.72f),
                    core.copy(alpha = 0.28f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.72f),
                radius = size.minDimension * 0.72f,
                tileMode = TileMode.Clamp,
            )
    }
}

/** Paints every visible pixel of the content with [color], so any icon can be recolored. */
private fun Modifier.recolor(color: Color): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(color, blendMode = BlendMode.SrcIn)
    }

private fun onFilledGlyph(theme: UiIconTheme, darkSurface: Boolean): Color {
    val core = theme.coreFor(darkSurface)
    val lum = 0.2126f * core.red + 0.7152f * core.green + 0.0722f * core.blue
    return if (lum > 0.55f) Color(0xFF0A0C10) else Color.White
}

/**
 * Settings-row icon badge. Colors from [UiIconTheme] (surface-aware); treatment from [IconStyle].
 */
@Composable
private fun PreferenceIconBadge(
    theme: UiIconTheme,
    style: IconStyle,
    icon: @Composable () -> Unit,
    sizeDp: Int = 40,
) {
    val darkSurface = AppearanceMode.isDark(LocalContext.current)
    val size = sizeDp.dp
    val squircle = RoundedCornerShape(12.dp)
    val core = theme.coreFor(darkSurface)
    val glow = theme.glowFor(darkSurface)
    val glyph = theme.glyphFor(darkSurface)
    when (style) {
        IconStyle.FIRE_AURA -> {
            val fill = Brush.linearGradient(listOf(glow, core))
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(squircle)
                    .background(fill)
                    .background(fireAuraBrush(glow, core)),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(onFilledGlyph(theme, darkSurface)), content = { icon() })
            }
        }
        IconStyle.SOFT_HALO -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(core.copy(alpha = if (darkSurface) 0.16f else 0.12f))
                    .border(2.dp, core.copy(alpha = if (darkSurface) 0.55f else 0.85f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(core), content = { icon() })
            }
        }
        IconStyle.FLAT_TINT -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(squircle)
                    .background(core),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(onFilledGlyph(theme, darkSurface)), content = { icon() })
            }
        }
        IconStyle.OUTLINE_RING -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.5.dp, core, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(core), content = { icon() })
            }
        }
        IconStyle.SQUIRCLE_TILE -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(squircle)
                    .background(Brush.linearGradient(listOf(glow, core))),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(onFilledGlyph(theme, darkSurface)), content = { icon() })
            }
        }
        IconStyle.NEON_EDGE -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(squircle)
                    .background(if (darkSurface) Color(0xFF050505) else Color(0xFF1A1F2A))
                    .border(1.dp, core, squircle),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(glow), content = { icon() })
            }
        }
        IconStyle.GLASS_CAPSULE -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(14.dp))
                    .background(core.copy(alpha = if (darkSurface) 0.22f else 0.14f))
                    .border(1.dp, core.copy(alpha = if (darkSurface) 0.45f else 0.7f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(if (darkSurface) glyph else core), content = { icon() })
            }
        }
        IconStyle.DUOTONE_BADGE -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(squircle)
                    .background(
                        Brush.linearGradient(
                            0f to core,
                            0.5f to core,
                            0.5f to glow,
                            1f to glow,
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(Color.White), content = { icon() })
            }
        }
        IconStyle.MINIMAL_GLYPH -> {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(core), content = { icon() })
            }
        }
        IconStyle.ORBIT_DOT -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(squircle)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f), squircle),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.recolor(MaterialTheme.colorScheme.onBackground), content = { icon() })
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(core),
                )
            }
        }
    }
}

@Composable
internal fun BasePreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subcomponent: @Composable (ColumnScope.() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
) {
    val highlighted = LocalPreferenceHighlighted.current
    val minHeight = LocalPreferenceMinHeight.current
    val style = LocalSettingsStyle.current
    val (position, count) = LocalItemPosition.current
    val uiIcon = LocalUiIconTheme.current
    val iconStyle = LocalIconStyle.current
    val surface = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)

    // Merge title into one clickable a11y node so TalkBack/UiAutomator hit the row, not orphan TextViews.
    fun Modifier.preferenceRowSemantics(): Modifier {
        if (onClick == null || title.isNullOrBlank()) return this
        return semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = title
        }
    }

    if (style == 2) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(surface)
                .highlightBackground(highlighted)
                .preferenceRowSemantics()
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .padding(top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Box(modifier = Modifier.padding(start = PrefsHorizontalPadding)) {
                    PreferenceIconBadge(theme = uiIcon, style = iconStyle, icon = icon, sizeDp = 36)
                }
            }
            if (!title.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 14.sp,
                )
            }
            subcomponent?.invoke(this)
            if (widget != null) {
                Box(
                    modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                    content = { widget() },
                )
            }
        }
        return
    }

    val cardShape = if (style == 3) {
        RoundedCornerShape(
            topStart = if (position == 0) 18.dp else 0.dp,
            topEnd = if (position == 0) 18.dp else 0.dp,
            bottomStart = if (position == count - 1) 18.dp else 0.dp,
            bottomEnd = if (position == count - 1) 18.dp else 0.dp,
        )
    } else {
        RoundedCornerShape(0.dp)
    }
    Row(
        modifier = modifier
            .let { if (style == 3) it.padding(horizontal = 12.dp).clip(cardShape).background(surface) else it }
            .highlightBackground(highlighted)
            .focusOutline()
            .sizeIn(minHeight = minHeight)
            .preferenceRowSemantics()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.padding(start = PrefsHorizontalPadding, end = 8.dp),
            ) {
                PreferenceIconBadge(
                    theme = uiIcon,
                    style = iconStyle,
                    icon = icon,
                    sizeDp = if (style == 1) 40 else 38,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = if (style == 1) 10.dp else PrefsVerticalPadding),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = TitleFontSize,
                )
            }
            subcomponent?.invoke(this)
        }
        if (widget != null) {
            Box(
                modifier = Modifier.padding(end = PrefsHorizontalPadding),
                content = { widget() },
            )
        }
    }
}
@Composable
internal fun Modifier.highlightBackground(highlighted: Boolean): Modifier {
    var highlightFlag by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (highlighted) {
            highlightFlag = true
            delay(3.seconds)
            highlightFlag = false
        }
    }
    val highlight by animateColorAsState(
        targetValue = if (highlightFlag) {
            MaterialTheme.colorScheme.onBackground.copy(alpha = .12f)
        } else {
            Color.Transparent
        },
        animationSpec = if (highlightFlag) {
            repeatable(
                iterations = 5,
                animation = tween(durationMillis = 200),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(
                    offsetMillis = 600,
                    offsetType = StartOffsetType.Delay,
                ),
            )
        } else {
            tween(200)
        },
        label = "highlight",
    )
    return this.background(color = highlight)
}

internal val TrailingWidgetBuffer = 16.dp
internal val PrefsHorizontalPadding = 16.dp
internal val PrefsVerticalPadding = 16.dp
internal val TitleFontSize = 16.sp
