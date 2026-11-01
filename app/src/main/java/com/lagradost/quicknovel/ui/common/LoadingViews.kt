package com.lagradost.quicknovel.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val loadingLineHeight = 15
const val loadingLineMargin = 7.5

/** Theme-aware skeleton fill used by the novel result loading screen. */
@Composable
fun Modifier.loading(): Modifier {
    return this
        .clip(RoundedCornerShape(5.dp))
        .shimmer()
}

@Composable
fun LoadingLine(fraction: Float = 1.0f) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .padding(loadingLineMargin.dp)
            .height(loadingLineHeight.dp)
            .loading()
    )
}

@Composable
fun RowScope.LoadingButton() {
    Box(
        modifier = Modifier
            .weight(1.0f)
            .padding(loadingLineMargin.dp)
            .height(35.dp)
            .loading()
    )
}

@Composable
fun LoadingWidth(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(loadingLineMargin.dp)
            .height(loadingLineHeight.dp)
            .loading()
    )
}

@Composable
fun RowScope.LoadingWeight() {
    Box(
        modifier = Modifier
            .weight(1.0f)
            .padding(loadingLineMargin.dp)
            .height(loadingLineHeight.dp)
            .loading()
    )
}

@Composable
fun LoadingPoster() {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 140.dp)
            .padding(loadingLineMargin.dp)
            .loading()
    )
}
