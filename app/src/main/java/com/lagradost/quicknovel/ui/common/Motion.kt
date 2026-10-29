package com.lagradost.quicknovel.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

private const val STAGGER_STEP_MS = 35L
private const val STAGGER_LIMIT = 10
private const val RISE_PX = 48f

/** Delay before the item at [index] starts entering, capped so long lists do not wait. */
fun staggerDelayMs(index: Int): Long = index.coerceIn(0, STAGGER_LIMIT) * STAGGER_STEP_MS

/** Fades and lifts an item in with a spring, one after another, the first time it appears. */
@Composable
fun Modifier.staggeredEnter(index: Int): Modifier {
    val progress = remember { Animatable(if (index > STAGGER_LIMIT) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(staggerDelayMs(index))
            progress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * RISE_PX
    }
}
