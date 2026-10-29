package com.lagradost.quicknovel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.lagradost.cloudstream4.AppSettings
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.getKeys
import com.lagradost.quicknovel.BaseApplication.Companion.setKey
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.mvvm.Resource
import com.lagradost.quicknovel.ui.ReadType
import com.lagradost.quicknovel.util.Apis
import com.lagradost.quicknovel.util.ResultCached
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/** What a check found for one followed novel. */
data class NewChaptersResult(val cached: ResultCached, val newCount: Int, val latestTitle: String?)

/** Pure comparison used by the worker, kept separate so it can be unit tested. */
object NewChaptersLogic {
    /** Novels in these states are the ones the user follows. */
    val followedStates: Set<Int> = setOf(ReadType.READING.prefValue, ReadType.PLAN_TO_READ.prefValue)

    /**
     * Number of chapters that appeared since [known], or 0 when nothing new
     * (also when the source removed chapters).
     */
    fun newChapters(known: Int, current: Int): Int = (current - known).coerceAtLeast(0)
}

/** Periodically looks for new chapters in the novels the user follows and notifies about them. */
class NewChaptersWorker(private val appContext: Context, private val params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        private const val PERIODIC_WORK = "new_chapters_check"
        private const val MANUAL_WORK = "new_chapters_check_now"
        private const val MANUAL = "manual"
        const val CHANNEL_ID = "quicknovel.newchapters"
        private const val MAX_NOVELS_PER_RUN = 60
        private const val REQUEST_DELAY_MS = 800L

        /** Enables or disables the periodic check, replacing the previous schedule. */
        fun schedule(context: Context, enabled: Boolean, hours: Int) {
            val manager = DownloadFileWorkManager.getWorkerManager(context)
            if (!enabled) {
                manager.cancelUniqueWork(PERIODIC_WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<NewChaptersWorker>(
                hours.coerceAtLeast(1).toLong(), TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            manager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** Applies the saved settings, used when the app starts. */
        fun scheduleFromSettings(context: Context) {
            val updates = AppSettings(context).updates
            schedule(context, updates.notifyNewChapters.get(), updates.chapterCheckHours.get())
        }

        /** Runs a single check right now, even when the periodic one is disabled. */
        fun checkNow(context: Context) {
            DownloadFileWorkManager.getWorkerManager(context).enqueueUniqueWork(
                MANUAL_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NewChaptersWorker>()
                    .setInputData(Data.Builder().putBoolean(MANUAL, true).build())
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .build()
            )
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.new_chapters_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.new_chapters_channel_desc) }
            )
        }
    }

    override suspend fun doWork(): Result {
        val manual = params.inputData.getBoolean(MANUAL, false)
        val updates = AppSettings(appContext).updates
        if (!manual && !updates.notifyNewChapters.get()) return Result.success()

        val followed = followedNovels().take(MAX_NOVELS_PER_RUN)
        val found = ArrayList<NewChaptersResult>()
        for (cached in followed) {
            check(cached)?.let { found.add(it) }
            delay(REQUEST_DELAY_MS.milliseconds)
        }

        ensureChannel(appContext)
        if (NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            found.forEach { notify(it) }
        }
        if (manual) {
            showToast(
                (CommonActivity.activity ?: appContext)
                    .getString(R.string.new_chapters_check_done, followed.size, found.size)
            )
        }
        return Result.success()
    }

    private fun followedNovels(): List<ResultCached> {
        val keys = getKeys(RESULT_BOOKMARK_STATE) ?: return emptyList()
        return keys.mapNotNull { key ->
            val state = getKey<Int>(key) ?: return@mapNotNull null
            if (state !in NewChaptersLogic.followedStates) return@mapNotNull null
            getKey<ResultCached>(key.replaceFirst(RESULT_BOOKMARK_STATE, RESULT_BOOKMARK))
        }.filterNot { it.isImported }
    }

    private suspend fun check(cached: ResultCached): NewChaptersResult? {
        val api = Apis.getApiFromNameOrNull(cached.apiName) ?: return null
        val loaded = api.load(cached.source, allowCache = false)
        val stream = (loaded as? Resource.Success)?.value as? StreamResponse ?: return null

        val known = getKey<Int>(NEW_CHAPTERS_KNOWN, cached.id.toString()) ?: cached.totalChapters
        val current = stream.data.size
        val newCount = NewChaptersLogic.newChapters(known, current)

        setKey(NEW_CHAPTERS_KNOWN, cached.id.toString(), current)
        if (current != cached.totalChapters) {
            setKey(
                RESULT_BOOKMARK,
                cached.id.toString(),
                cached.copy(totalChapters = current, cachedTime = System.currentTimeMillis())
            )
        }
        return if (newCount > 0) {
            NewChaptersResult(cached, newCount, stream.data.lastOrNull()?.name)
        } else {
            null
        }
    }

    private fun notify(result: NewChaptersResult) {
        val cached = result.cached
        val intent = Intent(appContext, MainActivity::class.java).apply {
            data = cached.source.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            cached.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = (CommonActivity.activity ?: appContext).resources.getQuantityString(
            R.plurals.new_chapters_text,
            result.newCount,
            result.newCount
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
            .setContentTitle(cached.name)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(listOfNotNull(text, result.latestTitle).joinToString("\n"))
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // Only reached after areNotificationsEnabled(), which also covers the runtime permission
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(appContext).notify(cached.id, notification)
    }
}
