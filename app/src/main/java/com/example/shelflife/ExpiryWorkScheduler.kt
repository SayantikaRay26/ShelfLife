
package com.example.shelflife

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ExpiryWorkScheduler {

    private const val WORK_NAME = "shelflife_expiry_check"
    private const val TEST_WORK_NAME = "shelflife_expiry_test"

    fun schedule(context: Context) {

        val immediateWork =
            OneTimeWorkRequestBuilder<ExpiryWorker>().build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TEST_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateWork
        )

        val expiryWork =
            PeriodicWorkRequestBuilder<ExpiryWorker>(
                24, TimeUnit.HOURS
            ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            expiryWork
        )
    }
}
