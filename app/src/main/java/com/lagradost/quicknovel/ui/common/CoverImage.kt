package com.lagradost.quicknovel.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lagradost.quicknovel.R

/**
 * Book cover that always occupies its space. While loading / on missing or failed art,
 * shows the title-as-cover placeholder (theme colors + localized “no cover” hint).
 */
@Composable
fun CoverImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    title: String? = contentDescription,
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val hint = stringResource(R.string.no_cover)
    val displayTitle = title?.trim()?.takeIf { it.isNotEmpty() && !it.equals(hint, ignoreCase = true) }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(primary.copy(alpha = 0.22f), surface),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            if (displayTitle != null) {
                Text(
                    text = displayTitle,
                    color = onSurface.copy(alpha = 0.88f),
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = hint.uppercase(),
                color = onSurface.copy(alpha = 0.55f),
                fontSize = 9.sp,
                letterSpacing = 1.1.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (displayTitle != null) 10.dp else 0.dp),
            )
        }
        AsyncImage(
            contentScale = ContentScale.Crop,
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
