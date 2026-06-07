package com.giantnovadevs.mysamoney

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.giantnovadevs.mysamoney.ads.AppOpenAdManager
import com.giantnovadevs.mysamoney.data.AppDatabase
import com.giantnovadevs.mysamoney.worker.ExpenseWorkerRepository
import com.giantnovadevs.mysamoney.worker.NotificationHelper
import com.giantnovadevs.mysamoney.worker.SubscriptionWorker
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MysaMoneyApplication : Application() {

    // App-scoped coroutine scope — lives as long as the process
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        AppOpenAdManager.load(this)
        setupRecurringWork()
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        // Fallback: process due recurring expenses on every launch.
        // WorkManager is the primary trigger (daily), but OEM battery
        // optimization on Xiaomi/Samsung/Realme often delays or kills it.
        // This ensures expenses are never missed even on restricted devices.
        appScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                ExpenseWorkerRepository(db).processDueExpenses()
            } catch (_: Exception) {}
        }
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