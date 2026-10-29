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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream4.compose.focusOutline
import com.mihon.presentation.settings.LocalPreferenceHighlighted
import com.mihon.presentation.settings.LocalPreferenceMinHeight
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/** Look of the settings list, chosen in settings: 1 = colored tiles, 2 = mosaic cards, 3 = grouped cards. */
val LocalSettingsStyle = compositionLocalOf { 1 }

/** Index and size of the item inside its group, used to round the corners of grouped cards. */
val LocalItemPosition = compositionLocalOf { 0 to 1 }

private val TilePalette = listOf(
    listOf(Color(0xFFE5322D), Color(0xFFB3001B)),
    listOf(Color(0xFFFF7A1A), Color(0xFFE5322D)),
    listOf(Color(0xFFFFC247), Color(0xFFFF7A1A)),
    listOf(Color(0xFF7A3CFF), Color(0xFF4B1FB8)),
    listOf(Color(0xFF1FB88A), Color(0xFF0A7D5C)),
    listOf(Color(0xFF2F7BFF), Color(0xFF1B4FC2)),
)

/** Paints every visible pixel of the content with [color], so any icon can be recolored. */
private fun Modifier.recolor(color: Color): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(color, blendMode = BlendMode.SrcIn)
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
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)

    if (style == 2) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(surface)
                .highlightBackground(highlighted)
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .padding(top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .padding(start = PrefsHorizontalPadding)
                        .recolor(primary),
                    content = { icon() },
                )
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
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val badge = when (style) {
                1 -> Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            TilePalette[(title.orEmpty().hashCode() and Int.MAX_VALUE) % TilePalette.size]
                        )
                    )
                else -> Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primary.copy(alpha = 0.13f))
                    .border(1.dp, primary.copy(alpha = 0.33f), RoundedCornerShape(10.dp))
            }
            Box(
                modifier = Modifier.padding(start = PrefsHorizontalPadding, end = 8.dp),
            ) {
                Box(
                    modifier = badge,
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.recolor(if (style == 1) Color.White else Color(0xFFFFC247)),
                        content = { icon() },
                    )
                }
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
