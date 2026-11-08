
package com.example.shelflife

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpiryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = PantryDatabase.getDatabase(applicationContext)
            val items = database.pantryDao().getAllItemsForNotification()

            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.isLenient = false

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val expiringItems = items.filter { item ->
                val expiryDate = formatter.parse(item.expiryDate)

                if (expiryDate == null) {
                    false
                } else {
                    val expiry = Calendar.getInstance().apply {
                        time = expiryDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val daysLeft =
                        (expiry.timeInMillis - today.timeInMillis) /
                                (24 * 60 * 60 * 1000L)

                    daysLeft in 0..3
                }
            }

            if (expiringItems.isNotEmpty()) {
                val names = expiringItems.joinToString(", ") { it.name }

                NotificationHelper.showExpiryNotification(
                    applicationContext,
                    "Food Expiry Reminder 🌿",
                    "These items are expiring within 3 days: $names"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
