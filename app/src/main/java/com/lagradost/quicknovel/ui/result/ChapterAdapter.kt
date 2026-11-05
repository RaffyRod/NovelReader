package com.lagradost.quicknovel.ui.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import com.lagradost.quicknovel.ChapterData
import com.lagradost.quicknovel.ChapterSpec
import com.lagradost.quicknovel.CommonActivity
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.databinding.SimpleChapterBinding
import com.lagradost.quicknovel.ui.BaseDiffCallback
import com.lagradost.quicknovel.ui.NoStateAdapter
import com.lagradost.quicknovel.ui.ViewHolderState
import com.lagradost.quicknovel.ui.newSharedPool

class ChapterAdapter(val viewModel: ResultViewModel) :
    NoStateAdapter<ChapterData>(
        diffCallback = BaseDiffCallback(
            itemSame = { a, b -> a.url == b.url },
            contentSame = { a, b -> a == b }
        )) {

    companion object {
        val sharedPool =
            newSharedPool {
                setMaxRecycledViews(CONTENT, 10)
            }
    }

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Any> {
        return ViewHolderState(
            SimpleChapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    private fun refresh(
        binding: SimpleChapterBinding,
        card: ChapterData,
        viewModel: ResultViewModel
    ) {
        val alpha = if (viewModel.hasReadChapter(chapter = card)) 0.5F else 1.0F

        binding.name.alpha = alpha
        binding.releaseDate.alpha = alpha
        binding.lastMarker.isGone = !viewModel.isLastChapter(card)
    }

    private fun showChapterMenu(
        binding: SimpleChapterBinding,
        card: ChapterData,
    ) {
        val context = binding.root.context
        val isLast = viewModel.isLastChapter(card)
        val read = viewModel.hasReadChapter(card)
        val labels = arrayOf(
            context.getString(if (isLast) R.string.clear_last_chapter else R.string.mark_last_chapter),
            context.getString(if (read) R.string.mark_unread else R.string.mark_read),
        )
        AlertDialog.Builder(context)
            .setTitle(card.name)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> {
                        if (isLast) {
                            viewModel.clearLastChapter()
                        } else {
                            viewModel.markLastChapter(card)
                            CommonActivity.showToast(
                                context.getString(R.string.last_chapter_saved, card.name)
                            )
                        }
                        notifyDataSetChanged()
                    }

                    else -> {
                        viewModel.setReadChapter(chapter = card, !read)
                        refresh(binding, card, viewModel)
                    }
                }
            }
            .show()
    }

    private fun refreshSelection(binding: SimpleChapterBinding, card: ChapterData) {
        val selecting = viewModel.selectionMode.value == true
        val selected = selecting && viewModel.isChapterSelected(card)
        binding.selectCheck.isGone = !selecting
        binding.selectCheck.isChecked = selected
    }

    override fun onBindContent(holder: ViewHolderState<Any>, item: ChapterData, position: Int) {
        val binding = holder.view as? SimpleChapterBinding ?: return
        binding.apply {
            val page = viewModel.chapterPage.value
            val absoluteNumber = (page?.firstPosition ?: 1) + position
            name.text = ChapterSpec.listTitle(
                absoluteNumber,
                item.name,
                binding.root.context.getString(R.string.chapter_sort),
            )
            releaseDate.isGone = true
            root.setOnClickListener {
                if (viewModel.selectionMode.value == true) {
                    viewModel.toggleChapterSelected(item)
                    refreshSelection(binding, item)
                    return@setOnClickListener
                }
                viewModel.streamRead(item)
                viewModel.isResume = true//to update read status
            }
            root.setOnLongClickListener {
                if (viewModel.selectionMode.value == true) {
                    viewModel.toggleChapterSelected(item)
                    refreshSelection(binding, item)
                    return@setOnLongClickListener true
                }
                showChapterMenu(binding, item)
                return@setOnLongClickListener true
            }
            refresh(binding, item, viewModel)
            refreshSelection(binding, item)
        }
    }
}