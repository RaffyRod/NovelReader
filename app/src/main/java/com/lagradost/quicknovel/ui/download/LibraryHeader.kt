package com.lagradost.quicknovel.ui.download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream4.compose.rounded
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.ui.common.CoverImage
import com.lagradost.quicknovel.ui.common.ImmutableSearchResponse
import com.lagradost.quicknovel.ui.common.SearchResponseAction
import com.lagradost.quicknovel.ui.common.SearchResponseItem
import com.lagradost.quicknovel.ui.common.SearchResponseOperation

/** Decides what the top of the library shows. */
object LibraryShelves {
    private const val MAX_NEW = 12

    /** The novel opened most recently, or null when none was ever opened. */
    fun continueReading(items: Collection<ImmutableSearchResponse>): ImmutableSearchResponse? =
        items.filter { (it.timeOfPageOpened ?: 0L) > 0L }.maxByOrNull { it.timeOfPageOpened ?: 0L }

    /** Novels with chapters that were downloaded after the last time they were read. */
    fun withNewChapters(items: Collection<ImmutableSearchResponse>): List<ImmutableSearchResponse> =
        items.filter { it.hasNewChapters }.take(MAX_NEW)
}

/** Top of the downloads page: the novel to keep reading and a shelf of novels with new chapters. */
@Composable
fun LibraryHeader(
    items: Collection<ImmutableSearchResponse>,
    action: (SearchResponseAction) -> Unit,
) {
    val current = LibraryShelves.continueReading(items)
    val fresh = LibraryShelves.withNewChapters(items)
    if (current == null && fresh.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (current != null) {
            ContinueCard(current, action)
        }
        if (fresh.isNotEmpty()) {
            Text(
                text = stringResource(R.string.library_new_chapters),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(fresh, key = { it.randomUuid }) { response ->
                    SearchResponseItem(
                        response = response,
                        action = action,
                        modifier = Modifier.width(104.dp)
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.library_all),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun ContinueCard(response: ImmutableSearchResponse, action: (SearchResponseAction) -> Unit) {
    val total = response.chapters ?: 0L
    val read = response.chaptersRead.toLong()
    val progress = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
    val open = { action(SearchResponseAction(response, SearchResponseOperation.Read)) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .rounded()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .clickable(onClick = open)
            .padding(12.dp)
    ) {
        CoverImage(
            model = response.imageRequest(),
            contentDescription = response.name,
            title = response.name,
            modifier = Modifier
                .width(64.dp)
                .height(92.dp)
                .rounded()
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.library_continue),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = response.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.library_chapter_of, read, total),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = open) { Text(stringResource(R.string.library_follow)) }
        }
    }
}
