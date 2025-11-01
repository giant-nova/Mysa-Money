package com.example.spendwise

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spendwise.worker.NotificationHelper
import com.example.spendwise.worker.SubscriptionWorker
import java.util.concurrent.TimeUnit

class SpendWiseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupRecurringWork()
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
    }

    private fun setupRecurringWork() {
        // 1. Define the constraints (e.g., run on Wi-Fi)
        // val constraints = Constraints.Builder()
        //     .setRequiredNetworkType(NetworkType.UNMETERED)
        //     .build()

        // 2. Create a repeating request to run once per day
        val repeatingRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(
            1, TimeUnit.DAYS
        )
            // .setConstraints(constraints) // Optional constraints
            .build()

        // 3. Schedule the work
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            SubscriptionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if it's already scheduled
            repeatingRequest
        )
    }
}