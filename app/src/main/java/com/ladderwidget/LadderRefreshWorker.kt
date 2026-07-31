package com.ladderwidget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LadderRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LadderRepository(applicationContext).fetchAndCache()
            LadderWidgetProvider.updateWidgets(applicationContext)
            Result.success()
        } catch (exception: Exception) {
            LadderWidgetProvider.updateWidgets(applicationContext, exception.message)
            Result.retry()
        }
    }

    companion object {
        fun oneTimeRequest(): WorkRequest = OneTimeWorkRequestBuilder<LadderRefreshWorker>().build()
    }
}
