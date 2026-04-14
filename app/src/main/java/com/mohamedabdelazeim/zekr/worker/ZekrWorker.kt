package com.mohamedabdelazeim.zekr.worker

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.mohamedabdelazeim.zekr.service.ZekrService
import java.util.concurrent.TimeUnit

class ZekrWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val intent = Intent(applicationContext, ZekrService::class.java)
        applicationContext.startForegroundService(intent)
        return Result.success()
    }
}

object ZekrScheduler {
    private const val WORK = "zekr_periodic"

    fun schedule(ctx: Context, minutes: Long) {
        val req = PeriodicWorkRequestBuilder<ZekrWorker>(minutes, TimeUnit.MINUTES)
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK, ExistingPeriodicWorkPolicy.REPLACE, req
        )
    }

    fun cancel(ctx: Context) = WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
}
