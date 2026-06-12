package com.paybuddy.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.model.Installment
import com.paybuddy.data.util.InstallmentMapper
import kotlinx.coroutines.tasks.await
import java.util.*

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ReminderWorker"
        private const val CHANNEL_ID = "payment_reminders"
        private const val CHANNEL_NAME = "Payment Reminders"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val vendorId = auth.currentUser?.uid ?: return Result.failure() // Unrecoverable: No user logged in

        val firestore = FirebaseFirestore.getInstance()
        
        return try {
            // Fetch all installments for this vendor
            // Filtering in memory to leverage InstallmentMapper's dynamic status logic
            val snapshot = firestore.collection("installments")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                return Result.success()
            }

            val installments = snapshot.documents.mapNotNull { 
                InstallmentMapper.mapDocumentToInstallment(it) 
            }

            val today = Calendar.getInstance()
            val todayStart = today.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val todayEnd = todayStart + 86400000L

            var dueTodayCount = 0
            var overdueCount = 0

            for (installment in installments) {
                // Only consider unpaid installments
                if (installment.status != "PAID" && installment.status != "COMPLETED") {
                    if (installment.dueDate in todayStart until todayEnd) {
                        dueTodayCount++
                    } else if (installment.dueDate < todayStart) {
                        overdueCount++
                    }
                }
            }

            if (dueTodayCount > 0 || overdueCount > 0) {
                sendSummaryNotification(dueTodayCount, overdueCount)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReminderWorker: ${e.message}", e)
            // Retry for transient network errors
            Result.retry()
        }
    }

    private fun sendSummaryNotification(dueToday: Int, overdue: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val contentText = when {
            dueToday > 0 && overdue > 0 -> "$dueToday due today, $overdue overdue payment reminder${if (overdue > 1) "s" else ""}"
            dueToday > 0 -> "$dueToday payment reminder${if (dueToday > 1) "s" else ""} due today"
            overdue > 0 -> "$overdue overdue payment reminder${if (overdue > 1) "s" else ""}"
            else -> "You have pending payment reminders today"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Payment Reminders")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon if available
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
