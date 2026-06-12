package com.paybuddy

import android.app.Application
import androidx.work.*
import com.paybuddy.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class PayBuddyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupReminderWorker()
    }

    private fun setupReminderWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .addTag("payment_reminder_worker")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "payment_reminder_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
