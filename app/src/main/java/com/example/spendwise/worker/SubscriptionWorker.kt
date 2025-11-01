package com.example.spendwise.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spendwise.data.AppDatabase
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class SubscriptionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = ExpenseWorkerRepository(db)

        return try {
            // ✅ Get the list of expenses that were just processed
            val processedExpenses = repository.processDueExpenses()

            // Check if we have permission to send notifications
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // ✅ If we do, send notifications
                val notificationHelper = NotificationHelper(applicationContext)
                processedExpenses.forEachIndexed { index, expense ->
                    val title = "Expense Logged"
                    val message = "Your recurring payment for '${expense.note ?: "Subscription"}' (₹${expense.amount}) was added."

                    // Give each notification a unique ID
                    notificationHelper.showNotification(title, message,
                        NotificationHelper.NOTIFICATION_ID_PREFIX + index
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    // Companion object to define the unique worker name
    companion object {
        const val WORK_NAME = "SubscriptionWorker"
    }
}